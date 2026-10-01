import SwiftUI

// MARK: - Interaction helpers

/// Press feedback is a colour change (the design's `style-active`), never a ripple or dimming.
struct PressFillStyle: ButtonStyle {
    var normal: Color
    var pressed: Color
    var radius: CGFloat
    var opacityWhenDisabled: Double = 0.45
    @Environment(\.isEnabled) private var isEnabled

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(configuration.isPressed ? pressed : normal, in: RoundedRectangle(cornerRadius: radius, style: .continuous))
            .opacity(isEnabled ? 1 : opacityWhenDisabled)
    }
}

/// Plain, un-highlighted tap target (text links, chips).
struct PlainTapStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View { configuration.label }
}

extension View {
    /// Tap handler without any system highlight.
    func onPlainTap(perform action: @escaping () -> Void) -> some View {
        Button(action: action) { self.contentShape(Rectangle()) }.buttonStyle(PlainTapStyle())
    }

    /// Dashed rounded border (CSS `border: 1px dashed`).
    func dashedBorder(_ color: Color, radius: CGFloat, width: CGFloat = 1) -> some View {
        overlay(RoundedRectangle(cornerRadius: radius, style: .continuous)
            .strokeBorder(color, style: StrokeStyle(lineWidth: width, dash: [6, 5])))
    }

    /// Solid rounded border drawn inside the view's bounds.
    func solidBorder(_ color: Color, radius: CGFloat, width: CGFloat = 1) -> some View {
        overlay(RoundedRectangle(cornerRadius: radius, style: .continuous).strokeBorder(color, lineWidth: width))
    }

    func clipRounded(_ radius: CGFloat) -> some View {
        clipShape(RoundedRectangle(cornerRadius: radius, style: .continuous))
    }

    /// Hairline along one edge.
    func hairline(_ edge: VerticalEdge, color: Color, width: CGFloat = 1) -> some View {
        overlay(alignment: edge == .top ? .top : .bottom) { Rectangle().fill(color).frame(height: width) }
    }
}

// MARK: - Text

struct SerifTitle: View {
    @Environment(\.eirs) private var eirs
    var plain: String
    var accent: String = ""
    var size: CGFloat = 40
    var lineSpacing: CGFloat = 0
    var accentColor: Color?

    var body: some View {
        // Instrument Serif has no italic weight in the regular family: the accent uses the italic file.
        (Text(plain).font(EirsFont.serif(size)) + Text(accent).font(EirsFont.serif(size, italic: true)).foregroundColor(accentColor ?? eirs.brand))
            .lineSpacing(lineSpacing)
            .fixedSize(horizontal: false, vertical: true)
    }
}

struct Eyebrow: View {
    @Environment(\.eirs) private var eirs
    var text: String
    var color: Color?
    var size: CGFloat = 11
    var tracking: CGFloat = 0.14

    var body: some View {
        Text(text)
            .font(EirsFont.mono(size))
            .tracking(size * tracking)
            .foregroundStyle(color ?? eirs.brandDark)
            .lineLimit(1)
    }
}

struct MonoText: View {
    @Environment(\.eirs) private var eirs
    var text: String
    var size: CGFloat = 12
    var color: Color?
    var weight: EirsFont.MonoWeight = .regular
    var strike = false
    var alignment: TextAlignment = .leading
    var lineLimit: Int?

    var body: some View {
        Text(text)
            .font(EirsFont.mono(size, weight))
            .strikethrough(strike)
            .foregroundStyle(color ?? eirs.ink)
            .multilineTextAlignment(alignment)
            .lineLimit(lineLimit)
    }
}

struct MutedText: View {
    @Environment(\.eirs) private var eirs
    var text: String
    var size: CGFloat = 13
    var lineSpacing: CGFloat = 0
    var lineLimit: Int?

    init(_ text: String, size: CGFloat = 13, lineSpacing: CGFloat = 0, lineLimit: Int? = nil) {
        self.text = text; self.size = size; self.lineSpacing = lineSpacing; self.lineLimit = lineLimit
    }

    var body: some View {
        Text(text).font(EirsFont.sans(size)).foregroundStyle(eirs.muted).lineSpacing(lineSpacing).lineLimit(lineLimit)
    }
}

// MARK: - Buttons

