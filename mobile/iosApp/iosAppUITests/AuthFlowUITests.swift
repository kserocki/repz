import XCTest

/// End-to-end auth flow against the local API (REP-30 §3.9, §6.3): Welcome -> Register/Login ->
/// signed-in shell, plus the Keychain-restore check from REP-25 Q6.
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
        let email = "uitest+\(Int(Date().timeIntervalSince1970))@repz.app"

        app.launch()
        attach("welcome-page-1")

        // 3-page Welcome pager (board 07): two swipes reach the last page, where Create account /
        // Log in sit alongside Skip.
        app.swipeLeft()
        app.swipeLeft()
        attach("welcome-page-3")

        let createAccount = element(identifier: "welcome_create_account")
        XCTAssertTrue(createAccount.waitForExistence(timeout: 5), "welcome_create_account not found")
        createAccount.tap()

        let displayName = element(identifier: "register_display_name")
        XCTAssertTrue(displayName.waitForExistence(timeout: 5), "register_display_name not found")
        displayName.tap()
        displayName.typeText("UI Test")

        let registerEmail = element(identifier: "register_email")
        registerEmail.tap()
        registerEmail.typeText(email)

        let registerPassword = element(identifier: "register_password")
        registerPassword.tap()
        registerPassword.typeText("correct-horse-battery")

        attach("register-filled")
        element(identifier: "register_submit").tap()

        XCTAssertTrue(trainTab.waitForExistence(timeout: 10), "Did not land on the signed-in shell after registering")
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
