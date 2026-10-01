import Foundation

/// Seller or buyer on a fiscal document. Business parties carry a TIN; consumers an optional ID.
public struct Party: Hashable, Sendable, Codable {
    public var name: String
    public var tin: String?
    public var vatNumber: String?
    public var idType: BuyerIdType?
    public var idNumber: String?
    public var phone: String?
    public var email: String?
    public var address: String?

    public init(name: String, tin: String? = nil, vatNumber: String? = nil, idType: BuyerIdType? = nil,
                idNumber: String? = nil, phone: String? = nil, email: String? = nil, address: String? = nil) {
        self.name = name; self.tin = tin; self.vatNumber = vatNumber; self.idType = idType
        self.idNumber = idNumber; self.phone = phone; self.email = email; self.address = address
    }

    public var isBusiness: Bool { tin != nil }

    /// Short identifier shown next to the name in lists ("TIN …", "NID …", phone, or walk-in).
    public var shortId: String {
        if let tin { return "TIN \(tin)" }
        if let idType { return "\(idType.rawValue) \(idNumber ?? "")".trimmingCharacters(in: .whitespaces) }
        if let phone { return phone }
        return "Walk-in"
    }

    public static let walkIn = Party(name: "Walk-in Customer")
}
