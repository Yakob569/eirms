import SwiftUI
import EIRSDomain

/// Signed-in shell: white top bar (branch filter, environment pill, avatar), one scrolling page per
/// tab, the Register FAB and the bottom tab bar. Sheets and the terminal-start loader draw above
/// everything. All tab pages stay alive (opacity + hit-testing toggled) so list filters survive.
@MainActor
struct MainScreen<Invoices: View, Receipts: View>: View {
    @Environment(\.eirs) private var eirs
    let container: AppContainer
    let navigator: MainNavigator
    let invoicesTab: (Int, @escaping (String) -> Void) -> Invoices
    let receiptsTab: (@escaping (String) -> Void) -> Receipts

    @State private var model: MainModel
    @State private var rowActions: RowActionsModel
    @State private var tab: MainTab = .home
    @State private var sheet: MainSheet?
    @State private var canceledRequest = 0
    @State private var terminalBranch: Branch?

    init(container: AppContainer,
         navigator: MainNavigator,
         @ViewBuilder invoicesTab: @escaping (Int, @escaping (String) -> Void) -> Invoices,
         @ViewBuilder receiptsTab: @escaping (@escaping (String) -> Void) -> Receipts) {
        self.container = container
        self.navigator = navigator
        self.invoicesTab = invoicesTab
        self.receiptsTab = receiptsTab
        _model = State(initialValue: MainModel(container: container))
        _rowActions = State(initialValue: RowActionsModel(container: container))
    }

    var body: some View {
        LoadingHost(loading: model.loading ?? rowActions.loading) {
            shell
                .eirsSheet(isPresented: sheetBinding(.branch)) { branchSheet }
                .eirsSheet(isPresented: sheetBinding(.user)) { userSheet }
                .eirsSheet(isPresented: sheetBinding(.terminal)) { terminalSheet }
                .eirsSheet(isPresented: sheetBinding(.new)) { registerSheet }
                .eirsSheet(isPresented: sheetBinding(.rowActions)) { rowActionSheet }
                .eirsSheet(isPresented: sheetBinding(.cancelInvoice)) { cancelInvoiceSheet }
        }
        .task { await model.observeSession() }
        .task { await model.observeBranch() }
        .task { await rowActions.observeInvoices() }
        .task { await rowActions.observeReceipts() }
    }

    // MARK: Shell

    private var shell: some View {
        ZStack(alignment: .bottomTrailing) {
            eirs.background.ignoresSafeArea()
            pages
            if tab != .more {
                MainRegisterFab { sheet = .new }
                    .padding(.trailing, 18)
                    .padding(.bottom, 18)
            }
        }
        .safeAreaInset(edge: .top, spacing: 0) {
            MainTopBar(
                tab: tab,
                branchName: model.selectedBranch?.displayName ?? "All branches",
                branchSelected: model.selectedBranch != nil,
                environment: model.session?.merchant.environment ?? "DEV",
                initials: model.session?.user.initials ?? "",
                onBranch: { sheet = .branch },
                onUser: { sheet = .user })
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            MainTabBar(selected: tab, onSelect: { selectTab($0) })
        }
    }

    private var pages: some View {
        ZStack {
            MainTabPage(tab: .home, active: tab == .home, branch: model.selectedBranch) {
                HomeTab(container: container,
                        onOpenInvoice: navigator.openInvoice,
                        onOpenReceipt: navigator.openReceipt,
                        onViewAllInvoices: { selectTab(.invoices) })
            }
            MainTabPage(tab: .invoices, active: tab == .invoices, branch: model.selectedBranch) {
                invoicesTab(canceledRequest, { openRowActions(.invoice($0)) })
            }
            MainTabPage(tab: .receipts, active: tab == .receipts, branch: model.selectedBranch) {
                receiptsTab({ openRowActions(.receipt($0)) })
            }
            MainTabPage(tab: .more, active: tab == .more, branch: model.selectedBranch) {
                MoreTab(container: container,
                        onStartTerminal: { sheet = .terminal },
                        onOpenItems: navigator.openItems,
                        onOpenWithholding: navigator.openWithholding,
                        onOpenCanceledInvoices: {
                            canceledRequest += 1
                            selectTab(.invoices)
                        },
                        onOpenSettings: navigator.openSettings)
            }
        }
    }

