# Repz — Design System

> Source: Claude Design export **v0.2** (`design/repz-design-v0.2.html`, local only, git-ignored), boards 00–18.
> This document is the **committed source of truth** for implementation. If it disagrees with the design file or you need a value that is not here: **stop and ask**. Never guess a colour, size or spring.
> Motion lives in [`MOTION.md`](MOTION.md); the logo in [`../brand/LOGO.md`](../brand/LOGO.md).

## 1. Principles

1. **Calm while logging:** quiet surfaces, huge tabular numbers, one accent. Every logging motion is under 300 ms.
2. **Loud at the right moment:** only PRs, a finished workout, a streak milestone and the end of a rest get bouncy springs, particles and gold.
3. **Thumb-first:** primary actions in the bottom 40 %, 48 dp minimum touch target, 56 dp for logging.
4. **Offline is normal:** offline is communicated quietly (icon + one line), never as an error.
5. **Dark is the lead look**, but the theme follows the system by default.

## 2. Kotlin mapping

Everything is exposed through `core:designsystem`. Features never use raw `MaterialTheme` values for brand concepts and never hard-code colours, sizes or springs.

| Token group | Kotlin |
|---|---|
| `color.*` (M3 roles) | `MaterialTheme.colorScheme.*` (set by `RepzTheme`) |
| `color.extended.*` | `RepzTheme.colors.*` (e.g. `RepzTheme.colors.recordContainer`) |
| `type.*` | `MaterialTheme.typography.*` + `RepzTheme.typography.numericHero/Large/Medium/Small/heroLabel` |
| `shape.*` | `MaterialTheme.shapes.*` (the §5 scale) + `RepzShapes.*` (`none`, `full`, accents) |
| `spacing.*` | `RepzSpacing.xxs … huge` |
| `layout.*` | `RepzLayout.*` (§6: screen margins, row/list/bar heights, touch targets, wide-window pane widths) |
| `elevation.*` | `RepzElevation.level0 … level4`, `RepzElevation.fab`: a `RepzElevationLevel(containerColor, shadow)` (§7) |
| `motion.*` | `RepzMotion.springs.*()`, `RepzMotion.effectsSprings.*()`, `RepzMotion.tweens.*()`, `RepzMotion.durations.*`, `RepzMotion.easing.*`, `RepzMotion.stagger.*` (see MOTION.md) |
| haptics | `RepzHaptics.current` (MOTION §3) |

`RepzTheme(darkTheme = isSystemInDarkTheme(), dynamicColor = false, reducedMotion: Boolean? = null, hapticsEnabled = true, content)` wraps `MaterialExpressiveTheme` and passes it the Repz `MotionScheme`. `reducedMotion = null` follows the platform setting (ADR-0024). Dynamic colour (Android 12+, opt-in in Settings; ignored on iOS and older Android) replaces **only** the M3 roles. `RepzTheme.colors` (`brand*`, `record*`, set-type, muscle and chart colours) depends only on `darkTheme` and **never** changes. `RepzTheme.colors` and `RepzTheme.typography` are the only accessors on `RepzTheme`; shapes, spacing and elevation are the standalone objects above.

`RepzShapes`: `none` (`RectangleShape`), `full` (`CircleShape`, 50 %) and the 9 §5 accents as `RoundedPolygon` aliases of `MaterialShapes`: `pill`, `cookie4Sided`, `cookie9Sided`, `sunny`, `softBurst`, `burst`, `gem`, `clover4Leaf`, `flower`. Turn one into a `Shape` with `.toShape()`. Morph pairs (§5) live in `RepzMorphs`; `RepzMorphShape(morph, progress)` turns one into a `Shape`.

## 3. Colour

M3 scheme, **Fidelity** variant, seed `#00C2E0` (brandCyan). `onPrimaryContainer` is overridden to brandInk; on-secondary-container is tone 10/90 for AA.

**Rule:** text or icons on cyan are **ink (`#0E1416`), never white** (white on cyan fails AA).

### 3.1 M3 roles

| Role | Light | Dark | Usage |
|---|---|---|---|
| `primary` | `#006879` | `#46DEFD` | Filled buttons, links, focused field ring |
| `onPrimary` | `#FFFFFF` | `#003640` |  |
| `primaryContainer` | `#00C2E0` | `#00C2E0` | Hero button, FAB, rest pill, selected key (= brandCyan) |
| `onPrimaryContainer` | `#0E1416` | `#0E1416` | Overridden to brandInk (8.66:1). Never white on cyan |
| `inversePrimary` | `#3CD8F6` | `#006879` |  |
| `secondary` | `#356570` | `#9ECEDB` |  |
| `onSecondary` | `#FFFFFF` | `#003640` |  |
| `secondaryContainer` | `#B7E8F5` | `#1A4D58` | Tonal buttons, nav indicator, selected chips |
| `onSecondaryContainer` | `#061F24` | `#CEE7EE` |  |
| `tertiary` | `#8D4F00` | `#FFC086` |  |
| `onTertiary` | `#FFFFFF` | `#4B2800` |  |
| `tertiaryContainer` | `#F79C3C` | `#F79C3C` | Rest timer last 5 s |
| `onTertiaryContainer` | `#2E1500` | `#2E1500` |  |
| `error` | `#BA1A1A` | `#FFB4AB` | Destructive actions, field errors |
| `onError` | `#FFFFFF` | `#690005` |  |
| `errorContainer` | `#FFDAD6` | `#93000A` |  |
| `onErrorContainer` | `#93000A` | `#FFDAD6` |  |
| `background` | `#F4FAFD` | `#0E1416` |  |
| `onBackground` | `#161D1E` | `#DDE3E6` |  |
| `surface` | `#F4FAFD` | `#0E1416` | Screen background |
| `onSurface` | `#161D1E` | `#DDE3E6` |  |
| `surfaceVariant` | `#D7E5E9` | `#3C494C` |  |
| `onSurfaceVariant` | `#3C494C` | `#BBC9CD` | Secondary text, previous-set ghost text |
| `surfaceTint` | `#006879` | `#3CD8F6` |  |
| `inverseSurface` | `#2B3133` | `#DDE3E6` |  |
| `inverseOnSurface` | `#ECF2F4` | `#2B3133` |  |
| `outline` | `#6C797D` | `#869397` | Empty checkbox, outlined buttons (non-text, 3:1) |
| `outlineVariant` | `#BBC9CD` | `#3C494C` |  |
| `scrim` | `#000000` | `#000000` |  |
| `surfaceBright` | `#F4FAFD` | `#343A3C` |  |
| `surfaceDim` | `#D5DBDD` | `#0E1416` |  |
| `surfaceContainerLowest` | `#FFFFFF` | `#090F11` |  |
| `surfaceContainerLow` | `#EEF5F7` | `#161D1E` | Nav bar, header, sections |
| `surfaceContainer` | `#E9EFF1` | `#1A2122` | Cards |
| `surfaceContainerHigh` | `#E3E9EB` | `#252B2D` | Set rows, sheets, dialogs |
| `surfaceContainerHighest` | `#DDE3E6` | `#2F3638` | Number fields, keypad keys, dragged item |

### 3.2 Extended (fixed) colours: `RepzTheme.colors`

Each semantic colour has `x / onX / xContainer / onXContainer`.

