import java.util.Base64
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}
fun setting(name: String): String = providers.gradleProperty(name).orNull
    ?: localProperties.getProperty(name)
    ?: System.getenv(name)
    ?: ""
fun quoted(value: String): String = "\"" + value.replace("\\", "\\\\")
    .replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\""

val supabaseUrl = setting("SUPABASE_URL").trim()
val supabaseKey = setting("SUPABASE_ANON_KEY").trim()
// The Web OAuth client ID is the ID-token audience. The Android OAuth client is matched by Google
// from the package name plus the signing fingerprint, so it is never needed in this file.
val googleWebClientId = setting("GOOGLE_WEB_CLIENT_ID").trim()
// A client secret has no business being in an APK: Credential Manager only needs the public client ID.
require(setting("GOOGLE_CLIENT_SECRET").isBlank()) {
    "Never bundle a Google OAuth client secret in the app. Only GOOGLE_WEB_CLIENT_ID is required."
}
listOf(
    "SUPABASE_URL" to supabaseUrl,
    "SUPABASE_ANON_KEY" to supabaseKey,
    "GOOGLE_WEB_CLIENT_ID" to googleWebClientId,
).filter { it.second.isBlank() }.forEach { (name, _) ->
    logger.warn(
        "RushIt: $name is not set, so Google Sign-In will report itself as unconfigured at runtime. " +
            "Set it in local.properties, a Gradle property, or the environment. See docs/AUTH_SETUP.md."
    )
}
// A client app may only contain a publishable/anon key, never a privileged key.
val jwtPayload = runCatching {
    String(Base64.getUrlDecoder().decode(supabaseKey.split('.')[1]), Charsets.UTF_8)
}.getOrDefault("")
require(!supabaseKey.startsWith("sb_secret_") &&
    !Regex("\"role\"\\s*:\\s*\"service_role\"").containsMatchIn(jwtPayload)) {
    "Never bundle a Supabase secret or service-role key. Use an anon/publishable key."
}

android {
    namespace = "com.rushit.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.rushit.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SUPABASE_URL", quoted(supabaseUrl))
        buildConfigField("String", "SUPABASE_ANON_KEY", quoted(supabaseKey))
        buildConfigField("String", "TERMS_URL", quoted(setting("RUSHIT_TERMS_URL").trim()))
        buildConfigField("String", "PRIVACY_URL", quoted(setting("RUSHIT_PRIVACY_URL").trim()))
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", quoted(googleWebClientId))
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    testOptions { unitTests.isReturnDefaultValues = true }
    buildFeatures { compose = true; buildConfig = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.9" }
    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.3")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("io.ktor:ktor-client-core:2.3.11")
    implementation(platform("io.github.jan-tennert.supabase:bom:2.5.0"))
    implementation("io.github.jan-tennert.supabase:gotrue-kt")
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    // Used only for the account-deletion Edge Function; the service-role key stays server-side.
    implementation("io.github.jan-tennert.supabase:functions-kt")
    implementation("io.ktor:ktor-client-android:2.3.11")
    // supabase-kt models session expiry with kotlinx.datetime.Instant; pinned to the version the
    // 2.5.0 BOM was built against so the encrypted SessionManager and the SDK agree.
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")

    // Google Credential Manager sign-in. googleid supplies GetGoogleIdOption and the ID-token
    // credential; credentials-play-services-auth bridges to Google Play services on older devices.
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    // Lets the tests build real supabase-kt RestExceptions, whose message embeds request headers.
    // That is what makes "no credential ever reaches a user-facing string" an asserted property.
    testImplementation("io.ktor:ktor-client-mock:2.3.11")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
