# RushIt

RushIt is an initial Android/Jetpack Compose quick-commerce app, built around the supplied cream, coral and peach reference. The repository originally contained only this README; there was no existing Android project, approved logo or backend configuration.

## What is included

- A session-aware, animated splash with visible first-frame artwork and a bounded session check that routes a restored account straight past the login screen; Google sign-in and phone-number (SMS code) sign-in, plus a one-time profile onboarding flow.
- A clearly marked **sample store** catalog that every signed-in shopper sees. Food & Grocery uses the existing sample products; E-commerce has one fully detailed listing plus a preview-only listing that cannot be bought; Gifts & More has preview categories but no configured inventory. Prices are illustrative and none of these modes places orders.
- One shared shopping-mode switcher (an animated segmented control with a sliding indicator) on Home and Categories. The selected mode is saved locally; each mode's cart is kept separately in memory, and carts do not survive process death. The wishlist is saved locally across modes.
- A horizontally scrolling category rail on Home with an explicit **All** entry. Selecting a card filters the products inline behind a short fade/slide transition, keeps the active card in view automatically, and clears itself when a mode change makes it invalid. A promotional carousel auto-advances, pauses while it is being dragged, and stays still under reduced motion. Wishlist, add-to-cart and bottom-navigation feedback share the timings in `ui/theme/Motion.kt`; the system "remove animations" setting collapses them.
- Debounced mode-specific search (including locally saved, clearable recent searches), category filters and sorting, product details, cart quantities and calculated subtotals.
- Device-local delivery addresses can be added, edited, selected and removed. Users may explicitly request approximate location while entering an address; a permission prompt appears only after that action. Coordinates are optional, are stored in app-private preferences with the address, and are not reverse-geocoded into a fabricated address. Address and search data are not encrypted or sent to a RushIt backend by the app; app backup is disabled.
- Checkout now shows the delivery address, the item list with quantity controls and a bill breakdown (items, delivery, platform fee, payable). Placing an order stores a **device-local demo order**: no gateway, no payment and no fulfilment backend exists, and the app says so on every screen involved.
- A complete illustrated delivery journey follows the order: confirmation, packing, rider-ready, an illustrated route, arrival and a delivered celebration, plus finding-rider, cancelled and failed states. The supplied packing artwork and a crop of the supplied rider reference are bundled as WebP; the existing route, city, bag, tick, confetti and timeline remain local Compose illustrations.
- Orders and Order details are backed by stored local orders, including status, timeline, items, bill breakdown and address. Demo checkout requires an address. A short local confirmation chime is attempted once after successful creation; Account can disable it and it respects the device notification volume and ringer mode. The celebration is recorded once per order.
- **No live tracking, rider assignment, ETA, inventory, serviceability or payment is implemented.** The journey advances only through explicit demo controls, and a status can never be inferred from an animation or a timer.
- Help & Support provides FAQs without invented contact details; Notifications explicitly says the service is not connected. Profile display name, username and photo are editable from Account; notification read/dismiss actions are not available without their service.
- A **Firebase Authentication** account system behind `AuthRepository`: Google sign-in through Jetpack Credential Manager and phone sign-in through Firebase's SMS verification. The Firebase UID is the canonical identity. Every Supabase request carries the Firebase ID token, and Row Level Security compares `auth.jwt() ->> 'sub'` with `profiles.firebase_uid`, so Supabase is used for the database and business data only — **no Supabase Auth call exists anywhere in the app**. Includes single-flight guards on both sign-in paths, idempotent profile provisioning keyed by Firebase UID, profile onboarding and editing, safe sign-out (the Supabase client can never keep the previous user's bearer token), account deletion that refuses to claim success it cannot verify, and centralized error translation that never shows a raw token or OTP. The sample catalog is behind `CatalogRepository`; addresses, wishlist and demo orders remain device-local.

