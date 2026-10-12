import Foundation

// MARK: - MoR credentials and seller profile

public enum MorEnvironment: String, CaseIterable, Codable, Hashable, Sendable, Identifiable {
    case dev = "DEV", prod = "PROD"
    public var id: String { rawValue }
    public var note: String {
        self == .dev ? "Test documents only. Nothing is filed with MoR." : "Live. Every document is filed with MoR."
    }
}

/// The merchant's EIRMS client credentials, used to sign every request to MoR.
public struct MorCredentials: Hashable, Sendable {
    public var clientId: String
    public var tin: String
    public var clientSecret: String
    public var apiKey: String
    public var environment: MorEnvironment

    public init(clientId: String, tin: String, clientSecret: String, apiKey: String, environment: MorEnvironment) {
        self.clientId = clientId; self.tin = tin; self.clientSecret = clientSecret; self.apiKey = apiKey
        self.environment = environment
    }
}

/// Legal entity printed on every invoice and receipt.
public struct SellerProfile: Hashable, Sendable {
    public var tin: String
    public var legalName: String
    public var vatNumber: String
    public var email: String
    public var phone: String
    public var region: String
    public var city: String
    public var wereda: String
    public var subCity: String
    public var zone: String
    public var kebele: String
    public var locality: String
    public var houseNumber: String
    public var country: String

    public init(tin: String, legalName: String, vatNumber: String, email: String, phone: String, region: String,
                city: String, wereda: String, subCity: String, zone: String, kebele: String, locality: String,
                houseNumber: String, country: String) {
        self.tin = tin; self.legalName = legalName; self.vatNumber = vatNumber; self.email = email; self.phone = phone
        self.region = region; self.city = city; self.wereda = wereda; self.subCity = subCity; self.zone = zone
        self.kebele = kebele; self.locality = locality; self.houseNumber = houseNumber; self.country = country
    }
}

// MARK: - Capability vocabularies (shared by branches, source systems and API keys)

/// Transaction types a branch or key can be limited to (G2C is not configurable per branch).
public let configurableTransactionTypes: [TransactionType] = [.b2c, .b2b, .b2g, .g2b]

public enum IssuableDocumentType: String, CaseIterable, Codable, Hashable, Sendable, Identifiable {
    case inv = "INV", cre = "CRE", deb = "DEB", rct = "RCT", wht = "WHT"
    public var id: String { rawValue }
}

public enum SupplyNature: String, CaseIterable, Codable, Hashable, Sendable, Identifiable {
    case goods = "GOODS", service = "SERVICE"
    public var id: String { rawValue }
}

public enum ApiOperation: String, CaseIterable, Codable, Hashable, Sendable, Identifiable {
    case singleRegister = "Single Register", bulkRegister = "Bulk Register", singleCancel = "Single Cancel"
    case bulkCancel = "Bulk Cancel", receiptIssue = "Receipt Issue", withholdingIssue = "Withholding Issue"
    public var id: String { rawValue }
}

public enum BranchPermission: String, CaseIterable, Codable, Hashable, Sendable, Identifiable {
    case issueInvoices = "Issue invoices", cancelInvoices = "Cancel invoices", issueReceipts = "Issue receipts"
    case issueWithholding = "Issue withholding", viewReports = "View reports", manageItems = "Manage items"
    public var id: String { rawValue }
}

public enum SourceSystemType: String, CaseIterable, Codable, Hashable, Sendable, Identifiable {
    case pos = "POS", erp = "ERP", ecommerce = "E-COMMERCE", manual = "MANUAL"
    public var id: String { rawValue }
}

/// Tax types a source system or key may use. EXCISE is never chosen per system: it is derived from the
/// merchant-wide excise switch.
public enum SettingsTaxType: String, CaseIterable, Codable, Hashable, Sendable, Identifiable {
    case vat0 = "VAT0", vat15 = "VAT15", vatex = "VATEX", excise = "EXCISE"
    public var id: String { rawValue }
    /// The types a source system picks itself.
    public static let selectable: [SettingsTaxType] = [.vat0, .vat15, .vatex]
}

