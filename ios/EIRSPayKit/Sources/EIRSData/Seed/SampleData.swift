import Foundation
import EIRSDomain

/// Sample merchant data mirroring the design prototype. Replace with API responses.
enum SampleData {
    static let seller = Party(
        name: "Addispay Financial Technology S.C", tin: "4082549522", vatNumber: "43256663343256663322",
        phone: "+251921280809", email: "cto@addispay.et", address: "Addis Ababa · Wereda 13")

    static let branches = [
        BranchInfo(branch: .main, sourceSystems: 2, isDefault: true),
        BranchInfo(branch: .bole, sourceSystems: 1, isDefault: false),
        BranchInfo(branch: .piassa, sourceSystems: 1, isDefault: false),
    ]

    static let systemFor: [Branch: String] = [.main: "2EC77ABF63", .bole: "5054087CDF", .piassa: "9A1C44B0E2"]

    private static func line(_ desc: String, _ code: String, _ nature: NatureOfSupply, _ unit: String,
                             _ qty: Int, _ price: String, _ tax: TaxCode) -> LineItem {
        LineItem(description: desc, itemCode: code, nature: nature, unit: unit,
                 quantity: Decimal(qty), unitPrice: money(price), taxCode: tax)
    }

    private static let jhone = Party(name: "Jhone Doe", idType: .nid, idNumber: "FAN 4521 8890 1123", phone: "+251911442210")
    private static let jhoneNoPhone = Party(name: "Jhone Doe", idType: .nid, idNumber: "FAN 4521 8890 1123")
    static let ethioTelecom = Party(name: "Ethio Telecom", tin: "0000030603", vatNumber: "1087650012", phone: "+251115515000",
                                    email: "procurement@ethiotelecom.et", address: "Addis Ababa · Wereda 05")
    static let dashen = Party(name: "Dashen Trading PLC", tin: "0041527796", vatNumber: "2231009874", phone: "+251116612233",
                              email: "ap@dashentrading.et", address: "Addis Ababa · Wereda 03")
    static let awash = Party(name: "Awash Logistics S.C", tin: "0063321458", vatNumber: "3345120098", phone: "+251114408877",
                             email: "finance@awashlogistics.et", address: "Addis Ababa · Wereda 11")
    static let zemen = Party(name: "Zemen Bank S.C", tin: "0005581342", vatNumber: "1122334455",
                             email: "ops@zemenbank.com", address: "Addis Ababa · Wereda 01")

    private static func invoice(
        _ id: String, _ no: String, _ dt: DocumentType, _ tt: TransactionType, _ status: InvoiceStatus,
        _ date: String, _ time: String, _ branch: Branch, _ buyer: Party, _ pay: InvoicePaymentMode, _ term: PaymentTerm,
        _ items: [LineItem], amendment: Amendment? = nil, cancellation: Cancellation? = nil
    ) -> Invoice {
        Invoice(id: id, number: no, documentType: dt, transactionType: tt, status: status,
                issuedAt: ethiopiaDate(date, time), branch: branch,
                sourceSystem: SourceSystem(type: "POS", number: systemFor[branch] ?? ""), seller: seller, buyer: buyer,
                paymentMode: pay, paymentTerm: term, items: items, irn: FakeIrn.of(id),
                amendment: amendment, cancellation: cancellation)
    }