The reference-inspired wordmark is a **reconstruction**, not an approved production logo. Replace `res/drawable-nodpi/rushit_wordmark.webp` and the decorative splash artwork when an approved RushIt asset is available. Grocery, e-commerce, gift and product illustrations were generated for this project. The packing image and stationary rider reference were supplied for this change. The rounded outline vectors are converted from the MIT-licensed free Hugeicons set. Font and icon licenses are in `licenses/`.

## Build

Install JDK 17, Android SDK Platform 34 and Android SDK Build-Tools 34.0.0. Android Studio Koala or later can manage these. Gradle needs access to Google Maven, Maven Central and the Gradle distribution service.

```sh
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

The debug APK, when the build succeeds, is at `app/build/outputs/apk/debug/app-debug.apk`. The project uses Gradle 8.7, Android Gradle Plugin 8.5.2, Kotlin 1.9.22 and Compose compiler 1.5.9. Local builds retain `versionCode = 1` unless overridden with `-PRUSHIT_VERSION_CODE=123` or the `RUSHIT_VERSION_CODE` environment variable. Values must be integers from 1 through 2,100,000,000. Local builds can also use the persistent test signing environment variables described below; without them, the normal local debug keystore is preserved.

## Manually build a test APK

The **RushIt User Build** GitHub Actions workflow runs on pull requests, pushes to `main`, and manual dispatch. Before Gradle runs, it validates the required public Google/Supabase configuration and the CI version code. It then runs exactly:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug --no-daemon --console=plain
```

After the build, it checks the package ID, exact run version code, configured Google Web Client ID in BuildConfig/DEX, and APK signature. It prints only the public certificate SHA-1/SHA-256 fingerprints. Successful runs upload **`rushit-user-apk`** (containing `app-debug.apk`); **`rushit-user-unit-test-reports`** contains the HTML unit-test report and JUnit XML results. Existing pull-request failure comments are retained. A failed preflight, test, build, or APK check does not produce a successful APK artifact.

### Create the persistent test keystore once

Use one long-lived **test-only** identity for this workflow, separate from any production/Play signing key. Run these commands on a trusted machine, **outside the repository**. Do not regenerate the keystore for each CI run. The interactive password prompt keeps passwords out of shell history and command arguments.

```sh
# Bash, with JDK 17 installed. This directory is outside the checkout.
mkdir -p "$HOME/rushit-private-signing"
chmod 700 "$HOME/rushit-private-signing"
keytool -genkeypair \
  -keystore "$HOME/rushit-private-signing/rushit-test.p12" \
  -storetype PKCS12 -alias rushit-test \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=RushIt Test, O=RushIt"
```

Choose a strong password and use the same password for the PKCS12 store and key. Preserve the original file and passwords in an access-controlled password manager/secure backup. If you already have a test keystore, reuse it instead of creating a new identity. Losing the key means future builds cannot update installations signed by it.

Calculate the **public SHA-1 fingerprint** for the Android OAuth client (the command prompts for the store password):

```sh
keytool -list -v \
  -keystore "$HOME/rushit-private-signing/rushit-test.p12" \
  -alias rushit-test
```

In Google Cloud, register an **Android** OAuth client with package **`com.rushit.app`** and that SHA-1. This is separate from the **Web** OAuth client ID passed to the app. CI also prints SHA-1 and SHA-256 after each build; with the same keystore and alias, these must be identical across runs. SHA-256 is useful for comparing the APK signing identity.

Base64-encode the **binary keystore**, not its fingerprint or a text export. Write the encoded secret outside Git and restrict its permissions:

```sh
(umask 077; base64 -w 0 "$HOME/rushit-private-signing/rushit-test.p12" \
  > "$HOME/rushit-private-signing/rushit-test.p12.base64")
# macOS: replace `base64 -w 0 INPUT` with `base64 -i INPUT | tr -d '\n'`.
```

Under **Settings → Secrets and variables → Actions → Secrets**, create these exact repository **Secrets**:

