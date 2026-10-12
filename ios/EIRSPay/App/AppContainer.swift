import Foundation
import EIRSDomain
import EIRSData

/// Composition root: the one place that decides which repository implementations the app uses.
///
/// Everything is an in-memory fake today. When the local backend is ready, add API-backed
/// implementations in `EIRSData` (URLSession + Codable) and swap them in `live()`.
final class AppContainer {
    // Repositories
    let auth: AuthRepository
    let merchant: MerchantRepository
    let invoices: InvoiceRepository
    let receipts: ReceiptRepository
    let catalog: CatalogRepository
    let withholding: WithholdingRepository
    let analytics: AnalyticsRepository
    let taxpayers: TaxpayerRepository
    let preferences: PreferencesRepository
    let settings: SettingsRepository

    // Use cases
    let signIn: SignInUseCase
    let signUp: SignUpUseCase
    let sendVerificationCode: SendVerificationCodeUseCase
    let verifyCode: VerifyCodeUseCase
    let requestPasswordReset: RequestPasswordResetUseCase
    let validateResetToken: ValidateResetTokenUseCase
    let resetPassword: ResetPasswordUseCase
    let changeTemporaryPassword: ChangeTemporaryPasswordUseCase
    let signOut: SignOutUseCase

    let observeInvoices: ObserveInvoicesUseCase
    let registerInvoice: RegisterInvoiceUseCase
    let cancelInvoice: CancelInvoiceUseCase
    let observeReceiptViews: ObserveReceiptViewsUseCase
    let registerReceipt: RegisterReceiptUseCase
    let observeWithholding: ObserveWithholdingUseCase
    let markCertificateReceived: MarkCertificateReceivedUseCase
    let observeCatalog: ObserveCatalogUseCase
    let validateCatalogItem: ValidateCatalogItemUseCase
    let saveCatalogItem: SaveCatalogItemUseCase
    let setCatalogItemActive: SetCatalogItemActiveUseCase
    let lookupTaxpayer: LookupTaxpayerUseCase
    let getSalesAnalytics: GetSalesAnalyticsUseCase
    let observeRecentActivity: ObserveRecentActivityUseCase
    let registerAdjustmentNote: RegisterAdjustmentNoteUseCase
    let registerRetainerInvoice: RegisterRetainerInvoiceUseCase

    // Settings
    let observeSettings: ObserveSettingsUseCase
    let saveMerchantProfile: SaveMerchantProfileUseCase
    let setMerchantExcise: SetMerchantExciseUseCase
    let manageBranches: ManageBranchesUseCase
    let manageSourceSystems: ManageSourceSystemsUseCase
    let manageApiKeys: ManageApiKeysUseCase
    let manageSessions: ManageSessionsUseCase
    let runSetupVerification: RunSetupVerificationUseCase

    init(auth: AuthRepository, merchant: MerchantRepository, invoices: InvoiceRepository, receipts: ReceiptRepository,
         catalog: CatalogRepository, withholding: WithholdingRepository, analytics: AnalyticsRepository,
         taxpayers: TaxpayerRepository, preferences: PreferencesRepository, settings: SettingsRepository) {
        self.auth = auth; self.merchant = merchant; self.invoices = invoices; self.receipts = receipts
        self.catalog = catalog; self.withholding = withholding; self.analytics = analytics
        self.taxpayers = taxpayers; self.preferences = preferences; self.settings = settings

        signIn = SignInUseCase(auth: auth)
        signUp = SignUpUseCase(auth: auth)
        sendVerificationCode = SendVerificationCodeUseCase(auth: auth)
        verifyCode = VerifyCodeUseCase(auth: auth)
        requestPasswordReset = RequestPasswordResetUseCase(auth: auth)
        validateResetToken = ValidateResetTokenUseCase(auth: auth)
        resetPassword = ResetPasswordUseCase(auth: auth)
        changeTemporaryPassword = ChangeTemporaryPasswordUseCase(auth: auth)
        signOut = SignOutUseCase(auth: auth)

        observeInvoices = ObserveInvoicesUseCase(invoices: invoices)
        registerInvoice = RegisterInvoiceUseCase(invoices: invoices)
        cancelInvoice = CancelInvoiceUseCase(invoices: invoices)
        let receiptViews = ObserveReceiptViewsUseCase(receipts: receipts, invoices: invoices, withholding: withholding)
        observeReceiptViews = receiptViews
        registerReceipt = RegisterReceiptUseCase(receipts: receipts)
        observeWithholding = ObserveWithholdingUseCase(withholding: withholding, receiptViews: receiptViews, invoices: invoices)
        markCertificateReceived = MarkCertificateReceivedUseCase(withholding: withholding)
        observeCatalog = ObserveCatalogUseCase(catalog: catalog)
        validateCatalogItem = ValidateCatalogItemUseCase()
        saveCatalogItem = SaveCatalogItemUseCase(catalog: catalog)
        setCatalogItemActive = SetCatalogItemActiveUseCase(catalog: catalog)
        lookupTaxpayer = LookupTaxpayerUseCase(taxpayers: taxpayers)
        getSalesAnalytics = GetSalesAnalyticsUseCase(analytics: analytics)
        observeRecentActivity = ObserveRecentActivityUseCase(invoices: invoices, receipts: receiptViews)
        registerAdjustmentNote = RegisterAdjustmentNoteUseCase(invoices: invoices)
        registerRetainerInvoice = RegisterRetainerInvoiceUseCase(invoices: invoices)

        observeSettings = ObserveSettingsUseCase(settings: settings)
        saveMerchantProfile = SaveMerchantProfileUseCase(settings: settings)
        setMerchantExcise = SetMerchantExciseUseCase(settings: settings)
        manageBranches = ManageBranchesUseCase(settings: settings)
        manageSourceSystems = ManageSourceSystemsUseCase(settings: settings)
        manageApiKeys = ManageApiKeysUseCase(settings: settings)
        manageSessions = ManageSessionsUseCase(settings: settings)
        runSetupVerification = RunSetupVerificationUseCase(settings: settings, merchant: merchant)
    }

    /// Offline build backed by the in-memory sample data.
    static func live() -> AppContainer {
        AppContainer(auth: FakeAuthRepository(), merchant: FakeMerchantRepository(), invoices: FakeInvoiceRepository(),
                     receipts: FakeReceiptRepository(), catalog: FakeCatalogRepository(), withholding: FakeWithholdingRepository(),
                     analytics: FakeAnalyticsRepository(), taxpayers: FakeTaxpayerRepository(), preferences: InMemoryPreferencesRepository(),
                     settings: FakeSettingsRepository())
    }

    /// Same data with no simulated latency, for SwiftUI previews and UI tests.
    static func preview() -> AppContainer {
        FakeLatency.milliseconds = 0
        return live()
    }
}
