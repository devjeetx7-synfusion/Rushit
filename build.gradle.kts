plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22" apply false
    // Firebase configures the Android app from app/google-services.json. The version is declared here
    // (not in the module) so the app module can decide whether to apply it — see app/build.gradle.kts.
    id("com.google.gms.google-services") version "4.4.2" apply false
}
