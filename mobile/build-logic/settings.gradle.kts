import org.gradle.api.artifacts.dsl.RepositoryHandler

/** Google's Maven repo, scoped to the groups it actually hosts (speeds up dependency resolution). */
fun RepositoryHandler.googleFiltered() {
    google {
        content {
            includeGroupByRegex("com\\.android.*")
            includeGroupByRegex("com\\.google.*")
            includeGroupByRegex("androidx.*")
        }
    }
}

pluginManagement {
    // The pluginManagement block is compiled before top-level declarations in this file are
    // visible, so `googleFiltered()` can't be called here; only `dependencyResolutionManagement`
    // below reuses it.
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
        googleFiltered()
        mavenCentral()
        // org.jlleitschuh.gradle:ktlint-gradle is published only to the Gradle Plugin Portal's
        // own repository, not Maven Central.
        exclusiveContent {
            forRepository { gradlePluginPortal() }
            filter { includeGroup("org.jlleitschuh.gradle") }
        }
    }

    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"

include(":convention")
