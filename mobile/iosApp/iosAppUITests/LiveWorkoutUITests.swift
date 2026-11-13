import XCTest

/// Live workout end-to-end flows: starting an empty workout and from a template, adding
/// exercises, the keypad entry/Next flow, completing sets (incl. the reps-zero rule),
/// swipe-to-delete with Undo, notes, renaming, reorder, discard and Finish.
///
/// Needs the API reachable at `localhost:3000` (`pnpm dev`, migrated DB). `testStartEmptyAddExerciseCompleteDeleteUndo`
/// registers a brand-new account, like `AuthFlowUITests`. The other two tests need a pre-seeded
/// account with one template ("Push A": 3 exercises × 3 sets, 6-8 rep range) and read its
/// credentials from `TEST_RUNNER_REPZ_UITEST_EMAIL` / `TEST_RUNNER_REPZ_UITEST_PASSWORD` (seed
/// commands in the impl log); they skip (not fail) when those aren't set. Never runs in CI, same
/// reasoning as `AuthFlowUITests`. Run explicitly with
/// `-only-testing:iosAppUITests/LiveWorkoutUITests/<method>`.
///
/// Every element below is found the same way `AuthFlowUITests` does: by `accessibilityIdentifier`
/// (Compose `Modifier.testTag`) where the live-workout components expose one, by visible
/// label text otherwise (card-menu rows, the keypad's digit/decimal/Next keys, snackbar actions).
/// Every tap waits for its element first (`tap(identifier:)`/`tap(label:)`): the live workout's
/// screens have their own loading states and sheet slide-in animations, so an element that will
/// exist a moment later often doesn't exist yet the instant the previous action returns.
/// Every card/set lookup that can sit outside the exercise list's initial viewport goes through
/// `scrollToElement` instead of a plain wait: CMP's iOS accessibility bridge only reports a
/// `LazyColumn` item once it has actually scrolled into the container's visible bounds (confirmed a
/// viewport/accessibility-bridge limit, not a `commonMain` layout bug - impl-log.md, "Flex · round 3
/// · W1 card bug" - the identical shared layout renders every card correctly on Android at the same
/// scroll position), so a longer timeout alone would never find one that hasn't been scrolled to.
/// Several assertions read a set row's complete-checkbox `label`: its content description is built
/// from the set's own badge/weight/reps (`set_complete`/`set_uncomplete`, `LiveWorkoutSetMapping.kt`)
/// and is the one place the exact typed values are exposed as plain, stable text; `RepzSetRow`'s
/// weight/reps *fields* expose a fixed label ("Weight, set 1") instead, so they aren't usable for
/// reading back the value, only for tapping to open the keypad on them. A merged accessible node
/// (`semantics(mergeDescendants = true)`) concatenates its own `contentDescription` with any
/// descendant text instead of replacing it - the keypad's Next key, for instance, reads
/// "Next: repetitions, Reps" (description, then the visible destination label), not just the
/// description alone - so lookups on those nodes use `tap(containingLabel:)`, not an exact match.
final class LiveWorkoutUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        // Forced regardless of the host Mac's own region, so "82.5 kg"/"Decimal separator"
        // (EN default) are deterministic; Spotter's own PL pass is a separate concern.
        app.launchArguments += ["-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
    }

    /// Start an empty workout, add one exercise from the picker, add a second set, exercise the
    /// "reps 0" rule on it (completing an untouched set opens the keypad instead of completing it),
    /// complete it, then run the full keypad entry + Next flow (typed digits, the decimal separator,
    /// retargeting, tap-outside dismissal) on the first set, complete both, and finally swipe-delete
    /// a third set with Undo (restore, then let the snackbar expire) and confirm a start-to-end
    /// swipe is a no-op.
    func testStartEmptyAddExerciseCompleteDeleteUndo() throws {
        ensureSignedOut()
        registerNewAccount()
        // The active workout is local-only, device-wide state with no account scoping (ADR-0023):
        // a stale one left by an earlier, unrelated test account on this same simulator install
        // would otherwise still show up as "Resume" for this brand-new account too.
        ensureNoActiveWorkout()

        tap(identifier: "train_start_empty")
        let liveFinish = element(identifier: "live_finish")
        XCTAssertTrue(liveFinish.waitForExistence(timeout: 10), "live_finish not found")
        XCTAssertFalse(liveFinish.isEnabled, "Finish should start disabled with nothing completed")
        XCTAssertTrue(element(identifier: "live_empty_add_exercises").waitForExistence(timeout: 5), "Empty state not shown")
        attach("ios-live-empty")

        tap(identifier: "live_empty_add_exercises")
        let firstPickerItem = element(identifier: "picker_item_0")
        XCTAssertTrue(firstPickerItem.waitForExistence(timeout: 20), "Exercise picker never loaded any exercise")
        attach("ios-picker")

        firstPickerItem.tap()
        scrollToElement(identifier: "card_0", reason: "card_0 not found after adding the first exercise")
        attach("ios-live-one-card")

        scrollToElement(identifier: "card_0_add_set", reason: "card_0_add_set not found").tap()
        let set01Check = scrollToElement(identifier: "set_0_1_check", reason: "set_0_1 was not added")

        // Completing an untouched (reps 0) set doesn't complete it, it opens the keypad on reps.
        assertLabel(set01Check, contains: "Complete set 2")
        set01Check.tap()
        XCTAssertFalse(
            element(identifier: "live_add_exercise").exists,
            "Tapping check on a reps-0 set should open the keypad (bottom bar hidden), not complete it"
        )
        assertLabel(set01Check, contains: "for 0 reps", "Completing a reps-0 set should not complete it")

        tap(label: "5")
        tap(label: "Done")
        XCTAssertTrue(element(identifier: "live_add_exercise").waitForExistence(timeout: 5), "Keypad did not close after Done")
        assertLabel(set01Check, contains: "for 5 reps", "Reps typed via the keypad were not saved")

        set01Check.tap()
        assertLabel(set01Check, contains: "Mark set 2 as not done", "set_0_1 should complete now that reps > 0")
        XCTAssertTrue(liveFinish.isEnabled, "Finish should enable once a set is completed")

        // Full keypad entry on set 0 - digits, the decimal separator, Next (Weight -> Reps), then
        // dismiss by tapping outside the sheet instead of following Next into the other set.
        scrollToElement(identifier: "set_0_0_weight", reason: "set_0_0_weight not found").tap()
        XCTAssertFalse(element(identifier: "live_add_exercise").exists, "Keypad should hide the bottom bar")
        tap(label: "8")
        tap(label: "2")
        tap(containingLabel: "Decimal separator")
        tap(label: "5")
        tap(containingLabel: "Next: repetitions")
        tap(label: "8")
        tapOutsideKeypad()
        XCTAssertTrue(element(identifier: "live_add_exercise").waitForExistence(timeout: 5), "Tap-outside did not dismiss the keypad")

        let set00Check = scrollToElement(identifier: "set_0_0_check", reason: "set_0_0_check not found")
        // "82.5\u{00A0}kg": formatSetWeightWithUnit joins the value and the unit with a non-breaking
        // space, not a plain one, so the value and the unit are checked separately rather than
        // depending on that exact character.
        assertLabel(set00Check, contains: "82.5", "Typed weight was not saved")
        assertLabel(set00Check, contains: "kg", "Typed weight's unit was not shown")
        assertLabel(set00Check, contains: "8 reps", "Typed reps were not saved")
        attach("ios-keypad-flow")

        set00Check.tap()
        assertLabel(set00Check, contains: "Mark set 1 as not done", "set_0_0 should complete")
        XCTAssertTrue(
            element(label: NSPredicate(format: "label CONTAINS %@", "2 of 2 done")).waitForExistence(timeout: 5),
            "Card progress should read 2 of 2 done"
        )
        attach("ios-complete-sets")

        // Swipe-to-delete + Undo on a third, untouched set.
        scrollToElement(identifier: "card_0_add_set", reason: "card_0_add_set not found").tap()
        let set02 = scrollToElement(identifier: "set_0_2", reason: "set_0_2 was not added")

        set02.swipeLeft()
        XCTAssertFalse(element(identifier: "set_0_2").waitForExistence(timeout: 3), "Swiped set should be removed at once")
        let undoButton = element(label: "Undo")
        XCTAssertTrue(undoButton.waitForExistence(timeout: 5), "Undo snackbar did not appear")
        attach("ios-swipe-undo")
        undoButton.tap()
        XCTAssertTrue(element(identifier: "set_0_2").waitForExistence(timeout: 5), "Undo did not restore the deleted set")

        element(identifier: "set_0_2").swipeLeft()
        XCTAssertFalse(element(identifier: "set_0_2").waitForExistence(timeout: 3), "Swiped set should be removed at once")
        XCTAssertTrue(element(label: "Undo").waitForExistence(timeout: 5), "Undo snackbar did not appear the second time")
        Thread.sleep(forTimeInterval: 7) // outlasts the 6 s undo window (UndoSnackbarDuration)
        XCTAssertFalse(element(label: "Undo").exists, "Undo snackbar should have expired")
        XCTAssertFalse(element(identifier: "set_0_2").exists, "A set should stay deleted once Undo has expired")

        // A start-to-end swipe (right) is a no-op.
        let set01 = element(identifier: "set_0_1")
        XCTAssertTrue(set01.exists, "set_0_1 should still be present before the no-op swipe")
        set01.swipeRight()
        XCTAssertTrue(element(identifier: "set_0_1_check").waitForExistence(timeout: 3), "A start-to-end swipe must not delete the row")
    }

    /// Start the seeded "Push A" template (confirming the no-history prefill), add exercise notes
    /// and confirm they survive a minimize/resume round trip, rename the workout (including
    /// restoring the last name when the new one is left blank), attempt the reorder-mode drag,
    /// then walk the discard dialog both ways (Keep training, then Discard).
    func testTemplateReorderNotesDiscard() throws {
        try signInToSeededAccount()
        ensureNoActiveWorkout()

        tap(identifier: "train_template_0_start")
        // The second and third exercise cards sit outside the list's initial viewport (see the class
        // doc's note on scrollToElement) - every lookup below scrolls the card/set it needs into view
        // first, in whichever direction gets it there, instead of just waiting longer.
        scrollToElement(identifier: "card_2", reason: "Push A should start with 3 exercise cards")
        assertLabel(element(identifier: "live_name"), contains: "Push A", "Workout name should be the template's name")

        let set00Check = scrollToElement(identifier: "set_0_0_check", direction: .up, reason: "set_0_0_check not found")
        assertLabel(set00Check, contains: "for 6 reps", "A template row with no history should prefill reps to the bottom of its range")
        attach("ios-live-template")

        // Exercise notes persist across minimize/resume.
        scrollToElement(identifier: "card_1_menu", reason: "card_1_menu not found").tap()
        tap(label: "Notes")
        let notesField = scrollToElement(identifier: "card_1_notes", reason: "card_1_notes field did not appear")
        notesField.tap()
        notesField.typeText("Pause on chest")
        Thread.sleep(forTimeInterval: 1) // outlasts the 500 ms notes debounce so the draft is saved before we navigate away

        // While the notes field's on-screen keyboard is up, `live_minimize` (the fixed header's own
        // button, no app code applies any ime/keyboard inset here) is reported at a corrupted,
        // off-screen frame - the same accessibility-bridge limitation documented in the class doc,
        // now affecting a pinned header element instead of a scrolled LazyColumn item. A raw
        // coordinate tap at its known on-screen position (same technique as tapOutsideKeypad())
        // reaches the real, correctly-rendered button instead of going through the broken lookup.
        tapRawCoordinate(dx: 0.086, dy: 0.093)

        let trainResume = element(identifier: "train_resume")
        XCTAssertTrue(trainResume.waitForExistence(timeout: 10), "Minimize did not return to Train")
        trainResume.tap()
        scrollToElement(identifier: "card_2", reason: "Resume did not return to the live workout")
        let notesAfterResume = scrollToElement(identifier: "card_1_notes", direction: .up, reason: "Notes row should still show after resuming")
        XCTAssertEqual(notesAfterResume.label, "Pause on chest", "Notes text did not survive minimize/resume")
        attach("ios-notes")

        // Rename: tap the name to edit it, clear the old value and type a new one, then finish with
        // the keyboard's Done key (the field's own `KeyboardActions(onDone = …)`, more reliable than
        // tapping elsewhere since it's wired for exactly this). A blank name on Done restores the
        // last non-empty name rather than saving blank.
        tap(identifier: "live_name")
        let nameField = element(identifier: "live_name")
        XCTAssertTrue(nameField.waitForExistence(timeout: 5), "live_name field did not appear in edit mode")
        clearText(nameField)
        nameField.typeText("Push A heavy")
        nameField.typeText("\n")
        assertLabel(element(identifier: "live_name"), contains: "Push A heavy", "Renaming the workout did not save the new name")
        attach("ios-rename")

        tap(identifier: "live_name")
        let nameFieldAgain = element(identifier: "live_name")
        XCTAssertTrue(nameFieldAgain.waitForExistence(timeout: 5), "live_name field did not reappear in edit mode")
        clearText(nameFieldAgain)
        nameFieldAgain.typeText("\n")
        assertLabel(element(identifier: "live_name"), contains: "Push A heavy", "A blank rename should restore the last name, not save blank")

        // Reorder mode; the drag itself is a best-effort XCUITest gesture (noted in the impl log
        // either way).
        scrollToElement(identifier: "card_0_menu", reason: "card_0_menu not found").tap()
        tap(label: "Reorder")
        let row0 = scrollToElement(identifier: "reorder_row_0", reason: "Reorder mode did not open")
        let row2 = scrollToElement(identifier: "reorder_row_2", reason: "reorder_row_2 not found")
        attach("ios-reorder-mode-before")

        let originalFirstRowLabel = row0.label
        let start = row0.coordinate(withNormalizedOffset: CGVector(dx: 0.08, dy: 0.5))
        let end = row2.coordinate(withNormalizedOffset: CGVector(dx: 0.08, dy: 0.5))
        start.press(forDuration: 0.6, thenDragTo: end)
        attach("ios-reorder-mode-after")
        if row0.label != originalFirstRowLabel {
            attach("ios-reorder-drag-moved")
        } else {
            // The long-press-drag gesture isn't reliably drivable through XCUITest/Compose's pointer
            // input bridge on this host (noted in the impl log for Spotter's manual pass); reorder
            // mode's own entry/exit and the Move up/down accessibility actions (VoiceOver) are
            // Spotter's job, not re-asserted here as a hard failure.
            add(XCTAttachment(string: "Reorder drag did not change row order; see impl-log.md deviations."))
        }
        tap(identifier: "reorder_done")
        scrollToElement(identifier: "card_2", reason: "Done did not return to the normal card list")

        // Discard, first cancelled, then confirmed, with at least one completed set.
        scrollToElement(identifier: "set_0_0_check", direction: .up, reason: "set_0_0_check not found before discard flow").tap()
        assertLabel(element(identifier: "set_0_0_check"), contains: "Mark set", "Completing set_0_0 failed")

        tap(identifier: "live_overflow")
        tap(identifier: "live_discard")
        let discardCancel = element(identifier: "discard_cancel")
        let discardConfirm = element(identifier: "discard_confirm")
        XCTAssertTrue(discardConfirm.waitForExistence(timeout: 5), "Discard dialog did not appear with a completed set")
        attach("ios-discard-dialog")
        discardCancel.tap()
        scrollToElement(identifier: "card_2", reason: "Keep training should return to the live workout")

        tap(identifier: "live_overflow")
        tap(identifier: "live_discard")
        XCTAssertTrue(discardConfirm.waitForExistence(timeout: 5), "Discard dialog did not reappear")
        discardConfirm.tap()
        XCTAssertTrue(element(identifier: "train_start_empty").waitForExistence(timeout: 10), "Discard did not return to Train")
    }

    /// Start Push A, complete two sets, leave the rest untouched, Finish, and confirm the live
    /// workout closes back to Train with "Start workout" available again (the finished workout's
    /// own row content is Spotter's `psql` check, not this UI test's job).
    func testFinishWorkout() throws {
        try signInToSeededAccount()
        ensureNoActiveWorkout()

        tap(identifier: "train_template_0_start")
        scrollToElement(identifier: "card_2", reason: "Push A should start with 3 exercise cards")

        scrollToElement(identifier: "set_0_0_check", direction: .up, reason: "set_0_0_check not found").tap()
        scrollToElement(identifier: "set_1_0_check", reason: "set_1_0_check not found").tap()
        assertLabel(element(identifier: "set_0_0_check"), contains: "Mark set", "Completing set_0_0 failed")
        assertLabel(element(identifier: "set_1_0_check"), contains: "Mark set", "Completing set_1_0 failed")

        let liveFinish = element(identifier: "live_finish")
        XCTAssertTrue(liveFinish.isEnabled, "Finish should be enabled with 2 completed sets")
        attach("ios-finish-before")
        liveFinish.tap()

        XCTAssertTrue(element(identifier: "train_start_empty").waitForExistence(timeout: 10), "Finish did not return to Train")
        attach("ios-finish")
    }

    // MARK: - Shared setup

    /// Launches the app and leaves it on a signed-out, interactive Welcome screen, regardless of
    /// whichever account a previous test method (or a previous, interrupted run) left signed in:
    /// the app persists its session in the Keychain across launches (ADR-0025), so a plain
    /// `launch()` alone isn't enough to guarantee Welcome actually shows.
    private func ensureSignedOut() {
        app.launch()
        if element(label: "Train").waitForExistence(timeout: 8) {
            logOutFromProfile()
        }
        waitForWelcomeReady()
    }

    private func logOutFromProfile() {
        tap(label: "Profile")
        let logOut = element(identifier: "profile_log_out")
        XCTAssertTrue(logOut.waitForExistence(timeout: 5), "profile_log_out not found")
        logOut.tap()
        let confirm = element(identifier: "log_out_confirm")
        XCTAssertTrue(confirm.waitForExistence(timeout: 5), "log_out_confirm not found")
        confirm.tap()
        XCTAssertTrue(element(identifier: "welcome_create_account").waitForExistence(timeout: 10), "Did not return to Welcome after logging out")
    }

    /// Logs into the seeded account (impl log's seed commands) from a signed-out Welcome screen.
    /// Skips (doesn't fail) the test when the env vars aren't set, since the pipeline that runs this
    /// suite is expected to set them explicitly.
    private func signInToSeededAccount() throws {
        let environment = ProcessInfo.processInfo.environment
        let email = environment["REPZ_UITEST_EMAIL"]
        let password = environment["REPZ_UITEST_PASSWORD"]
        try XCTSkipIf(
            email == nil || password == nil,
            "Set TEST_RUNNER_REPZ_UITEST_EMAIL/TEST_RUNNER_REPZ_UITEST_PASSWORD to a seeded account (impl-log.md) to run this test"
        )

        ensureSignedOut()
        tap(identifier: "welcome_log_in")

        let loginEmail = element(identifier: "login_email")
        XCTAssertTrue(loginEmail.waitForExistence(timeout: 5), "login_email not found")
        loginEmail.tap()
        loginEmail.typeText(email!)

        let loginPassword = element(identifier: "login_password")
        loginPassword.tap()
        loginPassword.typeText(password!)

        tap(identifier: "login_submit")
        XCTAssertTrue(element(label: "Train").waitForExistence(timeout: 10), "Did not land on Train after logging in")
        goToTrainTab()
    }

    /// A leftover active workout (a previous run of this suite stopping mid-test) would make
    /// `train_template_0_start` fail with `WorkoutAlreadyActive`, so every template-based test
    /// starts by resuming and discarding whatever is there, leaving Train in its "Start" state.
    private func ensureNoActiveWorkout() {
        let trainResume = element(identifier: "train_resume")
        guard trainResume.waitForExistence(timeout: 3) else { return }
        trainResume.tap()

        XCTAssertTrue(element(identifier: "live_overflow").waitForExistence(timeout: 10), "Resumed workout did not open")
        tap(identifier: "live_overflow")
        tap(identifier: "live_discard")
        let discardConfirm = element(identifier: "discard_confirm")
        if discardConfirm.waitForExistence(timeout: 2) {
            discardConfirm.tap()
        }
        XCTAssertTrue(element(identifier: "train_start_empty").waitForExistence(timeout: 10), "Could not clear the leftover active workout")
    }

    /// Taps a point above where the keypad sheet sits (it only covers roughly the bottom half), to
    /// hit the transparent full-screen overlay `LiveWorkoutContent` places beneath it
    /// (`LiveWorkoutIntent.KeypadDismissed`).
    private func tapOutsideKeypad() {
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.12)).tap()
    }

    /// Taps a point by its normalized position in the app's own window instead of by
    /// accessibility identifier - bypasses XCUITest's identifier-based hit-testing entirely, for the
    /// rare element whose accessibility frame is reported wrong while the real, rendered pixel is
    /// unaffected (see the call sites for the specific, confirmed case each one works around).
    private func tapRawCoordinate(dx: Double, dy: Double) {
        app.coordinate(withNormalizedOffset: CGVector(dx: dx, dy: dy)).tap()
    }

    /// Clears a focused text field by sending one delete keystroke per existing character (plus a
    /// margin), rather than assuming a starting value: `typeText` only appends, it never replaces,
    /// and a trailing delete past an empty field is a no-op, so this is safe even if the field is
    /// already blank.
    private func clearText(_ element: XCUIElement) {
        let currentValue = (element.value as? String) ?? ""
        let deleteCount = currentValue.count + 30
        element.typeText(String(repeating: XCUIKeyboardKey.delete.rawValue, count: deleteCount))
    }

    /// Which way to scroll the exercise list in [scrollToElement]: `.down` (a `swipeUp()` gesture)
    /// reveals content below the current viewport, `.up` (`swipeDown()`) reveals content above it.
    private enum ScrollDirection {
        case down
        case up
    }

    /// Scrolls the live workout's exercise list in bounded steps until `identifier` exists *and* is
    /// hittable, then returns it. `waitForExistence` alone isn't enough: a `LazyColumn` item can enter
    /// CMP's iOS accessibility tree (so `exists` turns true) a moment before it has actually settled
    /// inside the scroll container's visible bounds, which still fails a `.tap()` with "Not hittable"
    /// at a negative/off-screen coordinate - the same "exists == true AND isHittable == true" pattern
    /// `waitForWelcomeReady` already uses, applied per scroll step here. The same bridge quirk also
    /// makes a card that's barely past the hittability threshold (not comfortably settled) an
    /// unreliable tap target even once `isHittable` reports true, so a short settle pause follows
    /// before handing the element back. Only for a "this should exist" check: never call it where the
    /// element is expected to be absent, since it would otherwise scroll the whole list hunting for it.
    @discardableResult
    private func scrollToElement(
        identifier: String,
        direction: ScrollDirection = .down,
        maxSwipes: Int = 6,
        stepTimeout: TimeInterval = 2,
        reason: String = ""
    ) -> XCUIElement {
        let el = element(identifier: identifier)
        let isReady = NSPredicate(format: "exists == true AND isHittable == true")
        for _ in 0..<maxSwipes {
            let expectation = XCTNSPredicateExpectation(predicate: isReady, object: el)
            if XCTWaiter().wait(for: [expectation], timeout: stepTimeout) == .completed {
                waitUntilFrameSettles(el)
                return el
            }
            switch direction {
            case .down: app.swipeUp()
            case .up: app.swipeDown()
            }
        }
        let finalExpectation = XCTNSPredicateExpectation(predicate: isReady, object: el)
        let finalResult = XCTWaiter().wait(for: [finalExpectation], timeout: stepTimeout)
        if finalResult != .completed {
            print("DEBUG_SCROLL_TARGET_MISSING(\(identifier)):\n\(app.debugDescription)")
        }
        XCTAssertEqual(
            finalResult,
            .completed,
            reason.isEmpty ? "\(identifier) not hittable, even after scrolling" : reason
        )
        return el
    }

    /// Polls `element`'s `frame` until it reports the same value on two consecutive checks (or
    /// `maxChecks` is exhausted): `isHittable` alone can flip true for a `LazyColumn` item a frame or
    /// two before the scroll's fling/deceleration has actually stopped moving it, so a tap right then
    /// can land on the wrong, still-settling coordinate.
    private func waitUntilFrameSettles(_ element: XCUIElement, maxChecks: Int = 8, interval: TimeInterval = 0.25) {
        var lastFrame = element.frame
        for _ in 0..<maxChecks {
            usleep(UInt32(interval * 1_000_000))
            let currentFrame = element.frame
            if currentFrame == lastFrame {
                return
            }
            lastFrame = currentFrame
        }
    }

    // MARK: - Element lookup and tapping (duplicated from `AuthFlowUITests`: this file owns its own test target path)

    @discardableResult
    private func registerNewAccount(
        displayName: String = "UI Test",
        password: String = "correct-horse-battery"
    ) -> String {
        let email = "uitest+\(Int(Date().timeIntervalSince1970))@repz.app"

        app.swipeLeft()
        app.swipeLeft()

        let createAccount = element(identifier: "welcome_create_account")
        XCTAssertTrue(createAccount.waitForExistence(timeout: 5), "welcome_create_account not found")
        createAccount.tap()

        let displayNameField = element(identifier: "register_display_name")
        XCTAssertTrue(displayNameField.waitForExistence(timeout: 5), "register_display_name not found")
        displayNameField.tap()
        displayNameField.typeText(displayName)

        let registerEmail = element(identifier: "register_email")
        registerEmail.tap()
        registerEmail.typeText(email)

        let registerPassword = element(identifier: "register_password")
        registerPassword.tap()
        registerPassword.typeText(password)

        tap(identifier: "register_submit")

        XCTAssertTrue(element(label: "Train").waitForExistence(timeout: 10), "Did not land on the signed-in shell after registering")
        goToTrainTab()
        return email
    }

    /// Explicitly selects the Train tab: the bottom nav bar's "Train" label existing only proves the
    /// signed-in shell loaded, not that Train is the *visible* tab - a sign-out/sign-in cycle inside
    /// one test run (`ensureSignedOut`) can leave a different tab selected (e.g. Profile, from
    /// `logOutFromProfile`), so every caller that needs the Train screen itself taps this first.
    private func goToTrainTab() {
        tap(label: "Train")
    }

    private func waitForWelcomeReady(timeout: TimeInterval = 10) {
        let cta = element(identifier: "welcome_log_in")
        let isReady = NSPredicate(format: "exists == true AND isHittable == true")
        let expectation = XCTNSPredicateExpectation(predicate: isReady, object: cta)
        let result = XCTWaiter().wait(for: [expectation], timeout: timeout)
        XCTAssertEqual(result, .completed, "Welcome was not interactive (launch hand-off never finished) within \(timeout)s")
    }

    /// Waits for the identified element to exist, then taps it - every one of this suite's screens
    /// has a loading state or a sheet slide-in animation, so an element found a moment later often
    /// doesn't exist yet right after the previous action returns.
    @discardableResult
    private func tap(identifier: String, timeout: TimeInterval = 10) -> XCUIElement {
        let el = element(identifier: identifier)
        XCTAssertTrue(el.waitForExistence(timeout: timeout), "\(identifier) not found")
        el.tap()
        return el
    }

    @discardableResult
    private func tap(label: String, timeout: TimeInterval = 10) -> XCUIElement {
        let el = element(label: label)
        XCTAssertTrue(el.waitForExistence(timeout: timeout), "\(label) not found")
        el.tap()
        return el
    }

    /// For a merged node whose label is "<description>, <descendant text>" (see the class doc):
    /// matches on the description prefix instead of the full, less predictable merged string.
    @discardableResult
    private func tap(containingLabel text: String, timeout: TimeInterval = 10) -> XCUIElement {
        let el = element(label: NSPredicate(format: "label CONTAINS %@", text))
        XCTAssertTrue(el.waitForExistence(timeout: timeout), "No label containing '\(text)' found")
        el.tap()
        return el
    }

    /// Polls (doesn't just read once) `element`'s `label` until it contains `text`: a set row's
    /// content description is recomposed from state a frame or two after the action that changed it
    /// (a tap, a keypad digit, a snackbar undo) returns, so reading `.label` immediately can still
    /// observe the previous value.
    private func assertLabel(
        _ element: XCUIElement,
        contains text: String,
        _ reason: String = "",
        timeout: TimeInterval = 5
    ) {
        // A manual poll, not `XCTNSPredicateExpectation`: that mechanism doesn't reliably
        // re-evaluate a `label`-based predicate against a live `XCUIElement` (it appeared to judge
        // the whole wait on one early, stale snapshot), unlike `exists`/`isHittable`, which it does
        // handle (`waitForWelcomeReady`). Every `.label` access here is its own live IPC query.
        let deadline = Date().addingTimeInterval(timeout)
        var lastLabel = element.label
        while !lastLabel.contains(text) && Date() < deadline {
            usleep(100_000)
            lastLabel = element.label
        }
        XCTAssertTrue(lastLabel.contains(text), "\(reason): expected label to contain '\(text)', got '\(lastLabel)'")
    }

    private func element(identifier: String) -> XCUIElement {
        app.descendants(matching: .any).matching(identifier: identifier).firstMatch
    }

    private func element(label: String) -> XCUIElement {
        app.descendants(matching: .any)
            .matching(NSPredicate(format: "label == %@", label))
            .firstMatch
    }

    private func element(label predicate: NSPredicate) -> XCUIElement {
        app.descendants(matching: .any).matching(predicate).firstMatch
    }

    private func attach(_ name: String) {
        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = name
        screenshot.lifetime = .keepAlways
        add(screenshot)
    }
}
