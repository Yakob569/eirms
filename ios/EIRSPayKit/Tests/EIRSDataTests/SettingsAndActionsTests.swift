import XCTest
@testable import EIRSData
import EIRSDomain

final class SettingsAndActionsTests: XCTestCase {
    override func setUp() { FakeLatency.milliseconds = 0 }

    private func first<T: Sendable>(_ stream: AsyncStream<T>) async -> T? {
        for await value in stream { return value }
        return nil
    }

    /// First value of a stream, failing the test if it finished empty. (`await` can't sit inside
    /// XCTUnwrap's autoclosure.)
    private func value<T: Sendable>(_ stream: AsyncStream<T>) async throws -> T {
        let got = await first(stream)
        return try XCTUnwrap(got)
    }

    // MARK: Row actions

    func testInvoiceRowActionsFollowDocumentFamily() async throws {
        let all = try await value(FakeInvoiceRepository().observeInvoices())
        func actions(_ number: String) throws -> [InvoiceRowAction] {
            RowActions.forInvoice(try XCTUnwrap(all.first { $0.number == number }), among: all)
        }
        // INV-100 already has CRE-098, so only the debit note is offered.
        XCTAssertEqual(try actions("100"), [.viewDetails, .makeDebitNote, .cancel])
        XCTAssertEqual(try actions("104"), [.viewDetails, .makeCreditNote, .makeDebitNote, .cancel])
        XCTAssertEqual(try actions("098"), [.viewDetails])               // CRE
        XCTAssertEqual(try actions("103"), [.viewDetails])               // canceled
        XCTAssertEqual(try actions("095"), [.viewDetails, .makeInterim, .makeFinal])  // RTN
    }

    func testReceiptLinkedInvoiceActionOnlyWhenLinked() async throws {
        let views = ObserveReceiptViewsUseCase(receipts: FakeReceiptRepository(), invoices: FakeInvoiceRepository(),
                                               withholding: FakeWithholdingRepository())
        let list = try await value(views())
        let linked = try XCTUnwrap(list.first { $0.receipt.number == "2289" })
        let walkIn = try XCTUnwrap(list.first { $0.receipt.number == "2291" })
        XCTAssertEqual(RowActions.forReceipt(linked), [.viewDetails, .viewLinkedInvoice("INV-B3N5M7Q9WE")])
        XCTAssertEqual(RowActions.forReceipt(walkIn), [.viewDetails])
    }

    // MARK: Credit / debit notes

    func testCreditNoteIsCappedAndKeepsBuyer() async throws {
        let repo = FakeInvoiceRepository()
        let all = try await value(repo.observeInvoices())
        let original = try XCTUnwrap(all.first { $0.number == "104" })
        let register = RegisterAdjustmentNoteUseCase(invoices: repo)

        let tooMuch = [0: LineAdjustment(quantity: 2, unitPrice: money("23213"))]
        XCTAssertEqual(AdjustmentNote.issues(kind: .cre, original: original, adjustments: tooMuch, reason: "x"), [.exceedsOriginal(line: 0)])
        XCTAssertEqual(AdjustmentNote.issues(kind: .deb, original: original, adjustments: tooMuch, reason: "x"), [])
        XCTAssertEqual(AdjustmentNote.issues(kind: .cre, original: original, adjustments: [:], reason: ""), [.noLines, .missingReason])
        do {
            _ = try await register(kind: .cre, original: original, adjustments: tooMuch, reason: "Returned")
            XCTFail("expected a rule error")
        } catch let error as DocumentRuleError {
            XCTAssertEqual(error, .adjustment([.exceedsOriginal(line: 0)]))
        }

        let note = try await register(kind: .cre, original: original,
                                      adjustments: [0: LineAdjustment(quantity: 1, unitPrice: money("1000"))], reason: "Price agreed")
        XCTAssertEqual(note.documentType, .cre)
        XCTAssertEqual(note.buyer, original.buyer)
        XCTAssertEqual(note.amendment?.previousInvoiceId, original.id)
        XCTAssertEqual(note.totals.grandTotal, money("1150"))
        let after = try await value(repo.observeInvoices())
        XCTAssertFalse(RowActions.forInvoice(original, among: after).contains(.makeCreditNote))
        XCTAssertTrue(RowActions.forInvoice(original, among: after).contains(.makeDebitNote))
    }

    // MARK: Interim / final