public enum VerificationStatus: String, CaseIterable, Codable, Hashable, Sendable {
    case verified = "Verified", failed = "Failed", unverified = "Unverified"
}

// MARK: - Branch → source system → API key

public struct PostalAddress: Hashable, Sendable {
    public var city: String
    public var subCity: String
    public var wereda: String
    public var kebele: String
    public var houseNumber: String
    public var locality: String
    public var zone: String
    public var region: String
    public var country: String

    public init(city: String = "", subCity: String = "", wereda: String = "", kebele: String = "", houseNumber: String = "",
                locality: String = "", zone: String = "", region: String = "", country: String = "Ethiopia") {
        self.city = city; self.subCity = subCity; self.wereda = wereda; self.kebele = kebele; self.houseNumber = houseNumber
        self.locality = locality; self.zone = zone; self.region = region; self.country = country
    }
}

/// A merchant branch and the business rules that cap everything below it.
public struct BranchProfile: Hashable, Sendable, Identifiable {
    public let id: String
    public var name: String
    public var code: String
    public var isMain: Bool
    public var active: Bool
    public var manager: String?
    public var address: PostalAddress
    public var transactionTypes: Set<TransactionType>
    public var documentTypes: Set<IssuableDocumentType>
    public var natures: Set<SupplyNature>
    public var operations: Set<ApiOperation>
    public var permissions: Set<BranchPermission>

    public init(id: String, name: String, code: String, isMain: Bool, active: Bool, manager: String?, address: PostalAddress,
                transactionTypes: Set<TransactionType>, documentTypes: Set<IssuableDocumentType>, natures: Set<SupplyNature>,
                operations: Set<ApiOperation>, permissions: Set<BranchPermission>) {
        self.id = id; self.name = name; self.code = code; self.isMain = isMain; self.active = active; self.manager = manager
        self.address = address; self.transactionTypes = transactionTypes; self.documentTypes = documentTypes
        self.natures = natures; self.operations = operations; self.permissions = permissions
    }
}

/// A POS, ERP or storefront registered with MoR. Belongs to exactly one branch.
public struct SourceSystemProfile: Hashable, Sendable, Identifiable {
    public let id: String
    public var label: String
    public var type: SourceSystemType
    public var branchId: String
    public var active: Bool
    public var verification: VerificationStatus
    public var verificationNote: String
    /// Chosen tax types (never contains EXCISE; see `SettingsRules.effectiveTaxTypes`).
    public var taxTypes: Set<SettingsTaxType>
    public var cashier: String
    public var salesPerson: String
    public var systemId: String
    public var lastDocument: String

    public init(id: String, label: String, type: SourceSystemType, branchId: String, active: Bool,
                verification: VerificationStatus, verificationNote: String, taxTypes: Set<SettingsTaxType>,
                cashier: String, salesPerson: String, systemId: String, lastDocument: String) {
        self.id = id; self.label = label; self.type = type; self.branchId = branchId; self.active = active
        self.verification = verification; self.verificationNote = verificationNote; self.taxTypes = taxTypes
        self.cashier = cashier; self.salesPerson = salesPerson; self.systemId = systemId; self.lastDocument = lastDocument
    }
}

/// An integration key bound to one source system. Revoking deactivates it; keys are never removed.
public struct ApiKey: Hashable, Sendable, Identifiable {
    public let id: String
    public var label: String
    public var sourceSystemId: String
    public var active: Bool
    public var created: String
    public var operations: Set<ApiOperation>
    public var transactionTypes: Set<TransactionType>
    public var taxTypes: Set<SettingsTaxType>
    /// Last four characters of the secret; the full key is only shown once, at creation.
    public var tail: String
    public var users: [String]