    private func selectTab(_ target: MainTab) {
        tab = target
        sheet = nil
    }

    private func sheetBinding(_ target: MainSheet) -> Binding<Bool> {
        Binding<Bool>(
            get: { sheet == target },
            set: { isOn in
                if !isOn && sheet == target { sheet = nil }
            })
    }

    // MARK: Sheets

    private var effectiveTerminalBranch: Branch {
        terminalBranch ?? model.selectedBranch ?? Branch.main
    }

    private var branchSheet: some View {
        MainBranchSheet(selected: model.selectedBranch, branches: model.branches, onPick: { picked in
            model.selectBranch(picked)
            sheet = nil
        })
    }

    private var userSheet: some View {
        MainUserSheet(
            session: model.session,
            onAccountSettings: {
                sheet = nil
                navigator.openSettings(.account)
            },
            onSignOut: {
                sheet = nil
                Task {
                    await model.signOut()
                    navigator.signedOut()
                }
            })
    }

    private var terminalSheet: some View {
        MainTerminalSheet(
            branch: effectiveTerminalBranch,
            onPick: { terminalBranch = $0 },
            onStart: {
                let chosen = effectiveTerminalBranch
                sheet = nil
                Task {
                    await model.startTerminal(chosen, onStarted: { navigator.startTerminal($0) })
                }
            })
    }

    // MARK: Row actions (⋮ on invoice and receipt cards)

    private func openRowActions(_ target: RowActionTarget) {
        rowActions.target = target
        sheet = .rowActions
    }

    @ViewBuilder private var rowActionSheet: some View {
        switch rowActions.target {
        case let .invoice(id)?:
            if let inv = rowActions.invoice(id) {
                RowActionSheet(
                    eyebrow: "\(inv.documentType.rawValue) · \(inv.transactionType.rawValue) · \(inv.status.rawValue)",
                    title: inv.displayNumber,
                    subtitle: inv.buyer.name + " · " + Fmt.etb(inv.totals.grandTotal),
                    items: rowActions.invoiceActions(inv).map { invoiceItem($0, inv) })
            }
        case let .receipt(id)?:
            if let view = rowActions.receipt(id) {
                RowActionSheet(
                    eyebrow: "RECEIPT · " + view.receipt.type.rawValue,
                    title: ReceiptStyle.receiptNo(view.receipt),
                    subtitle: view.buyer.name + " · " + Fmt.etb(view.amount),
                    items: RowActions.forReceipt(view).map { receiptItem($0, view) })
            }
        case nil:
            EmptyView()
        }
    }

    /// Runs a sheet option: close the sheet first, then navigate.
    private func act(_ action: @escaping () -> Void) -> () -> Void {
        return {
            sheet = nil
            action()
        }
    }

    private func invoiceItem(_ action: InvoiceRowAction, _ inv: Invoice) -> RowActionItem {
        switch action {
        case .viewDetails:
            return RowActionItem(id: "view", label: "View details", icon: EirsIcons.eye, action: act { navigator.openInvoice(inv.id) })
        case .makeCreditNote:
            return RowActionItem(id: "cre", label: "Make credit note", icon: EirsIcons.fileMinus,
                                 action: act { navigator.makeAdjustment(inv.id, .cre) })
        case .makeDebitNote:
            return RowActionItem(id: "deb", label: "Make debit note", icon: EirsIcons.fileDiff,
                                 action: act { navigator.makeAdjustment(inv.id, .deb) })
        case .cancel:
            return RowActionItem(id: "cancel", label: "Cancel invoice", icon: EirsIcons.xCircle, danger: true, separatorBefore: true,
                                 action: {
                                     rowActions.cancelInvoiceId = inv.id
                                     rowActions.cancelReason = .customerRequest
                                     sheet = .cancelInvoice
                                 })
        case .makeInterim:
            return RowActionItem(id: "int", label: "Make interim invoice", icon: EirsIcons.fileLines,
                                 action: act { navigator.makeRetainerInvoice(inv.id, .int) })
        case .makeFinal:
            return RowActionItem(id: "fin", label: "Make final invoice", icon: EirsIcons.fileLines,
                                 action: act { navigator.makeRetainerInvoice(inv.id, .fin) })
        }
    }

