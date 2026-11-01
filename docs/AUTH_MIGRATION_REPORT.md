# RushIt — Firebase Authentication migration report

Date: 2026-11-01
Branch: `arena/ec01dd8c-rushit`
Repository: `devjeetx7-synfusion/Rushit` (Android app package `com.rushit.app`)
Supabase project: `https://uootcypdodfyqfigracp.supabase.co`

---

## 1. Executive summary and honest status

The Android app no longer has any Supabase Auth code path. Identity is **Firebase Authentication**:
Google sign-in through Jetpack Credential Manager and phone sign-in through Firebase's SMS
verification. The **Firebase UID** is the canonical identity, stored as `profiles.firebase_uid`, and
Supabase authorizes every request by verifying the Firebase ID token through its supported
**Third-Party Auth → Firebase** integration. Supabase keeps PostgreSQL, Storage, Edge Functions and all
business data; its `gotrue-kt` client module remains installed only because it is the sole way this SDK
version can attach an externally issued bearer token to PostgREST requests.

| Area | State |
| --- | --- |
| Application code | **Written and reviewed.** Compilation not executed here (see §12). |
| Database migration | **Written**, forward-only, non-destructive. **Not applied** to any database. |
| Firebase console configuration | **Not performed** — requires console access; exact steps in §4. |
| Supabase Third-Party Auth configuration | **Not performed** — requires dashboard/CLI access; §8. |
| Unit tests | **Rewritten/added**; **not executed** (no JDK/Android SDK/Gradle in this environment). |
| CI workflow | **Updated** to assert Firebase classes present and browser OAuth absent. |
| Device sign-in (Google, phone) | **Not performed.** No Firebase project, Google OAuth client or device was available. |

Nothing below is claimed as verified unless a command was actually run. §12 lists exactly what was and
was not executed.

---

## 2. Original state (audit)

What existed before the migration:

* `SupabaseAuthGateway` / `SupabaseAuthRepository` drove everything: `awaitInitialization()`,
  `sessionStatus`, `signInWith(IDToken)`, `currentSessionOrNull()`, `refreshCurrentSession()`,
  `signOut(LOCAL)`, `clearSession()`, `retrieveUserForCurrentSession()`, `getOAuthUrl()`,
  `exchangeCodeForSession()` and a PKCE `codeVerifierCache`.
* Sessions were stored in Keystore-encrypted preferences (`EncryptedSessionManager`,
  `SecureSessionStore`) and refreshed by the Supabase SDK.
* Google sign-in used Credential Manager for the native path and **Supabase's PKCE browser flow in a
  Custom Tab** as a fallback, returning through the `rushit://auth/callback` deep link, with
  `SignInNonce`, `GoogleIdToken` and `Base64Url` validation helpers.
* Phone authentication did not exist.
* The database keyed `public.profiles.id` directly to `auth.users.id`, and every policy used
  `auth.uid()`.
* No `localhost:3000`, no `redirect_uri` in the tree; the only redirect was the app deep link.
* The `:admin` module has no Supabase or Firebase dependency and no authentication at all, so there was
  no admin auth to migrate — only admin *authorization* in the database was missing.

Detailed audit and the A–J plan: `docs/AUTH_MIGRATION_AUDIT.md`.

---

## 3. Target architecture

```
ui/                     Compose screens; never touch Firebase, Supabase or Android SDK types
  viewmodel/AuthViewModel      AuthUiState / PhoneLoginUiState / AuthEvent; every action is single-flight
data/auth/
  AuthRepository               interface + AuthState/AuthResult/AuthFailure + all orchestration
  FirebaseAuthRepository       the only implementation; one Mutex; token install/clear invariants
  AuthGateway                  Firebase transport boundary (no SDK types in its signature)
  firebase/FirebaseAuthGateway FirebaseAuth, GoogleAuthProvider, PhoneAuthProvider
  firebase/FirebaseAuthErrors  error code → safe message (pure, unit-tested without the SDK)
  ProfileGateway               Supabase business data boundary, addressed by Firebase UID
  SupabaseProfileGateway       PostgREST: profiles, user_settings, user_activity_logs, username_available
  supabase/SupabaseIdentityBridge  hands the Firebase ID token to the Supabase client
  google/                      Credential Manager, request spec, failure classification, single-flight
  phone/PhoneNumber.kt         E.164 normalization for 38 country codes
```

