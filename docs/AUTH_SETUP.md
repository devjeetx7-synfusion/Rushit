# RushIt authentication setup — Firebase Auth + Supabase

This document is the complete, ordered setup for RushIt's authentication. Everything in the
**Android application** is already implemented; what remains is console configuration and applying one
database migration.

**Read this first, in one sentence:** Firebase Authentication issues the identity (Google sign-in and
phone SMS code), Supabase stores the profile data, and Supabase authorizes each request by verifying
the Firebase ID token — using Supabase's supported Third-Party Auth integration, not a custom
token exchange and not Supabase Auth.

---

## 0. What the app does versus what you configure

| Concern | Owner | Where it lives |
| --- | --- | --- |
| Who the user is | Firebase Authentication | `data/auth/firebase/FirebaseAuthGateway.kt` |
| Google account picker | Jetpack Credential Manager | `data/auth/google/CredentialManagerGoogleSignIn.kt` |
| Phone number → SMS code | Firebase Authentication | same gateway |
| Orchestration, state, error mapping | `FirebaseAuthRepository` | `data/auth/FirebaseAuthRepository.kt` |
| Profile, settings, activity data | Supabase PostgreSQL + RLS | `data/auth/SupabaseProfileGateway.kt` |
| Handing the Firebase token to Supabase | `SupabaseIdentityBridge` | `data/auth/supabase/SupabaseIdentityBridge.kt` |
| Database schema, policies, admin roles | Supabase migration | `supabase/migrations/20261101090000_firebase_identity.sql` |

There is **no** Supabase Auth sign-in, sign-up, refresh, OAuth or password call anywhere in the app,
and no `rushit://` deep link, no Custom Tab and no browser redirect.

---

## 1. Prerequisites

* A Firebase project (recommended: one per environment, e.g. `rushit-app`).
* Owner/editor access to that Firebase project and to the Supabase project
  (`https://uootcypdodfyqfigracp.supabase.co`).
* Supabase CLI (optional but recommended) and a database backup:
  `supabase db dump -f before_firebase_identity.sql`.
* The SHA-1 **and** SHA-256 fingerprints of every key you install the APK with. `./gradlew
  signingReport` (or `apksigner verify --print-certs app-debug.apk`) prints them.

---

## 2. Firebase console

### 2.1 Create the project and register the Android app

1. Firebase console → **Add project** (or open the existing one).
2. **Project settings → Your apps → Add app → Android**.
3. Android package name: **`com.rushit.app`** (must match `applicationId` exactly).
4. Add a SHA-1 for every signing identity:
   * the local debug key,
   * the CI persistent test key (if configured),
   * the Play App Signing key, once the app is distributed.

   Google sign-in through Credential Manager matches the *Android* OAuth client by package name **and**
   signing fingerprint. A missing fingerprint produces `GetCredentialProviderConfigurationException`
   / "no credentials", not a helpful error.
5. Download **`google-services.json`** and place it at **`app/google-services.json`**.
   It is git-ignored. Gradle applies the `google-services` plugin only when that file exists and emits
   `BuildConfig.FIREBASE_CONFIGURED`; without it the app still builds and reports itself as
   unconfigured instead of crashing.

### 2.2 Enable the sign-in methods

1. **Authentication → Sign-in method → Google → Enable.**
   * Set a support email.
   * Note the **Web client ID** shown for the Google provider (it also appears under
     **Project settings → Your apps** as the "Web client (auto created by Google Service)"
     with id `…apps.googleusercontent.com`).
   * That value is `GOOGLE_WEB_CLIENT_ID`. It is the **audience** of the Google ID token Firebase will
     verify, so it must be the *Web* client, never the Android client ID, and never a client secret.
2. **Authentication → Sign-in method → Phone → Enable.**
3. **Authentication → Settings → Authorized domains**: leave the defaults unless you add a web client.

### 2.3 Phone verification prerequisites

