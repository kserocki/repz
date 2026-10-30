# Repz — Logo

The Repz logo is a **dumbbell symbol** (a bar plus three pill-shaped plates per side) and a rounded monoline **"repz" wordmark**.
Every plate is a separate shape, so the symbol can be *assembled* in motion: the bar appears, then the plates slide on from both sides and bounce into place.

All files are generated from `logo/src/build.py` (geometry) and `logo/src/variants.py` (colours, icons, storyboard). **Never edit exported SVGs by hand.** Change the generator and re-run it.

---

## 1. Files

| Folder | Content |
|---|---|
| `logo/master/` | Outlined black masters: `symbol.svg`, `wordmark.svg`, `lockup-stacked.svg`, `lockup-horizontal.svg` |
| `logo/color/` | Colourways `symbol-*`, `lockup-stacked-*`, `lockup-horizontal-*` in `cyan`, `deep`, `ink`, `white`, `black`; tiles `lockup-stacked-on-{ink,cyan,paper}` |
| `logo/app-icon/` | iOS `ios-{light,dark,tinted}-1024` (SVG + PNG); Android adaptive `android-{foreground,background,monochrome}-108.svg` |
| `logo/motion/` | `splash-storyboard.svg/png` (6 key frames) |
| `logo/src/` | Generators + stroke-based sources (outlined to `master/` with picosvg); `android_vectors.py` converts `app-icon/android-*-108.svg` into Android VectorDrawables (stdlib only) |
| `mobile/app/android/src/main/res/drawable/` | Android adaptive-icon + splash VectorDrawables, generated from `logo/app-icon/android-*-108.svg` by `logo/src/android_vectors.py`. Never edit by hand |
| `mobile/iosApp/iosApp/Assets.xcassets/` | iOS `AppIcon` + `LaunchSymbol` image sets, copied as-is from `logo/app-icon/ios-*-1024.png` and `logo/color/symbol-{deep,cyan}.svg` by `mobile/iosApp/scripts/sync-brand-assets.sh`. Never edit by hand |

## 2. Geometry (symbol grid 320 × 160, centre x = 160, y = 80)

All parts are pills (`rx = width / 2`); right side mirrors left around x = 160.

| Part (left side) | x | y | width | height |
|---|---|---|---|---|
| bar (shared) | 100 | 70 | 120 | 20 |
| inner plate | 72 | 16 | 28 | 128 |
| middle plate | 40 | 32 | 24 | 96 |
| outer collar | 12 | 56 | 20 | 48 |

Gaps between plates: 8. Plate heights 1 : 0.75 : 0.375.

**Wordmark:** monoline stroke 22, round caps and joins, bowl radius 31, x-height 84, letter gap 10, every diagonal exactly 45°. Lockups:
- **Stacked** (320 × 260): wordmark at 0.7 scale, centred, 24 units under the plates.
- **Horizontal** (470 × 116): symbol at 0.6 scale, wordmark at 0.75 scale, with the x-height centred on the bar.

## 3. Colour

| Token | Hex | Use |
|---|---|---|
| `brandCyan` | `#00C2E0` | primary brand field (icon background, mark on dark) |
| `brandDeep` | `#00687A` | mark on light surfaces (≈ 6:1 on white) |
| `brandInk` | `#0E1416` | dark background, mark on cyan |
| `brandPaper` | `#F5FAFB` | light background |
| `brandMist` | `#A6EEFF` | secondary light tint on dark (labels, glows) |

Approved pairings: cyan on ink · ink on cyan · deep on paper · white or black one-colour. The mark is always **one colour**: no gradients, no outlines, no shadows, no multi-coloured plates. The only exception is the PR moment in §5, where the plates turn record gold.

## 4. Usage

- **Clear space:** the height of the outer collar (48 units) on every side.
- **Minimum size:** symbol 24 dp wide; horizontal lockup 96 dp wide; stacked lockup 72 dp wide.
- **Don't:** stretch, rotate, re-space plates, change plate count, recolour single parts, put it on busy imagery, set "repz" in a font.
- **App icon:** symbol at 60 % of the icon width, centred. Android adaptive: cyan background layer, ink foreground layer, monochrome layer for themed icons. iOS: light = ink on cyan, dark = cyan on ink, tinted = grey on black.

## 5. Motion: "Load the bar" (MOT-01, logo assembly)

| t (ms) | What | Spec |
|---|---|---|
| 0 | **bar** grows from its centre | `scaleX 0 → 1`, spring dampingRatio 0.8, stiffness 600 |
| 120 | **inner plates** slide in from the left and right screen edges simultaneously | `translateX ±offscreen → 0`, spring dampingRatio **0.45**, stiffness 500 (visible overshoot and bounce) |
| 230 | **middle plates** | same spring |
| 340 | **outer collars** | same spring |
| each landing | "impact": whole symbol `scaleY 1 → 0.97 → 1` (spring 0.5 / 800) + light haptic tick (not in the OS splash phase) | |
| 600 | **wordmark** rises | `alpha 0 → 1`, `translateY 12 dp → 0`, effects spring (critically damped, stiffness 380) |
| ≈ 1000 | done | total ≤ 1 s, interruptible (tap skips to final frame) |

**Reduced motion:** static logo, 150 ms fade.

**Splash hand-off:** the OS splash (Android SplashScreen API / iOS LaunchScreen) shows the *complete* symbol, 144 dp/pt wide, following system appearance: deep on paper by day, cyan on ink by night. The first Compose frame shows the same symbol in the same place. On a cold start into Welcome, the plates fly **out** and the bar is re-loaded (MOT-01). Returning users get a straight crossfade into Train home.

### Reuse inside the app (same parts, same springs)

| Where | Behaviour |
|---|---|
| **Branded loader** (restoring history, first sync, pull-to-refresh) | Plates load one pair at a time in a loop. On "Restoring history" each synced page locks one plate pair, a determinate version of the same animation |
| **"Start workout" press** | Icon-sized dumbbell loads its plates (≈ 400 ms) while the container transform runs |
| **PR celebration** | Plates slam in **record gold** (`#FCBC00` family) with stronger bounce, then particles burst from the plate edges |
| **Workout finished** | The loaded dumbbell does a short "lift": translateY −8 dp then settle, with a success haptic |
| **Empty states** | Idle: one outer collar wobbles every few seconds (max 3 loops, then rest) |
| **Sync indicator** | Syncing = plates pulse outward one by one; synced = all plates settle |

Implementation note: every part is a separate path (`bar`, `plate-{inner,middle,outer}-{left,right}` ids in `master/symbol.svg`), so Compose draws them as 7 shapes with independent `Animatable`s.
