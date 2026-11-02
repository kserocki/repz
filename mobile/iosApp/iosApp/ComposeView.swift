import RepzShared
import SwiftUI
import UIKit

/// The user-default key the debug design-system catalog reads its page from. Set it with a launch
/// argument, e.g. `-RepzCatalogPage colors`, which `NSUserDefaults`/`NSArgumentDomain` exposes
/// under this key.
private let catalogPageKeyDefaultsKey = "RepzCatalogPage"

/// Wraps the shared Compose UI (`mainViewController(catalogPageKey:)` in `app:shared` iosMain) as
/// a SwiftUI view.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.mainViewController(catalogPageKey: debugCatalogPageKey())
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
}
