# RushIt

RushIt is an initial Android/Jetpack Compose quick-commerce app, built around the supplied cream, coral and peach reference. The repository originally contained only this README; there was no existing Android project, approved logo or backend configuration.

## What is included

- A session-aware, animated splash with visible first-frame artwork and a bounded session check; a keyboard-safe phone login and real OTP-verification flow.
- A clearly marked **sample store** that can be browsed without signing in. Food & Grocery uses the existing sample products; E-commerce has two non-purchasable preview listings; Gifts & More has preview categories but no configured inventory. Prices are illustrative and none of these modes places orders.
- One shared shopping-mode switcher (an animated segmented control with a sliding indicator) on Home and Categories. The selected mode is saved locally; each mode's cart is kept separately in memory, and carts do not survive process death. The wishlist is saved locally across modes.
- A horizontally scrolling category rail on Home with an explicit **All** entry. Selecting a card filters the products inline behind a short fade/slide transition, keeps the active card in view automatically, and clears itself when a mode change makes it invalid. A promotional carousel auto-advances, pauses while it is being dragged, and stays still under reduced motion. Wishlist, add-to-cart and bottom-navigation feedback share the timings in `ui/theme/Motion.kt`; the system "remove animations" setting collapses them.
- Debounced mode-specific search (including locally saved, clearable recent searches), category filters and sorting, product details, cart quantities and calculated subtotals.
- Device-local delivery addresses can be added, edited, selected and removed. Users may explicitly request approximate location while entering an address; a permission prompt appears only after that action. Coordinates are optional, are stored in app-private preferences with the address, and are not reverse-geocoded into a fabricated address. Address and search data are not encrypted or sent to a RushIt backend by the app; app backup is disabled.
- Checkout is a review-only screen with order placement disabled; Orders and Tracking provide honest unconfigured states. Notifications, live inventory, serviceability, delivery fees, payment and real order history are not connected.
- A Supabase Auth REST implementation behind `AuthRepository`, with session verification/refresh, Android Keystore-encrypted token storage, duplicate-request protection, and network/error handling. The sample catalog is behind `CatalogRepository`; no database schema is assumed or queried.

The reference-inspired wordmark is a **reconstruction**, not an approved production logo. Replace `res/drawable-nodpi/rushit_wordmark.webp` and the decorative splash artwork when an approved RushIt asset is available. Grocery, e-commerce, gift, rider and product illustrations are generated for this project. The rounded outline vectors are converted from the MIT-licensed free Hugeicons set. Font and icon licenses are in `licenses/`.

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

- A GitHub Actions run on this branch completed `./gradlew :app:testDebugUnitTest :app:assembleDebug` with JDK 17 and Android SDK 34: all 30 JVM unit tests passed, including the startup-timeout, cancelled-restore, category-selection and rapid mode-switching regressions, and a debug APK was produced. This is a headless Linux CI build, not a device test.
- `:app:lintDebug` finishes with one pre-existing `NewApi` error and 11 warnings. The error is `android:windowLightNavigationBar` in `res/values/themes.xml`, which requires API 27 while `minSdk` is 24. The platform ignores that attribute below API 27 and `MainActivity` applies the navigation-bar appearance at runtime, so it does not affect `assembleDebug`. Moving the item to a `values-v27` override or annotating it with `tools:targetApi="27"` would clear it.
- No emulator or attached device was available. Actual splash rendering, navigation, small-screen and large-text layouts, keyboard, location permission, cutouts, system bars, accessibility and screenshots remain unverified on device.
- No real Supabase project/SMS provider was supplied, so end-to-end SMS delivery, session restore against a real project, and server-side policies have not been tested.
- Catalog, wishlist, address, cart and order screens are not connected to a backend; addresses, recent searches, selected mode and wishlist are device-local, and carts do not survive process death. Order history, notifications, checkout, live availability and delivery fees are intentionally unavailable. Google sign-in and image search are not presented as nonfunctional controls.

### Suggested device smoke test

1. Launch with and without a real existing session, and on a slow/offline connection; confirm visible splash artwork and one correct route without a long hang.
2. Try short/invalid numbers, a valid number, failed and successful OTP requests, verification and resend with a configured provider. Confirm that Help, policy links, back/change-number and duplicate-request guards behave correctly.
3. Switch modes repeatedly and quickly; confirm the indicator slides once, the category rail clears an incompatible selection, and no blank frame appears. Tap category cards, confirm the products change under a short fade, and check that the selected card scrolls into view. Browse product details, search/filter/sort, wishlist, separate cart retention and computed totals. Review checkout and confirm that no payment or order is possible.
4. Add, edit, select and remove a local address; request, deny and grant approximate location, disable location services, and verify the manual-entry fallback. Open Orders, Tracking and Notifications.
5. Repeat on small and large displays, with the keyboard open and larger font scales, both gesture and three-button navigation, and display cutouts. Capture screenshots and compare them against the references.
