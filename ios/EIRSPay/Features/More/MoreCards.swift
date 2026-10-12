import SwiftUI
import EIRSDomain

// MARK: - Terminal card

struct MoreTerminalCard: View {
    @Environment(\.eirs) private var eirs
    var onStart: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 12) {
                LineIcon(EirsIcons.monitor, size: 22, strokeWidth: 1.8, tint: .white)
                    .frame(width: 44, height: 44)
                    .background(eirs.brand, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                VStack(alignment: .leading, spacing: 2) {
                    Text("Terminal mode").font(EirsFont.serif(26)).foregroundStyle(Color.white)
                    Text("For counter tablets and sales staff").font(EirsFont.sans(12)).foregroundStyle(eirs.tint2)
                }
            }
            Text("Locks the app to a full-screen sales screen. Tabs, reports and settings stay hidden until you hold to exit.")
                .font(EirsFont.sans(13)).foregroundStyle(Color.white.opacity(0.8)).lineSpacing(5)
                .fixedSize(horizontal: false, vertical: true)
            PrimaryButton("Start terminal mode", height: 48, radius: 12, fontSize: 15, action: onStart)
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(eirs.ink)
        .overlay(alignment: .topTrailing) {
            Circle().strokeBorder(Color.white.opacity(0.12), lineWidth: 1)
                .frame(width: 160, height: 160)
                .offset(x: 50, y: -50)
                .allowsHitTesting(false)
        }
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
    }
}

// MARK: - Chevron row

private struct MoreChevronRow<Content: View>: View {
    @Environment(\.eirs) private var eirs
    var height: CGFloat
    var divider: Bool
    var action: () -> Void
    @ViewBuilder var content: () -> Content

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                content()
                LineIcon(EirsIcons.chevronRight, size: 15, tint: eirs.muted)
            }
            .padding(.horizontal, 16)
            .frame(maxWidth: .infinity, minHeight: height, maxHeight: height)
            .contentShape(Rectangle())
        }
        .buttonStyle(PressFillStyle(normal: Color.white, pressed: eirs.background, radius: 0))
        .overlay(alignment: .bottom) {
            if divider { Rectangle().fill(eirs.line).frame(height: 1) }
        }
    }
}

// MARK: - Tools

private struct MoreToolRow: Identifiable {
    let id: String
    let icon: LineIconSpec
    let label: String
    let meta: String
    let action: () -> Void
}

struct MoreToolsCard: View {
    @Environment(\.eirs) private var eirs
    var activeItems: Int
    var awaiting: Int
    var onItems: () -> Void
    var onWithholding: () -> Void
    var onCanceled: () -> Void

    private var rows: [MoreToolRow] {
        let withholdingMeta = awaiting > 0 ? "\(awaiting) awaiting certificate" : "All certificates in"
        return [
            MoreToolRow(id: "items", icon: EirsIcons.package, label: "Items", meta: "\(activeItems) active", action: onItems),
            MoreToolRow(id: "withholding", icon: EirsIcons.percent, label: "Withholding", meta: withholdingMeta, action: onWithholding),
            MoreToolRow(id: "canceled", icon: EirsIcons.ban, label: "Canceled invoices", meta: "", action: onCanceled),
        ]
    }