    private func receiptItem(_ action: ReceiptRowAction, _ view: ReceiptView) -> RowActionItem {
        switch action {
        case .viewDetails:
            return RowActionItem(id: "view", label: "View details", icon: EirsIcons.eye,
                                 action: act { navigator.openReceipt(view.id) })
        case let .viewLinkedInvoice(invoiceId):
            return RowActionItem(id: "linked", label: "View linked invoice", icon: EirsIcons.link,
                                 action: act { navigator.openInvoice(invoiceId) })
        }
    }

    /// The existing cancel sheet, opened from "Cancel invoice" in the ⋮ menu.
    private var cancelInvoiceSheet: some View {
        InvoiceCancelSheet(number: rowActions.cancelInvoiceId.flatMap { rowActions.invoice($0)?.number } ?? "",
                           reason: $rowActions.cancelReason) {
            sheet = nil
            Task { await rowActions.confirmCancel() }
        }
    }

    private var registerSheet: some View {
        MainRegisterSheet(
            onInvoice: {
                sheet = nil
                navigator.registerInvoice()
            },
            onReceipt: {
                sheet = nil
                navigator.registerReceipt()
            })
    }
}

// MARK: - Tab page

/// One tab's scrolling page: eyebrow + serif title, then the tab body. Hidden pages stay in the tree.
private struct MainTabPage<Content: View>: View {
    var tab: MainTab
    var active: Bool
    var branch: Branch?
    @ViewBuilder var content: () -> Content

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                MainPageTitle(tab: tab, branch: branch)
                content().modifier(MainTabRise(active: active))
            }
            .padding(.horizontal, 16)
            .padding(.top, 20)
            .padding(.bottom, 96)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .scrollDismissesKeyboard(.interactively)
        .opacity(active ? 1 : 0)
        .allowsHitTesting(active)
        .accessibilityHidden(!active)
    }
}

/// Replays the 0.3s rise-in each time the tab becomes the selected one.
private struct MainTabRise: ViewModifier {
    var active: Bool
    @State private var risen = false

    func body(content: Content) -> some View {
        content
            .opacity(risen ? 1 : 0)
            .offset(y: risen ? 0 : 12)
            .onAppear {
                if active { replay() } else { risen = true }
            }
            .onChange(of: active) { _, isActive in
                if isActive { replay() }
            }
    }

    private func replay() {
        risen = false
        DispatchQueue.main.async {
            withAnimation(.easeOut(duration: 0.3)) { risen = true }
        }
    }
}

private struct MainPageTitle: View {
    var tab: MainTab
    var branch: Branch?

    private var parts: (eyebrow: String, plain: String, accent: String) {
        let scope = (branch?.displayName ?? "All branches").uppercased()
        switch tab {
        case .home: return ("OVERVIEW · \(scope)", "Sales ", "overview.")
        case .invoices: return ("BROWSE · \(scope)", "Sales ", "invoices.")
        case .receipts: return ("BROWSE · \(scope)", "Your ", "receipts.")
        case .more: return ("MORE · TOOLS & SETUP", "More ", "tools.")
        }
    }

