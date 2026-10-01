import SwiftUI
import EIRSDomain

/// Two-column grid (CSS `grid-template-columns: 1fr 1fr; gap: 8px`); cells in a row share its height.
struct ReceiptGrid2<Item: Identifiable, Cell: View>: View {
    var items: [Item]
    var gap: CGFloat = 8
    @ViewBuilder var cell: (Item) -> Cell

    private var rows: [[Item]] {
        var out: [[Item]] = []
        var start = 0
        while start < items.count {
            let end = min(start + 2, items.count)
            out.append(Array(items[start..<end]))
            start = end
        }
        return out
    }

    var body: some View {
        let chunks = rows
        Grid(horizontalSpacing: gap, verticalSpacing: gap) {
            ForEach(chunks.indices, id: \.self) { rowIndex in
                GridRow {
                    ForEach(chunks[rowIndex]) { item in
                        cell(item).frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                    }
                    if chunks[rowIndex].count == 1 { Color.clear.frame(height: 1) }
                }
            }
        }
        .frame(maxWidth: .infinity)
    }
}

@MainActor
struct RegisterReceiptScreen: View {
    @Environment(\.eirs) private var eirs
    @State private var model: RegisterReceiptModel
    private let onBack: () -> Void
    private let onRegistered: (String) -> Void

    init(container: AppContainer, onBack: @escaping () -> Void, onRegistered: @escaping (String) -> Void) {
        _model = State(initialValue: RegisterReceiptModel(container: container))
        self.onBack = onBack
        self.onRegistered = onRegistered
    }

    var body: some View {
        LoadingHost(loading: model.loading) {
            ViewScaffold(
                header: {
                    ViewHeader(eyebrow: "NEW RECEIPT · \(model.branchCaps)", onBack: onBack,
                               title: { ViewTitle(plain: "Register ", accent: "receipt") })
                },
                bottomBar: { registerBar },
                content: {
                    typeCard
                    if model.needsItems { itemsCard } else { linkCard }
                    customerCard
                    paymentCard
                    Color.clear.frame(height: 4)
                })
        }
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
        .task { await model.observeCatalog() }
        .task { await model.observeInvoices() }
        .task { await model.observeWithholding() }
        .task { await model.observeBranch() }
    }

    // MARK: 1 · Receipt type

    private var typeCard: some View {
        ReceiptFormCard {
            StepHeading(number: 1, title: "Receipt type")
            ReceiptGrid2(items: ReceiptType.allCases) { type in
                typeCell(type)
            }
        }
    }

    private func typeCell(_ type: ReceiptType) -> some View {
        let on = model.form.type == type
        let shape = RoundedRectangle(cornerRadius: 12, style: .continuous)
        return Button { model.pickType(type) } label: {
            VStack(alignment: .leading, spacing: 3) {
                Text(type.displayName).font(EirsFont.sans(15, .semibold))
                Text(type.summary).font(EirsFont.sans(11)).lineSpacing(3).foregroundStyle(eirs.muted)
                    .multilineTextAlignment(.leading)
            }
            .padding(12)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            .background(on ? eirs.tint1 : Color.white, in: shape)
            .overlay(shape.strokeBorder(on ? eirs.brand : eirs.line, lineWidth: 1.5))
            .contentShape(shape)
        }
        .buttonStyle(PlainTapStyle())
    }

    // MARK: 2 · Items

    private var itemsCard: some View {
        ReceiptFormCard {
            StepHeading(number: 2, title: "Items", trailing: {
                Text(model.catalogNote).font(EirsFont.sans(11)).foregroundStyle(eirs.muted)
            })
            SearchField(text: $model.form.query, placeholder: "Search catalog")
            ReceiptGrid2(items: model.catalogTiles) { tile in
                catalogTile(tile)
            }
            ForEach(model.cartLines) { line in
                cartRow(line)
            }
            if model.form.lines.isEmpty {
                Text("Tap items above to add them.")
                    .font(EirsFont.sans(13)).foregroundStyle(eirs.muted)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
                    .padding(14)
                    .dashedBorder(eirs.line, radius: 12)
            }
        }
    }

    private func catalogTile(_ tile: ReceiptCatalogTile) -> some View {
        let shape = RoundedRectangle(cornerRadius: 12, style: .continuous)
        return Button { model.addItem(tile.code) } label: {
            VStack(alignment: .leading, spacing: 6) {
                Text(tile.description).font(EirsFont.sans(13, .medium)).lineLimit(1).truncationMode(.tail)
                HStack(spacing: 4) {
                    MonoText(text: tile.price, size: 12, lineLimit: 1)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    MonoText(text: tile.meta, size: 9, color: eirs.muted, lineLimit: 1)
                }
            }
            .padding(11)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(tile.inCart ? eirs.tint1 : Color.white, in: shape)
            .overlay(shape.strokeBorder(eirs.line, lineWidth: 1))
            .contentShape(shape)
        }
        .buttonStyle(PlainTapStyle())
    }

