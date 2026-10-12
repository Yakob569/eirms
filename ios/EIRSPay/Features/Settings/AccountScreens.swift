import SwiftUI
import EIRSDomain

/// Shared chrome for settings pages: header (eyebrow, "Plain *accent*" title, trailing), scrolling body,
/// optional sticky footer, loader.
struct SettingsPageFrame<Trailing: View, Footer: View, Content: View>: View {
    var eyebrow: String
    var plain: String
    var accent: String
    var loading: LoaderMessage?
    var onBack: () -> Void
    @ViewBuilder var trailing: () -> Trailing
    @ViewBuilder var footer: () -> Footer
    @ViewBuilder var content: () -> Content

    var body: some View {
        LoadingHost(loading: loading) {
            ViewScaffold(
                header: {
                    ViewHeader(eyebrow: eyebrow, onBack: onBack, title: { ViewTitle(plain: plain, accent: accent) },
                               trailing: trailing)
                },
                bottomBar: footer,
                content: content)
        }
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
    }
}


// MARK: - Account

@MainActor
struct AccountScreen: View {
    @Environment(\.eirs) private var eirs
    @State private var model: SettingsModel
    var onBack: () -> Void

    init(container: AppContainer, onBack: @escaping () -> Void) {
        _model = State(initialValue: SettingsModel(container: container))
        self.onBack = onBack
    }

    var body: some View {
        SettingsPageFrame(eyebrow: "SETTINGS · ACCOUNT", plain: "Your ", accent: "account", loading: nil, onBack: onBack,
                         trailing: { EmptyView() }, footer: { EmptyView() }) {
            identityCard
            userIdRow
            SettingsSectionLabel("SECURITY")
            passwordCard
            SettingsSectionLabel("SIGNED-IN DEVICES")
            sessionsCard
            if (model.settings?.sessions.count ?? 0) > 1 {
                Button { model.signOutOthers() } label: {
                    Text("Sign out all other devices").font(EirsFont.sans(14, .semibold)).foregroundStyle(eirs.red)
                        .padding(.horizontal, 16)
                        .frame(maxWidth: .infinity, minHeight: 48, maxHeight: 48, alignment: .leading)
                        .contentShape(Rectangle())
                }
                .buttonStyle(PressFillStyle(normal: .white, pressed: eirs.background, radius: 12))
                .solidBorder(eirs.line, radius: 12)
            }
            SettingsSectionLabel("RECENT ACTIVITY")
            activityCard
        }
        .task { await model.observe() }
        .task { await model.observeSession() }
    }

