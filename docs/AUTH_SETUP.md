# RushIt — Google Sign-In and Supabase account setup

This is the manual configuration that RushIt cannot do for itself. The app-side implementation is
complete and tested; **none of the external Google Cloud or Supabase console steps below have been
performed or verified**, because no credentials or projects were supplied for this work. Until they
are done, the app reports its own configuration state honestly instead of pretending to sign anybody
in.

What happens today with no configuration:

| Missing value | Behaviour |
| --- | --- |
| `GOOGLE_WEB_CLIENT_ID` | Tapping **Continue with Google** shows "Google Sign-In is not configured in this build…". No request is made and nobody is signed in. |
| `SUPABASE_URL` / `SUPABASE_ANON_KEY` | Start-up lands on the login screen with "RushIt is not connected to its account service in this build…". The Supabase client is never used. |
| Google provider not enabled in Supabase | Credential Manager succeeds, then Supabase rejects the token; the app shows a Google-provider configuration message. |
| Migrations not applied | Profile provisioning fails and the error is surfaced; nothing is silently half-created. |
| `delete-account` function not deployed | **Delete account** reports that deletion is unavailable. No data is removed and the user stays signed in. |

---

## 1. Google Cloud Console

Project: create one or reuse an existing project at <https://console.cloud.google.com>.

### 1.1 OAuth consent screen

**APIs & Services → OAuth consent screen**

- User type: **External** (unless the organisation uses Google Workspace and wants Internal).
- App name: `RushIt`. Support email: a real, monitored address.
- Authorized domains: your own domain (for example `rushit.example`). The consent screen requires a
  domain you control; a `supabase.co` subdomain is not appropriate here.
- Scopes: `openid`, `.../auth/userinfo.email`, `.../auth/userinfo.profile`. RushIt requests no other
  scope, and never asks for a Google password.
- Privacy policy and Terms of Service links: the same real URLs used for `RUSHIT_TERMS_URL` and
  `RUSHIT_PRIVACY_URL`.

**Publishing status**

- While the status is **Testing**, only accounts listed under *Test users* (100 maximum) can sign in.
  Anyone else sees `access_denied`. Add every tester's Google account here first.
- Moving to **In production** removes the test-user restriction. For an External consent screen,
  Google may show an "unverified app" warning until the app passes Google's OAuth verification, which
  requires the privacy policy, a home page on the authorized domain, and (for sensitive scopes) a
  review. RushIt requests only non-sensitive scopes, so verification is the lighter path.

### 1.2 Web application OAuth client — this is the ID token audience

**APIs & Services → Credentials → Create credentials → OAuth client ID**

- Application type: **Web application**.
- Name: `RushIt Web (Supabase audience)`.
- Authorized JavaScript origins / redirect URIs: **not required** for this flow. RushIt uses the
  Credential Manager ID-token flow, not a browser redirect, so Supabase never sends the user to
  Google itself.
- Copy the resulting **Client ID** — it becomes `GOOGLE_WEB_CLIENT_ID` in the app build *and* the
  client ID in the Supabase Google provider. Both must be the same value, because Supabase verifies
  that the token's `aud` claim matches a client ID it was configured with.
- The **client secret** for this client belongs in Supabase only. It must never be placed in the
  repository, in `local.properties`, or in the APK. `app/build.gradle.kts` fails the build if a
  `GOOGLE_CLIENT_SECRET` property is supplied.

### 1.3 Android OAuth client — lets Google recognise the app

**Create credentials → OAuth client ID**

- Application type: **Android**.
- Name: `RushIt Android`.
- Package name: `com.rushit.app` — it must match `applicationId` in `app/build.gradle.kts` exactly.
- SHA-1 certificate fingerprint: add one entry per signing identity that will run the app.

Get the debug SHA-1:

```sh
./gradlew :app:signingReport
# or directly:
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android
```

For release builds add the SHA-1 of the release keystore, **and** the SHA-1 of the *Play App Signing*
key if the app is distributed through Google Play — Play re-signs the APK, so the upload key's
fingerprint alone is not enough. Both appear under *Play Console → Setup → App integrity*.

