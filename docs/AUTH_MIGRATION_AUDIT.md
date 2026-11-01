# RushIt — Authentication audit (Supabase Auth → Firebase Authentication)

This document is the phase-1 audit performed **before** any file was modified, plus the plan that
the implementation followed. It is kept in the repository as the record of what the migration
changed and why.

Baseline commit: `389fffad95366d37a465d7647a247cea6874ed07` (`main`).
Branch: `arena/ec01dd8c-rushit`.

---

## 1. Existing authentication flow (before the migration)

1. `RushItApplication` created one `SupabaseClient` and installed three plugins: `Auth` (GoTrue),
   `Postgrest` and `Functions`. The GoTrue plugin owned the whole session lifecycle (persist, load,
   refresh, rotate, expire) and stored the serialized `UserSession` in an Android-Keystore-encrypted
   `SharedPreferences` entry via `EncryptedSessionManager` + `SecureSessionStore`.
2. `MainActivity` bound a weak foreground-activity tracker, consumed the `rushit://auth/callback`
   deep link and asked `AuthViewModel` to sign in.
3. `AuthViewModel.initiateGoogleSignIn()` called `GoogleSignInCoordinator`, which first tried
   Jetpack Credential Manager (`GetGoogleIdOption`, nonce-bound ID token) and, if no credential was
   available (or Play Services was unsupported), fell back to Supabase's official **PKCE browser
   OAuth** flow in a Custom Tab (`SupabaseBrowserSignIn` → `rushit://auth/callback`).
4. `AuthViewModel` pre-validated the Google ID token locally (`GoogleIdTokenValidator`) and then
   called `AuthRepository.signInWithGoogle(idToken, rawNonce)`.
5. `SupabaseAuthRepository` (orchestration) called `SupabaseAuthGateway.signInWithIdToken()`, i.e.
   **Supabase GoTrue `signInWith(IDToken)`** with `provider = Google`, which made Supabase Auth
   verify the Google token and mint a **Supabase session** (`auth.users` row, `auth.users.id` UUID).
6. `SupabaseAuthRepository.resolveAccount()` read `public.profiles` for `auth.uid()`, provisioned the
   row on first sign-in (`upsert … ignoreDuplicates`) and published `AuthState.Authenticated` or
   `AuthState.ProfileIncomplete`.
7. Navigation is driven by `AuthState` only (`SplashScreen.startupRouteFor`, then `AuthViewModel`
   events). Sign-out called `auth.signOut(SignOutScope.LOCAL)`; account deletion called the
   `delete-account` Edge Function and fell back to the `delete_user_account()` RPC.

**Search results** (whole repository, excluding `supabase/migrations`):

| Symbol | Where it was used |
| --- | --- |
| `SupabaseAuthGateway` | `RushItApplication`, `app/build.gradle.kts` (serializer), tests |
| `AuthViewModel` | `MainActivity`, `RushItApp`, `LoginScreen`, `ProfileSetupScreen`, `AccountOrdersScreens`, tests |
| `CredentialManagerGoogleSignIn` | `GoogleSignInCoordinator` |
| `signInWithGoogle` | `AuthRepository`, `AuthViewModel`, `RushItApp`, tests |
| `signInWith` / `signInWithIdToken` | `SupabaseAuthGateway` (`auth.signInWith(IDToken)`), `AuthGateway` |
| `signOut` | `SupabaseAuthGateway` (`auth.signOut(SignOutScope.LOCAL)`), `AuthViewModel`, Account UI |
| `currentSession` / `currentSessionOrNull` | `SupabaseAuthGateway`, `SupabaseAuthRepository` |
| `refreshSession` / `refreshCurrentSession` | `SupabaseAuthGateway`, `AuthGateway` |
| `authStateChanges` | not present (the app used `awaitInitialization()` + `sessionStatus`) |
| `GoogleAuthProvider` / `PhoneAuthProvider` / `FirebaseAuth` | **not present anywhere** |
| `localhost:3000` / `redirect_uri` | **not present anywhere** — the reported erroneous redirect does not exist in this tree; the only callback was `rushit://auth/callback` |
| `SUPABASE_URL` / `SUPABASE_ANON_KEY` / `GOOGLE_WEB_CLIENT_ID` | `app/build.gradle.kts` (validated → `BuildConfig`), CI workflow, README, `docs/AUTH_SETUP.md` |
| Supabase auth callbacks | `MainActivity.consumeAuthIntent`, `AuthViewModel.handleAuthRedirect`, `GoogleSignInCoordinator.completeBrowserRedirect` |

