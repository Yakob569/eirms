import SwiftUI
import EIRSDomain

/// One editable invoice line, shared by Register invoice and the interim/final invoice builder so the
/// two item tables can't drift. Without `number` it is the compact Register invoice card; with it, the
/// full row ("LINE 2", item code, unit, nature of supply) used against a retainer.
struct InvLineEditor: View {
    @Environment(\.eirs) private var eirs
    @Binding var line: InvLineDraft
    var number: Int?
    var canRemove = true
    var onRemove: () -> Void

    private static let natureOptions: [SegmentOption<NatureOfSupply>] = [
        SegmentOption(value: .goods, label: "Goods"),
        SegmentOption(value: .service, label: "Service")
    ]

    var body: some View {
        if let number { detailed(number) } else { compact }
    }

    // MARK: Bindings

    private var descriptionText: Binding<String> {
        Binding(get: { line.description }, set: { line.description = $0 })
    }
    private var quantityText: Binding<String> {
        Binding(get: { line.quantity }, set: { line.quantity = $0.decimalOnly })
    }
    private var priceText: Binding<String> {
        Binding(get: { line.price }, set: { line.price = $0.decimalOnly })
    }
    private var codeText: Binding<String> {
        Binding(get: { line.itemCode ?? "" }, set: { line.itemCode = $0.isEmpty ? nil : $0.uppercased() })
    }
    private var unitText: Binding<String> {
        Binding(get: { line.unit }, set: { line.unit = $0.uppercased() })
    }
    private var nature: Binding<NatureOfSupply> {
        Binding(get: { line.nature }, set: { line.nature = $0 })
    }

    // MARK: Compact (Register invoice)

    private var compact: some View {
        VStack(spacing: 8) {
            HStack(spacing: 8) {
                FormField(text: descriptionText, placeholder: "Item or service")
                Button(action: onRemove) {
                    LineIcon(EirsIcons.close, size: 15, tint: eirs.muted)
                        .frame(width: 36, height: 36)
                        .contentShape(Rectangle())
                }
                .buttonStyle(PlainTapStyle())
            }
            HStack(spacing: 8) {
                FormField(text: quantityText, mono: true, alignment: .trailing, keyboard: .decimalPad, fontSize: 15)
                    .frame(width: 72)
                FormField(text: priceText, placeholder: "Unit price", mono: true, alignment: .trailing,
                          keyboard: .decimalPad, fontSize: 15)
            }
            HStack(spacing: 10) {
                InvTaxPicker(selection: Binding(get: { line.taxCode }, set: { line.taxCode = $0 }))
                MonoText(text: Fmt.f2(line.total), size: 14, weight: .medium, alignment: .trailing, lineLimit: 1)
                    .frame(minWidth: 90, alignment: .trailing)
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity)
        .solidBorder(eirs.line, radius: 14)
    }

    // MARK: Detailed (interim / final)

    private func detailed(_ number: Int) -> some View {
        VStack(spacing: 10) {
            HStack {
                MonoText(text: "LINE \(number)", size: 11, color: eirs.muted)
                Spacer(minLength: 8)
                if canRemove {
                    Button(action: onRemove) {
                        Text("Remove").font(EirsFont.sans(12, .semibold)).foregroundStyle(eirs.red)
                            .padding(.vertical, 4).contentShape(Rectangle())
                    }
                    .buttonStyle(PlainTapStyle())
                }
            }
            FormField(text: descriptionText, placeholder: "Description", restingBackground: .white)
            HStack(spacing: 8) {
                FormField(text: codeText, placeholder: "Item code", mono: true, restingBackground: .white, fontSize: 14)
                FormField(text: unitText, placeholder: "Unit", mono: true, restingBackground: .white, fontSize: 14)
                    .frame(width: 110)
            }
            SegmentedControl(options: Self.natureOptions, selection: nature, height: 34)
            HStack(spacing: 8) {
                FormField(text: quantityText, placeholder: "Qty", mono: true, alignment: .trailing,
                          keyboard: .decimalPad, restingBackground: .white, fontSize: 15)
                    .frame(width: 104)
                FormField(text: priceText, placeholder: "Unit price", mono: true, alignment: .trailing,
                          keyboard: .decimalPad, restingBackground: .white, fontSize: 15)
            }
            HStack(spacing: 10) {
                InvTaxPicker(selection: Binding(get: { line.taxCode }, set: { line.taxCode = $0 }))
                MonoText(text: Fmt.f2(line.total), size: 14, weight: .medium, alignment: .trailing, lineLimit: 1)
                    .frame(minWidth: 90, alignment: .trailing)
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity)
        .background(eirs.background, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
        .solidBorder(eirs.line, radius: 12)
    }
}

/// VAT15 / VAT0 / EXC10 segmented picker used on invoice lines.
struct InvTaxPicker: View {
    @Environment(\.eirs) private var eirs
    @Binding var selection: TaxCode

    var body: some View {
        HStack(spacing: 0) {
            ForEach(TaxCode.allCases) { code in
                let on = code == selection
                Button { selection = code } label: {
                    Text(code.rawValue)
                        .font(EirsFont.mono(11))
                        .foregroundStyle(on ? Color.white : eirs.muted)
                        .lineLimit(1)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .background(on ? eirs.ink : Color.clear, in: RoundedRectangle(cornerRadius: 7, style: .continuous))
                        .contentShape(Rectangle())
                }
                .buttonStyle(PlainTapStyle())
            }
        }
        .padding(2)
        .frame(maxWidth: .infinity)
        .frame(height: 36)
        .background(eirs.background, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
        .solidBorder(eirs.line, radius: 10)
    }
}

/// "+ Add line" dashed button under an item table.
struct InvAddLineButton: View {
    @Environment(\.eirs) private var eirs
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            Text("+ Add line")
                .font(EirsFont.sans(14, .medium))
                .foregroundStyle(eirs.brand)
                .frame(maxWidth: .infinity)
                .frame(height: 44)
                .dashedBorder(eirs.line, radius: 12)
                .contentShape(Rectangle())
        }
        .buttonStyle(PlainTapStyle())
    }
}