The Android client ID itself is never referenced in RushIt's code: Credential Manager matches the
app by package name plus signing fingerprint. Adding it to the Supabase provider's *Authorized Client
IDs* is optional and only matters if a future flow requests a token audience other than the Web
client.

---

## 2. Supabase

### 2.1 Enable the Google provider

**Authentication → Providers → Google**

- Enable the provider.
- **Client ID**: the Web application client ID from §1.2.
- **Client Secret**: the matching secret from §1.2. This is the only place the secret lives.
- Leave *Authorized Client IDs* empty unless a non-Web audience is added later.
- **Authentication → URL Configuration**: Site URL and Redirect URLs are not used by the ID-token
  flow. No OAuth callback URL is required, because no browser redirect occurs. (If a browser-based
  flow is ever added, the callback is
  `https://YOUR_PROJECT.supabase.co/auth/v1/callback` and it must be registered as a redirect URI on
  a Web client.)
- **Authentication → Settings**: keep *Allow new users to sign up* enabled; RushIt creates an account
  on first Google sign-in. Consider enabling *Secure email change* and rate limits appropriate for
  production.

### 2.2 Apply the database migrations

Two migration files exist, in order:

1. `supabase/migrations/20261031_rushit_auth.sql` — the original schema.
2. `supabase/migrations/20261031120000_google_auth_accounts.sql` — reconciles that schema to the
   account model RushIt now uses, and replaces its over-permissive policies.

The second migration is idempotent and forward-only. It does not delete user-authored data; the only
columns it drops are ones RushIt deliberately stops collecting (`user_devices.device_id`,
`user_devices.fcm_token`, `user_activity_logs.ip_address`, `user_activity_logs.user_agent`).

```sh
supabase login
supabase link --project-ref YOUR_PROJECT_REF
supabase db push          # applies both migrations in order
```

Or paste `20261031120000_google_auth_accounts.sql` into the Supabase SQL editor. **Review it against
your production data before running it there** — in particular, creating the case-insensitive unique
index on `lower(username)` fails loudly if two existing rows differ only by letter case, and the
migration stops rather than choosing for you.

What the migration establishes:

- `profiles`, `user_settings`, `user_devices`, `user_activity_logs`, all owned by
  `auth.users.id` with `ON DELETE CASCADE`.
- RLS enabled on all four, with `USING` **and** `WITH CHECK` on every UPDATE policy, and no policy at
  all for `anon`.
- Column-level grants so a client cannot write `profiles.id` or `profiles.phone_verified` even with a
  valid row policy.
- `handle_new_user()` — a `SECURITY DEFINER` trigger on `auth.users` that provisions one profile and
  one settings row with `ON CONFLICT DO NOTHING`, so concurrent sign-ins cannot duplicate a row.
- `username_available(text)` — the only cross-account read, returning a boolean and nothing else.
- `delete_user_account()` — the fallback deletion path, authorized by `auth.uid()`.

### 2.3 Deploy account deletion

Preferred path — an Edge Function that verifies the caller's own JWT and uses the Admin API:

```sh
supabase secrets set SUPABASE_SERVICE_ROLE_KEY=<service-role key>
supabase functions deploy delete-account
```

Source: `supabase/functions/delete-account/index.ts`. Keep JWT verification **enabled** (the default).
The service-role key exists only in the function's environment; the app never sees it, and
`app/build.gradle.kts` rejects any recognizable service-role or `sb_secret_` key at build time.

If the function is not deployed, the app falls back to the `delete_user_account()` RPC created by the
migration. If neither exists, deletion reports that it is unavailable and changes nothing.

### 2.4 Verify the RLS policies

```sh
supabase start
supabase db reset
supabase test db        # runs supabase/tests/rls_isolation.sql
```

