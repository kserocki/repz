import RepzShared
import SwiftUI

/// The SwiftUI entry point. Named `RepzIOSApp` (not `RepzApp`) so it doesn't shadow the exported
/// Koin `RepzApp` object (ADR-0017). Starts the single shared Koin graph once, before any Compose
/// code runs, then hosts the shared UI full-bleed behind both the status bar and the home
/// indicator (Compose owns its own insets).
@main
struct RepzIOSApp: App {
    init() {
        RepzAppKt.startRepzKoin(platformDeclaration: { _ in })
    }

    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea()
        }
    }
}
