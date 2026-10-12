import XCTest
@testable import EIRSData
import EIRSDomain

final class RepositoriesTests: XCTestCase {
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

    func testInvoiceTotalsMatchPrototype() async throws {
        let list = try await value(FakeInvoiceRepository().observeInvoices())
        let inv = try XCTUnwrap(list.first { $0.number == "104" })
        XCTAssertEqual(inv.totals.subtotal, money("23213"))
        XCTAssertEqual(inv.totals.taxTotal, money("3481.95"))
        XCTAssertEqual(inv.totals.grandTotal, money("26694.95"))
    }

    func testCancelMovesInvoiceToCanceled() async throws {
        let repo = FakeInvoiceRepository()
        let canceled = try await repo.cancel(invoiceId: "INV-FP95G9D7JG", reason: .duplicate)
        XCTAssertEqual(canceled.status, .canceled)
        XCTAssertEqual(canceled.cancellation?.originalStatus, .issued)
    }

    func testRegisterPrependsInvoice() async throws {
        let repo = FakeInvoiceRepository()
        let item = LineItem(description: "X", itemCode: "ITM-1", nature: .goods, unit: "PCS", quantity: 1, unitPrice: money("100"), taxCode: .vat15)
        let new = try await repo.register(InvoiceDraft(documentType: .inv, transactionType: .b2c, branch: .bole, buyer: .walkIn,
                                                       paymentMode: .cash, paymentTerm: .immediate, items: [item]))
        let list = try await value(repo.observeInvoices())
        XCTAssertEqual(list.first?.id, new.id)
        XCTAssertEqual(new.totals.grandTotal, money("115"))
    }

    func testPaymentReceiptNetsOutWithholding() async throws {
        let inv = FakeInvoiceRepository(), wht = FakeWithholdingRepository()
        let views = ObserveReceiptViewsUseCase(receipts: FakeReceiptRepository(), invoices: inv, withholding: wht)
        let list = try await value(views())
        let payment = try XCTUnwrap(list.first { $0.receipt.number == "2289" })
        // INV-100: subtotal 11300, grand 12650; 2% withheld on subtotal = 226
        XCTAssertEqual(payment.withheldAmount, money("226"))
        XCTAssertEqual(payment.amount, money("12424"))
        let records = try await value(ObserveWithholdingUseCase(withholding: wht, receiptViews: views, invoices: inv)())
        XCTAssertEqual(records.count, 4)
        XCTAssertNotNil(records.first { $0.record.id == "WHT-2026-004310" }?.paymentReceipt)
    }

    func testPasswordPolicy() {
        XCTAssertEqual(PasswordPolicy.evaluate("").score, 0)
        XCTAssertTrue(PasswordPolicy.evaluate("Abcdefg1").acceptable)
        XCTAssertEqual(PasswordPolicy.evaluate("Abcdefg1!").score, 4)
    }

    func testAmountInWords() {
        XCTAssertEqual(AmountInWords.birr(money("26694.95")), "Twenty-six thousand six hundred ninety-four birr and ninety-five cents")
    }

    func testMasking() {
        XCTAssertEqual(Masking.email("yakobgirmad@gmail.com"), "ya••••@gmail.com")
        XCTAssertEqual(Masking.phone("+251921280809"), "+251 92 ••• 0809")
    }

    func testAnalyticsSplitByBranch() async {
        let repo = FakeAnalyticsRepository()
        let all = await repo.salesAnalytics(range: .days7, branch: nil)
        let bole = await repo.salesAnalytics(range: .days7, branch: .bole)
        XCTAssertEqual(all.points.count, 7)
        XCTAssertLessThan(bole.totalSales, all.totalSales)
    }
}
