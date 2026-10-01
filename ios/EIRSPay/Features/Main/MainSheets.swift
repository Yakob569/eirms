import SwiftUI
import EIRSDomain

private struct MainSheetTitle: View {
    var plain: String
    var accent: String = ""
    var size: CGFloat = 26
    var bottom: CGFloat = 8

    var body: some View {
        SerifTitle(plain: plain, accent: accent, size: size)
            .padding(.horizontal, 6)
            .padding(.bottom, bottom)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct BranchOption: Identifiable {
    let id: String
    let branch: Branch?
    let label: String
    let sub: String
}

/// "View data for": All branches + each merchant branch.
struct MainBranchSheet: View {
    @Environment(\.eirs) private var eirs
    var selected: Branch?
    var branches: [BranchInfo]
    var onPick: (Branch?) -> Void

    private var options: [BranchOption] {
        var list: [BranchOption] = [
            BranchOption(id: "all", branch: nil, label: "All branches", sub: "Combined view · \(branches.count) branches")
        ]
        for info in branches {
            let systems = Fmt.plural(info.sourceSystems, "source system")
            let sub = info.isDefault ? "Default · \(systems)" : systems
            list.append(BranchOption(id: info.branch.rawValue, branch: info.branch, label: info.branch.displayName, sub: sub))
        }
        return list
    }

    var body: some View {
        VStack(spacing: 6) {
            MainSheetTitle(plain: "View data for")
            ForEach(options) { option in
                optionRow(option)
            }
        }
    }

    private func optionRow(_ option: BranchOption) -> some View {
        let on = option.branch == selected
        let shape = RoundedRectangle(cornerRadius: 14, style: .continuous)
        return Button { onPick(option.branch) } label: {
            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(option.label).font(EirsFont.sans(15, on ? .semibold : .regular)).foregroundStyle(eirs.ink)
                    Text(option.sub).font(EirsFont.sans(12)).foregroundStyle(eirs.muted)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                if on { LineIcon(EirsIcons.check, size: 18, strokeWidth: 2.6, tint: eirs.brand) }
            }
            .padding(14)
            .background(on ? eirs.tint1 : Color.white, in: shape)
            .overlay(shape.strokeBorder(on ? eirs.brand : eirs.line, lineWidth: 1))
            .contentShape(Rectangle())
        }
        .buttonStyle(PlainTapStyle())
    }
}

private struct MainSheetRow: View {
    @Environment(\.eirs) private var eirs
    var icon: LineIconSpec
    var label: String
    var meta: String = ""
    var divider = false
    var color: Color
    var weight: EirsFont.SansWeight = .regular
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                LineIcon(icon, size: 18, strokeWidth: 1.8, tint: color)
                Text(label).font(EirsFont.sans(15, weight)).foregroundStyle(color)
                    .frame(maxWidth: .infinity, alignment: .leading)
                if !meta.isEmpty {
                    Text(meta).font(EirsFont.sans(13)).foregroundStyle(eirs.muted)
                }
            }
            .padding(.horizontal, 10)
            .frame(maxWidth: .infinity, minHeight: 52, maxHeight: 52)
            .contentShape(Rectangle())
            .overlay(alignment: .bottom) {
                if divider { Rectangle().fill(eirs.line).frame(height: 1) }
            }
        }
        .buttonStyle(PlainTapStyle())
    }
}

/// Profile sheet opened from the avatar.
struct MainUserSheet: View {
    @Environment(\.eirs) private var eirs
    var session: Session?
    var onAccountSettings: () -> Void
    var onSignOut: () -> Void

    var body: some View {
        VStack(spacing: 6) {
            profileHeader
            infoCard
            MainSheetRow(icon: EirsIcons.user, label: "Account settings", divider: true, color: eirs.ink, action: onAccountSettings)
            MainSheetRow(icon: EirsIcons.globe, label: "Language", meta: "English", divider: true, color: eirs.ink, action: {})
            MainSheetRow(icon: EirsIcons.logOut, label: "Sign out", color: eirs.red, weight: .medium, action: onSignOut)
        }
    }