## 2. Authentication-related files (before)

```
app/src/main/java/com/rushit/app/
  RushItApplication.kt                     Supabase client + Auth plugin + gateway wiring
  MainActivity.kt                          deep-link consumption, foreground tracker
  data/auth/AuthGateway.kt                 transport interface + session models (Supabase-shaped)
  data/auth/AuthRepository.kt              AuthState / AuthAccount / AuthResult + interface
  data/auth/AuthErrors.kt                  Supabase/GoTrue error translation
  data/auth/SupabaseAuthGateway.kt         GoTrue + Postgrest implementation (+ profiles)
  data/auth/SupabaseAuthRepository.kt      orchestration
  data/auth/EncryptedSessionManager.kt     GoTrue SessionManager over encrypted storage
  data/auth/SecureSessionStore.kt          Keystore AES-GCM store of the GoTrue UserSession
  data/auth/ProfileOnboarding.kt           profile rules (name/username/avatar validation)
  data/auth/google/CredentialManagerGoogleSignIn.kt
  data/auth/google/GoogleCredentialRequest.kt
  data/auth/google/GoogleSignInClient.kt   outcomes, messages, credential parsing
  data/auth/google/GoogleSignInCoordinator.kt  single-flight native + browser
  data/auth/google/SupabaseBrowserSignIn.kt    PKCE browser OAuth fallback
  data/auth/google/SupabaseOAuthRedirect.kt    rushit://auth/callback parser
  data/auth/google/SignInNonce.kt          nonce hashing for Supabase's GoTrue check
  data/auth/google/GoogleIdToken.kt        local JWT decode + pre-flight validation
  data/auth/google/Base64Url.kt            Base64URL decoder for the above
  data/auth/google/ForegroundActivityTracker.kt
  data/model/AuthModels.kt                 Profile / UserSettings / UserDevice / UserActivityLog
  ui/viewmodel/AuthViewModel.kt
  ui/screens/login/LoginScreen.kt, ProfileSetupScreen.kt
  ui/screens/splash/SplashScreen.kt        startupRouteFor(AuthState)
  ui/navigation/RushItApp.kt               auth-driven navigation + guards
  ui/screens/store/AccountOrdersScreens.kt sign-out / delete-account UI
```

Tests: `AuthErrorsTest`, `AuthTestFixtures`, `AuthViewModelTest`, `EncryptedSessionManagerTest`,
`GoogleCredentialRequestTest`, `GoogleIdTokenTest`, `GoogleSignInCoordinatorTest`,
`GoogleSignInFailureTest`, `SupabaseAuthRepositoryTest`, `SupabaseOAuthRedirectTest`,
`ProfileOnboardingTest`, `StartupRouteTest`, `ForegroundActivityTrackerTest`.

## 3. Supabase authentication dependencies and calls (before)

Artifacts (`app/build.gradle.kts`): `io.github.jan-tennert.supabase:bom:2.5.0`, `gotrue-kt`,
`postgrest-kt`, `functions-kt`, `ktor-client-android`, `kotlinx-datetime` (pinned for the encrypted
session store), `androidx.browser:browser:1.8.0` (Custom Tabs for the browser fallback),
`androidx.credentials:*` + `googleid` (Credential Manager).

Supabase Auth API surface actually called: `auth.awaitInitialization()`, `auth.sessionStatus`,
`auth.signInWith(IDToken)`, `auth.currentSessionOrNull()`, `auth.refreshCurrentSession()`,
`auth.signOut(SignOutScope.LOCAL)`, `auth.clearSession()`, `auth.retrieveUserForCurrentSession()`,
`auth.getOAuthUrl(Google, …)`, `auth.exchangeCodeForSession(code)`, `auth.codeVerifierCache`.

## 4. Existing user identity and profile models

* Canonical identity: `auth.users.id` (a **UUID**) — embedded in `AuthAccount.id`,
  `SessionSnapshot.userId`, `ProfileDraft.id`, `Profile.id`.