    func testInterimInvoiceAgainstRetainer() async throws {
        let repo = FakeInvoiceRepository()
        let all = try await value(repo.observeInvoices())
        let retainer = try XCTUnwrap(all.first { $0.documentType == .rtn })
        let register = RegisterRetainerInvoiceUseCase(invoices: repo)
        let item = LineItem(description: "Support, October", itemCode: "SRV-0200", nature: .service, unit: "JOB",
                            quantity: 1, unitPrice: money("15000"), taxCode: .vat15)
        XCTAssertEqual(RetainerInvoice.issues(kind: .int, retainer: retainer, buyer: Party(name: " "), items: []),
                       [.missingBuyerName, .noPricedLines])
        XCTAssertEqual(RetainerInvoice.issues(kind: .cre, retainer: retainer, buyer: retainer.buyer, items: [item]), [.invalidSource])

        let interim = try await register(kind: .int, retainer: retainer, buyer: Party(name: "Abyssinia Consulting"),
                                         paymentMode: .bankTransfer, paymentTerm: .net30, items: [item])
        XCTAssertEqual(interim.documentType, .int)
        XCTAssertEqual(interim.transactionType, retainer.transactionType)
        XCTAssertEqual(interim.amendment?.previousInvoiceId, retainer.id)
        XCTAssertEqual(interim.buyer.name, "Abyssinia Consulting")
        XCTAssertEqual(interim.totals.grandTotal, money("17250"))
    }

    // MARK: Settings

    func testDerivedExciseAndKeyCeilings() async throws {
        let repo = FakeSettingsRepository()
        var s = await repo.snapshot()
        let s1 = try XCTUnwrap(s.sourceSystem("s1"))
        XCTAssertEqual(SettingsRules.effectiveTaxTypes(s1, excise: false), [.vat0, .vat15])
        await SetMerchantExciseUseCase(settings: repo)(true)
        s = await repo.snapshot()
        XCTAssertEqual(SettingsRules.effectiveTaxTypes(s1, excise: s.merchantExcise), [.vat0, .vat15, .excise])
        XCTAssertEqual(SettingsRules.eligibleTaxTypes(branchId: "b1", in: s), [.vat0, .vat15, .vatex, .excise])

        // Piassa Till 2 sits on Piassa: B2C/B2B only, three operations, VAT15 (+ EXCISE now).
        let keys = ManageApiKeysUseCase(settings: repo)
        let created = try await keys.create(ApiKeyDraft(
            label: "  ", sourceSystemId: "s3", operations: Set(ApiOperation.allCases),
            transactionTypes: [.b2c, .b2g], taxTypes: [.vat0, .vat15, .excise], users: []))
        XCTAssertEqual(created.key.label, "Untitled key")
        XCTAssertEqual(created.key.operations, [.singleRegister, .singleCancel, .receiptIssue])
        XCTAssertEqual(created.key.transactionTypes, [.b2c])
        XCTAssertEqual(created.key.taxTypes, [.vat15, .excise])
        XCTAssertTrue(created.secret.hasPrefix(ApiKey.secretPrefix))
        XCTAssertEqual(created.key.tail, String(created.secret.suffix(4)))
        XCTAssertFalse(created.key.masked.contains(created.secret))

        try await keys.revoke(id: created.key.id)
        let revoked = await repo.snapshot().apiKey(created.key.id)
        XCTAssertEqual(revoked?.active, false)   // deactivated, not removed

        // Inactive source systems can't take new keys.
        do {
            _ = try await keys.create(ApiKeyDraft(label: "x", sourceSystemId: "s4", operations: [], transactionTypes: [], taxTypes: [], users: []))
            XCTFail("expected blocked")
        } catch let error as SettingsError {
            if case .blocked = error {} else { XCTFail("unexpected \(error)") }
        }
    }

    func testDeleteRulesAndVerification() async throws {
        let repo = FakeSettingsRepository()
        let branches = ManageBranchesUseCase(settings: repo)
        let systems = ManageSourceSystemsUseCase(settings: repo)
        do { try await branches.delete(id: "b1"); XCTFail("main branch") } catch let error as SettingsError {
            XCTAssertEqual(error, .blocked("The main branch can\u{2019}t be deleted."))
        }
        do { try await systems.delete(id: "s2"); XCTFail("has keys") } catch let error as SettingsError {
            XCTAssertEqual(error, .blocked("Remove its 2 API keys first."))
        }
        let fresh = await branches.add()
        try await branches.delete(id: fresh.id)
        let count = await repo.snapshot().branches.count
        XCTAssertEqual(count, 3)

        let verified = try await systems.verify(id: "s3", hsn: "")
        XCTAssertEqual(verified.verification, .verified)
        XCTAssertEqual(verified.verificationNote, "Verified just now · HSN 8471")

        let ok = try await RunSetupVerificationUseCase(settings: repo, merchant: FakeMerchantRepository())()
        XCTAssertTrue(ok)
        let state = await repo.snapshot().setupVerified
        XCTAssertTrue(state)
    }
}
