package com.eirspay.domain.model

/** Seller or buyer on a fiscal document. Business parties carry a TIN; consumers an optional ID. */
data class Party(
    val name: String,
    val tin: String? = null,
    val vatNumber: String? = null,
    val idType: BuyerIdType? = null,
    val idNumber: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
) {
    val isBusiness: Boolean get() = tin != null

    /** Short identifier shown next to the name in lists ("TIN …", "NID …", phone, or walk-in). */
    val shortId: String
        get() = when {
            tin != null -> "TIN $tin"
            idType != null -> "${idType.name} ${idNumber.orEmpty()}".trim()
            phone != null -> phone
            else -> "Walk-in"
        }

    companion object {
        val WalkIn = Party(name = "Walk-in Customer")
    }
}
