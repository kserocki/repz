import RepzShared
import SwiftUI
import UIKit

/// The user-default key the debug design-system catalog reads its page from. Set it with a launch
/// argument, e.g. `-RepzCatalogPage colors`, which `NSUserDefaults`/`NSArgumentDomain` exposes
/// under this key.
private let catalogPageKeyDefaultsKey = "RepzCatalogPage"

/// The user-default key the debug "force reduced motion" hook reads from. Set it with a launch
/// argument, e.g. `-RepzReducedMotion YES`.
private let reducedMotionDefaultsKey = "RepzReducedMotion"

/// Wraps the shared Compose UI (`mainViewController(catalogPageKey:catalogReducedMotion:)` in
/// `app:shared` iosMain) as a SwiftUI view.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.mainViewController(
            catalogPageKey: debugCatalogPageKey(),
            catalogReducedMotion: debugCatalogReducedMotion()
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}

    /// The debug-only design-system catalog hook: reads the page key from `UserDefaults` only in
    /// `DEBUG` builds, so a release build never compiles in this lookup and always passes `nil`.
    private func debugCatalogPageKey() -> String? {
        #if DEBUG
        return UserDefaults.standard.string(forKey: catalogPageKeyDefaultsKey)
        #else
        return nil
        #endif
    }

    /// The debug-only "force reduced motion" hook: reads the flag from `UserDefaults` only in
    /// `DEBUG` builds, so a release build never compiles in this lookup and always passes `false`
    /// (`mainViewController` maps that to "follow the live platform setting").
    private func debugCatalogReducedMotion() -> Bool {
        #if DEBUG
        return UserDefaults.standard.bool(forKey: reducedMotionDefaultsKey)
        #else
        return false
        #endif
    }
}
