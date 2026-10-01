import SwiftUI
import EIRSDomain

/// 80mm thermal-style printable receipt of an invoice.
@MainActor
struct InvoicePrintScreen: View {
    @Environment(\.eirs) private var eirs
    @State private var model: InvoiceDetailModel
    var onBack: () -> Void

    init(container: AppContainer, id: String, onBack: @escaping () -> Void) {
        _model = State(initialValue: InvoiceDetailModel(container: container, id: id))
        self.onBack = onBack
    }

    var body: some View {
        VStack(spacing: 0) {
            ViewHeader(eyebrow: "", onBack: onBack, title: {
                Text("Receipt \(model.invoice?.number ?? "")")
                    .font(EirsFont.sans(16, .semibold))
                    .lineLimit(1)
            }, trailing: {
                // Download / share the receipt as PDF is not wired yet.
                Button { } label: {
                    LineIcon(EirsIcons.download, size: 17, tint: .white)
                        .frame(width: 40, height: 40)
                        .background(eirs.brand, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                        .contentShape(Rectangle())
                }
                .buttonStyle(PlainTapStyle())
            })
            ScrollView {
                VStack(spacing: 0) {
                    if let inv = model.invoice {
                        InvoicePaper(inv: inv)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 18)
                .padding(.bottom, 40)
            }
        }
        .background(eirs.paperBackdrop.ignoresSafeArea())
        .task { await model.observe() }
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
    }
}

private struct InvoicePaper: View {
    @Environment(\.eirs) private var eirs
    let inv: Invoice

    private var title: String {
        if inv.documentType == .cre { return "Credit note · VAT" }
        if inv.transactionType.isConsumer { return "Cash sales invoice · VAT" }
        return "Tax invoice · VAT"
    }

    private func subtitle(_ totals: DocumentTotals) -> String {
        let excise = totals.taxes.contains { $0.code == .exc10 }
        return "የሽያጭ ደረሰኝ · " + inv.transactionType.rawValue + (excise ? " · EXCISE" : "")
    }

    var body: some View {
        let totals = inv.totals
        ZStack {
            VStack(spacing: 14) {
                sellerHeader
                titleBlock(totals)
                detailGrid
                itemsBlock(totals)
                totalsBlock(totals)
                qrBlock
            }
            .padding(.leading, 16).padding(.trailing, 16).padding(.top, 20).padding(.bottom, 24)
            .frame(maxWidth: .infinity)
            if inv.isCanceled { watermark }
        }
        .background(Color.white)
        .clipped()
        .shadow(color: eirs.ink.opacity(0.25), radius: 16, x: 0, y: 6)
    }

    private var watermark: some View {
        Text("CANCELED")
            .font(EirsFont.serif(64))
            .foregroundStyle(eirs.red)
            .lineLimit(1)
            .fixedSize()
            .rotationEffect(.degrees(-20))
            .opacity(0.14)
            .offset(y: -20)
            .allowsHitTesting(false)
    }

    // MARK: Sections

    private var sellerHeader: some View {
        let s = inv.seller
        return VStack(spacing: 0) {
            VStack(spacing: 3) {
                Text(s.name).font(EirsFont.sans(14, .bold)).multilineTextAlignment(.center)
                mutedText("TIN \(s.tin ?? "—") · VAT \(s.vatNumber ?? "—")")
                if let address = s.address { mutedText(address) }
            }
            .frame(maxWidth: .infinity)
            .padding(.bottom, 12)
            DashedRule()
        }
    }

    private func titleBlock(_ totals: DocumentTotals) -> some View {
        VStack(spacing: 2) {
            Text(title).font(EirsFont.serif(20)).multilineTextAlignment(.center)
            Text(subtitle(totals))
                .font(EirsFont.mono(10))
                .tracking(0.8)
                .foregroundStyle(eirs.muted)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
    }

    private var detailGrid: some View {
        VStack(spacing: 3) {
            gridRow("የሰነድ ቁጥር / No", inv.number, mono: true)
            gridRow("ቀን / Date", Fmt.isoDate(inv.issuedAt), mono: true)
            gridRow("ሰዓት / Time", Fmt.time(inv.issuedAt) + ":00", mono: true)
            gridRow("ገዢ / Buyer", inv.buyer.name, mono: false)
        }
    }

    private func gridRow(_ label: String, _ value: String, mono: Bool) -> some View {
        HStack(alignment: .top, spacing: 10) {
            mutedText(label)
            Text(value)
                .font(mono ? EirsFont.mono(11, .medium) : EirsFont.sans(11, .bold))
                .multilineTextAlignment(.trailing)
                .frame(maxWidth: .infinity, alignment: .trailing)
        }
    }

    private func itemsBlock(_ totals: DocumentTotals) -> some View {
        VStack(spacing: 0) {
            DashedRule()
            VStack(spacing: 8) {
                ForEach(totals.lines, id: \.number) { line in
                    VStack(alignment: .leading, spacing: 2) {
                        Text(line.item.description).font(EirsFont.sans(11))
                        HStack(alignment: .bottom) {
                            Text("\(invQtyText(line.item.quantity)) × \(Fmt.f2(line.item.unitPrice)) · \(line.item.taxCode.rawValue)")
                                .font(EirsFont.mono(10))
                                .foregroundStyle(eirs.muted)
                                .frame(maxWidth: .infinity, alignment: .leading)
                            Text(Fmt.f2(line.total)).font(EirsFont.mono(11, .medium))
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
            .padding(.vertical, 8)
            DashedRule()
        }
    }

    private func totalsBlock(_ totals: DocumentTotals) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            amountRow("Taxable total", Fmt.f2(totals.subtotal))
            ForEach(totals.taxes, id: \.code) { tax in
                amountRow(tax.code.label, Fmt.f2(tax.tax))
            }
            HStack {
                Text("ጠቅላላ / Total").font(EirsFont.sans(14, .bold))
                Spacer(minLength: 8)
                Text(Fmt.etb(totals.grandTotal)).font(EirsFont.mono(14, .medium))
            }
            .padding(.top, 6)
            .overlay(alignment: .top) { Rectangle().fill(eirs.ink).frame(height: 1.5) }
            .padding(.top, 2)
            Text(AmountInWords.birr(totals.grandTotal))
                .font(EirsFont.serif(13, italic: true))
                .foregroundStyle(eirs.muted)
                .lineSpacing(3)
        }
    }

    private func amountRow(_ label: String, _ value: String) -> some View {
        HStack {
            mutedText(label)
            Spacer(minLength: 8)
            Text(value).font(EirsFont.mono(11))
        }
    }

    private var qrBlock: some View {
        let payment = Text("የክፍያ ሁኔታ / Payment · ").font(EirsFont.sans(11)).foregroundColor(eirs.muted)
            + Text(inv.paymentMode.rawValue).font(EirsFont.sans(11, .bold)).foregroundColor(eirs.ink)
        return VStack(spacing: 0) {
            DashedRule()
            VStack(spacing: 8) {
                QrCodeView(content: verificationURL(irn: inv.irn))
                    .padding(6)
                    .frame(width: 120, height: 120)
                    .background(Color.white, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
                Text("IRN \(inv.irn)")
                    .font(EirsFont.mono(9))
                    .lineSpacing(4)
                    .multilineTextAlignment(.center)
                payment.multilineTextAlignment(.center)
                mutedText("Verify at portal.mor.gov.et").multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity)
            .padding(.top, 12)
        }
    }

    private func mutedText(_ text: String) -> Text {
        Text(text).font(EirsFont.sans(11)).foregroundColor(eirs.muted)
    }
}
