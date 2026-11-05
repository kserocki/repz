package com.repz.app.core.designsystem.logo

/** The symbol's 7 parts (brand/LOGO.md §2): the bar and three mirrored plate pairs. */
enum class RepzLogoPart { Bar, InnerLeft, InnerRight, MiddleLeft, MiddleRight, OuterLeft, OuterRight }

/** The 3 plate pairs, in landing order (brand/LOGO.md §5: inner, then middle, then outer). */
enum class RepzPlatePair { Inner, Middle, Outer }

/** Which pair [this] belongs to; `null` for [RepzLogoPart.Bar] (it never joins a pair: it doesn't
 * slide in, it is already drawn from the first frame). */
internal val RepzLogoPart.pair: RepzPlatePair?
    get() =
        when (this) {
            RepzLogoPart.Bar -> null
            RepzLogoPart.InnerLeft, RepzLogoPart.InnerRight -> RepzPlatePair.Inner
            RepzLogoPart.MiddleLeft, RepzLogoPart.MiddleRight -> RepzPlatePair.Middle
            RepzLogoPart.OuterLeft, RepzLogoPart.OuterRight -> RepzPlatePair.Outer
        }

/** Whether [this] sits left of the grid's centre (x = 160): left parts enter from the left screen
 * edge, right parts from the right (brand/LOGO.md §5). Always `false` for [RepzLogoPart.Bar] (it
 * never translates). */
internal val RepzLogoPart.isLeftSide: Boolean
    get() =
        when (this) {
            RepzLogoPart.Bar -> false
            RepzLogoPart.InnerLeft, RepzLogoPart.MiddleLeft, RepzLogoPart.OuterLeft -> true
            RepzLogoPart.InnerRight, RepzLogoPart.MiddleRight, RepzLogoPart.OuterRight -> false
        }

/** [this] pair's two mirrored parts, left first. */
internal val RepzPlatePair.parts: List<RepzLogoPart>
    get() =
        when (this) {
            RepzPlatePair.Inner -> listOf(RepzLogoPart.InnerLeft, RepzLogoPart.InnerRight)
            RepzPlatePair.Middle -> listOf(RepzLogoPart.MiddleLeft, RepzLogoPart.MiddleRight)
            RepzPlatePair.Outer -> listOf(RepzLogoPart.OuterLeft, RepzLogoPart.OuterRight)
        }