* `public.profiles(id uuid PK → auth.users.id ON DELETE CASCADE, full_name, username,
  avatar_url, phone_number, phone_verified, onboarding_completed, created_at, updated_at)`.
* `public.user_settings(user_id uuid → profiles.id)`, `public.user_devices(user_id)`,
  `public.user_activity_logs(user_id)` — all keyed by the same UUID.
* Username uniqueness is a case-insensitive partial unique index; `username_available(text)` is a
  `SECURITY DEFINER` RPC so availability can be answered without a cross-account SELECT.
* Phone fields existed in the schema but no phone authentication existed (`AuthProviderId` had only
  `GOOGLE`).

## 5. Existing session and navigation behaviour

* Session persistence: GoTrue `UserSession` serialized with kotlinx-serialization into
  Keystore-encrypted preferences; `autoLoadFromStorage`, `autoSaveToStorage`, `alwaysAutoRefresh`,
  lifecycle-aware refresh.
* Start-up: `AuthState.Loading` → `SplashScreen` holds routing until a non-transient state is known,
  so protected screens are never shown before the session is resolved.
* `AuthViewModel.publish()` maps state → navigation event; `RushItApp` clears the whole back stack on
  every authentication transition.
* Logout: repository revokes the refresh token, clears local state, publishes `Unauthenticated`.

## 6. Existing database / business dependencies

There is **no product, category or order table access** in the app today:
`RushItApplication.catalogRepository` is `SampleCatalogRepository` (bundled assets) and
`orderRepository` is `LocalOrderRepository` (device-local). The only Supabase data access is
`profiles`, `user_settings`, `user_activity_logs`, the `username_available` RPC and the
`delete-account` Edge Function. `CatalogRepository`/`OrderRepository` interfaces must stay untouched;
they are the extension points for the real backend.

Administration: the `admin` module is a separate application (`com.rushit.admin`) that is a **UI
shell with sample data**. It has a login screen that says
`"Authentication is not connected. Use demo access below; entered credentials are not checked."`,
its `AdminRoutes.isAuth()` only decides chrome, and it has no Supabase/Firebase dependency. There is
therefore no admin authentication to migrate — but also none to preserve. The migration adds the
server-side authorization primitive (`public.admin_roles` + `public.is_admin()`) that the admin
application must use when it gains a data layer, and documents that as a remaining task.

## 7. Required Firebase configuration

| Item | Value / action |
| --- | --- |
| Firebase project | reuse if one already exists for RushIt, otherwise create (documented in `docs/AUTH_SETUP.md`) |
| Android app | package `com.rushit.app`, plus SHA-1/SHA-256 for debug, release and Play App Signing |
| Providers | Google (Web client = `GOOGLE_WEB_CLIENT_ID`) and Phone |
| `google-services.json` | `app/google-services.json`, **not committed**; detected at build time |
| Gradle | `com.google.gms.google-services:4.4.2` (conditional), `firebase-bom:33.7.0`, `firebase-auth` |
| Supabase | Authentication → Third-Party Auth → **Firebase**, `project_id` = Firebase project ID |
| Supabase role claim | every Firebase user needs the custom claim `role: "authenticated"` (blocking function or Admin SDK script) |
| Phone | test numbers, SMS quota/billing, App Check / Play Integrity notes |

## 8. Potential breaking changes

1. **Identity is no longer a UUID.** Firebase UIDs are 28-character strings. `profiles.id` stays a
   UUID (internal key) and a new `firebase_uid text unique` column becomes the external identity.
   Every RLS policy must move from `auth.uid()` to `auth.jwt() ->> 'sub'`.
2. `profiles.id` no longer references `auth.users` — the FK is dropped because Firebase users have no
   `auth.users` row. Dependent tables keep `references profiles(id) on delete cascade`.
3. Existing Supabase-Auth users cannot be mapped automatically (there is no way to derive a Firebase
   UID from a Supabase UUID). Their rows remain, unmapped, and are only reachable after an explicit
   admin mapping — never deleted.
4. Session storage changes: GoTrue's encrypted store is removed; Firebase owns persistence.
5. Deep link `rushit://auth/callback`, the Custom Tabs fallback, nonce handling and the local Google
   token pre-flight become dead code.
