import SwiftUI

/// Parses SVG path data (the subset Lucide icons use: M L H V C S Q A Z, absolute and relative)
/// into a SwiftUI `Path` in the icon's 24×24 coordinate space.
enum SVGPath {
    static func parse(_ data: String) -> Path {
        var reader = Reader(Array(data))
        var path = Path()
        var command: Character = "M"
        var current = CGPoint.zero
        var subpathStart = CGPoint.zero
        var lastCubicControl: CGPoint?

        while true {
            if let next = reader.command() {
                command = next
                if next == "Z" || next == "z" {
                    path.closeSubpath()
                    current = subpathStart
                    lastCubicControl = nil
                    continue
                }
            } else if !reader.hasNumber() {
                break
            }

            let relative = command.isLowercase
            let base = relative ? current : .zero
            var usedCubic = false

            switch command {
            case "M", "m":
                guard let x = reader.number(), let y = reader.number() else { return path }
                current = CGPoint(x: base.x + x, y: base.y + y)
                subpathStart = current
                path.move(to: current)
                command = relative ? "l" : "L"   // further pairs are implicit line-tos
            case "L", "l":
                guard let x = reader.number(), let y = reader.number() else { return path }
                current = CGPoint(x: base.x + x, y: base.y + y)
                path.addLine(to: current)
            case "H", "h":
                guard let x = reader.number() else { return path }
                current = CGPoint(x: relative ? current.x + x : x, y: current.y)
                path.addLine(to: current)
            case "V", "v":
                guard let y = reader.number() else { return path }
                current = CGPoint(x: current.x, y: relative ? current.y + y : y)
                path.addLine(to: current)
            case "C", "c":
                guard let x1 = reader.number(), let y1 = reader.number(), let x2 = reader.number(),
                      let y2 = reader.number(), let x = reader.number(), let y = reader.number() else { return path }
                let c1 = CGPoint(x: base.x + x1, y: base.y + y1)
                let c2 = CGPoint(x: base.x + x2, y: base.y + y2)
                current = CGPoint(x: base.x + x, y: base.y + y)
                path.addCurve(to: current, control1: c1, control2: c2)
                lastCubicControl = c2
                usedCubic = true
            case "S", "s":
                guard let x2 = reader.number(), let y2 = reader.number(), let x = reader.number(), let y = reader.number() else { return path }
                let c1: CGPoint
                if let last = lastCubicControl { c1 = CGPoint(x: 2 * current.x - last.x, y: 2 * current.y - last.y) } else { c1 = current }
                let c2 = CGPoint(x: base.x + x2, y: base.y + y2)
                current = CGPoint(x: base.x + x, y: base.y + y)
                path.addCurve(to: current, control1: c1, control2: c2)
                lastCubicControl = c2
                usedCubic = true
            case "Q", "q":
                guard let x1 = reader.number(), let y1 = reader.number(), let x = reader.number(), let y = reader.number() else { return path }
                let control = CGPoint(x: base.x + x1, y: base.y + y1)
                current = CGPoint(x: base.x + x, y: base.y + y)
                path.addQuadCurve(to: current, control: control)
            case "A", "a":
                guard let rx = reader.number(), let ry = reader.number(), let rotation = reader.number(),
                      let largeArc = reader.flag(), let sweep = reader.flag(),
                      let x = reader.number(), let y = reader.number() else { return path }
                let end = CGPoint(x: base.x + x, y: base.y + y)
                addArc(to: &path, from: current, to: end, rx: CGFloat(rx), ry: CGFloat(ry),
                       rotationDegrees: CGFloat(rotation), largeArc: largeArc, sweep: sweep)
                current = end
            default:
                return path
            }
            if !usedCubic { lastCubicControl = nil }
        }
        return path
    }

