import SwiftUI

/// Left-aligned wrapping layout (CSS `flex-wrap: wrap`): chips and option pills that flow onto new lines.
struct FlowLayout: Layout {
    var spacing: CGFloat = 6
    var lineSpacing: CGFloat = 6

    private func arrange(maxWidth: CGFloat, subviews: Subviews) -> (size: CGSize, origins: [CGPoint]) {
        var origins: [CGPoint] = []
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        var widest: CGFloat = 0
        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x > 0 && x + size.width > maxWidth {
                x = 0
                y += rowHeight + lineSpacing
                rowHeight = 0
            }
            origins.append(CGPoint(x: x, y: y))
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
            widest = max(widest, x - spacing)
        }
        return (CGSize(width: widest, height: y + rowHeight), origins)
    }

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let maxWidth: CGFloat = proposal.width ?? .infinity
        return arrange(maxWidth: maxWidth, subviews: subviews).size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = arrange(maxWidth: bounds.width, subviews: subviews)
        for index in subviews.indices {
            let origin = result.origins[index]
            subviews[index].place(at: CGPoint(x: bounds.minX + origin.x, y: bounds.minY + origin.y),
                                  anchor: .topLeading, proposal: .unspecified)
        }
    }
}

/// `FlowLayout` that fills the available width, for use inside forms.
struct Wrap<Content: View>: View {
    var spacing: CGFloat = 6
    var lineSpacing: CGFloat = 6
    @ViewBuilder var content: () -> Content

    var body: some View {
        FlowLayout(spacing: spacing, lineSpacing: lineSpacing) { content() }
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}