6. Account deletion moves from the `delete-account` Edge Function to Firebase's client-side
   `FirebaseUser.delete()` plus a `profiles` DELETE policy.
7. `docs/AUTH_SETUP.md`, `README.md` and the CI workflow all describe Supabase Google auth and must
   be rewritten.

## 9. Phased implementation plan (what was executed)

| Stage | Work |
| --- | --- |
| A | this audit |
| B | rewrite the auth abstraction: `AuthGateway` (Firebase identity), `ProfileGateway` (Supabase business data), `AuthRepository`/`AuthState` |
| C | Gradle: Firebase BoM + `firebase-auth`, conditional `google-services` plugin, `FIREBASE_CONFIGURED` BuildConfig flag, manifest cleanup |
| D | Google sign-in: Credential Manager → `GoogleAuthProvider.getCredential` → `signInWithCredential` |
| E | Phone OTP: `PhoneAuthProvider.verifyPhoneNumber(PhoneAuthOptions)` + resend + auto-retrieval, new `PhoneLoginScreen` |
| F | session restore, logout, profile lookup/provisioning, `SupabaseIdentityBridge` |
| G | Firebase JWT → Supabase: third-party auth integration + `firebase_uid` identity column + RLS rewrite |
| H | delete Supabase auth code, unused dependencies, obsolete CI/manifest pieces, the Edge Function |
| I | security review: RLS, ownership, admin authorization, secret handling |
| J | verification: static checks, unit-test review, documented build/test status |

## 10. Verification constraints recorded honestly

The implementation sandbox has **no JDK, no Android SDK, no Gradle distribution and no Maven
access** (only `github.com`, `codeload.github.com`, `api.github.com`, `registry.npmjs.org`,
`pypi.org`, `files.pythonhosted.org`). Gradle therefore could not run here, and no Android build,
unit test or live sign-in could be executed in this environment. Everything that *could* be verified
statically (symbol references, imports, manifest/BuildConfig wiring, SQL syntax review, API
signatures checked against the published Firebase/Google/supabase-kt sources) was verified, and the
report states exactly which checks are still outstanding and must run in CI.

## 11. Stage C–H decisions as implemented

Recorded here so the audit and the code agree. Where this section differs from an earlier draft in
this document, **this section and the code win**.

### 11.1 Dependencies actually used

| Artifact | Version | Why this version |
| --- | --- | --- |
| `com.google.firebase:firebase-bom` | **33.7.0** | Matches `compileSdk 34` and AGP 8.5.2. Later BOMs expect newer build toolchains; upgrading the SDK/toolchain is out of scope for an auth migration. |
| `com.google.firebase:firebase-auth` (from the BOM) | 23.2.0 | Provides `PhoneAuthProvider.verifyPhoneNumber(PhoneAuthOptions)`, `signInWithCredential`, `getIdToken(forceRefresh)`, `delete()`. |
| `com.google.gms:google-services` plugin | **4.4.2** | Requires AGP ≥ 8.1; compatible with 8.5.2. Applied **only** when `app/google-services.json` exists. |
| `org.jetbrains.kotlinx:kotlinx-coroutines-play-services` | 1.8.1 | Matches the project's coroutines version; supplies `Task.await()`. |
| `androidx.credentials:credentials{,-play-services-auth}` | 1.3.0 | Credential Manager, already present before the migration. |
| `com.google.android.libraries.identity.googleid:googleid` | 1.1.1 | `GetGoogleIdOption` / `GoogleIdTokenCredential`. |

The latest published versions at the time of writing (Firebase BoM 35.x, google-services 4.5.0) were
**deliberately not** adopted: they would require raising `compileSdk`/AGP/Kotlin, which is a
toolchain upgrade unrelated to authentication. The metadata was checked against the Google Maven
repository index; the pinned versions are the newest that are known-compatible with this project's
build toolchain.

Removed: `io.github.jan-tennert.supabase:functions-kt` (the app never calls an Edge Function any
more), `androidx.browser` (no Custom Tabs), and the `org.json` test dependency. Kept:
`gotrue-kt` — **not** for Supabase Auth, but because supabase-kt 2.5.0 has no other way to attach an
externally issued bearer token to PostgREST requests (see 11.3).