* Android app verification uses **Play Integrity** when available. Add your release SHA-256 under
  **Project settings → App Check** (and register the app there) so production builds pass. Debug builds
  fall back to the reCAPTCHA/disabled-app-verification path and still work.
* **Test phone numbers (mandatory for testing):** Authentication → Sign-in method → Phone →
  **Phone numbers for testing**. Add a number (e.g. `+91 9999999999`) with a fixed code
  (e.g. `123456`). These never send an SMS and are not billed.
* SMS quota: new Firebase projects have a low daily SMS limit; a real number can fail with
  `ERROR_QUOTA_EXCEEDED`. Use test numbers instead of spending money.
* Billing: phone sign-in requires the project's billing/quota to be in order, otherwise
  `ERROR_BILLING_NOT_ENABLED` appears.

---

## 3. Supabase: trust Firebase ID tokens

This is Supabase's **supported** integration: Supabase verifies the Firebase JWT's signature, issuer
and audience before any SQL runs, and maps the token's claims into `auth.jwt()`.

### 3.1 Register the Firebase project

**Dashboard route**

1. Supabase Dashboard → **Authentication → Sign In / Providers → Third-Party Auth**.
2. Add provider **Firebase**, enter the Firebase **Project ID** (e.g. `rushit-app`), save.

**CLI route** (`supabase/config.toml`)

```toml
[auth.third_party.firebase]
enabled = true
project_id = "rushit-app"
```

Notes that matter:

* Firebase ID tokens are signed with **asymmetric** keys and carry a `kid`, which is what Supabase
  requires. Rotation is picked up automatically (within about 30 minutes).
* Supabase rejects tokens whose `iss` is not `https://securetoken.google.com/<project-id>` or whose
  `aud` is not the project id. Tokens from any other Firebase project are refused **before** the
  database is involved.
* Supabase Auth cannot be disabled, but RushIt never calls it. It is simply unused.

### 3.2 Give every user the `authenticated` role claim (required)

Supabase decides the Postgres role from the JWT's `role` claim. A Firebase token has no such claim, so
without this step every request runs as `anon` and **every policy denies it**. That is the single most
common cause of "signed in but everything is empty".

Pick one, and do it for both new **and** existing users:

**Option A — blocking functions (recommended for new users)**

```ts
// functions/src/index.ts, deployed with the Firebase CLI
import { beforeUserCreated, beforeUserSignedIn } from "firebase-functions/v2/identity";

export const beforecreated = beforeUserCreated((event) => ({
  customClaims: { role: "authenticated" },
}));

export const beforesignedin = beforeUserSignedIn((event) => ({
  customClaims: { role: "authenticated" },
}));
```

**Option B — Admin SDK for users that already exist**

```ts
// Run once, server-side, with a service-account key. NEVER ship this key in the Android app.
import { initializeApp, cert } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";

const app = initializeApp({ credential: cert(process.env.GOOGLE_APPLICATION_CREDENTIALS!) });
const auth = getAuth(app);

for await (const page of /* listUsers pagination */ []) {
  for (const user of page.users) {
    await auth.setCustomUserClaims(user.uid, { role: "authenticated" });
  }
}
```

The claim only appears in a **newly minted** token. RushIt forces one refresh immediately after
sign-in (`installToken(forceRefresh = true)`) precisely for this, and a user who was signed in before
the claim existed is fixed automatically on their next sign-in.

### 3.3 Apply the database migration

```sh
supabase link --project-ref uootcypdodfyqfigracp
supabase db push
```

`supabase/migrations/20261101090000_firebase_identity.sql` is forward-only and non-destructive:

* adds `profiles.firebase_uid text` with a unique index (one profile per Firebase account);
* gives `profiles.id` a `gen_random_uuid()` default so the client never chooses an id;
* drops the `auth.users` foreign keys and points the child tables at `public.profiles(id)`;
* rewrites every RLS policy to `(select auth.jwt() ->> 'sub')` against `firebase_uid`;
* adds `current_firebase_uid()`, `current_profile_id()`, `admin_roles`, `is_admin()`, and a safe
  replacement for `delete_user_account()`;