    var body: some View {
        let list = rows
        EirsCard(padding: EdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0)) {
            ForEach(list.indices, id: \.self) { index in
                toolRow(list[index], divider: index < list.count - 1)
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private func toolRow(_ row: MoreToolRow, divider: Bool) -> some View {
        MoreChevronRow(height: 60, divider: divider, action: row.action) {
            LineIcon(row.icon, size: 17, strokeWidth: 1.9, tint: eirs.brand)
                .frame(width: 34, height: 34)
                .background(eirs.tint1, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
            Text(row.label).font(EirsFont.sans(15, .medium)).foregroundStyle(eirs.ink).lineLimit(1)
                .frame(maxWidth: .infinity, alignment: .leading)
            if !row.meta.isEmpty {
                Text(row.meta).font(EirsFont.sans(12)).foregroundStyle(eirs.muted).lineLimit(1)
            }
        }
    }
}

// MARK: - Fiscal readiness

struct MoreReadinessCard: View {
    @Environment(\.eirs) private var eirs
    var done: Int
    var session: Session?

    var body: some View {
        let left = 5 - done
        let role = (session?.user.role ?? UserRole.owner).rawValue
        let plural = left == 1 ? "" : "s"
        let headline = done == 5 ? "You're ready. Live invoicing is unlocked." : "\(left) step\(plural) left before live invoicing."
        let shape = RoundedRectangle(cornerRadius: 18, style: .continuous)
        HStack(spacing: 16) {
            ring
            VStack(alignment: .leading, spacing: 6) {
                Eyebrow(text: "\(done * 20)% COMPLETE · \(role)", size: 10)
                Text(headline).font(EirsFont.serif(24)).foregroundStyle(eirs.ink)
                    .lineSpacing(1)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(eirs.tint1, in: shape)
        .overlay(shape.strokeBorder(eirs.tint2, lineWidth: 1))
    }

    private var ring: some View {
        let fraction = CGFloat(done) / 5
        return ZStack {
            Circle().stroke(eirs.tint2, lineWidth: 6).frame(width: 62, height: 62)
            Circle().trim(from: 0, to: fraction)
                .stroke(eirs.brand, style: StrokeStyle(lineWidth: 6, lineCap: .round))
                .rotationEffect(.degrees(-90))
                .frame(width: 62, height: 62)
                .opacity(done > 0 ? 1 : 0)
            Text("\(done)/5").font(EirsFont.serif(24)).foregroundStyle(eirs.ink)
        }
        .frame(width: 72, height: 72)
        .animation(.easeOut(duration: 0.4), value: done)
    }
}

struct MoreSetupStepper: View {
    @Environment(\.eirs) private var eirs
    var done: Int
    /// Review / Continue on a step opens its settings page.
    var onOpenStep: (SettingsPage) -> Void

    private static let stepPages: [SettingsPage] = [.morCredentials, .sellerProfile, .sourceSystems, .verifySetup, .apiKeys]

    var body: some View {
        EirsCard(padding: EdgeInsets(top: 6, leading: 16, bottom: 6, trailing: 16)) {
            ForEach(setupSteps.indices, id: \.self) { index in
                stepRow(index, setupSteps[index])
            }
        }
    }

    private func stepRow(_ index: Int, _ step: SetupStep) -> some View {
        let isDone = index < done
        let isCurrent = index == done
        let isLast = index == setupSteps.count - 1
        let titleColor: Color = (isDone || isCurrent) ? eirs.ink : eirs.muted
        return HStack(alignment: .top, spacing: 14) {
            VStack(spacing: 0) {
                marker(index: index, isDone: isDone, isCurrent: isCurrent)
                    .padding(.top, 14)
                Rectangle().fill(isDone ? eirs.brand : eirs.line)
                    .frame(width: 1.5)
                    .frame(maxHeight: .infinity)
                    .opacity(isLast ? 0 : 1)
            }
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 8) {
                    Text(step.title).font(EirsFont.sans(15, .semibold)).foregroundStyle(titleColor).lineLimit(1)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    statusBadge(isDone: isDone, isCurrent: isCurrent)
                }
                Text(step.description).font(EirsFont.sans(13)).foregroundStyle(eirs.muted).lineSpacing(4)
                    .fixedSize(horizontal: false, vertical: true)
                if isDone || isCurrent {
                    TextLink(isDone ? "Review →" : "Continue →", size: 13) { onOpenStep(Self.stepPages[index]) }
                        .padding(.top, 2)
                }
            }
            .padding(.top, 18)
            .padding(.bottom, 16)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .fixedSize(horizontal: false, vertical: true)
    }

    private func marker(index: Int, isDone: Bool, isCurrent: Bool) -> some View {
        let active = isDone || isCurrent
        return ZStack {
            Circle().fill(isDone ? eirs.brand : Color.white)
            Circle().strokeBorder(active ? eirs.brand : eirs.line, lineWidth: 1.5)
            if isDone {
                LineIcon(EirsIcons.check, size: 14, strokeWidth: 2.8, tint: .white)
            } else {
                MonoText(text: "\(index + 1)", color: isCurrent ? eirs.brand : eirs.muted)
            }
        }
        .frame(width: 32, height: 32)
    }

    private func statusBadge(isDone: Bool, isCurrent: Bool) -> some View {
        let bg: Color
        let fg: Color
        let label: String
        if isDone {
            bg = eirs.tint1; fg = eirs.brandDark; label = "Complete"
        } else if isCurrent {
            bg = eirs.ink; fg = Color.white; label = "Next"
        } else {
            bg = eirs.neutralChip; fg = eirs.muted; label = "Locked"
        }
        return Badge(text: label, background: bg, content: fg, radius: 999, fontSize: 11, tracking: 0, horizontal: 8, vertical: 3)
    }
}

// MARK: - Settings index

private struct MoreSettingRow: Identifiable {
    let label: String
    let meta: String
    /// nil: not built yet (Departments, Team, Roles do nothing).
    var page: SettingsPage?
    var id: String { label }
}

struct MoreSettingsCard: View {
    @Environment(\.eirs) private var eirs
    var session: Session?
    var settings: OrgSettings?
    var verified: Bool
    var onOpen: (SettingsPage) -> Void

    private var rows: [MoreSettingRow] {
        let userId = session?.user.id ?? ""
        let uid = userId.count > 8 ? String(userId.prefix(8)) + "…" : userId
        let env = settings?.credentials.environment.rawValue ?? session?.merchant.environment ?? "DEV"
        let activeSystems = settings?.sourceSystems.filter(\.active).count ?? 0
        let activeKeys = settings?.apiKeys.filter(\.active).count ?? 0
        return [
            MoreSettingRow(label: "Account", meta: uid, page: .account),
            MoreSettingRow(label: "MoR credentials", meta: env, page: .morCredentials),
            MoreSettingRow(label: "Seller profile", meta: settings?.seller.legalName ?? "", page: .sellerProfile),
            MoreSettingRow(label: "Branches", meta: "\(settings?.branches.count ?? 0)", page: .branches),
            MoreSettingRow(label: "Departments", meta: ""),
            MoreSettingRow(label: "Source systems", meta: "\(activeSystems) active", page: .sourceSystems),
            MoreSettingRow(label: "API keys", meta: "\(activeKeys) active", page: .apiKeys),
            MoreSettingRow(label: "Verify setup", meta: verified ? "Verified" : "Not verified", page: .verifySetup),
            MoreSettingRow(label: "Team", meta: "2"),
            MoreSettingRow(label: "Roles", meta: ""),
        ]
    }

    var body: some View {
        let list = rows
        EirsCard(padding: EdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0)) {
            ForEach(list.indices, id: \.self) { index in
                settingRow(list[index], divider: index < list.count - 1)
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private func settingRow(_ row: MoreSettingRow, divider: Bool) -> some View {
        MoreChevronRow(height: 52, divider: divider, action: { if let page = row.page { onOpen(page) } }) {
            Text(row.label).font(EirsFont.sans(15)).foregroundStyle(eirs.ink).lineLimit(1)
                .frame(maxWidth: .infinity, alignment: .leading)
            if !row.meta.isEmpty {
                Text(row.meta).font(EirsFont.sans(13)).foregroundStyle(eirs.muted).lineLimit(1)
                    .truncationMode(.tail)
                    .frame(maxWidth: 150, alignment: .trailing)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }
}
