package com.repz.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/** The `libs` version catalog, shared by every convention plugin. */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/**
 * Derives the Compose Multiplatform / Android namespace from the Gradle project path:
 * `:core:common` -> `com.repz.app.core.common`, `:feature:history:impl` -> `com.repz.app.feature.history.impl`,
 * `:app:shared` -> `com.repz.app.shared` (the leading `app` segment is dropped for `:app:*` modules).
 */
internal fun Project.repzNamespace(): String = (BASE_PACKAGE_SEGMENTS + pathSegmentsWithoutAppPrefix()).joinToString(".")

/**
 * Derives a unique Gradle group per module from its parent path segments, so Kotlin/Native klibs
 * (identified by `group:name`) never collide between modules that share a leaf name such as
 * `api` or `impl`: `:feature:history:impl` -> group `com.repz.app.feature.history`.
 */
internal fun Project.repzGroup(): String {
    val parentSegments = pathSegmentsWithoutAppPrefix().dropLast(1)
    return (BASE_PACKAGE_SEGMENTS + parentSegments).joinToString(".")
}

/** Project path segments with the leading `app` segment dropped for `:app:*` modules. */
private fun Project.pathSegmentsWithoutAppPrefix(): List<String> {
    val segments = path.removePrefix(":").split(":").filter { it.isNotEmpty() }
    return if (segments.firstOrNull() == "app") segments.drop(1) else segments
}

private val BASE_PACKAGE_SEGMENTS = listOf("com", "repz", "app")
