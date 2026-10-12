import SwiftUI
import EIRSDomain

/// More tab ("More tools."): terminal mode, tools, fiscal readiness and the settings index.
@MainActor
struct MoreTab: View {
    @Environment(\.eirs) private var eirs
    @State private var model: MoreModel
    var onStartTerminal: () -> Void
    var onOpenItems: () -> Void
    var onOpenWithholding: () -> Void
    var onOpenCanceledInvoices: () -> Void
    var onOpenSettings: (SettingsPage) -> Void

    init(container: AppContainer,
         onStartTerminal: @escaping () -> Void,
         onOpenItems: @escaping () -> Void,
         onOpenWithholding: @escaping () -> Void,
         onOpenCanceledInvoices: @escaping () -> Void,
         onOpenSettings: @escaping (SettingsPage) -> Void) {
        _model = State(initialValue: MoreModel(container: container))
        self.onStartTerminal = onStartTerminal
        self.onOpenItems = onOpenItems
        self.onOpenWithholding = onOpenWithholding
        self.onOpenCanceledInvoices = onOpenCanceledInvoices
        self.onOpenSettings = onOpenSettings
    }

    var body: some View {
        VStack(spacing: 12) {
            MoreTerminalCard(onStart: onStartTerminal)
            MoreToolsCard(activeItems: model.activeItems, awaiting: model.awaitingCertificates,
                          onItems: onOpenItems, onWithholding: onOpenWithholding, onCanceled: onOpenCanceledInvoices)
            sectionLabel("FISCAL READINESS")
            MoreReadinessCard(done: model.setupDone, session: model.session)
            MoreSetupStepper(done: model.setupDone, onOpenStep: onOpenSettings)
            sectionLabel("ALL SETTINGS")
            MoreSettingsCard(session: model.session, settings: model.settings, verified: model.verified,
                             onOpen: onOpenSettings)
        }
        .frame(maxWidth: .infinity)
        .task { await model.observeItems() }
        .task { await model.observeWithholding() }
        .task { await model.observeSession() }
        .task { await model.loadSetup() }
        .task { await model.observeSettings() }
    }

    private func sectionLabel(_ text: String) -> some View {
        Eyebrow(text: text, color: eirs.muted)
            .padding(.horizontal, 4)
            .padding(.top, 8)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}
