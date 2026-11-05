#!/usr/bin/env python3
"""Generates the Compose logo geometry and wordmark vector from the brand sources.

Reads `brand/logo/src/symbol.svg` (the 7 `<rect id=…>` parts), `brand/logo/src/lockup-stacked.svg`
(the stacked-lockup wordmark placement) and `brand/logo/master/wordmark.svg` (the outlined wordmark
path) and writes:
- `mobile/core/designsystem/src/commonMain/kotlin/com/repz/app/core/designsystem/logo/RepzLogoGeometry.kt`
- `mobile/core/designsystem/src/commonMain/composeResources/drawable/logo_wordmark.xml`

Stdlib only (`re`, `pathlib`), same style as `android_vectors.py`. Never edit the generated files by
hand; change this script and re-run it instead. Re-running it must leave `git status` clean.

Geometry: brand/LOGO.md §2.
"""
from __future__ import annotations

import re
from pathlib import Path

SRC_DIR = Path(__file__).resolve().parent
LOGO_DIR = SRC_DIR.parent
BRAND_DIR = LOGO_DIR.parent
REPO_ROOT = BRAND_DIR.parent
MASTER_DIR = LOGO_DIR / "master"
DESIGNSYSTEM_MAIN = REPO_ROOT / "mobile" / "core" / "designsystem" / "src" / "commonMain"
KOTLIN_LOGO_DIR = DESIGNSYSTEM_MAIN / "kotlin" / "com" / "repz" / "app" / "core" / "designsystem" / "logo"
DRAWABLE_DIR = DESIGNSYSTEM_MAIN / "composeResources" / "drawable"

# Maps `symbol.svg`'s `<rect id=…>` ids to the hand-written `RepzLogoPart` enum entries
# (`logo/RepzLogoPart.kt`). Fixed on purpose: a renamed or added id fails loudly instead of silently
# dropping a part.
PART_ID_TO_ENUM = {
    "bar": "Bar",
    "plate-inner-left": "InnerLeft",
    "plate-inner-right": "InnerRight",
    "plate-middle-left": "MiddleLeft",
    "plate-middle-right": "MiddleRight",
    "plate-outer-left": "OuterLeft",
    "plate-outer-right": "OuterRight",
}

RECT_RE = re.compile(
    r'<rect id="([\w-]+)" x="([-\d.]+)" y="([-\d.]+)" width="([\d.]+)" height="([\d.]+)" '
    r'rx="([\d.]+)" fill="(#[0-9A-Fa-f]{6})"/>'
)
VIEWBOX_RE = re.compile(r'viewBox="0 0 ([\d.]+) ([\d.]+)"')
WORDMARK_PATH_RE = re.compile(r'<path fill="(#[0-9A-Fa-f]{6})" d="([^"]+)"/>')
STACKED_WORDMARK_GROUP_RE = re.compile(
    r'<g transform="translate\(([-\d.]+)[ ,]+([-\d.]+)\)\s*scale\(([-\d.]+)\)">\s*<g id="wordmark">'
)


class LogoRect:
    def __init__(self, x: float, y: float, width: float, height: float) -> None:
        self.x = x
        self.y = y
        self.width = width
        self.height = height


def parse_symbol_parts() -> dict[str, LogoRect]:
    """Returns the 7 parts, keyed by their `RepzLogoPart` enum name, failing on an id this script
    doesn't know (renamed, added or removed) so geometry and the enum never silently drift apart."""
    svg_path = SRC_DIR / "symbol.svg"
    matches = RECT_RE.findall(svg_path.read_text())
    found_ids = {match[0] for match in matches}
    known_ids = set(PART_ID_TO_ENUM)

    unknown_ids = found_ids - known_ids
    if unknown_ids:
        raise SystemExit(
            f"{svg_path}: unknown rect id(s) {sorted(unknown_ids)}. Add them to PART_ID_TO_ENUM "
            "(and to RepzLogoPart.kt) if this is a real new part."
        )
    missing_ids = known_ids - found_ids
    if missing_ids:
        raise SystemExit(f"{svg_path}: missing expected rect id(s) {sorted(missing_ids)}.")

    return {
        PART_ID_TO_ENUM[part_id]: LogoRect(float(x), float(y), float(width), float(height))
        for part_id, x, y, width, height, _rx, _fill in matches
    }


def artwork_bounds(parts: dict[str, LogoRect]) -> LogoRect:
    """The union of all 7 parts: the visible symbol's bounding box (brand/LOGO.md §2, ADR-0017)."""
    min_x = min(part.x for part in parts.values())
    min_y = min(part.y for part in parts.values())
    max_x = max(part.x + part.width for part in parts.values())
    max_y = max(part.y + part.height for part in parts.values())
    return LogoRect(min_x, min_y, max_x - min_x, max_y - min_y)


def parse_wordmark_viewbox() -> tuple[float, float]:
    svg_path = MASTER_DIR / "wordmark.svg"
    match = VIEWBOX_RE.search(svg_path.read_text())
    if match is None:
        raise SystemExit(f"{svg_path}: expected a `viewBox=\"0 0 W H\"` attribute.")
    return float(match.group(1)), float(match.group(2))


