import Foundation

/// Money is always `Decimal`, rounded half-up to 2 places like the MoR documents.
public typealias Money = Decimal

/// Builds money from a string literal ("23213.50"); avoids Double rounding noise.
public func money(_ text: String) -> Money { Decimal(string: text, locale: Locale(identifier: "en_US_POSIX")) ?? 0 }

public extension Decimal {
    func rounded(scale: Int = 2) -> Decimal {
        var value = self
        var result = Decimal()
        NSDecimalRound(&result, &value, scale, .plain)
        return result
    }
    var doubleValue: Double { NSDecimalNumber(decimal: self).doubleValue }
}

public extension Sequence where Element == Decimal {
    func sumMoney() -> Decimal { reduce(Decimal(0), +) }
}
