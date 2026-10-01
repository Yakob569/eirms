import SwiftUI
import UIKit
import EIRSDomain

@MainActor @Observable
final class ReceiptDetailModel {
    private let container: AppContainer
    private let receiptId: String
    var view: ReceiptView?

    init(container: AppContainer, id: String) {
        self.container = container
        self.receiptId = id
    }

    func observe() async {
        for await views in container.observeReceiptViews() {
            view = views.first { $0.receipt.id == receiptId }
        }
    }
}

@MainActor
struct ReceiptDetailScreen: View {
    @Environment(\.eirs) private var eirs
    @State private var model: ReceiptDetailModel
    private let id: String
    private let onBack: () -> Void
    private let onOpenInvoice: (String) -> Void
    private let onOpenWithholding: (String) -> Void

    init(container: AppContainer, id: String, onBack: @escaping () -> Void,
         onOpenInvoice: @escaping (String) -> Void, onOpenWithholding: @escaping (String) -> Void) {
        _model = State(initialValue: ReceiptDetailModel(container: container, id: id))
        self.id = id
        self.onBack = onBack
        self.onOpenInvoice = onOpenInvoice
        self.onOpenWithholding = onOpenWithholding
    }

    var body: some View {
        Group {
            if let v = model.view {
                content(v)
            } else {
                ViewScaffold(
                    header: { ViewHeader(eyebrow: "RECEIPT", onBack: onBack, title: { ViewTitle(plain: id) }) },
                    content: { EmptyView() })
            }
        }
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
        .task { await model.observe() }
    }

    private func content(_ v: ReceiptView) -> some View {
        let r = v.receipt
        let tone = ReceiptStyle.tone(for: r.type, eirs)
        return ViewScaffold(
            padding: EdgeInsets(top: 16, leading: 16, bottom: 40, trailing: 16),
            header: {
                ViewHeader(
                    eyebrow: "RCP · \(r.type.displayName.uppercased()) · REGISTERED WITH EIRS",
                    onBack: onBack,
                    title: { ViewTitle(plain: ReceiptStyle.receiptNo(r)) },
                    trailing: { Badge(text: r.type.displayName, background: tone.bg, content: tone.fg) })
            },
            content: {
                amountCard(v)
                linkedInvoiceCard(v)
                lineItemsOrNote(v)
                ReceiptProofCard(irn: r.irn)
                buyerCard(v)
                documentCard(v)
            })
    }

    // MARK: Amount + payment

    private func amountCard(_ v: ReceiptView) -> some View {
        let r = v.receipt
        let refund = r.type == .refund
        let heading: String = (refund ? "REFUNDED" : "RECEIVED") + " · " + Fmt.dateTime(r.issuedAt)
        let amountText: String = (refund ? "−" : "") + Fmt.etb(v.amount)
        let rows = ReceiptDetailScreen.paymentRows(v)
        return EirsCard(spacing: 6) {
            ReceiptCardEyebrow(text: heading)
            Text(amountText).font(EirsFont.serif(38)).foregroundStyle(refund ? eirs.red : eirs.ink)
            VStack(spacing: 0) {
                ForEach(rows.indices, id: \.self) { index in
                    KeyValueRow(key: rows[index].0, value: rows[index].1)
                }
            }
            .padding(.top, 6)
        }
    }

    private static func paymentRows(_ v: ReceiptView) -> [(String, String)] {
        let r = v.receipt
        switch r.method {
        case .cash:
            return [("Method", "Cash"), ("Tendered", Fmt.f2(r.tendered ?? v.amount)), ("Change", Fmt.f2(v.change))]
        case .mobileMoney:
            return [("Method", "Mobile money · \(r.provider ?? "")"), ("Transaction ID", r.reference ?? "—")]
        case .bankTransfer:
            return [("Method", "Bank transfer"), ("Reference", r.reference ?? "—")]
        case .cheque:
            return [("Method", "Cheque"), ("Cheque no.", r.reference ?? "—")]
        case .card:
            return [("Method", "Card"), ("Card", r.reference ?? "—")]
        }
    }

    // MARK: Linked invoice

