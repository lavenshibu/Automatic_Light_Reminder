// Plugin management repositories used to resolve plugin dependencies
pluginManagement {
    repositories {
        google {
            content {
                // Include plugin groups from Google and AndroidX
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()        // Standard Java/Kotlin packages
        gradlePluginPortal()  // Gradle plugin-specific packages
    }
}

// Control how project dependencies are resolved
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) // Prevents projects from declaring their own repositories
    repositories {
        google()        // Android and Google libraries
        mavenCentral()  // Kotlin/Java/third-party libraries
    }
}

// Root project name and included modules
rootProject.name = "LhtkotApp"
include(":app")
