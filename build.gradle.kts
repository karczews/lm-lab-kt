plugins {
    base
}

tasks.register("testAll") {
    group = "verification"
    description = "Runs all tests for all projects in the tests/ folder"
    
    gradle.includedBuilds.forEach { includedBuild ->
        dependsOn(includedBuild.task(":app:test"))
        dependsOn(includedBuild.task(":list:test"))
        dependsOn(includedBuild.task(":utilities:test"))
    }
}
