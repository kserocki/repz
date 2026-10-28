# Repz — Claude Design brief

> Paste everything below the line into Claude Design.

---

## 0. Your role & what I need back

You are a senior product designer and motion designer. Design **Repz**, an offline-first workout logger for **Android and iOS**, and the **design system** behind it.

The design will be implemented 1:1 in **Compose Multiplatform with Material 3 Expressive** (one shared UI for both platforms). Everything you specify has to be implementable in Compose: Material 3 color roles, type scale, shapes, `MaterialShapes` morphing, spring-based motion, Canvas drawing. **Do not** rely on heavy background blur, video, or Lottie files. Motion has to be describable as code: springs, tweens, keyframes, path drawing, particles.

Deliverables (all of them, in this order):

1. **Design system foundations:** color, typography, shape, spacing, elevation, iconography, motion tokens. Provide them as visual specimens **and** as a token table (name → value) that I can paste into code (format in §9).
2. **Component library:** every component in §4 with all states (enabled, pressed, focused, disabled, selected, loading, error), in light and dark.
3. **Screens:** every screen in §5, on a **phone frame of 412×917 dp (Android)**, with key screens also on **393×852 pt (iPhone)** to show the iOS adaptations from §7. **Light and dark** for every screen, plus every listed state (empty, loading, error, offline).
4. **Motion spec:** every animation in §6 with trigger, properties, spring/duration values, choreography (what moves first), and a storyboard (3–6 key frames) or an interactive prototype.
5. **Illustration style guide:** the art style for exercise illustrations (§8), with 6 sample exercise illustrations, 12 muscle-group illustrations, and empty-state illustrations.
6. **App icon** (Android adaptive icon foreground/background + monochrome themed icon, iOS icon light/dark/tinted) and a **splash/launch** design.

If anything in this brief is ambiguous or contradictory, **list your assumptions explicitly** at the start of your answer instead of silently picking one.

---

## 1. Product

**Repz** helps people who lift weights log workouts quickly in the gym, see their progress and hit personal records (PRs).

- **Core promise:** logging a set takes about 2 taps and works with no signal (gyms are often basements). Data syncs later in the background.
- **Users:** intermediate gym-goers aged 18–40 who train 3–5×/week, use one hand between sets, are often sweaty, in a hurry, under bright or dim light.
- **Personality:** energetic, confident, playful at the right moments (PRs, finishing a workout), and calm and precise while logging. Think *"a coach who celebrates you but doesn't waste your time"*.
- **Name:** "Repz", short and punchy. The logo and wordmark should work at 24 dp.
- **Languages:** English (default) and **Polish**. Polish strings are about 30% longer (e.g. "Start workout" → "Rozpocznij trening"), so layouts must not break: no fixed-width buttons that truncate.
- **Units:** kg or lb (user setting).

### Ergonomics principles (apply everywhere)

