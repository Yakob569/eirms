import SwiftUI
import EIRSDomain

// MARK: - Status pill (the one pill used across every settings page)

enum PillTone { case positive, warning, danger, strong, neutral }

/// Rounded status pill: Active, Verified, Failed, Main branch, OWNER, Deactivated…
/// The tone follows the label, like the prototype's single `pill()` helper.
struct StatusPill: View {
    @Environment(\.eirs) private var eirs
    var label: String
    var compact = false
    /// Overrides the background (e.g. white pill on the tinted excise banner).
    var background: Color?

    init(_ label: String, compact: Bool = false, background: Color? = nil) {
        self.label = label; self.compact = compact; self.background = background
    }

    static func tone(for label: String) -> PillTone {
        switch label {
        case "Active", "Verified", "This device", "Enabled", "Email verified": return .positive
        case "Unverified": return .warning
        case "Failed": return .danger
        case "Main branch", "OWNER", "ADMIN", "CASHIER": return .strong
        default: return .neutral
        }
    }

    private var colors: (bg: Color, fg: Color) {
        switch Self.tone(for: label) {
        case .positive: return (eirs.tint1, eirs.brandDark)
        case .warning: return (eirs.warnTint, Color(hex: 0x7A4E00))
        case .danger: return (eirs.redTint, eirs.red)
        case .strong: return (eirs.ink, .white)
        case .neutral: return (Color(hex: 0xEDF2F3), eirs.muted)
        }
    }

    var body: some View {
        let c = colors
        Text(label)
            .font(EirsFont.sans(compact ? 10 : 11, .semibold))
            .foregroundStyle(c.fg)
            .lineLimit(1)
            .fixedSize()
            .padding(.horizontal, compact ? 7 : 8)
            .padding(.vertical, compact ? 2 : 3)
            .background(background ?? c.bg, in: Capsule())
    }
}

// MARK: - Chip multi-select (the one chip picker used across every settings page)

/// One chip. `disabled` = outside what the branch / source system allows (greyed, not tappable).
/// `derived` = set elsewhere (EXCISE from merchant excise): dashed border, read-only.
struct ChipOption: Identifiable, Hashable {
    let id: String
    var label: String
    var selected: Bool
    var disabled = false
    var derived = false

    /// Options for a typed vocabulary. Values outside `ceiling` are disabled and shown unselected.
    static func from<T: RawRepresentable & Hashable>(_ all: [T], selected: Set<T>, ceiling: Set<T>? = nil,
                                                     label: (T) -> String = { $0.rawValue }) -> [ChipOption]
    where T.RawValue == String {
        all.map { value in
            let allowed = ceiling?.contains(value) ?? true
            return ChipOption(id: value.rawValue, label: label(value), selected: selected.contains(value) && allowed,
                              disabled: !allowed)
        }
    }
}

/// Wrapping row of selectable chips. Also used for single pickers (manager, branch, system type).
/// Pass `onToggle: nil` for a read-only chip list.
struct ChipMultiSelect: View {
    @Environment(\.eirs) private var eirs
    var options: [ChipOption]
    var mono = true
    var small = false
    var onToggle: ((String) -> Void)?

    var body: some View {
        Wrap(spacing: small ? 5 : 6, lineSpacing: small ? 5 : 6) {
            ForEach(options) { option in
                chip(option)
            }
        }
    }

    @ViewBuilder private func chip(_ option: ChipOption) -> some View {
        let fg: Color = option.selected ? eirs.brandDark : eirs.muted
        let bg: Color = option.selected ? eirs.tint1 : .white
        let border: Color = option.selected ? eirs.brand : eirs.line
        let size: CGFloat = small ? 10 : (mono ? 12 : 13)
        let label = Text(option.label)
            .font(mono ? EirsFont.mono(size) : EirsFont.sans(size))
            .foregroundStyle(fg)
            .lineLimit(1)
            .padding(.horizontal, small ? 7 : 12)
            .frame(height: small ? 20 : 34)
            .background(bg, in: Capsule())
            .overlay {
                if option.derived {
                    Capsule().strokeBorder(border, style: StrokeStyle(lineWidth: 1, dash: [4, 3]))
                } else {
                    Capsule().strokeBorder(border, lineWidth: 1)
                }
            }
            .opacity(option.disabled ? 0.45 : 1)
        if let onToggle, !option.disabled, !option.derived {
            Button { onToggle(option.id) } label: { label.contentShape(Capsule()) }
                .buttonStyle(PlainTapStyle())
        } else {
            label
        }
    }
}

