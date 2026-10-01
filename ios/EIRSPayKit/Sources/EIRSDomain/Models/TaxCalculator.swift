import Foundation

public enum TaxCalculator {
    public static func calculate(_ items: [LineItem]) -> DocumentTotals {
        let lines = items.enumerated().map { index, item -> LineTotals in
            let pre = (item.quantity * item.unitPrice).rounded()
            let tax = (pre * item.taxCode.rate).rounded()
            return LineTotals(number: index + 1, item: item, preTax: pre, tax: tax)
        }
        // Keep tax codes in first-seen order, like the prototype.
        var order: [TaxCode] = []
        for line in lines where !order.contains(line.item.taxCode) { order.append(line.item.taxCode) }
        let taxes = order.map { code -> TaxLine in
            let group = lines.filter { $0.item.taxCode == code }
            return TaxLine(code: code, taxable: group.map(\.preTax).sumMoney(), tax: group.map(\.tax).sumMoney())
        }
        return DocumentTotals(lines: lines, subtotal: lines.map(\.preTax).sumMoney(),
                              taxTotal: lines.map(\.tax).sumMoney(), taxes: taxes)
    }
}
