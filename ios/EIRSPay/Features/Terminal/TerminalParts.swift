import SwiftUI
import EIRSDomain

// MARK: - Header + hold to exit

struct TerminalHeader: View {
    @Environment(\.eirs) private var eirs
    let model: TerminalModel
    let branch: Branch
    let tablet: Bool
    let topInset: CGFloat

    var body: some View {
        let subLine = tablet
            ? "Terminal mode · \(branch.displayName) · on shift \(model.shiftFor)"
            : "\(branch.displayName) · \(model.shiftFor)"
        HStack(spacing: 12) {
            Text(model.initials)
                .font(EirsFont.sans(14, .semibold))
                .foregroundStyle(Color.white)
                .frame(width: 42, height: 42)
                .background(eirs.ink, in: Circle())
            VStack(alignment: .leading, spacing: 2) {
                Text(model.staffName)
                    .font(EirsFont.sans(16, .semibold))
                    .foregroundStyle(eirs.ink)
                    .lineLimit(1)
                HStack(spacing: 6) {
                    Circle().fill(eirs.brand).frame(width: 7, height: 7)
                    Text(subLine)
                        .font(EirsFont.sans(12))
                        .foregroundStyle(eirs.brandDark)
                        .lineLimit(1)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if tablet {
                Text("\(model.staffRole) · \(model.clock)")
                    .font(EirsFont.mono(12))
                    .foregroundStyle(eirs.muted)
                    .lineLimit(1)
                    .padding(.trailing, 6)
            }
            TerminalHoldButton(onComplete: { model.holdComplete() })
        }
        .padding(.horizontal, tablet ? 20 : 14)
        .padding(.top, tablet ? 12 : 12 + topInset)
        .padding(.bottom, 12)
        .background(Color.white)
        .hairline(.bottom, color: eirs.line)
    }
}

/// Press and hold for 1.2s while a brand fill sweeps across; releasing early cancels.
@MainActor
struct TerminalHoldButton: View {
    @Environment(\.eirs) private var eirs
    var onComplete: () -> Void
    @State private var fill: CGFloat = 0
    @State private var holding = false
    @State private var holdTask: Task<Void, Never>?

    var body: some View {
        HStack(spacing: 8) {
            LineIcon(EirsIcons.lock, size: 16, strokeWidth: 2, tint: .white)
            Text(holding ? "Keep holding…" : "Hold to exit")
                .font(EirsFont.sans(14, .semibold))
                .foregroundStyle(Color.white)
                .lineLimit(1)
                .fixedSize()
        }
        .padding(.horizontal, 14)
        .frame(height: 46)
        .background {
            GeometryReader { proxy in
                ZStack(alignment: .leading) {
                    eirs.ink
                    eirs.brand.frame(width: proxy.size.width * fill)
                }
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: 13, style: .continuous))
        .contentShape(Rectangle())
        .gesture(
            DragGesture(minimumDistance: 0)
                .onChanged { _ in begin() }
                .onEnded { _ in release() }
        )
        .onDisappear { holdTask?.cancel() }
    }

    private func begin() {
        if holding { return }
        holding = true
        fill = 0
        withAnimation(.linear(duration: 1.2)) { fill = 1 }
        holdTask = Task {
            do { try await Task.sleep(nanoseconds: 1_200_000_000) } catch { return }
            if Task.isCancelled { return }
            holding = false
            var transaction = Transaction()
            transaction.disablesAnimations = true
            withTransaction(transaction) { fill = 0 }
            onComplete()
        }
    }

    private func release() {
        if !holding { return }
        holdTask?.cancel()
        holding = false
        withAnimation(.easeOut(duration: 0.15)) { fill = 0 }
    }
}

// MARK: - Items

struct TerminalFindField: View {
    @Environment(\.eirs) private var eirs
    @Binding var text: String

    var body: some View {
        EirsTextField(placeholder: "Find item", text: $text, height: 44, focusRing: false,
                      restingBackground: .white, horizontalPadding: 8,
                      leading: { LineIcon(EirsIcons.search, size: 17, tint: eirs.muted).padding(.leading, 12) },
                      trailing: { EmptyView() })
            .frame(width: 200)
    }
}

private struct TerminalTileStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.97 : 1)
            .animation(.easeOut(duration: 0.12), value: configuration.isPressed)
    }
}