### 11.2 Removed code (verified, not assumed)

Deleted after confirming nothing else referenced them: `SupabaseAuthGateway`, `SupabaseAuthRepository`,
`SecureSessionStore`, `EncryptedSessionManager`, `google/SupabaseBrowserSignIn`,
`google/SupabaseOAuthRedirect`, `google/SignInNonce`, `google/GoogleIdToken`, `google/Base64Url`, and
the four unit tests that only exercised those classes. Everything that also served profile data,
onboarding rules, error text or navigation was kept and re-pointed at the new abstractions:
`ProfileOnboarding`, `UsernameRules`/`FullNameRules`, `AuthErrors`, `phone/PhoneNumber`.

### 11.3 Supabase SDK: why `gotrue-kt` is still installed

Supabase's third-party auth expects the client to send the Firebase ID token as its bearer token.
supabase-kt **2.5.0** only reads a bearer token from its own `Auth` plugin session
(`AuthenticatedSupabaseApi` → `Auth.currentAccessTokenOrNull()`), and it has no per-request header
override. The 3.x `accessToken` callback does not exist in 2.5.0.

The chosen mechanism is therefore `Auth.importSession(session, autoRefresh = false)` with:

* `refreshToken = ""`, so no refresh is possible and no Supabase Auth endpoint is ever called;
* `autoLoadFromStorage = false`, `autoSaveToStorage = false`, `alwaysAutoRefresh = false`,
  `enableLifecycleCallbacks = false`, `sessionManager = MemorySessionManager()` — the SDK never reads,
  writes, rotates or revokes anything;
* the token re-imported before every backend operation and removed on sign-out/deletion.

Verified against the 2.5.0 sources: `importSession(session, autoRefresh = false, source = External)`
sets `SessionStatus.Authenticated` and returns without scheduling a refresh job, and
`currentAccessTokenOrNull()` is a pure read. When no token is installed the client falls back to the
publishable key, PostgREST runs as `anon`, and every policy denies the request — fail-closed.

Upgrading to supabase-kt 3.x to use its `accessToken` callback is the natural follow-up, but it is a
breaking SDK upgrade and was out of scope.

### 11.4 Firebase ↔ Supabase validation: supported integration, not a token exchange

Chosen: **Supabase Third-Party Auth → Firebase** (Dashboard → Authentication → Third-Party Auth, or
`[auth.third_party.firebase]` in `config.toml`). Supabase verifies the Firebase JWT (asymmetric keys
with `kid`, `iss = https://securetoken.google.com/<project-id>`, `aud = <project-id>`) before PostgREST
runs. The Firebase UID arrives as `sub`, which is **text**, so `auth.uid()` (uuid cast) is unusable and
every policy uses `(select auth.jwt() ->> 'sub')`.

One requirement is easy to miss and is documented prominently: Supabase derives the Postgres role from
the JWT's `role` claim, so each Firebase user needs `role: "authenticated"`
(`beforeUserCreated`/`beforeUserSignedIn` blocking functions and/or Admin SDK
`setCustomUserClaims`). Without it the request runs as `anon` and every policy denies.

No token-exchange Edge Function is needed; the Edge Function in the repository is only a deletion
fallback, and it verifies Firebase tokens against Google's JWKS.

### 11.5 Manifest and CI

* The `rushit://auth/callback` intent filter and the Custom Tabs `<queries>` entry were removed; the
  APK now declares only `INTERNET` and `ACCESS_COARSE_LOCATION`. No SMS permissions are declared: the
  app never reads SMS itself, so auto-retrieval is a bonus and manual entry always works.
* CI asserts the **absence** of the browser flow (`rushit://` scheme, `SupabaseOAuthRedirect`,
  `SupabaseBrowserSignIn`, `CustomTabsIntent`) and the presence of Credential Manager, Firebase
  Authentication and the Supabase client, replacing the old `SupabaseOAuthRedirect` assertion.
* CI writes `app/google-services.json` from the `GOOGLE_SERVICES_JSON` repository variable when it is
  set, and otherwise warns and builds without Firebase (the app reports itself as unconfigured).
* `google-services.json`, Firebase Admin service-account JSON and the existing keystore patterns are
  git-ignored.
