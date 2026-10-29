# RushIt

RushIt is an initial Android/Jetpack Compose quick-commerce app, built around the supplied cream, coral and peach reference. The repository originally contained only this README; there was no existing Android project, approved logo or backend configuration.

## What is included

- An animated, session-aware splash; a phone login and real OTP-verification flow; and a scrollable grocery home.
- A clearly marked **sample store** that can be browsed without signing in. Its products, stock and prices are illustrative, not live. It does not place orders.
- Search by product or category, category and bestseller listings, cart quantities and calculated totals, and a locally persisted wishlist. The cart is retained while navigating but is currently in memory only.
- A locally saved delivery-address prompt, a notification empty state, and useful Orders and Account screens. No location, delivery estimate, policy URL, SMS success or inventory is invented.
- A Supabase Auth REST implementation behind `AuthRepository`, with session verification/refresh, Android Keystore-encrypted token storage, duplicate-request protection, and network/error handling. The sample catalog is behind `CatalogRepository`; no database schema is assumed or queried.

The reference-inspired wordmark is a **reconstruction**, not an approved production logo. Replace `res/drawable-nodpi/rushit_wordmark.webp` and the decorative splash artwork when an approved RushIt asset is available. Grocery/rider/product illustrations are generated for this project. The rounded outline vectors are converted from the MIT-licensed free Hugeicons set. Font and icon licenses are in `licenses/`.

## Build

Install JDK 17, Android SDK Platform 34 and Android SDK Build-Tools 34.0.0. Android Studio Koala or later can manage these. Gradle needs access to Google Maven, Maven Central and the Gradle distribution service.

```sh
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

The debug APK, when the build succeeds, is at `app/build/outputs/apk/debug/app-debug.apk`. The project uses Gradle 8.7, Android Gradle Plugin 8.5.2, Kotlin 1.9.24 and Compose compiler 1.5.14.

## Manually build a test APK

The **Build test APK** GitHub Actions workflow runs only when manually dispatched. It builds the `:app` debug variant with Android's default debug signing, verifies the APK's package ID, positive version code and signature, then uploads `app-debug.apk` as the **RushIt-Test-APK** artifact. No signing secrets or certificate fingerprint variable are required. The workflow does not access or delete any previously configured signing secrets or keystores.

### Signing identity and updates

The application ID remains `com.rushit.app` and the current `versionCode` is `1`. GitHub-hosted runners are temporary, so their default debug keystore can differ between runs. **APK signing identities are not stable across builds**, and an APK from this workflow may not update a previous installation. A locally built debug APK or a separately signed release APK can also use a different certificate.

Android accepts an update only when the application ID and signing certificate match the installed app and its `versionCode` is **at least** the installed version. The workflow cannot determine what is installed on a device. It prints the public debug certificate SHA-256 fingerprint in the verification step so builds can be compared. The workflow never uninstalls an app; uninstalling an incompatible installation can remove its data. Preserve any existing test keystore and backup if you may need to restore the previous signing identity later.

Optional public repository Actions variables `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `RUSHIT_TERMS_URL`, and `RUSHIT_PRIVACY_URL` use the existing build configuration. Their values are embedded in the APK and are therefore public: only use a Supabase **anon/publishable** key, never a service-role key or another secret. Without these variables the sample store still opens, but phone login is unconfigured.

### Run and download

1. Under **Actions → Build test APK**, choose **Run workflow**, select a branch containing the updated workflow, and run it. Select `main` after the change is merged. Only run code from a branch you trust.
2. Open the completed successful run and download **RushIt-Test-APK** from **Artifacts**. Unzip it to obtain `app-debug.apk`. Failed builds or validation do not upload an APK.
3. Before updating a device, compare its installed version (`adb shell dumpsys package com.rushit.app | grep versionCode`) with the APK's version (`aapt dump badging app-debug.apk`) and compare signing certificates using `apksigner verify --verbose --print-certs`. Use `adb install -r app-debug.apk` only when the identity and version are compatible.