/// 54pt brand button with the label on the left and an arrow on the right. Disabled = 45% opacity.
struct PrimaryButton: View {
    @Environment(\.eirs) private var eirs
    var title: String
    var enabled = true
    var height: CGFloat = 54
    var radius: CGFloat = 14
    var fontSize: CGFloat = 16
    var showArrow = true
    var container: Color?
    var pressedColor: Color?
    var content: Color = .white
    var action: () -> Void

    init(_ title: String, enabled: Bool = true, height: CGFloat = 54, radius: CGFloat = 14, fontSize: CGFloat = 16,
         showArrow: Bool = true, container: Color? = nil, pressedColor: Color? = nil, content: Color = .white,
         action: @escaping () -> Void) {
        self.title = title; self.enabled = enabled; self.height = height; self.radius = radius; self.fontSize = fontSize
        self.showArrow = showArrow; self.container = container; self.pressedColor = pressedColor
        self.content = content; self.action = action
    }

    var body: some View {
        Button(action: action) {
            HStack {
                if !showArrow { Spacer(minLength: 0) }
                Text(title).font(EirsFont.sans(fontSize, .semibold)).foregroundStyle(content)
                if showArrow { Spacer(minLength: 0); LineIcon(EirsIcons.arrowRight, size: 18, tint: content) }
                else { Spacer(minLength: 0) }
            }
            .padding(.horizontal, 20)
            .frame(maxWidth: .infinity, minHeight: height, maxHeight: height)
            .contentShape(Rectangle())
        }
        .buttonStyle(PressFillStyle(normal: container ?? eirs.brand, pressed: pressedColor ?? eirs.brandDark, radius: radius))
        .disabled(!enabled)
    }
}

struct OutlineButton: View {
    @Environment(\.eirs) private var eirs
    var title: String
    var height: CGFloat = 50
    var radius: CGFloat = 14
    var borderColor: Color?
    var contentColor: Color?
    var weight: EirsFont.SansWeight = .medium
    var fontSize: CGFloat = 15
    var action: () -> Void

    init(_ title: String, height: CGFloat = 50, radius: CGFloat = 14, borderColor: Color? = nil, contentColor: Color? = nil,
         weight: EirsFont.SansWeight = .medium, fontSize: CGFloat = 15, action: @escaping () -> Void) {
        self.title = title; self.height = height; self.radius = radius; self.borderColor = borderColor
        self.contentColor = contentColor; self.weight = weight; self.fontSize = fontSize; self.action = action
    }

    var body: some View {
        Button(action: action) {
            Text(title).font(EirsFont.sans(fontSize, weight)).foregroundStyle(contentColor ?? eirs.ink)
                .padding(.horizontal, 16).frame(minHeight: height, maxHeight: height).contentShape(Rectangle())
        }
        .buttonStyle(PlainTapStyle())
        .background(Color.white, in: RoundedRectangle(cornerRadius: radius, style: .continuous))
        .solidBorder(borderColor ?? eirs.line, radius: radius, width: 1.5)
    }
}

/// Square outlined icon button used for back navigation in headers.
struct IconSquareButton: View {
    @Environment(\.eirs) private var eirs
    var icon: LineIconSpec
    var size: CGFloat = 40
    var filled = false
    var iconSize: CGFloat = 18
    var strokeWidth: CGFloat = 2
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            LineIcon(icon, size: iconSize, strokeWidth: strokeWidth, tint: filled ? .white : eirs.ink)
                .frame(width: size, height: size)
                .background(filled ? eirs.brand : Color.white, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                .overlay { if !filled { RoundedRectangle(cornerRadius: 12, style: .continuous).strokeBorder(eirs.line, lineWidth: 1) } }
                .contentShape(Rectangle())
        }
        .buttonStyle(PlainTapStyle())
    }
}

struct BackButton: View {
    var size: CGFloat = 40
    var action: () -> Void
    var body: some View { IconSquareButton(icon: EirsIcons.arrowLeft, size: size, action: action) }
}

/// Inline brand-coloured text action ("Sign in", "Resend code").
struct TextLink: View {
    @Environment(\.eirs) private var eirs
    var title: String
    var size: CGFloat = 14
    var color: Color?
    var weight: EirsFont.SansWeight = .semibold
    var action: () -> Void

    init(_ title: String, size: CGFloat = 14, color: Color? = nil, weight: EirsFont.SansWeight = .semibold, action: @escaping () -> Void) {
        self.title = title; self.size = size; self.color = color; self.weight = weight; self.action = action
    }

