import Foundation

public enum UserRole: String, Hashable, Sendable { case owner = "OWNER", admin = "ADMIN", cashier = "CASHIER"
    public var displayName: String { rawValue.capitalized }
}

public struct User: Hashable, Sendable {
    public let id: String
    public let firstName: String
    public let lastName: String
    public let email: String
    public let phone: String
    public let role: UserRole
    public init(id: String, firstName: String, lastName: String, email: String, phone: String, role: UserRole) {
        self.id = id; self.firstName = firstName; self.lastName = lastName; self.email = email; self.phone = phone; self.role = role
    }
    public var fullName: String { "\(firstName) \(lastName)" }
    public var initials: String { "\(firstName.prefix(1))\(lastName.prefix(1))".uppercased() }
}

public struct Merchant: Hashable, Sendable {
    public let id: String
    public let displayName: String
    public let seller: Party
    public let environment: String
    public init(id: String, displayName: String, seller: Party, environment: String) {
        self.id = id; self.displayName = displayName; self.seller = seller; self.environment = environment
    }
}

public struct Session: Hashable, Sendable {
    public let user: User
    public let merchant: Merchant
    public init(user: User, merchant: Merchant) { self.user = user; self.merchant = merchant }
}

public enum VerificationChannel: String, Hashable, Sendable { case sms = "SMS", email = "EMAIL" }

public struct PendingAccount: Hashable, Sendable {
    public let fullName: String
    public let maskedPhone: String
    public let maskedEmail: String
    public init(fullName: String, maskedPhone: String, maskedEmail: String) {
        self.fullName = fullName; self.maskedPhone = maskedPhone; self.maskedEmail = maskedEmail
    }
}

public struct SignUpRequest: Hashable, Sendable {
    public let firstName: String, lastName: String, email: String, phone: String, password: String
    public init(firstName: String, lastName: String, email: String, phone: String, password: String) {
        self.firstName = firstName; self.lastName = lastName; self.email = email; self.phone = phone; self.password = password
    }
}

public enum SignInResult: Sendable {
    case success(Session)
    /// Account was created with a temporary password; the user must set their own first.
    case passwordChangeRequired(Session)
    case invalidCredentials
}

public struct BranchInfo: Hashable, Sendable {
    public let branch: Branch
    public let sourceSystems: Int
    public let isDefault: Bool
    public init(branch: Branch, sourceSystems: Int, isDefault: Bool) {
        self.branch = branch; self.sourceSystems = sourceSystems; self.isDefault = isDefault
    }
}

public struct SetupStep: Hashable, Sendable {
    public let title: String
    public let description: String
}

public let setupSteps: [SetupStep] = [
    SetupStep(title: "MoR credentials", description: "Save your EIRMS client ID, secret, API key, TIN and environment."),
    SetupStep(title: "Seller profile", description: "Legal name, TIN, VAT number, wereda and region."),
    SetupStep(title: "Source system", description: "Register at least one POS or storefront system."),
    SetupStep(title: "Verify setup", description: "Run a signed probe against the MoR endpoint."),
    SetupStep(title: "API keys unlocked", description: "Issue live keys once every step above is green."),
]
