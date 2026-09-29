package com.eirspay.data.seed

import com.eirspay.data.fake.FakeIrn
import com.eirspay.domain.model.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/** Sample merchant data mirroring the design prototype. Replace with API responses. */
internal object SampleData {
    val seller = Party(
        name = "Addispay Financial Technology S.C", tin = "4082549522", vatNumber = "43256663343256663322",
        phone = "+251921280809", email = "cto@addispay.et", address = "Addis Ababa · Wereda 13",
    )

    val branches = listOf(
        BranchInfo(Branch.MAIN, sourceSystems = 2, isDefault = true),
        BranchInfo(Branch.BOLE, sourceSystems = 1, isDefault = false),
        BranchInfo(Branch.PIASSA, sourceSystems = 1, isDefault = false),
    )

    val systemFor = mapOf(Branch.MAIN to "2EC77ABF63", Branch.BOLE to "5054087CDF", Branch.PIASSA to "9A1C44B0E2")

    private fun at(date: String, time: String) = LocalDateTime.parse("${date}T$time")
    private fun line(desc: String, code: String, nature: NatureOfSupply, unit: String, qty: Int, price: Double, tax: TaxCode) =
        LineItem(desc, code, nature, unit, BigDecimal(qty), money(price), tax)

    private val G = NatureOfSupply.GOODS
    private val S = NatureOfSupply.SERVICE

    private val jhone = Party("Jhone Doe", idType = BuyerIdType.NID, idNumber = "FAN 4521 8890 1123", phone = "+251911442210")
    private val jhoneNoPhone = jhone.copy(phone = null)
    val ethioTelecom = Party("Ethio Telecom", tin = "0000030603", vatNumber = "1087650012", phone = "+251115515000", email = "procurement@ethiotelecom.et", address = "Addis Ababa · Wereda 05")
    val dashen = Party("Dashen Trading PLC", tin = "0041527796", vatNumber = "2231009874", phone = "+251116612233", email = "ap@dashentrading.et", address = "Addis Ababa · Wereda 03")
    val awash = Party("Awash Logistics S.C", tin = "0063321458", vatNumber = "3345120098", phone = "+251114408877", email = "finance@awashlogistics.et", address = "Addis Ababa · Wereda 11")
    val zemen = Party("Zemen Bank S.C", tin = "0005581342", vatNumber = "1122334455", email = "ops@zemenbank.com", address = "Addis Ababa · Wereda 01")

    private fun invoice(
        id: String, no: String, dt: DocumentType, tt: TransactionType, status: InvoiceStatus, date: String, time: String,
        branch: Branch, buyer: Party, pay: InvoicePaymentMode, term: PaymentTerm, items: List<LineItem>,
        amendment: Amendment? = null, cancel: Cancellation? = null,
    ) = Invoice(
        id = id, number = no, documentType = dt, transactionType = tt, status = status, issuedAt = at(date, time),
        branch = branch, sourceSystem = SourceSystem("POS", systemFor.getValue(branch)), seller = seller, buyer = buyer,
        paymentMode = pay, paymentTerm = term, items = items, irn = FakeIrn.of(id), amendment = amendment, cancellation = cancel,
    )

