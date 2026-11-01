// RushIt account deletion — optional server-side purge.
//
// THE APP NO LONGER CALLS THIS FUNCTION.
//
// The Android app deletes the Firebase account in-process (FirebaseAuth.delete) and then deletes its
// own `public.profiles` row, which cascades to settings, devices and activity logs. Row Level
// Security authorizes that write with the verified Firebase ID token, so no service-role key is
// needed anywhere for the normal path.
//
// This function remains for the one case the device cannot handle: the Firebase account is already
// gone but the profile row could not be purged (app killed, network lost). It deletes rows by the
// verified Firebase UID using the service-role key, which exists only in this runtime.
//
// Deploy (optional):
//   supabase secrets set SUPABASE_SERVICE_ROLE_KEY=<service-role key>
//   supabase secrets set FIREBASE_PROJECT_ID=<firebase project id>
//   supabase functions deploy delete-account
//
// Keep JWT verification ON at the platform level as well: it rejects requests that carry no token
// before this code runs. This function then re-verifies the token itself, because Firebase ID tokens
// are not Supabase sessions and the old `auth.getUser()` check no longer applies to them.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";
import { createRemoteJWKSet, jwtVerify } from "https://esm.sh/jose@5.9.6";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

// Google publishes the keys Firebase used to sign ID tokens. They rotate; the library caches and
// re-fetches as needed.
const firebaseJwks = createRemoteJWKSet(
  new URL("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"),
);

/**
 * Verifies a Firebase ID token and returns its subject (the Firebase UID), or null.
 *
 * Nothing from the request body is trusted: the account touched is always the account the token
 * belongs to. Both `iss` and `aud` are pinned to the configured Firebase project, so a token minted
 * for any other project is rejected even if its signature is valid.
 */
async function verifiedFirebaseUid(
  token: string,
  projectId: string,
): Promise<string | null> {
  try {
    const { payload } = await jwtVerify(token, firebaseJwks, {
      issuer: `https://securetoken.google.com/${projectId}`,
      audience: projectId,
      algorithms: ["RS256"],
    });
    const subject = typeof payload.sub === "string" ? payload.sub : "";
    // Firebase guarantees a non-empty, at-most-128-character subject.
    if (subject === "" || subject.length > 128) return null;
    return subject;
  } catch (error) {
    // Log the reason for operators, never return it to the caller.
    console.error("delete-account token verification failed", (error as Error).message);
    return null;
  }
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  if (req.method !== "POST") {
    return json(405, { error: "Use POST to delete an account." });
  }

  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const projectId = Deno.env.get("FIREBASE_PROJECT_ID");

  if (!serviceRoleKey || !supabaseUrl || !projectId) {
    // Deliberately vague: configuration problems are not described to a caller.
    console.error("delete-account is missing SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY or FIREBASE_PROJECT_ID");
    return json(500, { error: "Account deletion is not configured." });
  }

  const authorization = req.headers.get("Authorization") ?? "";
  const accessToken = authorization.startsWith("Bearer ")
    ? authorization.slice("Bearer ".length).trim()
    : "";

  if (accessToken === "") {
    return json(401, { error: "Sign in again before deleting your account." });
  }

  const firebaseUid = await verifiedFirebaseUid(accessToken, projectId);
  if (firebaseUid === null) {
    return json(401, { error: "Your session could not be verified. Sign in again." });
  }

  // Service role: the only place this key is ever used. It never appears in the Android app.
  const adminClient = createClient(supabaseUrl, serviceRoleKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });

  // Deleting the profile row cascades to user_settings, user_devices and user_activity_logs through
  // the foreign keys installed by 20261101090000_firebase_identity.sql.
  const { error: deleteError, count } = await adminClient
    .from("profiles")
    .delete({ count: "exact" })
    .eq("firebase_uid", firebaseUid);

  if (deleteError) {
    console.error("delete-account profile purge failed", deleteError.message);
    return json(500, { error: "RushIt could not delete your saved profile. Please try again." });
  }

  // count === 0 with no error means there was nothing left to delete, which is a success.
  console.log("delete-account purged rows for a verified Firebase identity", { rows: count ?? 0 });

  // The Firebase Authentication user itself is NOT deleted here: deleting it requires the Firebase
  // Admin SDK and a service-account private key. The app performs that step on the device while the
  // user is present to reauthenticate, which is also the only way to satisfy Firebase's
  // "requires recent login" rule.
  return json(200, { deleted: true });
});
