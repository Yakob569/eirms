import SwiftUI

/// The design's input: 52pt, radius 12, tinted background; on focus the border turns brand,
/// a 4pt tint ring appears and the fill goes white. Text is 16pt so iOS never zooms.
struct EirsTextField<Leading: View, Trailing: View>: View {
    @Environment(\.eirs) private var eirs
    @FocusState private var focused: Bool

    var placeholder = ""
    @Binding var text: String
    var height: CGFloat = 52
    var mono = false
    var alignment: TextAlignment = .leading
    var keyboard: UIKeyboardType = .default
    var capitalization: TextInputAutocapitalization = .never
    var submitLabel: SubmitLabel = .next
    var secure = false
    var focusRing = true
    var restingBackground: Color?
    var fontSize: CGFloat = 16
    var horizontalPadding: CGFloat = 14
    var onSubmit: (() -> Void)?
    @ViewBuilder var leading: () -> Leading
    @ViewBuilder var trailing: () -> Trailing

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: 12, style: .continuous)
        HStack(spacing: 0) {
            leading()
            field
                .padding(.horizontal, horizontalPadding)
            trailing()
        }
        .frame(height: height)
        .background(focused ? Color.white : (restingBackground ?? eirs.background), in: shape)
        .overlay(shape.strokeBorder(focused ? eirs.brand : eirs.line, lineWidth: 1))
        .background {
            if focused && focusRing {
                RoundedRectangle(cornerRadius: 16, style: .continuous).fill(eirs.tint1).padding(-4)
            }
        }
        .animation(.easeOut(duration: 0.15), value: focused)
        .contentShape(Rectangle())
        .onTapGesture { focused = true }
    }

    @ViewBuilder private var field: some View {
        let font = mono ? EirsFont.mono(fontSize) : EirsFont.sans(fontSize)
        ZStack(alignment: alignment == .trailing ? .trailing : .leading) {
            if text.isEmpty && !placeholder.isEmpty {
                Text(placeholder).font(font).foregroundStyle(eirs.placeholder).lineLimit(1)
            }
            Group {
                if secure { SecureField("", text: $text) } else { TextField("", text: $text) }
            }
            .font(font)
            .foregroundStyle(eirs.ink)
            .tint(eirs.brand)
            .multilineTextAlignment(alignment)
            .keyboardType(keyboard)
            .textInputAutocapitalization(capitalization)
            .autocorrectionDisabled()
            .submitLabel(submitLabel)
            .focused($focused)
            .onSubmit { onSubmit?() }
        }
    }
}

extension EirsTextField where Leading == EmptyView, Trailing == EmptyView {
    init(_ placeholder: String = "", text: Binding<String>, height: CGFloat = 52, mono: Bool = false,
         alignment: TextAlignment = .leading, keyboard: UIKeyboardType = .default,
         capitalization: TextInputAutocapitalization = .never, submitLabel: SubmitLabel = .next, secure: Bool = false,
         focusRing: Bool = true, restingBackground: Color? = nil, fontSize: CGFloat = 16, horizontalPadding: CGFloat = 14,
         onSubmit: (() -> Void)? = nil) {
        self.init(placeholder: placeholder, text: text, height: height, mono: mono, alignment: alignment, keyboard: keyboard,
                  capitalization: capitalization, submitLabel: submitLabel, secure: secure, focusRing: focusRing,
                  restingBackground: restingBackground, fontSize: fontSize, horizontalPadding: horizontalPadding,
                  onSubmit: onSubmit, leading: { EmptyView() }, trailing: { EmptyView() })
    }
}

/// Field with its label stacked above (CSS `<label>` wrapping).
struct LabeledField<Content: View, TrailingLabel: View>: View {
    var label: String
    var labelSize: CGFloat = 14
    var gap: CGFloat = 8
    @ViewBuilder var trailingLabel: () -> TrailingLabel
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: gap) {
            HStack {
                Text(label).font(EirsFont.sans(labelSize, .medium))
                Spacer(minLength: 8)
                trailingLabel()
            }
            content()
        }
    }
}

extension LabeledField where TrailingLabel == EmptyView {
    init(_ label: String, labelSize: CGFloat = 14, gap: CGFloat = 8, @ViewBuilder content: @escaping () -> Content) {
        self.init(label: label, labelSize: labelSize, gap: gap, trailingLabel: { EmptyView() }, content: content)
    }
}

/// Password field with the 44pt show/hide eye button inside the right edge.
struct PasswordField: View {
    @Environment(\.eirs) private var eirs
    @Binding var text: String
    var visible: Bool
    var placeholder = "8+ characters"
    var height: CGFloat = 52
    var submitLabel: SubmitLabel = .next
    var onToggle: () -> Void

    var body: some View {
        EirsTextField(placeholder: placeholder, text: $text, height: height, submitLabel: submitLabel, secure: !visible,
                      leading: { EmptyView() },
                      trailing: {
                          Button(action: onToggle) {
                              LineIcon(EirsIcons.eye, size: 19, tint: visible ? eirs.brand : eirs.muted)
                                  .frame(width: 44, height: 44)
                                  .contentShape(Rectangle())
                          }
                          .buttonStyle(PressFillStyle(normal: .clear, pressed: eirs.tint1, radius: 10))
                          .padding(.trailing, 4)
                      })
    }
}

/// White search box with magnifier (lists).
struct SearchField: View {
    @Environment(\.eirs) private var eirs
    @Binding var text: String
    var placeholder: String

    var body: some View {
        EirsTextField(placeholder: placeholder, text: $text, height: 46, submitLabel: .search, focusRing: false,
                      restingBackground: .white, horizontalPadding: 10,
                      leading: { LineIcon(EirsIcons.search, size: 16, tint: eirs.muted).padding(.leading, 12) },
                      trailing: { EmptyView() })
    }
}

/// Compact form input used inside register forms (46pt, no focus ring).
struct FormField: View {
    @Binding var text: String
    var placeholder = ""
    var mono = false
    var alignment: TextAlignment = .leading
    var keyboard: UIKeyboardType = .default
    var restingBackground: Color?
    var fontSize: CGFloat = 16

    var body: some View {
        EirsTextField(placeholder, text: $text, height: 46, mono: mono, alignment: alignment, keyboard: keyboard,
                      focusRing: false, restingBackground: restingBackground, fontSize: fontSize, horizontalPadding: 12)
    }
}

extension String {
    /// Keeps only digits and one decimal point, for money inputs.
    var decimalOnly: String {
        var seenDot = false
        return filter { char in
            if char.isNumber { return true }
            if char == "." && !seenDot { seenDot = true; return true }
            return false
        }
    }
}