    fun invoices(): List<Invoice> = listOf(
        invoice("INV-FP95G9D7JG", "104", DocumentType.INV, TransactionType.B2C, InvoiceStatus.ISSUED, "2026-09-24", "10:42", Branch.MAIN, jhone,
            InvoicePaymentMode.CASH, PaymentTerm.IMMEDIATE, listOf(line("Laptop stand, aluminium", "ITM-0012", G, "PCS", 1, 23213.0, TaxCode.VAT15))),
        invoice("INV-K2L8M3N4PQ", "103", DocumentType.INV, TransactionType.B2C, InvoiceStatus.CANCELED, "2026-09-24", "09:58", Branch.MAIN, jhoneNoPhone,
            InvoicePaymentMode.CASH, PaymentTerm.IMMEDIATE, listOf(line("Wireless keyboard", "ITM-0031", G, "PCS", 1, 1678.0, TaxCode.VAT15)),
            cancel = Cancellation(CancelReason.WRONG_AMOUNT, at("2026-09-24", "11:05"), InvoiceStatus.ISSUED)),
        invoice("INV-7HJ2K9D0LA", "102", DocumentType.INV, TransactionType.B2C, InvoiceStatus.CANCELED, "2026-09-23", "15:03", Branch.MAIN, jhoneNoPhone,
            InvoicePaymentMode.CASH, PaymentTerm.IMMEDIATE, listOf(line("Monitor arm", "ITM-0044", G, "PCS", 2, 1500.0, TaxCode.VAT15)),
            cancel = Cancellation(CancelReason.CUSTOMER_REQUEST, at("2026-09-23", "16:20"), InvoiceStatus.ISSUED)),
        invoice("INV-R4T6Y8U1IO", "101", DocumentType.INV, TransactionType.B2G, InvoiceStatus.ISSUED, "2026-09-24", "09:12", Branch.MAIN, ethioTelecom,
            InvoicePaymentMode.BANK_TRANSFER, PaymentTerm.NET_30, listOf(
                line("Network cabling installation", "SRV-0102", S, "JOB", 1, 30000.0, TaxCode.VAT15),
                line("Cat6 cable, 305m box", "ITM-0210", G, "BOX", 10, 1200.0, TaxCode.VAT15))),
        invoice("INV-B3N5M7Q9WE", "100", DocumentType.INV, TransactionType.B2B, InvoiceStatus.PAID, "2026-09-23", "17:20", Branch.BOLE, dashen,
            InvoicePaymentMode.BANK_TRANSFER, PaymentTerm.IMMEDIATE, listOf(
                line("Ergonomic office chair", "ITM-0301", G, "PCS", 5, 1800.0, TaxCode.VAT15),
                line("Delivery and assembly", "SRV-0009", S, "JOB", 1, 2300.0, TaxCode.VAT0))),
        invoice("INV-Z8X6C4V2BN", "099", DocumentType.INV, TransactionType.B2B, InvoiceStatus.WITHHELD, "2026-09-23", "11:02", Branch.MAIN, awash,
            InvoicePaymentMode.CREDIT, PaymentTerm.NET_30, listOf(line("POS integration consulting", "SRV-0110", S, "HRS", 1, 8582.96, TaxCode.VAT15))),
        invoice("CRE-Q1W2E3R4TY", "098", DocumentType.CRE, TransactionType.B2B, InvoiceStatus.ISSUED, "2026-09-22", "14:10", Branch.BOLE,
            dashen.copy(phone = null, email = null), InvoicePaymentMode.BANK_TRANSFER, PaymentTerm.IMMEDIATE,
            listOf(line("Ergonomic office chair (return)", "ITM-0301", G, "PCS", 1, 1800.0, TaxCode.VAT15)),
            amendment = Amendment("Returned 1 chair, damaged in delivery", "INV-B3N5M7Q9WE")),
        invoice("INV-P0O9I8U7YT", "097", DocumentType.INV, TransactionType.B2C, InvoiceStatus.ISSUED, "2026-09-22", "12:31", Branch.PIASSA,
            Party("Selam Bekele", idType = BuyerIdType.KID, idNumber = "AA-114-22871"), InvoicePaymentMode.MOBILE_MONEY, PaymentTerm.IMMEDIATE, listOf(
                line("Yirgacheffe coffee beans, 1kg", "ITM-0508", G, "KG", 4, 650.0, TaxCode.VAT15),
                line("Printed notebooks", "ITM-0612", G, "PCS", 2, 350.0, TaxCode.VAT0))),
        invoice("INV-L5K4J3H2GF", "096", DocumentType.INV, TransactionType.B2B, InvoiceStatus.ISSUED, "2026-09-21", "10:05", Branch.MAIN, zemen,
            InvoicePaymentMode.BANK_TRANSFER, PaymentTerm.NET_30, listOf(
                line("Thermal receipt paper rolls", "ITM-0701", G, "PCS", 40, 85.0, TaxCode.VAT15),
                line("Imported soft drinks, case", "ITM-0815", G, "CASE", 12, 150.0, TaxCode.EXC10))),
    )