    static func invoices() -> [Invoice] {
        let g = NatureOfSupply.goods, s = NatureOfSupply.service
        return [
            invoice("INV-FP95G9D7JG", "104", .inv, .b2c, .issued, "2026-09-24", "10:42", .main, jhone, .cash, .immediate,
                    [line("Laptop stand, aluminium", "ITM-0012", g, "PCS", 1, "23213", .vat15)]),
            invoice("INV-K2L8M3N4PQ", "103", .inv, .b2c, .canceled, "2026-09-24", "09:58", .main, jhoneNoPhone, .cash, .immediate,
                    [line("Wireless keyboard", "ITM-0031", g, "PCS", 1, "1678", .vat15)],
                    cancellation: Cancellation(reason: .wrongAmount, canceledAt: ethiopiaDate("2026-09-24", "11:05"), originalStatus: .issued)),
            invoice("INV-7HJ2K9D0LA", "102", .inv, .b2c, .canceled, "2026-09-23", "15:03", .main, jhoneNoPhone, .cash, .immediate,
                    [line("Monitor arm", "ITM-0044", g, "PCS", 2, "1500", .vat15)],
                    cancellation: Cancellation(reason: .customerRequest, canceledAt: ethiopiaDate("2026-09-23", "16:20"), originalStatus: .issued)),
            invoice("INV-R4T6Y8U1IO", "101", .inv, .b2g, .issued, "2026-09-24", "09:12", .main, ethioTelecom, .bankTransfer, .net30,
                    [line("Network cabling installation", "SRV-0102", s, "JOB", 1, "30000", .vat15),
                     line("Cat6 cable, 305m box", "ITM-0210", g, "BOX", 10, "1200", .vat15)]),
            invoice("INV-B3N5M7Q9WE", "100", .inv, .b2b, .paid, "2026-09-23", "17:20", .bole, dashen, .bankTransfer, .immediate,
                    [line("Ergonomic office chair", "ITM-0301", g, "PCS", 5, "1800", .vat15),
                     line("Delivery and assembly", "SRV-0009", s, "JOB", 1, "2300", .vat0)]),
            invoice("INV-Z8X6C4V2BN", "099", .inv, .b2b, .withheld, "2026-09-23", "11:02", .main, awash, .credit, .net30,
                    [line("POS integration consulting", "SRV-0110", s, "HRS", 1, "8582.96", .vat15)]),
            invoice("CRE-Q1W2E3R4TY", "098", .cre, .b2b, .issued, "2026-09-22", "14:10", .bole,
                    Party(name: dashen.name, tin: dashen.tin, vatNumber: dashen.vatNumber, address: dashen.address),
                    .bankTransfer, .immediate,
                    [line("Ergonomic office chair (return)", "ITM-0301", g, "PCS", 1, "1800", .vat15)],
                    amendment: Amendment(reason: "Returned 1 chair, damaged in delivery", previousInvoiceId: "INV-B3N5M7Q9WE")),
            invoice("INV-P0O9I8U7YT", "097", .inv, .b2c, .issued, "2026-09-22", "12:31", .piassa,
                    Party(name: "Selam Bekele", idType: .kid, idNumber: "AA-114-22871"), .mobileMoney, .immediate,
                    [line("Yirgacheffe coffee beans, 1kg", "ITM-0508", g, "KG", 4, "650", .vat15),
                     line("Printed notebooks", "ITM-0612", g, "PCS", 2, "350", .vat0)]),
            invoice("INV-L5K4J3H2GF", "096", .inv, .b2b, .issued, "2026-09-21", "10:05", .main, zemen, .bankTransfer, .net30,
                    [line("Thermal receipt paper rolls", "ITM-0701", g, "PCS", 40, "85", .vat15),
                     line("Imported soft drinks, case", "ITM-0815", g, "CASE", 12, "150", .exc10)]),
        ]
    }

    private static func receipt(
        _ id: String, _ no: String, _ type: ReceiptType, _ pm: PaymentMethod, _ date: String, _ time: String, _ branch: Branch,
        buyer: Party? = nil, items: [LineItem] = [], ref: String? = nil, provider: String? = nil,
        tendered: String? = nil, linked: String? = nil, amount: String? = nil
    ) -> Receipt {
        Receipt(id: id, number: no, type: type, method: pm, issuedAt: ethiopiaDate(date, time), branch: branch,
                sourceSystem: SourceSystem(type: "POS", number: systemFor[branch] ?? ""), irn: FakeIrn.of(id),
                buyer: buyer, items: items, reference: ref, provider: provider, tendered: tendered.map(money),
                linkedInvoiceId: linked, amount: amount.map(money))
    }

    static func receipts() -> [Receipt] {
        let g = NatureOfSupply.goods, s = NatureOfSupply.service
        return [
            receipt("RCP-A2291", "2291", .sales, .cash, "2026-09-24", "10:31", .bole, buyer: .walkIn,
                    items: [line("Yirgacheffe coffee beans, 1kg", "ITM-0508", g, "KG", 1, "650", .vat15),
                            line("Printed notebooks", "ITM-0612", g, "PCS", 1, "100", .vat0)], tendered: "1000"),
            receipt("RCP-A2290", "2290", .sales, .mobileMoney, "2026-09-24", "08:47", .piassa, buyer: .walkIn,
                    items: [line("Imported soft drinks, case", "ITM-0815", g, "CASE", 2, "150", .exc10),
                            line("Thermal receipt paper rolls", "ITM-0701", g, "PCS", 1, "85", .vat15)],
                    ref: "TB7Q2K91XZ", provider: "telebirr"),
            receipt("RCP-A2289", "2289", .payment, .bankTransfer, "2026-09-23", "14:36", .bole, ref: "FT26267AB91C", linked: "INV-B3N5M7Q9WE"),
            receipt("RCP-A2288", "2288", .sales, .card, "2026-09-23", "10:15", .bole,
                    buyer: Party(name: "Hanna Tesfaye", phone: "+251911238840"),
                    items: [line("Wireless keyboard", "ITM-0031", g, "PCS", 1, "1678", .vat15)], ref: "•••• 4417"),
            receipt("RCP-A2287", "2287", .advance, .cheque, "2026-09-22", "16:02", .main,
                    buyer: Party(name: "Zemen Bank S.C", tin: "0005581342"),
                    items: [line("Advance: network cabling, phase 1", "SRV-0102", s, "JOB", 1, "15000", .vat15)], ref: "CBE 00482913"),
            receipt("RCP-A2286", "2286", .refund, .cash, "2026-09-22", "15:20", .piassa, linked: "INV-P0O9I8U7YT", amount: "747.5"),
            receipt("RCP-A2285", "2285", .payment, .cheque, "2026-09-24", "11:40", .main, ref: "Awash 11029384", linked: "INV-Z8X6C4V2BN"),
        ]
    }

