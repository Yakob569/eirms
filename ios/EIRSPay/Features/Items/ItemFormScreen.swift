import SwiftUI
import EIRSDomain

struct ItemForm {
    var isNew: Bool = true
    var code: String = ""
    var description: String = ""
    var nature: NatureOfSupply = .goods
    var hs: String = ""
    var tax: TaxCode = .vat15
    var unit: String = "PCS"
    var price: String = ""
    var active: Bool = true
    var category: String = ""
}

@MainActor @Observable
final class ItemFormModel {
    private let container: AppContainer
    private let editCode: String?
    private var bound = false

    var form = ItemForm()
    private var existing: [CatalogItem] = []

    init(container: AppContainer, code: String?) {
        self.container = container
        self.editCode = code
    }

    /// Keeps the catalog current (for duplicate-code checks) and loads the item being edited once.
    func observe() async {
        for await catalog in container.observeCatalog() {
            existing = catalog
            if !bound {
                bound = true
                if let code = editCode, let item = catalog.first(where: { $0.code == code }) {
                    form = ItemForm(
                        isNew: false, code: item.code, description: item.description, nature: item.nature,
                        hs: item.hsCode, tax: item.taxCode, unit: item.unit,
                        price: NSDecimalNumber(decimal: item.unitPrice).stringValue,
                        active: item.active, category: item.category)
                }
            }
        }
    }

    private func toItem(_ f: ItemForm) -> CatalogItem {
        let parsed: Decimal = Decimal(string: f.price, locale: Locale(identifier: "en_US_POSIX")) ?? 0
        let category: String = f.category.isEmpty ? (f.nature == .service ? "Services" : "General") : f.category
        return CatalogItem(code: f.code, description: f.description, nature: f.nature, hsCode: f.hs,
                           taxCode: f.tax, unit: f.unit, unitPrice: parsed.rounded(), active: f.active, category: category)
    }

    private var validation: CatalogValidation {
        container.validateCatalogItem(toItem(form), isNew: form.isNew, existing: existing)
    }

    var ready: Bool { validation == .ok }

    var readyText: String {
        switch validation {
        case .duplicateCode: return "That code is already in the catalog."
        case .missing(let fields): return "Missing: " + fields.joined(separator: ", ")
        case .ok: return "Ready to save."
        }
    }

    var gross: String { Fmt.etb(toItem(form).grossPrice) + " per " + form.unit }

    /// Saves the item; returns true when it was saved.
    func submit() async -> Bool {
        guard ready else { return false }
        var item = toItem(form)
        item.code = form.code.trimmingCharacters(in: .whitespaces)
        item.description = form.description.trimmingCharacters(in: .whitespaces)
        await container.saveCatalogItem(item, isNew: form.isNew)
        return true
    }
}

@MainActor
struct ItemFormScreen: View {
    @Environment(\.eirs) private var eirs
    @State private var model: ItemFormModel
    private let onBack: () -> Void

    init(container: AppContainer, code: String?, onBack: @escaping () -> Void) {
        _model = State(initialValue: ItemFormModel(container: container, code: code))
        self.onBack = onBack
    }

    private var goods: Bool { model.form.nature == .goods }

    var body: some View {
        ViewScaffold(
            header: {
                ViewHeader(eyebrow: "ITEMS", onBack: onBack,
                           title: { ViewTitle(plain: model.form.isNew ? "New item" : "Edit item") })
            },
            bottomBar: { saveBar },
            content: {
                Text(model.form.isNew
                     ? "Adds to the catalog your team picks from."
                     : "Changes apply to new documents. Registered invoices keep their original lines.")
                    .font(EirsFont.sans(13)).lineSpacing(4).foregroundStyle(eirs.muted)
                    .frame(maxWidth: .infinity, alignment: .leading)
                basicsCard
                pricingCard
            })
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
        .task { await model.observe() }
    }

    private var saveBar: some View {
        StickyBottomBar(spacing: 8, shadow: false) {
            Text(model.readyText).font(EirsFont.sans(12)).foregroundStyle(eirs.muted)
                .frame(maxWidth: .infinity, alignment: .leading)
            PrimaryButton("Save item", enabled: model.ready, height: 52, fontSize: 15) {
                Task {
                    if await model.submit() { onBack() }
                }
            }
        }
    }

    // MARK: Basics

    private var codeBinding: Binding<String> {
        Binding(get: { model.form.code }, set: { model.form.code = $0.uppercased() })
    }

    private var hsBinding: Binding<String> {
        Binding(get: { model.form.hs },
                set: { value in model.form.hs = value.filter { $0.isNumber || $0 == "." } })
    }