    private var profileHeader: some View {
        HStack(spacing: 14) {
            Text(session?.user.initials ?? "")
                .font(EirsFont.sans(17, .semibold)).foregroundStyle(Color.white)
                .frame(width: 52, height: 52)
                .background(eirs.ink, in: Circle())
            VStack(alignment: .leading, spacing: 3) {
                Text(session?.user.fullName ?? "").font(EirsFont.sans(17, .semibold)).foregroundStyle(eirs.ink).lineLimit(1)
                Text(session?.user.email ?? "").font(EirsFont.sans(13)).foregroundStyle(eirs.muted).lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.horizontal, 6)
        .padding(.top, 6)
        .padding(.bottom, 14)
    }

    private var infoCard: some View {
        VStack(spacing: 9) {
            infoRow("Merchant") {
                Text(session?.merchant.displayName ?? "").font(EirsFont.sans(13, .medium)).foregroundStyle(eirs.ink)
            }
            infoRow("Merchant ID") { MonoText(text: session?.merchant.id ?? "", size: 13) }
            infoRow("User ID") { MonoText(text: session?.user.id ?? "", size: 13) }
            infoRow("Role") {
                Badge(text: session?.user.role.rawValue ?? "", background: eirs.ink, content: .white,
                      radius: 999, fontSize: 11, tracking: 0.06, horizontal: 8, vertical: 2)
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity)
        .background(eirs.background, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
    }

    private func infoRow<Value: View>(_ key: String, @ViewBuilder value: () -> Value) -> some View {
        HStack {
            Text(key).font(EirsFont.sans(13)).foregroundStyle(eirs.muted)
            Spacer(minLength: 8)
            value()
        }
    }
}

/// "Start terminal mode": what happens, which branch to sell for, and the lock button.
struct MainTerminalSheet: View {
    @Environment(\.eirs) private var eirs
    var branch: Branch
    var onPick: (Branch) -> Void
    var onStart: () -> Void

    private let lines: [String] = [
        "The app locks to a full-screen sales screen for this device.",
        "Tabs, reports and settings are hidden while it runs.",
        "Hold the Exit button to leave. The cart is cleared on exit.",
    ]

    var body: some View {
        VStack(spacing: 6) {
            MainSheetTitle(plain: "Start terminal ", accent: "mode", size: 28, bottom: 0)
            VStack(alignment: .leading, spacing: 10) {
                ForEach(lines.indices, id: \.self) { index in
                    HStack(alignment: .top, spacing: 10) {
                        Text("0\(index + 1)").font(EirsFont.mono(14, .medium)).foregroundStyle(eirs.brand)
                        Text(lines[index]).font(EirsFont.sans(14)).foregroundStyle(eirs.ink).lineSpacing(4)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
            }
            .padding(.horizontal, 6)
            .padding(.top, 6)
            .padding(.bottom, 8)
            .frame(maxWidth: .infinity, alignment: .leading)

            Text("Sell for branch").font(EirsFont.sans(13, .medium)).foregroundStyle(eirs.ink)
                .padding(.horizontal, 6)
                .frame(maxWidth: .infinity, alignment: .leading)
            HStack(spacing: 6) {
                ForEach(Branch.allCases) { option in
                    branchPill(option)
                }
            }
            .padding(.horizontal, 6)
            .padding(.bottom, 6)
            PrimaryButton("Lock into terminal mode", height: 52, radius: 14, fontSize: 15, action: onStart)
        }
    }

    private func branchPill(_ option: Branch) -> some View {
        let on = option == branch
        let shape = RoundedRectangle(cornerRadius: 12, style: .continuous)
        return Button { onPick(option) } label: {
            Text(option.displayName)
                .font(EirsFont.sans(14, .medium))
                .foregroundStyle(on ? Color.white : eirs.ink)
                .lineLimit(1)
                .frame(maxWidth: .infinity, minHeight: 44, maxHeight: 44)
                .background(on ? eirs.ink : Color.white, in: shape)
                .overlay(shape.strokeBorder(on ? eirs.ink : eirs.line, lineWidth: 1.5))
                .contentShape(Rectangle())
        }
        .buttonStyle(PlainTapStyle())
    }
}

/// "Register a document": invoice or receipt.
struct MainRegisterSheet: View {
    var onInvoice: () -> Void
    var onReceipt: () -> Void

    var body: some View {
        VStack(spacing: 6) {
            MainSheetTitle(plain: "Register a document")
            MainRegisterOption(icon: EirsIcons.filePlus, title: "Register invoice",
                               sub: "B2B, B2G and credit / debit notes", primary: true, action: onInvoice)
            MainRegisterOption(icon: EirsIcons.receipt, title: "Register receipt",
                               sub: "Walk-in and point-of-sale", primary: false, action: onReceipt)
        }
    }
}

private struct MainRegisterOption: View {
    @Environment(\.eirs) private var eirs
    var icon: LineIconSpec
    var title: String
    var sub: String
    var primary: Bool
    var action: () -> Void

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: 16, style: .continuous)
        let titleColor: Color = primary ? .white : eirs.ink
        let subColor: Color = primary ? Color.white.opacity(0.85) : eirs.muted
        let iconBg: Color = primary ? Color.white.opacity(0.16) : eirs.tint1
        let iconColor: Color = primary ? .white : eirs.brand
        Button(action: action) {
            HStack(spacing: 14) {
                LineIcon(icon, size: 20, strokeWidth: 1.8, tint: iconColor)
                    .frame(width: 44, height: 44)
                    .background(iconBg, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).font(EirsFont.sans(16, .semibold)).foregroundStyle(titleColor)
                    Text(sub).font(EirsFont.sans(12)).foregroundStyle(subColor)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                LineIcon(EirsIcons.arrowRight, size: 18, tint: primary ? Color.white : eirs.muted)
            }
            .padding(16)
            .background(primary ? eirs.brand : Color.white, in: shape)
            .overlay { if !primary { shape.strokeBorder(eirs.line, lineWidth: 1) } }
            .contentShape(Rectangle())
        }
        .buttonStyle(PlainTapStyle())
    }
}
