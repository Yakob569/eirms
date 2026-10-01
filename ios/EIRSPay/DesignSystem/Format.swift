import Foundation
import EIRSDomain

/// Money and date formatting shared by every screen (en-US grouping, 2 decimals, "ETB " prefix,
/// Addis Ababa time).
enum Fmt {
    private static let two: NumberFormatter = {
        let f = NumberFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.numberStyle = .decimal
        f.usesGroupingSeparator = true
        f.groupingSeparator = ","
        f.decimalSeparator = "."
        f.minimumFractionDigits = 2
        f.maximumFractionDigits = 2
        return f
    }()

    private static func formatter(_ pattern: String) -> DateFormatter {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = ethiopiaTimeZone
        f.dateFormat = pattern
        return f
    }

    private static let dayMonthYear = formatter("d MMM yyyy")
    private static let clock = formatter("HH:mm")
    private static let clockSeconds = formatter("HH:mm:ss")
    private static let iso = formatter("yyyy-MM-dd")

    static func f2(_ value: Decimal) -> String { two.string(from: NSDecimalNumber(decimal: value)) ?? "0.00" }
    static func f2(_ value: Double) -> String { two.string(from: NSNumber(value: value)) ?? "0.00" }
    static func etb(_ value: Decimal) -> String { "ETB " + f2(value) }
    static func etb(_ value: Double) -> String { "ETB " + f2(value) }
    /// Whole-number amounts drop ".00" (tender chips).
    static func compact(_ value: Decimal) -> String {
        let text = f2(value)
        return text.hasSuffix(".00") ? String(text.dropLast(3)) : text
    }

    static func day(_ date: Date) -> String { dayMonthYear.string(from: date) }
    static func dateTime(_ date: Date) -> String { dayMonthYear.string(from: date) + ", " + clock.string(from: date) }
    static func time(_ date: Date) -> String { clock.string(from: date) }
    static func timeSeconds(_ date: Date) -> String { clockSeconds.string(from: date) }
    static func isoDate(_ date: Date) -> String { iso.string(from: date) }

    /// "ETH-0f4a1b2c··9c21ab": IRN shortened for lists.
    static func irnShort(_ irn: String) -> String { "ETH-" + irn.prefix(8) + "··" + irn.suffix(6) }

    static func plural(_ n: Int, _ word: String) -> String { "\(n) \(word)" + (n == 1 ? "" : "s") }

    static func parseMoney(_ text: String) -> Decimal? {
        guard !text.isEmpty, let value = Decimal(string: text, locale: Locale(identifier: "en_US_POSIX")) else { return nil }
        return value.rounded()
    }
}
