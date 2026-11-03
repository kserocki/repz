# ADR-0024: Motion tokens, haptics and reduced motion

- **Status:** Accepted (2026-11-03, REP-20)

## Context
MOTION.md defines every spring, duration, stagger and haptic of the app, plus a reduced-motion fallback for each MOT-xx. Features must not hard-code springs (AGENTS.md §3 rule 7), must never check reduced motion themselves (MOTION §2), and M3 components should move with the same springs as Repz code. Haptics need one entry point that honours the in-app "Haptics" setting (F23) on both platforms.

## Decision
- **`RepzMotion`** (`core:designsystem`, `motion/`) is the only way to get an animation spec:
  - `RepzMotion.springs: RepzSpatialSprings`: `spatialFast/Default/Slow`, `celebrate`, `pressMorph`, `logoBar/Plate/PlatePr/Impact/Wordmark`;
  - `RepzMotion.effectsSprings: RepzEffectsSprings`: `effectsFast/Default/Slow` (colour and alpha only);
  - `RepzMotion.tweens: RepzTweens`: `short` (100 ms), `medium` (200 ms), `pathDraw` (280 ms) and `chartDraw` (600 ms), the last two with `emphasizedDecelerate`;
  - each factory is `fun <T> x(): FiniteAnimationSpec<T>`, so `animateDpAsState(target, animationSpec = RepzMotion.springs.spatialDefault())` infers `T`. The three getters are `@Composable @ReadOnlyComposable` and return one of two cached instances (normal, reduced);
  - plain values: `RepzMotion.durations` (`RepzDurations`, `kotlin.time.Duration`, incl. `restRingStep` 1 s and `reducedMotionFade` 150 ms), `RepzMotion.easing` (`emphasized`, `emphasizedDecelerate`, `linear`) and `RepzMotion.stagger` (`RepzStagger`; `LIST_MAX_ITEMS = 8` is the one `const val`).
  - The springs are split into two classes so each stays under Detekt's default function limit; token names and ζ/k values are unchanged from MOTION §2.
- **M3 `MotionScheme`.** `RepzTheme` passes `repzMotionScheme(reducedMotion)` to `MaterialExpressiveTheme`. Its six slots map 1:1 to the M3-named tokens (`fast/default/slowSpatialSpec` → `spatialFast/Default/Slow`, `fast/default/slowEffectsSpec` → `effectsFast/Default/Slow`), so M3 components also honour reduced motion.
- **Reduced motion** is the `internal LocalReducedMotion` in `core:designsystem`, so the compiler stops features from reading it. Features only get reduced-aware specs:

  | Spec | Normal | Reduced |
  |---|---|---|
  | spatial springs (incl. `celebrate`, `pressMorph`, `logo*`) | `spring(ζ, k)` | `snap()`; morphs driven by them jump to the end shape |
  | effects springs | `spring(ζ, k)` | `tween(150)`, a crossfade |
  | `tweens.short` | `tween(100)` | unchanged |
  | `tweens.medium` | `tween(200)` | `tween(150)`, a fade-through |
  | `tweens.pathDraw` / `chartDraw` | `tween(280/600, emphasizedDecelerate)` | `snap()` |
  | durations, easing, stagger | plain values | unchanged |

  - The platform value is read live: Android watches `Settings.Global.ANIMATOR_DURATION_SCALE` with a `ContentObserver` (only exactly 0 counts as reduced); iOS reads `UIAccessibilityIsReduceMotionEnabled()` and observes `UIAccessibilityReduceMotionStatusDidChangeNotification`.
  - With Android "Remove animations" (scale 0), Compose also runs the 150 ms crossfades instantly, so everything is instant. Accepted: stricter, never more motion.
- **`RepzHaptics`** is a theme-provided service: `val haptics = RepzHaptics.current`, then `haptics.confirm()`.
  - 7 events: `tick`, `confirm`, `restEnd`, `success`, `reject`, `dragStart`, `dragEnd`.
  - `RepzTheme(hapticsEnabled = false)` drops every event before it reaches the platform.
  - It never reads reduced motion; haptics have their own setting (MOTION §3).
  - Android uses `ViewCompat.performHapticFeedback` with `HapticFeedbackConstantsCompat` (androidx.core maps the API 30+ constants on API 29). `RestEnd` is two `LONG_PRESS` pulses 120 ms apart. `Success` is `CONFIRM` plus a `VibrationEffect.EFFECT_HEAVY_CLICK` 120 ms later through `Vibrator`, which needs the `VIBRATE` permission (normal, install-time) in `app:android`'s manifest.
  - iOS uses `UISelectionFeedbackGenerator`, `UIImpactFeedbackGenerator` (light, medium) and `UINotificationFeedbackGenerator` (warning, success, error), as listed in MOTION §3.
- **`RepzMorphs`** holds the DESIGN_SYSTEM §5 morph pairs as `androidx.graphics.shapes.Morph`: `circleToBurst`, `sunnyToCircle`, `circleToGem`, `circleToFlower`, `flowerToCookie9Sided` (built once, lazily), and `circleToRoundedSquare(cornerRadius, size)` / `roundedSquareToCookie4Sided(cornerRadius, size)`, which callers `remember` per size. `RepzMorphShape(morph, progress)` turns a morph into a `Shape`; callers drive `progress` with a `RepzMotion` spring, so reduced motion makes it jump to the end. The set checkbox uses a 10 dp radius (MOT-05 aligned with DESIGN_SYSTEM §5). The FAB morph keeps 16 dp until REP-22 defines its rest shape. The button press is a corner animation, not a `Morph`.
- **`RepzTheme(darkTheme, dynamicColor, reducedMotion: Boolean? = null, hapticsEnabled: Boolean = true, content)`.** `null` follows the platform; previews and tests force `true`. `hapticsEnabled` will be wired to the F23 setting later.
- **Debug hooks.** `RepzRoot(…, catalogReducedMotion: Boolean? = null)` forwards to `RepzTheme(reducedMotion)`. Android: `--ez repz.reducedMotion true`, read only in debuggable builds; an absent extra follows the system. iOS: launch argument `-RepzReducedMotion YES`, read under `#if DEBUG`; absent follows the system.
- **Catalog pages** `motion-springs-1`, `motion-springs-2`, `motion-tweens`, `motion-morphs` and `haptics` auto-play their demos and each fit one iPhone Duo screen (ARCHITECTURE §2).
- **Detekt.** The only config change is `complexity.CyclomaticComplexMethod.ignoreSimpleWhenEntries: true` (user-approved): the catalog's page switch is one single-call branch per page and grows with every page. No other thresholds or exclusions were added.

## Consequences
- A feature cannot use a spring that ignores reduced motion or diverges from MOTION §2, and M3 components move with the same tokens.
- Structural fallbacks (particles off, no idle loops) are not covered by specs; they will live in `core:designsystem` components, which can read `LocalReducedMotion`.
- The haptic feel has not been checked on real devices yet; that check (Android phone and iPhone) is REP-78.
