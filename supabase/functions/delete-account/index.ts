// RushIt account deletion.
//
// This is the trusted server-side half of "Delete account". The Android app calls it with the
// signed-in user's own access token; the service-role key is read from the function environment and
// never leaves this runtime.
//
// Deploy:
//   supabase functions deploy delete-account
//   (JWT verification stays ON — the function must only run for an authenticated user)
//
// Required secret, set once per project:
//   supabase secrets set SUPABASE_SERVICE_ROLE_KEY=<service-role key>
// The runtime already provides SUPABASE_URL and SUPABASE_ANON_KEY.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
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
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");

  if (!serviceRoleKey || !supabaseUrl || !anonKey) {
    // Deliberately vague: configuration problems must not be described to a caller.
    console.error("delete-account is missing required environment variables");
    return json(500, { error: "Account deletion is not configured." });
  }

  const authorization = req.headers.get("Authorization") ?? "";
  const accessToken = authorization.startsWith("Bearer ")
    ? authorization.slice("Bearer ".length).trim()
    : "";

  if (accessToken === "") {
    return json(401, { error: "Sign in again before deleting your account." });
  }

  // Authorize with the caller's own token. Nothing in the request body is trusted: the account to
  // delete is always the account that the JWT belongs to.
  const callerClient = createClient(supabaseUrl, anonKey, {
    global: { headers: { Authorization: `Bearer ${accessToken}` } },
    auth: { persistSession: false, autoRefreshToken: false },
  });

  const { data, error } = await callerClient.auth.getUser();
  const userId = data?.user?.id;

  if (error || !userId) {
    return json(401, { error: "Your session could not be verified. Sign in again." });
  }

  const adminClient = createClient(supabaseUrl, serviceRoleKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });

  const { error: deleteError } = await adminClient.auth.admin.deleteUser(userId);

  if (deleteError) {
    console.error("delete-account failed for user", userId, deleteError.message);
    // The caller is told it failed, but not why: internal detail stays in the function logs.
    return json(500, { error: "RushIt could not delete your account. Please try again." });
  }

  console.log("delete-account removed user", userId);

  // profiles, user_settings, user_devices and user_activity_logs all reference auth.users with
  // ON DELETE CASCADE, so removing the auth user removes every owned row.
  return json(200, { deleted: true });
});
