# RushIt

RushIt is an initial Android/Jetpack Compose quick-commerce app, built around the supplied cream, coral and peach reference. The repository originally contained only this README; there was no existing Android project, approved logo or backend configuration.

## What is included

- A session-aware, animated splash with visible first-frame artwork and a bounded session check that routes a restored account straight past the login screen; a Google-only login and a one-time profile onboarding flow.
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
- A Supabase-backed account system behind `AuthRepository`: Credential Manager Google Sign-In exchanged for a session through `signInWith(IDToken)`, SDK-owned refresh, Android Keystore-encrypted session storage, duplicate-request protection, idempotent account provisioning, profile onboarding and editing, and centralized error translation that never shows a raw token. The sample catalog is behind `CatalogRepository`; addresses, wishlist and demo orders remain device-local.

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
| `GOOGLE_WEB_CLIENT_ID` | The real **Web application** OAuth client ID, with the form `PROJECT_NUMBER-IDENTIFIER.apps.googleusercontent.com`. It must match the Supabase Google provider's client ID. |
| `SUPABASE_URL` | The real HTTPS project origin, such as `https://YOUR_PROJECT.supabase.co`; no embedded credentials, path, query, or fragment. |
| `SUPABASE_ANON_KEY` | The project's public `sb_publishable_...` key or legacy JWT whose payload has `role=anon`. |
| `RUSHIT_TERMS_URL` | Optional real HTTPS terms destination. |
| `RUSHIT_PRIVACY_URL` | Optional real HTTPS privacy destination. |

The first three are **required for CI**. Missing/malformed values fail in preflight before tests or compilation; this is intentional because Google Sign-In is the app's only authentication method. Format/role checks cannot prove that a real project accepts a token: the Web client type, provider configuration, and test users still need to be checked in the consoles. Local development still supports blank configuration and reports the unconfigured state at runtime.

These values are embedded in the APK and are public by design. **Never** put a Google OAuth Client Secret, Supabase service-role JWT, or `sb_secret_...` key in these Variables or in the Android build configuration. The Google client secret belongs in Supabase; Supabase privileged keys belong only in trusted server/Edge Function configuration. Gradle also rejects a supplied `GOOGLE_CLIENT_SECRET`, malformed public values, and non-anon/secret Supabase keys.

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

## Configure Google Sign-In

Google Sign-In is the only authentication method in this release. No credential is committed; see
[`docs/AUTH_SETUP.md`](docs/AUTH_SETUP.md) for the full Google Cloud Console and Supabase setup, and
add the following to the **ignored** root `local.properties` (or supply them as Gradle
properties/environment variables in your own build environment). Replace the placeholders with real public values before configuring a build; leave them unset for unconfigured local development. CI requires the three authentication values as described above:

```properties
SUPABASE_URL=https://YOUR_PROJECT.supabase.co
SUPABASE_ANON_KEY=YOUR_PUBLIC_ANON_OR_PUBLISHABLE_KEY
GOOGLE_WEB_CLIENT_ID=000000000000-xxxxxxxx.apps.googleusercontent.com
# Optional; use real HTTPS destinations owned by your organization:
RUSHIT_TERMS_URL=https://YOUR_REAL_TERMS_URL
RUSHIT_PRIVACY_URL=https://YOUR_REAL_PRIVACY_URL
```

`GOOGLE_WEB_CLIENT_ID` is the **Web application** OAuth client ID. It is the audience of the ID token
that Credential Manager returns, and it must be the same value Supabase is configured with, because
Supabase rejects a token whose audience it does not know. The matching **client secret belongs in
Supabase only** — it is never referenced by the app, and the build fails if a `GOOGLE_CLIENT_SECRET`
property is supplied or `SUPABASE_ANON_KEY` is not a well-formed publishable key/legacy anon JWT.

The manual steps that remain, all documented in `docs/AUTH_SETUP.md`:

1. Configure the OAuth consent screen and add test users while the publishing status is *Testing*.
2. Create a **Web application** OAuth client (the audience) and an **Android** OAuth client for
   `com.rushit.app` with debug, release and Play App Signing SHA-1 fingerprints.
3. Enable the Google provider in Supabase with that client ID and secret. Add
   `https://YOUR_PROJECT.supabase.co/auth/v1/callback` to the Google Web client's authorized redirect
   URIs and the exact app URL `rushit://auth/callback` to Supabase **Authentication → URL
   Configuration → Redirect URLs**. These support the official browser OAuth fallback.
