import SwiftUI
import CoreImage
import CoreImage.CIFilterBuiltins

/// The public MoR verification link encoded in every document QR.
func verificationURL(irn: String) -> String { "https://portal.mor.gov.et/verify?irn=\(irn)" }

/// Real, scannable QR code. Today it encodes the verification URL for the IRN; once the backend
/// returns MoR's `signed_qr` payload, pass that instead.
struct QrCodeView: View {
    @Environment(\.eirs) private var eirs
    var content: String
    var color: Color?

    var body: some View {
        if let matrix = QrMatrix.make(content) {
            Canvas { context, size in
                let n = matrix.size
                let cell = min(size.width, size.height) / CGFloat(n)
                for y in 0..<n {
                    for x in 0..<n where matrix.isDark(x, y) {
                        let rect = CGRect(x: CGFloat(x) * cell, y: CGFloat(y) * cell, width: cell + 0.5, height: cell + 0.5)
                        context.fill(Path(rect), with: .color(color ?? eirs.ink))
                    }
                }
            }
            .aspectRatio(1, contentMode: .fit)
        } else {
            Color.clear.aspectRatio(1, contentMode: .fit)
        }
    }
}

private struct QrMatrix {
    let size: Int
    private let dark: [Bool]

    func isDark(_ x: Int, _ y: Int) -> Bool { dark[y * size + x] }

    /// Renders with Core Image, then reads the 1-pixel-per-module bitmap back as booleans.
    static func make(_ text: String) -> QrMatrix? {
        let filter = CIFilter.qrCodeGenerator()
        filter.message = Data(text.utf8)
        filter.correctionLevel = "M"
        guard let image = filter.outputImage else { return nil }
        let context = CIContext(options: [.useSoftwareRenderer: true])
        guard let cg = context.createCGImage(image, from: image.extent) else { return nil }
        let w = cg.width, h = cg.height
        guard w == h, w > 0 else { return nil }
        var pixels = [UInt8](repeating: 0, count: w * h)
        let drawn: Bool = pixels.withUnsafeMutableBytes { buffer in
            guard let gray = CGColorSpace(name: CGColorSpace.genericGrayGamma2_2),
                  let bitmap = CGContext(data: buffer.baseAddress, width: w, height: h, bitsPerComponent: 8, bytesPerRow: w,
                                         space: gray, bitmapInfo: CGImageAlphaInfo.none.rawValue) else { return false }
            bitmap.interpolationQuality = .none
            bitmap.draw(cg, in: CGRect(x: 0, y: 0, width: w, height: h))
            return true
        }
        guard drawn else { return nil }
        return QrMatrix(size: w, dark: pixels.map { $0 < 128 })
    }
}

/// Decorative 13×13 QR-like pattern for the onboarding illustration (same generator as the prototype).
struct DecorativeQr: View {
    @Environment(\.eirs) private var eirs
    var color: Color?

    private static let cells: [Bool] = {
        var seed = 7
        return (0..<169).map { i in
            let r = i / 13, c = i % 13
            func f(_ rr: Int, _ cc: Int) -> Bool { (0...3).contains(rr) && (0...3).contains(cc) }
            if f(r, c) || f(r, c - 9) || f(r - 9, c) {
                let rr = r >= 9 ? r - 9 : r
                let cc = c >= 9 ? c - 9 : c
                return rr == 0 || rr == 3 || cc == 0 || cc == 3 || ((1...2).contains(rr) && (1...2).contains(cc))
            }
            seed = (seed * 9301 + 49297) % 233_280
            return Double(seed) / 233_280.0 > 0.52
        }
    }()

    var body: some View {
        Canvas { context, size in
            let cell = min(size.width, size.height) / 13
            for (i, on) in Self.cells.enumerated() where on {
                let rect = CGRect(x: CGFloat(i % 13) * cell, y: CGFloat(i / 13) * cell, width: cell + 0.5, height: cell + 0.5)
                context.fill(Path(rect), with: .color(color ?? eirs.ink))
            }
        }
        .aspectRatio(1, contentMode: .fit)
    }
}
