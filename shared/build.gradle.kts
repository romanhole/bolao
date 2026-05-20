plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.library)
}

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
            // libs.supabase.auth → adicionar quando implementar autenticação
        }

        // ── androidMain: engine Ktor para Android ────────────────────────────
        androidMain.dependencies {
            implementation(libs.ktor.client.android)
            implementation(libs.koin.android)
            implementation(libs.androidx.activity.compose)
        }

        // ── iosMain: engine Ktor para iOS (Darwin) ───────────────────────────
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

android {
    namespace = "com.bolao.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
