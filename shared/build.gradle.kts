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

    wasmJs {
        moduleName = "shared"
        browser {
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "shared.js"
                devServer = (devServer ?: org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        // Serve resources from the resources directory
                        add(projectDirPath + "/src/wasmJsMain/resources")
                    }
                }
            }
        }
        binaries.executable()
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
            implementation(libs.androidx.lifecycle.runtime.compose)

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

            // Multiplatform Settings
            implementation(libs.multiplatform.settings)
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

        // ── wasmJsMain: engine Ktor para Web (JS) ────────────────────────────
        val wasmJsMain by getting {
            dependencies {
                implementation(libs.ktor.client.js)
            }
        }

        // ── commonTest: testes unitários KMP ─────────────────────────────────
        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        // ── androidUnitTest: testes de UI Compose na JVM (Robolectric) ───────
        // Rodam no mesmo `testDebugUnitTest` dos testes unitários, sem emulador.
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.androidx.compose.ui.test.junit4)
                // Registra a ComponentActivity vazia usada por createComposeRule()
                implementation(libs.androidx.compose.ui.test.manifest)
                implementation(libs.androidx.test.ext.junit)
                implementation(libs.robolectric)
                implementation(libs.junit)
            }
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

    testOptions {
        // Robolectric precisa dos recursos/manifest mesclados (tema, ComponentActivity de teste)
        unitTests.isIncludeAndroidResources = true
    }

    // Manifest só de teste (ver comentário no arquivo): libera o minSdk do Kamel nos testes
    sourceSets.getByName("test").manifest.srcFile("src/androidUnitTest/AndroidManifest.xml")
}

// ── Separação entre testes unitários e testes de UI no CI ─────────────────────
// Por convenção, classes de teste de UI terminam em "UiTest".
//   ./gradlew :shared:testDebugUnitTest -PtestScope=unit  → só testes unitários
//   ./gradlew :shared:testDebugUnitTest -PtestScope=ui    → só testes de UI
//   ./gradlew :shared:testDebugUnitTest                   → todos (uso local)
val testScope = providers.gradleProperty("testScope").orNull
tasks.withType<Test>().configureEach {
    when (testScope) {
        "ui" -> filter.includeTestsMatching("*UiTest")
        "unit" -> filter.excludeTestsMatching("*UiTest")
    }
    // Lista cada teste (com status) no log do CI; falhas com stack trace completo
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

// ── Resolução de Conflitos KMP Wasm ──────────────────────────────────────────
configurations.all {
    // Impede que bibliotecas antigas arrastem a stdlib velha do Wasm, o que causava crash no linker do K2
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-wasm")
}
