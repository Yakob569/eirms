import SwiftUI
import EIRSDomain

@MainActor
struct InvoiceDetailScreen: View {
    @Environment(\.eirs) private var eirs
    @State private var model: InvoiceDetailModel
    var onBack: () -> Void
    var onViewReceipt: (String) -> Void

    init(container: AppContainer, id: String, onBack: @escaping () -> Void, onViewReceipt: @escaping (String) -> Void) {
        _model = State(initialValue: InvoiceDetailModel(container: container, id: id))
        self.onBack = onBack
        self.onViewReceipt = onViewReceipt
    }

    var body: some View {
        LoadingHost(loading: model.loading) {
            ViewScaffold(
                header: { header },
                bottomBar: { bottomBar },
                content: { content })
        }
        .eirsSheet(isPresented: $model.cancelSheetOpen) { cancelSheet }
        .task { await model.observe() }
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
    }

    // MARK: Header / bar / content

    private var header: some View {
        let inv: Invoice? = model.invoice
        let eyebrow: String = inv.map { $0.documentType.displayName + " · " + Fmt.dateTime($0.issuedAt) } ?? ""
        return ViewHeader(eyebrow: eyebrow, onBack: onBack) {
            HStack(spacing: 8) {
                titleText(inv?.number ?? "")
                if let inv {
                    let colors = invStatusColors(inv.status, eirs)
                    Badge(text: inv.status.rawValue, background: colors.bg, content: colors.fg, tracking: 0)
                }
            }
        }
    }

    private func titleText(_ number: String) -> some View {
        let plain = Text("No. ").font(EirsFont.serif(26)).foregroundColor(eirs.ink)
        let accent = Text(number).font(EirsFont.serif(26, italic: true)).foregroundColor(eirs.brand)
        return (plain + accent).lineLimit(1)
    }

    @ViewBuilder private var bottomBar: some View {
        if let inv = model.invoice {
            StickyBottomBar(shadow: false) {
                HStack(spacing: 10) {
                    if !inv.isCanceled {
                        OutlineButton("Cancel", height: 50, borderColor: eirs.redBorder, contentColor: eirs.red) {
                            model.openCancel()
                        }
                    }
                    PrimaryButton("View receipt", height: 50, fontSize: 15) { onViewReceipt(inv.id) }
                }
            }
        }
    }

    @ViewBuilder private var content: some View {
        if let inv = model.invoice {
            InvoiceDetailBody(
                inv: inv, amended: model.amended,
                copied: model.copier.copiedKey == "detail",
                onCopy: { model.copier.copy(key: "detail", text: inv.irn) })
        }
    }

    // MARK: Cancel sheet

    private var cancelSheet: some View {
        InvoiceCancelSheet(number: model.invoice?.number ?? "", reason: $model.cancelReason) {
            Task { await model.confirmCancel() }
        }
    }
}

/// "Cancel invoice 104": reason picker and the red confirm button. Shared by the detail screen and the
/// list cards' ⋮ menu.
struct InvoiceCancelSheet: View {
    @Environment(\.eirs) private var eirs
    var number: String
    @Binding var reason: CancelReason
    var onConfirm: () -> Void

    var body: some View {
        let title = Text("Cancel invoice ").font(EirsFont.serif(26)).foregroundColor(eirs.ink)
            + Text(number).font(EirsFont.serif(26, italic: true)).foregroundColor(eirs.red)
        VStack(alignment: .leading, spacing: 8) {
            title
                .padding(.horizontal, 6)
                .frame(maxWidth: .infinity, alignment: .leading)
            Text("MoR records this against the original IRN. It can't be undone.")
                .font(EirsFont.sans(13))
                .foregroundStyle(eirs.muted)
                .lineSpacing(3)
                .padding(.horizontal, 6)
                .padding(.bottom, 8)
            ForEach(CancelReason.allCases) { option in
                reasonRow(option)
            }
            PrimaryButton("Cancel with MoR", height: 50, fontSize: 15, showArrow: false,
                          container: eirs.red, pressedColor: eirs.redInk, action: onConfirm)
            .padding(.top, 8)
        }
    }

    private func reasonRow(_ option: CancelReason) -> some View {
        let on = option == reason
        let shape = RoundedRectangle(cornerRadius: 12, style: .continuous)
        return Button { reason = option } label: {
            HStack(spacing: 12) {
                MonoText(text: option.code, size: 11, color: eirs.muted)
                Text(option.label)
                    .font(EirsFont.sans(15))
                    .frame(maxWidth: .infinity, alignment: .leading)
                if on { LineIcon(EirsIcons.check, size: 16, strokeWidth: 2.6, tint: eirs.red) }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 13)
            .background(on ? eirs.redTint : Color.white, in: shape)
            .overlay(shape.strokeBorder(on ? eirs.redBorder : eirs.line, lineWidth: 1))
            .contentShape(Rectangle())
        }
        .buttonStyle(PlainTapStyle())
    }
}

// MARK: - Body

private struct InvoiceDetailBody: View {
    @Environment(\.eirs) private var eirs
    let inv: Invoice
    let amended: Invoice?
    let copied: Bool
    let onCopy: () -> Void