    public init(id: String, label: String, sourceSystemId: String, active: Bool, created: String,
                operations: Set<ApiOperation>, transactionTypes: Set<TransactionType>, taxTypes: Set<SettingsTaxType>,
                tail: String, users: [String]) {
        self.id = id; self.label = label; self.sourceSystemId = sourceSystemId; self.active = active; self.created = created
        self.operations = operations; self.transactionTypes = transactionTypes; self.taxTypes = taxTypes
        self.tail = tail; self.users = users
    }

    public static let secretPrefix = "eirs_live_"
    public var masked: String { "\(Self.secretPrefix)••••••••\(tail)" }
}

/// What the "New key" form collects before the key exists.
public struct ApiKeyDraft: Hashable, Sendable {
    public var label: String
    public var sourceSystemId: String
    public var operations: Set<ApiOperation>
    public var transactionTypes: Set<TransactionType>
    public var taxTypes: Set<SettingsTaxType>
    public var users: [String]

    public init(label: String, sourceSystemId: String, operations: Set<ApiOperation>, transactionTypes: Set<TransactionType>,
                taxTypes: Set<SettingsTaxType>, users: [String]) {
        self.label = label; self.sourceSystemId = sourceSystemId; self.operations = operations
        self.transactionTypes = transactionTypes; self.taxTypes = taxTypes; self.users = users
    }
}

/// A freshly created key plus its secret. The secret is never stored or returned again.
public struct CreatedApiKey: Hashable, Sendable {
    public let key: ApiKey
    public let secret: String
    public init(key: ApiKey, secret: String) { self.key = key; self.secret = secret }
}

// MARK: - Account security

public struct DeviceSession: Hashable, Sendable, Identifiable {
    public let id: String
    public var device: String
    public var fingerprint: String
    public var location: String
    public var lastActive: String
    public var isCurrent: Bool

    public init(id: String, device: String, fingerprint: String, location: String, lastActive: String, isCurrent: Bool) {
        self.id = id; self.device = device; self.fingerprint = fingerprint; self.location = location
        self.lastActive = lastActive; self.isCurrent = isCurrent
    }
}

public enum SecurityEventKind: String, Hashable, Sendable { case signIn, failedAttempt, signOut, passwordReset }

public struct SecurityEvent: Hashable, Sendable, Identifiable {
    public let id: String
    public var kind: SecurityEventKind
    public var title: String
    public var detail: String
    public var when: String

    public init(id: String, kind: SecurityEventKind, title: String, detail: String, when: String) {
        self.id = id; self.kind = kind; self.title = title; self.detail = detail; self.when = when
    }
}

// MARK: - Aggregate

/// Everything under Settings, as one observable snapshot.
public struct OrgSettings: Hashable, Sendable {
    public var staff: [String]
    public var branches: [BranchProfile]
    public var sourceSystems: [SourceSystemProfile]
    public var apiKeys: [ApiKey]
    public var sessions: [DeviceSession]
    public var activity: [SecurityEvent]
    /// Business-wide excise status; when on, EXCISE joins every source system's tax types.
    public var merchantExcise: Bool
    public var credentials: MorCredentials
    public var seller: SellerProfile
    /// Whether the signed MoR probe ("Verify setup") has passed.
    public var setupVerified: Bool
    public var passwordChangedNote: String

    public init(staff: [String], branches: [BranchProfile], sourceSystems: [SourceSystemProfile], apiKeys: [ApiKey],
                sessions: [DeviceSession], activity: [SecurityEvent], merchantExcise: Bool, credentials: MorCredentials,
                seller: SellerProfile, setupVerified: Bool, passwordChangedNote: String) {
        self.staff = staff; self.branches = branches; self.sourceSystems = sourceSystems; self.apiKeys = apiKeys
        self.sessions = sessions; self.activity = activity; self.merchantExcise = merchantExcise
        self.credentials = credentials; self.seller = seller; self.setupVerified = setupVerified
        self.passwordChangedNote = passwordChangedNote
    }