Layering rules that are enforced by construction:

* No `com.google.firebase.*`, `io.github.jan.supabase.*`, `androidx.credentials.*` or `android.*` type
  appears in `AuthRepository`, `AuthState`, `AuthResult`, `AuthGateway`, `ProfileGateway`, the
  ViewModel or the screens. Everything below is testable on the JVM.
* The UI cannot start a second concurrent sign-in: the flag is set synchronously before the coroutine
  is launched, and an application-scoped `Mutex.tryLock()` guards the Google request across Activity
  recreation.
* OTP-related secrets (verification id, resend token) live only in the gateway's in-memory map, never
  in UI state, saved-instance state, preferences or logs.

---

## 4. Firebase console configuration (manual — exact steps)

Full walkthrough in `docs/AUTH_SETUP.md` §2. Summary:

1. Create/select the Firebase project; add an **Android** app with package `com.rushit.app` and a SHA-1
   for every signing identity (debug, CI test key, Play App Signing).
2. Download **`google-services.json`** → `app/google-services.json` (git-ignored). Gradle applies the
   `google-services` plugin only when the file exists and sets `BuildConfig.FIREBASE_CONFIGURED`.
3. **Authentication → Sign-in method:** enable **Google** (note the **Web** client ID →
   `GOOGLE_WEB_CLIENT_ID`) and **Phone**.
4. Add **test phone numbers** with fixed codes for all automated/repeated testing; never spend real SMS
   in tests.
5. For production phone verification, register the release SHA-256 and App Check (Play Integrity);
   reCAPTCHA is the automatic fallback.
6. Ensure the project's SMS quota/billing allows verification; `ERROR_QUOTA_EXCEEDED` and
   `ERROR_BILLING_NOT_ENABLED` are surfaced as readable messages, not silent failures.

**Not done here:** none of the above can be performed without Firebase console access, so no Firebase
project is known to exist for this app.

---

## 5. Removed Supabase Auth (what, where, why it is safe)

Deleted only after verifying no other code depended on them:

| Deleted | Also served | Verified |
| --- | --- | --- |
| `data/auth/SupabaseAuthGateway.kt` | Supabase Auth transport | no remaining reference (grep) |
| `data/auth/SupabaseAuthRepository.kt` | orchestration | replaced by `FirebaseAuthRepository`; profile logic preserved there |
| `data/auth/SecureSessionStore.kt`, `EncryptedSessionManager.kt` | Supabase session storage | Firebase persists its own session; the app no longer stores tokens |
| `data/auth/google/SupabaseBrowserSignIn.kt`, `SupabaseOAuthRedirect.kt`, `SignInNonce.kt`, `GoogleIdToken.kt`, `Base64Url.kt` | browser fallback, nonce, token pre-validation | Firebase verifies the token instead; native path kept |
| tests `EncryptedSessionManagerTest`, `GoogleIdTokenTest`, `SupabaseOAuthRedirectTest`, `SupabaseAuthRepositoryTest` | — | replaced by `FirebaseAuthRepositoryTest`, `FirebaseAuthErrorsTest`, `PhoneNumbersTest`, rewritten ViewModel/coordinator/request tests |

Kept, because they also serve profile data, authorization rules, error text or navigation:
`ProfileOnboarding`, `UsernameRules`, `FullNameRules`, `AuthErrors`, `phone/PhoneNumber`,
`AuthProviderId`, and every UI screen except the new phone screen.