struct TerminalItemTile: View {
    @Environment(\.eirs) private var eirs
    let item: CatalogItem
    let qty: Int
    let height: CGFloat
    let onTap: () -> Void

    var body: some View {
        let inCart = qty > 0
        let shape = RoundedRectangle(cornerRadius: 16, style: .continuous)
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 0) {
                Text(item.description)
                    .font(EirsFont.sans(15, .medium))
                    .foregroundStyle(eirs.ink)
                    .lineSpacing(1)
                    .lineLimit(3)
                    .multilineTextAlignment(.leading)
                    .padding(.trailing, 30)
                Spacer(minLength: 0)
                HStack(alignment: .bottom) {
                    Text(Fmt.f2(item.unitPrice))
                        .font(EirsFont.mono(16, .medium))
                        .foregroundStyle(eirs.ink)
                        .lineLimit(1)
                    Spacer(minLength: 6)
                    Text(item.taxCode.rawValue)
                        .font(EirsFont.mono(10))
                        .foregroundStyle(eirs.muted)
                        .lineLimit(1)
                }
            }
            .padding(13)
            .frame(maxWidth: .infinity, minHeight: height, maxHeight: height, alignment: .topLeading)
            .background(inCart ? eirs.tint1 : Color.white, in: shape)
            .overlay(shape.strokeBorder(inCart ? eirs.brand : eirs.line, lineWidth: 1.5))
            .overlay(alignment: .topTrailing) {
                if inCart {
                    Text("\(qty)")
                        .font(EirsFont.mono(12))
                        .foregroundStyle(Color.white)
                        .padding(.horizontal, 6)
                        .frame(minWidth: 26, minHeight: 26)
                        .background(eirs.brand, in: Capsule())
                        .padding(.top, 10)
                        .padding(.trailing, 10)
                }
            }
            .contentShape(shape)
        }
        .buttonStyle(TerminalTileStyle())
    }
}

// MARK: - Cart (tablet aside + phone sheet body)

struct TerminalCartPanel: View {
    @Environment(\.eirs) private var eirs
    let model: TerminalModel
    let tablet: Bool
    let bottomInset: CGFloat

    private var hPad: CGFloat { tablet ? 20 : 18 }

    var body: some View {
        VStack(spacing: 0) {
            titleRow
            buyerBlock
            linesList
            TerminalCartFooter(model: model, tablet: tablet)
                .padding(.horizontal, hPad)
                .padding(.top, 12)
                .padding(.bottom, (tablet ? 16 : 30) + bottomInset)
                .frame(maxWidth: .infinity)
                .hairline(.top, color: eirs.line)
        }
    }

    private var titleRow: some View {
        HStack {
            SerifTitle(plain: "Current ", accent: "sale", size: 28)
            Spacer(minLength: 8)
            if !tablet || model.hasCart {
                Button { model.clearCart() } label: {
                    Text("Clear")
                        .font(EirsFont.sans(14))
                        .foregroundStyle(eirs.muted)
                        .padding(8)
                        .contentShape(Rectangle())
                }
                .buttonStyle(PlainTapStyle())
            } else {
                Color.clear.frame(width: 1, height: 36)
            }
        }
        .padding(.horizontal, hPad)
        .padding(.top, tablet ? 16 : 6)
        .padding(.bottom, 10)
    }

    private var buyerBlock: some View {
        VStack(spacing: 8) {
            SegmentedControl(
                options: TerminalBuyerMode.allCases.map { SegmentOption(value: $0, label: $0.label) },
                selection: Binding(get: { model.buyer }, set: { model.pickBuyer($0) }),
                height: 40, fontSize: 14)
            if model.isBusiness { businessRow }
            if model.buyer == .phone {
                FormField(text: Binding(get: { model.phone }, set: { model.setPhone($0) }),
                          placeholder: "+251 9… for SMS receipt", mono: true, keyboard: .phonePad)
            }
        }
        .padding(.horizontal, hPad)
        .padding(.bottom, tablet ? 12 : 10)
    }

