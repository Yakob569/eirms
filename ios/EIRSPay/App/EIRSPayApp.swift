import SwiftUI

@main
struct EIRSPayApp: App {
    private let container = AppContainer.live()

    var body: some Scene {
        WindowGroup {
            RootView(container: container)
                .eirsTheme(.teal)
                .preferredColorScheme(.light)
        }
    }
}