Kept, **not** for Supabase Auth: `gotrue-kt`. supabase-kt 2.5.0 reads its bearer token only from its own
`Auth` plugin session and has no per-request header override, so `SupabaseIdentityBridge` imports the
Firebase ID token with `importSession(session, autoRefresh = false)` (empty refresh token, no storage,
no refresh job, no lifecycle callbacks). Verified against the 2.5.0 sources. No Supabase Auth endpoint
is ever called: no sign-in, sign-up, refresh, OAuth or password flow exists in the app.

---

## 6. Google Sign-In

* Jetpack **Credential Manager** only. No deprecated `GoogleSignInClient`, no browser, no Custom Tab,
  no nonce (Firebase verifies signature, audience and expiry server-side).
* `setServerClientId(GOOGLE_WEB_CLIENT_ID)` — the **Web** OAuth client ID, which is the ID-token
  audience Firebase expects. The Android client is matched by package name + signing fingerprint.
* Two stages: authorized accounts first (`setFilterByAuthorizedAccounts(true)`), then the unrestricted
  picker on `NoCredentialException`. That exception is explicitly **not** reported as "no Google
  account on this device" (a naive implementation does exactly that on first sign-in).
* The foreground Activity is re-resolved for each stage and re-checked after the response, so a
  configuration change or a destroyed host cannot receive a late credential.
* Application-scoped single-flight (`GoogleSignInCoordinator` with `Mutex.tryLock()`): a second tap
  returns `ALREADY_SIGNING_IN` instead of opening a second picker; Activity recreation neither
  duplicates a request nor leaves a stale flag.
* Outcomes: `Success(idToken)`, `Cancelled` (silent), `Unconfigured`, `NoCredentialAvailable`,
  `Unsupported`, `Failed(message, offline)`. No path turns a failure into a success.
* The Google ID token goes to `FirebaseAuth.signInWithCredential(GoogleAuthProvider.getCredential(...))`
  and **nowhere else** — never to Supabase.

---

## 7. Phone OTP

* `PhoneAuthProvider.verifyPhoneNumber(PhoneAuthOptions)` with the current Activity, a 60-second
  timeout, and a 65-second coroutine bound so a hung SDK call cannot wedge the UI.
* Number validated and normalized to E.164 locally (`PhoneNumbers`, 38 countries; national trunk zero
  dropped, pasted `+91`/`91` not doubled, too-short/too-long rejected with a specific message).
  Firebase rejects anything else with `ERROR_INVALID_PHONE_NUMBER`.
* `onCodeSent` stores the verification id and resend token in the gateway; the UI only ever receives an
  opaque `PhoneChallenge(id)`.
* A wrong code keeps the challenge, so a typo does not need a new SMS; a blank code is rejected locally.
* Re-sends are throttled twice: the ViewModel runs a 30-second countdown, and the repository enforces
  the interval against an injected clock. A second request while a code is pending never sends a
  second SMS.
* Instant verification / SMS auto-retrieval (`onVerificationCompleted`) signs in without a code and is
  reported as `AutoVerified`.
* No `RECEIVE_SMS`/`READ_SMS` permission is declared: the app never reads SMS itself.
* New `PhoneLoginScreen` reached from the existing login screen (`onPhoneSignIn`), with a working back
  path, country picker, code step and resend countdown. The existing login UI is otherwise preserved.

---

## 8. Firebase ↔ Supabase identity, RLS and admin authorization

**Chosen mechanism: Supabase's supported Third-Party Auth integration for Firebase.** It is a
first-class feature (not a custom token exchange): Supabase verifies the Firebase JWT before PostgREST
runs, and the token is passed as the `Authorization: Bearer` value.

* Configured in Dashboard → Authentication → Third-Party Auth → Firebase (**Project ID**), or
  `[auth.third_party.firebase]` in `supabase/config.toml`.
* Claims relied upon: `sub` = Firebase UID, `iss = https://securetoken.google.com/<project-id>`,
  `aud = <project-id>`. Tokens from other Firebase projects are rejected before the database.