    var body: some View {
        let totals = inv.totals
        VStack(spacing: 12) {
            canceledBanner
            totalCard(totals)
            proofCard
            partyCard(role: "SELLER", party: inv.seller, rows: sellerRows)
            partyCard(role: "BUYER · \(inv.transactionType.rawValue)", party: inv.buyer, rows: buyerRows)
            lineItemsCard(totals)
            KeyValueCard(title: "DOCUMENT", rows: [
                ("Document no.", inv.number),
                ("Counter", inv.counter),
                ("Type", "\(inv.documentType.rawValue) · \(inv.documentType.displayName)"),
                ("Transaction", inv.transactionType.rawValue),
                ("Currency", "ETB"),
                ("Status", inv.status.rawValue)
            ])
            amendsCard
            KeyValueCard(title: "SOURCE SYSTEM", rows: [
                ("Branch", inv.branch.displayName),
                ("System type", inv.sourceSystem.type),
                ("System no.", inv.sourceSystem.number)
            ])
            KeyValueCard(title: "PAYMENT", rows: [
                ("Mode", inv.paymentMode.rawValue),
                ("Term", inv.paymentTerm.rawValue)
            ])
        }
    }

    // MARK: Canceled banner

    @ViewBuilder private var canceledBanner: some View {
        if inv.isCanceled, let cancellation = inv.cancellation {
            let shape = RoundedRectangle(cornerRadius: 14, style: .continuous)
            HStack(alignment: .top, spacing: 12) {
                LineIcon(EirsIcons.ban, size: 16, tint: .white)
                    .frame(width: 34, height: 34)
                    .background(eirs.red, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                VStack(alignment: .leading, spacing: 3) {
                    Text(cancellation.reason.label).font(EirsFont.sans(15, .bold)).foregroundStyle(eirs.red)
                    Text("Code \(cancellation.reason.code) · canceled \(Fmt.dateTime(cancellation.canceledAt)) · was \(cancellation.originalStatus.rawValue). The IRN stays on record as proof.")
                        .font(EirsFont.sans(12))
                        .foregroundStyle(eirs.redInk)
                        .lineSpacing(3)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(14)
            .background(eirs.redTint, in: shape)
            .overlay(shape.strokeBorder(eirs.redBorder, lineWidth: 1))
        }
    }

    // MARK: Grand total

    private func totalCard(_ totals: DocumentTotals) -> some View {
        EirsCard(spacing: 6) {
            Text("Grand total · \(inv.branch.displayName)").font(EirsFont.sans(12)).foregroundStyle(eirs.muted)
            Text(Fmt.etb(totals.grandTotal))
                .font(EirsFont.serif(38))
                .strikethrough(inv.isCanceled)
            Text("Tax \(Fmt.f2(totals.taxTotal)) · \(Fmt.plural(totals.lines.count, "line")) · \(inv.sourceSystem.type) \(inv.sourceSystem.number)")
                .font(EirsFont.sans(12))
                .foregroundStyle(eirs.muted)
        }
    }

    // MARK: Proof of registration

    private var proofCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Eyebrow(text: "PROOF OF REGISTRATION", color: eirs.tint2, size: 10, tracking: 0.12)
                Spacer(minLength: 8)
                Text(inv.isCanceled ? "Canceled" : "Signed by MoR")
                    .font(EirsFont.sans(11, .semibold))
                    .foregroundStyle(Color.white)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(inv.isCanceled ? eirs.red : eirs.brand, in: Capsule())
            }
            HStack(spacing: 14) {
                QrCodeView(content: verificationURL(irn: inv.irn))
                    .padding(7)
                    .frame(width: 100, height: 100)
                    .background(Color.white, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
                verifyText
            }
            Text(inv.irn)
                .font(EirsFont.mono(11))
                .foregroundStyle(Color.white)
                .lineSpacing(5)
                .padding(10)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.white.opacity(0.07), in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                .overlay(RoundedRectangle(cornerRadius: 10, style: .continuous).strokeBorder(Color.white.opacity(0.14), lineWidth: 1))
            copyButton
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(eirs.ink, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private var verifyText: some View {
        let lead = Text("Scan to verify at ").font(EirsFont.sans(12)).foregroundColor(Color.white.opacity(0.8))
        let host = Text("portal.mor.gov.et").font(EirsFont.sans(12, .bold)).foregroundColor(Color.white)
        let tail = Text(". Anyone can confirm MoR signed it.").font(EirsFont.sans(12)).foregroundColor(Color.white.opacity(0.8))
        return (lead + host + tail)
            .lineSpacing(4)
            .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var copyButton: some View {
        HStack(spacing: 6) {
            LineIcon(EirsIcons.copy, size: 13, tint: .white)
            Text(copied ? "Copied" : "Copy IRN").font(EirsFont.sans(13, .semibold)).foregroundStyle(Color.white)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 40)
        .background(Color.white.opacity(0.1), in: RoundedRectangle(cornerRadius: 10, style: .continuous))
        .contentShape(Rectangle())
        .onTapGesture { onCopy() }
    }

    // MARK: Parties

    private var sellerRows: [(String, String)] {
        let s = inv.seller
        return [
            ("TIN", s.tin ?? "—"), ("VAT number", s.vatNumber ?? "—"), ("Address", s.address ?? "—"),
            ("Phone", s.phone ?? "—"), ("Email", s.email ?? "—")
        ]
    }

    private var buyerRows: [(String, String)] {
        let b = inv.buyer
        if let tin = b.tin {
            return [
                ("TIN", tin), ("VAT number", b.vatNumber ?? "—"), ("Address", b.address ?? "—"),
                ("Phone", b.phone ?? "—"), ("Email", b.email ?? "—")
            ]
        }
        return [
            ("ID type", b.idType?.rawValue ?? "Walk-in"), ("ID number", b.idNumber ?? "—"), ("Phone", b.phone ?? "—")
        ]
    }

    private func partyCard(role: String, party: Party, rows: [(String, String)]) -> some View {
        EirsCard(padding: EdgeInsets(top: 14, leading: 16, bottom: 14, trailing: 16), spacing: 6) {
            Eyebrow(text: role, color: eirs.muted, size: 10, tracking: 0.12)
            Text(party.name).font(EirsFont.serif(22)).padding(.bottom, 4)
            ForEach(rows.indices, id: \.self) { index in
                KeyValueRow(key: rows[index].0, value: rows[index].1)
            }
        }
    }

    // MARK: Line items

    private func lineItemsCard(_ totals: DocumentTotals) -> some View {
        let lineCount = Fmt.plural(totals.lines.count, "line")
        return VStack(spacing: 0) {
            HStack {
                Text("Line items").font(EirsFont.sans(15, .semibold))
                Spacer(minLength: 8)
                Text(lineCount).font(EirsFont.sans(12)).foregroundStyle(eirs.muted)
            }
            .padding(.leading, 16).padding(.trailing, 16).padding(.top, 14).padding(.bottom, 8)
            ForEach(totals.lines, id: \.number) { line in
                Hairline()
                lineRow(line)
            }
            Hairline()
            totalsBlock(totals)
        }
        .frame(maxWidth: .infinity)
        .background(Color.white, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .solidBorder(eirs.line, radius: 16)
        .clipRounded(16)
    }

    private func lineRow(_ line: LineTotals) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(alignment: .top, spacing: 10) {
                Text(line.item.description)
                    .font(EirsFont.sans(14, .medium))
                    .frame(maxWidth: .infinity, alignment: .leading)
                MonoText(text: Fmt.f2(line.total), size: 13, weight: .medium, lineLimit: 1)
            }
            HStack(spacing: 8) {
                MonoText(text: "\(invQtyText(line.item.quantity)) \(line.item.unit) × \(Fmt.f2(line.item.unitPrice))", size: 11, color: eirs.muted)
                    .frame(maxWidth: .infinity, alignment: .leading)
                HStack(spacing: 6) {
                    CodeTag(text: line.item.taxCode.rawValue)
                    Text("tax \(Fmt.f2(line.tax))").font(EirsFont.sans(12)).foregroundStyle(eirs.muted)
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func totalsBlock(_ totals: DocumentTotals) -> some View {
        VStack(spacing: 6) {
            HStack {
                Text("Taxable subtotal").font(EirsFont.sans(13)).foregroundStyle(eirs.muted)
                Spacer(minLength: 8)
                MonoText(text: Fmt.f2(totals.subtotal), size: 13)
            }
            ForEach(totals.taxes, id: \.code) { tax in
                HStack(spacing: 8) {
                    taxLabel(tax)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    MonoText(text: Fmt.f2(tax.tax), size: 13)
                }
            }
            VStack(spacing: 8) {
                Hairline()
                HStack(alignment: .bottom) {
                    Text("Grand total").font(EirsFont.sans(13, .bold)).padding(.bottom, 3)
                    Spacer(minLength: 8)
                    Text(Fmt.etb(totals.grandTotal))
                        .font(EirsFont.serif(24))
                        .strikethrough(inv.isCanceled)
                }
            }
            .padding(.top, 2)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity)
        .background(eirs.background)
    }

    private func taxLabel(_ tax: TaxLine) -> Text {
        let name = Text(tax.code.label + " ").font(EirsFont.sans(13)).foregroundColor(eirs.muted)
        let base = Text("on " + Fmt.f2(tax.taxable)).font(EirsFont.mono(11)).foregroundColor(eirs.muted)
        return name + base
    }

    // MARK: Amends

    @ViewBuilder private var amendsCard: some View {
        if let amendment = inv.amendment {
            let previous: String = amended.map { "ETH-" + String($0.irn.prefix(10)) + "…" } ?? amendment.previousInvoiceId
            KeyValueCard(title: inv.documentType.isRetainerFollowUp ? "AGAINST RETAINER" : "AMENDS", rows: [
                ("Reason", amendment.reason),
                ("Previous IRN", previous),
                ("Related doc", amended?.displayNumber ?? "—")
            ])
        }
    }
}
