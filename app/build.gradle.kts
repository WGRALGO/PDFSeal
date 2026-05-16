import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Release signing is configured only if key.properties exists (gitignored).
// Fresh clones and CI without the key still build (release falls back unsigned).
val keystorePropertiesFile = rootProject.file("key.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}
val hasReleaseSigning = keystoreProperties.getProperty("storeFile") != null

android {
    namespace = "org.thewealthgapresolutionalgorithm.pdfseal"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.thewealthgapresolutionalgorithm.pdfseal"
        minSdk = 24
        targetSdk = 34
        versionCode = 4
        versionName = "0.3.0"
        vectorDrawables { useSupportLibrary = true }

        buildConfigField(
            "String",
            "SIGNING_CERT_SHA256",
            "\"F8:D7:4E:09:42:74:10:8F:B9:EB:A8:06:AE:61:0B:39:" +
                "BA:E0:9F:39:F6:C9:F0:41:25:4E:38:03:56:13:7E:D5\"",
        )
        buildConfigField(
            "String",
            "SOURCE_URL",
            "\"https://github.com/WGRALGO/PDFSeal\"",
        )
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Tesseract memory-maps the trained data; it must NOT be compressed in the APK.
    androidResources {
        noCompress += "traineddata"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // PDF engine — official Artifex MuPDF Android (AGPL; see
    // THIRD_PARTY_LICENSES.md). Prebuilt AAR from maven.ghostscript.com, no NDK.
    implementation("com.artifex.mupdf:fitz:1.27.1")

    // Offline OCR (Apache-2.0). Prebuilt native libs via JitPack.
    implementation("cz.adaptech.tesseract4android:tesseract4android:4.9.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