* **`role: "authenticated"` is required** on every Firebase user, otherwise PostgREST runs as `anon`
  and every policy denies. Documented in §4 of `docs/AUTH_SETUP.md` with the blocking-function and
  Admin-SDK recipes. The app forces one token refresh immediately after sign-in to pick the claim up.
* `auth.uid()` casts the subject to `uuid`, which is wrong for Firebase UIDs, so every policy uses
  `(select auth.jwt() ->> 'sub')` compared with a **text** column.
* No service-role key and no Firebase Admin key exist in the app; no client-supplied UID is ever
  trusted — the only identity the database sees is the verified `sub`.

**Database authorization** (`supabase/migrations/20261101090000_firebase_identity.sql`):

* `profiles.firebase_uid text` + unique partial index → one profile per Firebase account.
* `profiles` policies: select/insert/update/delete restricted to `firebase_uid = current_firebase_uid()`,
  with both `USING` and `WITH CHECK` on update so ownership cannot be reassigned.
* `user_settings`, `user_devices`, `user_activity_logs`: ownership via `current_profile_id()`
  (SECURITY DEFINER lookup), activity stays append-only for clients.
* No policy for `anon` anywhere; `anon` also loses all table privileges.
* Column-level grants: a client may insert `(firebase_uid, full_name, avatar_url)` and update
  `(full_name, username, avatar_url, onboarding_completed)` — never `id`, never `phone_verified`.
* **Admin authorization**: `public.admin_roles(firebase_uid, role, granted_at, granted_by)` with RLS
  enabled, no policies and no grants to API roles, plus `public.is_admin()` (SECURITY DEFINER) that
  reads the caller's verified `sub`. Admins get a read-only policy on `profiles`; there is no admin
  write path from a client.
* `delete_user_account()` was rewritten: it previously deleted the `auth.users` row; it now deletes the
  caller's own profile row and cascades, authorized by the verified `sub`.

**Fallback (documented, not required):** if Third-Party Auth is unavailable, the `delete-account` Edge
Function verifies the Firebase ID token against Google's JWKS (`iss`/`aud` pinned) and purges by
Firebase UID with a service-role key that exists only in the function runtime.

---

## 9. Profiles, account linking, logout, session and navigation

* Provisioning is addressed by Firebase UID and is idempotent:
  `upsert(..., onConflict = "firebase_uid", ignoreDuplicates = true)` then a read-back, so concurrent
  sign-ins cannot duplicate rows and a returning user's saved name/avatar is never overwritten by the
  provider's copy. Provider data is only ever a *suggestion* for an empty field.
* A first sign-in lands on `ProfileIncomplete` → onboarding; a completed profile goes straight to Home.
* Account linking is a **Firebase** decision (link with reauthentication). The app never merges
  profiles on a matching email, phone number or name. Legacy Supabase-Auth rows stay with
  `firebase_uid = NULL` and are reachable only after a deliberate, audited `update` by an operator.
* Session restore waits for Firebase's first auth-state callback (5 s bound), so a cold start cannot be
  mistaken for signed-out and the login screen never flashes for a signed-in user.
* A transient failure while refreshing an already signed-in user keeps them signed in and offers
  **Retry**; only a definitively absent session signs them out.
* Sign-out records a minimal activity event, discards any pending phone challenge, calls Firebase, and
  **unconditionally clears the Supabase bearer token in a `finally`** — a failed Firebase sign-out can
  never leave the previous user's token installed. Navigation resets to Login, so Back cannot return to
  Home.
* Account deletion: fresh token → `FirebaseUser.delete()` → purge profile row (cascade) → clear token.
  A stale sign-in (`ERROR_REQUIRES_RECENT_LOGIN`) leaves everything intact and asks the person to sign
  in again; a purge failure *after* the Firebase account is gone is reported as a partial deletion
  instead of claiming success.
