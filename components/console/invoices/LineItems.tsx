"use client";

import { TAX_CODES, TAX_RATES, UNITS, emptyDraftLine, fmt2, type DraftLine, type TaxCode } from "@/lib/merchant";
import { Icon } from "../Icon";
import rg from "./register.module.css";

const numeric = (s: string) => s.replace(/[^0-9.]/g, "");

/** Line totals (incl. tax), taxable subtotal, tax per code and grand total for a draft. */
export function draftTotals(lines: DraftLine[]) {
  let subtotal = 0;
  const byCode: Partial<Record<TaxCode, number>> = {};
  const lineTotals = lines.map((l) => {
    const pre = (parseFloat(l.quantity) || 0) * (parseFloat(l.unitPrice) || 0);
    const tax = pre * TAX_RATES[l.taxCode].rate;
    subtotal += pre;
    if (pre) byCode[l.taxCode] = (byCode[l.taxCode] ?? 0) + tax;
    return pre + tax;
  });
  const taxLines = (Object.entries(byCode) as [TaxCode, number][]).map(([code, value]) => ({ code, label: TAX_RATES[code].label, value }));
  const grand = subtotal + taxLines.reduce((a, t) => a + t.value, 0);
  return { lineTotals, subtotal, taxLines, grand };
}

/**
 * The free-form item builder shared by Register invoice and the interim / final
 * invoice panel: add / remove rows, quantity, unit price and tax code. `detailed`
 * also shows item code, nature of supplies and unit (two-row layout for narrow
 * containers).
 */
export function LineItemsEditor({ lines, onChange, detailed }: { lines: DraftLine[]; onChange: (lines: DraftLine[]) => void; detailed?: boolean }) {
  const { lineTotals } = draftTotals(lines);
  const setLine = (i: number, patch: Partial<DraftLine>) => onChange(lines.map((l, j) => (j === i ? { ...l, ...patch } : l)));
  const remove = (i: number) => onChange(lines.length > 1 ? lines.filter((_, j) => j !== i) : [emptyDraftLine()]);

  const qty = (l: DraftLine, i: number) => (
    <input
      className={`${rg.input} ${rg.lineInput} ${rg.num}`}
      inputMode="decimal"
      placeholder="Qty"
      aria-label={`Line ${i + 1} quantity`}
      value={l.quantity}
      onChange={(e) => setLine(i, { quantity: numeric(e.target.value) })}
    />
  );
  const price = (l: DraftLine, i: number) => (
    <input
      className={`${rg.input} ${rg.lineInput} ${rg.num}`}
      inputMode="decimal"
      placeholder="Unit price"
      aria-label={`Line ${i + 1} unit price`}
      value={l.unitPrice}
      onChange={(e) => setLine(i, { unitPrice: numeric(e.target.value) })}
    />
  );
  const codes = (l: DraftLine, i: number) => (
    <div className={rg.codes} role="group" aria-label={`Line ${i + 1} tax code`}>
      {TAX_CODES.map((c) => (
        <button key={c} type="button" aria-pressed={l.taxCode === c} onClick={() => setLine(i, { taxCode: c })}>
          {c}
        </button>
      ))}
    </div>
  );
  const description = (l: DraftLine, i: number) => (
    <input
      className={`${rg.input} ${rg.lineInput}`}
      placeholder={detailed ? "Description" : "Item or service"}
      aria-label={`Line ${i + 1} description`}
      value={l.description}
      onChange={(e) => setLine(i, { description: e.target.value })}
    />
  );
  const removeBtn = (i: number) => (
    <button type="button" className={rg.remove} aria-label={`Remove line ${i + 1}`} onClick={() => remove(i)}>
      <Icon name="x" size={14} />
    </button>
  );

  return (
    <>
      {!detailed && (
        <div className={`${rg.lineCols} ${rg.lineLabels}`}>
          <span>DESCRIPTION</span>
          <span style={{ textAlign: "right" }}>QTY</span>
          <span style={{ textAlign: "right" }}>UNIT PRICE</span>
          <span>TAX CODE</span>
          <span style={{ textAlign: "right" }}>LINE TOTAL</span>
          <span />
        </div>
      )}
      {lines.map((l, i) =>
        detailed ? (
          <div key={i} className={rg.lineCard}>
            <div className={rg.lineTop}>
              <span className={rg.lineNo}>{i + 1}</span>
              <input
                className={`${rg.input} ${rg.lineInput} ${rg.inputMono}`}
                placeholder="Item code"
                aria-label={`Line ${i + 1} item code`}
                value={l.itemCode}
                onChange={(e) => setLine(i, { itemCode: e.target.value })}
              />
              {description(l, i)}
              <span className={rg.num}>{fmt2(lineTotals[i])}</span>
              {removeBtn(i)}
            </div>
            <div className={rg.lineBottom}>
              <select
                className={`${rg.input} ${rg.lineInput}`}
                aria-label={`Line ${i + 1} nature of supplies`}
                value={l.nature}
                onChange={(e) => setLine(i, { nature: e.target.value as DraftLine["nature"] })}
              >
                <option value="Goods">Goods</option>
                <option value="Service">Service</option>
              </select>
              <select
                className={`${rg.input} ${rg.lineInput} ${rg.inputMono}`}
                aria-label={`Line ${i + 1} unit`}
                value={l.unit}
                onChange={(e) => setLine(i, { unit: e.target.value })}
              >
                {(UNITS.includes(l.unit) ? UNITS : [l.unit, ...UNITS]).map((u) => (
                  <option key={u} value={u}>
                    {u}
                  </option>
                ))}
              </select>
              {qty(l, i)}
              {price(l, i)}
              {codes(l, i)}
            </div>
          </div>
        ) : (
          <div key={i} className={rg.lineCols}>
            {description(l, i)}
            {qty(l, i)}
            {price(l, i)}
            {codes(l, i)}
            <span className={rg.num}>{fmt2(lineTotals[i])}</span>
            {removeBtn(i)}
          </div>
        ),
      )}
      <button type="button" className={rg.addLine} onClick={() => onChange([...lines, emptyDraftLine()])}>
        <Icon name="plus" size={15} strokeWidth={2.2} />
        {detailed ? "Add item" : "Add line"}
      </button>
    </>
  );
}
