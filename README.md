# RushIt

RushIt is an initial Android/Jetpack Compose quick-commerce app, built around the supplied cream, coral and peach reference. The repository originally contained only this README; there was no existing Android project, approved logo or backend configuration.

## What is included

- A session-aware, animated splash with visible first-frame artwork and a bounded session check that routes a restored account straight past the login screen; a Google-only login and a one-time profile onboarding flow.
- A clearly marked **sample store** catalog that every signed-in shopper sees. Food & Grocery uses the existing sample products; E-commerce has two non-purchasable preview listings; Gifts & More has preview categories but no configured inventory. Prices are illustrative and none of these modes places orders.
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

The debug APK, when the build succeeds, is at `app/build/outputs/apk/debug/app-debug.apk`. The project uses Gradle 8.7, Android Gradle Plugin 8.5.2, Kotlin 1.9.24 and Compose compiler 1.5.14.

## Manually build a test APK

The **RushIt User Build** GitHub Actions workflow runs on pull requests, on pushes to `main`, and when manually dispatched. It runs `:app:testDebugUnitTest`, then builds the `:app` debug variant with Android's default debug signing, verifies the APK's package ID, positive version code and signature, and uploads `app-debug.apk` as the **RushIt-Test-APK** artifact plus the unit-test report. No signing secrets or certificate fingerprint variable are required. The workflow does not access or delete any previously configured signing secrets or keystores.

### Signing identity and updates

The application ID remains `com.rushit.app` and the current `versionCode` is `1`. GitHub-hosted runners are temporary, so their default debug keystore can differ between runs. **APK signing identities are not stable across builds**, and an APK from this workflow may not update a previous installation. A locally built debug APK or a separately signed release APK can also use a different certificate.

Android accepts an update only when the application ID and signing certificate match the installed app and its `versionCode` is **at least** the installed version. The workflow cannot determine what is installed on a device. It prints the public debug certificate SHA-256 fingerprint in the verification step so builds can be compared. The workflow never uninstalls an app; uninstalling an incompatible installation can remove its data. Preserve any existing test keystore and backup if you may need to restore the previous signing identity later.

Optional public repository Actions variables `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `GOOGLE_WEB_CLIENT_ID`, `RUSHIT_TERMS_URL`, and `RUSHIT_PRIVACY_URL` use the existing build configuration. Their values are embedded in the APK and are therefore public: only use a Supabase **anon/publishable** key, never a service-role key or another secret. Without them the app still builds and still opens, but it reports that Google Sign-In or the account service is not configured instead of signing anybody in.

### Run and download

1. Under **Actions → Build test APK**, choose **Run workflow**, select a branch containing the updated workflow, and run it. Select `main` after the change is merged. Only run code from a branch you trust.
2. Open the completed successful run and download **RushIt-Test-APK** from **Artifacts**. Unzip it to obtain `app-debug.apk`. Failed builds or validation do not upload an APK.
3. Before updating a device, compare its installed version (`adb shell dumpsys package com.rushit.app | grep versionCode`) with the APK's version (`aapt dump badging app-debug.apk`) and compare signing certificates using `apksigner verify --verbose --print-certs`. Use `adb install -r app-debug.apk` only when the identity and version are compatible.

## Configure Google Sign-In

Google Sign-In is the only authentication method in this release. No credential is committed; see
[`docs/AUTH_SETUP.md`](docs/AUTH_SETUP.md) for the full Google Cloud Console and Supabase setup, and
add the following to the **ignored** root `local.properties` (or supply them as Gradle
properties/environment variables in your own build environment):

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
property is supplied or a recognizable service-role/`sb_secret_` key appears in any credential value.

The manual steps that remain, all documented in `docs/AUTH_SETUP.md`:

1. Configure the OAuth consent screen and add test users while the publishing status is *Testing*.
2. Create a **Web application** OAuth client (the audience) and an **Android** OAuth client for
   `com.rushit.app` with debug, release and Play App Signing SHA-1 fingerprints.
3. Enable the Google provider in Supabase with that client ID and secret.
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

- The editing sandbox has no JDK, Android SDK, PostgreSQL or Supabase CLI, so builds are verified by the `RushIt User Build` workflow on GitHub Actions rather than locally. `supabase/tests/rls_isolation.sql` needs the Supabase CLI and Docker and has **not** been executed. Run `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` and `supabase test db` in a configured environment before shipping.
- The previously reported API 27 lint issue was addressed by moving `windowLightNavigationBar` into a `values-v27` resource; lint still needs to be rerun.
- No emulator or attached device was available. Actual splash rendering, navigation, small-screen and large-text layouts, keyboard, location permission, cutouts, system bars, accessibility and screenshots remain unverified on device.
- No Google Cloud OAuth configuration, Supabase project or signing credential was supplied, so **no end-to-end Google sign-in has been performed**: token exchange against a real Supabase project, session restore against a real project, live account deletion, and the applied RLS policies are all unverified. The app reports its own unconfigured state honestly instead of simulating success.
- Catalog, wishlist, address, cart and order screens are not connected to a backend; addresses, recent searches, selected mode, wishlist and demo orders are device-local, and carts do not survive process death. Demo orders survive app restarts, but nothing is sent anywhere.
- The delivery journey is a **demo preview**. There is no order service, rider directory, live location, ETA, payment, inventory reservation or real delivery, and the UI states that explicitly instead of inventing a rider, rating or countdown.

### Suggested device smoke test

1. Launch with and without a real existing session, and on a slow/offline connection; confirm visible splash artwork and one correct route without a long hang, and that a restored session never shows the login screen first. From Account, exercise Edit profile, Sign out and Delete account.
2. Tap **Continue with Google**: confirm the real account picker opens, that backing out shows no error, that a device with no Google account is reported as such, and that a first sign-in lands on onboarding with the Google name and photo pre-filled. Sign in again after killing the app and confirm the customized name survives. Confirm Help, the policy links and the duplicate-tap guard behave correctly.
3. Switch modes repeatedly and quickly; confirm the indicator slides once, the category rail clears an incompatible selection, and no blank frame appears. Tap category cards, confirm the products change under a short fade, and check that the selected card scrolls into view. Browse product details, search/filter/sort, wishlist, separate cart retention and computed totals.
4. Add, edit, select and remove a local address; request, deny and grant approximate location, disable location services, and verify the manual-entry fallback. Open Orders, Tracking and Notifications.
5. From the cart, open checkout, add an address, change quantities, place a demo order, and walk the journey: confirmation, packing, rider-ready, route, arrival and delivered. Confirm the celebration does not replay when you reopen the order, that Cancel is only offered before pickup, and that every stage is labelled as a demo.
6. Repeat on small and large displays, with the keyboard open and larger font scales, both gesture and three-button navigation, and display cutouts. Capture screenshots and compare them against the references.

### Artwork and audio notes

- `order_packing.webp`: supplied 1448 × 1086 opaque PNG, resized to 1200 × 900 (26,432 bytes), no crop; displayed with fit scaling. It remains static because its objects are not separate layers.
- `order_rider_ready.webp`: 781 × 602 crop of the supplied screen's stationary rider illustration (29,906 bytes); used only for the stationary rider scene. The existing on-the-way route illustration is retained.
- `order_confirmation.wav`: a 320 ms, mono, 22.05 kHz two-note sine chime synthesized locally for this project; no external sample is used. Device playback and audio policy behavior still need physical-device testing.