* Home/onboarding/login navigation is derived from `AuthState` in one place (`routeFor`), published as
  one-shot `AuthEvent`s, with the splash owning the first route so it is never duplicated.

---

## 10. Database migration (non-destructive)

`supabase/migrations/20261101090000_firebase_identity.sql`:

* **No** table, row, or content column is dropped; no RLS is disabled; no data is rewritten.
* `profiles.firebase_uid` added, unique partial index, blank-value check, and a `BEFORE INSERT` trigger
  that refuses a new row without a UID (legacy rows are untouched).
* `profiles.id` gets `gen_random_uuid()`; the `auth.users` FK is dropped; child tables are re-pointed at
  `public.profiles(id) on delete cascade` — added `NOT VALID` and validated only when no orphan rows
  exist, so the migration never deletes anything to make itself pass.
* All policies rebuilt on `(select auth.jwt() ->> 'sub')`; helper functions `current_firebase_uid()`,
  `current_profile_id()`, `is_admin()`, `require_profile_identity()`; `admin_roles` added.
* The old `handle_new_user()` trigger on `auth.users` is retired (Firebase creates users now),
  `delete_user_account()` is replaced, and `username_available(text)` keeps its behaviour and grants.
* No `BEGIN`/`COMMIT`, no `CONCURRENTLY` requirements, idempotent, and it ends with five verification
  queries plus a commented, explicitly-manual linking statement.
* **Not applied.** Applying it to the live project needs explicit authorization and a backup;
  `supabase db push` is the documented command.

---

## 11. Build, manifest, dependencies and CI

* `build.gradle.kts`: `com.google.gms.google-services` **4.4.2** declared `apply false`.
* `app/build.gradle.kts`: applies that plugin **only when `app/google-services.json` exists**, emits
  `buildConfigField("boolean", "FIREBASE_CONFIGURED", …)`, adds `firebase-bom:33.7.0`, `firebase-auth`
  and `kotlinx-coroutines-play-services:1.8.1`, keeps `gotrue-kt`/`postgrest-kt`, and removes
  `functions-kt`, `androidx.browser` and the `org.json` test dependency.
  Version choice is recorded in `docs/AUTH_MIGRATION_AUDIT.md` §11.1: 33.7.0/4.4.2 are the newest
  releases compatible with `compileSdk 34` + AGP 8.5.2; the newer 35.x/4.5.0 would force an unrelated
  toolchain upgrade. The existing guards that reject a client secret, a malformed URL or a
  service-role/secret Supabase key are unchanged.
* `AndroidManifest.xml`: the `rushit://auth/callback` intent filter and the Custom Tabs `<queries>`
  block are removed; permissions remain `INTERNET` and `ACCESS_COARSE_LOCATION` only; `MainActivity`
  keeps `singleTask` and no longer consumes a sign-in Intent.
* `.gitignore`: `google-services.json` and Firebase Admin service-account JSON are ignored (keystore
  patterns unchanged).
* CI (`rushit-user-build.yml`): optionally materialises `google-services.json` from the
  `GOOGLE_SERVICES_JSON` variable (validating it as JSON, never printing it), warns and builds without
  it otherwise; asserts the **absence** of the `rushit://` scheme and of `SupabaseOAuthRedirect` /
  `SupabaseBrowserSignIn` / `CustomTabsIntent`, and the **presence** of Credential Manager, Firebase
  Authentication and the Supabase client; the summary text no longer claims an OAuth fallback.

---

## 12. Tests, verification status and remaining manual work

### Tests written (not yet executed here)

