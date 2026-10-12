package com.eirspay.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

fun money(value: Double): BigDecimal = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP)
fun money(value: Long): BigDecimal = BigDecimal.valueOf(value).setScale(2)
fun BigDecimal.cents(): BigDecimal = setScale(2, RoundingMode.HALF_UP)

val ZeroMoney: BigDecimal = BigDecimal.ZERO.setScale(2)

fun Iterable<BigDecimal>.sumMoney(): BigDecimal = fold(ZeroMoney) { acc, v -> acc + v }
