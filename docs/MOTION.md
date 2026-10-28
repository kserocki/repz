# Repz — Motion

> Source: design v0.2, boards 03 (tokens), 15 (catalog), 17 (logo motion). Logo motion is also specified in [`../brand/LOGO.md`](../brand/LOGO.md) §5.
> If an animation you need is not in this file, **ask** before inventing one. Every new animation must get an ID, tokens, a haptic and a reduced-motion fallback here first.

## 1. Rules

- **Springs first.** Tweens are only for things a spring can't express: path drawing, the timer ring and shimmer.
- **Never animate:** typing or keypad digits in a field, list scroll position, timer digits during normal ticking (MOT-09 only when a value *jumps*), text in inputs, or the appearance of error text (instant, then a 100 ms fade).
- **Celebrations:**
  - total ≤ 1.5 s; inline PR ≤ 600 ms;
  - only PR, workout finished, streak milestone and rest-done may use the `celebrate` spring or particles.
- **Interruptible:** springs retarget from their current velocity. Any touch during a celebration snaps it to the end state and handles the touch.
- **Particles:**
  - ≤ 24 inline, ≤ 120 on the summary;
  - one Canvas layer, no per-particle composables;
  - stop emitting if frame time > 20 ms twice.
- **Scrolling:** only stagger-in for the first load (max 8 items). Heatmap, ring and chart never re-animate on scroll back.
- **Logging speed:** nothing on the live-workout screen blocks input. Set completion is visually done at 220 ms.
- **Predictive back** (Android): preview scales to 0.9, corner radius 0 → 28 dp, follows the finger, commit = `spatialDefault`. The iOS edge-swipe drives the same state.

## 2. Tokens: `RepzMotion`

### Springs (`RepzMotion.springs.*`, use `spring(dampingRatio, stiffness)`)

| Token | dampingRatio | stiffness | Use |
|---|---|---|---|
| spatialFast | 0.9 | 1400 | small movements, indicators, snaps |
| spatialDefault | 0.9 | 700 | sheets, containers, list reflow |
| spatialSlow | 0.9 | 300 | rings, long travel, auto-scroll |
| effectsFast | 1.0 | 3800 | colour/alpha quick |
| effectsDefault | 1.0 | 1600 | colour/alpha |
| effectsSlow | 1.0 | 800 | glows, tints |
| celebrate | 0.55 | 380 | PR, Go!, badges (celebrations only) |
| pressMorph | 0.6 | 1200 | press shape morphs |
| logoBar | 0.8 | 600 | logo bar grow |
| logoPlate | 0.45 | 500 | logo plates slide-in (visible overshoot) |
| logoPlatePr | 0.4 | 500 | gold plates on PR |
| logoImpact | 0.5 | 800 | landing squash |
| logoWordmark | 1.0 | 380 | wordmark rise |

### Durations & easing (`RepzMotion.durations.*`)

| Token | Value |
|---|---|
| short | 100 ms (fade, tick) |
| medium | 200 ms (crossfade, fade-through) |
| pathDraw | 280 ms · EmphasizedDecelerate (check, sync ✓) |
| chartDraw | 600 ms · EmphasizedDecelerate |
| restRing | linear, updates once per second of rest |
| particleLifeInline / Summary | 600 ms / 1200 ms |
| celebrationMax | 1500 ms |
| shimmer | 1200 ms linear |
| easing.emphasized | cubic(0.2, 0, 0, 1) |
| easing.emphasizedDecelerate | cubic(0.05, 0.7, 0.1, 1) |

### Stagger (`RepzMotion.stagger.*`)

logoPlates 110 ms/pair · list 30 ms/item (max 8) · summary 70 ms/block · heatmap 12 ms/diagonal · digits 20 ms/digit, right → left.

### Reduced motion (system animator scale 0 / iOS Reduce Motion)

- Spatial springs snap instantly. Effects become a 150 ms crossfade.
- Shape morphs jump to the end shape. The ring sweep still updates every second, because it carries information.
- Particles, confetti and idle loops are off. PR still shows the gold container and a static badge.
- Number roll becomes a plain swap. Container transforms become a 150 ms fade-through.
- Haptics stay on (they have their own setting).

Implementation: a single `LocalReducedMotion` (expect/actual reading the platform setting) consumed by `RepzMotion` helpers. Features never check it themselves.

## 3. Haptics: `RepzHaptics`