    var body: some View {
        let p = parts
        VStack(alignment: .leading, spacing: 6) {
            Eyebrow(text: p.eyebrow)
            SerifTitle(plain: p.plain, accent: p.accent, size: 36)
        }
        .padding(.bottom, 16)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

// MARK: - Top bar

private struct MainTopBar: View {
    @Environment(\.eirs) private var eirs
    var tab: MainTab
    var branchName: String
    var branchSelected: Bool
    var environment: String
    var initials: String
    var onBranch: () -> Void
    var onUser: () -> Void

    var body: some View {
        HStack(spacing: 8) {
            if tab != .more { branchButton } else { brandMark }
            Spacer(minLength: 0)
            environmentPill
            avatar
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(Color.white.ignoresSafeArea(edges: .top))
        .hairline(.bottom, color: eirs.line)
    }

    private var branchButton: some View {
        let shape = RoundedRectangle(cornerRadius: 12, style: .continuous)
        return Button(action: onBranch) {
            HStack(spacing: 8) {
                LineIcon(EirsIcons.building, size: 15, tint: eirs.muted)
                Text(branchName).font(EirsFont.sans(14, .medium)).foregroundStyle(eirs.ink).lineLimit(1)
                LineIcon(EirsIcons.chevronDown, size: 14, tint: eirs.muted)
            }
            .padding(.horizontal, 12)
            .frame(height: 40)
            .overlay(shape.strokeBorder(branchSelected ? eirs.brand : eirs.line, lineWidth: 1))
            .contentShape(Rectangle())
        }
        .buttonStyle(PlainTapStyle())
    }

    private var brandMark: some View {
        HStack(spacing: 8) {
            LineIcon(EirsIcons.shield, size: 15, strokeWidth: 2.2, tint: .white)
                .frame(width: 28, height: 28)
                .background(eirs.brand, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
            Text("EIRSPay").font(EirsFont.sans(15, .semibold)).foregroundStyle(eirs.ink)
        }
        .frame(height: 40)
    }

    private var environmentPill: some View {
        HStack(spacing: 6) {
            Circle().fill(eirs.brand).frame(width: 6, height: 6)
            Text(environment).font(EirsFont.sans(11, .semibold)).foregroundStyle(eirs.brandDark)
        }
        .padding(.horizontal, 10)
        .frame(height: 28)
        .background(eirs.tint1, in: Capsule())
    }

    private var avatar: some View {
        Button(action: onUser) {
            Text(initials).font(EirsFont.sans(13, .semibold)).foregroundStyle(Color.white)
                .frame(width: 40, height: 40)
                .background(eirs.ink, in: Circle())
                .contentShape(Circle())
        }
        .buttonStyle(PlainTapStyle())
    }
}

// MARK: - FAB

private struct MainRegisterFab: View {
    @Environment(\.eirs) private var eirs
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                LineIcon(EirsIcons.plus, size: 20, strokeWidth: 2.4, tint: .white)
                Text("Register").font(EirsFont.sans(15, .semibold)).foregroundStyle(Color.white)
            }
            .padding(.leading, 16)
            .padding(.trailing, 20)
            .frame(height: 56)
            .contentShape(Rectangle())
        }
        .buttonStyle(PressFillStyle(normal: eirs.brand, pressed: eirs.brandDark, radius: 18))
        .shadow(color: eirs.ink.opacity(0.55), radius: 16, x: 0, y: 8)
    }
}

// MARK: - Tab bar

private struct MainTabItem: Identifiable {
    let tab: MainTab
    let label: String
    let icon: LineIconSpec
    var id: Int { tab.rawValue }
}

private struct MainTabBar: View {
    @Environment(\.eirs) private var eirs
    var selected: MainTab
    var onSelect: (MainTab) -> Void

    private let items: [MainTabItem] = [
        MainTabItem(tab: .home, label: "Home", icon: EirsIcons.grid),
        MainTabItem(tab: .invoices, label: "Invoices", icon: EirsIcons.fileText),
        MainTabItem(tab: .receipts, label: "Receipts", icon: EirsIcons.receipt),
        MainTabItem(tab: .more, label: "More", icon: EirsIcons.sliders),
    ]

    var body: some View {
        HStack(spacing: 0) {
            ForEach(items) { item in
                tabButton(item)
            }
        }
        .padding(.top, 8)
        .padding(.bottom, 10)
        .frame(maxWidth: .infinity)
        .background(Color.white.ignoresSafeArea(edges: .bottom))
        .hairline(.top, color: eirs.line)
    }

    private func tabButton(_ item: MainTabItem) -> some View {
        let on = item.tab == selected
        let fg: Color = on ? eirs.brandDark : eirs.muted
        return Button { onSelect(item.tab) } label: {
            VStack(spacing: 4) {
                LineIcon(item.icon, size: 20, strokeWidth: 1.9, tint: fg)
                    .frame(width: 52, height: 30)
                    .background(on ? eirs.tint1 : Color.clear, in: Capsule())
                Text(item.label)
                    .font(EirsFont.sans(11, on ? .semibold : .medium))
                    .foregroundStyle(fg)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, minHeight: 52, maxHeight: 52)
            .contentShape(Rectangle())
        }
        .buttonStyle(PlainTapStyle())
    }
}
