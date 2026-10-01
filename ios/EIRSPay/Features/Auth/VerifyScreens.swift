import SwiftUI
import EIRSDomain

// MARK: - Verify method

@MainActor
struct VerifyMethodScreen: View {
    @State private var model: VerifyMethodModel
    let onBack: () -> Void
    let onCodeSent: (_ channel: String, _ sentTo: String) -> Void

    init(container: AppContainer, onBack: @escaping () -> Void, onCodeSent: @escaping (_ channel: String, _ sentTo: String) -> Void) {
        _model = State(initialValue: VerifyMethodModel(container: container))
        self.onBack = onBack
        self.onCodeSent = onCodeSent
    }

    var body: some View {
        LoadingHost(loading: model.loading) {
            AuthScaffold(onBack: onBack, step: 2, spacing: 22) {
                AuthConfirmBanner(text: "Account created for \(model.accountName)")
                AuthHeading(plain: "Verify your account", lead: "Choose where you want to receive your verification code.")
                VerifyMethodOption(icon: EirsIcons.smartphone, title: "Phone (SMS)", detail: model.maskedPhone, badge: "Faster") {
                    send(.sms)
                }
                VerifyMethodOption(icon: EirsIcons.mail, title: "Email", detail: model.maskedEmail, badge: nil) {
                    send(.email)
                }
                if let message = model.errorMessage { AuthErrorText(text: message) }
            }
        }
        .task { await model.load() }
        .authPushedChrome()
    }

    private func send(_ channel: VerificationChannel) {
        Task {
            if let result = await model.send(channel) { onCodeSent(result.channel.rawValue, result.sentTo) }
        }
    }
}

private struct VerifyMethodButtonStyle: ButtonStyle {
    let brand: Color
    let line: Color
    let pressedFill: Color

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(configuration.isPressed ? pressedFill : Color.white,
                        in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous)
                .strokeBorder(configuration.isPressed ? brand : line, lineWidth: 1))
    }
}

