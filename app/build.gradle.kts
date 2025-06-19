// build.gradle.kts (App:)

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services") // Importante para o Firebase
}

android {
    namespace = "com.ibf.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ibf.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // Importar o Firebase BOM primeiro para gerenciar as versões
    // SEMPRE use 'platform()' ao importar o BOM
    implementation(platform(libs.firebase.bom))

    // As dependências individuais do Firebase, agora gerenciadas pelo BOM,
    // não precisam mais da referência de versão aqui.
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)

    // Suas outras dependências
    implementation(libs.androidx.core.ktx)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.gridlayout)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)
    implementation(libs.mpandroidchart)


    // Dependências de teste
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}