| Token | Light | Dark | Usage |
|---|---|---|---|
| `brandCyan` | `#00C2E0` | `#00C2E0` | Logo, app-icon field, seed (fixed) |
| `brandDeep` | `#00687A` | `#00687A` | Logo on light surfaces (fixed) |
| `brandInk` | `#0E1416` | `#0E1416` | Logo on cyan, dark background (fixed) |
| `brandPaper` | `#F5FAFB` | `#F5FAFB` | Light logo tiles |
| `brandMist` | `#A6EEFF` | `#A6EEFF` | Labels, glows on dark |
| `success` | `#296B2A` | `#91D888` | Set completed, synced |
| `onSuccess` | `#FFFFFF` | `#003908` |  |
| `successContainer` | `#ACF4A2` | `#0B5314` | Completed set row |
| `onSuccessContainer` | `#002203` | `#ACF4A2` |  |
| `warning` | `#865300` | `#FFB960` | Pending, offline icon |
| `onWarning` | `#FFFFFF` | `#472A00` |  |
| `warningContainer` | `#FFDDB8` | `#653E00` | Unsynced warning card |
| `onWarningContainer` | `#2B1700` | `#FFDDB8` |  |
| `record` | `#896500` | `#FCBC00` | PR text and icons (fixed) |
| `onRecord` | `#FFFFFF` | `#261900` |  |
| `recordContainer` | `#FFDEA0` | `#5C4300` | PR row, PR cards (fixed) |
| `onRecordContainer` | `#261900` | `#FFDEA0` |  |
| `recordBright` | `#FCBC00` | `#FCBC00` | Glow, particles, Gem, Burst. **Never text on light** |
| `setWarmup` | `#4359A9` | `#B7C4FF` | W badge (indigo, distinct from brand cyan) |
| `onSetWarmup` | `#FFFFFF` | `#0B2878` |  |
| `setWarmupContainer` | `#DCE1FF` | `#294190` |  |
| `onSetWarmupContainer` | `#001551` | `#DCE1FF` |  |
| `setNormal` | `#595F61` | `#BBC9CD` | Numbered badge |
| `onSetNormal` | `#FFFFFF` | `#2B3133` |  |
| `setNormalContainer` | `#DDE3E6` | `#2F3638` |  |
| `onSetNormalContainer` | `#161D1E` | `#DDE3E6` |  |
| `setDrop` | `#8D3F92` | `#FFA9FF` | D badge |
| `onSetDrop` | `#FFFFFF` | `#57065F` |  |
| `setDropContainer` | `#FFD6FB` | `#712578` |  |
| `onSetDropContainer` | `#36003D` | `#FFD6FB` |  |
| `setFailure` | `#AF2E27` | `#FFB4AB` | F badge |
| `onSetFailure` | `#FFFFFF` | `#690005` |  |
| `setFailureContainer` | `#FFDAD6` | `#8E1313` |  |
| `onSetFailureContainer` | `#410002` | `#FFDAD6` |  |
| `musclePrimary` | `#F04600` | `#FF7348` | Primary muscles (graphic only in light, 3.57:1) |
| `muscleSecondary` | `#FFB779` | `#C56E00` | Secondary muscles |
| `muscleInactive` | `#DEE3E5` | `#404749` | Inactive regions |
| `chartSeries1` | `#006879` | `#46DEFD` | Est. 1RM line |
| `chartSeries2` | `#B34B00` | `#FFA273` | Top set line |
| `chartSeries3` | `#6F44FC` | `#BDAEFF` | Volume line |
| `chartGrid` | `#DEE3E5` | `#363C3E` | Gridlines (decor) |
| `chartAxis` | `#666C6D` | `#A6ACAE` | Axis labels |

The set-type on-colours (`onSetX`, `onSetXContainer`) are taken from board 01 (decided 2026-11-02).

### 3.3 Contrast (verified by the designer)

All text pairs ≥ 4.5:1: onSurface/surface 16.23 / 14.34; primary/surface 6.12 / 11.61; onPrimaryContainer/primaryContainer 8.66; record/surface 5.08 / 10.91; setWarmup/surface 6.15 / 10.93. Graphics only: musclePrimary/surface (light) 3.57 (never use it for text).

## 4. Typography

Fonts (OFL, both cover ą ć ę ł ń ó ś ź ż). Bundle as Compose resources:
- **Archivo** (variable: wght 100–900, wdth 62–125): display, headlines and **every number**.
- **Inter** (variable: wght 100–900, opsz 14–32): titles, body, labels.

Sources (`core:designsystem` `composeResources`, from `github.com/google/fonts` at pinned commits):

| File | Source @ commit | SHA-256 |
|---|---|---|
| `font/archivo_variable.ttf` | `ofl/archivo/Archivo[wdth,wght].ttf` @ `95f4904fc8bcf26d3420fe315560c96417c6dec7` | `0e094a7d3c7c4c25cf1310c4b30014f1dae9332220b1c2c88f4fa996f0b05053` |
| `font/inter_variable.ttf` | `ofl/inter/Inter[opsz,wght].ttf` @ `0b58fb370093f9a9f4ff785d94405710b79de67c` | `29160a80ff49ddcab2c97711247e08b1fab27a484a329ce8b813d820dc559031` |
| `files/licenses/archivo_ofl.txt`, `inter_ofl.txt` | `ofl/<family>/OFL.txt`, same commits | |

The OFL texts ship with the app (redistribution condition; shown later in F26 About & licenses). Archivo has no U+202F (narrow no-break space); EN and PL only need U+00A0, but check this before adding a locale that groups digits with U+202F.

Axes per style: Archivo uses `wght` and `wdth` (100 unless stated). Inter uses `wght` and **`opsz` = the font size in sp, clamped to 14–32**. Every distinct axis combination is its own `FontFamily`.

| Style | Font · size/line · weight · tracking |
|---|---|
| displayLarge | Archivo 57/64 · 700 · −1 |
| displayMedium | Archivo 45/52 · 700 · −0.5 |
| displaySmall | Archivo 36/44 · 700 · −0.25 |
| headlineLarge | Archivo 32/40 · 700 · −0.25 |
| headlineMedium | Archivo 28/36 · 700 · 0 |
| headlineSmall | Archivo 24/32 · 700 · 0 |
| titleLarge | Inter 22/28 · 600 · 0 |
| titleMedium | Inter 16/24 · 600 · +0.15 |
| titleSmall | Inter 14/20 · 600 · +0.1 |
| bodyLarge | Inter 16/24 · 400 · +0.15 |
| bodyMedium | Inter 14/20 · 400 · +0.2 |
| bodySmall | Inter 12/16 · 400 · +0.3 |
| labelLarge | Inter 14/20 · 600 · +0.1 |
| labelMedium | Inter 12/16 · 600 · +0.4 |
| labelSmall | Inter 11/16 · 600 · +0.5 |
| *Emphasized | see the emphasized table below |
| **numericHero** | Archivo 96/96 · 800 · wdth 112 · −2 · `tnum` |
| **numericLarge** | Archivo 32/36 · 700 · wdth 100 · −0.5 · `tnum` |
| **numericMedium** | Archivo 22/28 · 650 · wdth 100 · 0 · `tnum` |
| **numericSmall** | Archivo 14/20 · 600 · wdth 100 · +0.1 · `tnum` |
| **heroLabel** | Archivo 22/28 · 750 · wdth 105 · 0 (the `RepzHeroStartButton` label; user decision 2026-11-06, §13) |

Archivo's digits are already near-tabular in the pinned file (advances 585–587/1000 for 0–9), so `tnum` mostly guarantees alignment rather than changing it visibly. The feature string reaches the font on Android (verified with `zero`); iOS has not been checked for font features yet.

