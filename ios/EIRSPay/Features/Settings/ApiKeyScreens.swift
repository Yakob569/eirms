import SwiftUI
import EIRSDomain

// MARK: - List

@MainActor
struct ApiKeysScreen: View {
    @Environment(\.eirs) private var eirs
    @State private var model: SettingsModel
    var onBack: () -> Void
    var onOpenKey: (String?) -> Void

    init(container: AppContainer, onBack: @escaping () -> Void, onOpenKey: @escaping (String?) -> Void) {
        _model = State(initialValue: SettingsModel(container: container))
        self.onBack = onBack
        self.onOpenKey = onOpenKey
    }

    var body: some View {
        let keys = model.settings?.apiKeys ?? []
        let active = keys.filter(\.active).count
        SettingsPageFrame(eyebrow: "SETTINGS · \(active) ACTIVE", plain: "API ", accent: "keys", loading: nil, onBack: onBack,
                          trailing: { SettingsPlusButton { onOpenKey(nil) } },
                          footer: { EmptyView() }) {
            SettingsCountLine(text: Fmt.plural(active, "active key") + " of \(keys.count)")
            ForEach(keys) { key in
                row(key)
            }
        }
        .task { await model.observe() }
    }

    private func row(_ key: ApiKey) -> some View {
        let system = model.settings?.sourceSystem(key.sourceSystemId)
        let branch = system.flatMap { model.settings?.branch($0.branchId) }
        return ListCard(opacity: key.active ? 1 : 0.6, action: { onOpenKey(key.id) }) {
            HStack(spacing: 8) {
                Text(key.label).font(EirsFont.sans(15, .semibold)).lineLimit(1)
                    .frame(maxWidth: .infinity, alignment: .leading)
                StatusPill(key.active ? "Active" : "Deactivated")
            }
            MonoText(text: key.masked, size: 11, lineLimit: 1)
                .padding(.horizontal, 10)
                .padding(.vertical, 8)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(eirs.background, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
            HStack(spacing: 8) {
                Text("\(system?.label ?? "—") · \(branch?.name ?? "—")").font(EirsFont.sans(12)).foregroundStyle(eirs.muted)
                    .lineLimit(1)
                Spacer(minLength: 8)
                Text(key.created).font(EirsFont.sans(12)).foregroundStyle(eirs.muted).lineLimit(1)
            }
            Text(Fmt.plural(key.operations.count, "operation") + " · " + Fmt.plural(key.transactionTypes.count, "transaction type"))
                .font(EirsFont.sans(12)).foregroundStyle(eirs.muted)
        }
    }
}

// MARK: - Detail / create

@MainActor @Observable
final class ApiKeyDetailModel {
    var settings: OrgSettings?
    /// nil while creating; set to the new key's id once it exists.
    var keyId: String?
    /// The editable fields. For an existing key every change is saved straight away.
    var working: ApiKeyDraft?
    /// Full secret, held only by this screen right after creation.
    var secret: String?
    var assignOpen = false
    var loading: LoaderMessage?
    let copier = InvCopyFeedback()
    @ObservationIgnored private let container: AppContainer

    init(container: AppContainer, id: String?) {
        self.container = container
        self.keyId = id
    }

    var isNew: Bool { keyId == nil }
    var key: ApiKey? { keyId.flatMap { settings?.apiKey($0) } }

    func observe() async {
        for await value in container.observeSettings() {
            settings = value
            if working == nil { working = initialDraft(value) }
        }
    }

    private func initialDraft(_ settings: OrgSettings) -> ApiKeyDraft? {
        if let keyId {
            guard let key = settings.apiKey(keyId) else { return nil }
            return ApiKeyDraft(label: key.label, sourceSystemId: key.sourceSystemId, operations: key.operations,
                               transactionTypes: key.transactionTypes, taxTypes: key.taxTypes, users: key.users)
        }
        let system = settings.sourceSystems.first { $0.active } ?? settings.sourceSystems.first
        return ApiKeyDraft(label: "", sourceSystemId: system?.id ?? "", operations: [.singleRegister, .receiptIssue],
                           transactionTypes: [.b2c], taxTypes: [.vat15], users: [])
    }

    var ceiling: KeyCeiling? {
        guard let settings, let working else { return nil }
        return SettingsRules.keyCeiling(sourceSystemId: working.sourceSystemId, in: settings)
    }

    func update(_ change: (inout ApiKeyDraft) -> Void) {
        guard var value = working else { return }
        change(&value)
        working = value
        guard var key else { return }
        key.label = value.label
        key.operations = value.operations
        key.transactionTypes = value.transactionTypes
        key.taxTypes = value.taxTypes
        key.users = value.users
        let toSave = key
        Task { try? await container.manageApiKeys.save(toSave) }
    }

    func create() async {
        guard isNew, let working, loading == nil else { return }
        loading = LoaderMessage(title: "Creating API key", subtitle: "Binding it to its source system")
        if let created = try? await container.manageApiKeys.create(working) {
            keyId = created.key.id
            secret = created.secret
            self.working = ApiKeyDraft(label: created.key.label, sourceSystemId: created.key.sourceSystemId,
                                       operations: created.key.operations, transactionTypes: created.key.transactionTypes,
                                       taxTypes: created.key.taxTypes, users: created.key.users)
        }
        loading = nil
    }