    @ViewBuilder
    private func linkedInvoiceCard(_ v: ReceiptView) -> some View {
        if let li = v.linkedInvoice {
            let st = ReceiptStyle.tone(for: li.status, eirs)
            Button { onOpenInvoice(li.id) } label: {
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        ReceiptCardEyebrow(text: "LINKED INVOICE")
                        Spacer(minLength: 8)
                        Badge(text: li.status.rawValue, background: st.bg, content: st.fg)
                    }
                    HStack(alignment: .lastTextBaseline) {
                        Text(ReceiptStyle.invoiceNo(li)).font(EirsFont.serif(26))
                        Spacer(minLength: 8)
                        MonoText(text: Fmt.etb(li.totals.grandTotal), size: 13)
                    }
                    Text("\(li.buyer.name) · \(Fmt.dateTime(li.issuedAt))")
                        .font(EirsFont.sans(13)).foregroundStyle(eirs.muted)
                    withheldPill(v)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 14)
                .frame(maxWidth: .infinity, alignment: .leading)
                .contentShape(Rectangle())
            }
            .buttonStyle(ListCardStyle())
        }
    }

    @ViewBuilder
    private func withheldPill(_ v: ReceiptView) -> some View {
        if let w = v.withholding {
            let text = "Buyer withheld \(Fmt.etb(v.withheldAmount)) (\(ReceiptStyle.percentText(w.rate))) · \(w.id) ↗"
            Text(text)
                .font(EirsFont.sans(12))
                .lineSpacing(3)
                .foregroundStyle(eirs.amberInk)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 11)
                .padding(.vertical, 9)
                .background(eirs.warnTint, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                .contentShape(Rectangle())
                .onTapGesture { onOpenWithholding(w.id) }
        }
    }

    // MARK: Line items or note

    @ViewBuilder
    private func lineItemsOrNote(_ v: ReceiptView) -> some View {
        if !v.totals.lines.isEmpty {
            lineItemsCard(v)
        } else {
            let note: String = v.receipt.type == .payment
                ? "VAT was registered on the linked invoice. This receipt records the payment only."
                : "Refund against the linked invoice. MoR reverses the tax on the refunded amount."
            Text(note)
                .font(EirsFont.sans(13))
                .lineSpacing(4)
                .foregroundStyle(eirs.brandDark)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 16)
                .padding(.vertical, 14)
                .background(eirs.tint1, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
    }

    private func lineItemsCard(_ v: ReceiptView) -> some View {
        let lines = v.totals.lines
        return EirsCard(padding: EdgeInsets()) {
            Text("Line items").font(EirsFont.sans(15, .semibold))
                .padding(.leading, 16).padding(.trailing, 16).padding(.top, 14).padding(.bottom, 8)
            ForEach(lines.indices, id: \.self) { index in
                lineRow(lines[index])
            }
            totalsBlock(v)
        }
    }

    private func lineRow(_ lt: LineTotals) -> some View {
        let item = lt.item
        let meta = "\(ReceiptStyle.qtyText(item.quantity)) \(item.unit) × \(Fmt.f2(item.unitPrice)) · \(item.itemCode)"
        return VStack(spacing: 0) {
            Hairline()
            VStack(alignment: .leading, spacing: 5) {
                HStack(alignment: .top, spacing: 10) {
                    Text(item.description).font(EirsFont.sans(14, .medium))
                        .frame(maxWidth: .infinity, alignment: .leading)
                    MonoText(text: Fmt.f2(lt.total), size: 13, lineLimit: 1)
                }
                MonoText(text: meta, size: 11, color: eirs.muted)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 11)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private func totalsBlock(_ v: ReceiptView) -> some View {
        let taxes = v.totals.taxes
        return VStack(spacing: 0) {
            Hairline()
            VStack(spacing: 5) {
                totalsLine("Taxable", Fmt.f2(v.totals.subtotal))
                ForEach(taxes.indices, id: \.self) { index in
                    totalsLine(taxes[index].code.label, Fmt.f2(taxes[index].tax))
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
        }
        .background(eirs.background)
    }

    private func totalsLine(_ label: String, _ value: String) -> some View {
        HStack {
            Text(label).font(EirsFont.sans(13)).foregroundStyle(eirs.muted)
            Spacer(minLength: 8)
            MonoText(text: value, size: 13)
        }
    }

    // MARK: Parties and document

    private func buyerCard(_ v: ReceiptView) -> some View {
        var rows: [(String, String)] = [("Name", v.buyer.name)]
        rows.append(contentsOf: ReceiptStyle.detailRows(v.buyer))
        return ReceiptRowsCard(title: "RECEIVED FROM", rows: rows)
    }

    private func documentCard(_ v: ReceiptView) -> some View {
        let r = v.receipt
        let rows: [(String, String)] = [
            ("Receipt no.", ReceiptStyle.receiptNo(r)), ("Type", r.type.displayName), ("Currency", "ETB"),
            ("Branch", r.branch.displayName), ("System no.", r.sourceSystem.number),
        ]
        return ReceiptRowsCard(title: "DOCUMENT", rows: rows)
    }
}

/// Dark "PROOF OF REGISTRATION" card with the real verification QR, the IRN and a copy button.
struct ReceiptProofCard: View {
    @Environment(\.eirs) private var eirs
    var irn: String
    @State private var copied = false

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Eyebrow(text: "PROOF OF REGISTRATION", color: eirs.tint2, size: 10, tracking: 0.12)
            HStack(spacing: 14) {
                QrCodeView(content: verificationURL(irn: irn))
                    .padding(7)
                    .frame(width: 96, height: 96)
                    .background(Color.white, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
                Text(irn)
                    .font(EirsFont.mono(10))
                    .lineSpacing(6)
                    .foregroundStyle(Color.white.opacity(0.85))
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            copyButton
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(eirs.ink, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .task(id: copied) {
            if copied {
                try? await Task.sleep(nanoseconds: 1_400_000_000)
                copied = false
            }
        }
    }

    private var copyButton: some View {
        Button {
            UIPasteboard.general.string = irn
            copied = true
        } label: {
            Text(copied ? "Copied" : "Copy IRN")
                .font(EirsFont.sans(13, .semibold))
                .foregroundStyle(Color.white)
                .frame(maxWidth: .infinity, minHeight: 40, maxHeight: 40)
                .contentShape(Rectangle())
        }
        .buttonStyle(PressFillStyle(normal: Color.white.opacity(0.1), pressed: Color.white.opacity(0.18), radius: 10))
    }
}