`supabase/tests/rls_isolation.sql` asserts that User A cannot read or modify User B's rows, that
`anon` reads nothing, that `phone_verified` cannot be self-set, and that username availability works
across accounts. These tests need the Supabase CLI and Docker; they are **not** part of the Gradle
build and were **not** executed for this change.

---

## 3. Build configuration

No credential is committed. Put them in the **ignored** root `local.properties`, or supply them as
Gradle properties or environment variables:

```properties
SUPABASE_URL=https://YOUR_PROJECT.supabase.co
SUPABASE_ANON_KEY=YOUR_PUBLIC_ANON_OR_PUBLISHABLE_KEY
GOOGLE_WEB_CLIENT_ID=000000000000-xxxxxxxx.apps.googleusercontent.com

# Optional, real HTTPS destinations owned by your organization:
RUSHIT_TERMS_URL=https://YOUR_REAL_TERMS_URL
RUSHIT_PRIVACY_URL=https://YOUR_REAL_PRIVACY_URL
```

These become `BuildConfig` fields and are embedded in the APK, so they are **public by design**. Only
a publishable/anon key belongs here. There is no `GOOGLE_CLIENT_SECRET` field, and adding one fails
the build.

For CI, set the same names as repository **Variables** (not Secrets — they are public values):
`SUPABASE_URL`, `SUPABASE_ANON_KEY`, `GOOGLE_WEB_CLIENT_ID`, `RUSHIT_TERMS_URL`,
`RUSHIT_PRIVACY_URL`. The `RushIt User Build` workflow passes them to
`:app:testDebugUnitTest` and `:app:assembleDebug`.

A build with blank values still compiles and still runs its unit tests; at runtime the app reports
that Google Sign-In or the account service is not configured.

---

## 4. End-to-end check on a device

This is the check that must be performed by someone with the credentials above. It has **not** been
run for this change.

1. Install a debug build made with all five values set, on a device whose Google account is in the
   consent screen's test-user list.
2. Tap **Continue with Google** → the account picker opens → choose an account.
3. First run: onboarding appears with the Google name and photo pre-filled. Enter a display name,
   optionally a username, and save. Home opens.
4. Kill the app and relaunch: it goes straight to Home with no login screen flash.
5. Account → Edit profile: change the display name, save, kill and relaunch. The change persists.
6. Sign in with a second Google account on another device: separate profile, no shared data.
7. Account → Sign out → the login screen appears and the back button cannot return to Home.
8. Sign in again with the first account: the customized name and avatar are still there.
9. Account → Delete account → type `DELETE` → confirm. The user returns to login, and in the Supabase
   dashboard the row is gone from `auth.users`, `profiles` and `user_settings`.
10. Turn on airplane mode and relaunch: the app explains that the session could not be confirmed and
    offers **Retry**; it does not sign the user out.

---

## 5. What RushIt keeps out of the APK

- No Google OAuth **client secret**. Credential Manager only needs the public Web client ID.
- No Supabase **service-role** or `sb_secret_` key. The build fails if one is supplied.
- No hard-coded credential of any kind; all five values come from the build environment.
- Refresh and access tokens are serialized by the Supabase SDK and stored through
  `SecureSessionStore`, encrypted with a non-exportable Android Keystore AES/GCM key.
  `android:allowBackup` is `false`, so the ciphertext is never copied to a device that lacks the key.
- Tokens are never written to logs, to `user_activity_logs`, or to any user-facing message.
  `RestException.message` is deliberately never shown: supabase-kt builds it from the request URL and
  headers, which include the anon key and the bearer token.

## 6. Future phone authentication

Phone sign-in is intentionally absent. The schema already carries `profiles.phone_number` (unique,
nullable) and `profiles.phone_verified`, which no client can set — the column has no `UPDATE` grant.
`AuthProviderId` and `AuthGateway.signInWithIdToken(provider, …)` are provider-parameterized, and
account ownership is always `auth.users.id`, so a verified phone identity can later be linked to an
existing account through Supabase identities without creating a second profile. Nothing merges
accounts on the basis of a matching email or user-supplied metadata.