Numeric styles and `heroLabel` live in `RepzTheme.typography`; the 30 others in `MaterialTheme.typography`. The wdth 100 and the tracking 0 of `numericMedium` are taken from board 02 (decided 2026-11-02).

**Emphasized styles** (rule decided 2026-11-02, from board 02): same size and line height as the baseline style, and
- display: Archivo 850, wdth 112, tracking = baseline − 0.5;
- headline: Archivo 850, wdth 108, tracking = baseline − 0.25;
- title, body, label: Inter 700, baseline tracking.

| Style | Font · size/line · weight · wdth · tracking |
|---|---|
| displayLargeEmphasized | Archivo 57/64 · 850 · 112 · −1.5 |
| displayMediumEmphasized | Archivo 45/52 · 850 · 112 · −1 |
| displaySmallEmphasized | Archivo 36/44 · 850 · 112 · −0.75 |
| headlineLargeEmphasized | Archivo 32/40 · 850 · 108 · −0.5 |
| headlineMediumEmphasized | Archivo 28/36 · 850 · 108 · −0.25 |
| headlineSmallEmphasized | Archivo 24/32 · 850 · 108 · −0.25 |
| titleLargeEmphasized | Inter 22/28 · 700 · – · 0 |
| titleMediumEmphasized | Inter 16/24 · 700 · – · +0.15 |
| titleSmallEmphasized | Inter 14/20 · 700 · – · +0.1 |
| bodyLargeEmphasized | Inter 16/24 · 700 · – · +0.15 |
| bodyMediumEmphasized | Inter 14/20 · 700 · – · +0.2 |
| bodySmallEmphasized | Inter 12/16 · 700 · – · +0.3 |
| labelLargeEmphasized | Inter 14/20 · 700 · – · +0.1 |
| labelMediumEmphasized | Inter 12/16 · 700 · – · +0.4 |
| labelSmallEmphasized | Inter 11/16 · 700 · – · +0.5 |

Rules:
- Every number uses a `numeric*` style with `fontFeatureSettings = "tnum"`, so timers never jitter.
- Headlines may compress to **wdth 88** (never below) before wrapping. **Buttons always wrap, never truncate.**
- Number formatting is locale-aware: EN `82.5 kg`, `12,340 kg`; PL `82,5 kg`, `12 340 kg` (comma decimal, non-breaking-space thousands).
- The separator between a number and its unit is U+00A0 (no-break space), e.g. `82,5 kg`, so the unit never wraps onto its own line.

## 5. Shape

| Token | dp | Used for |
|---|---|---|
| none | 0 | |
| extraSmall | 4 | |
| small | 8 | chips, text fields (filled top corners) |
| medium | 12 | set rows, pressed buttons |
| large | 16 | cards, exercise cards |
| largeIncreased | 20 | FAB, mini bar |
| extraLarge | 28 | bottom sheets, dialogs |
| extraLargeIncreased | 32 | phone-sized surfaces |
| extraExtraLarge | 48 | |
| full | 50 % | buttons at rest, selected chips |

Buttons are `full` at rest and morph to `medium` (hero: radius 22) on press (M3 Expressive press morph, `pressMorph` spring).

**MaterialShapes (accents only):**

| Shape | Use |
|---|---|
| Pill | Hero "Start workout", rest pill |
| Cookie4Sided | Square FAB pressed (not the extended FAB, §13), empty-state backdrop, illustration backdrop |
| Cookie9Sided | Avatar, weekly streak ring core, Welcome pager backdrop (§13) |
| Sunny | Rest timer while counting (slow breathe) |
| SoftBurst / Burst | Rest finished "Go!", PR flash |
| Gem | PR badge container |
| Clover4Leaf | Muscle-group chip leading shape |
| Flower | Workout summary header, restore progress |