/// Titled chip group: "Operations   3 of 6", chips, optional caption.
struct ChipGroup: View {
    @Environment(\.eirs) private var eirs
    var title: String
    var options: [ChipOption]
    var caption: String = ""
    var mono = true
    var onToggle: ((String) -> Void)?

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .firstTextBaseline, spacing: 8) {
                Text(title).font(EirsFont.sans(14, .semibold)).frame(maxWidth: .infinity, alignment: .leading)
                MonoText(text: "\(options.filter(\.selected).count) of \(options.count)", size: 11, color: eirs.muted)
            }
            ChipMultiSelect(options: options, mono: mono, onToggle: onToggle)
            if !caption.isEmpty {
                Text(caption).font(EirsFont.sans(12)).foregroundStyle(eirs.muted).lineSpacing(3)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }
}

extension Set {
    /// Adds the value if missing, removes it if present (chip taps).
    mutating func toggle(_ value: Element) {
        if contains(value) { remove(value) } else { insert(value) }
    }
}

// MARK: - Layout pieces

/// Mono small-caps label between cards ("SECURITY", "WHAT THIS BRANCH CAN ISSUE").
struct SettingsSectionLabel: View {
    @Environment(\.eirs) private var eirs
    var text: String
    var top: CGFloat = 8

    init(_ text: String, top: CGFloat = 8) { self.text = text; self.top = top }

