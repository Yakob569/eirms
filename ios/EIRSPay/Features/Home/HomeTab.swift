import SwiftUI
import EIRSDomain

/// Home (dashboard) tab body, rendered inside the shell's scrolling page.
@MainActor
struct HomeTab: View {
    @State private var model: HomeModel
    var onOpenInvoice: (String) -> Void
    var onOpenReceipt: (String) -> Void
    var onViewAllInvoices: () -> Void

    init(container: AppContainer,
         onOpenInvoice: @escaping (String) -> Void,
         onOpenReceipt: @escaping (String) -> Void,
         onViewAllInvoices: @escaping () -> Void) {
        _model = State(initialValue: HomeModel(container: container))
        self.onOpenInvoice = onOpenInvoice
        self.onOpenReceipt = onOpenReceipt
        self.onViewAllInvoices = onViewAllInvoices
    }

    var body: some View {
        VStack(spacing: 12) {
            rangeChips
            if let a = model.analytics {
                HomeTotalSalesCard(a: a)
                HomeTiles(a: a)
                HomeSalesTaxCard(a: a)
                HomeTaxByCodeCard(a: a)
                HomeTransactionTypeCard(a: a)
                HomeBranchCard(a: a, selected: model.branch, infos: model.branches, onToggle: { model.toggleBranch($0) })
            }
            HomeRecentActivityCard(items: model.recent, onOpen: openActivity, onViewAll: onViewAllInvoices)
        }
        .frame(maxWidth: .infinity)
        .task { await model.observeBranch() }
        .task { await model.observeRecent() }
        .task(id: model.analyticsKey) { await model.loadAnalytics() }
    }

    private var rangeChips: some View {
        ChipRail {
            ForEach(DashboardRange.allCases) { r in
                EirsChip(label: r.chipLabel, selected: model.range == r, action: { model.selectRange(r) })
            }
        }
    }

    private func openActivity(_ item: ActivityItem) {
        if item.kind == .receipt { onOpenReceipt(item.documentId) } else { onOpenInvoice(item.documentId) }
    }
}
