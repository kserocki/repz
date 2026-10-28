pluginManagement {
    includeBuild("build-logic")

    repositories {
        google {
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
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

// Incubating feature: must stay enabled for `implementation(projects.core.domain)` style accessors.
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "repz"

// Module `include(...)` lines are added in WP-3, once the module directories exist
// (Gradle 9 rejects an included project whose directory is missing).