| Event | Android | iOS | Used for |
|---|---|---|---|
| Tick | CLOCK_TICK | UISelectionFeedback | keypad key, stepper, chart snap, slider detent, last 5 s of rest |
| Confirm | CONFIRM | UIImpact .light | set completed, chip toggle, nav |
| RestEnd | LONG_PRESS × 2 | UINotification .warning | rest timer 0 |
| Success | CONFIRM + heavy click 120 ms later | UINotification .success | PR, workout finished |
| Reject | REJECT | UINotification .error | invalid value, delete |
| Drag | GESTURE_START / END | UIImpact .medium | reorder lift/drop |

Haptics respect the in-app "Haptics" setting (F23).

## 4. Catalog

★ = has a full storyboard in the design (boards 15/17).

### MOT-01 · Logo · "Load the bar" ★
- **Trigger:** Cold start into Welcome (after the OS splash shows the full symbol)
- **Animates:** 7 separate parts (LOGO.md): plates fly out, bar scaleX 0→1 (logoBar ζ0.8/k600); inner plates slide in from both screen edges at 120 ms, middle 230 ms, collars 340 ms (logoPlate ζ0.45/k500, visible overshoot); each landing = symbol scaleY 1→0.97→1 (logoImpact ζ0.5/k800); wordmark alpha + y 12 dp→0 at 600 ms (effects ζ1.0/k380)
- **Choreography:** Bar → inner → middle → collars → wordmark; done ≈ 1000 ms; tap skips to final frame. Returning users: straight crossfade to Train home
- **Haptic:** Light tick per plate landing (not during OS splash)
- **Reduced motion:** Static logo, 150 ms fade

### MOT-02 · Nav bar selection
- **Trigger:** Tab tap
- **Animates:** Indicator leading edge spatialFast, trailing edge spatialDefault (stretch), icon FILL 0→1 + weight 400→500 (effectsFast)
- **Choreography:** Indicator first; label weight with it; content fade-through 200 ms
- **Haptic:** Confirm (light)
- **Reduced motion:** Indicator jumps, icon swap

### MOT-03 · Start workout ★
- **Trigger:** Hero button release
- **Animates:** Press: Pill → RoundedRect(22) + scale 0.96 (pressMorph); the play icon swaps for an icon-sized dumbbell that loads its plates (logoPlate spring, ≈ 400 ms) while the button container-transforms into the live screen (spatialDefault)
- **Choreography:** Squish 0–90 ms → plates load + container grows 90–450 ms → header timer + first card fade 300–500 ms
- **Haptic:** Confirm
- **Reduced motion:** Fade-through 150 ms

### MOT-04 · Mini bar ↔ live screen
- **Trigger:** Tap mini bar / minimize chevron / swipe down
- **Animates:** Shared bounds of the bar container; elapsed timer is a shared element (position + size, numericSmall ↔ hero)
- **Choreography:** Timer moves first, container follows 30 ms later, cards stagger 30 ms
- **Haptic:** none
- **Reduced motion:** Crossfade 150 ms

### MOT-05 · Set completion ★
- **Trigger:** Tap checkbox
- **Animates:** Circle → RoundedSquare(11) (pressMorph), check path draws 280 ms, successContainer floods from checkbox outward (radial clip, spatialFast), numbers scale 1→1.06→1 (celebrate, tiny)
- **Choreography:** Morph 0–120 → check 60–280 → flood 80–300 → numbers settle 150–320; rest timer starts at 0
- **Haptic:** Confirm (light)
- **Reduced motion:** Instant fill + check, no scale

### MOT-06 · Rest timer ★
- **Trigger:** Set completed / last 5 s / 0
- **Animates:** Ring sweep linear per second; Sunny backdrop breathes ±3 % (4 s sine); last 5 s: colour → tertiaryContainer + pulse 1.0→1.05 per second; at 0: Circle → Burst (celebrate) + "Go!" scale 0.6→1; pill ↔ sheet = container transform
- **Choreography:** Pulse on each second boundary; burst 0–400 ms; auto-collapse at 1.5 s
- **Haptic:** Tick each of last 5 s; double heavy at 0
- **Reduced motion:** No breathe / pulse; colour change + "Go!" text only

### MOT-07 · Inline PR moment ★
- **Trigger:** Completed set beats a PR
- **Animates:** Row → recordContainer (effectsDefault), record glow 0→1→0.6 (effectsSlow), trophy badge scale 0→1.15→1 (celebrate), ≤ 24 Canvas particles, gravity 900 dp/s², life 600 ms
- **Choreography:** Runs after MOT-05 settles (+80 ms); particles from the checkbox
- **Haptic:** Success
- **Reduced motion:** Gold container + static badge

