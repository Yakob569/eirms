import SwiftUI

extension Color {
    /// 0xRRGGBB
    init(hex: UInt32, opacity: Double = 1) {
        self.init(.sRGB,
                  red: Double((hex >> 16) & 0xFF) / 255,
                  green: Double((hex >> 8) & 0xFF) / 255,
                  blue: Double(hex & 0xFF) / 255,
                  opacity: opacity)
    }
}

/// Brand tokens from the design (CSS vars --b, --bd, --ink, --t1, --t2, --bg, --ln, --mu).
struct EirsColors {
    var brand: Color
    var brandDark: Color
    var ink: Color
    var tint1: Color
    var tint2: Color
    var background: Color
    var line: Color
    var muted: Color

    var surface = Color.white
    var placeholder = Color(hex: 0x9AAEB3)
    var red = Color(hex: 0xB42318)
    var redTint = Color(hex: 0xFDECEA)
    var redBorder = Color(hex: 0xF3C5BF)
    var redInk = Color(hex: 0x7A2A20)
    var amberTint = Color(hex: 0xFFF6E0)
    var amberBorder = Color(hex: 0xF1DDA6)
    var amberInk = Color(hex: 0x6B4700)
    var warnTint = Color(hex: 0xFFF3DC)
    var warnInk = Color(hex: 0x8A5A00)
    var paidTint = Color(hex: 0xE4F2EA)
    var paidInk = Color(hex: 0x12704F)
    var neutralChip = Color(hex: 0xEEF1F2)
    var pending = Color(hex: 0xC9A227)
    var strengthFair = Color(hex: 0xC98A00)
    var paperBackdrop = Color(hex: 0xE4EAEB)
    var scrim = Color(hex: 0x0B2A33, opacity: 0.4)
}

/// Teal is the chosen brand; green and blue were the alternates explored in the design.
enum BrandPalette: CaseIterable {
    case teal, green, blue

    var colors: EirsColors {
        switch self {
        case .teal:
            return EirsColors(brand: Color(hex: 0x0E7C86), brandDark: Color(hex: 0x0A636B), ink: Color(hex: 0x0B2A33),
                              tint1: Color(hex: 0xE6F3F4), tint2: Color(hex: 0xC9E5E7), background: Color(hex: 0xF5F9F9),
                              line: Color(hex: 0xD7E5E6), muted: Color(hex: 0x4A6570))
        case .green:
            return EirsColors(brand: Color(hex: 0x12704F), brandDark: Color(hex: 0x0C5A3F), ink: Color(hex: 0x0A2E24),
                              tint1: Color(hex: 0xE7F2EC), tint2: Color(hex: 0xC8E3D4), background: Color(hex: 0xF5FAF7),
                              line: Color(hex: 0xD4E4DA), muted: Color(hex: 0x4B665A))
        case .blue:
            return EirsColors(brand: Color(hex: 0x2150D6), brandDark: Color(hex: 0x1A40AE), ink: Color(hex: 0x0C1B48),
                              tint1: Color(hex: 0xEAEFFD), tint2: Color(hex: 0xCFDAF9), background: Color(hex: 0xF6F8FE),
                              line: Color(hex: 0xD9E0F2), muted: Color(hex: 0x4D5877))
        }
    }
}

private struct EirsColorsKey: EnvironmentKey {
    static let defaultValue = BrandPalette.teal.colors
}

extension EnvironmentValues {
    var eirs: EirsColors {
        get { self[EirsColorsKey.self] }
        set { self[EirsColorsKey.self] = newValue }
    }
}

extension View {
    /// Installs the brand palette for everything below this view.
    func eirsTheme(_ palette: BrandPalette = .teal) -> some View {
        environment(\.eirs, palette.colors)
            .tint(palette.colors.brand)
            .foregroundStyle(palette.colors.ink)
            .font(EirsFont.sans(15))
    }
}

/// Instrument Serif for display, Geist for UI, Geist Mono for codes, IRNs and amounts.
/// Font files are bundled in Resources/Fonts and registered through Info.plist (UIAppFonts).
enum EirsFont {
    enum SansWeight { case regular, medium, semibold, bold }
    enum MonoWeight { case regular, medium }

    static func sans(_ size: CGFloat, _ weight: SansWeight = .regular) -> Font {
        let name: String
        switch weight {
        case .regular: name = "Geist-Regular"
        case .medium: name = "Geist-Medium"
        case .semibold: name = "Geist-SemiBold"
        case .bold: name = "Geist-Bold"
        }
        return .custom(name, fixedSize: size)
    }

    static func mono(_ size: CGFloat, _ weight: MonoWeight = .regular) -> Font {
        .custom(weight == .medium ? "GeistMono-Medium" : "GeistMono-Regular", fixedSize: size)
    }

    static func serif(_ size: CGFloat, italic: Bool = false) -> Font {
        .custom(italic ? "InstrumentSerif-Italic" : "InstrumentSerif-Regular", fixedSize: size)
    }
}
