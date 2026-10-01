import SwiftUI
import EIRSDomain

/// The dashboard's "Sales & tax" plot: three grid lines, a brand area + line for sales and a dashed
/// ink line for tax (drawn x4 so it reads on the same scale, as in the design). Touch or drag to scrub;
/// `onScrub` receives the nearest point index.
struct SalesChart: View {
    @Environment(\.eirs) private var eirs
    var points: [SalesPoint]
    var highlighted: Int?
    var onScrub: (Int) -> Void

    var body: some View {
        GeometryReader { proxy in
            Canvas { context, size in
                draw(in: &context, size: size)
            }
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { value in
                        if let index = nearestIndex(x: value.location.x, width: proxy.size.width) {
                            onScrub(index)
                        }
                    }
            )
        }
        .frame(height: 150)
    }

    private func nearestIndex(x: CGFloat, width: CGFloat) -> Int? {
        let n = points.count
        if n == 0 || width <= 0 { return nil }
        let raw = (x / width) * CGFloat(n - 1)
        let index = Int(raw.rounded())
        return min(max(index, 0), n - 1)
    }

    private func linePath(_ pts: [CGPoint]) -> Path {
        var path = Path()
        for (i, p) in pts.enumerated() {
            if i == 0 { path.move(to: p) } else { path.addLine(to: p) }
        }
        return path
    }

    private func draw(in context: inout GraphicsContext, size: CGSize) {
        let w = size.width
        let h = size.height
        let n = points.count

        var grid = Path()
        for y in [CGFloat(0.5), h / 2, h - 0.5] {
            grid.move(to: CGPoint(x: 0, y: y))
            grid.addLine(to: CGPoint(x: w, y: y))
        }
        context.stroke(grid, with: .color(eirs.line), lineWidth: 1)
        if n < 2 { return }

        let maxSales = points.map { $0.sales }.max() ?? 0
        let maxValue: Double = maxSales > 0 ? maxSales * 1.12 : 1
        var sales: [CGPoint] = []
        var tax: [CGPoint] = []
        for (i, p) in points.enumerated() {
            let x = CGFloat(i) / CGFloat(n - 1) * w
            let ySales = h - CGFloat(p.sales / maxValue) * h
            let yTax = h - CGFloat(p.tax / maxValue) * h * 4
            sales.append(CGPoint(x: x, y: ySales))
            tax.append(CGPoint(x: x, y: yTax))
        }

        var area = linePath(sales)
        area.addLine(to: CGPoint(x: w, y: h))
        area.addLine(to: CGPoint(x: 0, y: h))
        area.closeSubpath()
        let gradient = Gradient(colors: [eirs.brand.opacity(0.28), eirs.brand.opacity(0)])
        context.fill(area, with: .linearGradient(gradient, startPoint: CGPoint(x: 0, y: 0), endPoint: CGPoint(x: 0, y: h)))
        context.stroke(linePath(sales), with: .color(eirs.brand), style: StrokeStyle(lineWidth: 2.5, lineJoin: .round))
        context.stroke(linePath(tax), with: .color(eirs.ink), style: StrokeStyle(lineWidth: 1.5, dash: [5, 4]))

        if let i = highlighted, i >= 0, i < n {
            let p = sales[i]
            var guide = Path()
            guide.move(to: CGPoint(x: p.x, y: 0))
            guide.addLine(to: CGPoint(x: p.x, y: h))
            context.stroke(guide, with: .color(eirs.ink.opacity(0.25)), lineWidth: 1)
            let outer = Path(ellipseIn: CGRect(x: p.x - 6, y: p.y - 6, width: 12, height: 12))
            context.fill(outer, with: .color(Color.white))
            let ring = Path(ellipseIn: CGRect(x: p.x - 4.5, y: p.y - 4.5, width: 9, height: 9))
            context.stroke(ring, with: .color(eirs.brand), lineWidth: 3)
        }
    }
}

/// X-axis labels at 0, 1/3, 2/3 and the last point; the first is left-aligned, the last right-aligned.
struct SalesChartTicks: View {
    @Environment(\.eirs) private var eirs
    var points: [SalesPoint]

    private var indices: [Int] {
        let n = points.count
        if n == 0 { return [] }
        let third = Int((Double(n - 1) / 3.0).rounded())
        let twoThirds = Int((Double(2 * (n - 1)) / 3.0).rounded())
        return [0, third, twoThirds, n - 1]
    }

    var body: some View {
        GeometryReader { proxy in
            ZStack(alignment: .topLeading) {
                ForEach(indices.indices, id: \.self) { j in
                    tick(j, width: proxy.size.width)
                }
            }
        }
        .frame(height: 14)
    }

    private func tick(_ j: Int, width: CGFloat) -> some View {
        let n = points.count
        let idx = indices[j]
        let fraction: CGFloat = n > 1 ? CGFloat(idx) / CGFloat(n - 1) : 0
        let shift: CGFloat = j == 0 ? 0 : (j == indices.count - 1 ? 1 : 0.5)
        return Text(points[idx].label)
            .font(EirsFont.mono(10))
            .foregroundStyle(eirs.muted)
            .lineLimit(1)
            .fixedSize()
            .alignmentGuide(.leading) { d in d.width * shift }
            .offset(x: fraction * width)
    }
}
