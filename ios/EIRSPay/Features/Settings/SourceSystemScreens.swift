import SwiftUI
import EIRSDomain

// MARK: - List

@MainActor
struct SourceSystemsScreen: View {
    @Environment(\.eirs) private var eirs
    @State private var model: SettingsModel
    var onBack: () -> Void
    var onOpenSourceSystem: (String) -> Void

    init(container: AppContainer, onBack: @escaping () -> Void, onOpenSourceSystem: @escaping (String) -> Void) {
        _model = State(initialValue: SettingsModel(container: container))
        self.onBack = onBack
        self.onOpenSourceSystem = onOpenSourceSystem
    }

    var body: some View {
        let systems = model.settings?.sourceSystems ?? []
        let verified = systems.filter { $0.verification == .verified }.count
        SettingsPageFrame(eyebrow: "SETTINGS · " + Fmt.plural(systems.count, "system").uppercased(), plain: "Source ",
                          accent: "systems", loading: nil, onBack: onBack,
                          trailing: {
                              SettingsPlusButton {
                                  guard let branchId = model.defaultBranchId else { return }
                                  Task { if let id = await model.addSourceSystem(branchId: branchId) { onOpenSourceSystem(id) } }
                              }
                          },
                          footer: { EmptyView() }) {
            exciseBanner
            SettingsCountLine(text: Fmt.plural(systems.count, "source system") + " · \(verified) verified")
            ForEach(systems) { system in
                row(system)
            }
        }
        .task { await model.observe() }
    }

    /// Merchant-wide excise switch, above the list.
    private var exciseBanner: some View {
        let on = model.settings?.merchantExcise ?? false
        let shape = RoundedRectangle(cornerRadius: 16, style: .continuous)
        return VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 8) {
                        Text("Merchant excise").font(EirsFont.sans(15, .semibold))
                        StatusPill(on ? "Enabled" : "Not enabled", background: .white)
                    }
                    Text("Business-wide").font(EirsFont.sans(12)).foregroundStyle(eirs.brandDark)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                EirsSwitch(isOn: on) { model.setExcise(!on) }
            }
            Text(on ? "EXCISE is merged into every source system\u{2019}s tax types and can be granted to API keys."
                    : "Applies to your whole business, not per till. When on, EXCISE joins every source system\u{2019}s tax types.")
                .font(EirsFont.sans(12)).foregroundStyle(eirs.brandDark).lineSpacing(3)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(eirs.tint1, in: shape)
        .overlay(shape.strokeBorder(eirs.tint2, lineWidth: 1))
    }

    private func row(_ system: SourceSystemProfile) -> some View {
        let excise = model.settings?.merchantExcise ?? false
        let branchName = model.settings?.branch(system.branchId)?.name ?? "—"
        let taxes = SettingsRules.effectiveTaxTypes(system, excise: excise).map {
            ChipOption(id: $0.rawValue, label: $0.rawValue, selected: true, derived: $0 == .excise)
        }
        return ListCard(action: { onOpenSourceSystem(system.id) }) {
            HStack(spacing: 8) {
                HStack(spacing: 8) {
                    Text(system.label).font(EirsFont.sans(15, .semibold)).lineLimit(1)
                    Text(system.type.rawValue)
                        .font(EirsFont.mono(10)).foregroundStyle(eirs.muted).lineLimit(1)
                        .padding(.horizontal, 5).padding(.vertical, 2)
                        .solidBorder(eirs.line, radius: 5)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                StatusPill(system.verification.rawValue)
            }
            HStack(spacing: 8) {
                Text(branchName).font(EirsFont.sans(13)).foregroundStyle(eirs.muted)
                Spacer(minLength: 8)
                StatusPill(system.active ? "Active" : "Inactive")
            }
            ChipMultiSelect(options: taxes, small: true)
        }
    }
}

// MARK: - Detail

@MainActor @Observable
final class SourceSystemDetailModel {
    var settings: OrgSettings?
    var draft: SourceSystemProfile?
    var hsn = ""
    var loading: LoaderMessage?
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
            if draft == nil { draft = value.sourceSystem(id) }
        }
    }

    var deleteBlocker: String? {
        guard let settings, let system = settings.sourceSystem(id) else { return nil }
        return SettingsRules.sourceSystemDeleteBlocker(system, in: settings)
    }

    func save() async -> Bool {
        guard let draft else { return false }
        do { try await container.manageSourceSystems.save(draft); return true }
        catch SettingsError.blocked(let reason) { error = reason; return false }
        catch { return false }
    }

    func delete() async -> Bool {
        do { try await container.manageSourceSystems.delete(id: id); return true }
        catch SettingsError.blocked(let reason) { error = reason; return false }
        catch { return false }
    }

    /// Runs the probe for this system; only the verification fields are taken from the result so
    /// unsaved edits stay in the draft.
    func verify() async {
        guard loading == nil else { return }
        loading = LoaderMessage(title: "Verifying with MoR", subtitle: "Sending a probe for " + (draft?.label ?? ""))
        if let result = try? await container.manageSourceSystems.verify(id: id, hsn: hsn) {
            draft?.verification = result.verification
            draft?.verificationNote = result.verificationNote
        }
        loading = nil
    }
}

@MainActor
struct SourceSystemDetailScreen: View {
    @Environment(\.eirs) private var eirs
    @State private var model: SourceSystemDetailModel
    var onBack: () -> Void
    var onOpenBranch: (String) -> Void

