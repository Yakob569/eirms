import Foundation

public enum Branch: String, CaseIterable, Codable, Hashable, Sendable, Identifiable {
    case main, bole, piassa
    public var id: String { rawValue }
    public var displayName: String {
        switch self {
        case .main: return "Main Branch"
        case .bole: return "Bole"
        case .piassa: return "Piassa"
        }
    }
}

/// MoR transaction types. Consumer sales (B2C/G2C) need lighter buyer identification.
public enum TransactionType: String, CaseIterable, Codable, Hashable, Sendable, Identifiable {
    case b2c = "B2C", b2b = "B2B", b2g = "B2G", g2b = "G2B", g2c = "G2C"
    public var id: String { rawValue }
    public var displayName: String {
        switch self {
        case .b2c: return "Business to consumer"
        case .b2b: return "Business to business"
        case .b2g: return "Business to government"
        case .g2b: return "Government to business"
        case .g2c: return "Government to consumer"
        }
    }
    public var isConsumer: Bool { self == .b2c || self == .g2c }
}

/// INV/CRE/DEB are the standard family; RTN (retainer) is followed up by INT (interim) and FIN (final) invoices.
public enum DocumentType: String, CaseIterable, Codable, Hashable, Sendable, Identifiable {
    case inv = "INV", cre = "CRE", deb = "DEB", rtn = "RTN", int = "INT", fin = "FIN"
    public var id: String { rawValue }
    public var displayName: String {
        switch self {
        case .inv: return "Invoice"
        case .cre: return "Credit note"
        case .deb: return "Debit note"
        case .rtn: return "Retainer invoice"
        case .int: return "Interim invoice"
        case .fin: return "Final invoice"
        }
    }
    /// Credit and debit notes amend an earlier invoice and must keep its buyer.
    public var isAmendment: Bool { self == .cre || self == .deb }
    /// Interim and final invoices are issued against a retainer.
    public var isRetainerFollowUp: Bool { self == .int || self == .fin }
}

public enum InvoiceStatus: String, CaseIterable, Codable, Hashable, Sendable {
    case issued = "ISSUED", paid = "PAID", withheld = "WITHHELD", canceled = "CANCELED"
}

public enum TaxCode: String, CaseIterable, Codable, Hashable, Sendable, Identifiable {
    case vat15 = "VAT15", vat0 = "VAT0", exc10 = "EXC10"
    public var id: String { rawValue }
    public var rate: Decimal {
        switch self {
        case .vat15: return money("0.15")
        case .vat0: return 0
        case .exc10: return money("0.10")
        }
    }
    public var label: String {
        switch self {
        case .vat15: return "VAT 15%"
        case .vat0: return "VAT 0%"
        case .exc10: return "Excise 10%"
        }
    }
    public var summary: String {
        switch self {
        case .vat15: return "Standard VAT 15%"
        case .vat0: return "Zero-rated"
        case .exc10: return "Excise 10%"
        }
    }
}

public enum NatureOfSupply: String, CaseIterable, Codable, Hashable, Sendable {
    case goods = "Goods", service = "Service"
}

/// Buyer identification accepted by MoR for consumer sales.
public enum BuyerIdType: String, CaseIterable, Codable, Hashable, Sendable, Identifiable {
    case nid = "NID", kid = "KID", sid = "SID", wid = "WID", pst = "PST", dls = "DLS", mrs = "MRS"
    public var id: String { rawValue }
    public var displayName: String {
        switch self {
        case .nid: return "National ID"
        case .kid: return "Kebele ID"
        case .sid: return "Student ID"
        case .wid: return "Work ID"
        case .pst: return "Passport"
        case .dls: return "Driving licence"
        case .mrs: return "Military / residence"
        }
    }
}

public enum InvoicePaymentMode: String, CaseIterable, Codable, Hashable, Sendable, Identifiable {
    case cash = "CASH", bankTransfer = "BANK TRANSFER", mobileMoney = "MOBILE MONEY", credit = "CREDIT"
    public var id: String { rawValue }
    public var displayName: String {
        switch self {
        case .cash: return "Cash"
        case .bankTransfer: return "Bank transfer"
        case .mobileMoney: return "Mobile money"
        case .credit: return "Credit"
        }
    }
}

public enum PaymentTerm: String, CaseIterable, Codable, Hashable, Sendable, Identifiable {
    case immediate = "IMMEDIATE", net30 = "NET 30"
    public var id: String { rawValue }
    public var displayName: String { self == .immediate ? "Immediate" : "Net 30" }
}

public enum CancelReason: String, CaseIterable, Codable, Hashable, Sendable, Identifiable {
    case customerRequest = "01", wrongAmount = "02", duplicate = "03", wrongBuyer = "04"
    public var id: String { rawValue }
    public var code: String { rawValue }
    public var label: String {
        switch self {
        case .customerRequest: return "Customer request"
        case .wrongAmount: return "Wrong amount entered"
        case .duplicate: return "Duplicate document"
        case .wrongBuyer: return "Wrong buyer details"
        }
    }
}

public let catalogUnits = ["PCS", "KG", "L", "BOX", "CASE", "HRS", "JOB"]

/// All fiscal timestamps are shown in Addis Ababa time regardless of the device's zone.
public let ethiopiaTimeZone = TimeZone(identifier: "Africa/Addis_Ababa") ?? .current

public extension Calendar {
    static var ethiopia: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = ethiopiaTimeZone
        return calendar
    }
}

/// Builds a date from "yyyy-MM-dd" and "HH:mm" in Addis Ababa time (used by sample data).
public func ethiopiaDate(_ day: String, _ time: String = "00:00") -> Date {
    let formatter = DateFormatter()
    formatter.locale = Locale(identifier: "en_US_POSIX")
    formatter.timeZone = ethiopiaTimeZone
    formatter.dateFormat = "yyyy-MM-dd HH:mm"
    return formatter.date(from: "\(day) \(time)") ?? Date()
}
