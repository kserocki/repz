package com.repz.app.konsist.rules

import com.lemonappdev.konsist.api.container.KoScope
import com.lemonappdev.konsist.api.declaration.KoImportDeclaration
import com.lemonappdev.konsist.api.declaration.KoPackageDeclaration
import com.lemonappdev.konsist.api.declaration.KoPropertyDeclaration
import com.lemonappdev.konsist.api.declaration.combined.KoClassAndObjectDeclaration
import com.lemonappdev.konsist.api.verify.assertTrue
import com.repz.app.konsist.support.repzSourceFile

private val FEATURE_API_MODULE = Regex(""":feature:([^:]+):api""")

/**
 * V1: `feature:x:api` main files declare only `@Serializable` `NavKey` types (`data object`/`data
 * class`) and exactly one top-level `val <feature>NavKeySerializers: SerializersModule` per module,
 * named from the module's folder in lowerCamelCase (ADR-0004 + amendment, ARCH §8).
 */
fun checkV1FeatureApiShape(scope: KoScope) {
    val apiFiles = scope.files.filter { FEATURE_API_MODULE.matches(it.repzSourceFile().modulePath) && !it.repzSourceFile().isTest }

    apiFiles.assertTrue(
        additionalMessage = "V1: feature:x:api main files declare only NavKeys and one xNavKeySerializers val (ADR-0004, ARCH §8)",
    ) { file ->
        file.declarations(includeNested = false, includeLocal = false).all { declaration ->
            when (declaration) {
                is KoClassAndObjectDeclaration -> {
                    declaration.hasAnnotationWithName("Serializable") && declaration.hasParentWithName("NavKey")
                }

                is KoPropertyDeclaration -> {
                    declaration.name.endsWith("NavKeySerializers")
                }

                is KoPackageDeclaration, is KoImportDeclaration -> {
                    true
                }

                else -> {
                    false
                }
            }
        }
    }

    apiFiles
        .groupBy { it.repzSourceFile().modulePath }
        .forEach { (modulePath, filesInModule) ->
            val featureName = FEATURE_API_MODULE.find(modulePath)!!.groupValues[1]
            val expectedValName = "${featureName}NavKeySerializers"
            val serializerVals =
                filesInModule
                    .flatMap {
                        it.properties(
                            includeNested = false,
                        )
                    }.filter { it.name.endsWith("NavKeySerializers") }

            check(serializerVals.size == 1 && serializerVals.single().name == expectedValName) {
                "V1: $modulePath must expose exactly one top-level val $expectedValName (ADR-0004): found ${serializerVals.map { it.name }}"
            }
        }
}