| Secret | Value |
| --- | --- |
| `RUSHIT_TEST_KEYSTORE_BASE64` | Contents of the Base64 file; wrapped Base64 is also accepted. |
| `RUSHIT_TEST_KEYSTORE_PASSWORD` | Keystore/store password. |
| `RUSHIT_TEST_KEY_ALIAS` | The existing alias, `rushit-test` for the command above. |
| `RUSHIT_TEST_KEY_PASSWORD` | Private-key password (same as the store password for the PKCS12 example). |

Do not paste these values into issues, logs, README, or command-line arguments. Do not add a Base64 print command. After securely storing the original and configuring GitHub, remove the temporary Base64 copy if it is no longer needed. Never commit the keystore, passwords, Base64 export, or signing environment files; `.gitignore` protects common names/extensions, but keeping all signing material outside the checkout is the primary protection.

The workflow decodes the keystore with owner-only permissions into a fresh directory under **`$RUNNER_TEMP`**, validates its store password and alias, and passes only its path through `GITHUB_ENV`. Gradle receives the path and the three credential Secrets as environment variables. The debug signing configuration uses them only when all four are configured. Temporary signing files are removed in an always-run cleanup step and are never uploaded. Release signing is unchanged. Partial or invalid signing configuration fails rather than silently switching identities. If all signing Secrets are absent, CI explicitly warns that the APK uses the runner's **ephemeral default debug identity** and may not update an existing installation. Fork PRs do not receive repository Secrets, so this fallback can apply there. The workflow never creates a replacement persistent/random keystore.

### Required public Actions Variables

Under **Settings → Secrets and variables → Actions → Variables**, configure:

| Variable | Required value |
| --- | --- |
| `GOOGLE_WEB_CLIENT_ID` | The real **Web application** OAuth client ID of the Firebase project, with the form `PROJECT_NUMBER-IDENTIFIER.apps.googleusercontent.com`. This is the Firebase ID token audience, so it must come from the same Firebase project as `google-services.json`. |
| `SUPABASE_URL` | The real HTTPS project origin, such as `https://YOUR_PROJECT.supabase.co`; no embedded credentials, path, query, or fragment. |
| `SUPABASE_ANON_KEY` | The project's public `sb_publishable_...` key or legacy JWT whose payload has `role=anon`. |
| `GOOGLE_SERVICES_JSON` | Optional but required for a build that can actually sign in (a repository **variable** or a **secret**; the secret is preferred because it carries multi-line values): paste the full contents of the Firebase `google-services.json` for project `rushit-app` (Firebase console → Project settings → Your apps → Android `com.rushit.app`). It contains public identifiers only, never a service account key. The workflow writes it to the ignored `app/google-services.json`; without it the build still succeeds and the app reports "not configured". |
| `RUSHIT_TERMS_URL` | Optional real HTTPS terms destination. |
| `RUSHIT_PRIVACY_URL` | Optional real HTTPS privacy destination. |

`GOOGLE_WEB_CLIENT_ID`, `SUPABASE_URL` and `SUPABASE_ANON_KEY` are **required for CI**: missing or malformed values fail in preflight before tests or compilation. `GOOGLE_SERVICES_JSON` is optional for compilation but necessary for a build that can actually authenticate; the workflow warns and builds without Firebase when it is absent. Format/role checks cannot prove that a real project accepts a token: the Web client type, provider configuration, and test users still need to be checked in the consoles. Local development still supports blank configuration and reports the unconfigured state at runtime.

These values are embedded in the APK and are public by design. **Never** commit or supply a Google OAuth Client Secret, a Firebase Admin service-account key, a Supabase service-role JWT, or an `sb_secret_...` key: the app needs none of them, and authentication is never authorized by a key held on the device. Supabase service-role credentials belong only in trusted server/Edge Function configuration. Gradle also rejects a supplied `GOOGLE_CLIENT_SECRET`, malformed public values, and non-anon/secret Supabase keys.

