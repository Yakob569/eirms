import SwiftUI
import EIRSDomain

/// Terminal mode: a full-screen, locked point-of-sale for sales staff on phones and counter tablets.
/// There is no swipe-back; the only way out is "Hold to exit" then the confirmation, which wipes all state.
@MainActor
struct TerminalScreen: View {
    @Environment(\.eirs) private var eirs
    @State private var model: TerminalModel
    let branch: Branch
    let onExit: () -> Void

    init(container: AppContainer, branch: Branch, onExit: @escaping () -> Void) {
        _model = State(initialValue: TerminalModel(container: container, branch: branch))
        self.branch = branch
        self.onExit = onExit
    }

    var body: some View {
        GeometryReader { proxy in
            let tablet = proxy.size.width >= 840
            let top = proxy.safeAreaInsets.top
            let bottom = min(proxy.safeAreaInsets.bottom, 34)
            ZStack {
                TerminalContent(model: model, branch: branch, tablet: tablet, topInset: top, bottomInset: bottom)
                    .blur(radius: model.busy == nil ? 0 : 6)
                    .allowsHitTesting(model.busy == nil)
                if let busy = model.busy {
                    TerminalSpinnerLoader(message: busy)
                        .transition(.opacity)
                        .zIndex(10)
                }
            }
            .animation(.easeOut(duration: 0.2), value: model.busy)
        }
        .ignoresSafeArea(.container)
        .background(eirs.background.ignoresSafeArea())
        .interactiveDismissDisabled(true)
        .onAppear { UIApplication.shared.isIdleTimerDisabled = true }
        .onDisappear {
            UIApplication.shared.isIdleTimerDisabled = false
            model.teardown()
        }
        .task { await model.observeSession() }
        .task { await model.observeCatalog() }
        .task { await model.tick() }
        .onChange(of: model.exited) { _, exited in
            if exited {
                model.consumeExit()
                onExit()
            }
        }
    }
}

private struct TerminalContent: View {
    @Environment(\.eirs) private var eirs
    let model: TerminalModel
    let branch: Branch
    let tablet: Bool
    let topInset: CGFloat
    let bottomInset: CGFloat

    var body: some View {
        ZStack(alignment: .bottom) {
            mainColumn
            if !tablet {
                if model.hasCart && !model.cartOpen && model.sale == nil {
                    TerminalReviewBar(model: model, bottomInset: bottomInset)
                        .transition(.opacity.combined(with: .offset(y: 12)))
                }
                phoneCartSheet
            }
            TerminalExitConfirm(model: model, tablet: tablet, bottomInset: bottomInset)
        }
        .background(eirs.background)
        .animation(.easeOut(duration: 0.2), value: model.hasCart)
        .animation(.easeOut(duration: 0.2), value: model.sale != nil)
    }

    private var mainColumn: some View {
        VStack(spacing: 0) {
            Rectangle().fill(eirs.brand).frame(height: 5)
            TerminalHeader(model: model, branch: branch, tablet: tablet, topInset: topInset)
            ZStack {
                HStack(spacing: 0) {
                    TerminalItemsPane(model: model, tablet: tablet, bottomInset: bottomInset)
                    if tablet {
                        Rectangle().fill(eirs.line).frame(width: 1)
                        TerminalCartPanel(model: model, tablet: true, bottomInset: bottomInset)
                            .frame(width: 389)
                            .background(Color.white)
                    }
                }
                if let sale = model.sale {
                    TerminalSaleView(sale: sale, servedBy: model.firstName, tablet: tablet,
                                     bottomInset: bottomInset, onNewSale: { model.newSale() })
                        .transition(.opacity)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }

    private var phoneCartSheet: some View {
        ZStack(alignment: .bottom) {
            if model.cartOpen {
                eirs.ink.opacity(0.4)
                    .contentShape(Rectangle())
                    .onTapGesture { model.closeCart() }
                    .transition(.opacity)
                VStack(spacing: 0) {
                    Capsule().fill(eirs.line).frame(width: 40, height: 5)
                        .padding(.top, 10)
                        .padding(.bottom, 4)
                    TerminalCartPanel(model: model, tablet: false, bottomInset: bottomInset)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(
                    UnevenRoundedRectangle(topLeadingRadius: 26, topTrailingRadius: 26, style: .continuous)
                        .fill(Color.white)
                )
                .padding(.top, topInset + 66)
                .transition(.move(edge: .bottom))
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .animation(.timingCurve(0.2, 0.8, 0.2, 1, duration: 0.28), value: model.cartOpen)
    }
}

private struct TerminalItemsPane: View {
    @Environment(\.eirs) private var eirs
    let model: TerminalModel
    let tablet: Bool
    let bottomInset: CGFloat

    var body: some View {
        VStack(spacing: 12) {
            HStack(spacing: 8) {
                categoryRail
                if tablet {
                    TerminalFindField(text: Binding(get: { model.query }, set: { model.setQuery($0) }))
                }
            }
            grid
        }
        .padding(.horizontal, tablet ? 18 : 14)
        .padding(.top, tablet ? 16 : 14)
        .padding(.bottom, tablet ? 16 : 0)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
    }

    private var categoryRail: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(model.categories, id: \.self) { category in
                    let on = model.category == category
                    Button { model.pickCategory(category) } label: {
                        Text(category)
                            .font(EirsFont.sans(14, .medium))
                            .foregroundStyle(on ? Color.white : eirs.ink)
                            .lineLimit(1)
                            .padding(.horizontal, 16)
                            .frame(height: 44)
                            .background(on ? eirs.ink : Color.white, in: Capsule())
                            .overlay(Capsule().strokeBorder(on ? eirs.ink : eirs.line, lineWidth: 1))
                            .contentShape(Capsule())
                    }
                    .buttonStyle(PlainTapStyle())
                }
            }
        }
        .frame(height: 44)
    }

    private var grid: some View {
        let columns: [GridItem] = Array(repeating: GridItem(.flexible(), spacing: 10), count: tablet ? 4 : 2)
        let tileHeight: CGFloat = tablet ? 124 : 112
        return ScrollView {
            LazyVGrid(columns: columns, spacing: 10) {
                ForEach(model.tiles) { item in
                    TerminalItemTile(item: item, qty: model.qtyOf(item.code), height: tileHeight, onTap: { model.add(item) })
                }
            }
            .padding(.bottom, tablet ? 0 : 110 + bottomInset)
        }
        .frame(maxHeight: .infinity)
    }
}