    @ViewBuilder private var businessRow: some View {
        HStack(spacing: 8) {
            FormField(text: Binding(get: { model.tin }, set: { model.setTin($0) }),
                      placeholder: tablet ? "Buyer TIN (10 digits)" : "Buyer TIN", mono: true, keyboard: .numberPad)
            Button { model.lookup() } label: {
                Text("Look up")
                    .font(EirsFont.sans(14, .semibold))
                    .foregroundStyle(eirs.brand)
                    .padding(.horizontal, 14)
                    .frame(height: 46)
                    .solidBorder(eirs.brand, radius: 10)
                    .opacity(model.lookingUp ? 0.6 : 1)
                    .contentShape(Rectangle())
            }
            .buttonStyle(PlainTapStyle())
        }
        if let party = model.business {
            HStack {
                Text(party.name)
                    .font(EirsFont.sans(13, .semibold))
                    .foregroundStyle(eirs.brandDark)
                    .lineLimit(1)
                Spacer(minLength: 8)
                Text("B2B")
                    .font(EirsFont.sans(13))
                    .foregroundStyle(eirs.brandDark)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(eirs.tint1, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
        }
    }

    private var linesList: some View {
        let lineTotals = model.totals.lines
        return ScrollView {
            VStack(spacing: 0) {
                ForEach(model.cart.indices, id: \.self) { index in
                    let totalText = index < lineTotals.count ? Fmt.f2(lineTotals[index].total) : ""
                    TerminalCartLineRow(line: model.cart[index], total: totalText, tablet: tablet,
                                        onDec: { model.dec(index) }, onInc: { model.inc(index) })
                }
                if model.cart.isEmpty && tablet {
                    Text("Tap items to start a sale.")
                        .font(EirsFont.sans(14))
                        .foregroundStyle(eirs.muted)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity, minHeight: 120)
                        .padding(20)
                }
            }
        }
        .frame(maxHeight: .infinity)
        .hairline(.top, color: eirs.line)
    }
}

private struct TerminalCartLineRow: View {
    @Environment(\.eirs) private var eirs
    let line: TerminalCartLine
    let total: String
    let tablet: Bool
    let onDec: () -> Void
    let onInc: () -> Void

    var body: some View {
        HStack(spacing: tablet ? 10 : 8) {
            VStack(alignment: .leading, spacing: 2) {
                Text(line.item.description)
                    .font(EirsFont.sans(14, .medium))
                    .foregroundStyle(eirs.ink)
                    .lineLimit(1)
                Text(Fmt.f2(line.item.unitPrice) + " · " + line.item.taxCode.rawValue)
                    .font(EirsFont.mono(11))
                    .foregroundStyle(eirs.muted)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            stepper
            Text(total)
                .font(EirsFont.mono(tablet ? 13 : 12))
                .foregroundStyle(eirs.ink)
                .lineLimit(1)
                .frame(width: tablet ? 84 : 76, alignment: .trailing)
        }
        .padding(.horizontal, tablet ? 20 : 18)
        .padding(.vertical, 10)
        .hairline(.bottom, color: eirs.line)
    }

    private var stepper: some View {
        let buttonWidth: CGFloat = tablet ? 42 : 40
        return HStack(spacing: 0) {
            Button(action: onDec) {
                Text("−").font(EirsFont.sans(22)).foregroundStyle(eirs.ink)
                    .frame(width: buttonWidth, height: 44)
                    .contentShape(Rectangle())
            }
            .buttonStyle(PressFillStyle(normal: .clear, pressed: eirs.tint1, radius: 0))
            Text("\(line.qty)")
                .font(EirsFont.mono(16))
                .foregroundStyle(eirs.ink)
                .frame(maxWidth: .infinity)
            Button(action: onInc) {
                Text("+").font(EirsFont.sans(22)).foregroundStyle(eirs.ink)
                    .frame(width: buttonWidth, height: 44)
                    .contentShape(Rectangle())
            }
            .buttonStyle(PressFillStyle(normal: .clear, pressed: eirs.tint1, radius: 0))
        }
        .frame(width: tablet ? 124 : 116, height: 44)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        .solidBorder(eirs.line, radius: 12)
    }
}

private struct TerminalCartFooter: View {
    @Environment(\.eirs) private var eirs
    let model: TerminalModel
    let tablet: Bool

