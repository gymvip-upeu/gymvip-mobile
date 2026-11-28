import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.googleServices) apply false
    alias(libs.plugins.firebaseCrashlytics) apply false
    alias(libs.plugins.firebasePerf) apply false
}

val firebaseConfigPresent = listOf("google-services.json", "src/debug/google-services.json", "src/release/google-services.json")
    .any { file(it).isFile }
val firebaseEnabled = providers.gradleProperty("gymvip.firebase").orNull?.toBooleanStrictOrNull() ?: firebaseConfigPresent
if (firebaseEnabled) {
    require(firebaseConfigPresent) { "Firebase requiere google-services.json del proyecto del equipo." }
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
    apply(plugin = "com.google.firebase.firebase-perf")
}
val localBuildProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
}
val debugBackendUrl = providers.gradleProperty("gymvip.backendUrl")
    .orElse(localBuildProperties.getProperty("gymvip.backendUrl", "https://gymvip-backend.onrender.com")).get()
require(debugBackendUrl.matches(Regex("https?://[A-Za-z0-9.:-]+"))) { "gymvip.backendUrl debe ser una URL HTTP(S) sin credenciales." }
val releaseBackendUrl = providers.gradleProperty("gymvip.releaseBackendUrl").orElse("https://gymvip-backend.onrender.com").get()
require(releaseBackendUrl.matches(Regex("https://[A-Za-z0-9.:-]+"))) { "gymvip.releaseBackendUrl debe ser HTTPS sin credenciales." }
val debugAppId = providers.gradleProperty("gymvip.debugApplicationId").orElse("com.gymvip.mobile").get()
require(debugAppId == "com.gymvip.mobile" || debugAppId.startsWith("com.gymvip.mobile.")) { "Identificador Android inválido" }
val receptionNumber = providers.gradleProperty("gymvip.receptionWhatsApp").orElse("+51905436989").get()
require(receptionNumber.matches(Regex("[+]?[0-9]{8,15}"))) { "Número de recepción inválido" }

kotlin {
    targets.configureEach {
        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
            }
        }
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.logging)

            implementation(libs.sqldelight.coroutines.extensions)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.security.crypto)
            implementation(libs.androidx.biometric)
            implementation(libs.androidx.camera.core)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
            implementation(libs.mlkit.barcode.scanning)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.kotlinx.coroutines.android)

            // Firebase BoM & Official Services
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.analytics)
            implementation(libs.firebase.crashlytics)
            implementation(libs.firebase.perf)
            implementation(libs.firebase.messaging)
            implementation(libs.firebase.config)
            implementation(libs.firebase.appcheck.playintegrity)
            implementation(libs.firebase.appcheck.debug)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.sqldelight.sqlite.driver)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.ktor.client.mock)
            }
        }
        // JDBC es de JVM: compartir estas pruebas entre Android host y escritorio, nunca iOS.
        val jvmTest by creating {
            dependsOn(commonTest)
            dependencies { implementation(libs.sqldelight.sqlite.driver) }
        }
        val desktopTest by getting { dependsOn(jvmTest) }
        val androidUnitTest by getting { dependsOn(jvmTest) }
        val androidInstrumentedTest by getting {
            dependencies {
                implementation("androidx.test:runner:1.6.2")
                implementation("junit:junit:4.13.2")
            }
        }
    }
}