### Signing identity, version codes and in-place updates

The application ID remains **`com.rushit.app`**. CI sets **`RUSHIT_VERSION_CODE` to `github.run_number`**, so distinct new runs of this workflow have increasing version codes. A re-run deliberately keeps the original run number and version code. Gradle accepts a same-named Gradle property or environment variable and otherwise defaults to `1` for local development. The workflow checks that the APK's actual version code equals its run number.

Android accepts a normal in-place update when the package ID and signing certificate/lineage match, the version code is **not lower** than the installed version, and the platform/ABI are compatible. Keeping the same Secrets therefore allows future test APKs to update without uninstalling. Using a new run also supplies a higher version code; re-running an existing run can still replace an equal-version installation when the identity is compatible. The workflow cannot inspect what is already installed on a tester's device.

**One migration reinstall may be necessary** when replacing an APK signed by an old ephemeral runner key, a different local debug key, or a release key. Android does not allow changing signing identity through an ordinary update. Before uninstalling, back up anything needed: uninstalling can delete device-local addresses, wishlist and demo orders. A Supabase account is separate, but this workflow does not back up or restore device-local data. If you still have the previous test keystore, prefer configuring that identity instead of forcing a reinstall. After migration to the persistent test key, keep it unchanged. A different release/Play App Signing identity can require another migration; this change does not implement signing-key rotation.

### Run, download and install

