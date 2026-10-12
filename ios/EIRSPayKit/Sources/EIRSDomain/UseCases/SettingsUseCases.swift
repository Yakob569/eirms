import Foundation

public struct ObserveSettingsUseCase: Sendable {
    let settings: SettingsRepository
    public init(settings: SettingsRepository) { self.settings = settings }
    public func callAsFunction() -> AsyncStream<OrgSettings> { settings.observeSettings() }
}

/// MoR credentials and seller profile are read-only until Edit; Save stores the whole record.
public struct SaveMerchantProfileUseCase: Sendable {
    let settings: SettingsRepository
    public init(settings: SettingsRepository) { self.settings = settings }
    public func credentials(_ value: MorCredentials) async { await settings.saveCredentials(value) }
    public func seller(_ value: SellerProfile) async { await settings.saveSeller(value) }
}

public struct SetMerchantExciseUseCase: Sendable {
    let settings: SettingsRepository
    public init(settings: SettingsRepository) { self.settings = settings }
    public func callAsFunction(_ enabled: Bool) async { await settings.setMerchantExcise(enabled) }
}

public struct ManageBranchesUseCase: Sendable {
    let settings: SettingsRepository
    public init(settings: SettingsRepository) { self.settings = settings }

    public func add() async -> BranchProfile { await settings.addBranch() }

    /// The main branch is always active.
    public func save(_ branch: BranchProfile) async throws {
        var value = branch
        if value.isMain { value.active = true }
        try await settings.saveBranch(value)
    }

    public func delete(id: String) async throws {
        let snapshot = await settings.snapshot()
        guard let branch = snapshot.branch(id) else { throw SettingsError.notFound }
        if let reason = SettingsRules.branchDeleteBlocker(branch, in: snapshot) { throw SettingsError.blocked(reason) }
        try await settings.deleteBranch(id: id)
    }
}

public struct ManageSourceSystemsUseCase: Sendable {
    let settings: SettingsRepository
    public init(settings: SettingsRepository) { self.settings = settings }

    public func add(branchId: String) async throws -> SourceSystemProfile {
        let snapshot = await settings.snapshot()
        guard let branch = snapshot.branch(branchId) else { throw SettingsError.notFound }
        guard branch.active else { throw SettingsError.blocked("Inactive branches can\u{2019}t take source systems.") }
        return try await settings.addSourceSystem(branchId: branchId)
    }

    /// EXCISE is derived, never stored; a system can't move onto an inactive branch.
    public func save(_ system: SourceSystemProfile) async throws {
        let snapshot = await settings.snapshot()
        var value = system
        value.taxTypes.remove(.excise)
        if let branch = snapshot.branch(value.branchId), let stored = snapshot.sourceSystem(value.id),
           !SettingsRules.canAssign(stored, to: branch) {
            throw SettingsError.blocked("Inactive branches can\u{2019}t take source systems.")
        }
        try await settings.saveSourceSystem(value)
    }

    public func delete(id: String) async throws {
        let snapshot = await settings.snapshot()
        guard let system = snapshot.sourceSystem(id) else { throw SettingsError.notFound }
        if let reason = SettingsRules.sourceSystemDeleteBlocker(system, in: snapshot) { throw SettingsError.blocked(reason) }
        try await settings.deleteSourceSystem(id: id)
    }

    public func verify(id: String, hsn: String) async throws -> SourceSystemProfile {
        let code = hsn.trimmingCharacters(in: .whitespaces)
        return try await settings.verifySourceSystem(id: id, hsn: code.isEmpty ? "8471" : code)
    }
}

public struct ManageApiKeysUseCase: Sendable {
    let settings: SettingsRepository
    public init(settings: SettingsRepository) { self.settings = settings }

    /// Creates a key bound to an active source system, capped at what that system and its branch allow.
    public func create(_ draft: ApiKeyDraft) async throws -> CreatedApiKey {
        let snapshot = await settings.snapshot()
        guard let system = snapshot.sourceSystem(draft.sourceSystemId),
              let ceiling = SettingsRules.keyCeiling(sourceSystemId: draft.sourceSystemId, in: snapshot)
        else { throw SettingsError.notFound }
        guard system.active else { throw SettingsError.blocked("Inactive source systems can\u{2019}t take new keys.") }
        var value = SettingsRules.clamp(draft, to: ceiling)
        let label = value.label.trimmingCharacters(in: .whitespaces)
        value.label = label.isEmpty ? "Untitled key" : label
        return try await settings.createApiKey(value)
    }

    /// Keys stay bound to their source system; capabilities are clamped to its ceiling.
    public func save(_ key: ApiKey) async throws {
        let snapshot = await settings.snapshot()
        guard let stored = snapshot.apiKey(key.id),
              let ceiling = SettingsRules.keyCeiling(sourceSystemId: stored.sourceSystemId, in: snapshot)
        else { throw SettingsError.notFound }
        var value = key
        value.sourceSystemId = stored.sourceSystemId
        value.operations = key.operations.intersection(ceiling.operations)
        value.transactionTypes = key.transactionTypes.intersection(ceiling.transactionTypes)
        value.taxTypes = key.taxTypes.intersection(ceiling.taxTypes)
        try await settings.saveApiKey(value)
    }

    /// Revoking deactivates the key; it is never removed.
    public func revoke(id: String) async throws { try await settings.revokeApiKey(id: id) }
}

public struct ManageSessionsUseCase: Sendable {
    let settings: SettingsRepository
    public init(settings: SettingsRepository) { self.settings = settings }
    public func signOut(id: String) async { await settings.signOutSession(id: id) }
    public func signOutOthers() async { await settings.signOutOtherSessions() }
    public func passwordChanged() async { await settings.recordPasswordChange() }
}

/// Runs the signed MoR probe. Refuses until credentials, seller profile and a source system are done.
public struct RunSetupVerificationUseCase: Sendable {
    let settings: SettingsRepository
    let merchant: MerchantRepository
    public init(settings: SettingsRepository, merchant: MerchantRepository) { self.settings = settings; self.merchant = merchant }

    public func callAsFunction() async throws -> Bool {
        let done = await merchant.setupStepsDone()
        let snapshot = await settings.snapshot()
        guard SettingsRules.canRunVerification(stepsDone: done, verified: snapshot.setupVerified) else {
            throw SettingsError.blocked("Finish MoR credentials, seller profile and a source system first.")
        }
        return try await settings.runSetupVerification()
    }
}
