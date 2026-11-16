---
name: animation
description: Implement Repz animations, shape morphs, particles, logo motion and haptics exactly as specified in docs/MOTION.md (MOT-01…MOT-30) using RepzMotion tokens. Use for any animation, transition, shared element, celebration, loader or haptic feedback.
---

# Animation & haptics

## Rule zero: never guess

If anything is unclear (naming, behaviour, UX copy, a token value, which module it belongs to, an API shape) or the docs disagree with the code, **stop and ask the user** before writing code. Do not invent values, screens, endpoints or animations that are not specified. Record approved assumptions in the docs (or an ADR) in the same change.

**Every animation must have an ID in `docs/MOTION.md`.** If the one you need isn't there, propose a spec (trigger, properties, tokens, choreography, haptic, reduced-motion fallback) and wait for approval before coding it.

## Steps
1. Find the MOT-xx entry. Use exactly its tokens as specs: `RepzMotion.springs.*()` (spatial, celebrate, pressMorph, logo), `RepzMotion.effectsSprings.*()` (colour/alpha), `RepzMotion.tweens.*()` (short, medium, pathDraw, chartDraw), plus `RepzMotion.durations/easing/stagger`. Example: `animateDpAsState(target, animationSpec = RepzMotion.springs.spatialDefault())`. Never write `spring(0.7f, 500f)` or `tween(…)` inline.
2. Pick the API from MOTION.md §5:
   - springs: `Animatable` / `animate*AsState`;
   - shape morphs: `RepzMorphs` pairs + `RepzMorphShape(morph, progress)`, progress driven by a `RepzMotion` spring;
   - shared elements: `SharedTransitionLayout`;
   - particles: one `Canvas` + `withFrameNanos`;
   - logo: `RepzLogoSymbol` parts (geometry from `brand/LOGO.md`, never redrawn);
   - number roll: per-digit `AnimatedContent`.
3. **Reduced motion:** the `RepzMotion` specs already snap or crossfade (MOTION §2, ADR-0024). Never read `LocalReducedMotion` in a feature (it is `internal` to `core:designsystem`). A structural fallback (particles off, no idle loop) belongs in a `core:designsystem` component. Check the catalog row's fallback with `RepzTheme(reducedMotion = true)`.
4. **Haptics:** `val haptics = RepzHaptics.current`, then `tick/confirm/restEnd/success/reject/dragStart/dragEnd` only. They respect the user setting and ignore reduced motion.
5. **Interruptibility:** springs retarget from current velocity. A touch during a celebration snaps it to the end state.
6. **Performance budget:**
   - particles ≤ 24 inline / ≤ 120 on the summary;
   - no allocation per frame;
   - nothing blocks input on the live-workout screen;
   - set completion done at 220 ms.
7. Add a preview (normal and `reducedMotion = true`) and a demo page in the design-system catalog (`RepzCatalogPage`, key listed in ARCHITECTURE §2). The page auto-plays and fits one iPhone Duo screen without scrolling. Add a unit test for any state machine (e.g. rest-timer phases computed from `restEndsAt` + `Clock`).
8. **Every animation must appear in Spotter's review video**: one Android dark-mode recording per QA round that walks through each animation's catalog page (ARCHITECTURE §2 has the launch commands; `--ez repz.reducedMotion true` forces the reduced-motion fallback). No per-animation clip sets, no light or iOS media unless the behaviour is iOS-only.

## Don'ts
- No celebration springs or particles outside PR, workout finished, streak milestone and rest-done.
- Don't animate typing, normal timer ticks or scroll position.
- Don't modify the logo geometry or colours. Only animate its parts.
