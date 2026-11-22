import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
    alias(libs.plugins.firebasePerf)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
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

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.sqldelight.sqlite.driver)
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
        versionCode = 6
        versionName = "1.1.4"
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

            if (!resolvedFile.exists()) {
                throw GradleException("ERROR DE SEGURIDAD CRÍTICO: No se encontró el archivo keystore de producción en '${resolvedFile.absolutePath}'. El fallback a debug ha sido revocado permanentemente.")
            }

            val storePwd = System.getenv("GYMVIP_KEYSTORE_PASSWORD")
                ?: keystoreProps.getProperty("storePassword")
            val keyAliasProp = System.getenv("GYMVIP_KEY_ALIAS")
                ?: keystoreProps.getProperty("keyAlias")
            val keyPwd = System.getenv("GYMVIP_KEY_PASSWORD")
                ?: keystoreProps.getProperty("keyPassword")

            if (storePwd.isNullOrBlank() || keyAliasProp.isNullOrBlank() || keyPwd.isNullOrBlank()) {
                throw GradleException("ERROR DE SEGURIDAD CRÍTICO: Credenciales de firma de release incompletas (storePassword, keyAlias o keyPassword ausentes).")
            }

            storeFile = resolvedFile
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
            buildConfigField("String", "DEFAULT_SERVER_URL", "\"https://gymvip-backend.onrender.com\"")
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            buildConfigField("String", "DEFAULT_SERVER_URL", "\"https://gymvip-backend.onrender.com\"")
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
        val configuredUrl = "https://gymvip-backend.onrender.com"
        if (configuredUrl.contains("localhost") || configuredUrl.contains("10.0.2.2") || configuredUrl.contains("127.0.0.1")) {
            throw GradleException("ERROR DE SEGURIDAD CRÍTICO: La compilación de release no puede apuntar a IP local o emulador ($configuredUrl)")
        }
        logger.lifecycle("[SEGURIDAD CI/CD] URL de servidor release verificada para producción: $configuredUrl")
    }
}

tasks.matching { it.name.startsWith("assembleRelease") || it.name.startsWith("bundleRelease") }.configureEach {
    dependsOn("validateReleaseConfiguration")
}