| File | Purpose |
| --- | --- |
| `FirebaseAuthRepositoryTest` | start-up states, restore, first-time provisioning vs returning user, Google/phone sign-in, OTP retry without a new SMS, resend throttle, profile save + conflict, sign-out with a failing Firebase call, deletion ordering and partial deletion, refresh, token failures |
| `AuthViewModelTest` | single-flight Google taps, silent cancellation, unconfigured build, OTP screen lifecycle, resend gating, taken username, sign-out, one-shot routing events |
| `FirebaseAuthErrorsTest` | every mapped Firebase code → safe message; no code or SDK class name leaks |
| `PhoneNumbersTest` | E.164 rules for 38 countries, trunk zeros, pasted international numbers, length errors |
| `GoogleSignInCoordinatorTest` | token extraction, web client ID, unconfigured, no resumed host, late credential after host loss, wrong credential type, cancellation, single-flight, lock released after a throw, cancellation propagation |
| `GoogleCredentialRequestTest` | two-stage authorized/unrestricted behaviour, `NoCredentialException` semantics, cancellation, unsupported provider, offline, blank token, non-Google credential |
| `AuthErrorsTest`, `ProfileOnboardingTest`, `StartupRouteTest`, `ForegroundActivityTrackerTest` | rewritten/adapted to the new contracts |

Tests use in-memory fakes for Firebase, Supabase and the token bridge, virtual time for the countdown,
and **no real SMS**: Firebase test phone numbers are used for manual checks only. `AuthTestFixtures`
contains `FakeAuthGateway`, `FakeProfileGateway` and `RecordingTokenBridge`, so the security-relevant
invariants (token installed only for the current user, cleared on sign-out/deletion) are asserted
rather than assumed.

### What was actually verified in this environment

* No JDK, no Android SDK, no Gradle distribution, no Maven access — `./gradlew` **cannot run here**, so
  no compilation, unit test, lint, APK or device behaviour was observed.
* Statically verified instead: every removed symbol has no remaining reference; every kept declaration
  used by the app/tests exists; interface members match implementations; the supabase-kt 2.5.0 API
  surface used here (`UserSession` constructor, `importSession(..., autoRefresh)`, `clearSession`,
  `MemorySessionManager`, `AuthConfig` flags, `AuthenticatedSupabaseApi` bearer-token reading) was
  checked against the 2.5.0 sources; the Firebase SDK members used were checked against the Firebase
  Auth API; the Supabase Third-Party Auth behaviour was checked against current Supabase documentation;
  the migration SQL was reviewed for syntax, idempotency and non-destructiveness; the CI YAML and both
  Python heredocs were reviewed.

### Remaining manual work, in order

1. `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` in an environment with
   JDK 17 + Android SDK 34; fix anything that the compiler or tests report (this is the first real
   check of the migration).
2. Firebase console: project, Android app + SHA-1s, `google-services.json`, enable Google + Phone, add
   test phone numbers (§4).
3. Supabase: register the Firebase project under Third-Party Auth; assign `role: "authenticated"` to
   every user; set `GOOGLE_WEB_CLIENT_ID` to the **Web** client ID.
4. Back up the database, then apply `20261101090000_firebase_identity.sql` and run its five
   verification queries.
5. Seed `admin_roles` for real administrators.
6. Optionally deploy the `delete-account` function with its two secrets.
7. On a device: Google sign-in, phone sign-in with a test number, restart (session persistence),
   sign-out (back-stack), profile edit, account deletion — the checklist in `docs/AUTH_SETUP.md` §5.
8. Configure the CI variables (`GOOGLE_SERVICES_JSON` included) and confirm a green build whose APK
   assertions pass.

### Known risks

* The unit tests and the compiler have not run; a typo or an SDK signature mismatch is possible and
  will surface in step 1.
* Without the `role: "authenticated"` claim the app signs in correctly but every table read returns
  nothing. This is documented prominently because it is the most likely deployment mistake.
* Phone sign-in depends on Firebase's SMS quota/billing and app verification; only test numbers are
  reliable in CI-like environments.
* Legacy Supabase-Auth users are not migrated automatically and will see onboarding until an operator
  links their rows.
* supabase-kt 2.5.0's `importSession` mechanism is a documented workaround for that SDK version;
  upgrading to 3.x (which supports an `accessToken` callback) is the recommended follow-up.
