import Foundation
import EIRSDomain

/// Settings sample data mirroring the design prototype (`seedOrg`, `cred`, `seller`). Replace with API responses.
extension SampleData {
    static let staff = ["Abebe Kebede", "Hana Tesfaye", "Dawit Mekonnen", "Selam Girma", "Meron Alemu"]

    static func settings() -> OrgSettings {
        let allOps = Set(ApiOperation.allCases)
        let allPerms = Set(BranchPermission.allCases)
        let branches = [
            BranchProfile(
                id: "b1", name: "Bole Main", code: "BR-001", isMain: true, active: true, manager: "Abebe Kebede",
                address: PostalAddress(city: "Addis Ababa", subCity: "Bole", wereda: "03", kebele: "14", houseNumber: "412",
                                       locality: "Atlas", zone: "Zone 1", region: "Addis Ababa", country: "Ethiopia"),
                transactionTypes: [.b2c, .b2b, .b2g], documentTypes: [.inv, .cre, .deb, .rct, .wht], natures: [.goods, .service],
                operations: allOps, permissions: allPerms),
            BranchProfile(
                id: "b2", name: "Piassa", code: "BR-002", isMain: false, active: true, manager: "Hana Tesfaye",
                address: PostalAddress(city: "Addis Ababa", subCity: "Arada", wereda: "01", kebele: "06", houseNumber: "88",
                                       locality: "Piassa", zone: "Zone 1", region: "Addis Ababa", country: "Ethiopia"),
                transactionTypes: [.b2c, .b2b], documentTypes: [.inv, .cre, .rct], natures: [.goods],
                operations: [.singleRegister, .singleCancel, .receiptIssue],
                permissions: [.issueInvoices, .issueReceipts, .viewReports]),
            BranchProfile(
                id: "b3", name: "Hawassa", code: "BR-003", isMain: false, active: false, manager: "Dawit Mekonnen",
                address: PostalAddress(city: "Hawassa", subCity: "Tabor", wereda: "02", kebele: "09", houseNumber: "17",
                                       locality: "Piazza", zone: "Sidama", region: "Sidama", country: "Ethiopia"),
                transactionTypes: [.b2c], documentTypes: [.inv, .rct], natures: [.goods, .service],
                operations: [.singleRegister, .receiptIssue], permissions: [.issueInvoices, .issueReceipts]),
        ]
        let systems = [
            SourceSystemProfile(id: "s1", label: "Front Counter POS", type: .pos, branchId: "b1", active: true, verification: .verified,
                                verificationNote: "Verified 9 Oct 2026, 14:02 · HSN 8471", taxTypes: [.vat15, .vat0],
                                cashier: "Selam Girma", salesPerson: "Abebe Kebede", systemId: "SS-7F21-0001", lastDocument: "INV-2026-004812"),
            SourceSystemProfile(id: "s2", label: "Odoo ERP", type: .erp, branchId: "b1", active: true, verification: .verified,
                                verificationNote: "Verified 2 Sep 2026, 10:41 · HSN 3004", taxTypes: [.vat15, .vatex],
                                cashier: "", salesPerson: "Meron Alemu", systemId: "SS-7F21-0002", lastDocument: "INV-2026-001377"),
            SourceSystemProfile(id: "s3", label: "Piassa Till 2", type: .pos, branchId: "b2", active: true, verification: .failed,
                                verificationNote: "Failed 11 Oct 2026, 09:12 · HSN 8471 rejected by MoR", taxTypes: [.vat15],
                                cashier: "Hana Tesfaye", salesPerson: "", systemId: "SS-7F21-0003", lastDocument: "INV-2026-000214"),
            SourceSystemProfile(id: "s4", label: "Hawassa Manual Book", type: .manual, branchId: "b3", active: false, verification: .unverified,
                                verificationNote: "Not verified yet", taxTypes: [.vat0, .vat15],
                                cashier: "", salesPerson: "", systemId: "SS-7F21-0004", lastDocument: "—"),
        ]
        let keys = [
            ApiKey(id: "k1", label: "Front Counter · Till 1", sourceSystemId: "s1", active: true, created: "14 Aug 2026",
                   operations: [.singleRegister, .singleCancel, .receiptIssue], transactionTypes: [.b2c, .b2b],
                   taxTypes: [.vat15, .vat0], tail: "3f2a", users: ["Selam Girma", "Abebe Kebede"]),
            ApiKey(id: "k2", label: "Odoo nightly sync", sourceSystemId: "s2", active: true, created: "2 Sep 2026",
                   operations: [.bulkRegister, .bulkCancel, .withholdingIssue], transactionTypes: [.b2b, .b2g],
                   taxTypes: [.vat15, .vatex], tail: "91c0", users: ["Abebe Kebede"]),
            ApiKey(id: "k3", label: "Piassa Till 2", sourceSystemId: "s3", active: true, created: "20 Sep 2026",
                   operations: [.singleRegister, .receiptIssue], transactionTypes: [.b2c],
                   taxTypes: [.vat15], tail: "b7e4", users: ["Hana Tesfaye", "Meron Alemu"]),
            ApiKey(id: "k4", label: "Odoo test key", sourceSystemId: "s2", active: false, created: "3 Jun 2026",
                   operations: [.singleRegister], transactionTypes: [.b2b], taxTypes: [.vat15], tail: "0d18", users: []),
        ]
        let sessions = [
            DeviceSession(id: "x1", device: "EIRSPay iOS · iPhone 14", fingerprint: "fp_31d0…e5c2", location: "Addis Ababa", lastActive: "Active now", isCurrent: true),
            DeviceSession(id: "x2", device: "Chrome · macOS", fingerprint: "fp_9c1e…a07b", location: "Addis Ababa", lastActive: "2 hours ago", isCurrent: false),
            DeviceSession(id: "x3", device: "EIRSPay POS · Tablet", fingerprint: "fp_77ab…0f19", location: "Bole Main", lastActive: "Yesterday, 18:40", isCurrent: false),
            DeviceSession(id: "x4", device: "Firefox · Windows", fingerprint: "fp_c4e8…9d31", location: "Hawassa", lastActive: "28 Aug 2026", isCurrent: false),
        ]
        let activity = [
            SecurityEvent(id: "a1", kind: .signIn, title: "Signed in", detail: "EIRSPay iOS · Addis Ababa", when: "2 min ago"),
            SecurityEvent(id: "a2", kind: .failedAttempt, title: "Failed sign-in attempt", detail: "Wrong password · 196.188.4.x", when: "Yesterday, 22:14"),
            SecurityEvent(id: "a3", kind: .signIn, title: "Signed in", detail: "Chrome · macOS", when: "Yesterday, 08:30"),
            SecurityEvent(id: "a4", kind: .signOut, title: "Signed out", detail: "EIRSPay POS · Tablet", when: "9 Oct 2026"),
            SecurityEvent(id: "a5", kind: .passwordReset, title: "Password reset", detail: "Via email link", when: "30 Aug 2026"),
            SecurityEvent(id: "a6", kind: .failedAttempt, title: "3 failed attempts", detail: "Sign-in paused for 15 minutes", when: "27 Aug 2026"),
        ]
        return OrgSettings(
            staff: staff, branches: branches, sourceSystems: systems, apiKeys: keys, sessions: sessions, activity: activity,
            merchantExcise: false,
            credentials: MorCredentials(clientId: "8d7fa3ed-c649-4efb-ba9d-9792db3a3476", tin: "0045218796",
                                        clientSecret: "cs_4f9a1d7e22b0c8a5e61f3d90b7c2a4e8",
                                        apiKey: "ak_9be20c7714fa3d58a0e6c1b2f9d4e7a3", environment: .dev),
            seller: SellerProfile(tin: "0045218796", legalName: "Bole Market PLC", vatNumber: "4325666334325666",
                                  email: "finance@bolemarket.et", phone: "+251 911 234 567", region: "Addis Ababa",
                                  city: "Addis Ababa", wereda: "03", subCity: "Bole", zone: "Zone 1", kebele: "14",
                                  locality: "Atlas", houseNumber: "412", country: "ET"),
            setupVerified: false,
            passwordChangedNote: "Last changed 42 days ago")
    }
}