1. Under **Actions → RushIt User Build → Run workflow**, select a branch containing this workflow and run it; select `main` after merging. Only run trusted code with access to signing Secrets.
2. Open the successful run. Confirm the version code and public certificate fingerprints in **Verify APK identity, configuration and signature** (also in the run summary). Compare the fingerprints with an earlier configured run; they must match.
3. Download **`rushit-user-apk`** from **Artifacts** and unzip it to obtain **`app-debug.apk`**. Download **`rushit-user-unit-test-reports`** and open `reports/tests/testDebugUnitTest/index.html` for test details; the JUnit XML files are under `test-results/testDebugUnitTest/`. The workflow name and artifact name are different; use the exact lowercase artifact name.
4. With Android SDK platform-tools/build-tools available, inspect and install:

   ```sh
   adb shell dumpsys package com.rushit.app | grep versionCode
   aapt dump badging app-debug.apk | head -n 2
   apksigner verify --verbose --print-certs --min-sdk-version 24 app-debug.apk
   adb install -r app-debug.apk
   ```

   Alternatively, transfer the APK to the device and open it, allowing installation from that trusted source. `adb install -r` keeps app data when the update is compatible; it does not bypass signature or version restrictions. If you see `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, compare signing certificates and follow the one-time migration guidance above rather than repeatedly regenerating keys. Do not use `-d` to force a version downgrade.

## Configure authentication (Firebase Auth + Supabase)

Authentication is **Firebase**. Google sign-in is a Credential Manager request whose ID token is
exchanged with Firebase, and phone sign-in is Firebase's SMS verification. Supabase keeps the
PostgreSQL database, Storage and Edge Functions, and authorizes every request by verifying the
Firebase ID token it is given. No Supabase Auth endpoint is called by the app, there is no OAuth
redirect, no Custom Tab and no callback deep link.

No credential is committed. `docs/AUTH_SETUP.md` has the complete console walkthrough; the short
version is:

1. **Firebase project.** Create (or reuse) a Firebase project, add an Android app with package
   `com.rushit.app`, and register the SHA-1 of every signing identity you install with (debug key,
   CI test key, Play App Signing). Download `google-services.json` and place it at
   `app/google-services.json` (keep your own project's copy out of version control). Gradle applies
   the `google-services` plugin only when the file exists (`BuildConfig.FIREBASE_CONFIGURED`).
2. **Enable the sign-in methods.** Firebase console → Authentication → Sign-in method: enable
   **Google** and **Phone**. Under Google, note the **Web client ID** — that value is
   `GOOGLE_WEB_CLIENT_ID`, and it is the audience Firebase expects in the Google ID token. It is
   optional at build time: when it is not supplied, the build uses the Web OAuth client that
   `google-services.json` already records for `com.rushit.app`, so Google sign-in works from the
   Firebase config file alone.
3. **Phone verification.** Android app verification uses Play Integrity automatically (add your
   release SHA-256 under Project settings → App Check / Android app), with reCAPTCHA as the fallback.
   For automated or repeated manual testing, add Firebase **test phone numbers** with fixed codes
   under Authentication → Sign-in method → Phone → Phone numbers for testing. A real SMS costs money
   and is rate-limited.
4. **Teach Supabase to trust Firebase.** Supabase Dashboard → Authentication → Third-Party Auth →
   add **Firebase** with the Firebase **Project ID** (or `supabase/config.toml`:
   `[auth.third_party.firebase]` with `enabled = true` and `project_id = "..."`). This is the
   supported JWT integration: Supabase verifies the token's signature, issuer and audience before any
   SQL runs. A documented Edge Function fallback exists in `supabase/functions/delete-account`, but it
   is **not** required for normal operation.
5. **Assign the `authenticated` role claim.** Supabase's Postgres role for a third-party JWT comes
   from the `role` claim, so every Firebase user needs `role: "authenticated"` or the request runs as
   `anon` and every policy denies it. Use a blocking function
   (`beforeUserCreated`/`beforeUserSignedIn`, `sessionClaims`) and, for existing users, the Admin SDK
   `setCustomUserClaims(uid, { role: "authenticated" })` plus `getIdToken(true)`. The app already
   forces one token refresh immediately after sign-in for exactly this reason.
6. **Apply the schema.** `supabase db push` applies
   `supabase/migrations/20261101090000_firebase_identity.sql` on top of the existing migrations. It is
   forward-only and non-destructive: it adds `profiles.firebase_uid text` with a unique index, points
   ownership at the Firebase UID, rewrites every policy to `(select auth.jwt() ->> 'sub')`, adds
   `public.is_admin()` plus `public.admin_roles`, and leaves every legacy row in place with
   `firebase_uid = NULL` until it is linked deliberately. Take a backup first.
7. **Optional account-deletion fallback.** `supabase secrets set SUPABASE_SERVICE_ROLE_KEY=…` and
   `supabase secrets set FIREBASE_PROJECT_ID=…`, then
   `supabase functions deploy delete-account`. The app deletes the Firebase account and its own
   profile row without it; the function verifies a Firebase ID token against Google's JWKS and purges
   leftover rows by Firebase UID using the service-role key, which never reaches the app.

Local configuration lives in the **ignored** `local.properties` (or Gradle properties/environment
variables). Replace the placeholders with real public values; leave them unset for unconfigured local
development:

```properties
SUPABASE_URL=https://YOUR_PROJECT.supabase.co
SUPABASE_ANON_KEY=YOUR_PUBLIC_ANON_OR_PUBLISHABLE_KEY
GOOGLE_WEB_CLIENT_ID=000000000000-xxxxxxxx.apps.googleusercontent.com
# Optional; use real HTTPS destinations owned by your organization:
RUSHIT_TERMS_URL=https://YOUR_REAL_TERMS_URL
RUSHIT_PRIVACY_URL=https://YOUR_REAL_PRIVACY_URL
```

### How the two sign-in paths behave

**Google.** Credential Manager asks for a previously authorized account first, then retries with the
unrestricted picker when that finds nothing (`NoCredentialException` is **not** reported as "no Google
account on this device"). Cancellation is silent. The ID token goes to Firebase and nowhere else, and
Firebase verifies its signature, audience and expiry. If Credential Manager is unavailable on the
device the app says so and offers phone sign-in; there is no browser fallback and no `rushit://`
deep link.