    var body: some View {
        VStack(spacing: 10) {
            HStack {
                Text("Taxable " + Fmt.f2(model.totals.subtotal))
                Spacer(minLength: 8)
                Text("Tax " + Fmt.f2(model.totals.taxTotal))
            }
            .font(EirsFont.sans(13))
            .foregroundStyle(eirs.muted)
            payRow
            if model.pay == .cash && !model.tenderOptions.isEmpty { tenderRow }
            chargeButton
        }
    }

    private var payRow: some View {
        HStack(spacing: 6) {
            ForEach(TerminalPay.allCases, id: \.self) { option in
                let on = model.pay == option
                let shape = RoundedRectangle(cornerRadius: 12, style: .continuous)
                Button { model.pickPay(option) } label: {
                    Text(option.label)
                        .font(EirsFont.sans(tablet ? 13 : 12, .semibold))
                        .foregroundStyle(on ? Color.white : eirs.ink)
                        .lineLimit(1)
                        .frame(maxWidth: .infinity)
                        .frame(height: tablet ? 48 : 46)
                        .background(on ? eirs.ink : Color.white, in: shape)
                        .overlay(shape.strokeBorder(on ? eirs.ink : eirs.line, lineWidth: 1.5))
                        .contentShape(shape)
                }
                .buttonStyle(PlainTapStyle())
            }
        }
    }

    private var tenderRow: some View {
        let options = model.tenderOptions
        return HStack(spacing: 6) {
            ForEach(options.indices, id: \.self) { index in
                let value = options[index]
                let on = model.isTendered(value)
                Button { model.pickTender(value) } label: {
                    Text(index == 0 ? "Exact" : Fmt.compact(value))
                        .font(EirsFont.mono(12))
                        .foregroundStyle(eirs.brandDark)
                        .lineLimit(1)
                        .frame(maxWidth: .infinity)
                        .frame(height: 40)
                        .background(on ? eirs.tint2 : eirs.tint1, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                        .dashedBorder(on ? eirs.brand : eirs.tint2, radius: 10)
                        .contentShape(Rectangle())
                }
                .buttonStyle(PlainTapStyle())
            }
        }
    }

    private var chargeButton: some View {
        Button { model.requestCharge() } label: {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Charge")
                        .font(EirsFont.sans(tablet ? 17 : 16, .semibold))
                        .foregroundStyle(Color.white)
                    Text(model.chargeSub)
                        .font(EirsFont.sans(tablet ? 12 : 11))
                        .foregroundStyle(Color.white.opacity(0.85))
                        .lineLimit(1)
                }
                Spacer(minLength: 10)
                Text(Fmt.etb(model.total))
                    .font(EirsFont.serif(tablet ? 30 : 28))
                    .foregroundStyle(Color.white)
                    .lineLimit(1)
            }
            .padding(.horizontal, tablet ? 20 : 18)
            .frame(maxWidth: .infinity)
            .frame(height: tablet ? 68 : 64)
            .contentShape(Rectangle())
        }
        .buttonStyle(PressFillStyle(normal: eirs.brand, pressed: eirs.brandDark, radius: 16, opacityWhenDisabled: 0.45))
        .disabled(!model.ok)
        .animation(.easeOut(duration: 0.2), value: model.ok)
    }
}

// MARK: - Review bar (phone)

struct TerminalReviewBar: View {
    @Environment(\.eirs) private var eirs
    let model: TerminalModel
    let bottomInset: CGFloat

