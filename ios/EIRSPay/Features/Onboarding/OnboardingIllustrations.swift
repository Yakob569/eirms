import SwiftUI

private let onboardingShadow = Color(hex: 0x0B2A33, opacity: 0.4)

private struct OnboardingDashedTop: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: 0, y: 0.5))
        path.addLine(to: CGPoint(x: rect.width, y: 0.5))
        return path
    }
}

@MainActor
struct OnboardingReceiptIllustration: View {
    @Environment(\.eirs) private var eirs

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: 16, style: .continuous)
        VStack(alignment: .leading, spacing: 12) {
            header
            Text("Abebe Coffee House").font(EirsFont.sans(14, .semibold))
            lines
            totalRow
            MonoText(text: "IRN ETH-2026-0F4A··9C21", size: 11)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 10)
                .padding(.vertical, 8)
                .background(eirs.background, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
        }
        .padding(18)
        .frame(width: 250)
        .background(Color.white, in: shape)
        .shadow(color: onboardingShadow, radius: 15, x: 0, y: 12)
    }

    private var header: some View {
        HStack {
            Eyebrow(text: "RECEIPT #004218", color: eirs.muted, size: 10, tracking: 0.1)
            Spacer(minLength: 6)
            Badge(text: "Registered", background: eirs.tint1, content: eirs.brandDark, radius: 999, fontSize: 11, tracking: 0)
        }
    }

    private var lines: some View {
        VStack(spacing: 7) {
            line("Macchiato × 2", "180.00", eirs.ink)
            line("Injera combo × 1", "420.00", eirs.ink)
            line("VAT 15%", "90.00", eirs.muted)
        }
        .padding(.top, 10)
        .overlay(alignment: .top) {
            OnboardingDashedTop()
                .stroke(eirs.line, style: StrokeStyle(lineWidth: 1, dash: [4, 3]))
                .frame(height: 1)
        }
    }

    private func line(_ label: String, _ amount: String, _ color: Color) -> some View {
        HStack {
            Text(label).font(EirsFont.sans(12)).foregroundStyle(color)
            Spacer(minLength: 6)
            MonoText(text: amount, size: 12, color: color)
        }
    }

    private var totalRow: some View {
        HStack {
            Text("Total").font(EirsFont.sans(13, .semibold))
            Spacer(minLength: 6)
            MonoText(text: "ETB 690.00", size: 13, weight: .medium)
        }
        .padding(.top, 10)
        .overlay(alignment: .top) { Hairline() }
    }
}

@MainActor
struct OnboardingOtpIllustration: View {
    @Environment(\.eirs) private var eirs
    private let digits: [String] = ["4", "8", "2", "9", "1", "3"]

    var body: some View {
        VStack(spacing: 14) {
            HStack(spacing: 0) {
                bubble
                Spacer(minLength: 0)
            }
            cells
            HStack(spacing: 0) {
                Spacer(minLength: 0)
                OnboardingPill(text: "+251 verified", background: eirs.brand, content: Color.white)
            }
        }
        .frame(width: 270)
    }

    private var bubble: some View {
        let shape = UnevenRoundedRectangle(topLeadingRadius: 16, bottomLeadingRadius: 4, bottomTrailingRadius: 16,
                                           topTrailingRadius: 16, style: .continuous)
        let lead = Text("Your verification code is ").font(EirsFont.sans(13))
        let code = Text("482 913").font(EirsFont.mono(13, .medium))
        let tail = Text(". Expires in 15 min.").font(EirsFont.sans(13))
        return VStack(alignment: .leading, spacing: 4) {
            MonoText(text: "SMS · EIRSPay", size: 10, color: eirs.muted)
            (lead + code + tail)
                .foregroundStyle(eirs.ink)
                .lineSpacing(3)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .frame(maxWidth: 230, alignment: .leading)
        .background(Color.white, in: shape)
        .shadow(color: onboardingShadow, radius: 10, x: 0, y: 8)
    }

    private var cells: some View {
        let cell = RoundedRectangle(cornerRadius: 8, style: .continuous)
        return HStack(spacing: 6) {
            ForEach(digits.indices, id: \.self) { index in
                Text(digits[index])
                    .font(EirsFont.mono(18))
                    .foregroundStyle(eirs.ink)
                    .frame(maxWidth: .infinity)
                    .frame(height: 44)
                    .background(eirs.background, in: cell)
                    .overlay(cell.strokeBorder(eirs.brand, lineWidth: 1.5))
            }
        }
        .padding(12)
        .background(Color.white, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
        .shadow(color: onboardingShadow, radius: 10, x: 0, y: 8)
    }
}

@MainActor
struct OnboardingQrIllustration: View {
    @Environment(\.eirs) private var eirs
    @State private var scanning = false

    var body: some View {
        VStack(spacing: 16) {
            qrBox
            OnboardingPill(text: "Authentic · portal.mor.gov.et", background: Color.white, content: eirs.brandDark)
        }
    }

    private var scanY: CGFloat { scanning ? 118 : -4 }

    private var qrBox: some View {
        let shape = RoundedRectangle(cornerRadius: 16, style: .continuous)
        let glow = LinearGradient(colors: [Color.clear, eirs.brand.opacity(0.35), Color.clear], startPoint: .top, endPoint: .bottom)
        return ZStack(alignment: .topLeading) {
            DecorativeQr().padding(12)
            Rectangle().fill(glow).frame(height: 14).offset(y: scanY)
            Rectangle().fill(eirs.brand).frame(height: 2).offset(y: scanY + 6)
        }
        .frame(width: 150, height: 150)
        .background(Color.white, in: shape)
        .clipShape(shape)
        .shadow(color: onboardingShadow, radius: 15, x: 0, y: 12)
        .animation(Animation.easeInOut(duration: 1.4).repeatForever(autoreverses: true), value: scanning)
        .onAppear { scanning = true }
    }
}

@MainActor
struct OnboardingPill: View {
    let text: String
    let background: Color
    let content: Color

    var body: some View {
        HStack(spacing: 6) {
            LineIcon(EirsIcons.check, size: 14, strokeWidth: 2.6, tint: content)
            Text(text).font(EirsFont.sans(13, .semibold)).foregroundStyle(content).lineLimit(1)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(background, in: Capsule())
    }
}
