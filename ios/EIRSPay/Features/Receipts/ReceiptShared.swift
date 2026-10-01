import SwiftUI
import EIRSDomain

/// Background / foreground pair for a badge.
struct ReceiptTone {
    let bg: Color
    let fg: Color
}

enum ReceiptStyle {
    static func tone(for type: ReceiptType, _ c: EirsColors) -> ReceiptTone {
        switch type {
        case .sales: return ReceiptTone(bg: c.tint1, fg: c.brandDark)
        case .payment: return ReceiptTone(bg: c.neutralChip, fg: c.ink)
        case .advance: return ReceiptTone(bg: c.warnTint, fg: c.warnInk)
        case .refund: return ReceiptTone(bg: c.redTint, fg: c.red)
        }
    }

    static func tone(for status: InvoiceStatus, _ c: EirsColors) -> ReceiptTone {
        switch status {
        case .issued: return ReceiptTone(bg: c.tint1, fg: c.brandDark)
        case .paid: return ReceiptTone(bg: c.paidTint, fg: c.paidInk)
        case .withheld: return ReceiptTone(bg: c.warnTint, fg: c.warnInk)
        case .canceled: return ReceiptTone(bg: c.redTint, fg: c.red)
        }
    }

    static func receiptNo(_ receipt: Receipt) -> String { "RCP-\(receipt.number)" }
    static func invoiceNo(_ invoice: Invoice) -> String { "INV-\(invoice.number)" }

    /// "1", "2.5": quantities without trailing zeros.
    static func qtyText(_ value: Decimal) -> String { NSDecimalNumber(decimal: value).stringValue }

    /// 0.02 becomes "2%".
    static func percentText(_ rate: Decimal) -> String {
        let scaled: Decimal = (rate * Decimal(100)).rounded(scale: 0)
        return "\(NSDecimalNumber(decimal: scaled).intValue)%"
    }

    /// Buyer rows for "RECEIVED FROM" (without the name row).
    static func detailRows(_ party: Party) -> [(String, String)] {
        if let tin = party.tin {
            return [("TIN", tin), ("VAT number", party.vatNumber ?? "—"), ("Address", party.address ?? "—")]
        }
        if let phone = party.phone { return [("Phone", phone)] }
        if let idType = party.idType {
            let text = "\(idType.rawValue) \(party.idNumber ?? "")".trimmingCharacters(in: .whitespaces)
            return [("ID", text)]
        }
        return [("Type", "Walk-in, no details")]
    }
}

/// Mono section label inside cards ("LINKED INVOICE", "DOCUMENT").
struct ReceiptCardEyebrow: View {
    @Environment(\.eirs) private var eirs
    var text: String
    var body: some View { Eyebrow(text: text, color: eirs.muted, size: 10, tracking: 0.12) }
}

/// Brand chip that opens a linked document ("INV-100 ↗"). Uses a tap gesture (not a Button) so it can sit inside a card button.
struct ReceiptLinkChip: View {
    @Environment(\.eirs) private var eirs
    var text: String
    var onTap: () -> Void
    var body: some View {
        let shape = RoundedRectangle(cornerRadius: 6, style: .continuous)
        Text("\(text) ↗")
            .font(EirsFont.mono(11))
            .foregroundStyle(eirs.brand)
            .lineLimit(1)
            .padding(.horizontal, 7)
            .padding(.vertical, 3)
            .background(eirs.tint1, in: shape)
            .overlay(shape.strokeBorder(eirs.tint2, lineWidth: 1))
            .contentShape(shape)
            .onTapGesture { onTap() }
    }
}

/// Titled card of rows built from (key, value) pairs, each with a hairline on top.
struct ReceiptRowsCard: View {
    var title: String
    var rows: [(String, String)]
    var body: some View {
        EirsCard(padding: EdgeInsets(top: 12, leading: 16, bottom: 12, trailing: 16)) {
            ReceiptCardEyebrow(text: title).padding(.bottom, 6)
            ForEach(rows.indices, id: \.self) { index in
                KeyValueRow(key: rows[index].0, value: rows[index].1)
            }
        }
    }
}

/// Form card with 12pt gaps (register forms).
struct ReceiptFormCard<Content: View>: View {
    @ViewBuilder var content: () -> Content
    var body: some View {
        EirsCard(spacing: 12) { content() }
    }
}

/// Stacked 13pt label over a compact form input.
struct ReceiptFieldLabel<Content: View>: View {
    var label: String
    @ViewBuilder var content: () -> Content
    var body: some View {
        LabeledField(label, labelSize: 13, gap: 6, content: content)
    }
}

