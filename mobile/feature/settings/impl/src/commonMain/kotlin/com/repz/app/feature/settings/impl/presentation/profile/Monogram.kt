package com.repz.app.feature.settings.impl.presentation.profile

/**
 * The trimmed display name's first full code point (a surrogate pair, e.g. an emoji, isn't cut
 * in half), uppercased. Blank after trimming returns an empty string (the avatar then shows the
 * Q9 placeholder, since [ProfileAccount.monogram] is never used on a `null` account).
 */
internal fun monogramOf(displayName: String): String {
    val trimmed = displayName.trim()
    if (trimmed.isEmpty()) return ""

    val firstChar = trimmed[0]
    val isSurrogatePair = firstChar.isHighSurrogate() && trimmed.length > 1 && trimmed[1].isLowSurrogate()
    val codePointLength = if (isSurrogatePair) 2 else 1
    return trimmed.take(codePointLength).uppercase()
}
