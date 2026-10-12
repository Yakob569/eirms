import SwiftUI
import EIRSDomain

@MainActor
struct RegisterInvoiceScreen: View {
    @Environment(\.eirs) private var eirs
    @State private var model: RegisterInvoiceModel
    var onBack: () -> Void
    var onRegistered: (String) -> Void

    private static let docOptions: [SegmentOption<DocumentType>] = [
        SegmentOption(value: .inv, label: "Invoice"),
        SegmentOption(value: .cre, label: "Credit note"),
        SegmentOption(value: .deb, label: "Debit note")
    ]

    private static let ttOrder: [TransactionType] = [.b2c, .b2b, .b2g, .g2b, .g2c]

    init(container: AppContainer, onBack: @escaping () -> Void, onRegistered: @escaping (String) -> Void) {
        _model = State(initialValue: RegisterInvoiceModel(container: container))
        self.onBack = onBack
        self.onRegistered = onRegistered
    }

    private var docWord: String {
        switch model.documentType {
        case .inv: return "invoice."
        case .cre: return "credit note."
        case .deb: return "debit note."
        case .rtn: return "retainer."
        case .int: return "interim invoice."
        case .fin: return "final invoice."
        }
    }

    var body: some View {
        LoadingHost(loading: model.loading) {
            ViewScaffold(
                header: { header },
                bottomBar: { bottomBar },
                content: {
                    documentCard
                    buyerCard
                    linesCard
                    paymentCard
                })
        }
        .task { await model.observeCatalogItems() }
        .task { await model.observeInvoiceList() }
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
    }

    private func submit() {
        Task {
            if let newId = await model.submit() { onRegistered(newId) }
        }
    }

    // MARK: Header and bottom bar

    private var header: some View {
        ViewHeader(eyebrow: "NEW · " + model.branch.displayName.uppercased(), onBack: onBack) {
            ViewTitle(plain: "Register ", accent: docWord)
        }
    }

