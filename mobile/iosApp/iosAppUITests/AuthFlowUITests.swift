import XCTest

/// End-to-end auth flow against the local API (REP-30 §3.9, §6.3): Welcome -> Register/Login ->
/// signed-in shell, plus the Keychain-restore check from REP-25 Q6, the Profile tab with its
/// log-out confirmation dialog, and logging back in afterward.
///
/// Needs the API reachable at `localhost:3000` (`pnpm dev`, migrated DB) and the app not yet
/// signed in. Never runs in CI: `mobile.yml`'s iOS job only ever calls `xcodebuild ... build`
/// (never `xcodebuild test`), so this target's `TestAction` wiring is irrelevant to CI either way.
/// Run explicitly with `-only-testing:iosAppUITests/AuthFlowUITests/<method>`, see the impl log.
///
/// Compose's `Modifier.testTag` becomes the element's `accessibilityIdentifier` on iOS
/// unconditionally (verified against the Compose Multiplatform 1.12.2 `ui` sources,
/// `iosMain/androidx/compose/ui/platform/Accessibility.ios.kt`: `accessibilityIdentifier` reads
/// `SemanticsProperties.TestTag` directly, no `testTagsAsResourceId` opt-in needed on iOS, unlike
/// Android). Lookups below therefore search by identifier across any element type, because the
/// exact `XCUIElementType` Compose maps a given control to is not documented and not worth
/// guessing; elements without a test tag (the "Train" nav item, the credentials-rejected message)
/// are matched by their visible, localized label instead.
final class AuthFlowUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
    }

    /// I2 (register -> signed-in shell) and I3, the REP-25 Q6 Keychain check (relaunch stays
    /// signed in without showing any auth screen).
    func testRegisterThenRelaunchStaysSignedIn() throws {
        app.launch()
        waitForWelcomeReady()
        attach("welcome-page-1")

        let email = registerNewAccount()
        attach("signed-in-after-register")

        app.terminate()
        app.launch()

        XCTAssertTrue(trainTab.waitForExistence(timeout: 10), "Session was not restored from the Keychain on relaunch")
        XCTAssertFalse(element(identifier: "welcome_create_account").exists, "Welcome re-appeared after relaunch")
        attach("signed-in-after-relaunch")

        let emailAttachment = XCTAttachment(string: email)
        emailAttachment.name = "registered-email"
        emailAttachment.lifetime = .keepAlways
        add(emailAttachment)
    }

    /// Register -> Profile tab shows the account header -> the log-out dialog cancels without
    /// side effects -> confirming it clears the session and returns to Welcome at once ->
    /// relaunch stays on Welcome, proving the Keychain was cleared -> logging back in with the
    /// same credentials lands on Train (ADR-0025) and Profile shows the same account again.
    func testLogOutThenLogBackIn() throws {
        let password = "correct-horse-battery"
        app.launch()
        waitForWelcomeReady()
        attach("welcome-page-1")

        let email = registerNewAccount(password: password)

        let profileTab = element(label: "Profile")
        XCTAssertTrue(profileTab.waitForExistence(timeout: 5), "Profile nav item not found")
        profileTab.tap()

        let displayName = element(identifier: "profile_display_name")
        XCTAssertTrue(displayName.waitForExistence(timeout: 5), "profile_display_name not found")
        XCTAssertEqual(displayName.label, "UI Test", "Profile header showed the wrong display name")
        let emailLabel = element(identifier: "profile_email")
        XCTAssertEqual(emailLabel.label, email, "Profile header showed the wrong email")
        attach("profile")

        element(identifier: "profile_log_out").tap()
        let cancelButton = element(identifier: "log_out_cancel")
        XCTAssertTrue(cancelButton.waitForExistence(timeout: 5), "log_out_cancel not found")
        attach("logout-dialog")
        cancelButton.tap()
        XCTAssertTrue(displayName.waitForExistence(timeout: 5), "Cancel did not return to Profile")

        element(identifier: "profile_log_out").tap()
        let confirmButton = element(identifier: "log_out_confirm")
        XCTAssertTrue(confirmButton.waitForExistence(timeout: 5), "log_out_confirm not found")
        confirmButton.tap()

        let createAccount = element(identifier: "welcome_create_account")
        XCTAssertTrue(createAccount.waitForExistence(timeout: 10), "Did not return to Welcome after logging out")
        attach("welcome-after-logout")

        app.terminate()
        app.launch()
        XCTAssertTrue(
            element(identifier: "welcome_create_account").waitForExistence(timeout: 10),
            "Welcome did not persist across relaunch after logging out"
        )
        attach("welcome-after-relaunch")

        waitForWelcomeReady()
        element(identifier: "welcome_log_in").tap()
        let loginEmail = element(identifier: "login_email")
        XCTAssertTrue(loginEmail.waitForExistence(timeout: 5), "login_email not found")
        loginEmail.tap()
        loginEmail.typeText(email)
        let loginPassword = element(identifier: "login_password")
        loginPassword.tap()
        loginPassword.typeText(password)
        element(identifier: "login_submit").tap()

        XCTAssertTrue(trainTab.waitForExistence(timeout: 10), "Did not land on Train after logging back in")
        attach("train-after-relogin")

        element(label: "Profile").tap()
        XCTAssertTrue(displayName.waitForExistence(timeout: 5), "profile_display_name not found after logging back in")
        XCTAssertEqual(displayName.label, "UI Test", "Profile showed the wrong display name after logging back in")
        XCTAssertEqual(emailLabel.label, email, "Profile showed the wrong email after logging back in")
        attach("profile-after-relogin")
    }

    /// The same Profile and log-out dialog walk in Polish (board 13 copy), launched with the
    /// Polish locale so `-AppleLanguages`/`-AppleLocale` select the generated `values-pl`
    /// resources (ADR-0017).
    func testProfileInPolish() throws {
        app.launchArguments += ["-AppleLanguages", "(pl)", "-AppleLocale", "pl_PL"]
        app.launch()
        waitForWelcomeReady()
        attach("welcome-page-1-pl")

        registerNewAccount(signedInTabLabel: "Trening")

        let profileTab = element(label: "Profil")
        XCTAssertTrue(profileTab.waitForExistence(timeout: 5), "Profil nav item not found")
        profileTab.tap()
        attach("profile-pl")

        element(identifier: "profile_log_out").tap()
        XCTAssertTrue(element(identifier: "log_out_confirm").waitForExistence(timeout: 5), "log_out_confirm not found")
        attach("logout-dialog-pl")
    }

    /// I5: a never-registered email on a fresh install (Spotter uninstalls the app first; this
    /// test does not, since it only needs a signed-out Welcome screen, not a clean install).
    func testWrongPasswordShowsError() throws {
        app.launch()
        waitForWelcomeReady()
        attach("welcome-page-1")

        element(identifier: "welcome_log_in").tap()

        let loginEmail = element(identifier: "login_email")
        XCTAssertTrue(loginEmail.waitForExistence(timeout: 5), "login_email not found")
        loginEmail.tap()
        loginEmail.typeText("never-registered-\(Int(Date().timeIntervalSince1970))@repz.app")

        let loginPassword = element(identifier: "login_password")
        loginPassword.tap()
        loginPassword.typeText("wrong-password")

        element(identifier: "login_submit").tap()

        XCTAssertTrue(
            element(label: "Email or password is incorrect").waitForExistence(timeout: 10),
            "Wrong-credentials error was not shown"
        )
        attach("login-wrong-credentials")
    }

    /// Registers a brand-new account from the Welcome pager (board 07) through to the signed-in
    /// shell and returns the email used. Assumes `app.launch()` already ran and Welcome page 1 is
    /// showing. `signedInTabLabel` is the localized nav bar label for the first tab ("Train" in
    /// English), since the signed-in shell is detected by that label, not a test tag.
    @discardableResult
    private func registerNewAccount(
        displayName: String = "UI Test",
        password: String = "correct-horse-battery",
        signedInTabLabel: String = "Train"
    ) -> String {
        let email = "uitest+\(Int(Date().timeIntervalSince1970))@repz.app"

        // 3-page Welcome pager (board 07): two swipes reach the last page, where Create account /
        // Log in sit alongside Skip.
        app.swipeLeft()
        app.swipeLeft()
        attach("welcome-page-3")

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

        attach("register-filled")
        element(identifier: "register_submit").tap()

        XCTAssertTrue(
            element(label: signedInTabLabel).waitForExistence(timeout: 10),
            "Did not land on the signed-in shell after registering"
        )
        return email
    }

    /// Waits out the MOT-01 launch hand-off (bar -> plates -> crossfade, REP-21) before the first
    /// interaction with Welcome. The hand-off overlay sits on top of Welcome, consumes every touch
    /// and hides Welcome from accessibility until it finishes; a tap landing during it only skips
    /// the animation instead of reaching Welcome underneath. `welcome_log_in` lives outside the
    /// pager and is present on all 3 Welcome pages, so it is a reliable "Welcome is interactive"
    /// signal regardless of which page is current; waiting for it to both exist and be hittable
    /// (not just exist) is what catches the hand-off still intercepting touches.
    private func waitForWelcomeReady(timeout: TimeInterval = 10) {
        let cta = element(identifier: "welcome_log_in")
        let isReady = NSPredicate(format: "exists == true AND isHittable == true")
        let expectation = XCTNSPredicateExpectation(predicate: isReady, object: cta)
        let result = XCTWaiter().wait(for: [expectation], timeout: timeout)
        XCTAssertEqual(result, .completed, "Welcome was not interactive (launch hand-off never finished) within \(timeout)s")
    }

    private var trainTab: XCUIElement { element(label: "Train") }

    private func element(identifier: String) -> XCUIElement {
        app.descendants(matching: .any).matching(identifier: identifier).firstMatch
    }

    private func element(label: String) -> XCUIElement {
        app.descendants(matching: .any)
            .matching(NSPredicate(format: "label == %@", label))
            .firstMatch
    }

    private func attach(_ name: String) {
        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = name
        screenshot.lifetime = .keepAlways
        add(screenshot)
    }
}