    var body: some View {
        Button(action: action) {
            Text(title).font(EirsFont.sans(size, weight)).foregroundStyle(color ?? eirs.brand).lineLimit(1)
        }
        .buttonStyle(PlainTapStyle())
    }
}

/// "Already have an account? Sign in" footer line.
struct PromptLink: View {
    @Environment(\.eirs) private var eirs
    var prompt: String
    var action: String
    var onTap: () -> Void

    var body: some View {
        HStack(spacing: 4) {
            Text(prompt).font(EirsFont.sans(14)).foregroundStyle(eirs.muted)
            TextLink(action, action: onTap)
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - Chips, badges, segmented controls, switches

enum ChipStyle { case solid, tint }

/// Filter chip. `.solid`: selected is ink-filled (range, status chips).
/// `.tint`: selected is tinted with a brand border (transaction-type, method chips).
struct EirsChip: View {
    @Environment(\.eirs) private var eirs
    var label: String
    var selected: Bool
    var style: ChipStyle = .solid
    var height: CGFloat = 36
    var radius: CGFloat = 999
    var mono = false
    var fontSize: CGFloat?
    var horizontalPadding: CGFloat = 14
    var action: () -> Void

    var body: some View {
        let colors = chipColors
        let size = fontSize ?? (mono ? 12 : 13)
        Button(action: action) {
            Text(label)
                .font(mono ? EirsFont.mono(size) : EirsFont.sans(size, .medium))
                .foregroundStyle(colors.fg)
                .lineLimit(1)
                .padding(.horizontal, horizontalPadding)
                .frame(height: height)
                .background(colors.bg, in: RoundedRectangle(cornerRadius: min(radius, height / 2), style: .continuous))
                .overlay(RoundedRectangle(cornerRadius: min(radius, height / 2), style: .continuous).strokeBorder(colors.border, lineWidth: 1))
                .contentShape(Rectangle())
        }
        .buttonStyle(PlainTapStyle())
    }

    private var chipColors: (bg: Color, fg: Color, border: Color) {
        if !selected { return (.white, eirs.ink, eirs.line) }
        switch style {
        case .solid: return (eirs.ink, .white, eirs.ink)
        case .tint: return (eirs.tint1, eirs.brandDark, eirs.brand)
        }
    }
}

/// Horizontally scrolling chip row that bleeds to the screen edges like the design's chip rails.
/// Place it inside a container with `edge` of horizontal padding.
struct ChipRail<Content: View>: View {
    var edge: CGFloat = 16
    var spacing: CGFloat = 6
    @ViewBuilder var content: () -> Content

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: spacing) { content() }
                .padding(.horizontal, edge)
        }
        .padding(.horizontal, -edge)
    }
}

struct Badge: View {
    var text: String
    var background: Color
    var content: Color
    var radius: CGFloat = 6
    var fontSize: CGFloat = 10
    var weight: EirsFont.SansWeight = .semibold
    var tracking: CGFloat = 0.04
    var horizontal: CGFloat = 7
    var vertical: CGFloat = 3

    var body: some View {
        Text(text)
            .font(EirsFont.sans(fontSize, weight))
            .tracking(fontSize * tracking)
            .foregroundStyle(content)
            .lineLimit(1)
            .padding(.horizontal, horizontal)
            .padding(.vertical, vertical)
            .background(background, in: RoundedRectangle(cornerRadius: radius, style: .continuous))
    }
}

/// Small mono tag such as a tax code ("VAT15") on a tinted background.
struct CodeTag: View {
    @Environment(\.eirs) private var eirs
    var text: String
    var background: Color?
    var content: Color?
    var fontSize: CGFloat = 10

    var body: some View {
        Text(text).font(EirsFont.mono(fontSize)).foregroundStyle(content ?? eirs.brandDark).lineLimit(1)
            .padding(.horizontal, 6).padding(.vertical, 2)
            .background(background ?? eirs.tint1, in: RoundedRectangle(cornerRadius: 5, style: .continuous))
    }
}

struct SegmentOption<Value: Hashable>: Identifiable {
    let value: Value
    let label: String
    var id: Value { value }
}

/// Grey track with a raised white thumb for the selected option (document type, buyer mode).
struct SegmentedControl<Value: Hashable>: View {
    @Environment(\.eirs) private var eirs
    var options: [SegmentOption<Value>]
    @Binding var selection: Value
    var height: CGFloat = 38
    var fontSize: CGFloat = 13
    var solidSelection = false

