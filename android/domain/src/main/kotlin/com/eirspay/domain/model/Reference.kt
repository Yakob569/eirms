package com.eirspay.domain.model

enum class Branch(val displayName: String) {
    MAIN("Main Branch"),
    BOLE("Bole"),
    PIASSA("Piassa"),
}

/** MoR transaction types. Consumer sales (B2C/G2C) need lighter buyer identification. */
enum class TransactionType(val displayName: String) {
    B2C("Business to consumer"),
    B2B("Business to business"),
    B2G("Business to government"),
    G2B("Government to business"),
    G2C("Government to consumer");

    val isConsumer: Boolean get() = this == B2C || this == G2C
}

enum class DocumentType(val displayName: String) {
    INV("Invoice"),
    CRE("Credit note"),
    DEB("Debit note");

    val isAmendment: Boolean get() = this != INV
}

enum class InvoiceStatus { ISSUED, PAID, WITHHELD, CANCELED }

enum class TaxCode(val rate: Double, val label: String, val description: String) {
    VAT15(0.15, "VAT 15%", "Standard VAT 15%"),
    VAT0(0.0, "VAT 0%", "Zero-rated"),
    EXC10(0.10, "Excise 10%", "Excise 10%"),
}

enum class NatureOfSupply(val displayName: String) { GOODS("Goods"), SERVICE("Service") }

/** Buyer identification accepted by MoR for consumer sales. */
enum class BuyerIdType(val displayName: String) {
    NID("National ID"),
    KID("Kebele ID"),
    SID("Student ID"),
    WID("Work ID"),
    PST("Passport"),
    DLS("Driving licence"),
    MRS("Military / residence"),
}

enum class InvoicePaymentMode(val displayName: String, val code: String) {
    CASH("Cash", "CASH"),
    BANK_TRANSFER("Bank transfer", "BANK TRANSFER"),
    MOBILE_MONEY("Mobile money", "MOBILE MONEY"),
    CREDIT("Credit", "CREDIT"),
}

enum class PaymentTerm(val displayName: String, val code: String) {
    IMMEDIATE("Immediate", "IMMEDIATE"),
    NET_30("Net 30", "NET 30"),
}

enum class CancelReason(val code: String, val label: String) {
    CUSTOMER_REQUEST("01", "Customer request"),
    WRONG_AMOUNT("02", "Wrong amount entered"),
    DUPLICATE("03", "Duplicate document"),
    WRONG_BUYER("04", "Wrong buyer details"),
}

val CatalogUnits = listOf("PCS", "KG", "L", "BOX", "CASE", "HRS", "JOB")
