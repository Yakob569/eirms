import Foundation
import Observation
import EIRSDomain

/// Shell state: signed-in session, global branch filter and the terminal-start loader.
@MainActor
@Observable
final class MainModel {
    var session: Session?
    var selectedBranch: Branch?
    var loading: LoaderMessage?
    let branches: [BranchInfo]

    @ObservationIgnored private let container: AppContainer

    init(container: AppContainer) {
        self.container = container
        self.selectedBranch = container.preferences.selectedBranch
        self.branches = container.merchant.branches
    }

    func observeSession() async {
        session = await container.auth.currentSession()
        for await value in container.auth.observeSession() {
            session = value
        }
    }

    func observeBranch() async {
        for await value in container.preferences.observeSelectedBranch() {
            selectedBranch = value
        }
    }

    func selectBranch(_ branch: Branch?) {
        container.preferences.selectBranch(branch)
        selectedBranch = branch
    }

    /// Shows the loader for 1.8s, hands the branch to `onStarted`, then clears the loader.
    func startTerminal(_ branch: Branch, onStarted: (Branch) -> Void) async {
        if loading != nil { return }
        loading = LoaderMessage(title: "Starting terminal mode", subtitle: "Locking the app to sales · \(branch.displayName)")
        try? await Task.sleep(nanoseconds: 1_800_000_000)
        onStarted(branch)
        loading = nil
    }

    func signOut() async {
        await container.signOut()
    }
}
