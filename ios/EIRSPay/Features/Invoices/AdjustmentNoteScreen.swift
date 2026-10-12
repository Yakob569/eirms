import SwiftUI
import EIRSDomain

/// Quantity and price text for one ticked line.
struct AdjustmentLineText: Equatable {
    var quantity: String
    var price: String
}

/// Credit / debit note: a constrained copy of the original invoice. Seller, buyer and branch are locked;
/// only ticked lines get editable quantity and unit price, and a reason is required.
@MainActor @Observable
final class AdjustmentNoteModel {
    var original: Invoice?
    /// Ticked lines by index into the original's items.
    var selections: [Int: AdjustmentLineText] = [:]
    var reason = ""
    var loading: LoaderMessage?

    let invoiceId: String
    let kind: DocumentType
    @ObservationIgnored private let container: AppContainer

    init(container: AppContainer, invoiceId: String, kind: DocumentType) {
        self.container = container
        self.invoiceId = invoiceId
        self.kind = kind
    }

    func observe() async {
        for await all in container.observeInvoices() {
            if original == nil { original = all.first { $0.id == invoiceId } }
        }
    }

    var isCredit: Bool { kind == .cre }
    var name: String { kind.displayName }

    private static func parse(_ text: String) -> Decimal {
        Decimal(string: text.trimmingCharacters(in: .whitespaces), locale: Locale(identifier: "en_US_POSIX")) ?? 0
    }

    var adjustments: [Int: LineAdjustment] {
        selections.mapValues { LineAdjustment(quantity: Self.parse($0.quantity), unitPrice: Self.parse($0.price)) }
    }

    func toggle(_ index: Int) {
        guard let line = original?.items[safe: index] else { return }
        if selections[index] != nil {
            selections[index] = nil
        } else {
            selections[index] = AdjustmentLineText(quantity: invQtyText(line.quantity),
                                                   price: NSDecimalNumber(decimal: line.unitPrice).stringValue)
        }
    }

    func setQuantity(_ index: Int, _ value: String) { selections[index]?.quantity = value.decimalOnly }
    func setPrice(_ index: Int, _ value: String) { selections[index]?.price = value.decimalOnly }

    func exceeds(_ index: Int) -> Bool {
        guard let original, let adjustment = adjustments[index], let line = original.items[safe: index] else { return false }
        return AdjustmentNote.exceeds(kind: kind, original: line, adjustment: adjustment)
    }

    /// New line total including tax, for a ticked line.
    func newLineTotal(_ index: Int) -> Decimal {
        guard let original, let adjustment = adjustments[index], let line = original.items[safe: index] else { return 0 }
        let pre = (adjustment.quantity * adjustment.unitPrice).rounded()
        return pre + (pre * line.taxCode.rate).rounded()
    }

    var totals: DocumentTotals {
        guard let original else { return .empty }
        return AdjustmentNote.totals(original: original, adjustments: adjustments)
    }

    var issues: [AdjustmentIssue] {
        guard let original else { return [.invalidSource] }
        return AdjustmentNote.issues(kind: kind, original: original, adjustments: adjustments, reason: reason)
    }

    var valid: Bool { issues.isEmpty }

    var hint: String {
        let list = issues
        if list.isEmpty { return "Ready to register. MoR returns an IRN and signed QR." }
        if list.contains(.noLines) { return "Tick at least one line to adjust." }
        if list.contains(.missingReason) { return "Add a reason." }
        if list.contains(.invalidSource) { return "This invoice can\u{2019}t be amended." }
        return "Fix the highlighted line."
    }

    func submit() async -> String? {
        guard let original, valid, loading == nil else { return nil }
        loading = LoaderMessage(title: "Registering with EIRS", subtitle: "Waiting for MoR to sign the \(name.lowercased())")
        let created = try? await container.registerAdjustmentNote(kind: kind, original: original, adjustments: adjustments, reason: reason)
        loading = nil
        return created?.id
    }
}

extension Array {
    subscript(safe index: Int) -> Element? { indices.contains(index) ? self[index] : nil }
}