    var body: some View {
        Button { model.openCart() } label: {
            HStack(spacing: 12) {
                Text("\(model.count)")
                    .font(EirsFont.mono(14))
                    .foregroundStyle(Color.white)
                    .padding(.horizontal, 8)
                    .frame(minWidth: 30, minHeight: 30)
                    .background(Color.white.opacity(0.2), in: Capsule())
                Text("Review sale")
                    .font(EirsFont.sans(16, .semibold))
                    .foregroundStyle(Color.white)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Text(Fmt.etb(model.total))
                    .font(EirsFont.serif(26))
                    .foregroundStyle(Color.white)
                    .lineLimit(1)
            }
            .padding(.horizontal, 18)
            .frame(maxWidth: .infinity)
            .frame(height: 66)
            .contentShape(Rectangle())
        }
        .buttonStyle(PressFillStyle(normal: eirs.brand, pressed: eirs.brandDark, radius: 18))
        .shadow(color: eirs.ink.opacity(0.6), radius: 18, x: 0, y: 12)
        .padding(.horizontal, 14)
        .padding(.bottom, 28 + bottomInset)
    }
}

// MARK: - Sale success

struct TerminalSaleView: View {
    @Environment(\.eirs) private var eirs
    let sale: TerminalSale
    let servedBy: String
    let tablet: Bool
    let bottomInset: CGFloat
    let onNewSale: () -> Void

    var body: some View {
        GeometryReader { proxy in
            if tablet {
                HStack(spacing: 0) {
                    ScrollView {
                        summary
                            .frame(maxWidth: .infinity, minHeight: proxy.size.height, alignment: .leading)
                    }
                    Rectangle().fill(eirs.line).frame(width: 1)
                    ScrollView {
                        TerminalReceiptPanel(sale: sale, servedBy: servedBy, bottomInset: bottomInset)
                    }
                    .frame(width: 379)
                }
            } else {
                ScrollView {
                    VStack(spacing: 0) {
                        summary
                        TerminalReceiptPanel(sale: sale, servedBy: servedBy, bottomInset: bottomInset)
                    }
                }
            }
        }
        .background(eirs.background)
        .contentShape(Rectangle())
        .onTapGesture { }
    }