* keeps every existing row, with `firebase_uid = NULL` for legacy Supabase-Auth rows.

Verify afterwards (the queries are printed at the end of the migration file):

```sql
select count(*) filter (where firebase_uid is null) as legacy,
       count(*) filter (where firebase_uid is not null) as linked
  from public.profiles;

select tablename, policyname from pg_policies
 where schemaname = 'public'
   and coalesce(qual, '') || coalesce(with_check, '') like '%auth.uid()%';   -- expect zero rows

select relname, relrowsecurity from pg_class
 where relname in ('profiles','user_settings','user_devices','user_activity_logs','admin_roles');
```

### 3.4 Administrator authorization

Administrators are rows, never claims the client can mint:

```sql
-- Run in the SQL editor / with a service role. There is deliberately no client-writable policy.
insert into public.admin_roles (firebase_uid, role, granted_by)
values ('<verified firebase uid of the administrator>', 'admin', 'setup')
on conflict (firebase_uid) do update set role = excluded.role;
```

`public.is_admin()` returns true for a caller whose **verified** `sub` is listed, and it is the only
thing an admin policy consults. `admin_roles` has RLS enabled, no policies and no grants to `anon` or
`authenticated`, so no client can read or write it.

### 3.5 Optional: the account-deletion Edge Function

The app deletes the Firebase account and its own profile row without any server help. The function is
only a fallback for "Firebase account gone, profile purge failed":

```sh
supabase secrets set SUPABASE_SERVICE_ROLE_KEY=<service-role key>
supabase secrets set FIREBASE_PROJECT_ID=<firebase project id>
supabase functions deploy delete-account
```

It verifies the caller's Firebase ID token against Google's JWKS (`iss` and `aud` pinned) and deletes
rows by the verified `sub`. It never deletes the Firebase user (that needs the Admin SDK and must
happen while the person is present to reauthenticate).

---

## 4. Build configuration

`app/build.gradle.kts` reads these from a Gradle property, the ignored `local.properties`, or the
environment. Public values only.

```properties
# local.properties (git-ignored)
SUPABASE_URL=https://uootcypdodfyqfigracp.supabase.co
SUPABASE_ANON_KEY=YOUR_PUBLIC_ANON_OR_PUBLISHABLE_KEY
GOOGLE_WEB_CLIENT_ID=000000000000-xxxxxxxx.apps.googleusercontent.com
RUSHIT_TERMS_URL=https://YOUR_REAL_TERMS_URL
RUSHIT_PRIVACY_URL=https://YOUR_REAL_PRIVACY_URL
```

Plus `app/google-services.json` from step 2.1.

CI supplies the same values as repository **variables**, including `GOOGLE_SERVICES_JSON` (the raw
file contents), which the workflow writes to `app/google-services.json`.

The build **fails** if `GOOGLE_CLIENT_SECRET` is supplied, if `GOOGLE_WEB_CLIENT_ID` is not a Web
client id, if `SUPABASE_URL` is not a bare HTTPS origin, or if `SUPABASE_ANON_KEY` is anything other
than a publishable key or a legacy `role: anon` JWT.

**Never** place the following in the APK, the repository, Gradle files or CI variables that reach the
build: a Google OAuth client secret, a Firebase Admin service-account key, a Supabase service-role key
or `sb_secret_…` key. The app authenticates with Firebase and authorizes with RLS; it needs none of
them.

---

## 5. Verification checklist

Run these in order; each one has an observable outcome.

1. `./gradlew :app:testDebugUnitTest` — the auth unit tests (repository, ViewModel, phone rules,
   Google transport and single-flight, error mapping, startup routing) pass without a device, network
   or Firebase project.
2. `./gradlew :app:assembleDebug` then `apksigner verify --min-sdk-version 24 app-debug.apk`.
3. Install and tap **Continue with Google**: the real account picker appears; cancelling shows no error;
   a first-time account lands on onboarding with the Google name/photo pre-filled.
