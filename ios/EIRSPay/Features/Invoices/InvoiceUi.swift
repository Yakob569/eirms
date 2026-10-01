import SwiftUI
import UIKit
import EIRSDomain

/// Badge background / content colours for an invoice status (prototype `STC`).
func invStatusColors(_ status: InvoiceStatus, _ eirs: EirsColors) -> (bg: Color, fg: Color) {
    switch status {
    case .issued: return (eirs.tint1, eirs.brandDark)
    case .paid: return (eirs.paidTint, eirs.paidInk)
    case .withheld: return (eirs.warnTint, eirs.warnInk)
    case .canceled: return (eirs.redTint, eirs.red)
    }
}

/// "TIN …", "NID 123…" or "Walk-in": the prototype's partyId.
func invPartyIdLabel(_ party: Party) -> String {
    if let tin = party.tin { return "TIN \(tin)" }
    if let idType = party.idType { return "\(idType.rawValue) \(party.idNumber ?? "—")" }
    return "Walk-in"
}

/// Quantity without trailing zeros ("2", "2.5").
func invQtyText(_ quantity: Decimal) -> String { NSDecimalNumber(decimal: quantity).stringValue }

/// Writes text to the clipboard and remembers which key was copied for 1.4s (the "Copied" feedback).
@MainActor @Observable final class InvCopyFeedback {
    var copiedKey: String?
    @ObservationIgnored private var resetTask: Task<Void, Never>?

    func copy(key: String, text: String) {
        UIPasteboard.general.string = text
        copiedKey = key
        resetTask?.cancel()
        resetTask = Task {
            try? await Task.sleep(nanoseconds: 1_400_000_000)
            if !Task.isCancelled { copiedKey = nil }
        }
    }
}

/// Small "Copy" chip used on list cards. Uses a tap gesture (not a Button) so it can sit inside
/// a tappable card without triggering the card's own tap.
struct InvCopyChip: View {
    @Environment(\.eirs) private var eirs
    var copied: Bool
    var onCopy: () -> Void

    var body: some View {
        let fg: Color = copied ? eirs.brandDark : eirs.ink
        let bg: Color = copied ? eirs.tint1 : Color.white
        HStack(spacing: 5) {
            LineIcon(EirsIcons.copy, size: 11, tint: fg)
            Text(copied ? "Copied" : "Copy")
                .font(EirsFont.sans(11, .semibold))
                .foregroundStyle(fg)
                .lineLimit(1)
        }
        .padding(.horizontal, 9)
        .frame(height: 26)
        .background(bg, in: RoundedRectangle(cornerRadius: 7, style: .continuous))
        .solidBorder(eirs.line, radius: 7)
        .contentShape(Rectangle())
        .onTapGesture { onCopy() }
    }
}