@MainActor
struct AdjustmentNoteScreen: View {
    @Environment(\.eirs) private var eirs
    @State private var model: AdjustmentNoteModel
    var onClose: () -> Void
    var onRegistered: (String) -> Void

    init(container: AppContainer, invoiceId: String, kind: DocumentType, onClose: @escaping () -> Void,
         onRegistered: @escaping (String) -> Void) {
        _model = State(initialValue: AdjustmentNoteModel(container: container, invoiceId: invoiceId, kind: kind))
        self.onClose = onClose
        self.onRegistered = onRegistered
    }

    var body: some View {
        let srcNo = model.original?.displayNumber ?? ""
        LoadingHost(loading: model.loading) {
            ViewScaffold(
                header: {
                    ViewHeader(eyebrow: (model.isCredit ? "CREDIT NOTE" : "DEBIT NOTE") + " · AMENDS " + srcNo,
                               onBack: onClose, backIcon: EirsIcons.close) {
                        ViewTitle(plain: model.name + " ", accent: srcNo)
                    }
                },
                bottomBar: { footer(srcNo) },
                content: {
                    if let original = model.original {
                        summaryCard(original)
                        HStack(alignment: .firstTextBaseline) {
                            Eyebrow(text: "LINES TO ADJUST", color: eirs.muted)
                            Spacer(minLength: 8)
                            Text("\(model.selections.count) of \(original.items.count) lines")
                                .font(EirsFont.sans(12)).foregroundStyle(eirs.muted)
                        }
                        .padding(.horizontal, 4)
                        .padding(.top, 8)
                        ForEach(original.items.indices, id: \.self) { index in
                            lineCard(index, original.items[index])
                        }
                        SettingsSectionLabel("REASON *")
                        reasonField
                        SettingsRowsCard(rows: [("Taxable", Fmt.f2(model.totals.subtotal)), ("Tax", Fmt.f2(model.totals.taxTotal))],
                                         monoValues: true)
                    }
                })
        }
        .task { await model.observe() }
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
    }

    // MARK: Locked summary

