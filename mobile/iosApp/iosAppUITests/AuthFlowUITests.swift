import XCTest

/// End-to-end auth flow against the local API (REP-30 §3.9, §6.3): Welcome -> Register/Login ->
/// signed-in shell, plus the Keychain-restore check from REP-25 Q6, and the REP-87 Profile tab +
/// log-out walk (§3.10 W1).
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

    /// REP-87 W1: register -> Profile tab shows the account header -> Log out dialog cancels
    /// without side effects -> confirming it returns to Welcome at once -> relaunch stays on
    /// Welcome (Keychain cleared, REP-87 §3.1) -> logging back in with the same credentials lands
    /// on Train (ADR-0025, Q11).
    func testLogOutThenLogBackIn() throws {
        let password = "correct-horse-battery"
        app.launch()
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
    }

    /// REP-87 W1 / I3: the same Profile + logout dialog walk in Polish (board 13, Q8 copy),
    /// launched with the Polish locale so `-AppleLanguages`/`-AppleLocale` select the generated
    /// `values-pl` resources (ADR-0017).
    func testProfileInPolish() throws {
        app.launchArguments += ["-AppleLanguages", "(pl)", "-AppleLocale", "pl_PL"]
        app.launch()
        attach("welcome-page-1-pl")

        registerNewAccount()

        let profileTab = element(label: "Profil")
        XCTAssertTrue(profileTab.waitForExistence(timeout: 5), "Profil nav item not found")
        profileTab.tap()
        attach("profile-pl")

        element(identifier: "profile_log_out").tap()
        XCTAssertTrue(element(identifier: "log_out_confirm").waitForExistence(timeout: 5), "log_out_confirm not found")
        attach("logout-dialog-pl")
    }

    /// I5: a never-registered email on a fresh install (Spotter uninstalls the app first; this
    /// test does not, since logging out to reset state mid-suite is not possible yet).
    func testWrongPasswordShowsError() throws {
        app.launch()
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
    /// showing.
    @discardableResult
    private func registerNewAccount(
        displayName: String = "UI Test",
        password: String = "correct-horse-battery"
    ) -> String {
        let email = "uitest+\(Int(Date().timeIntervalSince1970))@repz.app"

        // 3-page Welcome pager (board 07): two swipes reach the last page, where Create account /
        // Log in sit alongside Skip.
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

        element(identifier: "register_submit").tap()

        XCTAssertTrue(trainTab.waitForExistence(timeout: 10), "Did not land on the signed-in shell after registering")
        return email
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
