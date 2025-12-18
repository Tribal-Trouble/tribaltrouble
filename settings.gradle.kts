rootProject.name = "tribaltrouble"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

include("assets", "common")
// server and servlet excluded - have compilation errors
// include("server", "servlet")
include("tools", "tt")