@MainActor
private struct VerifyMethodOption: View {
    @Environment(\.eirs) private var eirs
    let icon: LineIconSpec
    let title: String
    let detail: String
    let badge: String?
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 14) {
                ZStack {
                    RoundedRectangle(cornerRadius: 12, style: .continuous).fill(eirs.tint1)
                    LineIcon(icon, size: 20, tint: eirs.brand)
                }
                .frame(width: 46, height: 46)
                texts
                LineIcon(EirsIcons.arrowRight, size: 18, tint: eirs.muted)
            }
            .padding(16)
            .contentShape(Rectangle())
        }
        .buttonStyle(VerifyMethodButtonStyle(brand: eirs.brand, line: eirs.line, pressedFill: eirs.background))
    }

    private var texts: some View {
        VStack(alignment: .leading, spacing: 3) {
            HStack(spacing: 8) {
                Text(title).font(EirsFont.sans(16, .semibold)).foregroundStyle(eirs.ink)
                if let badge {
                    Badge(text: badge, background: eirs.tint1, content: eirs.brandDark, radius: 999, fontSize: 11,
                          tracking: 0, vertical: 2)
                }
            }
            MonoText(text: detail, size: 13, color: eirs.muted)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

// MARK: - Verify code

@MainActor
struct VerifyCodeScreen: View {
    @Environment(\.eirs) private var eirs
    @State private var model: VerifyCodeModel
    @FocusState private var focused: Bool
    let sentTo: String
    let onBack: () -> Void
    let onUseDifferentMethod: () -> Void
    let onVerified: () -> Void

    init(container: AppContainer, channel: String, sentTo: String, onBack: @escaping () -> Void,
         onUseDifferentMethod: @escaping () -> Void, onVerified: @escaping () -> Void) {
        _model = State(initialValue: VerifyCodeModel(container: container, channel: channel))
        self.sentTo = sentTo
        self.onBack = onBack
        self.onUseDifferentMethod = onUseDifferentMethod
        self.onVerified = onVerified
    }

    var body: some View {
        LoadingHost(loading: model.loading) {
            AuthScaffold(onBack: onBack, step: 2, spacing: 22) {
                AuthHeading(plain: "Enter the ", accent: "6-digit", trailing: " code", leadText: sentToText)
                codeEntry
                timerRow
                if let message = model.errorMessage { AuthErrorText(text: message) }
                PrimaryButton("Verify", enabled: model.code.count == 6, action: verify)
                AuthMutedAction(text: "Use a different method", action: onUseDifferentMethod)
            }
        }
        .task {
            while !Task.isCancelled {
                do {
                    try await Task.sleep(nanoseconds: 1_000_000_000)
                } catch {
                    return
                }
                model.tick()
            }
        }
        .task {
            try? await Task.sleep(nanoseconds: 350_000_000)
            focused = true
        }
        .authPushedChrome()
    }

    private var sentToText: Text {
        Text("Sent to ") + Text(sentTo).font(EirsFont.mono(15)).foregroundColor(eirs.ink)
    }

    private var codeBinding: Binding<String> {
        Binding<String>(get: { model.code }, set: { model.setCode($0) })
    }

    private var codeEntry: some View {
        ZStack {
            HStack(spacing: 8) {
                ForEach(0..<6, id: \.self) { index in
                    cell(index)
                }
            }
            TextField("", text: codeBinding)
                .keyboardType(.numberPad)
                .focused($focused)
                .foregroundStyle(Color.clear)
                .tint(Color.clear)
                .opacity(0.02)
                .frame(height: 58)
        }
        .contentShape(Rectangle())
        .onTapGesture { focused = true }
    }

    private func cell(_ index: Int) -> some View {
        let digits = Array(model.code)
        let value: String = index < digits.count ? String(digits[index]) : ""
        let active = index == min(digits.count, 5) && digits.count < 6
        let border: Color = active ? eirs.brand : (value.isEmpty ? eirs.line : eirs.ink)
        let shape = RoundedRectangle(cornerRadius: 12, style: .continuous)
        return Text(value)
            .font(EirsFont.mono(24, .medium))
            .foregroundStyle(eirs.ink)
            .frame(maxWidth: .infinity)
            .frame(height: 58)
            .background(value.isEmpty ? eirs.background : Color.white, in: shape)
            .overlay(shape.strokeBorder(border, lineWidth: 1.5))
            .background {
                if active {
                    RoundedRectangle(cornerRadius: 16, style: .continuous).fill(eirs.tint1).padding(-4)
                }
            }
    }

    private var timerRow: some View {
        let lead = Text("Expires in ").font(EirsFont.sans(14)).foregroundColor(eirs.muted)
        let time = Text(model.countdown).font(EirsFont.mono(14)).foregroundColor(eirs.ink)
        return HStack {
            (lead + time).lineLimit(1)
            Spacer(minLength: 8)
            TextLink("Resend code", action: { model.resend() })
        }
    }

    private func verify() {
        Task {
            if await model.verify() { onVerified() }
        }
    }
}

// MARK: - Verify success

@MainActor
struct VerifySuccessScreen: View {
    @State private var model: VerifySuccessModel
    let sentTo: String
    let onBack: () -> Void
    let onContinue: () -> Void

    init(container: AppContainer, sentTo: String, onBack: @escaping () -> Void, onContinue: @escaping () -> Void) {
        _model = State(initialValue: VerifySuccessModel(container: container))
        self.sentTo = sentTo
        self.onBack = onBack
        self.onContinue = onContinue
    }

    var body: some View {
        AuthScaffold(onBack: onBack, step: 3, spacing: 0) {
            AuthSuccessLayout(
                icon: EirsIcons.check, plain: "You're ", accent: "verified.", titleSize: 46, iconSize: 34,
                message: {
                    Text("Your merchant account is ready. Add your business details next to start issuing EIRS-registered receipts.")
                },
                card: {
                    AuthInfoRows(rows: [("Account", model.accountName), ("Verified via", sentTo)], monoLast: true)
                },
                actions: {
                    PrimaryButton("Go to dashboard", action: onContinue)
                })
        }
        .task { await model.load() }
        .authPushedChrome()
    }
}
