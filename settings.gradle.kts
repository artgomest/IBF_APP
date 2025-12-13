// Top-level build file where you can add configuration options common to all sub-projects/modules.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
        repositories {
            google()
            mavenCentral()
            maven { url = uri("https://jitpack.io") } // Para MPAndroidChart, por exemplo
        }
}

rootProject.name = "IBF APP"
include(":app")
