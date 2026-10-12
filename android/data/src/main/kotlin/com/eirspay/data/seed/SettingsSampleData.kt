package com.eirspay.data.seed

import com.eirspay.domain.model.*

/** Settings sample data mirroring the design prototype (Settings pages). Replace with API responses. */
internal object SettingsSampleData {
    private val ops = SettingsCatalog.operations
    private val perms = SettingsCatalog.permissions

    fun settings(): MerchantSettings = MerchantSettings(
        credentials = MorCredentials(
            clientId = "8d7fa3ed-c649-4efb-ba9d-9792db3a3476", tin = "0045218796",
            clientSecret = "cs_4f9a1d7e22b0c8a5e61f3d90b7c2a4e8", apiKey = "ak_9be20c7714fa3d58a0e6c1b2f9d4e7a3",
            environment = MorEnvironment.DEV,
        ),
        seller = SellerProfile(
            tin = "0045218796", legalName = "Bole Market PLC", vatNumber = "4325666334325666", email = "finance@bolemarket.et",
            phone = "+251 911 234 567", region = "Addis Ababa", city = "Addis Ababa", wereda = "03", subCity = "Bole", zone = "Zone 1",
            kebele = "14", locality = "Atlas", houseNumber = "412", country = "ET",
        ),
        staff = listOf("Abebe Kebede", "Hana Tesfaye", "Dawit Mekonnen", "Selam Girma", "Meron Alemu"),
        branches = listOf(
            MerchantBranch(
                id = "b1", name = "Bole Main", code = "BR-001", isMain = true, active = true, manager = "Abebe Kebede",
                address = BranchAddress("Addis Ababa", "Bole", "03", "14", "412", "Atlas", "Zone 1", "Addis Ababa", "Ethiopia"),
                transactionTypes = listOf("B2C", "B2B", "B2G"), documentTypes = listOf("INV", "CRE", "DEB", "RCT", "WHT"),
                natures = listOf("GOODS", "SERVICE"), operations = ops, permissions = perms,
            ),
            MerchantBranch(
                id = "b2", name = "Piassa", code = "BR-002", isMain = false, active = true, manager = "Hana Tesfaye",
                address = BranchAddress("Addis Ababa", "Arada", "01", "06", "88", "Piassa", "Zone 1", "Addis Ababa", "Ethiopia"),
                transactionTypes = listOf("B2C", "B2B"), documentTypes = listOf("INV", "CRE", "RCT"), natures = listOf("GOODS"),
                operations = listOf("Single Register", "Single Cancel", "Receipt Issue"),
                permissions = listOf("Issue invoices", "Issue receipts", "View reports"),
            ),
            MerchantBranch(
                id = "b3", name = "Hawassa", code = "BR-003", isMain = false, active = false, manager = "Dawit Mekonnen",
                address = BranchAddress("Hawassa", "Tabor", "02", "09", "17", "Piazza", "Sidama", "Sidama", "Ethiopia"),
                transactionTypes = listOf("B2C"), documentTypes = listOf("INV", "RCT"), natures = listOf("GOODS", "SERVICE"),
                operations = listOf("Single Register", "Receipt Issue"), permissions = listOf("Issue invoices", "Issue receipts"),
            ),
        ),
        sourceSystems = listOf(
            SourceSystemProfile("s1", "Front Counter POS", "POS", "b1", true, VerificationStatus.VERIFIED, "Verified 9 Oct 2026, 14:02 · HSN 8471",
                listOf("VAT15", "VAT0"), "Selam Girma", "Abebe Kebede", "SS-7F21-0001", "INV-2026-004812"),
            SourceSystemProfile("s2", "Odoo ERP", "ERP", "b1", true, VerificationStatus.VERIFIED, "Verified 2 Sep 2026, 10:41 · HSN 3004",
                listOf("VAT15", "VATEX"), "", "Meron Alemu", "SS-7F21-0002", "INV-2026-001377"),
            SourceSystemProfile("s3", "Piassa Till 2", "POS", "b2", true, VerificationStatus.FAILED, "Failed 11 Oct 2026, 09:12 · HSN 8471 rejected by MoR",
                listOf("VAT15"), "Hana Tesfaye", "", "SS-7F21-0003", "INV-2026-000214"),
            SourceSystemProfile("s4", "Hawassa Manual Book", "MANUAL", "b3", false, VerificationStatus.UNVERIFIED, "Not verified yet",
                listOf("VAT0", "VAT15"), "", "", "SS-7F21-0004", "—"),
        ),
        apiKeys = listOf(
            ApiKey("k1", "Front Counter · Till 1", "s1", true, "14 Aug 2026", listOf("Single Register", "Single Cancel", "Receipt Issue"),
                listOf("B2C", "B2B"), listOf("VAT15", "VAT0"), "3f2a", listOf("Selam Girma", "Abebe Kebede")),
            ApiKey("k2", "Odoo nightly sync", "s2", true, "2 Sep 2026", listOf("Bulk Register", "Bulk Cancel", "Withholding Issue"),
                listOf("B2B", "B2G"), listOf("VAT15", "VATEX"), "91c0", listOf("Abebe Kebede")),
            ApiKey("k3", "Piassa Till 2", "s3", true, "20 Sep 2026", listOf("Single Register", "Receipt Issue"),
                listOf("B2C"), listOf("VAT15"), "b7e4", listOf("Hana Tesfaye", "Meron Alemu")),
            ApiKey("k4", "Odoo test key", "s2", false, "3 Jun 2026", listOf("Single Register"), listOf("B2B"), listOf("VAT15"), "0d18", emptyList()),
        ),
        exciseEnabled = false,
        devices = listOf(
            SignedInDevice("x1", "EIRSPay iOS · iPhone 14", "fp_31d0…e5c2", "Addis Ababa", "Active now", current = true),
            SignedInDevice("x2", "Chrome · macOS", "fp_9c1e…a07b", "Addis Ababa", "2 hours ago", current = false),
            SignedInDevice("x3", "EIRSPay POS · Tablet", "fp_77ab…0f19", "Bole Main", "Yesterday, 18:40", current = false),
            SignedInDevice("x4", "Firefox · Windows", "fp_c4e8…9d31", "Hawassa", "28 Aug 2026", current = false),
        ),
        securityEvents = listOf(
            SecurityEvent(SecurityEventKind.SIGN_IN, "Signed in", "EIRSPay iOS · Addis Ababa", "2 min ago"),
            SecurityEvent(SecurityEventKind.FAILED, "Failed sign-in attempt", "Wrong password · 196.188.4.x", "Yesterday, 22:14"),
            SecurityEvent(SecurityEventKind.SIGN_IN, "Signed in", "Chrome · macOS", "Yesterday, 08:30"),
            SecurityEvent(SecurityEventKind.SIGN_OUT, "Signed out", "EIRSPay POS · Tablet", "9 Oct 2026"),
            SecurityEvent(SecurityEventKind.PASSWORD_RESET, "Password reset", "Via email link", "30 Aug 2026"),
            SecurityEvent(SecurityEventKind.FAILED, "3 failed attempts", "Sign-in paused for 15 minutes", "27 Aug 2026"),
        ),
        passwordNote = "Last changed 42 days ago",
        setupVerified = false,
    )
}
