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

include(":app:android")
include(":app:shared")

include(":core:common")
include(":core:domain")
include(":core:data")
include(":core:database")
include(":core:network")
include(":core:sync")
include(":core:datastore")
include(":core:designsystem")
include(":core:ui")
include(":core:navigation")
include(":core:testing")

include(":feature:auth:api")
include(":feature:auth:impl")
include(":feature:workout:api")
include(":feature:workout:impl")
include(":feature:exercises:api")
include(":feature:exercises:impl")
include(":feature:history:api")
include(":feature:history:impl")
include(":feature:templates:api")
include(":feature:templates:impl")
include(":feature:settings:api")
include(":feature:settings:impl")
