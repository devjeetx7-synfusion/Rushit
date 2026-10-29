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

## Manually build a persistently signed test APK

The **Build test APK** GitHub Actions workflow runs only when manually dispatched. It restores a test keystore from Actions secrets, signs the `:app` debug variant, checks the package ID and version, verifies its signature and certificate, and uploads only the APK as `RushIt-Test-APK`. It never creates or rotates a key. The workflow has read-only repository permissions and deletes the restored keystore from the temporary runner directory after the job.

### Signing identity and updates

The application ID is `com.rushit.app` and the current `versionCode` is `1`. Android accepts an update only when its application ID and signing certificate match the installed app and its `versionCode` is **at least** the installed version. Do not decrease `versionCode` after distributing a higher one. The workflow checks for a positive version code, but cannot know what is installed on a device.

An ordinary local debug build uses that developer's default Android debug keystore unless explicitly configured otherwise; a separately signed release APK will generally use another certificate. Neither can necessarily be updated by this workflow's test APK. If an existing test installation must be updated, **first recover its exact non-production signing keystore and credentials** and use that identity for the test secrets. A new key cannot update an app signed by a different key. This test workflow cannot update an installation signed with a production key; use the separately managed production signing process instead. Do not uninstall the existing app to work around a mismatch.

### One-time setup by a repository administrator

Before creating anything, check whether a test signing identity already exists (including an organization-level Actions secret or a previously distributed APK). If one exists, recover and reuse the original keystore, alias and passwords from secure backups. Never overwrite the existing signing secrets or change the pinned fingerprint as a routine fix. GitHub secrets cannot be read back after creation, so keep an encrypted, access-controlled backup of the keystore and the credentials in a separate secure password manager or backup system. Do **not** use production signing credentials.

Configure these **repository Actions secrets** under **Settings → Secrets and variables → Actions → Secrets**:

| Secret | Value |
| --- | --- |
| `TEST_KEYSTORE_BASE64` | Base64 encoding of the original binary test keystore |
| `TEST_KEYSTORE_PASSWORD` | Keystore password |
| `TEST_KEY_ALIAS` | Alias of the signing private key |
| `TEST_KEY_PASSWORD` | Password for that key |

Also configure the **repository Actions variable** `TEST_CERT_SHA256` under the **Variables** tab. It is the public certificate's SHA-256 fingerprint (64 hex characters; colons are also accepted), not a private key. The workflow requires this independent pin and fails if the restored keystore or APK does not match it. Protect this variable from accidental changes along with the secrets.

On a trusted private machine with JDK 17 and the GitHub CLI, first check with the organization administrator for inherited secrets; `gh secret list --repo` only checks repository secrets. If any signing secrets are inherited, retain them and add only missing values from the same original identity rather than running the bulk provisioning below. Otherwise, run this preflight in a separate Bash session and keep the session open for the commands below. It refuses to proceed if repository signing settings already exist or cannot be inspected:

```bash
set +x                    # Do not trace commands containing secrets.
set -euo pipefail
repo='devjeetx7-synfusion/Rushit'  # Change this if using a fork.

check_test_config_absent() {
  local secrets variables
  if ! secrets="$(gh secret list --app actions --repo "$repo")" ||
     ! variables="$(gh variable list --repo "$repo")"; then
    echo 'Cannot inspect repository settings; stop and ask an administrator.' >&2
    return 1
  fi
  if grep -Eq '^TEST_(KEYSTORE_BASE64|KEYSTORE_PASSWORD|KEY_ALIAS|KEY_PASSWORD)([[:space:]]|$)' <<< "$secrets" ||
     grep -Eq '^TEST_CERT_SHA256([[:space:]]|$)' <<< "$variables"; then
    echo 'Test signing settings already exist; recover them instead of replacing them.' >&2
    return 1
  fi
}
check_test_config_absent
```

If there is **no existing test identity** and no need to update an older installation signed by another key, generate one once. This command refuses to overwrite an existing file:

