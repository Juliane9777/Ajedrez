@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.dagger.hilt.android")
    //id("androidx.baselineprofile")
}

android {
    //compileSdk = BuildSettings.compileSdk
    compileSdk = 35 // o 35 si lo tienes instalado


    defaultConfig {
        //targetSdk = 35 // opcional, pero recomendable alinearlo
        namespace = "dev.mcd.chess"
        applicationId = "dev.mcd.chess"
        minSdk = BuildSettings.minSdk
        //targetSdk = 35

        //targetSdk = BuildSettings.targetSdk
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

    sourceSets.configureEach {
        java.srcDirs("src/$name/kotlin")
    }

    buildFeatures {
        aidl = true
    }

    buildTypes {
        val release = getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard/proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
        create("benchmark") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard/benchmark-rules.pro")
            signingConfig = release.signingConfig
        }
    }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }


    kotlin {
        //jvmToolchain(BuildSettings.jdkVersion)
        jvmToolchain(17)

    }

    kotlinOptions {
        freeCompilerArgs += "-Xcontext-receivers"
        freeCompilerArgs += "-Xopt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        freeCompilerArgs += "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        freeCompilerArgs += "-Xopt-in=kotlin.ExperimentalStdlibApi"

        if (project.hasProperty("enableComposeCompilerReports")) {
            val metricsDir = "${project.buildDir.absolutePath}/compose_metrics"
            freeCompilerArgs += listOf("-P", "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=$metricsDir")
            freeCompilerArgs += listOf("-P", "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=$metricsDir")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        //kotlinCompilerExtensionVersion = Versions.composeCompiler
        kotlinCompilerExtensionVersion = "1.5.14"
        useLiveLiterals = false
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

kapt {
    correctErrorTypes = true
}

dependencies {
    with(Versions) {
        // Projects
        implementation(project(":engine-stockfish"))
        implementation(project(":engine-lc0"))
        implementation(project(":common"))
        implementation(project(":online"))
        //"baselineProfile"(project(mapOf("path" to ":baselineprofile")))

        // Core
        implementation("org.slf4j:slf4j-nop:$slf4j")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines")
        implementation("androidx.core:core-ktx:$coreKtx")
        implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleRuntimeKtx")
        implementation("androidx.profileinstaller:profileinstaller:$androidProfileInstaller")

        // Compose
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

        testImplementation("io.kotest:kotest-assertions-core:$kotest")
        testImplementation("io.kotest:kotest-runner-junit5:$kotest")
        testImplementation("app.cash.turbine:turbine:$turbine")
        testImplementation("io.mockk:mockk:$mockk")
        testImplementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin")
        androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutines")
        androidTestImplementation("androidx.benchmark:benchmark-macro-junit4:$androidBenchmarkJunit")
    }
}