    private var priceBinding: Binding<String> {
        Binding(get: { model.form.price }, set: { model.form.price = $0.decimalOnly })
    }

    private var basicsCard: some View {
        ReceiptFormCard {
            if model.form.isNew {
                ReceiptFieldLabel(label: "Item code *") {
                    FormField(text: codeBinding, placeholder: "ITM-0000", mono: true)
                }
            } else {
                HStack {
                    Text("Item code").font(EirsFont.sans(13)).foregroundStyle(eirs.muted)
                    Spacer(minLength: 8)
                    MonoText(text: model.form.code, size: 13, weight: .medium)
                }
                .padding(12)
                .background(eirs.background, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
            ReceiptFieldLabel(label: "Description *") {
                FormField(text: $model.form.description, placeholder: "As printed on the receipt")
            }
            VStack(alignment: .leading, spacing: 6) {
                Text("Nature of supply").font(EirsFont.sans(13, .medium))
                SegmentedControl(
                    options: [SegmentOption(value: NatureOfSupply.goods, label: "Goods"),
                              SegmentOption(value: NatureOfSupply.service, label: "Service")],
                    selection: $model.form.nature, height: 40, fontSize: 14, solidSelection: true)
            }
            ReceiptFieldLabel(label: goods ? "Harmonization (HS) code *" : "Harmonization (HS) code") {
                FormField(text: hsBinding, placeholder: "e.g. 0901.11.00", mono: true, keyboard: .decimalPad)
            }
            Text(goods ? "Required for goods. Used in MoR and customs reporting." : "Optional for services.")
                .font(EirsFont.sans(12)).foregroundStyle(eirs.muted)
                .frame(maxWidth: .infinity, alignment: .leading)
                .offset(y: -6)
        }
    }

    // MARK: Tax, unit, price

    private var pricingCard: some View {
        ReceiptFormCard {
            Text("Tax code").font(EirsFont.sans(13, .medium))
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 6), count: 3), spacing: 6) {
                ForEach(TaxCode.allCases) { code in
                    taxCell(code)
                }
            }
            Text("Unit").font(EirsFont.sans(13, .medium))
            Wrap {
                ForEach(catalogUnits, id: \.self) { unit in
                    unitChip(unit)
                }
            }
            ReceiptFieldLabel(label: "Unit price (excl. tax) *") {
                FormField(text: priceBinding, placeholder: "0.00", mono: true, alignment: .trailing, keyboard: .decimalPad)
            }
            HStack {
                Text("Customer pays").font(EirsFont.sans(13)).foregroundStyle(eirs.brandDark)
                Spacer(minLength: 8)
                MonoText(text: model.gross, size: 12, color: eirs.brandDark, weight: .medium)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 11)
            .background(eirs.tint1, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            HStack(spacing: 12) {
                EirsSwitch(isOn: model.form.active) { model.form.active.toggle() }
                Text("Active").font(EirsFont.sans(14, .medium))
                Spacer(minLength: 0)
            }
        }
    }

    private func taxCell(_ code: TaxCode) -> some View {
        let on = model.form.tax == code
        let shape = RoundedRectangle(cornerRadius: 11, style: .continuous)
        return Button { model.form.tax = code } label: {
            VStack(alignment: .leading, spacing: 2) {
                MonoText(text: code.rawValue, size: 12, weight: .medium)
                Text(code.summary).font(EirsFont.sans(10)).foregroundStyle(eirs.muted)
                    .multilineTextAlignment(.leading)
            }
            .padding(10)
            .frame(maxWidth: .infinity, minHeight: 58, alignment: .topLeading)
            .background(on ? eirs.tint1 : Color.white, in: shape)
            .overlay(shape.strokeBorder(on ? eirs.brand : eirs.line, lineWidth: 1.5))
            .contentShape(shape)
        }
        .buttonStyle(PlainTapStyle())
    }

    private func unitChip(_ unit: String) -> some View {
        let on = model.form.unit == unit
        let shape = RoundedRectangle(cornerRadius: 9, style: .continuous)
        return Button { model.form.unit = unit } label: {
            MonoText(text: unit, size: 12, color: on ? Color.white : eirs.ink)
                .padding(.horizontal, 12)
                .frame(height: 36)
                .background(on ? eirs.ink : Color.white, in: shape)
                .overlay(shape.strokeBorder(on ? eirs.ink : eirs.line, lineWidth: 1))
                .contentShape(shape)
        }
        .buttonStyle(PlainTapStyle())
    }
}
