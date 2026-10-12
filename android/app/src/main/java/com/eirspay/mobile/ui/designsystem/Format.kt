package com.eirspay.mobile.ui.designsystem

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Money and date formatting shared by every screen (en-US grouping, 2 decimals, "ETB " prefix). */
object Fmt {
    private val two = ThreadLocal.withInitial { DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US)) }
    private val dayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    private val time = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

    fun f2(v: BigDecimal): String = two.get()!!.format(v)
    fun f2(v: Double): String = two.get()!!.format(v)
    fun etb(v: BigDecimal): String = "ETB " + f2(v)
    fun etb(v: Double): String = "ETB " + f2(v)
    /** Whole-number amounts drop ".00" (tender chips). */
    fun compact(v: BigDecimal): String = f2(v).removeSuffix(".00")

    fun day(d: LocalDate): String = dayMonthYear.format(d)
    fun day(d: LocalDateTime): String = dayMonthYear.format(d)
    fun dateTime(d: LocalDateTime): String = dayMonthYear.format(d) + ", " + time.format(d)
    fun time(d: LocalDateTime): String = time.format(d)
    fun isoDate(d: LocalDateTime): String = d.toLocalDate().toString()

    /** "ETH-0f4a1b2c··9c21ab" — IRN shortened for lists. */
    fun irnShort(irn: String): String = "ETH-" + irn.take(8) + "··" + irn.takeLast(6)

    fun plural(n: Int, word: String): String = "$n $word" + if (n == 1) "" else "s"

    fun parseMoney(text: String): BigDecimal? = text.toBigDecimalOrNull()?.setScale(2, java.math.RoundingMode.HALF_UP)
}