    var body: some View {
        HStack(spacing: 0) {
            ForEach(options) { option in
                let on = option.value == selection
                Button { selection = option.value } label: {
                    Text(option.label)
                        .font(EirsFont.sans(fontSize, .medium))
                        .foregroundStyle(on ? (solidSelection ? Color.white : eirs.ink) : (solidSelection ? eirs.ink : eirs.muted))
                        .lineLimit(1)
                        .frame(maxWidth: .infinity, minHeight: height, maxHeight: height)
                        .background {
                            if on {
                                RoundedRectangle(cornerRadius: 9, style: .continuous)
                                    .fill(solidSelection ? eirs.ink : Color.white)
                                    .shadow(color: solidSelection ? .clear : eirs.ink.opacity(0.15), radius: 1.5, y: 1)
                            }
                        }
                        .contentShape(Rectangle())
                }
                .buttonStyle(PlainTapStyle())
            }
        }
        .padding(3)
        .background(eirs.background, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
        .solidBorder(eirs.line, radius: 12)
    }
}

/// 42×26 pill switch with a sliding knob.
struct EirsSwitch: View {
    @Environment(\.eirs) private var eirs
    var isOn: Bool
    var small = false
    var onToggle: () -> Void

    var body: some View {
        let width: CGFloat = small ? 40 : 42
        let height: CGFloat = small ? 24 : 26
        let knob: CGFloat = small ? 16 : 18
        Button(action: onToggle) {
            ZStack(alignment: .leading) {
                Capsule().fill(isOn ? eirs.brand : eirs.line)
                Circle().fill(Color.white).frame(width: knob, height: knob)
                    .offset(x: isOn ? width - knob - 3 : 3)
            }
            .frame(width: width, height: height)
            .animation(.easeOut(duration: 0.15), value: isOn)
            .contentShape(Rectangle())
        }
        .buttonStyle(PlainTapStyle())
    }
}

/// Rounded checkbox square used for "I agree to the Terms".
struct EirsCheckbox: View {
    @Environment(\.eirs) private var eirs
    var checked: Bool

    var body: some View {
        RoundedRectangle(cornerRadius: 6, style: .continuous)
            .fill(checked ? eirs.brand : Color.white)
            .frame(width: 20, height: 20)
            .overlay(RoundedRectangle(cornerRadius: 6, style: .continuous).strokeBorder(checked ? eirs.brand : eirs.line, lineWidth: 1.5))
            .overlay { if checked { LineIcon(EirsIcons.check, size: 12, strokeWidth: 3, tint: .white) } }
    }
}

// MARK: - Surfaces

/// White card with a hairline border (radius 16): the building block of every app screen.
struct EirsCard<Content: View>: View {
    @Environment(\.eirs) private var eirs
    var padding = EdgeInsets(top: 16, leading: 16, bottom: 16, trailing: 16)
    var radius: CGFloat = 16
    var background: Color = .white
    var borderColor: Color?
    var spacing: CGFloat = 0
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: spacing) { content() }
            .padding(padding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(background, in: RoundedRectangle(cornerRadius: radius, style: .continuous))
            .solidBorder(borderColor ?? eirs.line, radius: radius)
    }
}

/// Tappable list card: brand border + soft shadow + tinted fill while pressed (the design's hover/active state).
struct ListCardStyle: ButtonStyle {
    @Environment(\.eirs) private var eirs

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(configuration.isPressed ? eirs.tint1 : Color.white, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .solidBorder(configuration.isPressed ? eirs.brand : eirs.line, radius: 16)
            .shadow(color: configuration.isPressed ? eirs.ink.opacity(0.35) : .clear, radius: 10, y: 6)
            .animation(.easeOut(duration: 0.15), value: configuration.isPressed)
    }
}

struct ListCard<Content: View>: View {
    var opacity: Double = 1
    var action: () -> Void
    @ViewBuilder var content: () -> Content

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 9) { content() }
                .padding(14)
                .frame(maxWidth: .infinity, alignment: .leading)
                .contentShape(Rectangle())
        }
        .buttonStyle(ListCardStyle())
        .opacity(opacity)
    }
}

