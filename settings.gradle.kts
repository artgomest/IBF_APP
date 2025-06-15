// Top-level build file where you can add configuration options common to all sub-projects/modules.
pluginManagement {
    repositories {
        google { // Repositório Maven do Google para plugins
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
        repositories {
            google() // <--- CORREÇÃO AQUI: Adiciona o repositório Google explicitamente
            mavenCentral()
            maven { url = uri("https://jitpack.io") } // Para MPAndroidChart, por exemplo
        }
}

rootProject.name = "IBF APP"
include(":app")
