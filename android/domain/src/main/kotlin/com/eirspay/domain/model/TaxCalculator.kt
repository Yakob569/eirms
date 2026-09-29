package com.eirspay.domain.model

import java.math.BigDecimal

object TaxCalculator {
    fun calculate(items: List<LineItem>): DocumentTotals {
        val lines = items.mapIndexed { i, item ->
            val pre = (item.quantity * item.unitPrice).cents()
            val tax = (pre * BigDecimal.valueOf(item.taxCode.rate)).cents()
            LineTotals(number = i + 1, item = item, preTax = pre, tax = tax)
        }
        val taxes = lines.groupBy { it.item.taxCode }.map { (code, group) ->
            TaxLine(code, group.map { it.preTax }.sumMoney(), group.map { it.tax }.sumMoney())
        }
        return DocumentTotals(
            lines = lines,
            subtotal = lines.map { it.preTax }.sumMoney(),
            taxTotal = lines.map { it.tax }.sumMoney(),
            taxes = taxes,
        )
    }
}