### MOT-08 · PR celebration (summary) ★
- **Trigger:** Summary opens with ≥ 1 PR
- **Animates:** Header dumbbell: plates slam in in record gold #FCBC00 with a stronger bounce (ζ0.4/k500), then 120 Canvas particles burst from the plate edges (brandCyan + record gold + white, 1400–2200 dp/s, drag 0.9, gravity 1200); PR cards fly in (y 40 dp, celebrate); values roll (MOT-09)
- **Choreography:** Plates 0–500 ms → burst at last collar landing → cards 70 ms stagger after stat tiles
- **Haptic:** Success on first PR card
- **Reduced motion:** Gold dumbbell static; cards fade in

### MOT-09 · Number roll / ticker
- **Trigger:** Value jumps (not per-second ticks)
- **Animates:** Per-digit vertical slide (spatialFast), new digit from below when value increases, above when it decreases; tabular so width is stable
- **Choreography:** Rightmost digit first, 20 ms stagger leftwards
- **Haptic:** none
- **Reduced motion:** Plain swap

### MOT-10 · Keypad sheet
- **Trigger:** Tap a number field
- **Animates:** Sheet y spring (spatialDefault); key press RoundedRect(16) → (28) + primaryContainer flash (effectsFast); ± long-press repeat 250 ms → 60 ms
- **Choreography:** Sheet then row scrolls into view
- **Haptic:** Tick per key / step
- **Reduced motion:** Sheet appears without slide

### MOT-11 · Add exercise
- **Trigger:** Add (n) in picker
- **Animates:** New card height 0 → auto (spatialDefault), neighbours push (spatialDefault), auto-scroll to card (spatialSlow)
- **Choreography:** Cards appear 40 ms apart in selection order
- **Haptic:** Confirm
- **Reduced motion:** Instant insert + scroll

### MOT-12 · Swipe delete / reorder
- **Trigger:** Horizontal drag on set / long-press drag
- **Animates:** Swipe: errorContainer revealed, delete icon scale 0.6→1.2 with progress, threshold 40 %; Reorder: lift scale 1.03 + containerHighest + shadow (spatialFast), neighbours slide (spatialDefault)
- **Choreography:** Commit: row collapses height (spatialFast) then snackbar
- **Haptic:** Tick at threshold; Drag start/end
- **Reduced motion:** No scale; instant reflow

### MOT-13 · List → details
- **Trigger:** Tap exercise / workout card
- **Animates:** Exercise: illustration + name shared elements (spatialDefault); Workout card: container transform card → screen
- **Choreography:** Shared elements lead, other content fades in at 50 %
- **Haptic:** none
- **Reduced motion:** Fade-through

### MOT-14 · Chart draw-in & scrub
- **Trigger:** Chart visible / range change / drag
- **Animates:** Path trim 0→1 (600 ms EmphasizedDecelerate), area alpha 0→0.14 after 50 %, PR markers scale-in (celebrate, 60 ms stagger); scrub cursor follows finger, snaps to nearest session (spatialFast)
- **Choreography:** Line → area → markers
- **Haptic:** Tick on each snap
- **Reduced motion:** Static chart; scrub still works

### MOT-15 · Heatmap reveal
- **Trigger:** History first visible
- **Animates:** Cell alpha + scale 0.6→1 (spatialFast) on a diagonal wave, 12 ms per diagonal
- **Choreography:** Oldest week to newest
- **Haptic:** none
- **Reduced motion:** Static

### MOT-16 · Stat tiles & weekly ring
- **Trigger:** Train home appears
- **Animates:** Ring sweep spring (spatialSlow), numbers roll (MOT-09)
- **Choreography:** Ring starts with tiles, numbers +100 ms
- **Haptic:** none
- **Reduced motion:** Static values

### MOT-17 · Sync indicator
- **Trigger:** Sync state change
- **Animates:** Syncing: the mini dumbbell’s plates pulse outward one pair at a time (1.2 s loop); Synced: all plates settle (logoPlate spring) + successContainer tint fades out 1.5 s; Offline: icon crossfade to cloud_off + warning tint (effectsSlow)
- **Choreography:** —
- **Haptic:** none (silent)
- **Reduced motion:** Icon swap only