4. Apply `supabase/migrations/20261031_rushit_auth.sql` and
   `supabase/migrations/20261031120000_google_auth_accounts.sql` with `supabase db push`. The second
   is idempotent and forward-only: it reconciles the first schema to the account model the app uses,
   replaces an over-permissive public `SELECT` policy on `profiles` with row-level policies that carry
   both `USING` and `WITH CHECK`, adds column-level grants so a client cannot write `profiles.id` or
   `profiles.phone_verified`, and adds a case-insensitive unique username index, `updated_at` triggers,
   an idempotent `handle_new_user()` provisioning trigger, `username_available()` and a hardened
   `delete_user_account()`.
5. Deploy account deletion: `supabase secrets set SUPABASE_SERVICE_ROLE_KEY=…` then
   `supabase functions deploy delete-account`. Without it, and without the fallback RPC, **Delete
   account** reports that deletion is unavailable and changes nothing.

Google sign-in first requests previously authorized accounts through Credential Manager, then retries
with the unrestricted account picker using the same hashed nonce. `NoCredentialException` is not
reported as “no Google account on this device.” Cancellation is silent. If native credentials remain
unavailable or Play Services is unsupported, RushIt opens Supabase's official PKCE browser flow in a
Custom Tab/browser and accepts only the verified `rushit://auth/callback` code callback. Native tokens
retain audience/nonce pre-validation before Supabase exchange; browser codes require the SDK's
persisted PKCE verifier. Duplicate requests/callbacks are guarded, and Activity recreation rebinds the
foreground host without retaining a destroyed Activity.

Runtime behaviour: on launch the stored session is read from Keystore-encrypted storage, handed to the
Supabase SDK, verified with `/auth/v1/user` and refreshed when necessary; a valid session routes
straight past the login screen with no flash. A first sign-in creates exactly one `profiles` row and
one `user_settings` row and routes to onboarding; a returning account keeps its customized name and
avatar, because provider data is only ever a *suggestion* for an empty field. Sign-out clears the local
session and the navigation stack, and reports when server-side revocation could not be confirmed.
Tokens are never logged and never reach a user-facing message. A network failure while restoring a
session keeps the session and offers **Retry** rather than signing the user out.

Phone-number authentication is intentionally absent: there is no phone UI and no phone-auth code path.
The schema already carries `profiles.phone_number` (unique, nullable) and `profiles.phone_verified`
(no client `UPDATE` grant), account ownership is always `auth.users.id`, and `AuthProviderId` is
parameterized, so a verified phone identity can be **linked** to an existing account later without
creating a second profile. Nothing merges accounts on the basis of a matching email or user-supplied
metadata.

## Verification and current limitations