    private fun receipt(
        id: String, no: String, type: ReceiptType, pm: PaymentMethod, date: String, time: String, branch: Branch,
        buyer: Party? = null, items: List<LineItem> = emptyList(), ref: String? = null, provider: String? = null,
        tendered: Double? = null, linked: String? = null, amount: Double? = null,
    ) = Receipt(
        id = id, number = no, type = type, method = pm, issuedAt = at(date, time), branch = branch,
        sourceSystem = SourceSystem("POS", systemFor.getValue(branch)), irn = FakeIrn.of(id), buyer = buyer, items = items,
        reference = ref, provider = provider, tendered = tendered?.let { money(it) }, linkedInvoiceId = linked, amount = amount?.let { money(it) },
    )

    fun receipts(): List<Receipt> = listOf(
        receipt("RCP-A2291", "2291", ReceiptType.SALES, PaymentMethod.CASH, "2026-09-24", "10:31", Branch.BOLE, Party.WalkIn, listOf(
            line("Yirgacheffe coffee beans, 1kg", "ITM-0508", G, "KG", 1, 650.0, TaxCode.VAT15),
            line("Printed notebooks", "ITM-0612", G, "PCS", 1, 100.0, TaxCode.VAT0)), tendered = 1000.0),
        receipt("RCP-A2290", "2290", ReceiptType.SALES, PaymentMethod.MOBILE_MONEY, "2026-09-24", "08:47", Branch.PIASSA, Party.WalkIn, listOf(
            line("Imported soft drinks, case", "ITM-0815", G, "CASE", 2, 150.0, TaxCode.EXC10),
            line("Thermal receipt paper rolls", "ITM-0701", G, "PCS", 1, 85.0, TaxCode.VAT15)), ref = "TB7Q2K91XZ", provider = "telebirr"),
        receipt("RCP-A2289", "2289", ReceiptType.PAYMENT, PaymentMethod.BANK_TRANSFER, "2026-09-23", "14:36", Branch.BOLE, ref = "FT26267AB91C", linked = "INV-B3N5M7Q9WE"),
        receipt("RCP-A2288", "2288", ReceiptType.SALES, PaymentMethod.CARD, "2026-09-23", "10:15", Branch.BOLE, Party("Hanna Tesfaye", phone = "+251911238840"),
            listOf(line("Wireless keyboard", "ITM-0031", G, "PCS", 1, 1678.0, TaxCode.VAT15)), ref = "•••• 4417"),
        receipt("RCP-A2287", "2287", ReceiptType.ADVANCE, PaymentMethod.CHEQUE, "2026-09-22", "16:02", Branch.MAIN, Party("Zemen Bank S.C", tin = "0005581342"),
            listOf(line("Advance: network cabling, phase 1", "SRV-0102", S, "JOB", 1, 15000.0, TaxCode.VAT15)), ref = "CBE 00482913"),
        receipt("RCP-A2286", "2286", ReceiptType.REFUND, PaymentMethod.CASH, "2026-09-22", "15:20", Branch.PIASSA, linked = "INV-P0O9I8U7YT", amount = 747.5),
        receipt("RCP-A2285", "2285", ReceiptType.PAYMENT, PaymentMethod.CHEQUE, "2026-09-24", "11:40", Branch.MAIN, ref = "Awash 11029384", linked = "INV-Z8X6C4V2BN"),
    )

