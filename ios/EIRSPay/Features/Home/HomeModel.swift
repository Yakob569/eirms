import Foundation
import Observation
import EIRSDomain

/// Dashboard state: range chip, global branch filter, analytics result and the latest activity.
@MainActor
@Observable
final class HomeModel {
    var range: DashboardRange = .days7
    var branch: Branch?
    let branches: [BranchInfo]
    /// Nil until the first analytics result for the current range/branch arrives.
    var analytics: SalesAnalytics?
    private var allRecent: [ActivityItem] = []

    @ObservationIgnored private let container: AppContainer

    init(container: AppContainer) {
        self.container = container
        self.branch = container.preferences.selectedBranch
        self.branches = container.merchant.branches
    }

    /// Latest five activities for the selected branch.
    var recent: [ActivityItem] {
        let filtered: [ActivityItem] = allRecent.filter { item in
            guard let branch else { return true }
            return item.branch == branch
        }
        return Array(filtered.prefix(5))
    }

    /// Changes whenever the analytics query must be re-run (used as the `.task(id:)` key).
    var analyticsKey: String { range.rawValue + "|" + (branch?.rawValue ?? "all") }

    func observeBranch() async {
        for await value in container.preferences.observeSelectedBranch() {
            branch = value
        }
    }

    func observeRecent() async {
        for await items in container.observeRecentActivity(limit: 50) {
            allRecent = items
        }
    }

    func loadAnalytics() async {
        let requestedRange = range
        let requestedBranch = branch
        let result = await container.getSalesAnalytics(range: requestedRange, branch: requestedBranch)
        if Task.isCancelled { return }
        analytics = result
    }

    func selectRange(_ value: DashboardRange) { range = value }

    /// Tapping a branch in "By branch" toggles the global filter.
    func toggleBranch(_ target: Branch) {
        let next: Branch? = (container.preferences.selectedBranch == target) ? nil : target
        container.preferences.selectBranch(next)
        branch = next
    }
}