**Morph pairs** (`androidx.graphics.shapes.Morph`):
- Circle → Burst: rest timer hits 0.
- Sunny ↔ Circle: rest breathing, 4 s period, ±3 %.
- Square(20) → Cookie4Sided: the square FAB's and the FAB-menu toggle's press-and-hold (REP-22, T1; the FAB's own rest radius is `largeIncreased` 20 dp, matching the shape-scale table above). The extended FAB does not use this morph (§13): its wide bounds would stretch Cookie4Sided into an unreadable blob, so its press is the plain `RepzButton` corner morph instead.
- Pill → RoundedRect(12): all buttons on press.
- Circle → RoundedSquare(10): set checkbox on complete.
- Circle → Gem: PR badge pop-in.
- Circle → Flower → Cookie9: restore progress 0 / 50 / 100 %.

## 6. Spacing & layout

Targets: phones only (iPhone only on iOS), **portrait only** on both platforms (ADR-0017). Wide-window layouts below apply to Android foldables/tablets where the system may ignore the lock.

Spacing (4 dp grid): `xxs 2 · xs 4 · sm 8 · md 12 · lg 16 · xl 24 · xxl 32 · xxxl 48 · huge 64`.

| Layout | dp |
|---|---|
| Screen margin compact / medium+ | 16 / 24 |
| Card padding / gap in lists | 16 / 12 |
| Set row height | 56 (64 at fontScale > 1.3) |
| List item 1-line / 2-line | 56 / 72 |
| Top app bar small / medium / large | 64 / 112 / 152 |
| Live-workout header | 92 |
| Active-workout mini bar | 64 |
| Navigation bar | 80 |
| Min touch target / logging target | 48 / 56 |

Wide windows (≥ 840 dp): History and Exercises use `ListDetailPaneScaffold` (list pane 360 dp fixed). The live workout stays single-pane, centred, max 600 dp.

## 7. Elevation (tonal first)

| Level | Container | Use |
|---|---|---|
| level0 | surface | screen background |
| level1 | surfaceContainerLow | nav bar, header |
| level2 | surfaceContainer | cards |
| level3 | surfaceContainerHigh | set rows, sheets, dialogs |
| level4 | surfaceContainerHighest + shadow `0 8 20 #0006` (both themes) | dragged item (+ scale 1.02–1.03) |
| fab | primaryContainer + shadow `0 4 12 #0E141638` (light theme only; none in dark) | FAB |

Shadows are `x y blur colour`. The fab shadow keeps board 18's geometry with a neutral ink colour (decided 2026-11-02). In Kotlin, `RepzElevation.level0 … level4` and `RepzElevation.fab` each give a `RepzElevationLevel(containerColor, shadow: Shadow?)`; components draw the shadow with `Modifier.dropShadow(shape, shadow)`.

Shadows are only for things floating under the finger (FAB, dragged item, rest pill over content). Sheets and dialogs use a scrim of `#000` at 32 %.

## 8. Iconography

- **Material Symbols Rounded**, opsz 24, wght 400, grade 0 (dark theme grade −25).
- FILL 0 by default. FILL 1 for: selected nav item, active toggle, completed state, PR trophy.
- Sizes: 20 dp in dense rows, 24 dp default, 28 dp in the nav bar, 16 dp inline in a field's error text, 64 dp for a large placeholder icon centred in an illustration backdrop (§13) — `RepzIconSize` (`core:designsystem`) holds the five as `dense`/`default`/`navBar`/`inline`/`illustration`; a feature never hard-codes an icon size.
- Custom equipment icons (24 dp, stroke): Barbell, Dumbbell, Kettlebell, Machine, Cable, Bodyweight, Band, Other. Path data is on board 03.
- Set types: W / 1·2·3 / D / F letter badges. PR: `trophy`, `workspace_premium`. Sync: `cloud_done`, `cloud_sync`, `cloud_upload`, `cloud_off`, `sync_problem`.
- **Landed (REP-28):** the 4 `RepzNavigationBar` icons (Train/`fitness_center`, History/`history`, Exercises/`exercise`, Profile/`person`, each with its FILL-1 `_filled` variant) as Material Symbols Rounded drawables in `core:designsystem`'s `composeResources/drawable/`, under the Apache License 2.0 (`composeResources/files/licenses/material_symbols_apache2.txt`). `core:designsystem`'s `build.gradle.kts` sets `publicResClass = true`, so its generated resources class is visible to other modules (`app:shared` references `DesignSystemRes.drawable.symbol_*`), not only within `core:designsystem` itself. The remaining icon set lands with REP-72.
- **Landed (REP-30/REP-82):** 5 more Material Symbols Rounded drawables for the auth screens — `arrow_back`, `visibility`, `visibility_off`, `error`, `cloud_off` — same opsz 24/wght 400/FILL 0/grade 0 export and Apache 2.0 licence file as REP-28's set, wrapped by `RepzIcon(icon: RepzIcon, contentDescription: String?, modifier, tint)` (enum `RepzIcon`: `ArrowBack`, `Visibility`, `VisibilityOff`, `Error`, `CloudOff`). Dark-theme grade −25 above isn't achievable with a static vector drawable; still open for REP-72.
- **Landed:** one more Material Symbols Rounded drawable, `trophy`, same export as the sets above, wrapped as `RepzIcon.Trophy`. `RepzIcon.FitnessCenter` is a new enum entry over REP-28's existing `symbol_fitness_center` drawable; `RepzIcon.CloudOff` (REP-30/82) is reused as-is. All three are Welcome's backdrop placeholder icons (§13).
- **Landed (REP-87):** one more Material Symbols Rounded drawable, `logout`, same export as the sets above, wrapped as `RepzIcon.Logout`. Used by the Profile tab's Log out `RepzListItem` row (§9, board 13), tinted `error` via `RepzListItemTone.Destructive`.
- **Landed (REP-22):** 6 more Material Symbols Rounded drawables, same export as the sets above — `symbol_add`, `symbol_close`, `symbol_check`, `symbol_play_arrow_filled` (FILL 1), `symbol_list_alt`, `symbol_keyboard_arrow_down` — wrapped as `RepzIcon.Add`, `Close`, `Check`, `PlayArrowFilled`, `ListAlt`, `KeyboardArrowDown`. `RepzIcon.History` is a new enum entry over the existing `symbol_history` drawable (REP-28); no new file. All 7 are appended after `Logout`, per `RepzIcon.kt`'s own rule that later feature work appends without reordering. Used throughout §9's new components (FAB/FAB menu, chips, connected button group, split buttons, hero button) and their catalog pages.

## 9. Components (`core:designsystem`, prefix `Repz`)

| Component | Key spec |
|---|---|
| `RepzButton` (filled/tonal/outlined/text, XS 32 · S 40 · M 56 · L 96 · XL 136), button group, split button | full → medium on press; loading = contained loading indicator, size stays |
| `HeroStartButton` | height 88, icon disc 56, label `heroLabel` (Archivo 22/28 · 750 · wdth 105, §13); split trailing segment 64 wide (main = empty workout, segment = from template); pill → radius 22 pressed |
| FAB / FAB menu | Square(20) → Cookie4 on hold; menu items stagger 30 ms bottom-up, + rotates to × (MOT-30) |
| `RepzNavigationBar` | 4 tabs Train / History / Exercises / Profile; indicator 64×32 secondaryContainer; MOT-02 |
| Top app bars + `LiveWorkoutHeader` | 64 / 112 / 152; live header 92 with elapsed timer, Finish |
| `ActiveWorkoutMiniBar` | height 64, margin 8, radius 20, above nav bar on every tab except Train; status dot, name + elapsed (numericSmall), next-set hint, rest pill, expand 48 |
| `SetRow` | height 56; radius 12; columns badge 38 · previous 68 · weight 1fr · reps 1fr · [rpe 48] · check 50; container surfaceContainerHigh, fields surfaceContainerHighest; values numericLarge, previous numericSmall/onSurfaceVariant. States: empty, prefilled, editing (2 dp primary inner ring), completed (successContainer), pr (recordContainer + recordBright glow), warmup/drop/failure, swiping (errorContainer underlay), error. Previous column hides < 360 dp width or fontScale ≥ 1.5 |
| `NumberKeypadSheet` | non-modal sheet, radius 28 top; mode segmented Weight/Reps/RPE; display numericHero 52; ± 60 dp; 4×4 grid of 52 dp keys, gap 8; Next spans 2 rows; step from settings (0.5/1.25/2.5/5 kg; 1/2.5/5 lb); "Same as last" chip; quick values; long-press ± 250 ms → 60 ms after 1 s; RPE mode = chips 6 … 10 |
| `ExerciseCard` (live) | header (thumbnail, name = shared element, menu: Reorder · Replace · Notes · Remove), set rows, Add set, rest chip |
| `RestTimer` | collapsed pill 56 (bottom) / 32–36 (header, mini bar), primaryContainer; expanded 260 dp Sunny backdrop + ring (r 39 %, stroke 7 %), numericHero 64, 3 × 56 dp actions (−15 s, +15 s, Skip), next-set card. States: counting, last5s (tertiaryContainer, pulse), finished (Burst, recordBright, "Go!"), resumed from wall clock |
| `ExerciseListItem` / `ExerciseTile` | illustration on Cookie4 backdrop, name, primary muscle chip, equipment icon; selectable variant |
| `TemplateCard`, `HistoryCard`, `TrainingHeatmap`, `StatTile`, `ProgressChart`, `PrBadge` (inline 24 / Gem card / Burst celebration), `MuscleMap` (40 dp / 240 dp+), chips, search bar | see boards 06, 08, 11, 12 |
| `SyncStatusIndicator` | synced · syncing · pending(n) · offline · error; detailed card variant for Settings |
| `RepzLogoSymbol`, `RepzLaunchLogo`, `RepzLoader` | the 7-part brand symbol, MOT-01's launch hand-off and the branded loader (indeterminate + determinate, 24 / 48 / 96 dp, MOT-25); see "Landed" below |
| Banners, snackbar (undo 6 s), dialogs, sheets, M3 Expressive loading indicator, skeleton shimmer (only after a 150 ms delay) | board 06 |
| Form fields | filled/outlined, password toggle + strength meter, errors on blur (not per keystroke), segmented buttons, switch, slider, settings list items |
| Empty states | illustration + title + body + action (MOT-23) |

**Landed (REP-30/REP-82):** the auth-screen subset, built to the signatures the implementation plan fixed in advance:
- `RepzButton(text, onClick, modifier, style: RepzButtonStyle = Filled, size: RepzButtonSize = Medium, enabled, isLoading, loadingContentDescription)` — `Filled`/`Outlined`/`Text`/`Hero` and `Small`(40)/`Medium`(56) exist; the size is a minimum height, so a long or enlarged label wraps onto more lines instead of clipping; `Tonal` and `XS`/`L`/`XL` come with REP-22. Pressed shape (rest `height / 2`, pressed `shapes.medium` 12) and scale 0.96 run on `RepzMotion.springs.pressMorph()` (MOT-22); reduced motion keeps the state layer only and skips the shape/scale change. Loading keeps the button's measured size, replaces the label with M3's own loading indicator, and never reads as disabled to accessibility unless it also is.
- `RepzTextField` / `RepzPasswordField(value, onValueChange, label, modifier, supportingText, isError, enabled, keyboardOptions, keyboardActions, onFocusLoss)` — outlined only (no filled field yet, no strength meter), height 56. An error message is shown with `RepzFieldError(text)` placed directly below the field (pair it with `isError = true`), not through the field: M3 merges a field's supportingText into the field's own accessibility node, and on iOS that text isn't exposed while the field has a value, so VoiceOver would never read it. `RepzFieldError` is its own element with a polite live region; it appears instantly, then fades in over `RepzMotion.tweens.short()`. The parameter is named **`onFocusLoss`**, not `onFocusLost`: Detekt's compose-rules `ParameterNaming` check rejects a past-tense verb on a `@Composable` lambda parameter.
- `RepzTopAppBar(modifier, title, onBackClick, actions, backButtonModifier)` — small bar, height 64. `backButtonModifier` (an addition beyond the original signature) tags the 48 dp back icon button itself, separately from the bar's own `modifier`, so a test tag lands on a tappable target instead of the whole bar.
- `RepzBanner(icon: RepzIcon, title, modifier, body)` — `warningContainer`/`onWarningContainer`, `shapes.large`, polite live region.
- `RepzPageIndicator(pageCount, currentPage, contentDescription, modifier)` — the Welcome pager's dots, active 24×8 / inactive 8×8, gap 6, no animation.
- `RepzSnackbarHostState.showWithAction(message, actionLabel): Boolean` + `rememberRepzSnackbarHostState()` / `RepzSnackbarHost` — wraps M3's `SnackbarHostState`; the `Boolean` is whether the action was tapped. `dismissCurrent()` hides the visible snackbar, so a stale message never survives a resubmit; callers show it in its own coroutine so it never blocks their effect collector.
- Two catalog pages, `components-actions` and `components-inputs` (ARCHITECTURE §2), each fitting one iPhone Duo screen.

**Landed:** `RepzButtonStyle.Hero` — container `primaryContainer`, content `onPrimaryContainer` (brandInk), same shape, sizes, disabled state and MOT-22 press/loading behaviour as `Filled`. Used for Welcome's "Create account" only (§13); every other CTA (Register, Log in submit, Welcome's "Log in") keeps `Filled`/`Outlined` per the 2026-11-03 decision. Shown in `components-actions` alongside the other three styles.

