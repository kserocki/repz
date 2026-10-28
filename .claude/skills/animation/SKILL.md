---
name: animation
description: Implement Repz animations, shape morphs, particles, logo motion and haptics exactly as specified in docs/MOTION.md (MOT-01…MOT-29) using RepzMotion tokens. Use for any animation, transition, shared element, celebration, loader or haptic feedback.
---

# Animation & haptics

## Rule zero: never guess

If anything is unclear (naming, behaviour, UX copy, a token value, which module it belongs to, an API shape) or the docs disagree with the code, **stop and ask the user** before writing code. Do not invent values, screens, endpoints or animations that are not specified. Record approved assumptions in the docs (or an ADR) in the same change.

**Every animation must have an ID in `docs/MOTION.md`.** If the one you need isn't there, propose a spec (trigger, properties, tokens, choreography, haptic, reduced-motion fallback) and wait for approval before coding it.

## Steps
1. Find the MOT-xx entry. Use exactly its tokens (`RepzMotion.springs.*`, `durations.*`, `stagger.*`). Never write `spring(0.7f, 500f)` inline.
2. Pick the API from MOTION.md §5:
   - springs: `Animatable` / `animate*AsState`;
   - shape morphs: `Morph` + `MaterialShapes`;
   - shared elements: `SharedTransitionLayout`;
   - particles: one `Canvas` + `withFrameNanos`;
   - logo: `RepzLogoSymbol` parts (geometry from `brand/LOGO.md`, never redrawn);
   - number roll: per-digit `AnimatedContent`.
3. **Reduced motion:** read `LocalReducedMotion` through the RepzMotion helpers and implement the fallback from the catalog row.
4. **Haptics:** `RepzHaptics.tick/confirm/restEnd/success/reject/drag` only. They respect the user setting.
5. **Interruptibility:** springs retarget from current velocity. A touch during a celebration snaps it to the end state.
6. **Performance budget:**
   - particles ≤ 24 inline / ≤ 120 on the summary;
   - no allocation per frame;
   - nothing blocks input on the live-workout screen;
   - set completion done at 220 ms.
7. Add a preview or showcase entry (in the designsystem showcase screen) to scrub the animation, and a unit test for any state machine (e.g. rest-timer phases computed from `restEndsAt` + `Clock`).

## Don'ts
- No celebration springs or particles outside PR, workout finished, streak milestone and rest-done.
- Don't animate typing, normal timer ticks or scroll position.
- Don't modify the logo geometry or colours. Only animate its parts.