    public func branch(_ id: String) -> BranchProfile? { branches.first { $0.id == id } }
    public func sourceSystem(_ id: String) -> SourceSystemProfile? { sourceSystems.first { $0.id == id } }
    public func apiKey(_ id: String) -> ApiKey? { apiKeys.first { $0.id == id } }
    public func systems(inBranch id: String) -> [SourceSystemProfile] { sourceSystems.filter { $0.branchId == id } }
    public func keys(forSystem id: String) -> [ApiKey] { apiKeys.filter { $0.sourceSystemId == id } }
}

// MARK: - Rules

/// The capability ceilings an API key inherits from its source system and that system's branch.
public struct KeyCeiling: Hashable, Sendable {
    public let operations: Set<ApiOperation>
    public let transactionTypes: Set<TransactionType>
    public let taxTypes: Set<SettingsTaxType>
    public let branchName: String
    public let systemLabel: String
}

/// Pure rules shared by every settings screen (and enforced again by the use cases).
public enum SettingsRules {
    /// A source system's tax types plus EXCISE when merchant excise is on, in display order.
    public static func effectiveTaxTypes(_ system: SourceSystemProfile, excise: Bool) -> [SettingsTaxType] {
        var list = SettingsTaxType.selectable.filter { system.taxTypes.contains($0) }
        if excise { list.append(.excise) }
        return list
    }

    /// Tax types a branch is eligible for: the union of its source systems' effective types (read-only).
    public static func eligibleTaxTypes(branchId: String, in settings: OrgSettings) -> [SettingsTaxType] {
        let all = Set(settings.systems(inBranch: branchId).flatMap { effectiveTaxTypes($0, excise: settings.merchantExcise) })
        return SettingsTaxType.allCases.filter { all.contains($0) }
    }

    public static func keyCeiling(sourceSystemId: String, in settings: OrgSettings) -> KeyCeiling? {
        guard let system = settings.sourceSystem(sourceSystemId), let branch = settings.branch(system.branchId) else { return nil }
        return KeyCeiling(operations: branch.operations, transactionTypes: branch.transactionTypes,
                          taxTypes: Set(effectiveTaxTypes(system, excise: settings.merchantExcise)),
                          branchName: branch.name, systemLabel: system.label)
    }

    /// Drops anything the bound source system or its branch doesn't allow.
    public static func clamp(_ draft: ApiKeyDraft, to ceiling: KeyCeiling) -> ApiKeyDraft {
        var out = draft
        out.operations = draft.operations.intersection(ceiling.operations)
        out.transactionTypes = draft.transactionTypes.intersection(ceiling.transactionTypes)
        out.taxTypes = draft.taxTypes.intersection(ceiling.taxTypes)
        return out
    }

    /// Why a branch can't be deleted right now, or nil when it can.
    public static func branchDeleteBlocker(_ branch: BranchProfile, in settings: OrgSettings) -> String? {
        if branch.isMain { return "The main branch can\u{2019}t be deleted." }
        let count = settings.systems(inBranch: branch.id).count
        if count > 0 { return "Move or delete its \(count) source system\(count == 1 ? "" : "s") first." }
        return nil
    }

    /// Why a source system can't be deleted right now, or nil when it can.
    public static func sourceSystemDeleteBlocker(_ system: SourceSystemProfile, in settings: OrgSettings) -> String? {
        let count = settings.keys(forSystem: system.id).count
        if count > 0 { return "Remove its \(count) API key\(count == 1 ? "" : "s") first." }
        return nil
    }

    /// Inactive branches can't take source systems (a system already there may stay).
    public static func canAssign(_ system: SourceSystemProfile, to branch: BranchProfile) -> Bool {
        branch.active || system.branchId == branch.id
    }

    /// The probe can run once credentials, seller profile and a source system are in (steps 1–3), or to re-run.
    public static func canRunVerification(stepsDone: Int, verified: Bool) -> Bool { stepsDone >= 3 || verified }
    public static func isVerified(stepsDone: Int, verified: Bool) -> Bool { verified || stepsDone >= 5 }
}

public enum SettingsError: Error, Equatable, Sendable {
    case blocked(String)
    case notFound
    case invalid(String)
}
