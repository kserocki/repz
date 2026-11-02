package com.repz.app.konsist.rules

import java.io.File

private const val BRAND_COLORS_PATH =
    "core/designsystem/src/commonMain/kotlin/com/repz/app/core/designsystem/RepzBrandColors.kt"
private const val ANDROID_COLORS_PATH = "app/android/src/main/res/values/colors.xml"
private const val IOS_LAUNCH_BACKGROUND_PATH = "iosApp/iosApp/Assets.xcassets/LaunchBackground.colorset/Contents.json"

private val ANDROID_BRAND_KEYS = listOf("cyan", "deep", "ink", "paper")

private val BRAND_COLOR_PROPERTY = Regex("""val (\w+): Color = Color\(0xFF([0-9A-Fa-f]{6})\)""")
private val ANDROID_BRAND_COLOR = Regex("""<color name="brand_(\w+)">#([0-9A-Fa-f]{6})</color>""")
private val IOS_IDIOM_UNIVERSAL = Regex(""""idiom"\s*:\s*"universal"""")
private val IOS_DARK_APPEARANCE = Regex(""""value"\s*:\s*"dark"""")
private val IOS_RED_COMPONENT = Regex(""""red"\s*:\s*"0x([0-9A-Fa-f]{2})"""")
private val IOS_GREEN_COMPONENT = Regex(""""green"\s*:\s*"0x([0-9A-Fa-f]{2})"""")
private val IOS_BLUE_COMPONENT = Regex(""""blue"\s*:\s*"0x([0-9A-Fa-f]{2})"""")

/**
 * B1: `RepzBrandColors` (the single Kotlin source of the 5 fixed brand colours, DESIGN_SYSTEM §3.2)
 * never drifts from the platform shells that paint them before any Kotlin code runs - Android's
 * splash `colors.xml` and iOS's `LaunchBackground` launch-screen colorset (ARCH §12.2 B1).
 *
 * Both sub-checks read [root] directly (not through a Konsist [com.lemonappdev.konsist.api.container.KoScope],
 * which only resolves `.kt` files) so they can parse the Android XML and iOS JSON colour values too.
 */
fun checkB1BrandColorsMatchPlatforms(root: File) {
    checkB1aAndroidColorsMatchBrand(root)
    checkB1bIosLaunchBackgroundMatchesBrand(root)
}

/** B1a: every `brand_cyan/deep/ink/paper` in Android's `colors.xml` equals its `RepzBrandColors` counterpart. */
fun checkB1aAndroidColorsMatchBrand(root: File) {
    val brandColors = parseBrandColors(root)
    val androidColors = parseAndroidColorsXml(root)

    ANDROID_BRAND_KEYS.forEach { key ->
        val propertyName = key.replaceFirstChar { it.uppercase() }
        val brandHex =
            checkNotNull(brandColors[propertyName]) {
                "B1a: RepzBrandColors has no '$propertyName' property to compare against colors.xml's brand_$key (ARCH §12.2 B1)"
            }
        val androidHex =
            checkNotNull(androidColors[key]) {
                "B1a: colors.xml is missing brand_$key (ARCH §12.2 B1)"
            }
        check(androidHex == brandHex) {
            "B1a: colors.xml brand_$key (#$androidHex) must equal RepzBrandColors.$propertyName (#$brandHex) (ARCH §12.2 B1)"
        }
    }
}

/** B1b: iOS's `LaunchBackground` colorset is light = `RepzBrandColors.Paper`, dark = `RepzBrandColors.Ink`. */
fun checkB1bIosLaunchBackgroundMatchesBrand(root: File) {
    val brandColors = parseBrandColors(root)
    val brandPaper = checkNotNull(brandColors["Paper"]) { "B1b: RepzBrandColors has no 'Paper' property (ARCH §12.2 B1)" }
    val brandInk = checkNotNull(brandColors["Ink"]) { "B1b: RepzBrandColors has no 'Ink' property (ARCH §12.2 B1)" }

    val (lightHex, darkHex) = parseIosLaunchBackground(root)

    check(lightHex == brandPaper) {
        "B1b: LaunchBackground's light colour (#$lightHex) must equal RepzBrandColors.Paper (#$brandPaper) (ARCH §12.2 B1)"
    }
    check(darkHex == brandInk) {
        "B1b: LaunchBackground's dark colour (#$darkHex) must equal RepzBrandColors.Ink (#$brandInk) (ARCH §12.2 B1)"
    }
}

private fun parseBrandColors(root: File): Map<String, String> {
    val file = File(root, BRAND_COLORS_PATH)
    check(file.isFile) { "B1: expected RepzBrandColors.kt at ${file.path} (ARCH §12.2 B1)" }
    return BRAND_COLOR_PROPERTY.findAll(file.readText()).associate { it.groupValues[1] to it.groupValues[2].uppercase() }
}

private fun parseAndroidColorsXml(root: File): Map<String, String> {
    val file = File(root, ANDROID_COLORS_PATH)
    check(file.isFile) { "B1a: expected colors.xml at ${file.path} (ARCH §12.2 B1)" }
    return ANDROID_BRAND_COLOR.findAll(file.readText()).associate { it.groupValues[1] to it.groupValues[2].uppercase() }
}

/** Returns the colorset's light (`#RRGGBB`) colour paired with its dark one. */
private fun parseIosLaunchBackground(root: File): Pair<String, String> {
    val file = File(root, IOS_LAUNCH_BACKGROUND_PATH)
    check(file.isFile) { "B1b: expected LaunchBackground Contents.json at ${file.path} (ARCH §12.2 B1)" }
    val text = file.readText()

    // Each colour entry ends with its own "idiom" : "universal" field, so splitting right after
    // every match yields one chunk per entry without needing a full JSON parser.
    val entryEnds = IOS_IDIOM_UNIVERSAL.findAll(text).map { it.range.last + 1 }.toList()
    check(entryEnds.size == 2) {
        "B1b: expected exactly 2 colour entries (light and dark) in ${file.path}, found ${entryEnds.size} (ARCH §12.2 B1)"
    }
    val chunks = listOf(text.substring(0, entryEnds[0]), text.substring(entryEnds[0], entryEnds[1]))

    val darkIndex = chunks.indexOfFirst { IOS_DARK_APPEARANCE.containsMatchIn(it) }
    check(darkIndex >= 0) { "B1b: expected one dark-appearance colour entry in ${file.path} (ARCH §12.2 B1)" }
    val lightIndex = 1 - darkIndex

    return hexOfIosColorEntry(chunks[lightIndex], file) to hexOfIosColorEntry(chunks[darkIndex], file)
}

private fun hexOfIosColorEntry(
    chunk: String,
    file: File,
): String {
    val red =
        checkNotNull(IOS_RED_COMPONENT.find(chunk)?.groupValues?.get(1)) { "B1b: a colour entry in ${file.path} has no red component" }
    val green =
        checkNotNull(IOS_GREEN_COMPONENT.find(chunk)?.groupValues?.get(1)) { "B1b: a colour entry in ${file.path} has no green component" }
    val blue =
        checkNotNull(IOS_BLUE_COMPONENT.find(chunk)?.groupValues?.get(1)) { "B1b: a colour entry in ${file.path} has no blue component" }
    return "$red$green$blue".uppercase()
}