def parse_wordmark_path() -> str:
    """The outlined wordmark as one fill path (brand/LOGO.md §2), copied as-is into the Android
    vector's `pathData`: Android's path-data mini-language is the same as SVG's (as `android_vectors.py`
    already relies on for the app-icon paths)."""
    svg_path = MASTER_DIR / "wordmark.svg"
    match = WORDMARK_PATH_RE.search(svg_path.read_text())
    if match is None:
        raise SystemExit(f"{svg_path}: expected exactly one `<path fill=\"#…\" d=\"…\"/>`.")
    return match.group(2)


def parse_stacked_wordmark_placement() -> tuple[float, float, float]:
    """Returns `(translateX, translateY, scale)` of the wordmark group inside the stacked lockup,
    parsed rather than retyped (brand/LOGO.md §2's "wordmark at 0.7 scale, centred, 24 units under
    the plates")."""
    svg_path = SRC_DIR / "lockup-stacked.svg"
    match = STACKED_WORDMARK_GROUP_RE.search(svg_path.read_text())
    if match is None:
        raise SystemExit(f"{svg_path}: expected a `<g transform=\"translate(…) scale(…)\"><g id=\"wordmark\">` group.")
    translate_x, translate_y, scale = match.groups()
    return float(translate_x), float(translate_y), float(scale)


def kotlin_float(value: float) -> str:
    """A Kotlin `Float` literal: `100f` for a whole number, `37.5f` otherwise."""
    if value == int(value):
        return f"{int(value)}f"
    return f"{value:g}f"


def logo_rect_literal(rect: LogoRect) -> str:
    return (
        f"LogoRect(x = {kotlin_float(rect.x)}, y = {kotlin_float(rect.y)}, "
        f"width = {kotlin_float(rect.width)}, height = {kotlin_float(rect.height)})"
    )


def render_kotlin(
    parts: dict[str, LogoRect],
    bounds: LogoRect,
    wordmark_width: float,
    wordmark_height: float,
    stacked_scale: float,
    stacked_top: float,
) -> str:
    part_entries = "\n".join(
        f"            RepzLogoPart.{enum_name} to {logo_rect_literal(parts[enum_name])},"
        for enum_name in PART_ID_TO_ENUM.values()
    )
    return f"""// Generated by brand/logo/src/compose_logo.py. Do not edit by hand.
package com.repz.app.core.designsystem.logo

/**
 * One rounded rectangle on the 320x160 symbol grid (brand/LOGO.md §2). Every part is a pill: its
 * corner radius is `min(width, height) / 2`.
 */
internal data class LogoRect(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
)

/**
 * The 7-part symbol's geometry and the stacked-lockup wordmark placement (brand/LOGO.md §2),
 * generated from brand/logo/src/symbol.svg, brand/logo/src/lockup-stacked.svg and
 * brand/logo/master/wordmark.svg by brand/logo/src/compose_logo.py. Never edit by hand.
 */
internal object RepzLogoGeometry {{
    const val GRID_WIDTH: Float = {kotlin_float(LOGO_GRID_WIDTH)}
    const val GRID_HEIGHT: Float = {kotlin_float(LOGO_GRID_HEIGHT)}

    /** The visible symbol's bounding box, the union of all 7 parts: "144 dp wide" (ADR-0017) means
     * this width, not [GRID_WIDTH]. */
    val artworkBounds: LogoRect = {logo_rect_literal(bounds)}

    val parts: Map<RepzLogoPart, LogoRect> =
        mapOf(
{part_entries}
        )

    const val WORDMARK_WIDTH: Float = {kotlin_float(wordmark_width)}
    const val WORDMARK_HEIGHT: Float = {kotlin_float(wordmark_height)}
    const val STACKED_WORDMARK_SCALE: Float = {kotlin_float(stacked_scale)}

    /** Grid units below the plates (brand/LOGO.md §2: "24 units under the plates"). */
    const val STACKED_WORDMARK_TOP: Float = {kotlin_float(stacked_top)}
}}
"""


def render_wordmark_vector_xml(width: float, height: float, path_data: str) -> str:
    width_str = kotlin_float(width)[:-1]  # drop the Kotlin `f` suffix; reuse the same int/decimal formatting
    height_str = kotlin_float(height)[:-1]
    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        "<!-- Generated by brand/logo/src/compose_logo.py from brand/logo/master/wordmark.svg. "
        "Do not edit by hand. -->\n"
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        f'    android:width="{width_str}dp"\n'
        f'    android:height="{height_str}dp"\n'
        f'    android:viewportWidth="{width_str}"\n'
        f'    android:viewportHeight="{height_str}">\n'
        "    <path\n"
        '        android:fillColor="#FF000000"\n'
        f'        android:pathData="{path_data}"/>\n'
        "</vector>\n"
    )


# brand/LOGO.md §2: "symbol grid 320 × 160".
LOGO_GRID_WIDTH = 320.0
LOGO_GRID_HEIGHT = 160.0


def write(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content)


def main() -> None:
    parts = parse_symbol_parts()
    bounds = artwork_bounds(parts)
    wordmark_width, wordmark_height = parse_wordmark_viewbox()
    wordmark_path_data = parse_wordmark_path()
    _translate_x, stacked_top, stacked_scale = parse_stacked_wordmark_placement()

    write(
        KOTLIN_LOGO_DIR / "RepzLogoGeometry.kt",
        render_kotlin(parts, bounds, wordmark_width, wordmark_height, stacked_scale, stacked_top),
    )
    write(
        DRAWABLE_DIR / "logo_wordmark.xml",
        render_wordmark_vector_xml(wordmark_width, wordmark_height, wordmark_path_data),
    )


if __name__ == "__main__":
    main()
