import Foundation

public enum Masking {
    public static func email(_ email: String) -> String {
        guard let at = email.firstIndex(of: "@"), at != email.startIndex else { return email }
        let local = email[..<at]
        return String(local.prefix(2)) + "••••" + String(email[at...])
    }

    /// "+251 92 ••• 2273" style mask for Ethiopian mobile numbers.
    public static func phone(_ phone: String) -> String {
        var digits = phone.filter(\.isNumber)
        if digits.hasPrefix("251") { digits = String(digits.dropFirst(3)) }
        guard digits.count >= 6 else { return phone }
        return "+251 \(digits.prefix(2)) ••• \(digits.suffix(4))"
    }
}

/// English amount in words for fiscal receipts: "Twelve thousand ... birr and fifty cents".
public enum AmountInWords {
    private static let ones = ["", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
                               "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"]
    private static let tens = ["", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"]

    private static func hundreds(_ value: Int) -> String {
        var x = value
        var out = ""
        if x >= 100 {
            out += ones[x / 100] + " hundred"
            x %= 100
            if x > 0 { out += " " }
        }
        if x >= 20 {
            out += tens[x / 10]
            if x % 10 > 0 { out += "-" + ones[x % 10] }
        } else if x > 0 {
            out += ones[x]
        }
        return out
    }

    private static func words(_ value: Int) -> String {
        if value == 0 { return "zero" }
        var x = value
        var parts: [String] = []
        if x >= 1_000_000 { parts.append(hundreds(x / 1_000_000) + " million"); x %= 1_000_000 }
        if x >= 1000 { parts.append(hundreds(x / 1000) + " thousand"); x %= 1000 }
        if x > 0 { parts.append(hundreds(x)) }
        return parts.joined(separator: " ")
    }

    public static func birr(_ amount: Money) -> String {
        let rounded = amount.rounded()
        let cents = NSDecimalNumber(decimal: (rounded * 100).rounded(scale: 0)).intValue
        let whole = cents / 100
        let rest = cents % 100
        var text = words(whole) + " birr"
        if rest > 0 { text += " and " + words(rest) + " cents" }
        return text.prefix(1).uppercased() + text.dropFirst()
    }
}

public enum EmailFormat {
    public static func isValid(_ email: String) -> Bool {
        let trimmed = email.trimmingCharacters(in: .whitespaces)
        guard let at = trimmed.firstIndex(of: "@"), at != trimmed.startIndex else { return false }
        let domain = trimmed[trimmed.index(after: at)...]
        guard let dot = domain.lastIndex(of: "."), dot != domain.startIndex else { return false }
        return domain.index(after: dot) != domain.endIndex
    }
}
