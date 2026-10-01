import SwiftUI
import EIRSDomain

extension View {
    /// Chrome for screens pushed in the NavigationStack (the design draws its own header).
    func authPushedChrome() -> some View {
        navigationBarBackButtonHidden(true)
            .toolbar(.hidden, for: .navigationBar)
    }
}

/// Shared auth chrome: white screen, top row with the 44pt back square, the optional 3-step progress
/// ("STEP n OF 3") and the "EN" language pill, then a scrollable body padded 24/28/40.
///
/// The body is at least as tall as the viewport, so a `Spacer` inside `content` pushes the primary
/// button to the bottom when the form is short.
struct AuthScaffold<Content: View>: View {
    @Environment(\.eirs) private var eirs
    var onBack: () -> Void
    var step: Int?
    var spacing: CGFloat
    @ViewBuilder var content: () -> Content

    init(onBack: @escaping () -> Void, step: Int? = nil, spacing: CGFloat = 22, @ViewBuilder content: @escaping () -> Content) {
        self.onBack = onBack
        self.step = step
        self.spacing = spacing
        self.content = content
    }

    var body: some View {
        VStack(spacing: 0) {
            topRow
            GeometryReader { proxy in
                ScrollView {
                    VStack(alignment: .leading, spacing: spacing) { content() }
                        .frame(maxWidth: .infinity, minHeight: max(0, proxy.size.height - 68), alignment: .top)
                        .padding(.horizontal, 24)
                        .padding(.top, 28)
                        .padding(.bottom, 40)
                        .riseIn()
                }
                .scrollDismissesKeyboard(.interactively)
            }
        }
        .background(Color.white.ignoresSafeArea())
    }

    private var topRow: some View {
        HStack(spacing: 12) {
            BackButton(size: 44, action: onBack)
            progress
            languagePill
        }
        .padding(.horizontal, 20)
        .padding(.top, 12)
    }

    @ViewBuilder private var progress: some View {
        if let step {
            VStack(spacing: 6) {
                HStack(spacing: 4) {
                    ForEach(0..<3, id: \.self) { index in
                        Capsule()
                            .fill(index < step ? eirs.brand : eirs.tint2)
                            .frame(height: 4)
                    }
                }
                .frame(width: 140)
                Eyebrow(text: "STEP \(step) OF 3", color: eirs.muted, size: 10)
            }
            .frame(maxWidth: .infinity)
        } else {
            Spacer(minLength: 0)
        }
    }

    private var languagePill: some View {
        HStack(spacing: 5) {
            LineIcon(EirsIcons.globe, size: 13, tint: eirs.muted)
            Text("EN").font(EirsFont.sans(12)).foregroundStyle(eirs.muted)
        }
        .padding(.horizontal, 12)
        .frame(height: 36)
        .overlay(Capsule().strokeBorder(eirs.line, lineWidth: 1))
    }
}

/// Serif headline (with italic brand accent) + muted lead paragraph.
struct AuthHeading: View {
    @Environment(\.eirs) private var eirs
    var plain: String
    var accent: String = ""
    var trailing: String = ""
    var lead: String?
    var leadText: Text?
    var size: CGFloat = 40
    var leadSpacing: CGFloat = 3
    var gap: CGFloat = 8

