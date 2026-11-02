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
| `type.*` | `MaterialTheme.typography.*` + `RepzTheme.typography.numericHero/Large/Medium/Small` |
| `shape.*` | `MaterialTheme.shapes.*` (the §5 scale) + `RepzShapes.*` (`none`, `full`, accents) |
| `spacing.*` | `RepzSpacing.xxs … huge` |
| `elevation.*` | `RepzElevation.level0 … level4`, `RepzElevation.fab`: a `RepzElevationLevel(containerColor, shadow)` (§7) |
| `motion.*` | `RepzMotion.springs.*`, `RepzMotion.durations.*`, `RepzMotion.stagger.*` (see MOTION.md) |

`RepzTheme(darkTheme = isSystemInDarkTheme(), dynamicColor = false, content)` wraps `MaterialExpressiveTheme`. Dynamic colour (Android 12+, opt-in in Settings; ignored on iOS and older Android) replaces **only** the M3 roles. `RepzTheme.colors` (`brand*`, `record*`, set-type, muscle and chart colours) depends only on `darkTheme` and **never** changes. `RepzTheme.colors` and `RepzTheme.typography` are the only accessors on `RepzTheme`; shapes, spacing and elevation are the standalone objects above.

`RepzShapes`: `none` (`RectangleShape`), `full` (`CircleShape`, 50 %) and the 9 §5 accents as `RoundedPolygon` aliases of `MaterialShapes`: `pill`, `cookie4Sided`, `cookie9Sided`, `sunny`, `softBurst`, `burst`, `gem`, `clover4Leaf`, `flower`. Turn one into a `Shape` with `.toShape()`. Morph pairs (§5) arrive with the motion work (REP-20).

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

Archivo's digits are already near-tabular in the pinned file (advances 585–587/1000 for 0–9), so `tnum` mostly guarantees alignment rather than changing it visibly. The feature string still reaches the font on both platforms (verified with `zero`).

Numeric styles live in `RepzTheme.typography`; the 30 others in `MaterialTheme.typography`. The wdth 100 and the tracking 0 of `numericMedium` are taken from board 02 (decided 2026-11-02).

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
| Cookie4Sided | FAB pressed, empty-state backdrop, illustration backdrop |
| Cookie9Sided | Avatar, weekly streak ring core |
| Sunny | Rest timer while counting (slow breathe) |
| SoftBurst / Burst | Rest finished "Go!", PR flash |
| Gem | PR badge container |
| Clover4Leaf | Muscle-group chip leading shape |
| Flower | Workout summary header, restore progress |

**Morph pairs** (`androidx.graphics.shapes.Morph`):
- Circle → Burst: rest timer hits 0.
- Sunny ↔ Circle: rest breathing, 4 s period, ±3 %.
- Square(16) → Cookie4Sided: FAB press-and-hold.
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
| fab | primaryContainer + shadow `0 4 12 #28145A38` (light theme only; none in dark) | FAB |

Shadows are `x y blur colour`. The fab shadow is taken from board 18 as drawn (decided 2026-11-02); it may be a design-tool default and is flagged to the user. In Kotlin, `RepzElevation.level0 … level4` and `RepzElevation.fab` each give a `RepzElevationLevel(containerColor, shadow: Shadow?)`; components draw the shadow with `Modifier.dropShadow(shape, shadow)`.

Shadows are only for things floating under the finger (FAB, dragged item, rest pill over content). Sheets and dialogs use a scrim of `#000` at 32 %.

## 8. Iconography

- **Material Symbols Rounded**, opsz 24, wght 400, grade 0 (dark theme grade −25).
- FILL 0 by default. FILL 1 for: selected nav item, active toggle, completed state, PR trophy.
- Sizes: 20 dp in dense rows, 24 dp default, 28 dp in the nav bar.
- Custom equipment icons (24 dp, stroke): Barbell, Dumbbell, Kettlebell, Machine, Cable, Bodyweight, Band, Other. Path data is on board 03.
- Set types: W / 1·2·3 / D / F letter badges. PR: `trophy`, `workspace_premium`. Sync: `cloud_done`, `cloud_sync`, `cloud_upload`, `cloud_off`, `sync_problem`.

## 9. Components (`core:designsystem`, prefix `Repz`)

| Component | Key spec |
|---|---|
| `RepzButton` (filled/tonal/outlined/text, XS 32 · S 40 · M 56 · L 96 · XL 136), button group, split button | full → medium on press; loading = contained loading indicator, size stays |
| `HeroStartButton` | height 88, icon disc 56, label titleLargeEmphasized; split trailing segment 64 wide (main = empty workout, segment = from template); pill → radius 22 pressed |
| FAB / FAB menu | Square → Cookie4 on hold; menu items stagger 30 ms bottom-up, + rotates to × |
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
| `RepzLoader` | logo-based loader, indeterminate + determinate, 24 / 48 / 96 dp (MOT-25) |
| Banners, snackbar (undo 6 s), dialogs, sheets, M3 Expressive loading indicator, skeleton shimmer (only after a 150 ms delay) | board 06 |
| Form fields | filled/outlined, password toggle + strength meter, errors on blur (not per keystroke), segmented buttons, switch, slider, settings list items |
| Empty states | illustration + title + body + action (MOT-23) |

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
| Weekly goal | default 4, setting not designed | stored in user preferences, default 4; setting UI **open question** | ask before building the goal UI |
