@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            setUrl("https://androidx.dev/snapshots/builds/9664109/artifacts/repository")
        }
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.5.2"
        id("com.android.library") version "8.5.2"

        id("org.jetbrains.kotlin.android") version "1.9.24"
        id("org.jetbrains.kotlin.jvm") version "1.9.24"
        id("org.jetbrains.kotlin.kapt") version "1.9.24"
        id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24"

        id("com.google.dagger.hilt.android") version "2.51.1"
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
    }
}
rootProject.name = "Chess"
include(
    "app",
    "common",
    "engine-stockfish",
    "engine-lc0",
    "online",
    //"baselineprofile",
)
