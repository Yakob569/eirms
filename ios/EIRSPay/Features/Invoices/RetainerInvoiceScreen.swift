import SwiftUI
import EIRSDomain

/// Interim / final invoice against a retainer: the full invoice form with only the RTN reference and
/// document type locked. Buyer is pre-filled from the retainer but every field stays editable.
@MainActor @Observable
final class RetainerInvoiceModel {
    var retainer: Invoice?
    var name = ""
    var tin = ""
    var vat = ""
    var phone = ""
    var email = ""
    var region = ""
    var city = ""
    var wereda = ""
    var lines: [InvLineDraft] = []
    var paymentMode: InvoicePaymentMode = .bankTransfer
    var paymentTerm: PaymentTerm = .net30
    var loading: LoaderMessage?

    let retainerId: String
    let kind: DocumentType
    @ObservationIgnored private let container: AppContainer
    @ObservationIgnored private var nextKey = 0

    init(container: AppContainer, retainerId: String, kind: DocumentType) {
        self.container = container
        self.retainerId = retainerId
        self.kind = kind
    }

    func observe() async {
        for await all in container.observeInvoices() {
            if retainer == nil, let found = all.first(where: { $0.id == retainerId }) { prefill(found) }
        }
    }

    /// Convenience defaults from the retainer; nothing here is enforced.
    private func prefill(_ rtn: Invoice) {
        retainer = rtn
        let b = rtn.buyer
        name = b.name
        tin = b.tin ?? ""
        vat = b.vatNumber ?? ""
        phone = b.phone ?? ""
        email = b.email ?? ""
        region = "Addis Ababa"
        city = "Addis Ababa"
        wereda = String((b.address ?? "").filter(\.isNumber))
        paymentMode = rtn.paymentMode
        paymentTerm = rtn.paymentTerm
        lines = rtn.items.map { item in
            var line = InvLineDraft(id: takeKey())
            line.description = item.description
            line.quantity = invQtyText(item.quantity)
            line.price = NSDecimalNumber(decimal: item.unitPrice).stringValue
            line.taxCode = item.taxCode
            line.itemCode = item.itemCode
            line.nature = item.nature
            line.unit = item.unit
            return line
        }
        if lines.isEmpty { lines = [InvLineDraft(id: takeKey())] }
    }

    private func takeKey() -> Int {
        nextKey += 1
        return nextKey
    }

    // MARK: Lines

    func replaceLine(_ line: InvLineDraft) {
        if let index = lines.firstIndex(where: { $0.id == line.id }) { lines[index] = line }
    }

    func addLine() { lines.append(InvLineDraft(id: takeKey())) }

    func removeLine(_ id: Int) {
        if lines.count > 1 { lines.removeAll { $0.id == id } }
    }

    // MARK: Derived

    var buyer: Party {
        func opt(_ s: String) -> String? {
            let t = s.trimmingCharacters(in: .whitespaces)
            return t.isEmpty ? nil : t
        }
        let address = [opt(city), opt(wereda).map { "Wereda \($0)" }].compactMap { $0 }.joined(separator: " · ")
        return Party(name: name.trimmingCharacters(in: .whitespaces), tin: opt(tin), vatNumber: opt(vat),
                     phone: opt(phone), email: opt(email), address: address.isEmpty ? nil : address)
    }

    var items: [LineItem] {
        lines.map { line in
            LineItem(description: line.description.trimmingCharacters(in: .whitespaces),
                     itemCode: line.itemCode ?? "ITM-NEW", nature: line.nature,
                     unit: line.unit.isEmpty ? "PCS" : line.unit,
                     quantity: line.qty > 0 ? line.qty : 1, unitPrice: line.unitPrice.rounded(), taxCode: line.taxCode)
        }
    }

    /// Live totals over every row (empty rows count as zero).
    var totals: DocumentTotals {
        TaxCalculator.calculate(lines.map { line in
            LineItem(description: line.description, itemCode: line.itemCode ?? "", nature: line.nature, unit: line.unit,
                     quantity: line.qty, unitPrice: line.unitPrice, taxCode: line.taxCode)
        })
    }

    var issues: [RetainerIssue] {
        guard let retainer else { return [.invalidSource] }
        return RetainerInvoice.issues(kind: kind, retainer: retainer, buyer: buyer, items: items)
    }

    var valid: Bool { issues.isEmpty }

    var hint: String {
        let list = issues
        if list.isEmpty { return "Ready to register. MoR returns an IRN and signed QR." }
        if list.contains(.missingBuyerName) { return "Add the buyer\u{2019}s legal name." }
        if list.contains(.noPricedLines) { return "Add at least one priced line item." }
        return "This retainer can\u{2019}t be invoiced against."
    }

    func submit() async -> String? {
        guard let retainer, valid, loading == nil else { return nil }
        loading = LoaderMessage(title: "Registering with EIRS",
                                subtitle: "Waiting for MoR to sign the \(kind.displayName.lowercased())")
        let created = try? await container.registerRetainerInvoice(kind: kind, retainer: retainer, buyer: buyer,
                                                                    paymentMode: paymentMode, paymentTerm: paymentTerm, items: items)
        loading = nil
        return created?.id
    }
}

@MainActor
struct RetainerInvoiceScreen: View {
    @Environment(\.eirs) private var eirs
    @State private var model: RetainerInvoiceModel
    var onClose: () -> Void
    var onRegistered: (String) -> Void

    init(container: AppContainer, retainerId: String, kind: DocumentType, onClose: @escaping () -> Void,
         onRegistered: @escaping (String) -> Void) {
        _model = State(initialValue: RetainerInvoiceModel(container: container, retainerId: retainerId, kind: kind))
        self.onClose = onClose
        self.onRegistered = onRegistered
    }

