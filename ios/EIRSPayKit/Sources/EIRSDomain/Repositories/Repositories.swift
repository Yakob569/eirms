import Foundation

public protocol AuthRepository: Sendable {
    func currentSession() async -> Session?
    func observeSession() -> AsyncStream<Session?>
    func signIn(identifier: String, password: String) async -> SignInResult
    func signUp(_ request: SignUpRequest) async throws -> PendingAccount
    func pendingAccount() async -> PendingAccount?
    /// Sends a one-time code and returns the masked destination it went to.
    func sendVerificationCode(channel: VerificationChannel) async throws -> String
    func verifyCode(_ code: String) async throws -> Bool
    func requestPasswordReset(email: String) async throws
    /// Validates an emailed reset token; returns the account email it belongs to.
    func validateResetToken(_ token: String) async throws -> String
    func resetPassword(token: String, newPassword: String) async throws
    func changeTemporaryPassword(current: String, newPassword: String) async throws
    func signOut() async
}

public protocol MerchantRepository: Sendable {
    var branches: [BranchInfo] { get }
    /// Completed fiscal-readiness steps, 0...5.
    func setupStepsDone() async -> Int
    func seller() -> Party
}

/// Settings: credentials, seller profile, branches → source systems → API keys, sessions and the MoR probe.
public protocol SettingsRepository: Sendable {
    func observeSettings() -> AsyncStream<OrgSettings>
    func snapshot() async -> OrgSettings
    func saveCredentials(_ credentials: MorCredentials) async
    func saveSeller(_ seller: SellerProfile) async
    func setMerchantExcise(_ enabled: Bool) async
    func addBranch() async -> BranchProfile
    func saveBranch(_ branch: BranchProfile) async throws
    func deleteBranch(id: String) async throws
    func addSourceSystem(branchId: String) async throws -> SourceSystemProfile
    func saveSourceSystem(_ system: SourceSystemProfile) async throws
    func deleteSourceSystem(id: String) async throws
    /// Sends a signed probe for one source system; returns it with its new verification state.
    func verifySourceSystem(id: String, hsn: String) async throws -> SourceSystemProfile
    func createApiKey(_ draft: ApiKeyDraft) async throws -> CreatedApiKey
    func saveApiKey(_ key: ApiKey) async throws
    func revokeApiKey(id: String) async throws
    func signOutSession(id: String) async
    func signOutOtherSessions() async
    func recordPasswordChange() async
    /// Sends the signed probe invoice built from credentials, seller profile and the first source system.
    func runSetupVerification() async throws -> Bool
}

public protocol InvoiceRepository: Sendable {
    func observeInvoices() -> AsyncStream<[Invoice]>
    func register(_ draft: InvoiceDraft) async throws -> Invoice
    func cancel(invoiceId: String, reason: CancelReason) async throws -> Invoice
}

public protocol ReceiptRepository: Sendable {
    func observeReceipts() -> AsyncStream<[Receipt]>
    func register(_ draft: ReceiptDraft) async throws -> Receipt
}

public protocol CatalogRepository: Sendable {
    func observeItems() -> AsyncStream<[CatalogItem]>
    func save(_ item: CatalogItem, isNew: Bool) async
    func setActive(code: String, active: Bool) async
}

public protocol WithholdingRepository: Sendable {
    func observeRecords() -> AsyncStream<[WithholdingRecord]>
    func markCertificateReceived(id: String) async
}

public protocol AnalyticsRepository: Sendable {
    func salesAnalytics(range: DashboardRange, branch: Branch?) async -> SalesAnalytics
}

public protocol TaxpayerRepository: Sendable {
    /// Looks a TIN up in the MoR taxpayer registry.
    func lookup(tin: String) async -> Party?
}

/// UI-wide selections that outlive a single screen (branch filter, onboarding flag).
public protocol PreferencesRepository: Sendable {
    var selectedBranch: Branch? { get }
    func selectBranch(_ branch: Branch?)
    func observeSelectedBranch() -> AsyncStream<Branch?>
    var onboardingCompleted: Bool { get }
    func completeOnboarding()
}

public struct InvoiceDraft: Sendable {
    public var documentType: DocumentType
    public var transactionType: TransactionType
    public var branch: Branch
    public var buyer: Party
    public var paymentMode: InvoicePaymentMode
    public var paymentTerm: PaymentTerm
    public var items: [LineItem]
    public var amendment: Amendment?

    public init(documentType: DocumentType, transactionType: TransactionType, branch: Branch, buyer: Party,
                paymentMode: InvoicePaymentMode, paymentTerm: PaymentTerm, items: [LineItem], amendment: Amendment? = nil) {
        self.documentType = documentType; self.transactionType = transactionType; self.branch = branch
        self.buyer = buyer; self.paymentMode = paymentMode; self.paymentTerm = paymentTerm
        self.items = items; self.amendment = amendment
    }
}

public struct ReceiptDraft: Sendable {
    public var type: ReceiptType
    public var method: PaymentMethod
    public var branch: Branch
    public var buyer: Party?
    public var items: [LineItem]
    public var reference: String?
    public var provider: String?
    public var tendered: Money?
    public var linkedInvoiceId: String?
    public var amount: Money?

    public init(type: ReceiptType, method: PaymentMethod, branch: Branch, buyer: Party?, items: [LineItem],
                reference: String?, provider: String?, tendered: Money?, linkedInvoiceId: String?, amount: Money?) {
        self.type = type; self.method = method; self.branch = branch; self.buyer = buyer; self.items = items
        self.reference = reference; self.provider = provider; self.tendered = tendered
        self.linkedInvoiceId = linkedInvoiceId; self.amount = amount
    }
}