    var body: some View {
        Eyebrow(text: text, color: eirs.muted)
            .padding(.horizontal, 4)
            .padding(.top, top)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// White settings card: 16pt padding, 12pt gaps.
struct SettingsCard<Content: View>: View {
    var spacing: CGFloat = 12
    @ViewBuilder var content: () -> Content
    var body: some View {
        EirsCard(spacing: spacing) { content() }
    }
}

/// Muted caption under a card or list.
struct SettingsNote: View {
    @Environment(\.eirs) private var eirs
    var text: String
    init(_ text: String) { self.text = text }
    var body: some View {
        Text(text).font(EirsFont.sans(12)).foregroundStyle(eirs.muted).lineSpacing(3)
            .fixedSize(horizontal: false, vertical: true)
            .padding(.horizontal, 4)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// Labelled 46pt field. Read-only fields render as a tinted box; `secure` masks the value unless revealed.
struct SettingsField: View {
    @Environment(\.eirs) private var eirs
    var label: String
    @Binding var text: String
    var editable = true
    var mono = false
    var secure = false
    var revealed = false
    var placeholder = ""
    var keyboard: UIKeyboardType = .default
    var fontSize: CGFloat = 15

    var body: some View {
        LabeledField(label, labelSize: 13, gap: 6) {
            if editable {
                EirsTextField(placeholder, text: $text, height: 46, mono: mono, keyboard: keyboard,
                              secure: secure && !revealed, focusRing: false, restingBackground: .white,
                              fontSize: fontSize, horizontalPadding: 12)
            } else {
                let shown = secure && !revealed ? String(repeating: "•", count: min(max(text.count, 8), 18)) : text
                Text(shown.isEmpty ? (placeholder.isEmpty ? "—" : placeholder) : shown)
                    .font(mono ? EirsFont.mono(fontSize - 1) : EirsFont.sans(fontSize))
                    .foregroundStyle(shown.isEmpty ? eirs.placeholder : eirs.ink)
                    .lineLimit(1)
                    .truncationMode(.middle)
                    .padding(.horizontal, 12)
                    .frame(maxWidth: .infinity, minHeight: 46, maxHeight: 46, alignment: .leading)
                    .background(eirs.background, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                    .solidBorder(eirs.line, radius: 12)
            }
        }
    }
}

/// Two-column grid of fields (addresses).
struct SettingsFieldGrid<Content: View>: View {
    @ViewBuilder var content: () -> Content
    var body: some View {
        LazyVGrid(columns: [GridItem(.flexible(), spacing: 10, alignment: .top), GridItem(.flexible(), spacing: 10, alignment: .top)],
                  alignment: .leading, spacing: 12) { content() }
    }
}

/// Switch with a title and note, in a bordered row ("Branch status", "Status").
struct SettingsSwitchRow<Trailing: View>: View {
    @Environment(\.eirs) private var eirs
    var title: String
    var note: String
    var isOn: Bool
    var disabled = false
    var onToggle: () -> Void
    @ViewBuilder var trailing: () -> Trailing

    var body: some View {
        HStack(spacing: 12) {
            EirsSwitch(isOn: isOn, onToggle: { if !disabled { onToggle() } })
                .opacity(disabled ? 0.5 : 1)
                .allowsHitTesting(!disabled)
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(EirsFont.sans(14, .medium))
                Text(note).font(EirsFont.sans(12)).foregroundStyle(eirs.muted)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            trailing()
        }
        .padding(12)
        .solidBorder(eirs.line, radius: 12)
    }
}

extension SettingsSwitchRow where Trailing == EmptyView {
    init(title: String, note: String, isOn: Bool, disabled: Bool = false, onToggle: @escaping () -> Void) {
        self.init(title: title, note: note, isOn: isOn, disabled: disabled, onToggle: onToggle, trailing: { EmptyView() })
    }
}

/// Card of key/value rows separated by hairlines (probe preview, sequence info).
struct SettingsRowsCard: View {
    @Environment(\.eirs) private var eirs
    var rows: [(String, String)]
    var monoValues = false

    var body: some View {
        VStack(spacing: 0) {
            ForEach(rows.indices, id: \.self) { index in
                HStack(alignment: .top, spacing: 12) {
                    Text(rows[index].0).font(EirsFont.sans(13)).foregroundStyle(eirs.muted).fixedSize()
                    Text(rows[index].1.isEmpty ? "—" : rows[index].1)
                        .font(monoValues ? EirsFont.mono(12, .medium) : EirsFont.sans(13, .medium))
                        .multilineTextAlignment(.trailing)
                        .frame(maxWidth: .infinity, alignment: .trailing)
                }
                .padding(.vertical, 11)
                .overlay(alignment: .bottom) { if index < rows.count - 1 { Hairline() } }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 4)
        .frame(maxWidth: .infinity)
        .background(Color.white, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .solidBorder(eirs.line, radius: 16)
    }
}

/// Count line above a settings list ("2 of 3 active").
struct SettingsCountLine: View {
    @Environment(\.eirs) private var eirs
    var text: String
    var body: some View {
        Text(text).font(EirsFont.sans(13)).foregroundStyle(eirs.muted)
            .padding(.horizontal, 4)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// Brand "+" square in the header of list pages.
struct SettingsPlusButton: View {
    var action: () -> Void
    var body: some View { IconSquareButton(icon: EirsIcons.plus, filled: true, strokeWidth: 2.4, action: action) }
}

/// Edit / Save toggle in the header of read-only pages (MoR credentials, Seller profile).
struct SettingsEditButton: View {
    @Environment(\.eirs) private var eirs
    var editing: Bool
    var action: () -> Void

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: 12, style: .continuous)
        Button(action: action) {
            Text(editing ? "Save" : "Edit")
                .font(EirsFont.sans(14, .semibold))
                .foregroundStyle(editing ? Color.white : eirs.ink)
                .padding(.horizontal, 16)
                .frame(height: 40)
                .background(editing ? eirs.brand : Color.white, in: shape)
                .overlay(shape.strokeBorder(editing ? eirs.brand : eirs.line, lineWidth: 1))
                .contentShape(Rectangle())
        }
        .buttonStyle(PlainTapStyle())
    }
}

/// Sticky footer of branch / source-system detail: why delete is blocked, Delete, Save changes.
struct SettingsEditFooter: View {
    @Environment(\.eirs) private var eirs
    var deleteBlocker: String?
    var onDelete: () -> Void
    var onSave: () -> Void

    var body: some View {
        StickyBottomBar(spacing: 8) {
            if let deleteBlocker {
                Text(deleteBlocker).font(EirsFont.sans(12)).foregroundStyle(eirs.muted)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            HStack(spacing: 8) {
                Button(action: onDelete) {
                    HStack(spacing: 8) {
                        LineIcon(EirsIcons.trash, size: 16, tint: eirs.red)
                        Text("Delete").font(EirsFont.sans(15, .semibold)).foregroundStyle(eirs.red)
                    }
                    .padding(.horizontal, 16)
                    .frame(height: 52)
                    .contentShape(Rectangle())
                }
                .buttonStyle(PlainTapStyle())
                .background(Color.white, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                .solidBorder(eirs.line, radius: 14)
                .opacity(deleteBlocker == nil ? 1 : 0.45)
                .disabled(deleteBlocker != nil)
                PrimaryButton("Save changes", height: 52, fontSize: 15, action: onSave)
            }
        }
    }
}
