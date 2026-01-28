/*
 * Composite build settings for running all tests in the tests/ folder
 */

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

rootProject.name = "lm-lab-kt"

// Automatically discover and include all test projects from tests/ folder
val testsDir = file("tests")
if (testsDir.exists() && testsDir.isDirectory) {
    testsDir.listFiles { file ->
        file.isDirectory && (
            file.resolve("settings.gradle.kts").exists() ||
            file.resolve("build.gradle.kts").exists() || 
            file.resolve("build.gradle").exists()
        )
    }?.sortedBy { it.name }?.forEach { testProject ->
        println("Including test project: ${testProject.name}")
        includeBuild(testProject.path)
    }
}
