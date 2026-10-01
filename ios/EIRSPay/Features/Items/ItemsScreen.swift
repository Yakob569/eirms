import SwiftUI
import EIRSDomain

enum ItemStatusFilter: CaseIterable {
    case all, active, inactive

    var label: String {
        switch self {
        case .all: return "Any status"
        case .active: return "Active"
        case .inactive: return "Inactive"
        }
    }
}

@MainActor @Observable
final class ItemsModel {
    private let container: AppContainer

    var catalog: [CatalogItem] = []
    var query: String = ""
    var nature: NatureOfSupply?
    var status: ItemStatusFilter = .all

    init(container: AppContainer) { self.container = container }

    func observe() async {
        for await value in container.observeCatalog() { catalog = value }
    }

    var eyebrow: String {
        "CATALOG · \(catalog.count) ITEMS · \(catalog.filter { $0.active }.count) ACTIVE"
    }

    var items: [CatalogItem] {
        let q = query.trimmingCharacters(in: .whitespaces).lowercased()
        return catalog.filter { item in
            if let selected = nature, item.nature != selected { return false }
            switch status {
            case .all: break
            case .active: if !item.active { return false }
            case .inactive: if item.active { return false }
            }
            if q.isEmpty { return true }
            return "\(item.code) \(item.description)".lowercased().contains(q)
        }
    }

    func toggle(_ item: CatalogItem) async {
        await container.setCatalogItemActive(code: item.code, active: !item.active)
    }
}

@MainActor
struct ItemsScreen: View {
    @Environment(\.eirs) private var eirs
    @State private var model: ItemsModel
    private let onBack: () -> Void
    private let onNewItem: () -> Void
    private let onEditItem: (String) -> Void

    init(container: AppContainer, onBack: @escaping () -> Void, onNewItem: @escaping () -> Void,
         onEditItem: @escaping (String) -> Void) {
        _model = State(initialValue: ItemsModel(container: container))
        self.onBack = onBack
        self.onNewItem = onNewItem
        self.onEditItem = onEditItem
    }

    private var natureOptions: [NatureOfSupply?] { [nil, NatureOfSupply.goods, NatureOfSupply.service] }

    private func natureLabel(_ nature: NatureOfSupply?) -> String {
        guard let nature else { return "All" }
        return nature == .goods ? "Goods" : "Services"
    }

    var body: some View {
        let items = model.items
        ViewScaffold(
            padding: EdgeInsets(top: 16, leading: 16, bottom: 40, trailing: 16),
            header: {
                ViewHeader(eyebrow: model.eyebrow, onBack: onBack,
                           title: { ViewTitle(plain: "Items ", accent: "catalog") },
                           trailing: {
                               IconSquareButton(icon: EirsIcons.plus, filled: true, strokeWidth: 2.4) { onNewItem() }
                           })
            },
            content: {
                SearchField(text: $model.query, placeholder: "Code or description")
                natureRail
                statusRail
                ForEach(items) { item in
                    ItemsListCard(item: item, onOpen: { onEditItem(item.code) },
                             onToggle: { Task { await model.toggle(item) } })
                }
                if items.isEmpty { EmptyState(text: "No items match these filters.") }
            })
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
        .task { await model.observe() }
    }

    private var natureRail: some View {
        ChipRail {
            ForEach(natureOptions.indices, id: \.self) { index in
                let option: NatureOfSupply? = natureOptions[index]
                EirsChip(label: natureLabel(option), selected: model.nature == option) { model.nature = option }
            }
        }
    }

    private var statusRail: some View {
        ChipRail {
            ForEach(ItemStatusFilter.allCases, id: \.self) { status in
                EirsChip(label: status.label, selected: model.status == status) { model.status = status }
            }
        }
    }
}

/// Catalog card. Built without an outer Button so the active switch (itself a Button) stays tappable.
private struct ItemsListCard: View {
    @Environment(\.eirs) private var eirs
    var item: CatalogItem
    var onOpen: () -> Void
    var onToggle: () -> Void

    var body: some View {
        let hs: String = item.hsCode.trimmingCharacters(in: .whitespaces).isEmpty ? "—" : item.hsCode
        VStack(alignment: .leading, spacing: 9) {
            HStack(alignment: .top, spacing: 10) {
                VStack(alignment: .leading, spacing: 3) {
                    Text(item.description).font(EirsFont.sans(15, .medium))
                    MonoText(text: "\(item.code) · HS \(hs)", size: 11, color: eirs.muted)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                EirsSwitch(isOn: item.active, onToggle: onToggle)
            }
            bottomRow
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .solidBorder(eirs.line, radius: 16)
        .contentShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .onTapGesture { onOpen() }
        .opacity(item.active ? 1 : 0.62)
    }

    private var bottomRow: some View {
        HStack {
            HStack(spacing: 6) {
                ItemNatureBadge(nature: item.nature)
                CodeTag(text: item.taxCode.rawValue)
                Text(item.unit).font(EirsFont.sans(11)).foregroundStyle(eirs.muted)
            }
            Spacer(minLength: 8)
            VStack(alignment: .trailing, spacing: 1) {
                MonoText(text: Fmt.f2(item.unitPrice), size: 14, weight: .medium)
                Text("incl. tax " + Fmt.f2(item.grossPrice)).font(EirsFont.sans(10)).foregroundStyle(eirs.muted)
            }
        }
    }
}

struct ItemNatureBadge: View {
    @Environment(\.eirs) private var eirs
    var nature: NatureOfSupply
    var body: some View {
        if nature == .goods {
            Badge(text: nature.rawValue, background: eirs.tint1, content: eirs.brandDark)
        } else {
            Badge(text: nature.rawValue, background: eirs.neutralChip, content: eirs.ink)
        }
    }
}
