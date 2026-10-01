import SwiftUI
import Observation
import EIRSDomain

@MainActor @Observable
final class OnboardingModel {
    var slide = 0
    private let preferences: PreferencesRepository

    init(container: AppContainer) {
        preferences = container.preferences
    }

    func complete() { preferences.completeOnboarding() }
}

struct OnboardingCopy {
    let eyebrow: String
    let plain: String
    let accent: String
    let body: String
}

let onboardingPanels: [OnboardingCopy] = [
    OnboardingCopy(eyebrow: "01 · EIRS-REGISTERED", plain: "Every receipt, ", accent: "compliant.",
                   body: "Every invoice is submitted to the Ministry of Revenues in real time and returns with a signed IRN."),
    OnboardingCopy(eyebrow: "02 · ETHIOPIAN PHONE FLOWS", plain: "Verify with ", accent: "one code.",
                   body: "OTP over SMS to +251 numbers, out of the box. Email works too."),
    OnboardingCopy(eyebrow: "03 · QR ON EVERY RECEIPT", plain: "Customers check it ", accent: "in one scan.",
                   body: "Receipts in Amharic or English, verifiable by anyone with a phone camera."),
]

@MainActor
struct OnboardingScreen: View {
    @Environment(\.eirs) private var eirs
    @State private var model: OnboardingModel
    let onCreateAccount: () -> Void
    let onSignIn: () -> Void

    init(container: AppContainer, onCreateAccount: @escaping () -> Void, onSignIn: @escaping () -> Void) {
        _model = State(initialValue: OnboardingModel(container: container))
        self.onCreateAccount = onCreateAccount
        self.onSignIn = onSignIn
    }

    var body: some View {
        ZStack(alignment: .top) {
            VStack(spacing: 0) {
                pager
                controls
            }
            topBar
        }
        .background(Color.white.ignoresSafeArea())
    }

    private var pager: some View {
        TabView(selection: $model.slide) {
            ForEach(0..<3, id: \.self) { page in
                OnboardingPanel(page: page).tag(page)
            }
        }
        .tabViewStyle(.page(indexDisplayMode: .never))
        .ignoresSafeArea(edges: .top)
    }

    private var controls: some View {
        VStack(alignment: .leading, spacing: 18) {
            dots
            PrimaryButton(model.slide < 2 ? "Next" : "Create account") {
                if model.slide < 2 {
                    withAnimation { model.slide += 1 }
                } else {
                    toSignUp()
                }
            }
            PromptLink(prompt: "Already have an account?", action: "Sign in", onTap: toSignIn)
        }
        .padding(.horizontal, 28)
        .padding(.top, 16)
        .padding(.bottom, 16)
    }

    private var dots: some View {
        HStack(spacing: 6) {
            ForEach(0..<3, id: \.self) { index in
                let active = index == model.slide
                Capsule()
                    .fill(active ? eirs.brand : eirs.tint2)
                    .frame(width: active ? 28 : 6, height: 6)
                    .padding(.vertical, 8)
                    .onPlainTap { withAnimation { model.slide = index } }
            }
        }
        .animation(.easeInOut(duration: 0.3), value: model.slide)
    }

    /// Brand mark + Skip, floating over the illustration.
    private var topBar: some View {
        HStack {
            HStack(spacing: 8) {
                ZStack {
                    RoundedRectangle(cornerRadius: 7, style: .continuous).fill(eirs.brand)
                    LineIcon(EirsIcons.shield, size: 14, strokeWidth: 2.2, tint: Color.white)
                }
                .frame(width: 26, height: 26)
                Text("EIRSPay").font(EirsFont.sans(15, .semibold))
            }
            Spacer(minLength: 8)
            Text("Skip")
                .font(EirsFont.sans(14, .medium))
                .foregroundStyle(eirs.muted)
                .padding(.leading, 16)
                .padding(.vertical, 10)
                .onPlainTap { toSignUp() }
        }
        .padding(.horizontal, 24)
        .padding(.top, 12)
    }

    private func toSignUp() {
        model.complete()
        onCreateAccount()
    }

    private func toSignIn() {
        model.complete()
        onSignIn()
    }
}

@MainActor
private struct OnboardingPanel: View {
    @Environment(\.eirs) private var eirs
    let page: Int

    var body: some View {
        GeometryReader { proxy in
            // 470pt in the design; shrink on short screens so the copy stays visible.
            let blockHeight = min(max(proxy.size.height - 230, 360), 470)
            VStack(alignment: .leading, spacing: 0) {
                illustrationBlock(height: blockHeight)
                copyBlock
                Spacer(minLength: 0)
            }
        }
    }

    private func illustrationBlock(height: CGFloat) -> some View {
        let shape = UnevenRoundedRectangle(bottomLeadingRadius: 32, bottomTrailingRadius: 32, style: .continuous)
        return ZStack {
            Circle().fill(eirs.tint2).opacity(0.6).frame(width: 300, height: 300)
            illustration.padding(.top, 40)
        }
        .frame(maxWidth: .infinity)
        .frame(height: height)
        .background(eirs.tint1, in: shape)
        .clipShape(shape)
    }

    @ViewBuilder private var illustration: some View {
        switch page {
        case 0: OnboardingReceiptIllustration()
        case 1: OnboardingOtpIllustration()
        default: OnboardingQrIllustration()
        }
    }

    private var copyBlock: some View {
        let copy = onboardingPanels[page]
        return VStack(alignment: .leading, spacing: 12) {
            Eyebrow(text: copy.eyebrow, color: eirs.brandDark)
            SerifTitle(plain: copy.plain, accent: copy.accent, size: 38)
            Text(copy.body)
                .font(EirsFont.sans(15))
                .foregroundStyle(eirs.muted)
                .lineSpacing(4)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.horizontal, 28)
        .padding(.top, 32)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
