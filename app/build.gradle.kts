import java.util.Base64
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
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

val supabaseKey = setting("SUPABASE_ANON_KEY").trim()
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

        buildConfigField("String", "SUPABASE_URL", quoted(setting("SUPABASE_URL").trim()))
        buildConfigField("String", "SUPABASE_ANON_KEY", quoted(supabaseKey))
        buildConfigField("String", "TERMS_URL", quoted(setting("RUSHIT_TERMS_URL").trim()))
        buildConfigField("String", "PRIVACY_URL", quoted(setting("RUSHIT_PRIVACY_URL").trim()))
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

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