/// Label / value row with a hairline on top, used in party, document and payment cards.
struct KeyValueRow: View {
    @Environment(\.eirs) private var eirs
    var key: String
    var value: String
    var mono = true
    var topBorder = true

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Text(key).font(EirsFont.sans(13)).foregroundStyle(eirs.muted).lineLimit(1).fixedSize()
            Text(value)
                .font(mono ? EirsFont.mono(12) : EirsFont.sans(13))
                .foregroundStyle(eirs.ink)
                .multilineTextAlignment(.trailing)
                .frame(maxWidth: .infinity, alignment: .trailing)
        }
        .padding(.vertical, 8)
        .overlay(alignment: .top) { if topBorder { Rectangle().fill(eirs.line).frame(height: 1) } }
    }
}

/// A titled card of key/value rows ("DOCUMENT", "SOURCE SYSTEM", "PAYMENT").
struct KeyValueCard: View {
    @Environment(\.eirs) private var eirs
    var title: String
    var rows: [(String, String)]

    var body: some View {
        EirsCard(padding: EdgeInsets(top: 12, leading: 16, bottom: 12, trailing: 16)) {
            Eyebrow(text: title, color: eirs.muted, size: 10, tracking: 0.12).padding(.bottom, 6)
            ForEach(rows.indices, id: \.self) { index in
                KeyValueRow(key: rows[index].0, value: rows[index].1)
            }
        }
    }
}

/// Dashed empty-state box: "No documents match these filters."
struct EmptyState: View {
    @Environment(\.eirs) private var eirs
    var text: String

    var body: some View {
        Text(text).font(EirsFont.sans(14)).foregroundStyle(eirs.muted).multilineTextAlignment(.center)
            .padding(.horizontal, 20).padding(.vertical, 36)
            .frame(maxWidth: .infinity)
            .background(Color.white, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            .dashedBorder(eirs.line, radius: 14)
    }
}

/// Numbered step heading inside a form card: ● 1  Document.
struct StepHeading<Trailing: View>: View {
    @Environment(\.eirs) private var eirs
    var number: Int
    var title: String
    @ViewBuilder var trailing: () -> Trailing

    var body: some View {
        HStack(spacing: 10) {
            Text("\(number)").font(EirsFont.mono(11)).foregroundStyle(Color.white)
                .frame(width: 24, height: 24).background(eirs.ink, in: Circle())
            Text(title).font(EirsFont.sans(15, .semibold)).frame(maxWidth: .infinity, alignment: .leading)
            trailing()
        }
    }
}

extension StepHeading where Trailing == EmptyView {
    init(number: Int, title: String) { self.init(number: number, title: title, trailing: { EmptyView() }) }
}

/// Soft rounded info banner (teal: confirmation, amber: warning, red: canceled).
struct Banner<Content: View>: View {
    var background: Color
    var content: Color
    var border: Color?
    var icon: LineIconSpec?
    @ViewBuilder var body_: () -> Content

    init(background: Color, content: Color, border: Color? = nil, icon: LineIconSpec? = nil, @ViewBuilder body: @escaping () -> Content) {
        self.background = background; self.content = content; self.border = border; self.icon = icon; self.body_ = body
    }

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            if let icon { LineIcon(icon, size: 18, strokeWidth: 2.2, tint: content).padding(.top, 1) }
            body_().font(EirsFont.sans(14, .medium)).foregroundStyle(content).lineSpacing(3)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.horizontal, 14).padding(.vertical, 12)
        .background(background, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
        .overlay { if let border { RoundedRectangle(cornerRadius: 12, style: .continuous).strokeBorder(border, lineWidth: 1) } }
    }
}

/// Thin 1pt divider in the line colour.
struct Hairline: View {
    @Environment(\.eirs) private var eirs
    var color: Color?
    var body: some View { Rectangle().fill(color ?? eirs.line).frame(height: 1) }
}

/// Dashed separator used on printed receipts.
struct DashedRule: View {
    @Environment(\.eirs) private var eirs
    var color: Color?
    var body: some View {
        Line().stroke(color ?? eirs.ink, style: StrokeStyle(lineWidth: 1.5, dash: [5, 4])).frame(height: 1.5)
    }

    private struct Line: Shape {
        func path(in rect: CGRect) -> Path {
            var p = Path()
            p.move(to: CGPoint(x: rect.minX, y: rect.midY))
            p.addLine(to: CGPoint(x: rect.maxX, y: rect.midY))
            return p
        }
    }
}
