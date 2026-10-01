import SwiftUI

// MARK: - Entrance animations

/// CSS `m-up`: fade in while rising 12pt. Runs once when the view appears.
private struct RiseIn: ViewModifier {
    var duration: Double
    var delay: Double
    var distance: CGFloat
    @State private var shown = false

    func body(content: Content) -> some View {
        content
            .opacity(shown ? 1 : 0)
            .offset(y: shown ? 0 : distance)
            .onAppear { withAnimation(.easeOut(duration: duration).delay(delay)) { shown = true } }
    }
}

/// CSS `m-pop`: scale from 0.6 with a slight overshoot.
private struct PopIn: ViewModifier {
    var duration: Double
    @State private var shown = false

    func body(content: Content) -> some View {
        content
            .scaleEffect(shown ? 1 : 0.6)
            .opacity(shown ? 1 : 0)
            .onAppear { withAnimation(.spring(response: duration, dampingFraction: 0.62)) { shown = true } }
    }
}

extension View {
    func riseIn(duration: Double = 0.4, delay: Double = 0, distance: CGFloat = 12) -> some View {
        modifier(RiseIn(duration: duration, delay: delay, distance: distance))
    }

    func popIn(duration: Double = 0.6) -> some View { modifier(PopIn(duration: duration)) }
}

/// Progress bar that fills left→right once over `duration` (splash + loader).
struct FillBar: View {
    var duration: Double = 2
    var delay: Double = 0
    var track: Color
    var fill: Color
    var height: CGFloat = 3
    @State private var progress: CGFloat = 0

    var body: some View {
        GeometryReader { proxy in
            ZStack(alignment: .leading) {
                Capsule().fill(track)
                Capsule().fill(fill).frame(width: proxy.size.width * progress)
            }
        }
        .frame(height: height)
        .onAppear { withAnimation(.timingCurve(0.4, 0, 0.2, 1, duration: duration).delay(delay)) { progress = 1 } }
    }
}

/// Expanding, fading ring behind success marks and the loader (CSS `m-ring`).
struct PulseRing: View {
    var color: Color
    var cornerRadius: CGFloat?       // nil = circle
    var filled = true
    var repeatForever = false
    var count = 2
    var delay: Double = 0
    var duration: Double = 1.6
    @State private var phase: CGFloat = 0

    var body: some View {
        ring
            .scaleEffect(0.8 + 1.3 * phase)
            .opacity(0.7 * Double(1 - phase))
            .onAppear {
                let base = Animation.easeOut(duration: duration).delay(delay)
                withAnimation(repeatForever ? base.repeatForever(autoreverses: false) : base.repeatCount(count, autoreverses: false)) { phase = 1 }
            }
    }

    @ViewBuilder private var ring: some View {
        if let cornerRadius {
            let shape = RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
            if filled { shape.fill(color) } else { shape.strokeBorder(color, lineWidth: 1.5) }
        } else {
            if filled { Circle().fill(color) } else { Circle().strokeBorder(color, lineWidth: 1.5) }
        }
    }
}

/// Brand success mark: filled circle, white icon, two pulses.
struct SuccessMark: View {
    @Environment(\.eirs) private var eirs
    var icon: LineIconSpec = EirsIcons.check
    var size: CGFloat = 76
    var iconSize: CGFloat = 34

    var body: some View {
        ZStack {
            PulseRing(color: eirs.brand).frame(width: size, height: size)
            Circle().fill(eirs.brand).frame(width: size, height: size)
            LineIcon(icon, size: iconSize, strokeWidth: 2.4, tint: .white)
        }
        .frame(width: size, height: size)
    }
}

// MARK: - Loader

struct LoaderMessage: Equatable {
    var title: String
    var subtitle: String
}

/// Wraps a screen and, while `loading` is set, shows the translucent brand loader over it:
/// the page stays visible behind a 55% white, blurred layer.
struct LoadingHost<Content: View>: View {
    var loading: LoaderMessage?
    var duration: Double = 1.8
    @ViewBuilder var content: () -> Content