    private func cartRow(_ line: ReceiptCartLine) -> some View {
        VStack(spacing: 0) {
            Hairline()
            HStack(spacing: 10) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(line.description).font(EirsFont.sans(14, .medium)).lineLimit(1).truncationMode(.tail)
                    MonoText(text: line.total, size: 11, color: eirs.muted)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                qtyStepper(line)
            }
            .padding(.top, 10)
        }
    }

    private func qtyStepper(_ line: ReceiptCartLine) -> some View {
        let shape = RoundedRectangle(cornerRadius: 12, style: .continuous)
        return HStack(spacing: 0) {
            Button { model.decrement(line.index) } label: {
                Text("−").font(EirsFont.sans(20)).frame(width: 38, height: 44).contentShape(Rectangle())
            }
            .buttonStyle(PlainTapStyle())
            MonoText(text: line.quantity, size: 15, alignment: .center)
                .frame(maxWidth: .infinity)
            Button { model.increment(line.index) } label: {
                Text("+").font(EirsFont.sans(20)).frame(width: 38, height: 44).contentShape(Rectangle())
            }
            .buttonStyle(PlainTapStyle())
        }
        .frame(width: 112, height: 44)
        .clipShape(shape)
        .overlay(shape.strokeBorder(eirs.line, lineWidth: 1))
    }

    // MARK: 2 · Linked invoice

    private var linkCard: some View {
        ReceiptFormCard {
            VStack(alignment: .leading, spacing: 6) {
                StepHeading(number: 2, title: "Linked invoice")
                Text(model.linkNote).font(EirsFont.sans(12)).foregroundStyle(eirs.muted)
            }
            ForEach(model.linkOptions) { option in
                linkRow(option)
            }
            VStack(alignment: .leading, spacing: 8) {
                ReceiptFieldLabel(label: model.amountLabel) {
                    FormField(text: amountBinding, placeholder: "0.00", mono: true, alignment: .trailing, keyboard: .decimalPad)
                }
                Text(model.amountHint).font(EirsFont.sans(12)).lineSpacing(3).foregroundStyle(eirs.muted)
            }
        }
    }

    private var amountBinding: Binding<String> {
        Binding(get: { model.form.amount }, set: { model.setAmount($0) })
    }

    private func linkRow(_ option: ReceiptLinkOption) -> some View {
        let shape = RoundedRectangle(cornerRadius: 12, style: .continuous)
        let on = option.selected
        return Button { model.pickLink(option.id) } label: {
            HStack(spacing: 12) {
                Circle()
                    .strokeBorder(on ? eirs.brand : eirs.line, lineWidth: on ? 5 : 1.5)
                    .frame(width: 18, height: 18)
                VStack(alignment: .leading, spacing: 2) {
                    HStack {
                        MonoText(text: option.number, size: 13, weight: .medium)
                        Spacer(minLength: 8)
                        MonoText(text: option.total, size: 12)
                    }
                    Text("\(option.party) · \(option.status)")
                        .font(EirsFont.sans(12)).foregroundStyle(eirs.muted).lineLimit(1).truncationMode(.tail)
                }
            }
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(on ? eirs.tint1 : Color.white, in: shape)
            .overlay(shape.strokeBorder(on ? eirs.brand : eirs.line, lineWidth: 1.5))
            .contentShape(shape)
        }
        .buttonStyle(PlainTapStyle())
    }

    // MARK: 3 · Customer

    private var customerCard: some View {
        ReceiptFormCard {
            StepHeading(number: 3, title: "Customer")
            if model.needsLink, let number = model.linkedNo {
                (Text("From \(number):").foregroundColor(eirs.muted) + Text(" ") +
                    Text(model.linkedParty).fontWeight(.semibold))
                    .font(EirsFont.sans(14))
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(12)
                    .background(eirs.background, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
            if model.needsItems {
                walkInRow
                if !model.form.walkIn {
                    ReceiptFieldLabel(label: "Name") {
                        FormField(text: $model.form.name, placeholder: "Customer or company")
                    }
                    ReceiptFieldLabel(label: "Phone or TIN") {
                        FormField(text: $model.form.phone, placeholder: "+251 9… or TIN", mono: true, keyboard: .phonePad)
                    }
                }
            }
        }
    }

    private var walkInRow: some View {
        HStack(spacing: 12) {
            EirsSwitch(isOn: model.form.walkIn) { model.form.walkIn.toggle() }
            VStack(alignment: .leading, spacing: 2) {
                Text("Walk-in customer").font(EirsFont.sans(14, .medium))
                Text("No details on the receipt").font(EirsFont.sans(12)).foregroundStyle(eirs.muted)
            }
            Spacer(minLength: 0)
        }
        .padding(12)
        .solidBorder(eirs.line, radius: 12)
    }

    // MARK: 4 · Payment method

    private var paymentCard: some View {
        ReceiptFormCard {
            StepHeading(number: 4, title: "Payment method")
            Wrap {
                ForEach(PaymentMethod.allCases) { method in
                    methodChip(method)
                }
            }
            if model.form.method == .cash { cashSection }
            if model.form.method == .mobileMoney { providerChips }
            if model.form.method != .cash {
                ReceiptFieldLabel(label: model.referenceLabel) {
                    FormField(text: $model.form.reference, placeholder: model.referencePlaceholder, mono: true)
                }
            }
        }
    }

    private func methodChip(_ method: PaymentMethod) -> some View {
        let on = model.form.method == method
        let shape = RoundedRectangle(cornerRadius: 11, style: .continuous)
        return Button { model.pickMethod(method) } label: {
            Text(method.displayName)
                .font(EirsFont.sans(14, .medium))
                .foregroundStyle(on ? Color.white : eirs.ink)
                .padding(.horizontal, 14)
                .frame(height: 42)
                .background(on ? eirs.ink : Color.white, in: shape)
                .overlay(shape.strokeBorder(on ? eirs.ink : eirs.line, lineWidth: 1.5))
                .contentShape(shape)
        }
        .buttonStyle(PlainTapStyle())
    }

    private var tenderedBinding: Binding<String> {
        Binding(get: { model.form.tendered }, set: { model.setTendered($0) })
    }

    @ViewBuilder
    private var cashSection: some View {
        ReceiptFieldLabel(label: "Amount tendered") {
            FormField(text: tenderedBinding, placeholder: model.tenderPlaceholder, mono: true,
                      alignment: .trailing, keyboard: .decimalPad)
        }
        let chips = model.tenderChips
        if !chips.isEmpty {
            Wrap {
                ForEach(chips) { chip in
                    Button { model.form.tendered = chip.value } label: {
                        MonoText(text: chip.label, size: 12, color: eirs.brandDark)
                            .padding(.horizontal, 12)
                            .frame(height: 34)
                            .background(eirs.tint1, in: Capsule())
                            .overlay(Capsule().strokeBorder(eirs.tint2, style: StrokeStyle(lineWidth: 1, dash: [6, 5])))
                            .contentShape(Capsule())
                    }
                    .buttonStyle(PlainTapStyle())
                }
            }
        }
        HStack {
            Text("Change due").font(EirsFont.sans(14)).foregroundStyle(eirs.brandDark)
            Spacer(minLength: 8)
            MonoText(text: model.change, size: 14, color: eirs.brandDark, weight: .medium)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 11)
        .background(eirs.tint1, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private var providerChips: some View {
        Wrap {
            ForEach(mobileMoneyProviders, id: \.self) { provider in
                let on = model.form.provider == provider
                Button { model.form.provider = provider } label: {
                    Text(provider)
                        .font(EirsFont.sans(13))
                        .foregroundStyle(on ? eirs.brandDark : eirs.ink)
                        .padding(.horizontal, 12)
                        .frame(height: 36)
                        .background(on ? eirs.tint1 : Color.white, in: Capsule())
                        .overlay(Capsule().strokeBorder(on ? eirs.brand : eirs.line, lineWidth: 1))
                        .contentShape(Capsule())
                }
                .buttonStyle(PlainTapStyle())
            }
        }
    }

    // MARK: Sticky bar

    private var registerBar: some View {
        StickyBottomBar(spacing: 8) {
            HStack(alignment: .lastTextBaseline, spacing: 10) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("\(model.fromLabel) \(model.billTo)")
                        .font(EirsFont.sans(12)).foregroundStyle(eirs.muted).lineLimit(1).truncationMode(.tail)
                    HStack(spacing: 6) {
                        Circle().fill(model.ready ? eirs.brand : eirs.pending).frame(width: 6, height: 6)
                        Text(model.typeCaps).font(EirsFont.sans(11)).foregroundStyle(eirs.muted).lineLimit(1)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                Text(model.grandText)
                    .font(EirsFont.serif(28))
                    .foregroundStyle(model.refund ? eirs.red : eirs.ink)
                    .lineLimit(1)
            }
            PrimaryButton("Register with MoR", enabled: model.ready, height: 52, fontSize: 15) {
                Task {
                    if let id = await model.submit() { onRegistered(id) }
                }
            }
        }
    }
}
