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
        versionCode = 19
        versionName = "2.7.0"
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
        // Sign every variant (debug and release, local and CI) with the same
        // key so a newer build installs as an update over an older one.
        val sharedSigning = if (hasReleaseSigning) {
            signingConfigs.getByName("release")
        } else {
            signingConfigs.getByName("debug")
        }

        debug {
            signingConfig = sharedSigning
        }

        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = sharedSigning
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
    implementation("com.google.android.material:material:1.12.0")
}