- **Thumb-first:** primary actions in the bottom 40% of the screen, minimum touch target 48 dp, 56 dp+ for set logging actions.
- **Glanceable numbers:** weights, reps and timers are the heroes. Use big **tabular (monospaced) figures**, so numbers don't jump while they change.
- **High contrast** in the live workout screen (readable at arm's length, in sunlight and in a dark gym).
- **Celebrate rarely but loudly:** the big animations are reserved for meaningful moments (PR, workout finished, streak). Everything else is quick and quiet.

---

## 2. Visual direction

**Material 3 Expressive** with a strong, ownable brand, not stock Material:

- Expressive shapes: mix rounded rectangles with expressive `MaterialShapes` (cookie, clover, sunny, pill, gem, soft burst) for **accents only**: rest timer, PR badge, FAB, avatars, muscle-group chips. Containers and cards stay calm (rounded rectangles).
- Bold, **variable-weight typography** with big display numbers.
- A **single high-energy brand accent** plus supporting tonal roles. Propose a seed color and justify it. I lean toward an electric, sporty hue (e.g. vivid lime/volt, hot orange-red, or electric violet). Give me 2 options and pick one.
- Both **light and dark** themes are first-class. The dark theme is the "gym" default look and should feel premium (deep, slightly tinted surfaces, not pure #000).
- **Dynamic color** (Material You, Android 12+) is an optional user setting. Show how 2–3 key screens look with a random dynamic palette, and point out which brand elements stay fixed regardless (e.g. PR gold, set type colors, logo).

Avoid: generic fitness clichés (stock photos of muscular people, flames, aggressive all-caps everywhere), glassmorphism, gradients on body text.

---

## 3. Design system foundations

### 3.1 Color

Provide the **full Material 3 color scheme for light and dark**, using the exact role names:

`primary, onPrimary, primaryContainer, onPrimaryContainer, inversePrimary, secondary, onSecondary, secondaryContainer, onSecondaryContainer, tertiary, onTertiary, tertiaryContainer, onTertiaryContainer, error, onError, errorContainer, onErrorContainer, background, onBackground, surface, onSurface, surfaceVariant, onSurfaceVariant, surfaceTint, inverseSurface, inverseOnSurface, outline, outlineVariant, scrim, surfaceBright, surfaceDim, surfaceContainerLowest, surfaceContainerLow, surfaceContainer, surfaceContainerHigh, surfaceContainerHighest`

Plus **extended (brand) colors**, each with `color / onColor / container / onContainer` for light and dark:

- `success`: set completed, sync OK
- `warning`: unsynced changes, offline
- `record`: PR gold/celebration color, must stay recognizable under dynamic color
- set type colors: `setWarmup`, `setNormal`, `setDrop`, `setFailure`
- muscle highlight: `musclePrimary`, `muscleSecondary`, `muscleInactive` (for illustrations and muscle maps)
- chart series: 3 categorical colors plus grid/axis colors, all with WCAG AA contrast on `surface`

Every text/background pairing must meet **WCAG AA** (4.5:1 body text, 3:1 large text/icons). State contrast ratios for the key pairs.

### 3.2 Typography

- Pick **1–2 Google Fonts (OFL license)** that ship as font files. One display/numeric face with strong **tabular figures** and multiple weights (variable preferred); one text face, or the same family. Both must support **Polish diacritics** (ą ć ę ł ń ó ś ź ż). Justify the choice.
- Provide the full M3 type scale (`displayLarge…labelSmall`, plus the M3 Expressive *emphasized* variants), with size/line height/weight/letter spacing in sp.
- Add custom numeric styles: `numericHero` (live timer, finish-summary totals), `numericLarge` (weight/reps in set rows), `numericMedium`, `numericSmall`. All use tabular figures.

### 3.3 Shape

- M3 shape scale: `extraSmall, small, medium, large, extraLarge` (+ `largeIncreased`, `extraLargeIncreased`, `extraExtraLarge` if you use them), with dp radii.
- Which `MaterialShapes` are used where, and which morph pairs exist (e.g. rest timer `Circle → SoftBurst` when the rest ends, FAB `Square → Cookie` on press, checkbox `Circle → Check-container`).

### 3.4 Spacing, layout, elevation

- 4 dp base grid; spacing tokens `xxs (2) … xxxl`. Name them and give values.
- Screen margins, card padding, list item heights, bottom bar height.
- Elevation: prefer **tonal elevation** (surfaceContainer levels) over shadows. Define where shadows are allowed.
- Window size classes: phone portrait is primary; just note how the History list/details and Exercise list/details become **list-detail on wide screens** (foldables/tablets). No full tablet designs needed.

### 3.5 Iconography

- Material Symbols (Rounded), with the weight/fill/grade you recommend, and when to use filled vs outlined (e.g. selected nav item = filled).
- Custom icons needed: equipment types (barbell, dumbbell, kettlebell, machine, cable, bodyweight, band, other), set types, PR trophy/medal, sync states.

### 3.6 Motion tokens

Based on **M3 Expressive motion** (spring physics). Define:

- **Spatial springs** (position/size/shape): `fast`, `default`, `slow`, each with `dampingRatio` and `stiffness`.
- **Effects springs** (color/alpha): `fast`, `default`, `slow`, critically damped.
- **Expressive/bouncy spring** for celebrations only.
- Duration and easing tokens for the few things that can't be springs (e.g. timer ring sweep, path drawing, staggering interval).
- **Stagger** rules for lists (delay per item, max items staggered).
- **Reduced-motion** fallback: what every animation becomes when the system "remove animations" setting is on.
- **Haptics map:** which interactions trigger which haptic (light tick, confirm, heavy success).

---

## 4. Components (all states, light + dark)

1. **Buttons:** filled, tonal, outlined, text, plus M3 Expressive button groups/split buttons; sizes XS–XL; the **"Start workout" hero button** with its own expressive shape and press morph.
2. **FAB / Extended FAB / FAB menu** (Train tab: "Start empty" / "From template").
3. **Navigation bar** (4 tabs) and the iOS variant notes; selected-indicator animation.
4. **Top app bars:** small, medium, large collapsing, plus the live-workout header with elapsed timer.
5. **Active-workout mini bar** (persistent above the nav bar while a workout is running): workout name, elapsed time, next-set hint, expand affordance.
6. **Set row** (the most important component), with columns: set number/type badge · previous performance (ghost text, e.g. "80 × 8") · weight input · reps input · optional RPE · complete-checkbox.
   - States: empty, prefilled from previous, editing, completed, completed + PR, warmup/drop/failure variants, swipe-to-delete reveal, error (invalid value).
7. **Number input:** big tappable fields that open a **custom numeric keypad bottom sheet** with ± steppers (weight step 0.5/1.25/2.5/5 kg, configurable), plate-friendly quick values, "same as last" chip. Design the keypad.
8. **Exercise card in the live workout:** header (illustration thumbnail, name, menu: reorder, replace, notes, remove), list of set rows, "Add set" button, rest-time chip.
9. **Rest timer:** expanded (bottom sheet/overlay with a big morphing-shape ring, ±15 s, skip) and collapsed (pill in the mini bar / workout header).
10. **Exercise list item and grid tile:** illustration, name, primary muscle chip, equipment icon; selectable variant for the multi-select picker.
11. **Template card:** name, exercise count, estimated duration, muscle groups, last performed, "Start" action.
12. **Workout history card:** date, name, duration, volume, sets, PR count badge, muscle-group dots.
13. **Calendar/heatmap strip:** training-days heatmap for the History header (weeks × days, intensity by volume).
14. **Progress chart:** line/area chart with range selector (1M/3M/6M/1Y/All), metric selector (est. 1RM / top set weight / total volume), PR markers, scrubbing tooltip.
15. **PR badge:** small inline badge, medium card, large celebration variant.
16. **Muscle map:** front/back body silhouette with every major muscle as a separate region (primary, secondary and inactive states); small (40 dp) and large (240 dp+) sizes.
17. **Chips:** filter chips (muscle, equipment), input chips, assist chips.
18. **Search bar** (M3 search, expanded and collapsed).
19. **Sync status indicator:** icon-sized, with states `synced`, `syncing`, `pending (n)`, `offline`, `error`, and a detailed variant for Settings.
20. **Banners & feedback:** offline banner, snackbar (with undo), dialogs (destructive confirmation, unsynced-data warning on logout), bottom sheets, M3 Expressive **loading indicator** (shape-morphing) vs contained loading indicator, skeleton/shimmer placeholders.
21. **Form fields:** text field (filled/outlined), password with visibility toggle, field errors, segmented button (units kg/lb, theme), switch, slider (default rest time), list items for settings.
22. **Empty states & error states:** illustration + title + body + action (patterns, see §5).
23. **Stat tiles:** big number + label + delta (e.g. "Volume this week · 12 340 kg · ▲ 8%").

---

## 5. Screens

For each screen show the default state plus every listed state. Annotate which components from §4 are used.

### A. Launch & auth

1. **Splash / launch:** logo animation (see MOT-01), Android 12+ splash API and iOS launch screen versions.
2. **Welcome:** 3-page value carousel ("Log in 2 taps", "Works offline", "See your progress & PRs"), each page with an animated illustration, plus primary "Create account" and secondary "Log in" buttons.
3. **Register:** display name, email, password (strength meter), terms note. States: validation errors, loading, server error, **no internet** ("You need a connection to create an account").
4. **Log in:** email, password. States: wrong credentials, loading, offline.
5. **Restoring your history** (first sync after login on a new device): animated progress ("Restoring 340 / 1 300 workouts"), which can be skipped into the app with a "continue in background" option. States: in progress, done (transition into Home), failed (retry).

### B. Train tab (home)

6. **Train home:**
   - greeting + sync indicator
   - **weekly summary** (workouts this week vs goal, total volume, streak as an expressive-shape progress ring)
   - **hero "Start workout"** button / FAB menu
   - **"Your templates"** horizontal or vertical list
   - **last workout** card
   - an "active workout in progress" resume card if one exists

   States: first-time user (no templates, no history, onboarding hints), normal, offline.
7. **Templates list** (from "See all"): reorder, delete with undo, empty state.
8. **Template editor:** name, notes, exercises with drag-to-reorder, target sets × reps range, rest per exercise, "Add exercises" (opens picker), save/discard. States: new, editing, validation (no exercises).
9. **Template preview:** exercises, muscles worked (muscle map), estimated duration, last 3 performances, big "Start".

### C. Live workout (the most important flow, design it in the most detail)

10. **Live workout screen:**
    - header with workout name (editable), **elapsed timer**, finish button
    - list of exercise cards with set rows
    - "Add exercise" button
    - rest timer (collapsed)

    States: empty workout (just started, no exercises), in progress, set just completed (rest timer starts), **set beats a PR** (inline PR moment, see MOT-07), editing a value (keypad open), swipe to delete a set, reordering exercises, notes expanded.
11. **Numeric keypad sheet** (weight / reps / RPE modes).
12. **Rest timer expanded:** big morphing countdown, ±15 s, skip, next set preview ("Next: Bench Press · Set 3 · 80 kg × 8"). States: counting, last 5 s (urgency), finished (morph + haptic + "Go!").
13. **Exercise picker** (full-screen, multi-select): search, filter chips, recent/frequent section, selected counter + "Add (3)" button, "Create custom exercise" entry.
14. **Finish confirmation:** a sheet with incomplete sets warning; options "Finish", "Discard workout" (destructive confirmation), "Keep training".
15. **Workout summary (post-finish):** celebration header, duration, total volume, sets, reps, **PRs list** (with PR celebration MOT-08), muscles worked map, comparison with last time, "Save as template", "Done". Variants: with PRs and without PRs (still rewarding but quieter).
16. **Rest-timer notification:** Android notification and iOS notification banner design (title, body, icon). Show both.

### D. History tab

17. **History list:**
    - header with training heatmap / month calendar
    - stat tiles (this month)
    - workouts grouped by week, with sticky headers

    States: empty (first workout CTA), loading skeleton, list, offline (still fully works, so just the indicator).
18. **Workout details:** header (date, duration, volume), exercises with sets (PR sets highlighted), notes, actions: edit, save as template, delete (with undo).
19. **Edit finished workout:** same layout as the live workout, but no timers. Shows "edited" state.

### E. Exercises tab

20. **Exercise library:** search, filter chips (muscle groups, equipment), list/grid toggle, alphabet/section headers, "Custom" section. States: loading skeleton, results, no results (with "Create '{query}' as custom exercise").
21. **Exercise details:**
    - hero illustration (shared-element from list)
    - name, muscles (muscle map), equipment
    - tabs: **About** (instructions, attribution "Data: wger.de, CC-BY-SA"), **History** (sessions with this exercise), **Progress** (chart, see §4.14), **Records** (PR cards: est. 1RM, heaviest, best set volume, most reps)

    States: no history yet.
22. **Create / edit custom exercise:** name, primary muscle (muscle-map picker), secondary muscles, equipment. The illustration is auto-assigned from the primary muscle group; preview it live as the muscle changes.

### F. Profile / Settings tab

23. **Profile & settings:**
    - profile header (avatar with expressive shape, name, email, lifetime stats)
    - preferences: units (kg/lb segmented), theme (system/light/dark), dynamic color switch (Android 12+ only, hidden on iOS), default rest time (slider), language (system/EN/PL)
    - **Sync** section (status, last synced, pending changes, "Sync now")
    - About & licenses (wger attribution)
    - Log out
24. **Sync details:** last sync time, pending changes count, current status animation, error message + retry.
25. **Log out confirmation:** normal and **"N changes haven't been synced yet"** warning variant (sync now / log out anyway).
26. **About & licenses**.

### G. Global states

27. **Offline:** how the app communicates offline globally (subtle, not alarming, because offline is a normal state here). Show it on Train home and Exercise library.
28. **Error patterns:** inline field error, snackbar error with retry, full-screen error (rare), sync error.
29. **Loading patterns:** skeletons vs M3 Expressive loading indicator, and when to use each.

### Key user flows to prototype (connect the frames)

- **F1** First launch → Welcome → Register → Train home (first-time state).
- **F2** Login on a new device → Restoring your history → Train home.
- **F3** Train home → Start from template → log 3 sets (one is a PR) → rest timer → finish → summary.
- **F4** History → workout details → edit a set → back.
- **F5** Exercises → search → details → Progress tab → scrub chart.

---

## 6. Motion catalog

For every item give: **trigger, what animates, spring/duration tokens, choreography/stagger, haptic, reduced-motion fallback**, and a storyboard.

| ID | Moment | Intent |
|---|---|---|
| MOT-01 | **Logo/splash reveal** | "R" mark morphs from a circle into the logo shape, wordmark slides in |
| MOT-02 | **Nav bar selection** | Selected indicator stretches/morphs between tabs (expressive spring), icon outline→filled |
| MOT-03 | **Start workout** | Hero button press morph (shape squish), then **container transform** from the button into the live workout screen |
| MOT-04 | **Active-workout mini bar ↔ full screen** | Container transform/shared bounds; elapsed timer is a shared element |
| MOT-05 | **Set completion** | Checkbox shape morph (circle → rounded square with check drawn as a path), row background floods with `success` container from the checkbox outward, numbers settle with a tiny bounce, light haptic |
| MOT-06 | **Rest timer** | Ring sweep countdown; shape slowly breathes; last 5 s pulse; at 0 it **morphs into a burst shape** + "Go!" + strong haptic; collapse/expand between pill and sheet |
| MOT-07 | **Inline PR moment** | When a completed set beats a PR: row gets the `record` glow, a small trophy badge pops in with an overshoot spring, short particle sparkle (≤600 ms, Canvas particles) |
| MOT-08 | **PR celebration (summary)** | Confetti/particle burst in brand + record colors (Canvas particle system, physics-based), PR cards fly in with stagger, number counters roll up |
| MOT-09 | **Number roll / ticker** | Weight, reps, volume and stats change with a vertical digit roll per digit (odometer), tabular figures so width stays stable |
| MOT-10 | **Keypad sheet** | Sheet springs up; pressed key morphs shape briefly; ± stepper long-press accelerates with tick haptics |
| MOT-11 | **Add exercise to workout** | New exercise card expands in place, others make room (spring), list auto-scrolls to it |
| MOT-12 | **Swipe to delete set / reorder** | Swipe reveals error container with an icon that scales with progress; drag-reorder lifts the card (scale + tonal elevation), neighbours slide |
| MOT-13 | **List → details shared elements** | Exercise illustration + name shared element (library → details), workout card → details container transform |
| MOT-14 | **Chart draw-in & scrubbing** | Line draws from left to right (path trim) when shown or the range changes; area fades in; scrub cursor follows the finger with a snapping tooltip; PR markers pop in |
| MOT-15 | **Heatmap reveal** | Cells appear with a diagonal stagger wave |
| MOT-16 | **Stat tiles & weekly ring** | Ring fills with a spring on appear; numbers roll (MOT-09) |
| MOT-17 | **Sync indicator** | Idle → syncing (rotating/morphing shape loop) → synced (check draw + brief success tint) / offline (gentle fade to warning) |
| MOT-18 | **Restoring history** | Progress expressed as a growing/morphing shape, plus a counter roll; workouts "stack up" visually |
| MOT-19 | **Muscle map highlight** | Primary muscles fill with color (radial fill), secondary follows with a delay; on exercise change, a crossfade per muscle region |
| MOT-20 | **Workout summary reveal** | Staggered entrance: header → stat tiles (counting up) → muscle map → PRs (MOT-08) → actions |
| MOT-21 | **Screen transitions** | Forward/back push transitions using M3 Expressive motion; **predictive back** on Android (preview scale + corner morph); tab switch = fade-through |
| MOT-22 | **Button & card press feedback** | Shape morph/scale on press for all primary interactive surfaces (consistent spring) |
| MOT-23 | **Empty states** | Illustrations with a subtle idle loop (a few seconds, then rest; no infinite distraction) |
| MOT-24 | **Snackbar undo** | Deleted item collapses; on undo it re-expands to its original position |

Also define **global motion rules**: what never animates (e.g. typing in fields), max celebration length (~1.5 s), interruptibility (every animation can be interrupted by user input), and performance limits (particle counts, which animations run on scroll).

---

## 7. Platform adaptations (same Compose UI, small differences)

- **Android:** edge-to-edge, predictive back, Material You dynamic color option, themed monochrome icon, notification style.
- **iOS:** same UI, but respect the safe areas and home indicator, **swipe-back gesture** from the left edge for push navigation, iOS-style notification banner, no dynamic color setting, haptics mapped to iOS feedback generators. Show the Live workout, Exercise details and Settings screens on the iPhone frame.
- Keyboard: numeric inputs never use the system keyboard (custom keypad); text inputs avoid being covered by it.

---

## 8. Illustrations

Every exercise in the app has an illustration. There are ~150 catalog exercises (AI-generated later from your style guide), and custom user exercises fall back to a muscle-group illustration.

Define **one illustration style**, for example a stylised, gender-neutral figure performing the movement, flat or limited shading, the **working muscles highlighted in `musclePrimary`/`muscleSecondary`**, equipment drawn simply, and a consistent camera angle, line weight, background and crop.

It must:
- work at 48 dp (list thumbnail) and at ~360 dp (details hero);
- look right on both light and dark surfaces (transparent background or a defined backdrop shape);
- be achievable consistently by an image-generation model.

Deliver:
1. A written **style spec**: palette, line weight, figure proportions, viewing angle, lighting, background, framing/safe area, export format (square WebP, size).
2. **A reusable image-generation prompt template** with variables (`{exercise_name}`, `{movement_description}`, `{equipment}`, `{primary_muscles}`, `{secondary_muscles}`, `{view: front|side|back}`), plus a negative prompt.
3. **6 sample illustrations:** Barbell Bench Press, Back Squat, Deadlift, Pull-up, Dumbbell Lateral Raise, Cable Triceps Pushdown.
4. **12 muscle-group fallback illustrations:** chest, back, shoulders, biceps, triceps, forearms, abs/core, glutes, quadriceps, hamstrings, calves, full body.
5. **Empty-state illustrations:** no workouts yet, no templates yet, no search results, offline, sync error, no exercise history.

---

## 9. Output format for handoff to code

- **Token table** (Markdown), one row per token: `token name (camelCase) | light value | dark value | usage`. Groups: `color.*`, `color.extended.*`, `type.*`, `shape.*`, `spacing.*`, `elevation.*`, `motion.spring.*`, `motion.duration.*`, `motion.stagger.*`. Colors as hex `#RRGGBB(AA)`, sizes in dp/sp, springs as `dampingRatio` + `stiffness`.
- **Component specs:** anatomy, dimensions, paddings, which tokens each part uses, state table.
- **Screen annotations:** components used, spacing, and which motion IDs (MOT-xx) apply.
- Name everything in **English**, consistently (I will map names 1:1 to Kotlin, e.g. `RepzTheme.colors.record`, `RepzSpacing.md`, `RepzMotion.springs.spatialFast`).
- Show **Polish text** on at least these screens to prove the layouts hold: Train home, Live workout, Workout summary, Settings.
- Give an **accessibility checklist**: contrast results, touch targets, content descriptions for icon-only buttons and illustrations, focus order on the live workout screen, font scaling up to 200% (show Live workout at 200%).

---

## 10. Sample data (use realistic content)

- User: "Alex", trains 4×/week, metric units.
- Templates: **Push A** (Bench Press, Overhead Press, Incline DB Press, Lateral Raise, Triceps Pushdown), **Pull A** (Deadlift, Pull-up, Barbell Row, Face Pull, Biceps Curl), **Legs A** (Back Squat, Romanian Deadlift, Leg Press, Leg Curl, Calf Raise).
- A live workout "Push A" in progress, 34:12 elapsed: Bench Press 3 of 4 sets done (warmup 60×10, then 80×8, 82.5×8 **PR**, current set prefilled 82.5×8), rest timer 1:24 left of 2:30.
- History: 3 months, with a few gaps; current streak 6 weeks.
- Bench Press est. 1RM progress: 92 → 104 kg over 6 months, 4 PR markers.
