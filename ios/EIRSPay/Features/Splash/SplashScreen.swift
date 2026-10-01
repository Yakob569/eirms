import SwiftUI

private func splashCircle(_ center: CGPoint, _ radius: CGFloat) -> Path {
    Path(ellipseIn: CGRect(x: center.x - radius, y: center.y - radius, width: radius * 2, height: radius * 2))
}

/// Teal brand splash; advances after 2.4s, tap to skip.
@MainActor
struct SplashScreen: View {
    @Environment(\.eirs) private var eirs
    @State private var finished = false
    let onFinished: () -> Void

    init(onFinished: @escaping () -> Void) {
        self.onFinished = onFinished
    }

    var body: some View {
        ZStack {
            eirs.brand.ignoresSafeArea()
            rings.ignoresSafeArea()
            VStack(spacing: 0) {
                center
                footer
            }
        }
        .contentShape(Rectangle())
        .onTapGesture { finish() }
        .task {
            do {
                try await Task.sleep(nanoseconds: 2_400_000_000)
            } catch {
                return
            }
            finish()
        }
    }

    private func finish() {
        if finished { return }
        finished = true
        onFinished()
    }

    /// Decorative ring outlines bleeding off the corners.
    private var rings: some View {
        Canvas { context, size in
            let strong = GraphicsContext.Shading.color(Color.white.opacity(0.18))
            let soft = GraphicsContext.Shading.color(Color.white.opacity(0.12))
            let topRight = CGPoint(x: size.width - 50, y: 90)
            let bottomLeft = CGPoint(x: 60, y: size.height - 20)
            context.stroke(splashCircle(topRight, 210), with: strong, lineWidth: 1)
            context.stroke(splashCircle(topRight, 130), with: strong, lineWidth: 1)
            context.stroke(splashCircle(bottomLeft, 180), with: soft, lineWidth: 1)
        }
    }

    private var center: some View {
        VStack(spacing: 22) {
            ZStack {
                RoundedRectangle(cornerRadius: 22, style: .continuous).fill(Color.white)
                LineIcon(EirsIcons.shield, size: 40, strokeWidth: 1.8, tint: eirs.brand)
            }
            .frame(width: 84, height: 84)
            .popIn()

            VStack(spacing: 8) {
                Text("EIRSPay")
                    .font(EirsFont.serif(52))
                    .foregroundStyle(Color.white)
                tagline
            }
            .riseIn(duration: 0.6, delay: 0.15)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var tagline: some View {
        let lead = Text("Electronic invoicing for ").font(EirsFont.sans(15))
        let accent = Text("Ethiopia").font(EirsFont.serif(18, italic: true))
        return (lead + accent).foregroundStyle(Color.white.opacity(0.85))
    }

    private var footer: some View {
        VStack(spacing: 14) {
            FillBar(duration: 2, delay: 0.2, track: Color.white.opacity(0.25), fill: Color.white)
                .frame(width: 140)
            Eyebrow(text: "SECURED BY EIRS", color: Color.white.opacity(0.75), size: 10, tracking: 0.2)
        }
        .padding(.horizontal, 48)
        .padding(.bottom, 40)
    }
}
