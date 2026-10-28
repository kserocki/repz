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
- Icons: Material Symbols Rounded via the designsystem icon set. FILL 1 only for selected, active or completed states.

## Adding a component to `core:designsystem`
1. Check DESIGN_SYSTEM.md §9 for its spec (anatomy, sizes, states). If it's missing, ask.
2. API: `@Composable fun RepzX(state/params…, onClick…, modifier: Modifier = Modifier)`. Stateless, with the modifier as the first optional parameter and slots for content where the spec has them.
3. All states from the spec, plus a `@Preview` per state in light and dark, plus one at fontScale 2.0 and one with PL strings.
4. Motion only through `RepzMotion` and the MOT-xx IDs (see the `animation` skill). Press feedback = MOT-22.
5. Accessibility: 48 dp minimum (56 for logging), `contentDescription` for icon-only controls, `semantics` custom actions where the spec lists them, and colour never as the only signal.
6. Screenshot tests (Roborazzi) come later. Keep previews deterministic (no `Clock.System`).

## Checklist before done
- [ ] light + dark + dynamic colour (Android) look right
- [ ] PL strings don't truncate (buttons wrap; headlines may compress to wdth 88)
- [ ] 200 % font scale doesn't break the layout
- [ ] reduced motion path works
