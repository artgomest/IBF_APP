plugins {
    alias(libs.plugins.android.application)
    kotlin("android") version "2.1.0"
    alias(libs.plugins.google.gms.google.services)
}

kotlin {
    jvmToolchain(21)
}

android {
    namespace = "com.ibf.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ibf.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 22
        versionName = "5.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
            isCrunchPngs = false
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation(libs.firebase.messaging.ktx)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.gridlayout)
    implementation(libs.androidx.swiperefreshlayout)

    implementation(libs.mpandroidchart)
    implementation(libs.glide)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

