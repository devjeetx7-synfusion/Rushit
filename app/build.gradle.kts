import java.util.Properties

import groovy.json.JsonSlurper

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.gms.google-services") apply false
}

val rushItApplicationId = "com.rushit.app"

val firebaseConfigured = file("google-services.json").isFile
if (firebaseConfigured) {
    apply(plugin = "com.google.gms.google-services")
} else {
    logger.warn(
        "RushIt: app/google-services.json is missing, so Firebase Authentication is not configured " +
            "for this build. Sign-in will report itself as unconfigured at runtime.",
    )
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

val firebaseWebClientId = if (firebaseConfigured) {
    runCatching {
        val parsed = JsonSlurper().parse(file("google-services.json")) as? Map<*, *>
        val clients = parsed?.get("client") as? List<*>
        val rushItClient = clients?.filterIsInstance<Map<*, *>>()?.firstOrNull { client ->
            val info = client["client_info"] as? Map<*, *>
            val androidInfo = info?.get("android_client_info") as? Map<*, *>
            androidInfo?.get("package_name") == rushItApplicationId
        }
        val oauthClients = rushItClient?.get("oauth_client") as? List<*>
        oauthClients?.filterIsInstance<Map<*, *>>()
            ?.firstOrNull { it["client_type"] == 3 }
            ?.get("client_id") as? String
    }.getOrNull()?.trim().orEmpty()
} else ""

val googleWebClientId = setting("GOOGLE_WEB_CLIENT_ID").trim().ifBlank { firebaseWebClientId }
if (firebaseWebClientId.isNotBlank() && setting("GOOGLE_WEB_CLIENT_ID").isBlank()) {
    logger.lifecycle(
        "RushIt: GOOGLE_WEB_CLIENT_ID is not set; using the Web OAuth client Firebase registered " +
            "for $rushItApplicationId in app/google-services.json.",
    )
}

require(setting("GOOGLE_CLIENT_SECRET").isBlank()) {
    "Never bundle a Google OAuth client secret in the app. Only GOOGLE_WEB_CLIENT_ID is required."
}

require(googleWebClientId.isBlank() ||
    Regex("^[0-9]+-[a-z0-9]+\\.apps\\.googleusercontent\\.com$").matches(googleWebClientId)) {
    "GOOGLE_WEB_CLIENT_ID must be the Web OAuth client ID ending in .apps.googleusercontent.com."
}

val versionCodeInput = setting("RUSHIT_VERSION_CODE").trim().ifBlank { "1" }
val ciVersionCode = versionCodeInput.toIntOrNull()
require(Regex("^[1-9][0-9]*$").matches(versionCodeInput) &&
    ciVersionCode != null && ciVersionCode <= 2_100_000_000) {
    "RUSHIT_VERSION_CODE must be an integer from 1 to 2100000000 (Android's versionCode limit)."
}

val testSigningSettings = listOf(
    "RUSHIT_TEST_KEYSTORE_PATH",
    "RUSHIT_TEST_KEYSTORE_PASSWORD",
    "RUSHIT_TEST_KEY_ALIAS",
    "RUSHIT_TEST_KEY_PASSWORD",
).associateWith(::setting)
val useStableTestSigning = testSigningSettings.values.any { it.isNotBlank() }
if (useStableTestSigning) {
    val missing = testSigningSettings.filterValues { it.isBlank() }.keys
    require(missing.isEmpty()) {
        "Incomplete persistent test signing configuration. Set all of: ${missing.joinToString()}."
    }
}
val testKeystoreFile = if (useStableTestSigning) {
    file(testSigningSettings.getValue("RUSHIT_TEST_KEYSTORE_PATH")).canonicalFile.also {
        require(it.isFile) { "The configured persistent test keystore does not exist or is not a file." }
        require(!it.toPath().startsWith(rootProject.projectDir.canonicalFile.toPath())) {
            "Keep the persistent test keystore outside the checkout; use RUNNER_TEMP in CI."
        }
    }
} else null

android {
    namespace = "com.rushit.app"
    compileSdk = 34

    defaultConfig {
        applicationId = rushItApplicationId
        minSdk = 24
        targetSdk = 34
        versionCode = ciVersionCode
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "TERMS_URL", quoted(setting("RUSHIT_TERMS_URL").trim()))
        buildConfigField("String", "PRIVACY_URL", quoted(setting("RUSHIT_PRIVACY_URL").trim()))
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", quoted(googleWebClientId))
        buildConfigField("boolean", "FIREBASE_CONFIGURED", firebaseConfigured.toString())
    }
    signingConfigs {
        if (useStableTestSigning) {
            getByName("debug") {
                storeFile = testKeystoreFile
                storePassword = testSigningSettings.getValue("RUSHIT_TEST_KEYSTORE_PASSWORD")
                keyAlias = testSigningSettings.getValue("RUSHIT_TEST_KEY_ALIAS")
                keyPassword = testSigningSettings.getValue("RUSHIT_TEST_KEY_PASSWORD")
            }
        }
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

    // Firebase Authentication: Google Sign-In and phone OTP.
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Google Credential Manager sign-in.
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("org.robolectric:robolectric:4.13")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
