import Foundation
import Observation
import EIRSDomain

/// Backs the settings list pages and the simple pages (account, credentials, seller, verify):
/// the live settings snapshot plus the intents those pages raise.
@MainActor @Observable
final class SettingsModel {
    var settings: OrgSettings?
    var session: Session?
    var stepsDone = 0
    var loading: LoaderMessage?

    // MoR credentials / seller profile: read-only until Edit, then edited as a draft until Save.
    var editingCredentials = false
    var credentialsDraft: MorCredentials?
    var showSecrets = false
    var editingSeller = false
    var sellerDraft: SellerProfile?

    // Account
    var passwordOpen = false
    var currentPassword = ""
    var newPassword = ""
    var confirmPassword = ""

    @ObservationIgnored let container: AppContainer

    init(container: AppContainer) { self.container = container }

    func observe() async {
        for await value in container.observeSettings() { settings = value }
    }

    func observeSession() async {
        session = await container.auth.currentSession()
        for await value in container.auth.observeSession() { session = value }
    }

    func loadSteps() async { stepsDone = await container.merchant.setupStepsDone() }

    // MARK: Verification

    var verified: Bool { SettingsRules.isVerified(stepsDone: stepsDone, verified: settings?.setupVerified ?? false) }
    var canVerify: Bool { SettingsRules.canRunVerification(stepsDone: stepsDone, verified: settings?.setupVerified ?? false) }

    func runVerification() async {
        guard canVerify, loading == nil else { return }
        loading = LoaderMessage(title: "Verifying with MoR", subtitle: "Sending the signed probe invoice")
        _ = try? await container.runSetupVerification()
        loading = nil
    }

    // MARK: Credentials / seller

    func toggleCredentialsEdit() {
        if editingCredentials, let draft = credentialsDraft {
            Task { await container.saveMerchantProfile.credentials(draft) }
        } else {
            credentialsDraft = settings?.credentials
        }
        editingCredentials.toggle()
    }

    func toggleSellerEdit() {
        if editingSeller, let draft = sellerDraft {
            Task { await container.saveMerchantProfile.seller(draft) }
        } else {
            sellerDraft = settings?.seller
        }
        editingSeller.toggle()
    }

    // MARK: Source systems list

    func setExcise(_ on: Bool) { Task { await container.setMerchantExcise(on) } }

    // MARK: Account

    var passwordReady: Bool {
        !currentPassword.isEmpty && PasswordPolicy.evaluate(newPassword).acceptable && newPassword == confirmPassword
    }

    func togglePassword() {
        passwordOpen.toggle()
        currentPassword = ""; newPassword = ""; confirmPassword = ""
    }

    func updatePassword() async {
        guard passwordReady else { return }
        await container.manageSessions.passwordChanged()
        togglePassword()
    }

    func signOut(sessionId: String) { Task { await container.manageSessions.signOut(id: sessionId) } }
    func signOutOthers() { Task { await container.manageSessions.signOutOthers() } }

    // MARK: Adding from list headers

    func addBranch() async -> String { await container.manageBranches.add().id }

    func addSourceSystem(branchId: String) async -> String? {
        try? await container.manageSourceSystems.add(branchId: branchId).id
    }

    /// The main branch is the default home for a new source system.
    var defaultBranchId: String? { settings?.branches.first { $0.isMain }?.id ?? settings?.branches.first?.id }
}
