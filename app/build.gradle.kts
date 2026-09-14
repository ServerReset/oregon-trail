import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

// CI provides a base64-decoded keystore at this path plus passwords in env vars.
val ciKeystore = rootProject.file("ci-release.keystore")
val hasReleaseSigning = keystorePropertiesFile.exists() || ciKeystore.exists()

android {
    namespace = "com.oregontrail.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.oregontrail.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 7
        versionName = "1.6.0"
    }

    if (hasReleaseSigning) {
        signingConfigs {
            create("release") {
                if (keystorePropertiesFile.exists()) {
                    storeFile = file(keystoreProperties.getProperty("storeFile"))
                    storePassword = keystoreProperties.getProperty("storePassword")
                    keyAlias = keystoreProperties.getProperty("keyAlias")
                    keyPassword = keystoreProperties.getProperty("keyPassword")
                } else {
                    storeFile = ciKeystore
                    storePassword = System.getenv("RELEASE_STORE_PASSWORD") ?: ""
                    keyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: ""
                    keyPassword = System.getenv("RELEASE_KEY_PASSWORD") ?: ""
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Use the private release key when available; otherwise fall back to
            // the debug key so CI can still produce an installable APK.
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = false
        buildConfig = true
    }
}

dependencies {
    implementation(project(":engine"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