    private var identityCard: some View {
        let user = model.session?.user
        return EirsCard {
            HStack(spacing: 14) {
                Text(user?.initials ?? "").font(EirsFont.sans(18, .semibold)).foregroundStyle(Color.white)
                    .frame(width: 56, height: 56)
                    .background(eirs.ink, in: Circle())
                VStack(alignment: .leading, spacing: 4) {
                    Text(user?.fullName ?? "").font(EirsFont.sans(17, .semibold)).lineLimit(1)
                    Text(user?.email ?? "").font(EirsFont.sans(13)).foregroundStyle(eirs.muted).lineLimit(1)
                    HStack(spacing: 6) {
                        StatusPill("Email verified")
                        StatusPill(user?.role.rawValue ?? "OWNER")
                    }
                    .padding(.top, 2)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }

    private var userIdRow: some View {
        HStack {
            Text("User ID").font(EirsFont.sans(13)).foregroundStyle(eirs.muted)
            Spacer(minLength: 8)
            MonoText(text: model.session?.user.id ?? "—", size: 13, weight: .medium)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(Color.white, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
        .solidBorder(eirs.line, radius: 12)
    }

    private var passwordCard: some View {
        SettingsCard {
            HStack(spacing: 10) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Password").font(EirsFont.sans(15, .semibold))
                    Text(model.settings?.passwordChangedNote ?? "").font(EirsFont.sans(12)).foregroundStyle(eirs.muted)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                TextLink(model.passwordOpen ? "Cancel" : "Change") { model.togglePassword() }
                    .padding(.vertical, 8)
            }
            if model.passwordOpen {
                SettingsField(label: "Current password", text: $model.currentPassword, secure: true, placeholder: "••••••••", fontSize: 16)
                SettingsField(label: "New password", text: $model.newPassword, secure: true, placeholder: "At least 8 characters", fontSize: 16)
                SettingsField(label: "Confirm new password", text: $model.confirmPassword, secure: true, placeholder: "Repeat it", fontSize: 16)
                PrimaryButton("Update password", enabled: model.passwordReady, height: 48, radius: 12, fontSize: 15) {
                    Task { await model.updatePassword() }
                }
            }
        }
    }

    private var sessionsCard: some View {
        let sessions = model.settings?.sessions ?? []
        return VStack(spacing: 0) {
            ForEach(sessions) { s in
                HStack(spacing: 12) {
                    VStack(alignment: .leading, spacing: 3) {
                        Text(s.device).font(EirsFont.sans(14, .medium))
                        MonoText(text: "\(s.fingerprint) · \(s.location) · \(s.lastActive)", size: 10, color: eirs.muted, lineLimit: 1)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    if s.isCurrent {
                        StatusPill("This device")
                    } else {
                        TextLink("Sign out", size: 13, color: eirs.red) { model.signOut(sessionId: s.id) }
                            .padding(.vertical, 8)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 14)
                .overlay(alignment: .bottom) { if s.id != sessions.last?.id { Hairline() } }
            }
        }
        .background(Color.white, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .solidBorder(eirs.line, radius: 16)
    }

    private func activityIcon(_ kind: SecurityEventKind) -> LineIconSpec {
        switch kind {
        case .signIn: return EirsIcons.signIn
        case .failedAttempt: return EirsIcons.alert
        case .signOut: return EirsIcons.logOut
        case .passwordReset: return EirsIcons.rotateCcw
        }
    }

    private var activityCard: some View {
        let events = model.settings?.activity ?? []
        return VStack(spacing: 0) {
            ForEach(events) { e in
                let bad = e.kind == .failedAttempt
                HStack(spacing: 12) {
                    LineIcon(activityIcon(e.kind), size: 15, tint: bad ? eirs.red : eirs.brand)
                        .frame(width: 32, height: 32)
                        .background(bad ? eirs.redTint : eirs.tint1, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                    VStack(alignment: .leading, spacing: 2) {
                        Text(e.title).font(EirsFont.sans(14, .medium))
                        Text(e.detail).font(EirsFont.sans(12)).foregroundStyle(eirs.muted).lineLimit(1)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    Text(e.when).font(EirsFont.sans(11)).foregroundStyle(eirs.muted).lineLimit(1)
                }
                .padding(.vertical, 12)
                .overlay(alignment: .bottom) { if e.id != events.last?.id { Hairline() } }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 4)
        .background(Color.white, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .solidBorder(eirs.line, radius: 16)
    }
}

// MARK: - MoR credentials

@MainActor
struct MorCredentialsScreen: View {
    @Environment(\.eirs) private var eirs
    @State private var model: SettingsModel
    var onBack: () -> Void
    var onVerify: () -> Void

    init(container: AppContainer, onBack: @escaping () -> Void, onVerify: @escaping () -> Void) {
        _model = State(initialValue: SettingsModel(container: container))
        self.onBack = onBack
        self.onVerify = onVerify
    }

    /// Draft while editing, stored value otherwise.
    private var shown: MorCredentials? {
        model.editingCredentials ? model.credentialsDraft : model.settings?.credentials
    }

    private func field(_ keyPath: WritableKeyPath<MorCredentials, String>) -> Binding<String> {
        Binding(get: { shown?[keyPath: keyPath] ?? "" },
                set: { value in model.credentialsDraft?[keyPath: keyPath] = value })
    }

    var body: some View {
        let env = shown?.environment ?? .dev
        SettingsPageFrame(eyebrow: "SETTINGS · EIRMS " + env.rawValue, plain: "MoR ", accent: "credentials", loading: nil,
                         onBack: onBack,
                         trailing: { SettingsEditButton(editing: model.editingCredentials) { model.toggleCredentialsEdit() } },
                         footer: { EmptyView() }) {
            verifiedRow
            SettingsCard(spacing: 10) {
                Text("Environment").font(EirsFont.sans(13, .medium))
                environmentSwitch(env)
                Text(env.note).font(EirsFont.sans(12)).foregroundStyle(eirs.muted)
            }
            SettingsCard {
                SettingsField(label: "Client ID", text: field(\.clientId), editable: model.editingCredentials, mono: true, fontSize: 14)
                SettingsField(label: "TIN", text: field(\.tin), editable: model.editingCredentials, mono: true,
                              keyboard: .numberPad, fontSize: 14)
            }
            SettingsCard {
                HStack {
                    Text("Secrets").font(EirsFont.sans(15, .semibold))
                    Spacer(minLength: 8)
                    TextLink(model.showSecrets ? "Hide secrets" : "Show secrets", size: 13) { model.showSecrets.toggle() }
                }
                SettingsField(label: "Client secret", text: field(\.clientSecret), editable: model.editingCredentials, mono: true,
                              secure: true, revealed: model.showSecrets, fontSize: 14)
                SettingsField(label: "API key", text: field(\.apiKey), editable: model.editingCredentials, mono: true,
                              secure: true, revealed: model.showSecrets, fontSize: 14)
            }
            SettingsNote(model.editingCredentials ? "Editing. Tap Save when you\u{2019}re done." : "Locked. Tap Edit to change credentials.")
        }
        .task { await model.observe() }
        .task { await model.loadSteps() }
    }

    private var verifiedRow: some View {
        HStack(spacing: 10) {
            HStack(spacing: 8) {
                StatusPill(model.verified ? "Verified" : "Not verified")
                Text("with MoR").font(EirsFont.sans(13))
            }
            Spacer(minLength: 8)
            TextLink("Verify setup →", size: 13, action: onVerify)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(Color.white, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
        .solidBorder(eirs.line, radius: 12)
    }

    /// DEV / PROD: brand-filled selection; only switchable while editing.
    private func environmentSwitch(_ env: MorEnvironment) -> some View {
        HStack(spacing: 0) {
            ForEach(MorEnvironment.allCases) { option in
                let on = option == env
                Button { model.credentialsDraft?.environment = option } label: {
                    Text(option.rawValue)
                        .font(EirsFont.mono(13, .medium))
                        .foregroundStyle(on ? Color.white : eirs.muted)
                        .frame(maxWidth: .infinity, minHeight: 40, maxHeight: 40)
                        .background(on ? eirs.brand : Color.clear, in: RoundedRectangle(cornerRadius: 9, style: .continuous))
                        .contentShape(Rectangle())
                }
                .buttonStyle(PlainTapStyle())
                .disabled(!model.editingCredentials)
            }
        }
        .padding(3)
        .background(eirs.background, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
        .solidBorder(eirs.line, radius: 12)
    }
}

// MARK: - Seller profile

@MainActor
struct SellerProfileScreen: View {
    @State private var model: SettingsModel
    var onBack: () -> Void

    init(container: AppContainer, onBack: @escaping () -> Void) {
        _model = State(initialValue: SettingsModel(container: container))
        self.onBack = onBack
    }

    private var shown: SellerProfile? { model.editingSeller ? model.sellerDraft : model.settings?.seller }

    private func field(_ label: String, _ keyPath: WritableKeyPath<SellerProfile, String>, required: Bool = false,
                       mono: Bool = false, placeholder: String = "", keyboard: UIKeyboardType = .default) -> some View {
        SettingsField(label: label + (required ? " *" : ""),
                      text: Binding(get: { shown?[keyPath: keyPath] ?? "" },
                                    set: { value in model.sellerDraft?[keyPath: keyPath] = value }),
                      editable: model.editingSeller, mono: mono, placeholder: placeholder, keyboard: keyboard)
    }

    var body: some View {
        SettingsPageFrame(eyebrow: "SETTINGS · LEGAL ENTITY", plain: "Seller ", accent: "profile", loading: nil, onBack: onBack,
                         trailing: { SettingsEditButton(editing: model.editingSeller) { model.toggleSellerEdit() } },
                         footer: { EmptyView() }) {
            SettingsSectionLabel("LEGAL", top: 0)
            SettingsCard {
                field("TIN", \.tin, required: true, mono: true, keyboard: .numberPad)
                field("Legal name", \.legalName, required: true)
                field("VAT number", \.vatNumber, required: true, mono: true)
            }
            SettingsSectionLabel("CONTACT")
            SettingsCard {
                field("Email", \.email, required: true, placeholder: "finance@company.et", keyboard: .emailAddress)
                field("Phone", \.phone, required: true, mono: true, placeholder: "+251 9…", keyboard: .phonePad)
            }
            SettingsSectionLabel("ADDRESS")
            SettingsCard {
                SettingsFieldGrid {
                    field("Region", \.region, required: true)
                    field("City", \.city)
                    field("Wereda", \.wereda, required: true)
                    field("Sub city", \.subCity)
                    field("Zone", \.zone)
                    field("Kebele", \.kebele)
                    field("Locality", \.locality)
                    field("House no.", \.houseNumber)
                    field("Country", \.country)
                }
            }
            SettingsNote("Printed on every invoice and receipt. Fields marked * are required by MoR.")
        }
        .task { await model.observe() }
    }
}

// MARK: - Verify setup

@MainActor
struct VerifySetupScreen: View {
    @Environment(\.eirs) private var eirs
    @State private var model: SettingsModel
    var onBack: () -> Void

    init(container: AppContainer, onBack: @escaping () -> Void) {
        _model = State(initialValue: SettingsModel(container: container))
        self.onBack = onBack
    }

    var body: some View {
        let seller = model.settings?.seller
        let system = model.settings?.sourceSystems.first
        SettingsPageFrame(eyebrow: "SETTINGS · MOR PROBE", plain: "Verify ", accent: "setup", loading: model.loading, onBack: onBack,
                         trailing: { EmptyView() }, footer: { EmptyView() }) {
            heroCard
            SettingsSectionLabel("PROBE INVOICE · SELLER")
            SettingsRowsCard(rows: [
                ("Legal name", seller?.legalName ?? ""), ("TIN", seller?.tin ?? ""), ("VAT number", seller?.vatNumber ?? ""),
                ("Region", seller?.region ?? ""), ("Wereda", seller?.wereda ?? "")
            ])
            SettingsSectionLabel("SOURCE SYSTEM")
            SettingsRowsCard(rows: [
                ("System type", system?.type.rawValue ?? ""), ("System number", system?.systemId ?? ""),
                ("Cashier", system?.cashier ?? ""), ("Sales person", system?.salesPerson ?? "")
            ])
            SettingsSectionLabel("BUYER · VALUE")
            SettingsRowsCard(rows: [
                ("Name", "Walk-in Customer"), ("ID type", "KID"),
                ("Tax", "150.00 ETB"), ("Total", "1,650.00 ETB"), ("Currency", "ETB")
            ])
            SettingsSectionLabel("ITEMS")
            itemsCard
        }
        .task { await model.observe() }
        .task { await model.loadSteps() }
    }

    private var heroCard: some View {
        let ok = model.verified
        let desc: String = ok
            ? "Your merchant is cleared for live invoicing. API keys are unlocked."
            : model.canVerify ? "Sends a signed probe invoice built from your credentials, seller profile and source system."
            : "Finish MoR credentials, seller profile and a source system first."
        return VStack(alignment: .leading, spacing: 14) {
            LineIcon(EirsIcons.shield, size: 22, tint: ok ? .white : eirs.brand)
                .frame(width: 48, height: 48)
                .background(ok ? eirs.brand : eirs.tint1, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            SerifTitle(plain: ok ? "Verified with " : "Ready to verify with ", accent: "MoR.", size: 32)
            Text(desc).font(EirsFont.sans(14)).foregroundStyle(eirs.muted).lineSpacing(5)
                .fixedSize(horizontal: false, vertical: true)
            PrimaryButton(ok ? "Re-run verification" : "Run verification", enabled: model.canVerify, height: 52, fontSize: 15) {
                Task { await model.runVerification() }
            }
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
        .solidBorder(eirs.line, radius: 20)
    }

    private var itemsCard: some View {
        let items: [(String, String, String, String, String)] = [
            ("1", "Item VAT15", "1 PCS", "VAT15", "1,150.00"), ("2", "Item VAT0", "1 PCS", "VAT0", "500.00")
        ]
        return VStack(spacing: 0) {
            ForEach(items.indices, id: \.self) { index in
                let it = items[index]
                HStack(spacing: 10) {
                    MonoText(text: it.0, size: 13, color: eirs.muted)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(it.1).font(EirsFont.sans(13, .medium))
                        Text("\(it.2) · \(it.3)").font(EirsFont.sans(11)).foregroundStyle(eirs.muted)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    MonoText(text: it.4, size: 13)
                }
                .padding(.vertical, 12)
                .overlay(alignment: .bottom) { if index < items.count - 1 { Hairline() } }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 4)
        .background(Color.white, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .solidBorder(eirs.line, radius: 16)
    }
}