**Phone.** The number is validated and normalized to E.164 locally, Firebase sends the code, and the
verification id and resend token stay inside the data layer — never in UI state, a saved-state
bundle, a log or an error message. A wrong code keeps the challenge so a typo does not cost another
SMS; resends are throttled to one per 30 seconds, and a second request while a code is pending never
sends a second SMS. Firebase test phone numbers are the supported way to exercise this without
spending money.

### The sign-in screens

`ui/screens/login/` holds three steps that share one visual language (cream background, RushIt
wordmark, rider artwork on the entry screen, white rounded card, policy footer):

| Screen | Content |
| --- | --- |
| `LoginScreen` | Welcome card: **Continue with phone number** as the red primary action, an "or" rule, then **Continue with Google**. Help button, terms and privacy links. |
| `PhoneNumberScreen` | "Enter your mobile number", country selector defaulting to **India (+91)**, digits-only field capped at the country's national length (10 for India), **Send OTP**, and the Google alternative. |
| `OtpVerificationScreen` | "Enter verification code", the masked destination number, six aligned boxes driven by one text field (paste, backspace across boxes, IME code suggestion and TalkBack all work), **Verify & Continue**, and `Resend OTP in 00:30`. |

The number and the code live in `AuthViewModel`'s `PhoneLoginUiState` plus screen-local state; the
Firebase verification id, resend token and OTP never do. Both phone steps are one navigation route so
the system back button undoes the OTP step instead of abandoning a pending verification.

### Runtime behaviour

On launch Firebase restores its own persisted session; the app waits for Firebase's first auth-state
callback so a cold start cannot be mistaken for a signed-out user, installs the resulting ID token on
the Supabase client, and loads the profile by `firebase_uid`. A restored account routes straight past
the login screen with no flash. A first sign-in creates exactly one `profiles` row and one
`user_settings` row and routes to onboarding; a returning account keeps its customized name and
avatar, because provider data is only ever a *suggestion* for an empty field. An interrupted profile
load of an already signed-in person keeps them signed in and offers **Retry** rather than logging them
out.

Sign-out clears the Supabase client's bearer token **before** returning, even if Firebase's own call
fails, so no later request can be authorized as the previous user, and it clears the back stack.
Account deletion deletes the Firebase account (which may require reauthentication; the app reports
that honestly), then purges the profile row with a freshly minted token, then clears the token. If the
purge fails after the Firebase account is gone, the app says so instead of claiming success.

Nothing is ever merged automatically: two accounts with the same email, phone number or display name
stay separate. Linking a legacy Supabase-Auth row to a Firebase account is a deliberate, audited
`update ... where id = '...' and firebase_uid is null` performed by an operator.

## Verification and current limitations