    private var bottomBar: some View {
        StickyBottomBar {
            HStack(alignment: .bottom) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Bill to \(model.billTo)").font(EirsFont.sans(12)).foregroundStyle(eirs.muted).lineLimit(1)
                    HStack(spacing: 6) {
                        Circle().fill(model.valid ? eirs.brand : eirs.pending).frame(width: 6, height: 6)
                        Text("\(model.documentType.rawValue) · \(model.transactionType.rawValue)")
                            .font(EirsFont.sans(11))
                            .foregroundStyle(eirs.muted)
                    }
                }
                Spacer(minLength: 8)
                Text(Fmt.etb(model.grandTotal)).font(EirsFont.serif(28)).lineLimit(1)
            }
            PrimaryButton("Register with MoR", enabled: model.valid, height: 50, fontSize: 15) { submit() }
        }
    }

    // MARK: Step 1 - Document

    private var documentCard: some View {
        EirsCard(spacing: 14) {
            StepHeading(number: 1, title: "Document")
            SegmentedControl(options: Self.docOptions, selection: $model.documentType)
            ChipRail(spacing: 8) {
                ForEach(Self.ttOrder) { tt in
                    transactionCard(tt)
                }
            }
            Text(model.consumer
                 ? "Consumer sale: buyer ID is optional for walk-in customers."
                 : "Business or government sale: buyer TIN and legal name are required by MoR.")
                .font(EirsFont.sans(12))
                .foregroundStyle(eirs.muted)
                .lineSpacing(3)
            if model.documentType.isAmendment { amendPanel }
        }
    }

    private func transactionCard(_ tt: TransactionType) -> some View {
        let on = tt == model.transactionType
        let shape = RoundedRectangle(cornerRadius: 12, style: .continuous)
        return Button { model.transactionType = tt } label: {
            VStack(alignment: .leading, spacing: 3) {
                MonoText(text: tt.rawValue, size: 15, weight: .medium)
                Text(tt.displayName)
                    .font(EirsFont.sans(11))
                    .foregroundStyle(eirs.muted)
                    .lineLimit(2, reservesSpace: true)
                    .multilineTextAlignment(.leading)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .frame(width: 112, alignment: .leading)
            .background(on ? eirs.tint1 : Color.white, in: shape)
            .overlay(shape.strokeBorder(on ? eirs.brand : eirs.line, lineWidth: 1.5))
            .contentShape(Rectangle())
        }
        .buttonStyle(PlainTapStyle())
    }

    private var amendPanel: some View {
        VStack(spacing: 10) {
            LabeledField("Previous IRN", labelSize: 13, gap: 6) {
                FormField(text: $model.previousIrn, placeholder: "IRN being amended", restingBackground: Color.white)
            }
            LabeledField("Reason", labelSize: 13, gap: 6) {
                FormField(text: $model.amendReason, placeholder: "e.g. Returned goods", restingBackground: Color.white)
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity)
        .background(eirs.tint1, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    // MARK: Step 2 - Buyer

    private var buyerCard: some View {
        EirsCard(spacing: 14) {
            StepHeading(number: 2, title: "Buyer") {
                Text(model.consumer ? "Consumer · light ID" : "Business · TIN required")
                    .font(EirsFont.sans(11, .semibold))
                    .foregroundStyle(eirs.brandDark)
                    .lineLimit(1)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(eirs.tint1, in: Capsule())
            }
            if model.consumer {
                consumerBuyer
            } else {
                businessBuyer
            }
        }
    }

    @ViewBuilder private var consumerBuyer: some View {
        walkInToggle
        if !model.walkIn {
            Wrap {
                ForEach(BuyerIdType.allCases) { type in
                    EirsChip(label: type.rawValue, selected: model.idType == type, height: 34, radius: 9,
                             mono: true, fontSize: 12, horizontalPadding: 11) {
                        model.idType = type
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            LabeledField("Full name", labelSize: 13, gap: 6) {
                FormField(text: $model.name, placeholder: "Selam Bekele")
            }
            LabeledField("\(model.idType.rawValue) number", labelSize: 13, gap: 6) {
                FormField(text: $model.idNumber, placeholder: "ID number")
            }
        }
    }

    private var walkInToggle: some View {
        Button { model.walkIn.toggle() } label: {
            HStack(spacing: 12) {
                EirsSwitch(isOn: model.walkIn, small: true) { model.walkIn.toggle() }
                VStack(alignment: .leading, spacing: 2) {
                    Text("Walk-in customer").font(EirsFont.sans(14, .medium))
                    Text("No buyer ID needed").font(EirsFont.sans(12)).foregroundStyle(eirs.muted)
                }
                Spacer(minLength: 0)
            }
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .solidBorder(eirs.line, radius: 12)
            .contentShape(Rectangle())
        }
        .buttonStyle(PlainTapStyle())
    }

    @ViewBuilder private var businessBuyer: some View {
        HStack(alignment: .bottom, spacing: 8) {
            LabeledField("Buyer TIN *", labelSize: 13, gap: 6) {
                FormField(text: tinBinding, placeholder: "10-digit TIN", keyboard: .numberPad)
            }
            Button { Task { await model.lookup() } } label: {
                Text("Look up")
                    .font(EirsFont.sans(14, .semibold))
                    .foregroundStyle(eirs.brand)
                    .lineLimit(1)
                    .padding(.horizontal, 14)
                    .frame(height: 46)
                    .solidBorder(eirs.brand, radius: 12)
                    .contentShape(Rectangle())
            }
            .buttonStyle(PlainTapStyle())
            .opacity(model.lookingUp ? 0.6 : 1)
        }
        LabeledField("Legal name *", labelSize: 13, gap: 6) {
            FormField(text: $model.name, placeholder: "Dashen Trading PLC")
        }
        LabeledField("VAT number", labelSize: 13, gap: 6) {
            FormField(text: $model.vat, placeholder: "Optional")
        }
    }

    private var tinBinding: Binding<String> {
        Binding<String>(get: { model.tin }, set: { model.setTin($0) })
    }

    // MARK: Step 3 - Line items

    private var linesCard: some View {
        EirsCard(spacing: 12) {
            StepHeading(number: 3, title: "Line items")
            if !model.quickAdd.isEmpty { quickAddRail }
            ForEach(model.lines) { line in
                InvLineEditor(line: lineBinding(line.id), onRemove: { model.removeLine(line.id) })
            }
            InvAddLineButton { model.addLine() }
        }
    }

    private func lineBinding(_ id: Int) -> Binding<InvLineDraft> {
        Binding<InvLineDraft>(
            get: { model.lines.first { $0.id == id } ?? InvLineDraft(id: id) },
            set: { model.replaceLine($0) })
    }

    private var quickAddRail: some View {
        ChipRail {
            ForEach(model.quickAdd) { item in
                Button { model.addCatalogItem(item) } label: {
                    Text("+ \(item.description)")
                        .font(EirsFont.sans(12, .medium))
                        .foregroundStyle(eirs.brandDark)
                        .lineLimit(1)
                        .padding(.horizontal, 12)
                        .frame(height: 34)
                        .background(eirs.tint1, in: Capsule())
                        .dashedBorder(eirs.tint2, radius: 17)
                        .contentShape(Rectangle())
                }
                .buttonStyle(PlainTapStyle())
            }
        }
    }

    // MARK: Step 4 - Payment

    private var paymentCard: some View {
        EirsCard(spacing: 12) {
            StepHeading(number: 4, title: "Payment")
            Wrap {
                ForEach(InvoicePaymentMode.allCases) { mode in
                    EirsChip(label: mode.displayName, selected: model.paymentMode == mode, height: 38, radius: 10,
                             horizontalPadding: 13) {
                        model.paymentMode = mode
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            HStack(spacing: 6) {
                ForEach(PaymentTerm.allCases) { term in
                    termChip(term)
                }
            }
        }
    }

    private func termChip(_ term: PaymentTerm) -> some View {
        let on = term == model.paymentTerm
        let shape = RoundedRectangle(cornerRadius: 10, style: .continuous)
        return Button { model.paymentTerm = term } label: {
            Text(term.displayName)
                .font(EirsFont.sans(13, .medium))
                .foregroundStyle(on ? Color.white : eirs.ink)
                .lineLimit(1)
                .frame(maxWidth: .infinity)
                .frame(height: 38)
                .background(on ? eirs.ink : Color.white, in: shape)
                .overlay(shape.strokeBorder(on ? eirs.ink : eirs.line, lineWidth: 1))
                .contentShape(Rectangle())
        }
        .buttonStyle(PlainTapStyle())
    }
}
