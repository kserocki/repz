import RepzShared
import SwiftUI
import UIKit

/// Wraps the shared Compose UI (`MainViewController()` in `app:shared` iosMain) as a SwiftUI view.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
