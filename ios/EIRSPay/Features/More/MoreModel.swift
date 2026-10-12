import Foundation
import Observation
import EIRSDomain

/// "More" tab state: counts for the tool rows, fiscal-readiness progress and the session.
@MainActor
@Observable
final class MoreModel {
    var activeItems = 0
    var awaitingCertificates = 0
    /// Completed fiscal-readiness steps, 0...5.
    var setupDone = 0
    let branchCount: Int
    var session: Session?
    var settings: OrgSettings?

    @ObservationIgnored private let container: AppContainer

    init(container: AppContainer) {
        self.container = container
        self.branchCount = container.merchant.branches.count
    }

    func observeItems() async {
        for await items in container.catalog.observeItems() {
            activeItems = items.filter { $0.active }.count
        }
    }

    func observeWithholding() async {
        for await records in container.withholding.observeRecords() {
            awaitingCertificates = records.filter { !$0.certificateReceived }.count
        }
    }

    func observeSession() async {
        for await value in container.auth.observeSession() {
            session = value
        }
    }

    func observeSettings() async {
        for await value in container.observeSettings() { settings = value }
    }

    var verified: Bool { SettingsRules.isVerified(stepsDone: setupDone, verified: settings?.setupVerified ?? false) }

    func loadSetup() async {
        let done = await container.merchant.setupStepsDone()
        setupDone = min(max(done, 0), 5)
    }
}