- Google Sign-In fix verified on GitHub Actions on 2026-11-01 in [run 41279518039](https://github.com/devjeetx7-synfusion/Rushit/actions/runs/41279518039): the exact command `./gradlew :app:testDebugUnitTest :app:assembleDebug --no-daemon --console=plain` passed **197 unit tests** (0 failures, 0 errors, 0 skipped). The generated `com.rushit.app` debug APK (versionCode 29) passed signature verification and packaged checks for the configured Google Web Client ID, Credential Manager, the Supabase OAuth fallback, and the exact `rushit://auth/callback` intent filter. Artifacts: `rushit-user-apk` and `rushit-user-unit-test-reports`. This run used the runner's ephemeral debug key because persistent test-signing Secrets are still unset; it is not evidence of stable in-place update signing or a live Google/Supabase device sign-in.
- The workflow/signing change was verified on GitHub Actions on 2026-10-31: [fixture verification run 41225367021](https://github.com/devjeetx7-synfusion/Rushit/actions/runs/41225367021) passed **172 existing JUnit tests** (0 failures, 0 errors, 0 skipped), the required test/build command, unconfigured local `versionCode = 1`, and negative Gradle configuration checks. Two **real debug APKs** built with one disposable test identity had version codes **2 then 3**, identical signing certificates, valid signatures, and the exact existing non-working Google Web Client ID test fixture. Their common public SHA-256 was `12203aabb9efcc4e7294dcd53d98812dab6ea62f4c93fc4496ac901441efa93f`. This was a CI-only fixture, not a production credential; the temporary harness and keys were removed. The HTML/XML test results remain in that run's `rushit-signing-fix-test-reports` artifact.
- The production workflow was also exercised in [run 41225366969](https://github.com/devjeetx7-synfusion/Rushit/actions/runs/41225366969): it **correctly failed before compilation** because `GOOGLE_WEB_CLIENT_ID`, `SUPABASE_URL`, and `SUPABASE_ANON_KEY` were missing, posted the pull-request failure comment, and uploaded no APK. A fully configured production-workflow build, repeated runs using the repository's persistent signing Secrets, real Google/Supabase authentication, and an on-device in-place update still need verification after the manual configuration above. The fixture run is not evidence that those external services are configured.
- Local workflow validation passed YAML parsing, actionlint, Bash syntax/ShellCheck, ignore-rule checks, and **57 workflow fixture checks** (including mocked APK-verifier failure paths and real `keytool` keystore decoding/reuse). The local Gradle attempt was blocked before configuration by the sandbox's inability to download the Gradle distribution; the actual Android tests/builds above ran on GitHub Actions, not locally. `supabase/tests/rls_isolation.sql` needs the Supabase CLI and Docker and has **not** been executed. Run `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` and `supabase test db` in a configured environment before shipping; lint and database tests were not part of this verification.
- The previously reported API 27 lint issue was addressed by moving `windowLightNavigationBar` into a `values-v27` resource; lint still needs to be rerun.
- No emulator or attached device was available. Actual splash rendering, navigation, small-screen and large-text layouts, keyboard, location permission, cutouts, system bars, accessibility and screenshots remain unverified on device.
- No Google Cloud OAuth configuration, Supabase project or signing credential was supplied, so **no end-to-end Google sign-in has been performed**: token exchange against a real Supabase project, session restore against a real project, live account deletion, and the applied RLS policies are all unverified. The app reports its own unconfigured state honestly instead of simulating success.
- Catalog, wishlist, address, cart and order screens are not connected to a backend; addresses, recent searches, selected mode, wishlist and demo orders are device-local, and carts do not survive process death. Demo orders survive app restarts, but nothing is sent anywhere.
- The delivery journey is a **demo preview**. There is no order service, rider directory, live location, ETA, payment, inventory reservation or real delivery, and the UI states that explicitly instead of inventing a rider, rating or countdown.

### Suggested device smoke test

1. Launch with and without a real existing session, and on a slow/offline connection; confirm visible splash artwork and one correct route without a long hang, and that a restored session never shows the login screen first. From Account, exercise Edit profile, Sign out and Delete account.
2. Tap **Continue with Google**: confirm the real account picker opens, that backing out shows no error, that a first-time account reaches the unrestricted picker rather than a false “no account” error,
   that the browser fallback works when native credentials are unavailable, and that a first sign-in lands on onboarding with the Google name and photo pre-filled. Sign in again after killing the app and confirm the customized name survives. Confirm Help, the policy links and the duplicate-tap guard behave correctly.
3. Switch modes repeatedly and quickly; confirm the indicator slides once, the category rail clears an incompatible selection, and no blank frame appears. Tap category cards, confirm the products change under a short fade, and check that the selected card scrolls into view. Browse product details, search/filter/sort, wishlist, separate cart retention and computed totals.
4. Add, edit, select and remove a local address; request, deny and grant approximate location, disable location services, and verify the manual-entry fallback. Open Orders, Tracking and Notifications.
5. From the cart, open checkout, add an address, change quantities, place a demo order, and walk the journey: confirmation, packing, rider-ready, route, arrival and delivered. Confirm the celebration does not replay when you reopen the order, that Cancel is only offered before pickup, and that every stage is labelled as a demo.
6. Repeat on small and large displays, with the keyboard open and larger font scales, both gesture and three-button navigation, and display cutouts. Capture screenshots and compare them against the references.

### Artwork and audio notes

- `order_packing.webp`: supplied 1448 × 1086 opaque PNG, resized to 1200 × 900 (26,432 bytes), no crop; displayed with fit scaling. It remains static because its objects are not separate layers.
- `order_rider_ready.webp`: 781 × 602 crop of the supplied screen's stationary rider illustration (29,906 bytes); used only for the stationary rider scene. The existing on-the-way route illustration is retained.
- `order_confirmation.wav`: a 320 ms, mono, 22.05 kHz two-note sine chime synthesized locally for this project; no external sample is used. Device playback and audio policy behavior still need physical-device testing.
