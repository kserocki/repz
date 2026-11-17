---
name: design-system
description: Build Repz UI with the design system: RepzTheme tokens (colours, type, shapes, spacing, elevation), Repz* components, light/dark, dynamic colour, i18n and accessibility. Use when writing any Composable, adding a component to core:designsystem, or translating a design board into code.
---

# Design system

## Rule zero: never guess

If anything is unclear (naming, behaviour, UX copy, a token value, which module it belongs to, an API shape) or the docs disagree with the code, **stop and ask the user** before writing code. Do not invent values, screens, endpoints or animations that are not specified. Record approved assumptions in the docs (or an ADR) in the same change.

Sources, in this order: `docs/DESIGN_SYSTEM.md` (committed truth) → `design/boards/NN-*.txt` (local reference, git-ignored) → ask. If a value is only in the board and not in the doc, add it to the doc in the same change after the user confirms.

## Using tokens
- M3 roles: `MaterialTheme.colorScheme.*`. Brand and semantic colours: `RepzTheme.colors.*` (`record`, `success`, `setWarmup`, `musclePrimary`, `chartSeries1`…). These never follow dynamic colour.
- Text on cyan (`primaryContainer` / `brandCyan`) is always `onPrimaryContainer` (ink). Never white.
- Every number uses `RepzTheme.typography.numeric*` (tabular figures). Format numbers per locale.
- Spacing `RepzSpacing.*`, shapes `MaterialTheme.shapes.*` / `RepzShapes.*`, elevation `RepzElevation.*`. No literal dp for spacing or radii in features.
- Shadows: `Modifier.dropShadow(shape, RepzElevation.<level>.shadow)`. Corner radii always come from shapes, never from spacing tokens.
- A new theme `CompositionLocal` must be added to detekt's `CompositionLocalAllowlist` (`mobile/config/detekt/detekt.yml`, ARCHITECTURE §12.1).
- Preview the result in the debug catalog (ARCHITECTURE §2); a new catalog page must fit one screen on the iPhone Duo simulator.
- Icons: Material Symbols Rounded via the designsystem icon set. FILL 1 only for selected, active or completed states.

## Adding a component to `core:designsystem`
1. Check DESIGN_SYSTEM.md §9 for its spec (anatomy, sizes, states). If it's missing, ask.
2. API: `@Composable fun RepzX(state/params…, onClick…, modifier: Modifier = Modifier)`. Stateless, with the modifier as the first optional parameter and slots for content where the spec has them.
3. All states from the spec, plus a `@Preview` per state in light and dark, plus one at fontScale 2.0 and one with PL strings.
4. Motion only through `RepzMotion` and the MOT-xx IDs (see the `animation` skill). Press feedback = MOT-22.
5. Accessibility: 48 dp minimum (56 for logging), `contentDescription` for icon-only controls, `semantics` custom actions where the spec lists them, and colour never as the only signal.
6. Screenshot tests (Roborazzi) come later. Keep previews deterministic (no `Clock.System`).

## Strings (multi-language ready)
- All user-facing text in Compose Resources (EN default + `values-pl`), including content descriptions. No literals in Kotlin.
- Placeholders instead of concatenation, `plurals` for counts, one key per full sentence.
- A new language = a new `values-<lang>` folder **only**. Platform language lists are generated from those folders (Android `generateLocaleConfig`, iOS `CFBundleLocalizations` via a Gradle task). No language checks in code.
- Locale-aware formatting for numbers, units, dates and durations.

## Checklist before done
- [ ] light + dark + dynamic colour (Android) look right
- [ ] no hard-coded strings; plurals/placeholders used; PL strings don't truncate (buttons wrap; headlines may compress to wdth 88)
- [ ] 200 % font scale doesn't break the layout
- [ ] reduced motion path works

## Known Compose pitfalls (found in REP-22/34/35 reviews)
- **Focus:** `onFocusChanged` fires once on first layout with `isFocused = false`. End editing only after the field was focused and then lost focus. Clear focus and hide the keyboard (`focusManager.clearFocus()`, `keyboardController?.hide()`) before minimize, finish or opening another sheet. On iOS an open keyboard pushes the whole screen up.
- **Overlays:** a full-screen tap-to-dismiss layer swallows every tap behind it. For a non-modal sheet, detect outside taps without consuming them, and consume blank taps inside the sheet itself.
- **Stale closures:** a handler remembered once captures old values. Read changing inputs through `rememberUpdatedState`, and keep one state object identity instead of recreating it under a new `remember` key.
- **Saved state:** anything that must survive rotation or process death uses `rememberSaveable` (with a `Saver` for custom types). A plain `remember` resets.
- **Sizing:** `Modifier.heightIn(min)` on a `Box` doesn't reach its children; use `propagateMinConstraints = true` or `matchParentSize()`. Segmented controls share one height via `Row(Modifier.height(IntrinsicSize.Min))` + `fillMaxHeight()`; two stacked rows next to a tall item each need `weight(1f)`. Pill radii come from the measured height, not the minimum.
- **Text:** `TextAutoSize` shrinks only on overflow and can't prevent a mid-word break; numbers use `maxLines = 1` + auto-size with a floor, and a font-scale cap where the design allows. Use a short-label fallback for long words.
- **Lists:** key every item (`key(id)`), bring the edited row into view accounting for overlays (e.g. a keypad sheet), and use one scroll mechanism at a time.
- **Thresholds:** state whether a breakpoint refers to the window (`LocalWindowInfo`) or the component's own width, and let related components (header + rows) take the decision from one place.