android {
    namespace = "com.gymvip.mobile"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.gymvip.mobile"
        minSdk = 24
        targetSdk = 34
        buildConfigField("boolean", "FIREBASE_ENABLED", firebaseEnabled.toString())
        buildConfigField("String", "RECEPTION_WHATSAPP", "\"$receptionNumber\"")
        versionCode = 7
        versionName = "1.1.5"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }

    buildFeatures {
        buildConfig = true
    }

    val keystorePropsFile = rootProject.file("keystore.properties")
    val keystoreProps = Properties()
    if (keystorePropsFile.exists()) {
        keystorePropsFile.inputStream().use { keystoreProps.load(it) }
    }

    signingConfigs {
        getByName("debug") {
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
        create("release") {
            val rawPath = System.getenv("GYMVIP_KEYSTORE_PATH")
                ?: keystoreProps.getProperty("storeFile")
                ?: "release.keystore"
            val resolvedFile = if (file(rawPath).isAbsolute) file(rawPath) else rootProject.file(rawPath)

            val storePwd = System.getenv("GYMVIP_KEYSTORE_PASSWORD")
                ?: keystoreProps.getProperty("storePassword")
            val keyAliasProp = System.getenv("GYMVIP_KEY_ALIAS")
                ?: keystoreProps.getProperty("keyAlias")
            val keyPwd = System.getenv("GYMVIP_KEY_PASSWORD")
                ?: keystoreProps.getProperty("keyPassword")

            storeFile = resolvedFile.takeIf { it.exists() }
            storePassword = storePwd
            keyAlias = keyAliasProp
            keyPassword = keyPwd
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            buildConfigField("String", "DEFAULT_SERVER_URL", "\"$releaseBackendUrl\"")
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            isDebuggable = true
            applicationIdSuffix = debugAppId.removePrefix("com.gymvip.mobile").ifBlank { null }
            signingConfig = signingConfigs.getByName("debug")
            buildConfigField("String", "DEFAULT_SERVER_URL", "\"$debugBackendUrl\"")
        }
    }

    // Valida también release al invocar tareas agregadas como build o assemble.
    gradle.taskGraph.whenReady {
        // La importación del IDE genera recursos/modelos de ambas variantes: eso no
        // produce un APK release y no debe exigir credenciales de firma.
        if (allTasks.any { it.project == project && it.name.matches(Regex("(assemble|bundle|package|validateSigning).*Release")) }) {
            val releaseSigning = signingConfigs.getByName("release")
            check(releaseSigning.storeFile?.isFile == true &&
                !releaseSigning.storePassword.isNullOrBlank() &&
                !releaseSigning.keyAlias.isNullOrBlank() &&
                !releaseSigning.keyPassword.isNullOrBlank()) {
                "La compilación release requiere el keystore y las credenciales de producción."
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

sqldelight {
    databases {
        create("GymVipDatabase") {
            packageName.set("com.gymvip.mobile.data.local")
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.gymvip.mobile.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.gymvip.mobile"
            packageVersion = "1.1.0"
        }
    }
}

tasks.register("validateReleaseConfiguration") {
    description = "Valida que las compilaciones de producción no dependan de localhost o emuladores"
    group = "verification"
    doLast {
        require(firebaseEnabled && (file("google-services.json").isFile || file("src/release/google-services.json").isFile)) {
            "Release requiere configuración Firebase real del equipo (google-services.json)."
        }
        val releaseSignature = android.signingConfigs.getByName("release")
        require(releaseSignature.storeFile?.isFile == true &&
                releaseSignature.storeFile?.canonicalPath != android.signingConfigs.getByName("debug").storeFile?.canonicalPath &&
                !releaseSignature.storePassword.isNullOrBlank() &&
                !releaseSignature.keyAlias.isNullOrBlank() &&
                !releaseSignature.keyPassword.isNullOrBlank()) {
            "Release requiere el keystore privado del equipo, no la firma debug."
        }
        val configuredUrl = releaseBackendUrl
        if (configuredUrl.contains("localhost") || configuredUrl.contains("10.0.2.2") || configuredUrl.contains("127.0.0.1")) {
            throw GradleException("ERROR DE SEGURIDAD CRÍTICO: La compilación de release no puede apuntar a IP local o emulador ($configuredUrl)")
        }
        logger.lifecycle("[SEGURIDAD CI/CD] URL de servidor release verificada para producción: $configuredUrl")
    }
}

tasks.matching { it.name.startsWith("assembleRelease") || it.name.startsWith("bundleRelease") }.configureEach {
    dependsOn("validateReleaseConfiguration")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    compilerOptions.freeCompilerArgs.add("-Xexpect-actual-classes")
}