    private func summaryCard(_ inv: Invoice) -> some View {
        let buyerId: String = inv.buyer.tin.map { "TIN \($0)" }
            ?? inv.buyer.idType.map { "\($0.rawValue) \(inv.buyer.idNumber ?? "")" } ?? "Walk-in"
        let entries: [(String, String, String)] = [
            ("SELLER", inv.seller.name, "TIN " + (inv.seller.tin ?? "—")),
            ("BUYER", inv.buyer.name, buyerId),
            ("BRANCH", inv.branch.displayName, "\(inv.sourceSystem.type) \(inv.sourceSystem.number)"),
            ("ORIGINAL", Fmt.etb(inv.totals.grandTotal), "\(inv.transactionType.rawValue) · \(Fmt.isoDate(inv.issuedAt))"),
        ]
        return EirsCard(spacing: 12) {
            LazyVGrid(columns: [GridItem(.flexible(), spacing: 12, alignment: .topLeading),
                                GridItem(.flexible(), spacing: 12, alignment: .topLeading)],
                      alignment: .leading, spacing: 14) {
                ForEach(entries.indices, id: \.self) { i in
                    VStack(alignment: .leading, spacing: 3) {
                        MonoText(text: entries[i].0, size: 10, color: eirs.muted)
                        Text(entries[i].1).font(EirsFont.sans(14, .medium)).lineLimit(1)
                        MonoText(text: entries[i].2, size: 11, color: eirs.muted, lineLimit: 1)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
            HStack(spacing: 6) {
                LineIcon(EirsIcons.lock, size: 13, tint: eirs.muted)
                Text("Locked to the original invoice").font(EirsFont.sans(12)).foregroundStyle(eirs.muted)
            }
            .padding(.top, 10)
            .frame(maxWidth: .infinity, alignment: .leading)
            .overlay(alignment: .top) { Hairline() }
        }
    }

    // MARK: Lines

    private func lineCard(_ index: Int, _ line: LineItem) -> some View {
        let selection = model.selections[index]
        let on = selection != nil
        let shape = RoundedRectangle(cornerRadius: 16, style: .continuous)
        let originalTotal = (line.quantity * line.unitPrice).rounded()
        return VStack(alignment: .leading, spacing: 12) {
            Button { model.toggle(index) } label: {
                HStack(alignment: .top, spacing: 12) {
                    EirsCheckbox(checked: on).padding(.top, 1)
                    VStack(alignment: .leading, spacing: 3) {
                        Text(line.description).font(EirsFont.sans(14, .medium)).foregroundStyle(eirs.ink)
                            .multilineTextAlignment(.leading)
                        MonoText(text: "\(line.itemCode) · \(invQtyText(line.quantity)) \(line.unit) × \(Fmt.f2(line.unitPrice))",
                                 size: 11, color: eirs.muted)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    VStack(alignment: .trailing, spacing: 4) {
                        CodeTag(text: line.taxCode.rawValue)
                        MonoText(text: Fmt.f2(originalTotal + (originalTotal * line.taxCode.rate).rounded()), size: 12, color: eirs.muted)
                    }
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(PlainTapStyle())
            if let selection {
                HStack(alignment: .top, spacing: 10) {
                    LabeledField("Quantity (\(line.unit))", labelSize: 12, gap: 6) {
                        FormField(text: Binding(get: { selection.quantity }, set: { model.setQuantity(index, $0) }),
                                  mono: true, alignment: .trailing, keyboard: .decimalPad)
                    }
                    .lineLimit(1)
                    .frame(maxWidth: .infinity)
                    LabeledField("Unit price", labelSize: 12, gap: 6) {
                        FormField(text: Binding(get: { selection.price }, set: { model.setPrice(index, $0) }),
                                  mono: true, alignment: .trailing, keyboard: .decimalPad)
                    }
                    .frame(maxWidth: .infinity)
                    .layoutPriority(1)
                }
                HStack {
                    Text("New line total").lineLimit(1)
                    Spacer(minLength: 8)
                    Text(Fmt.etb(model.newLineTotal(index))).font(EirsFont.mono(13, .medium)).lineLimit(1)
                }
                .font(EirsFont.sans(13))
                .foregroundStyle(eirs.brandDark)
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .background(eirs.tint1, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                if model.exceeds(index) {
                    Text("A credit note can\u{2019}t exceed the original line.")
                        .font(EirsFont.sans(12, .medium)).foregroundStyle(eirs.red)
                }
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(on ? Color.white : eirs.background, in: shape)
        .overlay(shape.strokeBorder(on ? eirs.brand : eirs.line, lineWidth: 1.5))
    }

    private var reasonField: some View {
        TextField("", text: $model.reason, prompt: Text("e.g. Returned 1 unit, damaged in delivery").foregroundColor(eirs.placeholder),
                  axis: .vertical)
            .font(EirsFont.sans(16))
            .lineLimit(3, reservesSpace: true)
            .tint(eirs.brand)
            .padding(12)
            .background(Color.white, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            .solidBorder(eirs.line, radius: 14)
    }

    // MARK: Footer

    private func footer(_ srcNo: String) -> some View {
        let grand = model.totals.grandTotal
        return StickyBottomBar(spacing: 8) {
            HStack(alignment: .bottom, spacing: 10) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(model.isCredit ? "Reduces \(srcNo) by" : "Adds to \(srcNo)")
                        .font(EirsFont.sans(12)).foregroundStyle(eirs.muted).lineLimit(1)
                    HStack(spacing: 6) {
                        Circle().fill(model.valid ? eirs.brand : eirs.pending).frame(width: 6, height: 6)
                        Text(model.hint).font(EirsFont.sans(11)).foregroundStyle(eirs.muted).lineLimit(1)
                    }
                }
                Spacer(minLength: 8)
                Text((model.isCredit ? "\u{2212}" : "+") + Fmt.etb(grand))
                    .font(EirsFont.serif(28))
                    .foregroundStyle(model.isCredit ? eirs.red : eirs.ink)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
            }
            PrimaryButton("Submit " + model.name.lowercased(), enabled: model.valid, height: 52, fontSize: 15) {
                Task { if let id = await model.submit() { onRegistered(id) } }
            }
        }
    }
}
