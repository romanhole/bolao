plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)       // kotlin.android — NÃO multiplatform aqui
    alias(libs.plugins.compose.compiler)
}

android {
    namespace   = "com.bolao.android"
    compileSdk  = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.bolao.android"
        minSdk        = libs.versions.android.minSdk.get().toInt()
        targetSdk     = libs.versions.android.targetSdk.get().toInt()
        versionCode   = 1
        versionName   = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("debug") // <-- Assinatura provisória para rodar release localmente
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        compose = true
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.koin.android)
    implementation(libs.koin.core)
}