- **The sign-in UI pass (2026-11-01) is compiled and tested by CI, not on a device.** The login card
  was reordered (phone primary, Google secondary), the phone flow was split into a number step and a
  six-box OTP step with a `mm:ss` resend timer, a shared policy footer was extracted, and
  `VerificationCodeTest` plus new `PhoneNumbersTest`/`AuthViewModelTest` cases were added. The
  authoring sandbox had no JDK, Android SDK or Gradle distribution, so `./gradlew` was not run there.
  GitHub Actions [run 41324448696](https://github.com/devjeetx7-synfusion/Rushit/actions/runs/41324448696)
  then ran `./gradlew :app:testDebugUnitTest :app:assembleDebug --no-daemon --console=plain`:
  **193 unit tests, 0 failures, 0 errors, 0 skipped**, `:app:assembleDebug` succeeded, and the
  resulting `com.rushit.app` debug APK (versionCode 44) passed the package/versionCode,
  packaged-configuration and `apksigner` signature checks. That proves compilation and the tested
  logic only: no screen was rendered on an emulator or device, and no real OTP or Google account was
  used (CI supplies no `GOOGLE_SERVICES_JSON` secret, so the build used only the
  `app/google-services.json` already in the checkout, and no OTP or Google account was entered
  anywhere). Layout, keyboard, font-scale, landscape and TalkBack behaviour of the new screens
  remain unverified.
- **The Firebase Authentication migration was authored without a build, and is now compiled by the
  CI run above.** The migration itself was prepared in an environment without a JDK, Android SDK or
  Gradle distribution, so no unit test, APK, Gradle configuration or Kotlin compilation was observed
  while it was written; the follow-up UI pass had the whole tree compiled and its 193 unit tests
  executed by GitHub Actions. Lint has **not** been run, and the backend steps in
  `docs/AUTH_SETUP.md` (Firebase console, Supabase third-party auth, the migration) are still
  outstanding. The exact remaining manual steps are listed in `docs/AUTH_MIGRATION_REPORT.md`.
- **Superseded history:** the run below predates the Firebase migration. It proves that the *previous*
  Google/Supabase-Auth build compiled, passed 197 unit tests and produced a signed APK; its
  OAuth-fallback and exact `rushit://auth/callback` intent-filter checks no longer describe the app,
  because that flow was removed. The workflow now asserts that those classes and that deep link are
  **absent**.
- Google Sign-In fix verified on GitHub Actions on 2026-11-01 in [run 41279518039](https://github.com/devjeetx7-synfusion/Rushit/actions/runs/41279518039): the exact command `./gradlew :app:testDebugUnitTest :app:assembleDebug --no-daemon --console=plain` passed **197 unit tests** (0 failures, 0 errors, 0 skipped). The generated `com.rushit.app` debug APK (versionCode 29) passed signature verification and packaged checks for the configured Google Web Client ID, Credential Manager, the Supabase OAuth fallback, and the exact `rushit://auth/callback` intent filter. Artifacts: `rushit-user-apk` and `rushit-user-unit-test-reports`. This run used the runner's ephemeral debug key because persistent test-signing Secrets are still unset; it is not evidence of stable in-place update signing or a live Google/Supabase device sign-in.
- The workflow/signing change was verified on GitHub Actions on 2026-10-31: [fixture verification run 41225367021](https://github.com/devjeetx7-synfusion/Rushit/actions/runs/41225367021) passed **172 existing JUnit tests** (0 failures, 0 errors, 0 skipped), the required test/build command, unconfigured local `versionCode = 1`, and negative Gradle configuration checks. Two **real debug APKs** built with one disposable test identity had version codes **2 then 3**, identical signing certificates, valid signatures, and the exact existing non-working Google Web Client ID test fixture. Their common public SHA-256 was `12203aabb9efcc4e7294dcd53d98812dab6ea62f4c93fc4496ac901441efa93f`. This was a CI-only fixture, not a production credential; the temporary harness and keys were removed. The HTML/XML test results remain in that run's `rushit-signing-fix-test-reports` artifact.
- The production workflow was also exercised in [run 41225366969](https://github.com/devjeetx7-synfusion/Rushit/actions/runs/41225366969): it **correctly failed before compilation** because `GOOGLE_WEB_CLIENT_ID`, `SUPABASE_URL`, and `SUPABASE_ANON_KEY` were missing, posted the pull-request failure comment, and uploaded no APK. A fully configured production-workflow build, repeated runs using the repository's persistent signing Secrets, real Google/Supabase authentication, and an on-device in-place update still need verification after the manual configuration above. The fixture run is not evidence that those external services are configured.
- Local workflow validation passed YAML parsing, actionlint, Bash syntax/ShellCheck, ignore-rule checks, and **57 workflow fixture checks** (including mocked APK-verifier failure paths and real `keytool` keystore decoding/reuse). The local Gradle attempt was blocked before configuration by the sandbox's inability to download the Gradle distribution; the actual Android tests/builds above ran on GitHub Actions, not locally. `supabase/tests/rls_isolation.sql` needs the Supabase CLI and Docker and has **not** been executed. Run `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` and `supabase test db` in a configured environment before shipping; lint and database tests were not part of this verification.
- The previously reported API 27 lint issue was addressed by moving `windowLightNavigationBar` into a `values-v27` resource; lint still needs to be rerun.
- No emulator or attached device was available. Actual splash rendering, navigation, small-screen and large-text layouts, keyboard, location permission, cutouts, system bars, accessibility and screenshots remain unverified on device.
- No Firebase project, Google Cloud OAuth configuration, Supabase project or signing credential was supplied, so **no end-to-end sign-in has been performed** for either method: no Google credential has been exchanged with Firebase, no Firebase ID token has been accepted by a real Supabase project, no profile has been written under the new policies, and no account has been deleted. The RLS policy set in
  `supabase/migrations/20261101090000_firebase_identity.sql` has not been executed against any database. The app reports its own unconfigured state honestly instead of simulating success.
- Catalog, wishlist, address, cart and order screens are not connected to a backend; addresses, recent searches, selected mode, wishlist and demo orders are device-local, and carts do not survive process death. Demo orders survive app restarts, but nothing is sent anywhere.
- The delivery journey is a **demo preview**. There is no order service, rider directory, live location, ETA, payment, inventory reservation or real delivery, and the UI states that explicitly instead of inventing a rider, rating or countdown.

### Suggested device smoke test

1. Launch with and without a real existing session, and on a slow/offline connection; confirm visible splash artwork and one correct route without a long hang, and that a restored session never shows the login screen first. From Account, exercise Edit profile, Sign out and Delete account.
2. On the login card tap **Continue with phone number** (the red primary button): confirm the number screen defaults to India (+91), that letters cannot be typed, that a 9- or 11-digit number keeps **Send OTP** disabled with a readable reason, and that **Send OTP** then shows the spinner. With a Firebase **test** number and its fixed code, confirm the six boxes fill one at a time, that pasting a whole code works, that the heading shows the masked destination number, that a wrong code keeps the step open and clears the boxes while the message stays, and that **Resend OTP in 00:30** counts down before resend becomes available. Pressing system Back on the code step must return to the number screen without an error. Then tap **Continue with Google**: confirm the real account picker opens, that backing out shows no error, that a first-time account reaches the unrestricted picker rather than a false “no account” error, and that a first sign-in lands on onboarding with the Google name and photo pre-filled. Sign in again after killing the app and confirm the customized name survives. Confirm Help, the policy links and the duplicate-tap guard behave correctly.
3. Switch modes repeatedly and quickly; confirm the indicator slides once, the category rail clears an incompatible selection, and no blank frame appears. Tap category cards, confirm the products change under a short fade, and check that the selected card scrolls into view. Browse product details, search/filter/sort, wishlist, separate cart retention and computed totals.
4. Add, edit, select and remove a local address; request, deny and grant approximate location, disable location services, and verify the manual-entry fallback. Open Orders, Tracking and Notifications.
5. From the cart, open checkout, add an address, change quantities, place a demo order, and walk the journey: confirmation, packing, rider-ready, route, arrival and delivered. Confirm the celebration does not replay when you reopen the order, that Cancel is only offered before pickup, and that every stage is labelled as a demo.
6. Repeat on small and large displays, with the keyboard open and larger font scales, both gesture and three-button navigation, and display cutouts. Capture screenshots and compare them against the references.

### Artwork and audio notes

- `order_packing.webp`: supplied 1448 × 1086 opaque PNG, resized to 1200 × 900 (26,432 bytes), no crop; displayed with fit scaling. It remains static because its objects are not separate layers.
- `order_rider_ready.webp`: 781 × 602 crop of the supplied screen's stationary rider illustration (29,906 bytes); used only for the stationary rider scene. The existing on-the-way route illustration is retained.
- `order_confirmation.wav`: a 320 ms, mono, 22.05 kHz two-note sine chime synthesized locally for this project; no external sample is used. Device playback and audio policy behavior still need physical-device testing.