    var body: some View {
        ZStack {
            content().blur(radius: loading == nil ? 0 : 6).allowsHitTesting(loading == nil)
            if let loading {
                BrandLoader(message: loading, duration: duration)
                    .transition(.opacity)
                    .zIndex(1)
            }
        }
        .animation(.easeOut(duration: 0.25), value: loading)
    }
}

private struct BrandLoader: View {
    @Environment(\.eirs) private var eirs
    var message: LoaderMessage
    var duration: Double

    var body: some View {
        VStack(spacing: 30) {
            ZStack {
                PulseRing(color: eirs.brand, cornerRadius: 20, filled: false, repeatForever: true, duration: 1.4)
                PulseRing(color: eirs.brand, cornerRadius: 20, filled: false, repeatForever: true, delay: 0.7, duration: 1.4)
                RoundedRectangle(cornerRadius: 20, style: .continuous).fill(eirs.brand)
                LineIcon(EirsIcons.shield, size: 34, strokeWidth: 1.8, tint: .white)
            }
            .frame(width: 76, height: 76)

            VStack(spacing: 8) {
                Text(message.title).font(EirsFont.serif(34, italic: true)).multilineTextAlignment(.center)
                Text(message.subtitle).font(EirsFont.sans(14)).foregroundStyle(eirs.muted).multilineTextAlignment(.center)
            }
            .padding(.horizontal, 32)

            FillBar(duration: duration, track: eirs.tint2, fill: eirs.brand)
                .frame(width: 200)
                .id(message.title + message.subtitle)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.white.opacity(0.55).ignoresSafeArea())
        .contentShape(Rectangle())
        .onTapGesture { }
    }
}

// MARK: - Bottom sheet

/// Bottom sheet matching the design: ink scrim, white sheet with 26pt top corners and a grab handle,
/// slides up (CSS `m-sheet`). Scrim tap and a downward drag dismiss it.
private struct EirsSheetModifier<SheetContent: View>: ViewModifier {
    @Environment(\.eirs) private var eirs
    @Binding var isPresented: Bool
    @ViewBuilder var sheet: () -> SheetContent
    @State private var drag: CGFloat = 0

    func body(content: Content) -> some View {
        ZStack(alignment: .bottom) {
            content
            if isPresented {
                eirs.scrim.ignoresSafeArea()
                    .onTapGesture { isPresented = false }
                    .transition(.opacity)
                    .zIndex(1)
                VStack(spacing: 6) {
                    Capsule().fill(eirs.line).frame(width: 40, height: 5).padding(.bottom, 10)
                    sheet()
                }
                .padding(.horizontal, 16)
                .padding(.top, 10)
                .padding(.bottom, 28)
                .frame(maxWidth: .infinity)
                .background(
                    UnevenRoundedRectangle(topLeadingRadius: 26, topTrailingRadius: 26, style: .continuous)
                        .fill(Color.white)
                        .ignoresSafeArea(edges: .bottom)
                )
                .offset(y: max(0, drag))
                .gesture(
                    DragGesture()
                        .onChanged { drag = $0.translation.height }
                        .onEnded { value in
                            if value.translation.height > 100 { isPresented = false }
                            drag = 0
                        }
                )
                .transition(.move(edge: .bottom))
                .zIndex(2)
            }
        }
        .animation(.timingCurve(0.2, 0.8, 0.2, 1, duration: 0.28), value: isPresented)
    }
}

extension View {
    /// Presents `sheet` as the design's bottom sheet over this view (and anything inside it).
    func eirsSheet<SheetContent: View>(isPresented: Binding<Bool>, @ViewBuilder content: @escaping () -> SheetContent) -> some View {
        modifier(EirsSheetModifier(isPresented: isPresented, sheet: content))
    }
}