**Landed (REP-21):** the logo family (`core:designsystem`'s `logo` package), geometry generated from the brand sources (`brand/logo/src/compose_logo.py` → `RepzLogoGeometry`, never hand-edited):
- `RepzLogoSymbol(modifier, color, state: RepzLogoSymbolState, contentDescription)` — the 7-part symbol (`RepzLogoPart`), sized by its own artwork width (2.3125 : 1). `RepzLogoSymbolState`/`rememberRepzLogoSymbolState(initialPose: RepzLogoPose)` holds one `Animatable` per animated value; `RepzLogoPose` is `BarOnly` (every plate retracted — the splash/launch-layer rest state) or `Assembled` (the finished frame). A caller passing `contentDescription` gets `Role.Image` semantics; leaving it `null` makes the symbol purely decorative.
- `RepzLaunchLogo(playback: RepzLaunchLogoPlayback, onFinish, modifier, color, symbolWidth = RepzLogoDefaults.SplashSymbolWidth, skipRequests)` — MOT-01: the symbol plus the wordmark below it in stacked-lockup proportions. `RepzLaunchLogoPlayback` is `StaticSymbol` (the bar-only splash frame, no animation) or `LoadTheBar` (plays the choreography once). `onFinish` fires after the post-wordmark hold, or immediately on a tap; `skipRequests` lets a caller wrapping this in its own full-window touch layer (the launch hand-off) forward a tap from outside this composable's own bounds.
- `RepzLoader(contentDescription, modifier, size: RepzLoaderSize = Medium, progress: Float? = null, color = RepzLogoDefaults.loaderColor())` — `RepzLoaderSize` is `Small`(24.dp) / `Medium`(48.dp) / `Large`(96.dp). `contentDescription` is required, unlike the symbol's: a loader is never purely decorative. `progressSemantics()`/`progressSemantics(progress)` exposes the state to accessibility.
- `RepzLogoDefaults`: `SplashSymbolWidth` (144.dp); `color()` — `brandDeep` light / `brandCyan` dark, the same colourway as the OS splash; `loaderColor()` — `MaterialTheme.colorScheme.primary`, so the in-app loader follows dynamic colour; `pendingOutlineColor()` — `MaterialTheme.colorScheme.outlineVariant`, not overridable (marks an empty loader slot, never the brand mark).
- Two catalog pages (ARCHITECTURE §2): `logo` (static symbol at 24/48/96/144 dp in both colourways, `RepzLaunchLogo` replaying every 3 s) and `logo-loader` (indeterminate at all 3 sizes; determinate at 0 / ⅓ / ⅔ / 1; one stepping specimen). Both auto-play and fit one iPhone Duo screen.

**Landed (REP-87):** the Profile tab's header and log-out subset (board 13, board 25), new files only, no catalog page yet (REP-22 adds one):
- `RepzAlertDialog(title, text, confirmText, onConfirm, dismissText, onDismissRequest, modifier, confirmButtonModifier, dismissButtonModifier, isConfirmDestructive = false)` — M3 `AlertDialog` (ADR-0006) in `surfaceContainerHigh`, `shapes.extraLarge` (28 dp), no icon, centred `headlineSmall` title + centred `bodyMedium`/`onSurfaceVariant` text, end-aligned `dismissText` then `confirmText` text buttons (`confirmText` in `error` when `isConfirmDestructive`, §13 Q4). `onDismissRequest` also fires on Back and a scrim tap. `confirmButtonModifier`/`dismissButtonModifier` tag the buttons themselves, the same precedent as `RepzTopAppBar`'s `backButtonModifier`. The scrim is §7's 32 % `#000`, drawn via `DialogProperties(scrimColor = …)` on iOS and `Window.setDimAmount` on Android (Compose Multiplatform 1.12.2's Android `DialogProperties` has no scrim parameter, §13).
- `RepzListItem(headline, onClick, modifier, leadingIcon: RepzIcon? = null, tone: RepzListItemTone = Default)` with `enum RepzListItemTone { Default, Destructive }` — a 56 dp minimum-height settings-style row (board 13 `.li`): 16 dp horizontal padding, `leadingIcon` at `RepzIconSize.default` with a 14 dp icon-text gap (component-internal, exact, Q12 precedent), `headline` in `bodyLarge`, full-row ripple and `Role.Button`; `Destructive` colours both the icon and the text `error` (the Log out row). `headline` always wraps, never truncates.
- `RepzAvatar(monogram: String?, modifier)` — `RepzShapes.cookie9Sided` clipped, 68 dp (board 13, exact), `monogram` in `headlineMediumEmphasized`/`onPrimaryContainer` on `primaryContainer`; `null` renders an empty `surfaceContainerHigh` placeholder (§13 Q9). Decorative (`clearAndSetSemantics {}`); the parent row's name text carries the accessible label.

Profile (`feature:settings:impl`) composes these into the tab root: the header (avatar + display name + email, from Room `current_user`, ARCHITECTURE §5.2) directly above the destructive Log out `RepzListItem`, and `RepzAlertDialog` for the F25 normal-variant confirmation — no section caption, no top app bar, no other rows yet (§13 Q5).

**Landed (REP-22):** the rest of this section's generic component set, all in `core:designsystem`, each with a light/dark `@Preview` per state and a catalog page (ARCHITECTURE §2):
- `RepzButton` gains `RepzButtonStyle.Tonal` (`secondaryContainer`/`onSecondaryContainer`), `RepzButtonSize.ExtraSmall` (32), `Large` (96) and `ExtraLarge` (136), and an optional decorative `leadingIcon: RepzIcon? = null` sized/spaced via M3's own `ButtonDefaults.iconSizeFor`/`iconSpacingFor`. Every size's pressed radius (12 dp, MOT-22) is unchanged and now shared by `Tonal` too; `L`/`XL` labels use `headlineSmall`/`headlineLarge` (§13).
- `RepzConnectedButtonGroup(options: ImmutableList<String>, selectedIndex, onSelectedIndexChange, modifier, enabled)` — board 04 "kg / lb": 40 dp tall segments 2 dp apart, `selectableGroup()` + `Role.RadioButton` per option, selected = `primary`/`onPrimary` with an 18 dp leading check, unselected = `surfaceContainerHighest`/`onSurfaceVariant`. MOT-22: only the pressed option's *outer* corners morph to 12 dp and it scales to 0.96; corners shared with a neighbour never move.
- `RepzSplitButton(text, onClick, onTrailingClick, trailingContentDescription, modifier, enabled)` — board 04 "Finish ▾": a 40 dp `Filled` pair of segments (leading label, trailing 44 dp `KeyboardArrowDown`), the same per-segment pressed-corner rule as the connected group.
- `RepzHeroStartButton(text, onClick, modifier, enabled)` / `RepzHeroStartSplitButton(text, onClick, secondaryContentDescription, onSecondaryClick, modifier, enabled)` — boards 04/08/18: 88 dp minimum height, `primaryContainer`/`onPrimaryContainer`, a 56 dp icon disc (`onPrimaryContainer` at 12 % alpha, `PlayArrowFilled` 32 dp), label in the new `RepzTheme.typography.heroLabel` token (§13). MOT-22/MOT-03 press stage only: container pill → 22 dp radius and the disc circle → 16 dp squircle, both scaling to 0.96; the split variant morphs only the pressed segment's outer corners, the shared 12 dp corner never moves. `TODO(REP-48)`: the container-transform-into-the-live-screen stage.
- `RepzFab(icon, contentDescription, onClick, modifier)` / `RepzExtendedFab(text, icon, onClick, modifier)` — board 04: 64 dp minimum, `largeIncreased` (20 dp) square, `primaryContainer`/`onPrimaryContainer`, the `RepzElevation.fab` shadow (light theme only). `RepzFab`'s press-and-hold morphs Square(20) → Cookie4Sided (MOT-30a); `RepzExtendedFab`'s press is the plain `RepzButton` MOT-22 corner morph (20 → 12 dp + scale 0.96) instead — no Cookie4 (§13). `RepzFabMenu(expanded, onExpandedChange, collapsedContentDescription, items: ImmutableList<RepzFabMenuItem>, modifier)` (`RepzFabMenuItem(text, icon, onClick)`) drives MOT-30 end to end (MOTION.md): press-and-hold Cookie4 morph on the toggle, 64 dp square → 56 dp circle open/close, the `+` glyph rotating to `×`, items entering/leaving 30 ms apart bottom-up/top-down, and closing on the toggle, an item, or system Back (`androidx.navigationevent.compose.NavigationBackHandler`, `org.jetbrains.androidx.navigationevent:navigationevent-compose` 1.1.0, ARCHITECTURE §11).
- `RepzNavigationBar`/`RepzNavigationBarItem`: signatures unchanged, now with explicit board-04 colours (indicator `secondaryContainer`, selected icon `onSecondaryContainer`, selected label `onSurface`, unselected icon/label `onSurfaceVariant`) and label weight (`labelMediumEmphasized` selected / `labelMedium` otherwise, §13); container stays `surfaceContainerLow` (§13). `TODO(REP-48)` remains for MOT-02.
- `RepzMediumTopAppBar`/`RepzLargeTopAppBar(title, modifier, backButtonModifier, onBackClick, actions, scrollState: RepzTopAppBarScrollState? = null)` — 112/152 dp expanded, collapsing to the same 64 dp small-bar height as `rememberRepzTopAppBarScrollState()` + `Modifier.repzTopAppBarScroll(state)` report scroll, `surface` container; these two functions are the only M3 top-app-bar-scroll types a feature ever touches.
- `RepzFilterChip(label, selected, onSelectedChange, modifier, leadingIcon, enabled)`, `RepzAssistChip(label, onClick, modifier, leadingIcon, enabled)`, `RepzInputChip(label, onRemove, modifier, enabled)` — board 04 §17, 32 dp tall (48 dp touch target). Toggling a filter chip always fires `RepzHaptics.confirm()`.
- `RepzTextField`/`RepzPasswordField` gain a last `style: RepzTextFieldStyle = Outlined` parameter (`Outlined` unchanged; `Filled` = `surfaceContainerHighest`, top corners only, board 04 §21), source-compatible with every existing caller.
- `RepzSwitch(checked, onCheckedChange, modifier, enabled)` — M3's own 52×32 `Switch`, with a 16 dp check glyph filling the thumb while checked.
- `RepzSlider(value, onValueChange, valueText, modifier, valueRange, steps, onValueChangeFinished, enabled)` — M3's own Expressive `Slider` defaults (16 dp track, 4×44 dp handle, `primary`/`secondaryContainer`); `valueText` becomes the slider's accessible state description; `RepzHaptics.tick()` fires once per step crossed.
- `RepzAlertDialog` gains an optional centred `icon: RepzIcon? = null` (24 dp, `error` tinted when `isConfirmDestructive` else `secondary`) and `confirmEmphasis: RepzDialogConfirmEmphasis = Text`, both additive (§13) — the Log out dialog is unchanged.
- `RepzSnackbarHostState` gains `showUndo(message, undoLabel): Boolean` (shown via an internal `RepzUndoSnackbarVisuals`, `Indefinite`, auto-dismissed by `RepzSnackbarHost` after an accessibility-adjusted ~6 s, §13) and `showMessage(message)` (M3's own `Short` default); the host's action is now styled `labelLargeEmphasized`/`inversePrimary` and the snackbar has a 48 dp minimum height (board 06). `RepzSnackbarHost` tells the undo snackbar apart from an `Indefinite` `showWithAction` one (M3 defaults to `Indefinite` whenever an action label is set) by its visuals' type, not its duration, so existing `showWithAction` callers keep M3's own non-timing-out behaviour.
- `RepzBanner` gains `style: RepzBannerStyle = Warning` (`Warning` unchanged; `Neutral` = board 06's thin strip — `surfaceContainerHigh`/`onSurfaceVariant`, `shapes.medium`, 40 dp min height, an 18 dp icon tinted `RepzTheme.colors.warning`, §13), additive.
- `RepzLoadingIndicator(contentDescription, modifier, contained = false)` — M3's own Expressive shape-loop `LoadingIndicator` (48 dp) or, when `contained`, `ContainedLoadingIndicator` on `secondaryContainer`/`onSecondaryContainer` (board 06); not used on the live workout screen.
- `RepzEmptyState(title, body, illustration, modifier, actions = {})` — boards 11/12: a 150 dp `cookie4Sided`/`surfaceContainerHigh` backdrop, `headlineSmallEmphasized` title, `bodyLarge` body (§13), a caller-built `actions` slot. Centred layout only (board 06's horizontal card variant isn't built). `TODO(REP-49)`: MOT-23's idle loop.
- `RepzSkeleton(modifier, contentDescription, content: @Composable RepzSkeletonScope.() -> Unit)` with `RepzSkeletonScope.skeletonBone(shape): Modifier` — renders nothing for the first `RepzMotion.durations.skeletonDelay` (150 ms) of its own composition, then shows every marked bone in `surfaceContainerHighest` with one shared 1.2 s linear diagonal `onSurface`-at-8%-alpha shimmer band sweeping them together (§13); reduced motion drops the band and keeps the bones static. One accessibility node for the whole skeleton.
- Tokens: `RepzLayout` (theme/RepzLayout.kt, §6's values) and `RepzMotion.durations.skeletonDelay` (150 ms, MOTION §2). `core:designsystem` adds its own `api(kotlinx-collections-immutable)` dependency (ARCHITECTURE §11) for the `ImmutableList` parameters above.
- 8 new catalog pages (ARCHITECTURE §2): `components-buttons`, `components-buttons-large`, `components-fab`, `components-bars`, `components-chips`, `components-controls`, `components-feedback`, `components-states`. `components-actions` now shows only the top app bar and page indicator; its `RepzButton` matrix moved into the two new button pages.

## 10. Screens (inventory)

IDs follow the design brief; flows F1–F5 are on the boards.

| Section | Screens |
|---|---|
| A · Launch & auth | A1 Splash · A2 Welcome (3-page pager, live UI illustrations) · A3 Register · A4 Log in · A5 Restoring history |
| B · Train | B6 Train home (first-time / normal / offline / workout in progress) · B7 Templates list · B8 Template editor · B9 Template preview |
| C · Live workout | C10 Live workout (+ empty, reordering) · C11 Keypad sheet · C12 Rest timer expanded · C13 Exercise picker · C14 Finish confirmation · C15 Workout summary (with/without PRs) · C16 Rest notification (Android + iOS) |
| D · History | D17 History list (+ skeleton, empty, mini bar) · D18 Workout details · D19 Edit finished workout |
| E · Exercises | E20 Exercise library (+ offline, no results) · E21 Exercise details (About / History / Progress / Records) · E22 Create/edit custom exercise |
| F · Profile | F23 Profile & settings (units, theme, dynamic colour, default rest, haptics, language, sync, about, log out) · F24 Sync details · F25 Log-out confirmation (+ unsynced) · F26 About & licenses |
| G · Global | offline banner, error and loading patterns |

Notable behaviours from the design:
- **Empty workout:** auto-named from the weekday. Finish is disabled until one set is completed.
- **Finish with incomplete sets:** "Complete them & finish" checks the remaining sets with their shown values. "Discard" sits far from the thumb.
- **Summary:** shows "Closest to a PR" hints.
- **Swipe-to-delete** is end-to-start only.
- **Inline PR moment** only for est. 1RM and heaviest weight. All 4 PR types appear on the summary. Warm-up sets never count.
- **Exercise picker:** the selection order is the add order.
- **Weekly goal:** default 4 workouts. The setting UI is not designed yet.

## 11. Illustrations

"Capsule figures": a gender-neutral body built from rounded capsules and circles (no face, no shading, no outlines). Working muscles are coloured `musclePrimary` (coral) and `muscleSecondary` (peach). The figure palette is fixed: figure `#999FA1`, plates `#4D5355`, bar ends `#252B2D`.
- **View:** strict side view for sagittal moves, front/back for frontal moves. The pose is the hardest point of the rep.
- **Format:** square, 80 % safe area, transparent background. Master is 1024² lossless WebP, with 512 and 144 px derivatives.
- **In app:** shown on a Cookie4 backdrop in `surfaceContainerHigh`.
- **Generation:** prompt template and negative prompt are on board 16 (`design/boards/16-illustration-style.txt`).

## 12. Accessibility checklist

- **Contrast:** see §3.3. `recordBright` is never used for text on light surfaces.
- **Colour is never the only signal:** completed = green + check; PR = gold + trophy + "PR"; set types = letters; offline = icon + text.
- **Content descriptions:**
  - icon-only buttons: e.g. "Complete set 3, 82.5 kilograms for 8 reps";
  - illustrations: "{exercise} illustration, {primary} highlighted";
  - decorative shapes are hidden.
- **Live workout focus order:** minimize → name → elapsed → Finish → per card: name → options → per set: badge, weight, reps, (RPE), complete → Add set → rest chip → … → rest pill → Add exercise.
- **Announcements:**
  - rest timer: polite live region at 30 s, 10 s and 0;
  - PR: assertive;
  - elapsed timer: not announced.
- **Custom accessibility actions:**
  - set rows: Delete, Mark as warm-up/drop/failure;
  - reorder: Move up/down.
- **Font scaling:** works up to 200 % (set rows reflow at ≥ 1.5). Polish strings get a +30 % length budget.
- **Reduced motion** fallback for every MOT. Haptics can be disabled in Settings and are never the only signal.
- **Keypad and charts:**
  - the keypad is made of real labelled buttons, and hardware digits work;
  - charts have a text summary and focusable scrub points.

## 13. Known deviations from the design (decided)

| Topic | Design v0.2 says | We do | Why |
|---|---|---|---|
| Rest timer in background | Android foreground service | **Scheduled local notification** (`AlarmManager` / `UNUserNotificationCenter`) + `restEndsAt` in DB | ADR-0013: reliable, little native code |
| Haptics toggle, language row | new rows in Settings | adopted | |
| Weekly goal (REP-27, Q3) | default 4, setting not designed | stored in user preferences (`weeklyWorkoutGoal`, workouts/week), default 4, accepted range 1–7; setting UI **open question** | a range was needed to validate the setter; what counts as a week is decided with the Train/History UI |
| Auth CTA colour (REP-30, Q8) | Board 07 draws Create account/Log in on `primaryContainer` | Register/Log in submit buttons: `RepzButtonStyle.Filled` (`primary`), matching board 04 + board 18 (2026-11-03). Welcome's "Create account" only: `RepzButtonStyle.Hero` (`primaryContainer`/`onPrimaryContainer`), matching board 07's own colours exactly; "Log in" stays `Outlined` | board 07 conflicts with the documented colour rules (`primaryContainer` = hero/FAB only) for CTAs generally, but board 07's own treatment is kept for Welcome's single boldest CTA; user decisions 2026-11-03 and 2026-11-05 |
| "Forgot password?" (REP-30, Q4) | shown on Log in | hidden | no password reset in v1 (ARCHITECTURE §1) |
| Terms/Privacy line (REP-30, Q5) | shown on Register | hidden | no Terms/Privacy documents exist yet |
| Password strength meter (REP-30, Q6) | 4-segment meter, advisory copy | password field's supporting text "At least 8 characters", red when violated | no scoring rule or level copy defined; deferred to a follow-up issue |
| Offline banner on auth (REP-30, Q7) | proactive: banner + disabled button, re-enables on reconnect | reactive: banner after a failed attempt, button stays enabled, fades out on the next submit | no feature-facing connectivity monitor exists (REP-26's network monitoring is internal to `core:sync` scheduling) |
| Welcome page sizes (REP-30, Q12) | titles 34/32 px @ weight 800, subtitle 15 px, banner body 13 px, gaps 28/10 px — off the type scale and the 4 dp grid | nearest tokens: titles `headlineLargeEmphasized`; subtitle and welcome body `bodyLarge`; banner title `titleSmall`; banner body `bodyMedium`; 28 → `xl` (24), 10 → `sm` (8). Component-internal sizes (page indicator 24×8 / 8×8, gap 6) stay exact | keeps `core:designsystem` on its token set; board values aren't on it |
| Welcome pager backdrop shape & colour (REP-30, superseded) | board 07's backdrop measures as a 9-lobed `Cookie9Sided` polygon on `surfaceContainer` | `RepzShapes.cookie9Sided` on `surfaceContainer`, matching board 07 exactly, on all 3 Welcome pages | board 07's other illustration backdrops (board 12) sit on `surfaceContainerHigh`, matching §11's default, so the lower elevation and the 9-lobed shape on Welcome read as deliberate rather than a mockup slip; supersedes an earlier same-day decision that had kept `cookie4Sided`/`surfaceContainerHigh` from the docs over the mockup; user decision 2026-11-05 |
| Welcome backdrop placeholder icon | board 07 shows a live UI illustration per page (checkbox morph, cloud-off → check, chart draw-in) | a single large Material Symbols Rounded icon centred in the backdrop — `fitness_center` / `cloud_off` / `trophy` — at `RepzIconSize.illustration` (64 dp), tinted `primary`, decorative (`contentDescription` null) | the live illustrations ship with REP-85; this is a placeholder until then |
| Welcome backdrop size on short screens (REP-30) | board 07 sizes the backdrop at 80 % of the width only | capped at `min(0.8 × width, 0.4 × height)`, with a vertical scroll as a fallback | on short or square-ish screens a width-only size pushed the page text off screen; no-op on regular phones |
| OS splash / MOT-01 fly-out | brand/LOGO.md originally specified the OS splash showing the *complete* symbol, with a "plates fly out" phase before MOT-01 reloads the bar | the OS splash shows only the bar; the app's launch layer draws the identical bar from its first Compose frame (invisible hand-off); MOT-01 starts directly from that bar — no fly-out phase | user decision 2026-11-05 (splash continuity): makes the native-to-Compose hand-off pixel-identical on both platforms instead of needing a separate growth/fly-out animation to disguise it |
| Log-out confirmation layout (REP-87, Q4) | board 25 draws only the **unsynced** variant (count, "Sync now, then log out" / "Log out anyway"); no layout for the plain confirmation board 13's row opens | the same container (`surfaceContainerHigh`, `shapes.extraLarge` 28, padding 24, centred title + body), **no icon** (the `cloud_upload` icon belongs to the unsynced message, not built yet), end-aligned `Cancel` (`primary`) then `Log out` (`error`) text buttons | REP-87 has no outbox writer yet, so only the normal variant exists (REP-45 adds unsynced); user decision 2026-11-05 |
| Log-out dialog scrim (REP-87, Q4) | board 25 draws a roughly 50 % scrim | §7's documented 32 % `#000` scrim, same as every other sheet/dialog | the board's own dialogs aren't the source of truth for a token already fixed elsewhere in this doc; user decision 2026-11-05 |
| Profile header before any profile is cached (REP-87, Q9) | not drawn (board 13 always shows a signed-in account) | same-height header: an empty Cookie9 `RepzAvatar` (`surfaceContainerHigh`, no monogram) and no name/email text, replaced as soon as the Room row arrives; no spinner, no error copy | covers an offline relaunch on a session from before this build, whose profile was never cached |
| HeroStartButton label font (REP-22, U1) | §9 said `titleLargeEmphasized` (Inter 22/28 700); boards 04/08/18 draw Archivo 750, 21–22 px, wdth 105 | new `RepzTheme.typography.heroLabel`: Archivo 22/28, weight 750, width 105, tracking 0 — the boards' own values | the hero is the one place the boards deliberately use the brand display face; one documented token matches it exactly |
| Nav bar container colour (REP-22, U6a) | boards 04/18 draw `surfaceContainer` | kept `surfaceContainerLow` (§7 level1, unchanged) | no reason to diverge from the already-documented elevation level; user default |
| Nav bar label weight (REP-22, U6b) | board 04 draws 500 → 700 on selection, off the type scale | nearest tokens: unselected `labelMedium` (600), selected `labelMediumEmphasized` (700), switched instantly until REP-48 animates it (MOT-02) | keeps `core:designsystem` on its token set (Q12 precedent); user default |
| Button labels L / XL (REP-22, U6c) | board 04 draws Inter 700, 24 / 30 px, off the Inter type scale | L → `headlineSmall` (Archivo 24/32 700), XL → `headlineLarge` (Archivo 32/40 700) — the same tokens M3 Expressive itself uses for those button tiers | nearest existing tokens (Q12); happens to match M3's own size breakpoints exactly; user default |
| Destructive dialog confirm (REP-22, U3) | board 06 ("Discard workout") draws a filled `error` pill plus a centred `error` icon; REP-87 made Log out a text button, no icon | `RepzAlertDialog` gains `icon: RepzIcon?` and `confirmEmphasis: RepzDialogConfirmEmphasis = Text`, additive; `Filled` renders board 06's pill (`error`/`onError` when destructive); Log out keeps `Text`, unchanged | both patterns are real UI (one reversible, one not); chosen per dialog instead of overriding the already-decided Log out look; user decision |
| Skeleton bone colour & shimmer (REP-22, U4) | boards 06/11 use `--skel` (`surfaceContainerHighest` light / `surfaceContainerHigh` dark) and a separate `--skel2` secondary-line colour; no shimmer highlight colour is drawn anywhere | one bone colour, `surfaceContainerHighest` in both themes; shimmer = a 1.2 s linear diagonal `onSurface`-at-8%-alpha band sweeping every bone together; no separate secondary-line colour | a single consistent bone colour across themes; the diagonal band is the nearest convention with no board colour to match; user decision |
| Empty-state typography (REP-22, U6d) | boards 11/12 draw Archivo 800, 24 px; board 06's caption says "titleLarge / Archivo 18–22" | title `headlineSmallEmphasized`, body `bodyLarge`, gap `RepzSpacing.md`; 150 dp backdrop exact (component-internal); centred layout only (board 06's horizontal card variant not built) | nearest tokens (Q12, REP-30 precedent 800 → Emphasized); user default |
| Neutral banner (REP-22, U6f) | board 06 draws 13 px body text, no shared token size | `bodyMedium` body (Q12 banner-body precedent), 18 dp icon tinted `RepzTheme.colors.warning`, `shapes.medium` (12), 40 dp min height | component-internal exact sizes, nearest token for text; user default |
| Chip label (REP-22, U6g) | board 04 draws 600 weight, 13 px, off the Inter type scale | `labelLarge` (Inter 14/20 600) | nearest existing token; user default |
| New `designsystem_*` strings (REP-22, U5) | board 04 draws DOM `aria-label`s ("Close menu", "Remove X") with no Kotlin resource keys | `designsystem_close_menu` ("Close menu"/"Zamknij menu"), `designsystem_remove_item` ("Remove %1$s"/"Usuń %1$s"); `designsystem_loading` ("Loading"/"Wczytywanie") reused as the skeleton/loading-indicator default | EN text taken verbatim from the board's own `aria-label`s; user decision |
| Keyboard focus ring (REP-22, U6e) | board 04 draws a 3 dp `secondary` outline on a focused control | not built in REP-22 | moved to REP-50's accessibility pass (default V-e) |
| Extended FAB press shape (REP-22, round-1 review) | U2(a) said `RepzExtendedFab` also morphs Square(20) → Cookie4Sided on press, like the square FAB | rest stays a true 20 dp `RoundedCornerShape`; press morphs corners 20 → 12 dp with scale 0.96, the same `RepzButton`/MOT-22 corner rule — no Cookie4 | Coach's round-1 review found a Cookie4 polygon stretched across the extended FAB's wide, non-square bounds rendered as an unreadable blob, not the intended shape; user decision |