    static func withholding() -> [WithholdingRecord] {
        let rate = money("0.02")
        return [
            WithholdingRecord(id: "WHT-2026-004417", invoiceId: "INV-Z8X6C4V2BN", rate: rate, withheldOn: ethiopiaDate("2026-09-24"), taxPeriod: "Sep 2026", certificateReceived: true),
            WithholdingRecord(id: "WHT-2026-004392", invoiceId: "INV-R4T6Y8U1IO", rate: rate, withheldOn: ethiopiaDate("2026-09-24"), taxPeriod: "Sep 2026", certificateReceived: false),
            WithholdingRecord(id: "WHT-2026-004310", invoiceId: "INV-B3N5M7Q9WE", rate: rate, withheldOn: ethiopiaDate("2026-09-23"), taxPeriod: "Sep 2026", certificateReceived: true),
            WithholdingRecord(id: "WHT-2026-004188", invoiceId: "INV-L5K4J3H2GF", rate: rate, withheldOn: ethiopiaDate("2026-09-21"), taxPeriod: "Sep 2026", certificateReceived: false),
        ]
    }

    private static func item(_ code: String, _ desc: String, _ nature: NatureOfSupply, _ hs: String, _ tax: TaxCode,
                             _ unit: String, _ price: String, _ active: Bool, _ cat: String) -> CatalogItem {
        CatalogItem(code: code, description: desc, nature: nature, hsCode: hs, taxCode: tax, unit: unit,
                    unitPrice: money(price), active: active, category: cat)
    }

    static func catalog() -> [CatalogItem] {
        let g = NatureOfSupply.goods, s = NatureOfSupply.service
        return [
            item("ITM-0012", "Laptop stand, aluminium", g, "7616.99.90", .vat15, "PCS", "23213", true, "Electronics"),
            item("ITM-0031", "Wireless keyboard", g, "8471.60.00", .vat15, "PCS", "1678", true, "Electronics"),
            item("ITM-0044", "Monitor arm", g, "8473.30.00", .vat15, "PCS", "1500", true, "Electronics"),
            item("ITM-0210", "Cat6 cable, 305m box", g, "8544.49.00", .vat15, "BOX", "1200", true, "Electronics"),
            item("ITM-0301", "Ergonomic office chair", g, "9401.30.00", .vat15, "PCS", "1800", true, "Furniture"),
            item("ITM-0508", "Yirgacheffe coffee beans, 1kg", g, "0901.11.00", .vat15, "KG", "650", true, "Grocery"),
            item("ITM-0509", "Sidamo coffee, 500g", g, "0901.11.00", .vat15, "PCS", "340", true, "Grocery"),
            item("ITM-0520", "Teff flour, 5kg", g, "1102.90.00", .vat0, "PCS", "780", true, "Grocery"),
            item("ITM-0531", "Honey, 1kg jar", g, "0409.00.00", .vat15, "PCS", "560", true, "Grocery"),
            item("ITM-0612", "Printed notebooks", g, "4820.10.00", .vat0, "PCS", "350", true, "Stationery"),
            item("ITM-0615", "Ballpoint pens, 10", g, "9608.10.00", .vat15, "BOX", "95", true, "Stationery"),
            item("ITM-0701", "Thermal receipt paper rolls", g, "4811.90.00", .vat15, "PCS", "85", true, "Stationery"),
            item("ITM-0815", "Imported soft drinks, case", g, "2202.10.00", .exc10, "CASE", "150", true, "Drinks"),
            item("ITM-0820", "Mineral water, 6 × 2L", g, "2201.10.00", .vat15, "CASE", "210", true, "Drinks"),
            item("ITM-0826", "Fresh juice, 1L", g, "2009.89.00", .vat15, "L", "120", true, "Drinks"),
            item("SRV-0009", "Delivery and assembly", s, "", .vat0, "JOB", "2300", true, "Services"),
            item("SRV-0102", "Network cabling installation", s, "", .vat15, "JOB", "30000", true, "Services"),
            item("SRV-0110", "POS integration consulting", s, "", .vat15, "HRS", "1250", true, "Services"),
            item("ITM-0099", "Fax machine toner", g, "8443.99.00", .vat15, "PCS", "2400", false, "Electronics"),
        ]
    }

    static let taxpayers = [ethioTelecom, dashen, awash, zemen]
}
