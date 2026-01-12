@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.dagger.hilt.android")
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "dev.mcd.chess"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.mcd.chess"
        minSdk = BuildSettings.minSdk
        targetSdk = 35

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"

        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField(
            type = "String",
            name = "ONLINE_API_HOST",
            value = "\"${AppConfig.ONLINE_API_HOST}\"",
        )
    }

    // Si tú realmente tienes el código en src/<variant>/kotlin
    sourceSets.configureEach {
        java.srcDirs("src/$name/kotlin")
    }

    buildFeatures {
        aidl = true
        compose = true
        buildConfig = true
    }

    // IMPORTANTE: define signingConfigs explícitamente (arregla el "Unresolved reference: signingConfig")
    signingConfigs {
        getByName("debug") { }

        // Para poder instalar release/benchmark firmados con debug:
        create("release") {
            val dbg = signingConfigs.getByName("debug")
            storeFile = dbg.storeFile
            storePassword = dbg.storePassword
            keyAlias = dbg.keyAlias
            keyPassword = dbg.keyPassword
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }

        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard/proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }

        create("benchmark") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard/benchmark-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-Xcontext-receivers",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlin.ExperimentalStdlibApi"
        )

        // Reportes opcionales de Compose compiler si pasas -PenableComposeCompilerReports
        if (project.hasProperty("enableComposeCompilerReports")) {
            val metricsDir = layout.buildDirectory.dir("compose_metrics").get().asFile.absolutePath
            freeCompilerArgs += listOf(
                "-P", "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=$metricsDir",
                "-P", "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=$metricsDir"
            )
        }
    }

    // 👇 RECOMENDADO: NO fijes una versión vieja aquí si usas Kotlin 1.9.24.
    // Si ya estás con Compose BOM, normalmente lo correcto es que tu variable Versions.composeCompiler
    // sea compatible con tu Kotlin, o directamente quitar este bloque si tu setup ya usa el plugin nuevo.
    composeOptions {
        kotlinCompilerExtensionVersion = Versions.composeCompiler
    }

    // Si te hace falta
    packaging {
        resources {
            excludes += setOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1"
            )
        }
    }
}

kapt {
    correctErrorTypes = true
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    with(Versions) {
        // Projects
        implementation(project(":engine-stockfish"))
        implementation(project(":engine-lc0"))
        implementation(project(":common"))
        implementation(project(":online"))

        // Core
        implementation("org.slf4j:slf4j-nop:$slf4j")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines")
        implementation("androidx.core:core-ktx:$coreKtx")
        implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleRuntimeKtx")
        implementation("androidx.profileinstaller:profileinstaller:$androidProfileInstaller")

        // Compose (BOM)
        implementation(platform("androidx.compose:compose-bom:2024.06.00"))
        androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))

        implementation("androidx.compose.ui:ui")
        implementation("androidx.compose.foundation:foundation")
        implementation("androidx.compose.material3:material3")
        implementation("androidx.compose.material:material-icons-extended")
        implementation("androidx.compose.ui:ui-tooling-preview")

        debugImplementation("androidx.compose.ui:ui-tooling")
        androidTestImplementation("androidx.compose.ui:ui-test-junit4")
        debugImplementation("androidx.compose.ui:ui-test-manifest")

        // Orbit
        implementation("org.orbit-mvi:orbit-core:$orbit")
        implementation("org.orbit-mvi:orbit-viewmodel:$orbit")
        implementation("org.orbit-mvi:orbit-compose:$orbit")

        // Hilt
        implementation("androidx.hilt:hilt-navigation-compose:$hiltNavigationCompose")
        implementation("com.google.dagger:hilt-android:$hilt")
        kapt("com.google.dagger:hilt-compiler:$hilt")

        // Other
        implementation("androidx.activity:activity-compose:$activityCompose")
        implementation("androidx.navigation:navigation-compose:$navigationCompose")
        implementation("com.github.bhlangonijr:chesslib:$chessLib")
        implementation("com.jakewharton.timber:timber:$timber")
        implementation("androidx.datastore:datastore-preferences:$datastorePreferences")

        // Tests
        testImplementation("io.kotest:kotest-assertions-core:$kotest")
        testImplementation("io.kotest:kotest-runner-junit5:$kotest")
        testImplementation("app.cash.turbine:turbine:$turbine")
        testImplementation("io.mockk:mockk:$mockk")
        testImplementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin")
        androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutines")
        androidTestImplementation("androidx.benchmark:benchmark-macro-junit4:$androidBenchmarkJunit")
    }
}
