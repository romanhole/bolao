import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.library)
}

// ── Lógica de Leitura Segura de Variáveis ────────────────────────────────────
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

val supabaseUrlDev = localProperties.getProperty("SUPABASE_URL_DEV") ?: System.getenv("SUPABASE_URL_DEV") ?: ""
val supabaseAnonKeyDev = localProperties.getProperty("SUPABASE_ANON_KEY_DEV") ?: System.getenv("SUPABASE_ANON_KEY_DEV") ?: ""

val supabaseUrlProd = localProperties.getProperty("SUPABASE_URL_PROD") ?: System.getenv("SUPABASE_URL_PROD") ?: ""
val supabaseAnonKeyProd = localProperties.getProperty("SUPABASE_ANON_KEY_PROD") ?: System.getenv("SUPABASE_ANON_KEY_PROD") ?: ""

// As variáveis seguras estão prontas para serem usadas no bloco Android


kotlin {
    // ── Targets ────────────────────────────────────────────────────────────────
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
                }
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    // ── Source Sets ────────────────────────────────────────────────────────────
    sourceSets {

        // ── commonMain: toda a lógica compartilhada ──────────────────────────
        commonMain.dependencies {
            // Compose Multiplatform (UI compartilhada)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)

            // Ktor Client
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.neg)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.websockets) // WebSocket support — necessário para Supabase Realtime

            // kotlinx
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)

            // Lifecycle ViewModel (KMP-compatible desde 2.8.0)
            implementation(libs.androidx.lifecycle.viewmodel)

            // Material Icons Extended (rounded icons for UI)
            implementation(compose.materialIconsExtended)

            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Supabase
            implementation(libs.supabase.postgrest)  // PostgREST: queries + upsert
            implementation(libs.supabase.realtime)   // Realtime: subscrições ao vivo
            implementation(libs.supabase.auth)       // Auth: login, signup, sessão persistente

            // Kamel Image Loader (Compose Multiplatform)
            implementation(libs.kamel.image)
        }

        // ── androidMain: engine OkHttp para Android ────────────────────────
        androidMain.dependencies {
            // OkHttp: único engine Ktor com suporte a WebSockets no Android
            // O engine 'Android' (HttpURLConnection) não tem WebSocketCapability
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
            implementation(libs.androidx.activity.compose)
        }

        // ── iosMain: engine Ktor para iOS (Darwin) ───────────────────────────
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        // ── commonTest: testes unitários KMP ─────────────────────────────────
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "com.bolao.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        getByName("debug") {
            buildConfigField("String", "SUPABASE_URL", "\"${supabaseUrlDev}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${supabaseAnonKeyDev}\"")
        }
        getByName("release") {
            buildConfigField("String", "SUPABASE_URL", "\"${supabaseUrlProd}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${supabaseAnonKeyProd}\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
