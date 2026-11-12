plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.dungeon.widget"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.dungeon.widget"
        minSdk = 26
        targetSdk = 35
        versionCode = 14
        versionName = "0.6.1"
    }
    signingConfigs {
        create("persistent") {
            storeFile = file(System.getenv("SIGNING_KEYSTORE_PATH") ?: "missing-release-keystore.jks")
            storePassword = System.getenv("SIGNING_STORE_PASSWORD") ?: ""
            keyAlias = System.getenv("SIGNING_KEY_ALIAS") ?: "dungeon"
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD") ?: ""
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("persistent")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
