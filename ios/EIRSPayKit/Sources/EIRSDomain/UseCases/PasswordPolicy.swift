import Foundation

public struct PasswordRule: Hashable, Sendable {
    public let label: String
    public let satisfied: Bool
}

public struct PasswordCheck: Hashable, Sendable {
    public let rules: [PasswordRule]
    public let score: Int
    public var strengthLabel: String { ["", "Weak", "Fair", "Good", "Strong"][score] }
    public var acceptable: Bool { score >= 3 && (rules.first?.satisfied ?? false) }
}

/// Same rules the web console enforces: 8+ chars, mixed case, a number, a symbol; 3 of 4 is acceptable.
public enum PasswordPolicy {
    public static func evaluate(_ password: String) -> PasswordCheck {
        let rules = [
            PasswordRule(label: "8+ characters", satisfied: password.count >= 8),
            PasswordRule(label: "Upper & lowercase", satisfied: password.contains(where: \.isLowercase) && password.contains(where: \.isUppercase)),
            PasswordRule(label: "A number", satisfied: password.contains(where: \.isNumber)),
            PasswordRule(label: "A symbol", satisfied: password.contains(where: { !$0.isLetter && !$0.isNumber })),
        ]
        return PasswordCheck(rules: rules, score: rules.filter(\.satisfied).count)
    }
}
