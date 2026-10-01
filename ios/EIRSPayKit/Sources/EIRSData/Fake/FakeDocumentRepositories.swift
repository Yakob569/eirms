import Foundation
import EIRSDomain

public final class FakeInvoiceRepository: InvoiceRepository, @unchecked Sendable {
    private let store = AsyncStore(SampleData.invoices())
    private let counter = AsyncStore(0)
    public init() {}

    public func observeInvoices() -> AsyncStream<[Invoice]> { store.stream() }

    public func register(_ draft: InvoiceDraft) async throws -> Invoice {
        await FakeLatency.wait()
        counter.update { $0 += 1 }
        let n = counter.value
        let suffix = String(Int(Date().timeIntervalSince1970), radix: 36).suffix(4).uppercased()
        let id = "\(draft.documentType.rawValue)-NEW\(n)X\(suffix)"
        let invoice = Invoice(
            id: id, number: String(104 + n), documentType: draft.documentType, transactionType: draft.transactionType,
            status: .issued, issuedAt: Date(), branch: draft.branch,
            sourceSystem: SourceSystem(type: "POS", number: SampleData.systemFor[draft.branch] ?? ""),
            seller: SampleData.seller, buyer: draft.buyer, paymentMode: draft.paymentMode, paymentTerm: draft.paymentTerm,
            items: draft.items, irn: FakeIrn.of(id), amendment: draft.amendment)
        store.update { $0.insert(invoice, at: 0) }
        return invoice
    }

    public func cancel(invoiceId: String, reason: CancelReason) async throws -> Invoice {
        await FakeLatency.wait()
        store.update { list in
            guard let index = list.firstIndex(where: { $0.id == invoiceId }), !list[index].isCanceled else { return }
            let original = list[index].status
            list[index].status = .canceled
            list[index].cancellation = Cancellation(reason: reason, canceledAt: Date(), originalStatus: original)
        }
        guard let result = store.value.first(where: { $0.id == invoiceId }) else { throw RepositoryError.notFound }
        return result
    }
}

public enum RepositoryError: Error, Equatable { case notFound, invalid(String) }

public final class FakeReceiptRepository: ReceiptRepository, @unchecked Sendable {
    private let store = AsyncStore(SampleData.receipts())
    private let counter = AsyncStore(0)
    public init() {}

    public func observeReceipts() -> AsyncStream<[Receipt]> { store.stream() }

    public func register(_ draft: ReceiptDraft) async throws -> Receipt {
        await FakeLatency.wait()
        counter.update { $0 += 1 }
        let n = counter.value
        let suffix = String(Int(Date().timeIntervalSince1970), radix: 36).suffix(4).uppercased()
        let id = "RCP-N\(n)\(suffix)"
        let receipt = Receipt(
            id: id, number: String(2291 + n), type: draft.type, method: draft.method, issuedAt: Date(), branch: draft.branch,
            sourceSystem: SourceSystem(type: "POS", number: SampleData.systemFor[draft.branch] ?? ""), irn: FakeIrn.of(id),
            buyer: draft.buyer, items: draft.items, reference: draft.reference, provider: draft.provider,
            tendered: draft.tendered, linkedInvoiceId: draft.linkedInvoiceId, amount: draft.amount)
        store.update { $0.insert(receipt, at: 0) }
        return receipt
    }
}

public final class FakeCatalogRepository: CatalogRepository, @unchecked Sendable {
    private let store = AsyncStore(SampleData.catalog())
    public init() {}

    public func observeItems() -> AsyncStream<[CatalogItem]> { store.stream() }

    public func save(_ item: CatalogItem, isNew: Bool) async {
        store.update { list in
            if isNew { list.insert(item, at: 0) }
            else if let index = list.firstIndex(where: { $0.code == item.code }) { list[index] = item }
        }
    }

    public func setActive(code: String, active: Bool) async {
        store.update { list in
            if let index = list.firstIndex(where: { $0.code == code }) { list[index].active = active }
        }
    }
}

public final class FakeWithholdingRepository: WithholdingRepository, @unchecked Sendable {
    private let store = AsyncStore(SampleData.withholding())
    public init() {}

    public func observeRecords() -> AsyncStream<[WithholdingRecord]> { store.stream() }

    public func markCertificateReceived(id: String) async {
        store.update { list in
            if let index = list.firstIndex(where: { $0.id == id }) { list[index].certificateReceived = true }
        }
    }
}

public final class FakeTaxpayerRepository: TaxpayerRepository, @unchecked Sendable {
    public init() {}

    public func lookup(tin: String) async -> Party? {
        await FakeLatency.wait()
        if let known = SampleData.taxpayers.first(where: { $0.tin == tin }) { return known }
        var guess = SampleData.dashen
        guess.tin = tin
        return guess
    }
}

public final class FakeMerchantRepository: MerchantRepository, @unchecked Sendable {
    public init() {}
    public var branches: [BranchInfo] { SampleData.branches }
    public func setupStepsDone() async -> Int { 5 }
    public func seller() -> Party { SampleData.seller }
}

public final class InMemoryPreferencesRepository: PreferencesRepository, @unchecked Sendable {
    private let branch = AsyncStore<Branch?>(nil)
    private let onboarding = AsyncStore(false)
    public init() {}

    public var selectedBranch: Branch? { branch.value }
    public func selectBranch(_ branch: Branch?) { self.branch.set(branch) }
    public func observeSelectedBranch() -> AsyncStream<Branch?> { branch.stream() }
    public var onboardingCompleted: Bool { onboarding.value }
    public func completeOnboarding() { onboarding.set(true) }
}