    var body: some View {
        VStack(alignment: .leading, spacing: gap) {
            title
            if let leadText {
                leadText
                    .font(EirsFont.sans(15))
                    .foregroundColor(eirs.muted)
                    .lineSpacing(leadSpacing)
                    .fixedSize(horizontal: false, vertical: true)
            } else if let lead {
                Text(lead)
                    .font(EirsFont.sans(15))
                    .foregroundColor(eirs.muted)
                    .lineSpacing(leadSpacing)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    private var title: some View {
        let first = Text(plain).font(EirsFont.serif(size))
        let second = Text(accent).font(EirsFont.serif(size, italic: true)).foregroundColor(eirs.brand)
        let third = Text(trailing).font(EirsFont.serif(size))
        return (first + second + third)
            .foregroundColor(eirs.ink)
            .fixedSize(horizontal: false, vertical: true)
    }
}

/// Small red validation message (13pt).
struct AuthErrorText: View {
    @Environment(\.eirs) private var eirs
    let text: String

    var body: some View {
        Text(text)
            .font(EirsFont.sans(13))
            .foregroundStyle(eirs.red)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// Teal confirmation banner with a check-circle ("Account created for ...").
struct AuthConfirmBanner: View {
    @Environment(\.eirs) private var eirs
    let text: String

    var body: some View {
        HStack(spacing: 10) {
            LineIcon(EirsIcons.checkCircle, size: 18, strokeWidth: 2.2, tint: eirs.brandDark)
            Text(text)
                .font(EirsFont.sans(14, .medium))
                .foregroundStyle(eirs.brandDark)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(eirs.tint1, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

/// Bordered info card of label/value rows ("Account · Yishak Girma").
struct AuthInfoRows: View {
    @Environment(\.eirs) private var eirs
    let rows: [(String, String)]
    var monoLast = false

    var body: some View {
        VStack(spacing: 0) {
            ForEach(rows.indices, id: \.self) { index in
                row(index)
            }
        }
        .clipRounded(14)
        .solidBorder(eirs.line, radius: 14)
    }

    private func row(_ index: Int) -> some View {
        let mono = monoLast && index == rows.count - 1
        return HStack(spacing: 12) {
            Text(rows[index].0).font(EirsFont.sans(14)).foregroundStyle(eirs.muted)
            Spacer(minLength: 0)
            Text(rows[index].1)
                .font(mono ? EirsFont.mono(13) : EirsFont.sans(14))
                .foregroundStyle(eirs.ink)
                .multilineTextAlignment(.trailing)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .overlay(alignment: .top) {
            if index > 0 { Rectangle().fill(eirs.line).frame(height: 1) }
        }
    }
}

/// Success / done layout: the mark, headline and body are vertically centred in the free space and
/// the actions sit at the bottom. Use inside an `AuthScaffold` with `spacing: 0`.
struct AuthSuccessLayout<Message: View, Card: View, Actions: View>: View {
    @Environment(\.eirs) private var eirs
    var icon: LineIconSpec
    var plain: String
    var accent: String
    var titleSize: CGFloat = 44
    var iconSize: CGFloat = 32
    @ViewBuilder var message: () -> Message
    @ViewBuilder var card: () -> Card
    @ViewBuilder var actions: () -> Actions

    var body: some View {
        VStack(spacing: 0) {
            Spacer(minLength: 0)
            VStack(alignment: .leading, spacing: 24) {
                SuccessMark(icon: icon, iconSize: iconSize)
                VStack(alignment: .leading, spacing: 10) {
                    SerifTitle(plain: plain, accent: accent, size: titleSize)
                    message()
                        .font(EirsFont.sans(15))
                        .foregroundColor(eirs.muted)
                        .lineSpacing(5)
                        .fixedSize(horizontal: false, vertical: true)
                }
                card()
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            Spacer(minLength: 0)
            Spacer().frame(height: 24)
            actions()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

extension AuthSuccessLayout where Card == EmptyView {
    init(icon: LineIconSpec, plain: String, accent: String, titleSize: CGFloat = 44, iconSize: CGFloat = 32,
         @ViewBuilder message: @escaping () -> Message, @ViewBuilder actions: @escaping () -> Actions) {
        self.init(icon: icon, plain: plain, accent: accent, titleSize: titleSize, iconSize: iconSize,
                  message: message, card: { EmptyView() }, actions: actions)
    }
}

/// Labelled password input with the shared show/hide eye.
struct AuthPasswordField: View {
    var label: String
    @Binding var text: String
    var visible: Bool
    var placeholder: String
    var submitLabel: SubmitLabel = .next
    var onToggle: () -> Void

    var body: some View {
        LabeledField(label) {
            PasswordField(text: $text, visible: visible, placeholder: placeholder, submitLabel: submitLabel, onToggle: onToggle)
        }
    }
}

/// Strength meter (4 bars) + the 2-column rule checklist, as in the prototype's `pwVals()`.
struct AuthPasswordStrength: View {
    @Environment(\.eirs) private var eirs
    let check: PasswordCheck
    let password: String

    private var scoreColor: Color {
        let colors: [Color] = [eirs.line, eirs.red, eirs.strengthFair, eirs.brand, eirs.brandDark]
        return colors[min(max(check.score, 0), 4)]
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            meter
            VStack(alignment: .leading, spacing: 8) {
                ForEach([0, 2], id: \.self) { start in
                    HStack(spacing: 10) {
                        ruleCell(start)
                        ruleCell(start + 1)
                    }
                }
            }
        }
        .animation(.easeOut(duration: 0.2), value: check.score)
    }

    private var meter: some View {
        HStack(spacing: 10) {
            HStack(spacing: 4) {
                ForEach(0..<4, id: \.self) { index in
                    Capsule()
                        .fill(index < check.score ? scoreColor : eirs.line)
                        .frame(height: 4)
                }
            }
            Text(password.isEmpty ? "" : check.strengthLabel)
                .font(EirsFont.sans(12, .semibold))
                .foregroundStyle(scoreColor)
                .frame(minWidth: 48, alignment: .trailing)
        }
    }

    @ViewBuilder private func ruleCell(_ index: Int) -> some View {
        if index < check.rules.count {
            let rule = check.rules[index]
            HStack(spacing: 7) {
                ZStack {
                    Circle().fill(rule.satisfied ? eirs.brand : Color.clear)
                    Circle().strokeBorder(rule.satisfied ? eirs.brand : eirs.line, lineWidth: 1.5)
                    LineIcon(EirsIcons.check, size: 9, strokeWidth: 4, tint: Color.white)
                }
                .frame(width: 16, height: 16)
                Text(rule.label)
                    .font(EirsFont.sans(13))
                    .foregroundStyle(rule.satisfied ? eirs.ink : eirs.muted)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        } else {
            Color.clear.frame(maxWidth: .infinity, maxHeight: 1)
        }
    }
}

/// Centered muted tappable line ("Use a different method").
struct AuthMutedAction: View {
    @Environment(\.eirs) private var eirs
    let text: String
    let action: () -> Void

    var body: some View {
        Text(text)
            .font(EirsFont.sans(14))
            .foregroundStyle(eirs.muted)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 6)
            .onPlainTap(perform: action)
    }
}
