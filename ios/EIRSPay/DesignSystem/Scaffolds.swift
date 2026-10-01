import SwiftUI

/// Header for full-screen views: back button, mono eyebrow over a serif title, optional trailing
/// content (status badge, + button). Sits under the status bar on a white surface with a hairline.
struct ViewHeader<Title: View, Trailing: View>: View {
    @Environment(\.eirs) private var eirs
    var eyebrow: String
    var onBack: () -> Void
    @ViewBuilder var title: () -> Title
    @ViewBuilder var trailing: () -> Trailing

    var body: some View {
        HStack(spacing: 12) {
            BackButton(action: onBack)
            VStack(alignment: .leading, spacing: 2) {
                if !eyebrow.isEmpty { Eyebrow(text: eyebrow, size: 10, tracking: 0.12) }
                title()
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            trailing()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(Color.white.ignoresSafeArea(edges: .top))
        .hairline(.bottom, color: eirs.line)
    }
}

extension ViewHeader where Trailing == EmptyView {
    init(eyebrow: String, onBack: @escaping () -> Void, @ViewBuilder title: @escaping () -> Title) {
        self.init(eyebrow: eyebrow, onBack: onBack, title: title, trailing: { EmptyView() })
    }
}

/// Serif header title with an optional italic accent: "Register *invoice.*"
struct ViewTitle: View {
    var plain: String
    var accent: String = ""

    var body: some View { SerifTitle(plain: plain, accent: accent, size: 26).lineLimit(1) }
}

/// Full-screen view: header, scrollable body on the tinted background, optional sticky bottom bar.
/// Body content is padded 16pt with 12pt gaps like the prototype.
struct ViewScaffold<Header: View, Bar: View, Body: View>: View {
    @Environment(\.eirs) private var eirs
    var background: Color?
    var spacing: CGFloat = 12
    var padding = EdgeInsets(top: 16, leading: 16, bottom: 16, trailing: 16)
    @ViewBuilder var header: () -> Header
    @ViewBuilder var bottomBar: () -> Bar
    @ViewBuilder var content: () -> Body

    var body: some View {
        VStack(spacing: 0) {
            header()
            ScrollView {
                VStack(spacing: spacing) { content() }
                    .padding(padding)
                    .frame(maxWidth: .infinity)
            }
            .scrollDismissesKeyboard(.interactively)
            bottomBar()
        }
        .background((background ?? eirs.background).ignoresSafeArea())
    }
}

extension ViewScaffold where Bar == EmptyView {
    init(background: Color? = nil, spacing: CGFloat = 12,
         padding: EdgeInsets = EdgeInsets(top: 16, leading: 16, bottom: 16, trailing: 16),
         @ViewBuilder header: @escaping () -> Header, @ViewBuilder content: @escaping () -> Body) {
        self.init(background: background, spacing: spacing, padding: padding, header: header, bottomBar: { EmptyView() }, content: content)
    }
}

/// Sticky bar pinned under a scrolling view: white, hairline on top, soft shadow, home-indicator safe.
struct StickyBottomBar<Content: View>: View {
    @Environment(\.eirs) private var eirs
    var spacing: CGFloat = 10
    var shadow = true
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(spacing: spacing) { content() }
            .padding(.horizontal, 16)
            .padding(.top, 12)
            .padding(.bottom, 14)
            .frame(maxWidth: .infinity)
            .background(Color.white.ignoresSafeArea(edges: .bottom))
            .hairline(.top, color: eirs.line)
            .shadow(color: shadow ? eirs.ink.opacity(0.18) : .clear, radius: 12, y: -4)
    }
}