## Configure phone authentication

No credentials are committed. Add the following to the **ignored** root `local.properties` (or supply them as Gradle properties/environment variables in your own build environment):

```properties
SUPABASE_URL=https://YOUR_PROJECT.supabase.co
SUPABASE_ANON_KEY=YOUR_PUBLIC_ANON_OR_PUBLISHABLE_KEY
# Optional; use real HTTPS destinations owned by your organization:
RUSHIT_TERMS_URL=https://YOUR_REAL_TERMS_URL
RUSHIT_PRIVACY_URL=https://YOUR_REAL_PRIVACY_URL
```

1. Enable Phone authentication in the Supabase project, configure an SMS provider and use a six-digit OTP. Configure rate limits and other abuse protections appropriate for production.
2. Use **only** the public anon/publishable client key. A key in an Android APK is extractable; never put a service-role key, `sb_secret_` key, SMS-provider secret or other privileged secret in the app. The build rejects recognizable service-role and secret keys.
3. Configure Row Level Security and policies before adding any real product, address, order or user data. This milestone does not create tables or make assumptions about a Supabase schema.
4. Supply genuine policy URLs when available. Until then the links open an explanatory dialog. The Help button also opens guidance instead of an invented contact destination.

Without Supabase configuration, continuing with a valid number displays an actionable configuration error; it does **not** navigate as if an SMS or sign-in had succeeded. The sample-store entry remains available. When configured, OTP requests go to Supabase's `/auth/v1/otp` and `/auth/v1/verify` endpoints; a session is saved only after successful verification. On launch, the stored session is checked with `/auth/v1/user` and refreshed when necessary. Tokens are encrypted with an Android Keystore key and excluded from backup. A network failure during sign-out clears the local session but reports that server revocation could not be confirmed.

## Verification and current limitations

- The updated no-secret workflow and full Gradle build require a successful GitHub Actions run before the resulting APK can be considered verified.
- Unit tests cover phone validation, cart arithmetic, search and the auth ViewModel/repository success and failure paths. Run them with the Gradle command above on a properly provisioned machine.
- An offline JVM run passed 5 domain tests and 6 Supabase-repository tests, plus 6 ViewModel checks using lifecycle/clock stubs. These are not a device or full Gradle test run.
- In the implementation sandbox, `assembleDebug` and `testDebugUnitTest` could not resolve `com.android.application:8.5.2` because outbound access to Google Maven/Maven Central was blocked. A full Android/Compose build is therefore **not verified** there. Android resources were independently compiled and linked with `aapt2`, and the Kotlin sources received an offline type check using cached AndroidX artifacts and limited stubs; this is not a substitute for a Gradle build.
- There was no emulator or attached device in the sandbox, so actual screen rendering, navigation, cutouts, system-bar modes, font scaling and keyboard behavior have not been device-tested or compared by screenshot. Test those on small, standard and large devices before release.
- No real Supabase project/SMS provider was supplied, so end-to-end SMS delivery, session restore against a real project, and server-side policies have not been tested.
- Catalog, wishlist, address, cart and order screens are not connected to a backend; address and wishlist are device-local, and cart contents do not survive process death. Order history, notifications, checkout, live availability and delivery fees are intentionally not implemented. Google sign-in and image search are not presented as nonfunctional controls.

### Suggested device smoke test

1. Launch with and without a real existing session; confirm splash routing and offline error handling.
2. Try short/invalid numbers, a valid number, a failed OTP request, and a successful SMS/verification with a configured provider. Confirm keyboard and large-text layouts remain usable.
3. Browse the sample store, search by name/category, open each category and bestseller listing, add the same product more than once, change quantities in the cart, toggle a wishlist heart, and navigate between all four tabs.
4. Change/remove the local address and open Help, policy links and Notifications. Check gesture and three-button navigation, display cutouts and larger font scales.
