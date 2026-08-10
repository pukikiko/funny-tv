plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Release CI passes the tag through as -PappVersionName / -PappVersionCode so the
// APK reports the same version Obtainium reads off the GitHub release. Local and
// PR builds fall back to the placeholder below.
val appVersionName = (findProperty("appVersionName") as String?) ?: "0.0.0"
val appVersionCode = (findProperty("appVersionCode") as String?)?.toInt() ?: 1

android {
    namespace = "com.pukikiko.funny"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.pukikiko.funny"
        minSdk = 26
        targetSdk = 34
        versionCode = appVersionCode
        versionName = appVersionName
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // No versionNameSuffix on either flavor: Obtainium checks the version baked
    // into the APK against the release tag, and "1.2.3-tv" does not match "1.2.3".
    // The flavor lives in the APK file name instead.
    flavorDimensions += "formFactor"
    productFlavors {
        create("tv") {
            dimension = "formFactor"
        }
        create("mobile") {
            dimension = "formFactor"
            // What Android Studio selects on a fresh sync; switch in the
            // Build Variants panel to run the tv build.
            isDefault = true
        }
    }

    // Public test key, committed to the repo on purpose. Every build is signed
    // with the same certificate so APKs upgrade-install over each other.
    // NEVER use this for a real release: the private key is public.
    signingConfigs {
        create("testkey") {
            storeFile = file("testkey.jks")
            storePassword = "android"
            keyAlias = "testkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("testkey")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("testkey")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // Compose for TV & standard Compose
    implementation("androidx.tv:tv-foundation:1.0.0-alpha10")
    implementation("androidx.tv:tv-material:1.0.0-alpha10")
    implementation("androidx.compose.material:material-icons-extended:1.5.4")
    implementation("androidx.compose.foundation:foundation:1.5.4")
    implementation("androidx.compose.animation:animation:1.5.4")

    // Touch-native Material for the mobile settings screen. TV Material only
    // reacts to focus, so its buttons do nothing on a touchscreen.
    "mobileImplementation"("androidx.compose.material3:material3:1.1.2")

    // Media3 (ExoPlayer)
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")

    // Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
}
