import SwiftUI

/// A 24×24 stroke icon (Lucide style, as used throughout the design) defined by SVG path data.
struct LineIconSpec {
    let paths: [Path]
    init(_ data: String...) { paths = data.map(SVGPath.parse) }
}

private func circle(_ cx: Double, _ cy: Double, _ r: Double) -> String {
    "M\(cx - r) \(cy)a\(r) \(r) 0 1 0 \(2 * r) 0a\(r) \(r) 0 1 0 \(-2 * r) 0Z"
}

private func rect(_ x: Double, _ y: Double, _ w: Double, _ h: Double, _ rx: Double) -> String {
    "M\(x + rx) \(y)h\(w - 2 * rx)a\(rx) \(rx) 0 0 1 \(rx) \(rx)v\(h - 2 * rx)a\(rx) \(rx) 0 0 1 \(-rx) \(rx)h\(-(w - 2 * rx))a\(rx) \(rx) 0 0 1 \(-rx) \(-rx)v\(-(h - 2 * rx))a\(rx) \(rx) 0 0 1 \(rx) \(-rx)Z"
}

enum EirsIcons {
    static let shield = LineIconSpec(
        "M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z",
        "m9 12 2 2 4-4")
    static let arrowRight = LineIconSpec("M5 12h14M13 6l6 6-6 6")
    static let arrowLeft = LineIconSpec("M19 12H5M11 18l-6-6 6-6")
    static let globe = LineIconSpec(circle(12, 12, 10), "M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20", "M2 12h20")
    static let check = LineIconSpec("M20 6 9 17l-5-5")
    static let checkCircle = LineIconSpec(circle(12, 12, 10), "m9 12 2 2 4-4")
    static let smartphone = LineIconSpec(rect(5, 2, 14, 20, 2), "M12 18h.01")
    static let mail = LineIconSpec(rect(2, 4, 20, 16, 2), "m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7")
    static let eye = LineIconSpec("M2.06 12.35a1 1 0 0 1 0-.7 10.75 10.75 0 0 1 19.88 0 1 1 0 0 1 0 .7 10.75 10.75 0 0 1-19.88 0", circle(12, 12, 3))
    static let lock = LineIconSpec(rect(3, 11, 18, 11, 2), "M7 11V7a5 5 0 0 1 10 0v4")
    static let building = LineIconSpec("M6 22V4a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v18ZM6 12H4a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2h2M18 9h2a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2h-2M10 6h4M10 10h4M10 14h4M10 18h4")
    static let chevronDown = LineIconSpec("m6 9 6 6 6-6")
    static let chevronRight = LineIconSpec("m9 18 6-6-6-6")
    static let search = LineIconSpec(circle(11, 11, 8), "m21 21-4.3-4.3")
    static let copy = LineIconSpec(rect(8, 8, 14, 14, 2), "M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2")
    static let ban = LineIconSpec(circle(12, 12, 10), "m4.9 4.9 14.2 14.2")
    static let download = LineIconSpec("M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M7 10l5 5 5-5M12 15V3")
    static let close = LineIconSpec("M18 6 6 18M6 6l12 12")
    static let plus = LineIconSpec("M12 5v14M5 12h14")
    static let monitor = LineIconSpec(rect(2, 3, 20, 14, 2), "M8 21h8M12 17v4")
    static let user = LineIconSpec(circle(12, 8, 4), "M20 21a8 8 0 0 0-16 0")
    static let logOut = LineIconSpec("M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9")
    static let filePlus = LineIconSpec("M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7ZM14 2v4a2 2 0 0 0 2 2h4M12 11v6M9 14h6")
    static let receipt = LineIconSpec("M4 2v20l2-1 2 1 2-1 2 1 2-1 2 1 2-1 2 1V2l-2 1-2-1-2 1-2-1-2 1-2-1-2 1ZM8 8h8M8 12h8M8 16h5")
    static let grid = LineIconSpec("M3 3h7v7H3zM14 3h7v7h-7zM14 14h7v7h-7zM3 14h7v7H3z")
    static let fileText = LineIconSpec("M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7ZM14 2v4a2 2 0 0 0 2 2h4M16 13H8M16 17H8M10 9H8")
    static let sliders = LineIconSpec("M21 4h-7M10 4H3M21 12h-9M8 12H3M21 20h-5M12 20H3M14 2v4M8 10v4M16 18v4")
    static let package = LineIconSpec("m7.5 4.27 9 5.15M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16ZM3.3 7l8.7 5 8.7-5M12 22V12")
    // Row-action menu and settings (prototype paths).
    static let fileMinus = LineIconSpec("M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7ZM14 2v4a2 2 0 0 0 2 2h4M9 15h6")
    static let fileDiff = LineIconSpec("M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7ZM14 2v4a2 2 0 0 0 2 2h4M9 15h6M12 12v6")
    static let fileLines = LineIconSpec("M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7ZM14 2v4a2 2 0 0 0 2 2h4M8 13h8M8 17h5")
    static let xCircle = LineIconSpec(circle(12, 12, 10), "M15 9l-6 6M9 9l6 6")
    static let link = LineIconSpec("M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71")
    static let trash = LineIconSpec("M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2")
    static let signIn = LineIconSpec("M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4M10 17l5-5-5-5M15 12H3")
    static let alert = LineIconSpec("M12 9v4M12 17h.01M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z")
    static let rotateCcw = LineIconSpec("M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8M3 3v5h5")
    static let percent = LineIconSpec("M19 5 5 19M9 6.5a2.5 2.5 0 1 1-5 0 2.5 2.5 0 0 1 5 0M20 17.5a2.5 2.5 0 1 1-5 0 2.5 2.5 0 0 1 5 0")
}

/// Renders a `LineIconSpec` at `size`, stroked with rounded caps like the design's SVGs.
struct LineIcon: View {
    @Environment(\.eirs) private var eirs
    let icon: LineIconSpec
    var size: CGFloat = 18
    var strokeWidth: CGFloat = 2
    var tint: Color?

    init(_ icon: LineIconSpec, size: CGFloat = 18, strokeWidth: CGFloat = 2, tint: Color? = nil) {
        self.icon = icon; self.size = size; self.strokeWidth = strokeWidth; self.tint = tint
    }

    var body: some View {
        let scale = size / 24
        let style = StrokeStyle(lineWidth: strokeWidth * scale, lineCap: .round, lineJoin: .round)
        ZStack {
            ForEach(icon.paths.indices, id: \.self) { index in
                icon.paths[index]
                    .applying(CGAffineTransform(scaleX: scale, y: scale))
                    .stroke(tint ?? eirs.ink, style: style)
            }
        }
        .frame(width: size, height: size, alignment: .topLeading)
    }
}