    fun withholding(): List<WithholdingRecord> = listOf(
        WithholdingRecord("WHT-2026-004417", "INV-Z8X6C4V2BN", BigDecimal("0.02"), LocalDate.parse("2026-09-24"), "Sep 2026", true),
        WithholdingRecord("WHT-2026-004392", "INV-R4T6Y8U1IO", BigDecimal("0.02"), LocalDate.parse("2026-09-24"), "Sep 2026", false),
        WithholdingRecord("WHT-2026-004310", "INV-B3N5M7Q9WE", BigDecimal("0.02"), LocalDate.parse("2026-09-23"), "Sep 2026", true),
        WithholdingRecord("WHT-2026-004188", "INV-L5K4J3H2GF", BigDecimal("0.02"), LocalDate.parse("2026-09-21"), "Sep 2026", false),
    )

    private fun item(code: String, desc: String, nature: NatureOfSupply, hs: String, tax: TaxCode, unit: String, price: Double, active: Boolean, cat: String) =
        CatalogItem(code, desc, nature, hs, tax, unit, money(price), active, cat)

    fun catalog(): List<CatalogItem> = listOf(
        item("ITM-0012", "Laptop stand, aluminium", G, "7616.99.90", TaxCode.VAT15, "PCS", 23213.0, true, "Electronics"),
        item("ITM-0031", "Wireless keyboard", G, "8471.60.00", TaxCode.VAT15, "PCS", 1678.0, true, "Electronics"),
        item("ITM-0044", "Monitor arm", G, "8473.30.00", TaxCode.VAT15, "PCS", 1500.0, true, "Electronics"),
        item("ITM-0210", "Cat6 cable, 305m box", G, "8544.49.00", TaxCode.VAT15, "BOX", 1200.0, true, "Electronics"),
        item("ITM-0301", "Ergonomic office chair", G, "9401.30.00", TaxCode.VAT15, "PCS", 1800.0, true, "Furniture"),
        item("ITM-0508", "Yirgacheffe coffee beans, 1kg", G, "0901.11.00", TaxCode.VAT15, "KG", 650.0, true, "Grocery"),
        item("ITM-0509", "Sidamo coffee, 500g", G, "0901.11.00", TaxCode.VAT15, "PCS", 340.0, true, "Grocery"),
        item("ITM-0520", "Teff flour, 5kg", G, "1102.90.00", TaxCode.VAT0, "PCS", 780.0, true, "Grocery"),
        item("ITM-0531", "Honey, 1kg jar", G, "0409.00.00", TaxCode.VAT15, "PCS", 560.0, true, "Grocery"),
        item("ITM-0612", "Printed notebooks", G, "4820.10.00", TaxCode.VAT0, "PCS", 350.0, true, "Stationery"),
        item("ITM-0615", "Ballpoint pens, 10", G, "9608.10.00", TaxCode.VAT15, "BOX", 95.0, true, "Stationery"),
        item("ITM-0701", "Thermal receipt paper rolls", G, "4811.90.00", TaxCode.VAT15, "PCS", 85.0, true, "Stationery"),
        item("ITM-0815", "Imported soft drinks, case", G, "2202.10.00", TaxCode.EXC10, "CASE", 150.0, true, "Drinks"),
        item("ITM-0820", "Mineral water, 6 × 2L", G, "2201.10.00", TaxCode.VAT15, "CASE", 210.0, true, "Drinks"),
        item("ITM-0826", "Fresh juice, 1L", G, "2009.89.00", TaxCode.VAT15, "L", 120.0, true, "Drinks"),
        item("SRV-0009", "Delivery and assembly", S, "", TaxCode.VAT0, "JOB", 2300.0, true, "Services"),
        item("SRV-0102", "Network cabling installation", S, "", TaxCode.VAT15, "JOB", 30000.0, true, "Services"),
        item("SRV-0110", "POS integration consulting", S, "", TaxCode.VAT15, "HRS", 1250.0, true, "Services"),
        item("ITM-0099", "Fax machine toner", G, "8443.99.00", TaxCode.VAT15, "PCS", 2400.0, false, "Electronics"),
    )

    val taxpayers = listOf(ethioTelecom, dashen, awash, zemen)
}