### MOT-18 · Restoring history
- **Trigger:** First sync on new device
- **Animates:** Branded determinate loader: bar loaded first, each synced third locks one plate pair (logoPlate spring); pending pairs shown as dashed outlines; counter rolls (MOT-09)
- **Choreography:** Pairs lock inner → middle → collars; done = “lift” −8 dp + settle, then container transform to Train home
- **Haptic:** Success at done
- **Reduced motion:** Progress bar + text

### MOT-19 · Muscle map highlight
- **Trigger:** Map appears / exercise changes
- **Animates:** Primary regions radial fill (clip circle 0→1, spatialDefault), secondary +120 ms; change = per-region color crossfade (effectsDefault)
- **Choreography:** Primary first, secondary after
- **Haptic:** none
- **Reduced motion:** Instant colors

### MOT-20 · Workout summary reveal ★
- **Trigger:** Finish confirmed
- **Animates:** Header: the dumbbell does a short “lift” (y −8 dp, then settle; spatialDefault) — gold-plate version with MOT-08 when there are PRs → stat tiles count up → muscle map → PRs → actions fade up
- **Choreography:** 70 ms per block; whole sequence ≤ 1.5 s
- **Haptic:** Success
- **Reduced motion:** All static, one 200 ms fade

### MOT-21 · Screen transitions
- **Trigger:** Navigate / back / tab switch
- **Animates:** Push: x 30 % + fade (spatialDefault); Back: reverse; Predictive back: scale 0.9 + radius 0→28 following gesture; Tab: fade-through 200 ms
- **Choreography:** Outgoing exits first 40 %
- **Haptic:** none
- **Reduced motion:** Crossfade 150 ms

### MOT-22 · Press feedback
- **Trigger:** Press on any primary surface
- **Animates:** Shape morph to pressed radius + scale 0.96 (pressMorph), release spring back
- **Choreography:** Immediate on touch down
- **Haptic:** none (except buttons that act)
- **Reduced motion:** Ripple/state layer only

### MOT-23 · Empty states
- **Trigger:** Empty state shown
- **Animates:** One idle loop of the illustration; logo-based empty states let one outer collar wobble (rotate ±4°, logoPlate spring) every few seconds — max 3 loops, then rest
- **Choreography:** —
- **Haptic:** none
- **Reduced motion:** Static illustration

### MOT-24 · Snackbar undo
- **Trigger:** Delete item / tap Undo
- **Animates:** Delete: height collapse (spatialFast) + alpha; Undo: re-expand at original index (spatialDefault) + 1 s primary outline
- **Choreography:** Snackbar enters after collapse
- **Haptic:** Reject on delete
- **Reduced motion:** Instant remove / restore


### MOT-25 · Branded loader `RepzLoader`
Plates load pair by pair in a loop (logoPlate). Determinate variant: each completed step locks one plate pair (see MOT-18). Sizes 24 / 48 / 96 dp. Used on A5 and in pull-to-refresh. Reduced motion: static symbol + progress text.

### MOT-26 · Start-workout plates
Part of MOT-03: the icon-sized dumbbell in the hero button loads its plates (~400 ms) during the container transform.

### MOT-27 · PR plates
Part of MOT-07/08: the dumbbell's plates slam in recoloured to `recordBright` with `logoPlatePr`, then particles burst from the plate edges.

### MOT-28 · Workout-finished lift
Part of MOT-20: the loaded dumbbell lifts −8 dp and settles (spatialDefault), with the Success haptic.

### MOT-29 · Empty-state idle
Part of MOT-23: one outer collar wobbles (rotate ±4°, logoPlate) every few seconds, max 3 loops, then rests.

## 5. Implementation map (Compose)

| Pattern | API |
|---|---|
| Spring values | `Animatable` / `animate*AsState(animationSpec = RepzMotion.springs.x)` |
| Shape morph | `androidx.graphics.shapes` `Morph` + `MaterialShapes`, progress driven by an `Animatable` |
| Shared elements / container transform | `SharedTransitionLayout` + `sharedBounds` / `sharedElement`, wired through Nav3 scenes |
| Particles | one `Canvas` + a frame loop (`withFrameNanos`), plain data arrays, no composable per particle |
| Logo parts | `RepzLogoSymbol(state)` draws 7 shapes with independent `Animatable`s; geometry from `brand/LOGO.md` §2 |
| Number roll | `AnimatedContent` per digit with a slide-from-below/above `ContentTransform` |
| Path draw | `PathMeasure.getSegment` driven by a tween (`pathDraw`) |
