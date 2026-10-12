import Foundation
import EIRSDomain

/// In-memory settings. Verification calls wait `FakeLatency` and always pass, like the prototype.
public final class FakeSettingsRepository: SettingsRepository, @unchecked Sendable {
    private let store = AsyncStore(SampleData.settings())
    private let counter = AsyncStore(0)
    private let clock: @Sendable () -> Date

    public init(clock: @escaping @Sendable () -> Date = { Date() }) { self.clock = clock }

    public func observeSettings() -> AsyncStream<OrgSettings> { store.stream() }
    public func snapshot() async -> OrgSettings { store.value }

    public func saveCredentials(_ credentials: MorCredentials) async { store.update { $0.credentials = credentials } }
    public func saveSeller(_ seller: SellerProfile) async { store.update { $0.seller = seller } }
    public func setMerchantExcise(_ enabled: Bool) async { store.update { $0.merchantExcise = enabled } }

    private func nextId(_ prefix: String) -> String {
        counter.update { $0 += 1 }
        return "\(prefix)-new\(counter.value)"
    }

    // MARK: Branches

    public func addBranch() async -> BranchProfile {
        let id = nextId("b")
        let count = store.value.branches.count
        let branch = BranchProfile(
            id: id, name: "New branch", code: "BR-00\(count + 1)", isMain: false, active: true, manager: nil,
            address: PostalAddress(), transactionTypes: [.b2c], documentTypes: [.inv, .rct], natures: [.goods],
            operations: [.singleRegister, .receiptIssue], permissions: [.issueInvoices, .issueReceipts])
        store.update { $0.branches.append(branch) }
        return branch
    }

    public func saveBranch(_ branch: BranchProfile) async throws {
        guard store.value.branch(branch.id) != nil else { throw RepositoryError.notFound }
        store.update { s in
            if let index = s.branches.firstIndex(where: { $0.id == branch.id }) { s.branches[index] = branch }
        }
    }

    public func deleteBranch(id: String) async throws {
        guard store.value.branch(id) != nil else { throw RepositoryError.notFound }
        store.update { $0.branches.removeAll { $0.id == id } }
    }

    // MARK: Source systems

    public func addSourceSystem(branchId: String) async throws -> SourceSystemProfile {
        guard store.value.branch(branchId) != nil else { throw RepositoryError.notFound }
        let system = SourceSystemProfile(
            id: nextId("s"), label: "New source system", type: .pos, branchId: branchId, active: true, verification: .unverified,
            verificationNote: "Not verified yet", taxTypes: [.vat15], cashier: "", salesPerson: "",
            systemId: "SS-pending", lastDocument: "—")
        store.update { $0.sourceSystems.append(system) }
        return system
    }

    public func saveSourceSystem(_ system: SourceSystemProfile) async throws {
        guard store.value.sourceSystem(system.id) != nil else { throw RepositoryError.notFound }
        store.update { s in
            if let index = s.sourceSystems.firstIndex(where: { $0.id == system.id }) { s.sourceSystems[index] = system }
        }
    }

    public func deleteSourceSystem(id: String) async throws {
        guard store.value.sourceSystem(id) != nil else { throw RepositoryError.notFound }
        store.update { $0.sourceSystems.removeAll { $0.id == id } }
    }

    public func verifySourceSystem(id: String, hsn: String) async throws -> SourceSystemProfile {
        await FakeLatency.wait()
        store.update { s in
            if let index = s.sourceSystems.firstIndex(where: { $0.id == id }) {
                s.sourceSystems[index].verification = .verified
                s.sourceSystems[index].verificationNote = "Verified just now · HSN \(hsn)"
            }
        }
        guard let system = store.value.sourceSystem(id) else { throw RepositoryError.notFound }
        return system
    }

    // MARK: API keys

    private static func randomHex(_ count: Int) -> String {
        let digits = Array("0123456789abcdef")
        return String((0..<count).map { _ in digits[Int.random(in: 0..<16)] })
    }

    private func today() -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = ethiopiaTimeZone
        formatter.dateFormat = "d MMM yyyy"
        return formatter.string(from: clock())
    }

    public func createApiKey(_ draft: ApiKeyDraft) async throws -> CreatedApiKey {
        guard store.value.sourceSystem(draft.sourceSystemId) != nil else { throw RepositoryError.notFound }
        let secret = ApiKey.secretPrefix + Self.randomHex(32)
        let key = ApiKey(id: nextId("k"), label: draft.label, sourceSystemId: draft.sourceSystemId, active: true,
                         created: today(), operations: draft.operations, transactionTypes: draft.transactionTypes,
                         taxTypes: draft.taxTypes, tail: String(secret.suffix(4)), users: draft.users)
        store.update { $0.apiKeys.insert(key, at: 0) }
        return CreatedApiKey(key: key, secret: secret)
    }

    public func saveApiKey(_ key: ApiKey) async throws {
        guard store.value.apiKey(key.id) != nil else { throw RepositoryError.notFound }
        store.update { s in
            if let index = s.apiKeys.firstIndex(where: { $0.id == key.id }) { s.apiKeys[index] = key }
        }
    }

    public func revokeApiKey(id: String) async throws {
        guard store.value.apiKey(id) != nil else { throw RepositoryError.notFound }
        store.update { s in
            if let index = s.apiKeys.firstIndex(where: { $0.id == id }) { s.apiKeys[index].active = false }
        }
    }

    // MARK: Account

    public func signOutSession(id: String) async { store.update { $0.sessions.removeAll { $0.id == id && !$0.isCurrent } } }
    public func signOutOtherSessions() async { store.update { $0.sessions.removeAll { !$0.isCurrent } } }
    public func recordPasswordChange() async { store.update { $0.passwordChangedNote = "Changed just now" } }

    public func runSetupVerification() async throws -> Bool {
        await FakeLatency.wait()
        store.update { $0.setupVerified = true }
        return true
    }
}
