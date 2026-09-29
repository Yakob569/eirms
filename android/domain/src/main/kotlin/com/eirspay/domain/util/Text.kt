package com.eirspay.domain.util

import java.math.BigDecimal
import java.math.RoundingMode

object Masking {
    fun email(email: String): String {
        val at = email.indexOf('@')
        if (at <= 0) return email
        return email.take(minOf(2, at)) + "••••" + email.substring(at)
    }

    /** "+251 92 ••• 2273" style mask for Ethiopian mobile numbers. */
    fun phone(phone: String): String {
        val digits = phone.filter { it.isDigit() }.removePrefix("251")
        if (digits.length < 6) return phone
        return "+251 ${digits.take(2)} ••• ${digits.takeLast(4)}"
    }
}

/** English amount in words for fiscal receipts: "Twelve thousand ... birr and fifty cents". */
object AmountInWords {
    private val ones = listOf("", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
        "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen")
    private val tens = listOf("", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")

    private fun hundreds(value: Int): String {
        var x = value
        val sb = StringBuilder()
        if (x >= 100) {
            sb.append(ones[x / 100]).append(" hundred"); x %= 100
            if (x > 0) sb.append(' ')
        }
        if (x >= 20) {
            sb.append(tens[x / 10]); if (x % 10 > 0) sb.append('-').append(ones[x % 10])
        } else if (x > 0) sb.append(ones[x])
        return sb.toString()
    }

    private fun words(value: Long): String {
        if (value == 0L) return "zero"
        var x = value
        val parts = mutableListOf<String>()
        if (x >= 1_000_000) { parts += hundreds((x / 1_000_000).toInt()) + " million"; x %= 1_000_000 }
        if (x >= 1000) { parts += hundreds((x / 1000).toInt()) + " thousand"; x %= 1000 }
        if (x > 0) parts += hundreds(x.toInt())
        return parts.joinToString(" ")
    }

    fun birr(amount: BigDecimal): String {
        val scaled = amount.setScale(2, RoundingMode.HALF_UP)
        val whole = scaled.toLong()
        val cents = scaled.subtract(BigDecimal.valueOf(whole)).movePointRight(2).toInt()
        val s = words(whole) + " birr" + if (cents > 0) " and ${words(cents.toLong())} cents" else ""
        return s.replaceFirstChar { it.uppercase() }
    }
}