    init(container: AppContainer, id: String, onBack: @escaping () -> Void, onOpenBranch: @escaping (String) -> Void) {
        _model = State(initialValue: SourceSystemDetailModel(container: container, id: id))
        self.onBack = onBack
        self.onOpenBranch = onOpenBranch
    }

    private func text(_ keyPath: WritableKeyPath<SourceSystemProfile, String>) -> Binding<String> {
        Binding(get: { model.draft?[keyPath: keyPath] ?? "" }, set: { value in model.draft?[keyPath: keyPath] = value })
    }

    var body: some View {
        let draft = model.draft
        let branch = draft.flatMap { model.settings?.branch($0.branchId) }
        SettingsPageFrame(eyebrow: (draft?.type.rawValue ?? "") + " · " + (branch?.name.uppercased() ?? ""),
                          plain: draft?.label ?? "", accent: "", loading: model.loading, onBack: onBack,
                          trailing: { EmptyView() },
                          footer: {
                              SettingsEditFooter(deleteBlocker: model.deleteBlocker,
                                                 onDelete: { Task { if await model.delete() { onBack() } } },
                                                 onSave: { Task { if await model.save() { onBack() } } })
                          }) {
            if let draft {
                Group {
                    basicCard(draft, branch: branch)
                    SettingsSectionLabel("SYSTEM TYPE")
                    SettingsCard {
                        ChipMultiSelect(options: SourceSystemType.allCases.map {
                            ChipOption(id: $0.rawValue, label: $0.rawValue, selected: draft.type == $0)
                        }) { id in
                            if let value = SourceSystemType(rawValue: id) { model.draft?.type = value }
                        }
                    }
                    SettingsSectionLabel("BRANCH")
                    branchCard(draft)
                    SettingsSectionLabel("STAFF")
                    SettingsCard {
                        SettingsFieldGrid {
                            SettingsField(label: "Cashier", text: text(\.cashier), placeholder: "Optional")
                            SettingsField(label: "Sales person", text: text(\.salesPerson), placeholder: "Optional")
                        }
                    }
                }
                Group {
                    taxCard(draft)
                    SettingsSectionLabel("VERIFICATION")
                    verificationCard(draft)
                    SettingsRowsCard(rows: [
                        ("System ID", draft.systemId),
                        ("Seller TIN", model.settings?.seller.tin ?? "—"),
                        ("Last document no.", draft.lastDocument)
                    ], monoValues: true)
                    if let error = model.error {
                        SettingsNote(error)
                    }
                }
            }
        }
        .task { await model.observe() }
    }

    private func basicCard(_ draft: SourceSystemProfile, branch: BranchProfile?) -> some View {
        SettingsCard {
            SettingsField(label: "Label", text: text(\.label), fontSize: 16)
            SettingsSwitchRow(title: "Status", note: draft.active ? "Can issue documents" : "Paused", isOn: draft.active,
                              onToggle: { model.draft?.active.toggle() }) {
                if let branch {
                    TextLink("\(branch.name) →", size: 12) { onOpenBranch(branch.id) }
                }
            }
        }
    }

    /// Inactive branches are greyed out unless the system already sits there.
    private func branchCard(_ draft: SourceSystemProfile) -> some View {
        let branches = model.settings?.branches ?? []
        return SettingsCard(spacing: 8) {
            ChipMultiSelect(options: branches.map {
                ChipOption(id: $0.id, label: $0.name, selected: draft.branchId == $0.id, disabled: !SettingsRules.canAssign(draft, to: $0))
            }, mono: false) { id in model.draft?.branchId = id }
            Text("Inactive branches can\u{2019}t take source systems.").font(EirsFont.sans(12)).foregroundStyle(eirs.muted)
        }
    }

    private func taxCard(_ draft: SourceSystemProfile) -> some View {
        let excise = model.settings?.merchantExcise ?? false
        var options = ChipOption.from(SettingsTaxType.selectable, selected: draft.taxTypes)
        if excise { options.append(ChipOption(id: "EXCISE", label: "EXCISE", selected: true, derived: true)) }
        return SettingsCard(spacing: 18) {
            ChipGroup(title: "Tax types", options: options,
                      caption: excise ? "EXCISE is derived from your merchant excise status, not set here."
                                      : "EXCISE is added here automatically once excise is enabled for your business.") { id in
                if let value = SettingsTaxType(rawValue: id), value != .excise { model.draft?.taxTypes.toggle(value) }
            }
        }
    }

    private func verificationCard(_ draft: SourceSystemProfile) -> some View {
        SettingsCard {
            HStack(spacing: 10) {
                StatusPill(draft.verification.rawValue)
                Text(draft.verificationNote).font(EirsFont.sans(12)).foregroundStyle(eirs.muted).lineSpacing(2)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            SettingsField(label: "HSN for the probe", text: $model.hsn, mono: true, placeholder: "8471", keyboard: .numberPad, fontSize: 16)
            Button { Task { await model.verify() } } label: {
                HStack {
                    Text("Run verification").font(EirsFont.sans(14, .semibold)).foregroundStyle(eirs.brand)
                    Spacer(minLength: 8)
                    LineIcon(EirsIcons.arrowRight, size: 16, tint: eirs.brand)
                }
                .padding(.horizontal, 16)
                .frame(height: 46)
                .contentShape(Rectangle())
            }
            .buttonStyle(PressFillStyle(normal: .white, pressed: eirs.tint1, radius: 12))
            .solidBorder(eirs.brand, radius: 12, width: 1.5)
        }
    }
}