    /// SVG elliptical arc → cubic Béziers (endpoint-to-centre parameterisation, per the SVG spec F.6.5).
    private static func addArc(to path: inout Path, from p0: CGPoint, to p1: CGPoint, rx rxIn: CGFloat, ry ryIn: CGFloat,
                               rotationDegrees: CGFloat, largeArc: Bool, sweep: Bool) {
        if p0 == p1 { return }
        var rx = abs(rxIn), ry = abs(ryIn)
        if rx == 0 || ry == 0 { path.addLine(to: p1); return }

        let phi = rotationDegrees * .pi / 180
        let cosPhi = cos(phi), sinPhi = sin(phi)
        let dx2 = (p0.x - p1.x) / 2, dy2 = (p0.y - p1.y) / 2
        let x1p = cosPhi * dx2 + sinPhi * dy2
        let y1p = -sinPhi * dx2 + cosPhi * dy2

        let lambda = (x1p * x1p) / (rx * rx) + (y1p * y1p) / (ry * ry)
        if lambda > 1 { let s = lambda.squareRoot(); rx *= s; ry *= s }

        let sign: CGFloat = (largeArc == sweep) ? -1 : 1
        let numerator = rx * rx * ry * ry - rx * rx * y1p * y1p - ry * ry * x1p * x1p
        let denominator = rx * rx * y1p * y1p + ry * ry * x1p * x1p
        let coefficient = sign * max(0, numerator / denominator).squareRoot()
        let cxp = coefficient * (rx * y1p / ry)
        let cyp = coefficient * -(ry * x1p / rx)
        let cx = cosPhi * cxp - sinPhi * cyp + (p0.x + p1.x) / 2
        let cy = sinPhi * cxp + cosPhi * cyp + (p0.y + p1.y) / 2

        let ux = (x1p - cxp) / rx, uy = (y1p - cyp) / ry
        let vx = (-x1p - cxp) / rx, vy = (-y1p - cyp) / ry
        let theta1 = atan2(uy, ux)
        var delta = atan2(ux * vy - uy * vx, ux * vx + uy * vy)
        if !sweep && delta > 0 { delta -= 2 * .pi }
        if sweep && delta < 0 { delta += 2 * .pi }

        let segments = max(1, Int((abs(delta) / (.pi / 2)).rounded(.up)))
        let step = delta / CGFloat(segments)
        let t = 4.0 / 3.0 * tan(step / 4)

        func map(_ px: CGFloat, _ py: CGFloat) -> CGPoint {
            CGPoint(x: cx + rx * px * cosPhi - ry * py * sinPhi, y: cy + rx * px * sinPhi + ry * py * cosPhi)
        }
        var a1 = theta1
        for _ in 0..<segments {
            let a2 = a1 + step
            let c1 = map(cos(a1) - t * sin(a1), sin(a1) + t * cos(a1))
            let c2 = map(cos(a2) + t * sin(a2), sin(a2) - t * cos(a2))
            let end = map(cos(a2), sin(a2))
            path.addCurve(to: end, control1: c1, control2: c2)
            a1 = a2
        }
    }

    private struct Reader {
        let chars: [Character]
        var index = 0
        init(_ chars: [Character]) { self.chars = chars }

        private mutating func skipSeparators() {
            while index < chars.count, chars[index] == " " || chars[index] == "," || chars[index] == "\n" || chars[index] == "\t" { index += 1 }
        }

        mutating func command() -> Character? {
            skipSeparators()
            guard index < chars.count, chars[index].isLetter else { return nil }
            defer { index += 1 }
            return chars[index]
        }

        mutating func hasNumber() -> Bool {
            skipSeparators()
            guard index < chars.count else { return false }
            let c = chars[index]
            return c.isNumber || c == "-" || c == "+" || c == "."
        }

        mutating func number() -> Double? {
            skipSeparators()
            let start = index
            if index < chars.count, chars[index] == "-" || chars[index] == "+" { index += 1 }
            var sawDigit = false, sawDot = false
            while index < chars.count {
                let c = chars[index]
                if c.isNumber { sawDigit = true; index += 1 }
                else if c == "." && !sawDot { sawDot = true; index += 1 }
                else { break }
            }
            guard sawDigit else { index = start; return nil }
            return Double(String(chars[start..<index]))
        }

        /// Arc flags are single characters and may be written without separators ("a1 1 0 011 1").
        mutating func flag() -> Bool? {
            skipSeparators()
            guard index < chars.count, chars[index] == "0" || chars[index] == "1" else { return nil }
            defer { index += 1 }
            return chars[index] == "1"
        }
    }
}