    private var summary: some View {
        VStack(alignment: .leading, spacing: 22) {
            SuccessMark(icon: EirsIcons.check, size: 76, iconSize: 36)
            VStack(alignment: .leading, spacing: 8) {
                SerifTitle(plain: "Sale ", accent: "registered.", size: tablet ? 56 : 44)
                Text("MoR signed \(sale.number) at \(sale.time) · \(sale.pay.label)")
                    .font(EirsFont.sans(15))
                    .foregroundStyle(eirs.muted)
            }
            if sale.hasChange {
                HStack(alignment: .lastTextBaseline, spacing: 14) {
                    Text("Change due")
                        .font(EirsFont.sans(15))
                        .foregroundStyle(Color.white.opacity(0.75))
                    Text(Fmt.etb(sale.change))
                        .font(EirsFont.serif(44))
                        .foregroundStyle(Color.white)
                }
                .padding(.horizontal, 22)
                .padding(.vertical, 18)
                .background(eirs.ink, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
            }
            actions
        }
        .padding(.horizontal, tablet ? 56 : 20)
        .padding(.vertical, tablet ? 40 : 24)
    }

    private var actions: some View {
        HStack(spacing: 10) {
            Button(action: onNewSale) {
                Text("+ New sale")
                    .font(EirsFont.sans(17, .semibold))
                    .foregroundStyle(Color.white)
                    .frame(maxWidth: 260)
                    .frame(height: 62)
                    .contentShape(Rectangle())
            }
            .buttonStyle(PressFillStyle(normal: eirs.brand, pressed: eirs.brandDark, radius: 16))
            Button { } label: {
                Text("Print")
                    .font(EirsFont.sans(16, .medium))
                    .foregroundStyle(eirs.ink)
                    .padding(.horizontal, 20)
                    .frame(height: 62)
                    .background(Color.white, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                    .solidBorder(eirs.line, radius: 16, width: 1.5)
                    .contentShape(Rectangle())
            }
            .buttonStyle(PlainTapStyle())
        }
    }
}

private struct TerminalReceiptPanel: View {
    @Environment(\.eirs) private var eirs
    let sale: TerminalSale
    let servedBy: String
    let bottomInset: CGFloat

    var body: some View {
        VStack(spacing: 12) {
            VStack(spacing: 10) {
                VStack(spacing: 6) {
                    ForEach(sale.lines.indices, id: \.self) { index in
                        let line = sale.lines[index]
                        HStack(alignment: .top, spacing: 10) {
                            Text("\(line.qty) × \(line.name)")
                                .font(EirsFont.sans(13))
                                .foregroundStyle(eirs.ink)
                                .frame(maxWidth: .infinity, alignment: .leading)
                            Text(line.total)
                                .font(EirsFont.mono(13))
                                .foregroundStyle(eirs.ink)
                        }
                    }
                }
                DashedRule(color: eirs.ink)
            }
            HStack {
                Text("Total").font(EirsFont.sans(16, .bold)).foregroundStyle(eirs.ink)
                Spacer(minLength: 8)
                Text(Fmt.etb(sale.total)).font(EirsFont.mono(16, .medium)).foregroundStyle(eirs.ink)
            }
            HStack(alignment: .top, spacing: 10) {
                Text("Buyer · served by")
                    .frame(maxWidth: .infinity, alignment: .leading)
                Text("\(sale.buyer) · \(servedBy)")
                    .multilineTextAlignment(.trailing)
            }
            .font(EirsFont.sans(13))
            .foregroundStyle(eirs.muted)
            VStack(spacing: 10) {
                DashedRule(color: eirs.ink)
                HStack(spacing: 14) {
                    QrCodeView(content: verificationURL(irn: sale.irn))
                        .frame(width: 96, height: 96)
                    Text("IRN \(sale.irn)")
                        .font(EirsFont.mono(10))
                        .foregroundStyle(eirs.muted)
                        .lineSpacing(4)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
        }
        .padding(.leading, 24)
        .padding(.trailing, 24)
        .padding(.top, 22)
        .padding(.bottom, 30 + bottomInset)
        .frame(maxWidth: .infinity)
        .background(Color.white)
    }
}

// MARK: - Exit confirmation

struct TerminalExitConfirm: View {
    @Environment(\.eirs) private var eirs
    let model: TerminalModel
    let tablet: Bool
    let bottomInset: CGFloat

    var body: some View {
        ZStack(alignment: tablet ? .center : .bottom) {
            if model.confirmOpen {
                eirs.ink.opacity(0.45)
                    .contentShape(Rectangle())
                    .onTapGesture { model.closeExit() }
                    .transition(.opacity)
                card
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .animation(.easeOut(duration: 0.2), value: model.confirmOpen)
    }

    private var card: some View {
        let phoneShape = UnevenRoundedRectangle(topLeadingRadius: 26, topTrailingRadius: 26, style: .continuous)
        let tabletShape = RoundedRectangle(cornerRadius: 22, style: .continuous)
        return VStack(alignment: .leading, spacing: 18) {
            VStack(alignment: .leading, spacing: 6) {
                Text("Exit terminal mode?")
                    .font(EirsFont.serif(30))
                    .foregroundStyle(eirs.ink)
                Text("The app unlocks back to your full workspace. This shift's cart and buyer details are cleared.")
                    .font(EirsFont.sans(14))
                    .foregroundStyle(eirs.muted)
                    .lineSpacing(5)
            }
            stats
            if model.hasCart {
                Text("The open sale (\(model.cartCount)) hasn't been registered and will be discarded.")
                    .font(EirsFont.sans(14))
                    .foregroundStyle(eirs.amberInk)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 12)
                    .background(eirs.amberTint, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
            buttons
        }
        .padding(24)
        .padding(.bottom, tablet ? 0 : bottomInset)
        .frame(maxWidth: tablet ? 480 : .infinity)
        .background {
            if tablet {
                tabletShape.fill(Color.white).shadow(color: eirs.ink.opacity(0.6), radius: 40, x: 0, y: 20)
            } else {
                phoneShape.fill(Color.white).shadow(color: eirs.ink.opacity(0.6), radius: 40, x: 0, y: 20)
            }
        }
        .contentShape(Rectangle())
        .onTapGesture { }
    }

    private var stats: some View {
        let entries: [(String, String)] = [
            ("Sales", "\(model.sales.count)"),
            ("Total", Fmt.etb(model.shiftTotal)),
            ("Cash", Fmt.etb(model.cashTotal)),
        ]
        return HStack(spacing: 0) {
            ForEach(entries.indices, id: \.self) { index in
                VStack(alignment: .leading, spacing: 4) {
                    Text(entries[index].0).font(EirsFont.sans(11)).foregroundStyle(eirs.muted)
                    Text(entries[index].1).font(EirsFont.serif(22)).foregroundStyle(eirs.ink).lineLimit(1)
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .overlay(alignment: .trailing) {
                    if index < entries.count - 1 { Rectangle().fill(eirs.line).frame(width: 1) }
                }
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .solidBorder(eirs.line, radius: 14)
    }

    private var buttons: some View {
        GeometryReader { proxy in
            let unit = max(0, (proxy.size.width - 10) / 2.3)
            HStack(spacing: 10) {
                Button { model.closeExit() } label: {
                    Text("Keep selling")
                        .font(EirsFont.sans(15, .medium))
                        .foregroundStyle(eirs.ink)
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .solidBorder(eirs.line, radius: 14, width: 1.5)
                        .contentShape(Rectangle())
                }
                .buttonStyle(PlainTapStyle())
                .frame(width: unit)
                Button { model.exit() } label: {
                    Text("Exit terminal")
                        .font(EirsFont.sans(15, .semibold))
                        .foregroundStyle(Color.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(eirs.ink, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                        .contentShape(Rectangle())
                }
                .buttonStyle(PlainTapStyle())
                .frame(width: unit * 1.3)
            }
        }
        .frame(height: 56)
    }
}

// MARK: - Loader (brand tile inside a spinning ring, POS `p-spin`)

struct TerminalSpinnerLoader: View {
    @Environment(\.eirs) private var eirs
    let message: LoaderMessage
    @State private var spinning = false

    var body: some View {
        VStack(spacing: 22) {
            ZStack {
                ring
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .fill(eirs.brand)
                    .frame(width: 76, height: 76)
                LineIcon(EirsIcons.shield, size: 34, strokeWidth: 1.8, tint: .white)
            }
            .frame(width: 92, height: 92)
            VStack(spacing: 6) {
                Text(message.title)
                    .font(EirsFont.serif(32, italic: true))
                    .foregroundStyle(eirs.ink)
                    .multilineTextAlignment(.center)
                Text(message.subtitle)
                    .font(EirsFont.sans(14))
                    .foregroundStyle(eirs.muted)
                    .multilineTextAlignment(.center)
            }
            .padding(.horizontal, 20)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.white.opacity(0.55))
        .contentShape(Rectangle())
        .onTapGesture { }
        .onAppear { spinning = true }
    }

    private var ring: some View {
        ZStack {
            Circle().stroke(eirs.tint2, lineWidth: 2)
            Circle()
                .trim(from: 0, to: 0.25)
                .stroke(eirs.brand, style: StrokeStyle(lineWidth: 2, lineCap: .butt))
                .rotationEffect(.degrees(-135))
        }
        .frame(width: 92, height: 92)
        .rotationEffect(.degrees(spinning ? 360 : 0))
        .animation(.linear(duration: 1).repeatForever(autoreverses: false), value: spinning)
    }
}
