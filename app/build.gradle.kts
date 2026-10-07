plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.wearstral"
    compileSdk = 36

    // release signing credentials come from ~/.gradle/gradle.properties
    // (wearstralReleaseStoreFile / StorePassword / KeyAlias / KeyPassword);
    // when they are absent the release APK is simply left unsigned
    val releaseStoreFile = providers.gradleProperty("wearstralReleaseStoreFile").orNull

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = providers.gradleProperty("wearstralReleaseStorePassword").get()
                keyAlias = providers.gradleProperty("wearstralReleaseKeyAlias").get()
                keyPassword = providers.gradleProperty("wearstralReleaseKeyPassword").get()
            }
        }
    }

    defaultConfig {
        applicationId = "dev.wearstral"
        minSdk = 26
        targetSdk = 36
        versionCode = 6
        versionName = "1.1.3"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (releaseStoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.wear.compose.material3)
    implementation(libs.androidx.wear.compose.navigation)
    implementation(libs.vosk.android)
}