```bash
umask 077
signing_dir="$HOME/.rushit-test-signing"  # Outside the repository.
mkdir -p "$signing_dir"
chmod 700 "$signing_dir"
keystore="$signing_dir/rushit-test.jks"
if [[ -e "$keystore" || -L "$keystore" ]]; then
  echo 'Keystore already exists; stop and reuse it.' >&2
  exit 1
fi

# Choose strong passwords and a stable alias; record them in a password manager.
IFS= read -r -s -p 'Test keystore password (at least 6 characters): ' TEST_KEYSTORE_PASSWORD; printf '\n'
IFS= read -r -s -p 'Confirm keystore password: ' store_confirmation; printf '\n'
IFS= read -r -s -p 'Test key password (at least 6 characters): ' TEST_KEY_PASSWORD; printf '\n'
IFS= read -r -s -p 'Confirm key password: ' key_confirmation; printf '\n'
IFS= read -r -s -p 'Test key alias: ' TEST_KEY_ALIAS; printf '\n'
if (( ${#TEST_KEYSTORE_PASSWORD} < 6 || ${#TEST_KEY_PASSWORD} < 6 )) || [[ -z "$TEST_KEY_ALIAS" ]] ||
   [[ "$TEST_KEYSTORE_PASSWORD" != "$store_confirmation" || "$TEST_KEY_PASSWORD" != "$key_confirmation" ]]; then
  echo 'Passwords or alias are invalid; no keystore was created.' >&2
  exit 1
fi
unset store_confirmation key_confirmation
export TEST_KEYSTORE_PASSWORD TEST_KEY_PASSWORD TEST_KEY_ALIAS
keytool -genkeypair -keystore "$keystore" -storetype JKS \
  -alias "$TEST_KEY_ALIAS" -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass:env TEST_KEYSTORE_PASSWORD -keypass:env TEST_KEY_PASSWORD \
  -dname 'CN=RushIt Test'
```

Back up the resulting keystore, alias and both passwords securely **before** continuing. Do not commit them, attach them to an artifact, or send them in chat.

Alternatively, if an identity already exists but repository secrets have not been configured, **do not generate a key**. After the preflight above, load its original file and credentials instead (replace the example path):

```bash
keystore='/secure/path/to/original-test.jks'
IFS= read -r -s -p 'Existing keystore password: ' TEST_KEYSTORE_PASSWORD; printf '\n'
IFS= read -r -s -p 'Existing key password: ' TEST_KEY_PASSWORD; printf '\n'
IFS= read -r -s -p 'Existing key alias: ' TEST_KEY_ALIAS; printf '\n'
export TEST_KEYSTORE_PASSWORD TEST_KEY_PASSWORD TEST_KEY_ALIAS
```

In either case, after confirming no other administrator is setting these values concurrently, use the same shell session to set the secrets and public fingerprint:

```bash
check_test_config_absent  # Stop rather than overwrite if settings appeared meanwhile.
if ! fingerprint="$(keytool -exportcert -keystore "$keystore" \
    -storepass:env TEST_KEYSTORE_PASSWORD -alias "$TEST_KEY_ALIAS" 2>/dev/null | sha256sum | cut -d ' ' -f 1)"; then
  echo 'Unable to read the original certificate; nothing was uploaded.' >&2
  exit 1
fi
base64 < "$keystore" | tr -d '\n' | gh secret set TEST_KEYSTORE_BASE64 --repo "$repo"
printf '%s' "$TEST_KEYSTORE_PASSWORD" | gh secret set TEST_KEYSTORE_PASSWORD --repo "$repo"
printf '%s' "$TEST_KEY_ALIAS" | gh secret set TEST_KEY_ALIAS --repo "$repo"
printf '%s' "$TEST_KEY_PASSWORD" | gh secret set TEST_KEY_PASSWORD --repo "$repo"
gh variable set TEST_CERT_SHA256 --repo "$repo" --body "$fingerprint"
unset TEST_KEYSTORE_PASSWORD TEST_KEY_PASSWORD TEST_KEY_ALIAS
```

If setup is only partially complete, do not rerun the bulk upload or regenerate the key. Have an administrator add **only the missing values** from the original secure backup; do not replace the values that already exist. If the original key or passwords are lost, recovery or a deliberate signing-key migration requires owner intervention.

Optional public repository Actions variables `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `RUSHIT_TERMS_URL`, and `RUSHIT_PRIVACY_URL` use the existing build configuration. Their values are embedded in the APK and are therefore public: only use a Supabase **anon/publishable** key, never a service-role key or another secret. Without these variables the sample store still opens, but phone login is unconfigured.

### Run and download

1. Make sure the workflow file is present on the default branch; GitHub registers `workflow_dispatch` workflows from that branch. Under **Actions → Build test APK**, choose **Run workflow**, select a trusted branch containing the workflow, and run it. Only dispatch branches whose code you trust with the signing secrets.
2. Open the completed successful run and download **RushIt-Test-APK** from **Artifacts**. Unzip it to obtain `app-debug.apk`. A missing secret, a mismatched key, a failed build, or a failed validation stops the run before upload.
3. Before updating a device, compare its installed version (`adb shell dumpsys package com.rushit.app | grep versionCode`) with the APK's version (`aapt dump badging app-debug.apk`). Use `apksigner verify --verbose --print-certs app-debug.apk` to inspect the public fingerprint, then install with `adb install -r app-debug.apk` only when the identity and version are compatible. The workflow never uninstalls an app.

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

- During workflow implementation, Actionlint/ShellCheck and signing/validation checks with disposable fixture APKs passed. A real GitHub Actions run and full Gradle build remain unverified until the required signing configuration is available and the workflow succeeds.
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