    private var isInterim: Bool { model.kind == .int }

    var body: some View {
        let srcNo = model.retainer?.displayNumber ?? ""
        LoadingHost(loading: model.loading) {
            ViewScaffold(
                header: {
                    ViewHeader(eyebrow: (isInterim ? "INTERIM INVOICE" : "FINAL INVOICE") + " · AGAINST " + srcNo,
                               onBack: onClose, backIcon: EirsIcons.close) {
                        ViewTitle(plain: model.kind.displayName)
                    }
                },
                bottomBar: { footer },
                content: {
                    if let retainer = model.retainer {
                        lockedChips(retainer)
                        buyerCard
                        itemsCard
                        paymentCard
                        totalsCard
                    }
                })
        }
        .task { await model.observe() }
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
    }

    // MARK: Locked reference

    private func lockedChips(_ retainer: Invoice) -> some View {
        Wrap {
            lockedChip("Retainer", retainer.displayNumber, locked: true)
            lockedChip("Document type", model.kind.rawValue, locked: true)
            lockedChip("Transaction", retainer.transactionType.rawValue, locked: false)
        }
    }

    private func lockedChip(_ label: String, _ value: String, locked: Bool) -> some View {
        HStack(spacing: 6) {
            if locked { LineIcon(EirsIcons.lock, size: 12, tint: eirs.muted) }
            Text(label).font(EirsFont.sans(12)).foregroundStyle(eirs.muted)
            Text(value).font(EirsFont.mono(12, .medium)).foregroundStyle(eirs.ink)
        }
        .padding(.horizontal, 12)
        .frame(height: 34)
        .background(Color.white, in: Capsule())
        .overlay(Capsule().strokeBorder(eirs.line, lineWidth: 1))
    }

    // MARK: 1 Buyer

    private var buyerCard: some View {
        EirsCard(spacing: 12) {
            StepHeading(number: 1, title: "Buyer") {
                Text("From the retainer").font(EirsFont.sans(11)).foregroundStyle(eirs.muted)
            }
            SettingsField(label: "Legal name *", text: $model.name, fontSize: 16)
            SettingsFieldGrid {
                SettingsField(label: "TIN", text: $model.tin, mono: true, keyboard: .numberPad)
                SettingsField(label: "VAT number", text: $model.vat, mono: true)
                SettingsField(label: "Phone", text: $model.phone, mono: true, keyboard: .phonePad)
                SettingsField(label: "Email", text: $model.email, keyboard: .emailAddress)
                SettingsField(label: "Region", text: $model.region)
                SettingsField(label: "City", text: $model.city)
                SettingsField(label: "Wereda", text: $model.wereda, mono: true)
            }
        }
    }

    // MARK: 2 Items (shared line editor)

    private var itemsCard: some View {
        EirsCard(spacing: 12) {
            StepHeading(number: 2, title: "Items")
            ForEach(Array(model.lines.enumerated()), id: \.element.id) { offset, line in
                InvLineEditor(line: lineBinding(line.id), number: offset + 1, canRemove: model.lines.count > 1,
                              onRemove: { model.removeLine(line.id) })
            }
            InvAddLineButton { model.addLine() }
        }
    }

    private func lineBinding(_ id: Int) -> Binding<InvLineDraft> {
        Binding<InvLineDraft>(
            get: { model.lines.first { $0.id == id } ?? InvLineDraft(id: id) },
            set: { model.replaceLine($0) })
    }

    // MARK: 3 Payment

    private var paymentCard: some View {
        EirsCard(spacing: 12) {
            StepHeading(number: 3, title: "Payment")
            Text("Mode").font(EirsFont.sans(13, .medium))
            Wrap {
                ForEach(InvoicePaymentMode.allCases) { mode in
                    EirsChip(label: mode.displayName, selected: model.paymentMode == mode, height: 40, radius: 11,
                             fontSize: 14, horizontalPadding: 14) {
                        model.paymentMode = mode
                    }
                }
            }
            Text("Term").font(EirsFont.sans(13, .medium))
            Wrap {
                ForEach(PaymentTerm.allCases) { term in
                    EirsChip(label: term.displayName, selected: model.paymentTerm == term, height: 40, radius: 11,
                             fontSize: 14, horizontalPadding: 14) {
                        model.paymentTerm = term
                    }
                }
            }
        }
    }

    private var totalsCard: some View {
        let totals = model.totals
        var rows: [(String, String)] = [("Taxable", Fmt.f2(totals.subtotal))]
        for tax in totals.taxes { rows.append((tax.code.label, Fmt.f2(tax.tax))) }
        return SettingsRowsCard(rows: rows, monoValues: true)
    }

    // MARK: Footer

    private var footer: some View {
        let billTo = model.name.trimmingCharacters(in: .whitespaces)
        return StickyBottomBar(spacing: 8) {
            HStack(alignment: .bottom, spacing: 10) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Bill to " + (billTo.isEmpty ? "—" : billTo)).font(EirsFont.sans(12)).foregroundStyle(eirs.muted).lineLimit(1)
                    HStack(spacing: 6) {
                        Circle().fill(model.valid ? eirs.brand : eirs.pending).frame(width: 6, height: 6)
                        Text(model.hint).font(EirsFont.sans(11)).foregroundStyle(eirs.muted).lineLimit(1)
                    }
                }
                Spacer(minLength: 8)
                Text(Fmt.etb(model.totals.grandTotal)).font(EirsFont.serif(28)).lineLimit(1).minimumScaleFactor(0.7)
            }
            PrimaryButton("Submit " + model.kind.displayName.lowercased(), enabled: model.valid, height: 52, fontSize: 15) {
                Task { if let id = await model.submit() { onRegistered(id) } }
            }
        }
    }
}