    func revoke() async {
        guard let keyId else { return }
        try? await container.manageApiKeys.revoke(id: keyId)
    }
}

@MainActor
struct ApiKeyDetailScreen: View {
    @Environment(\.eirs) private var eirs
    @State private var model: ApiKeyDetailModel
    var onBack: () -> Void

    init(container: AppContainer, id: String?, onBack: @escaping () -> Void) {
        _model = State(initialValue: ApiKeyDetailModel(container: container, id: id))
        self.onBack = onBack
    }

    private var system: SourceSystemProfile? { model.working.flatMap { model.settings?.sourceSystem($0.sourceSystemId) } }
    private var branch: BranchProfile? { system.flatMap { model.settings?.branch($0.branchId) } }

    var body: some View {
        let isNew = model.isNew
        let active = model.key?.active ?? true
        SettingsPageFrame(eyebrow: isNew ? "API KEYS · NEW" : "API KEY · " + (system?.label.uppercased() ?? ""),
                          plain: isNew ? "New " : (model.working?.label ?? ""), accent: isNew ? "key" : "",
                          loading: model.loading, onBack: onBack,
                          trailing: { EmptyView() },
                          footer: { footer(isNew: isNew, active: active) }) {
            if let working = model.working {
                Group {
                    if let secret = model.secret { revealPanel(secret) }
                    identityCard(working, isNew: isNew, active: active)
                    SettingsSectionLabel("SOURCE SYSTEM")
                    systemCard(working, isNew: isNew)
                    SettingsSectionLabel("WHAT THIS KEY CAN DO")
                    capabilitiesCard(working)
                }
                Group {
                    usersHeader
                    usersCard(working)
                    if !isNew && !active {
                        Text("This key is deactivated. Requests signed with it are rejected.")
                            .font(EirsFont.sans(13)).foregroundStyle(eirs.redInk).lineSpacing(3)
                            .padding(.horizontal, 14).padding(.vertical, 12)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(eirs.redTint, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                }
            }
        }
        .task { await model.observe() }
    }

    @ViewBuilder private func footer(isNew: Bool, active: Bool) -> some View {
        if isNew {
            StickyBottomBar(spacing: 8) {
                PrimaryButton("Create key", height: 52, fontSize: 15) { Task { await model.create() } }
            }
        } else if active {
            StickyBottomBar(spacing: 8) {
                Button { Task { await model.revoke() } } label: {
                    Text("Revoke key").font(EirsFont.sans(15, .semibold)).foregroundStyle(eirs.red)
                        .padding(.horizontal, 18)
                        .frame(maxWidth: .infinity, minHeight: 52, maxHeight: 52, alignment: .leading)
                        .contentShape(Rectangle())
                }
                .buttonStyle(PressFillStyle(normal: .white, pressed: eirs.redTint, radius: 14))
                .solidBorder(eirs.red, radius: 14, width: 1.5)
            }
        }
    }

    /// One-time reveal right after creation.
    private func revealPanel(_ secret: String) -> some View {
        let copied = model.copier.copiedKey == "apikey"
        return VStack(alignment: .leading, spacing: 10) {
            Text("Copy this key now").font(EirsFont.sans(14, .semibold)).foregroundStyle(Color.white)
            Text("It\u{2019}s shown once. After you leave this screen only the last four characters stay visible.")
                .font(EirsFont.sans(12)).foregroundStyle(Color.white.opacity(0.75)).lineSpacing(3)
                .fixedSize(horizontal: false, vertical: true)
            HStack(spacing: 8) {
                Text(secret).font(EirsFont.mono(11)).foregroundStyle(Color.white)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .textSelection(.enabled)
                Button { model.copier.copy(key: "apikey", text: secret) } label: {
                    Text(copied ? "Copied" : "Copy").font(EirsFont.sans(12, .semibold)).foregroundStyle(Color.white)
                        .padding(.horizontal, 12)
                        .frame(height: 32)
                        .contentShape(Rectangle())
                }
                .buttonStyle(PressFillStyle(normal: eirs.brand, pressed: eirs.brandDark, radius: 8))
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(Color.white.opacity(0.1), in: RoundedRectangle(cornerRadius: 10, style: .continuous))
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(eirs.ink, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private func identityCard(_ working: ApiKeyDraft, isNew: Bool, active: Bool) -> some View {
        SettingsCard {
            SettingsField(label: "Key label",
                          text: Binding(get: { model.working?.label ?? "" }, set: { value in model.update { $0.label = value } }),
                          placeholder: "e.g. Front Counter · Till 3", fontSize: 16)
            HStack(spacing: 10) {
                Text("Bound to \(system?.label ?? "—") · \(branch?.name ?? "—") branch")
                    .font(EirsFont.sans(12)).foregroundStyle(eirs.muted).lineSpacing(2)
                    .frame(maxWidth: .infinity, alignment: .leading)
                StatusPill(active ? "Active" : "Deactivated")
            }
            if model.secret == nil, let key = model.key {
                MonoText(text: key.masked, size: 12)
                    .padding(.horizontal, 12).padding(.vertical, 10)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(eirs.background, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
            }
        }
    }

    /// New keys can pick any active source system; an existing key stays bound to its own.
    private func systemCard(_ working: ApiKeyDraft, isNew: Bool) -> some View {
        let systems = model.settings?.sourceSystems ?? []
        let options = systems.map { s in
            ChipOption(id: s.id, label: "\(s.label) · \(model.settings?.branch(s.branchId)?.name ?? "—")",
                       selected: working.sourceSystemId == s.id,
                       disabled: isNew ? !s.active : s.id != working.sourceSystemId)
        }
        return SettingsCard(spacing: 8) {
            ChipMultiSelect(options: options, mono: false) { id in model.update { $0.sourceSystemId = id } }
            Text(isNew ? "Inactive source systems can\u{2019}t take new keys." : "A key stays bound to the source system it was created for.")
                .font(EirsFont.sans(12)).foregroundStyle(eirs.muted)
        }
    }

    private func capabilitiesCard(_ working: ApiKeyDraft) -> some View {
        let ceiling = model.ceiling
        let branchName = ceiling?.branchName ?? "—"
        let excise = model.settings?.merchantExcise ?? false
        return SettingsCard(spacing: 18) {
            ChipGroup(title: "Operations",
                      options: ChipOption.from(ApiOperation.allCases, selected: working.operations, ceiling: ceiling?.operations ?? []),
                      caption: "Greyed chips are outside what \(branchName) branch and \(ceiling?.systemLabel ?? "—") allow.") { id in
                if let value = ApiOperation(rawValue: id) { model.update { $0.operations.toggle(value) } }
            }
            ChipGroup(title: "Transaction types",
                      options: ChipOption.from(configurableTransactionTypes, selected: working.transactionTypes,
                                               ceiling: ceiling?.transactionTypes ?? [])) { id in
                if let value = TransactionType(rawValue: id) { model.update { $0.transactionTypes.toggle(value) } }
            }
            ChipGroup(title: "Tax types",
                      options: ChipOption.from(SettingsTaxType.allCases, selected: working.taxTypes, ceiling: ceiling?.taxTypes ?? []),
                      caption: excise ? "" : "Enable merchant excise to grant EXCISE.") { id in
                if let value = SettingsTaxType(rawValue: id) { model.update { $0.taxTypes.toggle(value) } }
            }
        }
    }

    private var usersHeader: some View {
        HStack {
            Eyebrow(text: "USERS", color: eirs.muted)
            Spacer(minLength: 8)
            TextLink(model.assignOpen ? "Done" : "+ Assign", size: 13) { model.assignOpen.toggle() }
        }
        .padding(.horizontal, 4)
        .padding(.top, 8)
    }

    private func usersCard(_ working: ApiKeyDraft) -> some View {
        let available = (model.settings?.staff ?? []).filter { !working.users.contains($0) }
        return VStack(alignment: .leading, spacing: 0) {
            ForEach(working.users, id: \.self) { name in
                HStack(spacing: 12) {
                    Text(name.split(separator: " ").compactMap { $0.first }.map { String($0) }.joined())
                        .font(EirsFont.sans(12, .semibold)).foregroundStyle(eirs.brandDark)
                        .frame(width: 32, height: 32)
                        .background(eirs.tint1, in: Circle())
                    Text(name).font(EirsFont.sans(14, .medium)).frame(maxWidth: .infinity, alignment: .leading)
                    Button { model.update { $0.users.removeAll { $0 == name } } } label: {
                        LineIcon(EirsIcons.close, size: 14, strokeWidth: 2.2, tint: eirs.muted)
                            .frame(width: 32, height: 32)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(PressFillStyle(normal: .clear, pressed: eirs.background, radius: 9))
                }
                .padding(.vertical, 10)
                .overlay(alignment: .bottom) { Hairline() }
            }
            if working.users.isEmpty {
                Text("No users assigned. Only the owner can use this key.")
                    .font(EirsFont.sans(13)).foregroundStyle(eirs.muted)
                    .padding(.vertical, 10)
            }
            if model.assignOpen {
                Wrap {
                    ForEach(available, id: \.self) { name in
                        Button { model.update { $0.users.append(name) } } label: {
                            Text("+ \(name)").font(EirsFont.sans(13)).foregroundStyle(eirs.brandDark)
                                .padding(.horizontal, 12)
                                .frame(height: 34)
                                .background(Color.white, in: Capsule())
                                .overlay(Capsule().strokeBorder(eirs.brand, style: StrokeStyle(lineWidth: 1, dash: [4, 3])))
                                .contentShape(Capsule())
                        }
                        .buttonStyle(PlainTapStyle())
                    }
                }
                .padding(.vertical, 12)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 6)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .solidBorder(eirs.line, radius: 16)
    }
}
