import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties =
    Properties().apply {
        if (keystorePropertiesFile.exists()) keystorePropertiesFile.inputStream().use { load(it) }
    }

val supportedAbis = setOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
val requestedAbi = providers.gradleProperty("scoopAbi").orNull
require(requestedAbi == null || requestedAbi in supportedAbis) {
    "Unsupported -PscoopAbi=$requestedAbi. Use one of: ${supportedAbis.sorted().joinToString()}"
}

android {
    namespace = "com.scoop.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.scoop.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 29
        versionName = "1.2.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // A specific ABI can be selected for local/CI builds with -PscoopAbi=<abi>.
        // Debug builds remain universal when the property is omitted.
        requestedAbi?.let { abi ->
            ndk {
                abiFilters.clear()
                abiFilters += abi
            }
        }
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // R8 minification here has silently broken reflection-based library internals before
            // (see proguard-rules.pro) in a way debug builds never surface, since debug skips
            // minification entirely. Debug installing and working is NOT sufficient signal that a
            // release build works - always smoke-test the actual assembleRelease APK's core flow
            // (paste a link, analyze, download) before publishing a release, not just debug.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            // Native media runtimes dominate the APK, so the normal release stays arm64-only.
            // Maintainers can publish separate installable variants with -PscoopAbi=<abi>.
            if (requestedAbi == null) {
                ndk {
                    abiFilters.clear()
                    abiFilters += "arm64-v8a"
                }
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    room {
        schemaDirectory("$projectDir/schemas")
    }
    ksp {
        arg("room.incremental", "true")
    }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
        jniLibs.useLegacyPackaging = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    lint {
        disable.addAll(listOf("MissingTranslation", "ExtraTranslation"))
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.coil.compose)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.jsoup)
    implementation(libs.coil.svg)
    coreLibraryDesugaring(libs.desugar)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    implementation(libs.mmkv)

    implementation(libs.bundles.youtubedlAndroid)

    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test)
}