4. Kill and relaunch: the app goes straight to Home without showing the login screen first.
5. Sign out: back to Login, and the back button does not return to Home.
6. Open the phone screen and use a **Firebase test number**: the code field appears, a wrong code
   keeps it open with a readable message, resend is disabled for 30 seconds, the right code signs in.
7. Profile edit: change the name/username/photo; the values survive a restart; a duplicate username is
   rejected with a readable message.
8. Account deletion: the account is removed, the app returns to Login, and a second sign-in with the
   same Google account creates a fresh profile.
9. In Supabase, confirm exactly one `profiles` row per Firebase UID and that a request with another
   user's token sees nothing:

   ```sh
   curl -s "$SUPABASE_URL/rest/v1/profiles?select=id,firebase_uid" \
        -H "apikey: $SUPABASE_ANON_KEY" -H "Authorization: Bearer $FIREBASE_ID_TOKEN"
   ```

   The response must contain only the caller's row. `[]` means the role claim is missing (step 3.2).

---

## 6. Troubleshooting

| Symptom | Cause | Fix |
| --- | --- | --- |
| "Sign-in is not configured in this build" | No `google-services.json` at `app/google-services.json` | Add the file, rebuild |
| Account picker never appears / `GetCredentialProviderConfigurationException` | Missing SHA-1 for this signing key, or Google provider disabled | Add the fingerprint; enable the Google provider |
| Google sign-in returns to the app but Firebase rejects it | `GOOGLE_WEB_CLIENT_ID` is the Android client id, or belongs to another Firebase project | Use the **Web** client id of the same Firebase project |
| `ERROR_OPERATION_NOT_ALLOWED` | Phone or Google provider not enabled in Firebase | Enable it in Authentication → Sign-in method |
| `ERROR_APP_NOT_AUTHORIZED` / `ERROR_INVALID_APP_CREDENTIAL` | App verification failed (Play Integrity/reCAPTCHA) | Register App Check and the SHA-256; use test numbers |
| `ERROR_TOO_MANY_REQUESTS` / `ERROR_QUOTA_EXCEEDED` | SMS quota/rate limit | Wait, or use Firebase test numbers |
| Signed in, but every table read is empty and no error appears | The Firebase token has no `role: "authenticated"` claim, so PostgREST runs as `anon` | Step 3.2, then sign in again |
| PostgREST returns 401/`PGRST301` with a valid token | Supabase project does not trust this Firebase project | Step 3.1, check `project_id` |
| Profile row created but only readable by nobody | `firebase_uid` was written for a different Firebase project's uid | Check the token's `iss`/`aud`; delete the stray row and sign in again |
| Delete account says "please sign in again" | Firebase requires a recent login for deletion | Sign in again, then delete |

---

## 7. Rolling back

The Android changes cannot be rolled back halfway: the old Supabase-Auth classes were deleted. The
database, however, is left fully intact by the migration (no tables, rows or content columns are
dropped), so an application rollback to an older APK would still find its `auth.users` identities and
`profiles` rows. The only additive changes are `firebase_uid`, the unique index, the helper functions,
`admin_roles`, the rewritten policies and the child-table foreign keys — nothing needs to be undone to
run the previous build, although its policies would then be the new (Firebase-based) ones.

## 8. What is deliberately not automated

* Creating the Firebase project, enabling providers, adding SHA fingerprints, and downloading
  `google-services.json` require console access.
* Registering the Firebase project in Supabase and assigning `role: "authenticated"` require console
  or server-side access.
* Applying the migration to the live project requires explicit authorization; review it first.
* Linking a **legacy** Supabase-Auth profile to a Firebase account is a manual, audited `update`:

  ```sql
  update public.profiles
     set firebase_uid = '<verified firebase uid>'
   where id = '<legacy supabase uuid>'
     and firebase_uid is null;
  ```

  Do this only after verifying out of band that both identities belong to the same person. RushIt never
  merges accounts on a matching email, phone number or name.
