import SwiftUI
import EIRSDomain

// MARK: - List

@MainActor
struct BranchesScreen: View {
    @Environment(\.eirs) private var eirs
    @State private var model: SettingsModel
    var onBack: () -> Void
    var onOpenBranch: (String) -> Void

    init(container: AppContainer, onBack: @escaping () -> Void, onOpenBranch: @escaping (String) -> Void) {
        _model = State(initialValue: SettingsModel(container: container))
        self.onBack = onBack
        self.onOpenBranch = onOpenBranch
    }

    var body: some View {
        let branches = model.settings?.branches ?? []
        SettingsPageFrame(eyebrow: "SETTINGS · \(branches.count) BRANCHES", plain: "", accent: "Branches", loading: nil,
                          onBack: onBack,
                          trailing: { SettingsPlusButton { Task { let id = await model.addBranch(); onOpenBranch(id) } } },
                          footer: { EmptyView() }) {
            SettingsCountLine(text: "\(branches.filter(\.active).count) of \(branches.count) active")
            ForEach(branches) { branch in
                row(branch)
            }
        }
        .task { await model.observe() }
    }

    private func row(_ branch: BranchProfile) -> some View {
        let systems = model.settings?.systems(inBranch: branch.id).count ?? 0
        return ListCard(action: { onOpenBranch(branch.id) }) {
            HStack(spacing: 8) {
                HStack(spacing: 8) {
                    Text(branch.name).font(EirsFont.sans(16, .semibold)).lineLimit(1)
                    if branch.isMain { StatusPill("Main branch") }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                StatusPill(branch.active ? "Active" : "Inactive")
            }
            HStack(spacing: 8) {
                MonoText(text: branch.code, size: 13, color: eirs.muted)
                Spacer(minLength: 8)
                Text(branch.manager ?? "Unassigned").font(EirsFont.sans(13)).foregroundStyle(eirs.muted)
                    .lineLimit(1).truncationMode(.tail)
            }
            HStack(spacing: 8) {
                Text("\(branch.address.city.isEmpty ? "—" : branch.address.city) · \(branch.address.region.isEmpty ? "—" : branch.address.region)")
                    .font(EirsFont.sans(12)).lineLimit(1)
                Spacer(minLength: 8)
                Text(Fmt.plural(systems, "source system")).font(EirsFont.sans(12)).foregroundStyle(eirs.muted)
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 8)
            .background(eirs.background, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
        }
    }
}

// MARK: - Detail

@MainActor @Observable
final class BranchDetailModel {
    var settings: OrgSettings?
    /// Edits are local until "Save changes".
    var draft: BranchProfile?
    var expandedSystem: String?
    var error: String?
    let id: String
    @ObservationIgnored private let container: AppContainer

    init(container: AppContainer, id: String) {
        self.container = container
        self.id = id
    }

    func observe() async {
        for await value in container.observeSettings() {
            settings = value
            if draft == nil { draft = value.branch(id) }
        }
    }

    var deleteBlocker: String? {
        guard let settings, let branch = settings.branch(id) else { return nil }
        return SettingsRules.branchDeleteBlocker(branch, in: settings)
    }

    func save() async -> Bool {
        guard let draft else { return false }
        do { try await container.manageBranches.save(draft); return true } catch { self.error = "\(error)"; return false }
    }

    func delete() async -> Bool {
        do { try await container.manageBranches.delete(id: id); return true }
        catch SettingsError.blocked(let reason) { error = reason; return false }
        catch { self.error = "\(error)"; return false }
    }

    func addSourceSystem() async -> String? {
        do { return try await container.manageSourceSystems.add(branchId: id).id }
        catch SettingsError.blocked(let reason) { error = reason; return nil }
        catch { return nil }
    }
}

@MainActor
struct BranchDetailScreen: View {
    @Environment(\.eirs) private var eirs
    @State private var model: BranchDetailModel
    var onBack: () -> Void
    var onOpenSourceSystem: (String) -> Void
    var onOpenApiKey: (String) -> Void

    private static let capCaption = "Source systems, API keys and users under this branch are limited to what\u{2019}s selected here."

    init(container: AppContainer, id: String, onBack: @escaping () -> Void,
         onOpenSourceSystem: @escaping (String) -> Void, onOpenApiKey: @escaping (String) -> Void) {
        _model = State(initialValue: BranchDetailModel(container: container, id: id))
        self.onBack = onBack
        self.onOpenSourceSystem = onOpenSourceSystem
        self.onOpenApiKey = onOpenApiKey
    }

    private func text(_ keyPath: WritableKeyPath<BranchProfile, String>) -> Binding<String> {
        Binding(get: { model.draft?[keyPath: keyPath] ?? "" }, set: { value in model.draft?[keyPath: keyPath] = value })
    }

    var body: some View {
        let draft = model.draft
        SettingsPageFrame(eyebrow: (draft?.code ?? "") + (draft?.isMain == true ? " · MAIN BRANCH" : ""),
                          plain: draft?.name ?? "", accent: "", loading: nil, onBack: onBack,
                          trailing: { EmptyView() },
                          footer: {
                              SettingsEditFooter(deleteBlocker: model.deleteBlocker,
                                                 onDelete: { Task { if await model.delete() { onBack() } } },
                                                 onSave: { Task { if await model.save() { onBack() } } })
                          }) {
            if let draft {
                Group {
                    basicCard(draft)
                    SettingsSectionLabel("MANAGER")
                    SettingsCard {
                        ChipMultiSelect(options: (model.settings?.staff ?? []).map {
                            ChipOption(id: $0, label: $0, selected: draft.manager == $0)
                        }, mono: false) { name in model.draft?.manager = name }
                    }
                    SettingsSectionLabel("ADDRESS")
                    addressCard
                }
                Group {
                    SettingsSectionLabel("WHAT THIS BRANCH CAN ISSUE")
                    rulesCard(draft)
                    SettingsSectionLabel("ELIGIBLE TAX TYPES")
                    eligibleCard
                    SettingsSectionLabel("PERMISSIONS")
                    SettingsCard {
                        ChipGroup(title: "Permission ceiling",
                                  options: ChipOption.from(BranchPermission.allCases, selected: draft.permissions),
                                  caption: "Per-user grants on this branch can\u{2019}t go beyond this list.", mono: false) { id in
                            if let value = BranchPermission(rawValue: id) { model.draft?.permissions.toggle(value) }
                        }
                    }
                    SettingsSectionLabel("SOURCE SYSTEMS")
                    linkedCard
                }
            }
        }
        .task { await model.observe() }
    }

    private func basicCard(_ draft: BranchProfile) -> some View {
        SettingsCard {
            SettingsField(label: "Branch name", text: text(\.name), fontSize: 16)
            SettingsField(label: "Branch code", text: text(\.code), mono: true, fontSize: 16)
            SettingsSwitchRow(
                title: "Branch status",
                note: draft.isMain ? "The main branch is always active"
                    : draft.active ? "Accepting documents" : "Paused \u{2014} its source systems can\u{2019}t issue",
                isOn: draft.active, disabled: draft.isMain,
                onToggle: { model.draft?.active.toggle() })
        }
    }

    private var addressCard: some View {
        SettingsCard {
            SettingsFieldGrid {
                SettingsField(label: "City", text: text(\.address.city))
                SettingsField(label: "Sub-city", text: text(\.address.subCity))
                SettingsField(label: "Wereda", text: text(\.address.wereda))
                SettingsField(label: "Kebele", text: text(\.address.kebele))
                SettingsField(label: "House no.", text: text(\.address.houseNumber))
                SettingsField(label: "Locality", text: text(\.address.locality))
                SettingsField(label: "Zone", text: text(\.address.zone))
                SettingsField(label: "Region", text: text(\.address.region))
                SettingsField(label: "Country", text: text(\.address.country))
            }
        }
    }

    private func rulesCard(_ draft: BranchProfile) -> some View {
        SettingsCard(spacing: 18) {
            ChipGroup(title: "Transaction types",
                      options: ChipOption.from(configurableTransactionTypes, selected: draft.transactionTypes)) { id in
                if let value = TransactionType(rawValue: id) { model.draft?.transactionTypes.toggle(value) }
            }
            ChipGroup(title: "Document types",
                      options: ChipOption.from(IssuableDocumentType.allCases, selected: draft.documentTypes)) { id in
                if let value = IssuableDocumentType(rawValue: id) { model.draft?.documentTypes.toggle(value) }
            }
            ChipGroup(title: "Nature of supplies",
                      options: ChipOption.from(SupplyNature.allCases, selected: draft.natures)) { id in
                if let value = SupplyNature(rawValue: id) { model.draft?.natures.toggle(value) }
            }
            ChipGroup(title: "Operations", options: ChipOption.from(ApiOperation.allCases, selected: draft.operations),
                      caption: Self.capCaption) { id in
                if let value = ApiOperation(rawValue: id) { model.draft?.operations.toggle(value) }
            }
        }
    }

    private var eligibleCard: some View {
        let types: [SettingsTaxType] = model.settings.map { SettingsRules.eligibleTaxTypes(branchId: model.id, in: $0) } ?? []
        let options: [ChipOption] = types.isEmpty
            ? [ChipOption(id: "none", label: "None yet", selected: false)]
            : types.map { ChipOption(id: $0.rawValue, label: $0.rawValue, selected: true, derived: $0 == .excise) }
        return SettingsCard(spacing: 8) {
            ChipMultiSelect(options: options)
            Text("Derived from this branch\u{2019}s source systems.").font(EirsFont.sans(12)).foregroundStyle(eirs.muted)
        }
    }

    private var linkedCard: some View {
        let systems = model.settings?.systems(inBranch: model.id) ?? []
        return VStack(spacing: 0) {
            ForEach(systems) { system in
                linkedRow(system)
                Hairline()
            }
            if systems.isEmpty {
                Text("No source systems on this branch yet.").font(EirsFont.sans(13)).foregroundStyle(eirs.muted)
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Hairline()
            }
            if let error = model.error {
                Text(error).font(EirsFont.sans(12)).foregroundStyle(eirs.red)
                    .padding(.horizontal, 16).padding(.top, 10)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            Button {
                Task { if let id = await model.addSourceSystem() { onOpenSourceSystem(id) } }
            } label: {
                HStack(spacing: 8) {
                    LineIcon(EirsIcons.plus, size: 16, strokeWidth: 2.4, tint: eirs.brand)
                    Text("Add source system").font(EirsFont.sans(14, .semibold)).foregroundStyle(eirs.brand)
                }
                .padding(.horizontal, 16)
                .frame(maxWidth: .infinity, minHeight: 50, maxHeight: 50, alignment: .leading)
                .contentShape(Rectangle())
            }
            .buttonStyle(PressFillStyle(normal: .white, pressed: eirs.background, radius: 0))
        }
        .background(Color.white)
        .clipRounded(16)
        .solidBorder(eirs.line, radius: 16)
    }

    private func linkedRow(_ system: SourceSystemProfile) -> some View {
        let keys = model.settings?.keys(forSystem: system.id) ?? []
        let expanded = model.expandedSystem == system.id
        return VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 10) {
                Button { model.expandedSystem = expanded ? nil : system.id } label: {
                    LineIcon(EirsIcons.chevronRight, size: 14, strokeWidth: 2.2, tint: eirs.muted)
                        .rotationEffect(.degrees(expanded ? 90 : 0))
                        .animation(.easeOut(duration: 0.15), value: expanded)
                        .frame(width: 32, height: 32)
                        .contentShape(Rectangle())
                }
                .buttonStyle(PressFillStyle(normal: .clear, pressed: eirs.background, radius: 9))
                Button { onOpenSourceSystem(system.id) } label: {
                    VStack(alignment: .leading, spacing: 3) {
                        Text(system.label).font(EirsFont.sans(14, .medium)).foregroundStyle(eirs.ink).lineLimit(1)
                        Text("\(system.type.rawValue) · \(Fmt.plural(keys.count, "API key"))")
                            .font(EirsFont.sans(12)).foregroundStyle(eirs.muted)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .contentShape(Rectangle())
                }
                .buttonStyle(PlainTapStyle())
                VStack(alignment: .trailing, spacing: 4) {
                    StatusPill(system.active ? "Active" : "Inactive", compact: true)
                    StatusPill(system.verification.rawValue, compact: true)
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            if expanded {
                VStack(alignment: .leading, spacing: 6) {
                    ForEach(keys) { key in
                        Button { onOpenApiKey(key.id) } label: {
                            HStack(spacing: 8) {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(key.label).font(EirsFont.sans(13, .medium)).foregroundStyle(eirs.ink)
                                    MonoText(text: "\(ApiKey.secretPrefix)••••\(key.tail)", size: 10, color: eirs.muted)
                                }
                                .frame(maxWidth: .infinity, alignment: .leading)
                                StatusPill(key.active ? "Active" : "Deactivated", compact: true)
                            }
                            .padding(.horizontal, 10)
                            .padding(.vertical, 9)
                            .background(eirs.background, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(PlainTapStyle())
                    }
                    if keys.isEmpty {
                        Text("No API keys yet.").font(EirsFont.sans(12)).foregroundStyle(eirs.muted)
                    }
                }
                .padding(.leading, 56)
                .padding(.trailing, 14)
                .padding(.bottom, 12)
            }
        }
    }
}
