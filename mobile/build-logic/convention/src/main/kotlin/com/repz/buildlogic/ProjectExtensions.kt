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
internal fun Project.repzNamespace(): String {
    val segments = pathSegments()
    val namespaceSegments = if (segments.firstOrNull() == "app") segments.drop(1) else segments
    return (BASE_PACKAGE_SEGMENTS + namespaceSegments).joinToString(".")
}

/**
 * Derives a unique Gradle group per module from its parent path segments, so Kotlin/Native klibs
 * (identified by `group:name`) never collide between modules that share a leaf name such as
 * `api` or `impl`: `:feature:history:impl` -> group `com.repz.app.feature.history`.
 */
internal fun Project.repzGroup(): String {
    val segments = pathSegments()
    val withoutAppPrefix = if (segments.firstOrNull() == "app") segments.drop(1) else segments
    val parentSegments = withoutAppPrefix.dropLast(1)
    return (BASE_PACKAGE_SEGMENTS + parentSegments).joinToString(".")
}

private fun Project.pathSegments(): List<String> = path.removePrefix(":").split(":").filter { it.isNotEmpty() }

private val BASE_PACKAGE_SEGMENTS = listOf("com", "repz", "app")
