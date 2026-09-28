"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import {
  MOBILE_PROVIDERS,
  PAYMENT_LABEL,
  PAYMENT_METHODS,
  RECEIPT_TYPES,
  RECEIPT_TYPE_LABEL,
  branchName,
  etb,
  fmt2,
  isConsumerSale,
  receiptHasItems,
  registerReceipt,
  totals,
  withheldAmount,
  type Invoice,
  type LineItem,
  type PaymentMethod,
  type ReceiptDraft,
} from "@/lib/merchant";
import { useConsole } from "../ConsoleProvider";
import { Icon } from "../Icon";
import rg from "../invoices/register.module.css";
import rc from "./receipts.module.css";
import rr from "./register-receipt.module.css";
import ui from "../ui.module.css";

const EMPTY: ReceiptDraft = {
  receiptType: "SALES",
  paymentMethod: "CASH",
  lines: [],
  linkedInvoiceId: null,
  amount: "",
  walkIn: true,
  name: "",
  phoneOrTin: "",
  reference: "",
  provider: "telebirr",
  tendered: "",
};

const REF_LABEL: Partial<Record<PaymentMethod, [string, string]>> = {
  CHEQUE: ["Cheque number *", "Bank and cheque no."],
  BANK_TRANSFER: ["Transfer reference *", "e.g. FT26267AB91C"],
  MOBILE_MONEY: ["Transaction ID *", "From the payment SMS"],
  CARD: ["Card (last 4 digits)", "•••• 1234"],
};

const REF_REQUIRED: Partial<Record<PaymentMethod, string>> = {
  CHEQUE: "cheque number",
  BANK_TRANSFER: "transfer reference",
  MOBILE_MONEY: "transaction ID",
};

const numeric = (s: string) => s.replace(/[^0-9.]/g, "");

export function RegisterReceipt() {
  const router = useRouter();
  const { branch, catalog, invoices, receipts, withholding, addReceipt, work } = useConsole();
  const [f, setF] = useState<ReceiptDraft>(EMPTY);
  const [iq, setIq] = useState("");
  const set = (patch: Partial<ReceiptDraft>) => setF((s) => ({ ...s, ...patch }));

  const withItems = receiptHasItems(f.receiptType);
  const refund = f.receiptType === "REFUND";
  const active = catalog.filter((c) => c.active);
  const byCode = new Map(catalog.map((c) => [c.code, c]));
  const needle = iq.trim().toLowerCase();

  const lineItems: LineItem[] = f.lines.flatMap((l) => {
    const c = byCode.get(l.code);
    return c ? [{ description: c.description, itemCode: c.code, nature: c.nature, unit: c.unit, quantity: l.quantity, unitPrice: c.unitPrice, taxCode: c.taxCode }] : [];
  });
  const t = totals({ items: lineItems });
  const grand = withItems ? t.grandTotal : parseFloat(f.amount) || 0;

  // Payments settle open business invoices; refunds can be against any live invoice.
  const linkable = invoices.filter(
    (v) =>
      v.docType === "INV" &&
      v.status !== "CANCELED" &&
      (f.receiptType !== "PAYMENT" || ((v.status === "ISSUED" || v.status === "WITHHELD") && !isConsumerSale(v.transactionType))),
  );
  const linked = f.linkedInvoiceId ? invoices.find((v) => v.id === f.linkedInvoiceId) : undefined;
  const whtFor = (v: Invoice) => withholding.find((w) => w.invoiceId === v.id);
  const defaultAmount = (v: Invoice) => {
    const w = f.receiptType === "PAYMENT" ? whtFor(v) : undefined;
    return totals(v).grandTotal - (w ? withheldAmount(w, v) : 0);
  };

  const tendered = parseFloat(f.tendered);
  const problems = [
    withItems && !f.lines.length && "add at least one item",
    !withItems && !linked && "pick the linked invoice",
    !withItems && !(grand > 0) && "enter the amount",
    f.paymentMethod === "CASH" && f.tendered && tendered < grand && "tendered is less than the total",
    REF_REQUIRED[f.paymentMethod] && !f.reference.trim() && `add the ${REF_REQUIRED[f.paymentMethod]}`,
    withItems && !f.walkIn && !f.name.trim() && "add customer name",
  ].filter(Boolean) as string[];
  const ok = problems.length === 0;

  // Documents register against the selected branch; linked receipts follow their invoice.
  const branchForDoc = !withItems && linked ? linked.branch : (branchName(branch) ?? "Main Branch");

  const roundUps = grand > 0 ? [...new Set([grand, Math.ceil(grand / 100) * 100, Math.ceil(grand / 500) * 500, Math.ceil(grand / 1000) * 1000])].slice(0, 4) : [];

  function addItem(code: string) {
    setF((s) => ({
      ...s,
      lines: s.lines.some((l) => l.code === code) ? s.lines.map((l) => (l.code === code ? { ...l, quantity: l.quantity + 1 } : l)) : [...s.lines, { code, quantity: 1 }],
    }));
  }

  function step(i: number, by: number) {
    setF((s) => ({
      ...s,
      lines: s.lines.flatMap((l, j) => (j !== i ? [l] : l.quantity + by > 0 ? [{ ...l, quantity: l.quantity + by }] : [])),
    }));
  }

  async function submit() {
    if (!ok) return;
    const nextNumber = String(Math.max(...receipts.map((r) => +r.number || 0)) + 1);
    const rec = await work("Registering receipt", "Waiting for MoR to sign it", () =>
      registerReceipt(f, { branch: branchForDoc, nextNumber, catalog, total: grand }),
    );
    addReceipt(rec);
    router.push(`/receipts/${rec.id}`);
  }

  const ref = REF_LABEL[f.paymentMethod];
  let n = 1;

  return (
    <main className={`${ui.page} ${rg.page}`}>
      <Link href="/receipts" className={ui.back}>
        <Icon name="arrowLeft" size={15} />
        Receipts
      </Link>
      <div className={ui.titleBlock}>
        <span className={ui.eyebrow}>NEW RECEIPT · {branchForDoc}</span>
        <h1 className={ui.title}>
          Register <i>receipt.</i>
        </h1>
        <p className={ui.sub}>Record money received or returned. MoR signs it and returns an IRN and QR.</p>
      </div>

      <div className={rg.grid}>
        <div className={rg.steps}>
          <section className={rg.section} style={{ gap: 16 }}>
            <div className={rg.sectionHead}>
              <span className={rg.stepNo}>{n++}</span>
              <span className={rg.sectionTitle}>Receipt type</span>
            </div>
            <div className={rr.typeCards}>
              {RECEIPT_TYPES.map((o) => (
                <button
                  key={o.code}
                  type="button"
                  className={rr.typeCard}
                  aria-pressed={f.receiptType === o.code}
                  onClick={() => set({ receiptType: o.code, linkedInvoiceId: null, amount: "" })}
                >
                  <b>{o.label}</b>
                  <span>{o.desc}</span>
                </button>
              ))}
            </div>
          </section>

          {withItems ? (
            <section className={rg.section} style={{ gap: 14 }}>
              <div className={rg.sectionHead}>
                <span className={rg.stepNo}>{n++}</span>
                <span className={rg.sectionTitle}>Items</span>
                <span className={rg.hintRight}>{active.length} active in Items</span>
              </div>
              <label className={rr.catSearch}>
                <Icon name="search" size={16} stroke="var(--muted)" />
                <input value={iq} onChange={(e) => setIq(e.target.value)} placeholder="Search catalog by name or code" aria-label="Search catalog" />
              </label>
              <div className={rr.catalog}>
                {active
                  .filter((c) => !needle || `${c.code} ${c.description}`.toLowerCase().includes(needle))
                  .slice(0, 9)
                  .map((c) => (
                    <button key={c.code} type="button" className={rr.catItem} data-in={f.lines.some((l) => l.code === c.code)} onClick={() => addItem(c.code)}>
                      <span className={rr.catDesc}>{c.description}</span>
                      <span className={rr.catMeta}>
                        <span>{fmt2(c.unitPrice)}</span>
                        <span>
                          {c.taxCode} · {c.unit}
                        </span>
                      </span>
                    </button>
                  ))}
              </div>
              <div className={rr.rule} />
              {t.lines.map((l, i) => (
                <div key={l.itemCode} className={rr.line}>
                  <span className={ui.stack2} style={{ minWidth: 0 }}>
                    <span className={rr.lineDesc}>{l.description}</span>
                    <span className={rr.lineMeta}>
                      {l.itemCode} · {l.taxCode} · {fmt2(l.unitPrice)}
                    </span>
                  </span>
                  <div className={rr.stepper}>
                    <button type="button" aria-label={`Less ${l.description}`} onClick={() => step(i, -1)}>
                      −
                    </button>
                    <span>{l.quantity}</span>
                    <button type="button" aria-label={`More ${l.description}`} onClick={() => step(i, 1)}>
                      +
                    </button>
                  </div>
                  <span className={rg.num}>{fmt2(l.total)}</span>
                  <button
                    type="button"
                    className={rg.remove}
                    aria-label={`Remove ${l.description}`}
                    onClick={() => setF((s) => ({ ...s, lines: s.lines.filter((_, j) => j !== i) }))}
                  >
                    <Icon name="x" size={14} />
                  </button>
                </div>
              ))}
              {f.lines.length === 0 && <div className={rr.emptyLines}>Pick items above to add them to this receipt.</div>}
            </section>
          ) : (
            <section className={rg.section} style={{ gap: 14 }}>
              <div className={rg.sectionHead}>
                <span className={rg.stepNo}>{n++}</span>
                <span className={rg.sectionTitle}>Linked invoice</span>
                <span className={rg.hintRight}>{f.receiptType === "PAYMENT" ? "Open B2B / B2G invoices" : "Invoice being refunded"}</span>
              </div>
              <div className={rr.linkOpts} role="radiogroup" aria-label="Linked invoice">
                {linkable.map((v) => (
                  <button
                    key={v.id}
                    type="button"
                    role="radio"
                    aria-checked={f.linkedInvoiceId === v.id}
                    className={rr.linkOpt}
                    onClick={() => set({ linkedInvoiceId: v.id, amount: defaultAmount(v).toFixed(2) })}
                  >
                    <span className={rr.radio} />
                    <b>INV-{v.number}</b>
                    <span className={ui.stack2} style={{ minWidth: 0 }}>
                      <span style={{ fontSize: 14, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{v.buyer.name}</span>
                      <span style={{ fontSize: 11, color: "var(--muted)" }}>
                        {v.transactionType} · {v.branch}
                      </span>
                    </span>
                    <span className={ui.pill} data-status={v.status} style={{ alignSelf: "center" }}>
                      {v.status}
                    </span>
                    <span className={rg.num}>{etb(totals(v).grandTotal)}</span>
                  </button>
                ))}
              </div>
              <div className={rr.amountRow}>
                <label className={rg.label}>
                  {refund ? "Refund amount *" : "Amount received *"}
                  <input
                    className={`${rg.input} ${rg.inputMono} ${rr.right}`}
                    inputMode="decimal"
                    placeholder="0.00"
                    value={f.amount}
                    onChange={(e) => set({ amount: numeric(e.target.value) })}
                  />
                </label>
                <span className={rr.amountHint}>
                  {!linked
                    ? "Pick an invoice to prefill the amount."
                    : f.receiptType === "PAYMENT"
                      ? whtFor(linked)
                        ? `Prefilled with the invoice total less the ${Math.round(whtFor(linked)!.rate * 100)}% the buyer withheld. Change it for a part payment.`
                        : "Prefilled with the invoice total. Change it for a part payment."
                      : "Prefilled with the full invoice total. Lower it for a partial refund."}
                </span>
              </div>
            </section>
          )}

          <section className={rg.section} style={{ gap: 14 }}>
            <div className={rg.sectionHead}>
              <span className={rg.stepNo}>{n++}</span>
              <span className={rg.sectionTitle}>Customer</span>
            </div>
            {withItems ? (
              <>
                <button type="button" role="switch" aria-checked={f.walkIn} className={rg.toggle} onClick={() => set({ walkIn: !f.walkIn })}>
                  <span className={rg.switch} />
                  <span className={rg.toggleText}>
                    <b>Walk-in customer</b>
                    <span>No customer details on the receipt</span>
                  </span>
                </button>
                {!f.walkIn && (
                  <div className={rr.two}>
                    <label className={rg.label}>
                      Name
                      <input className={rg.input} placeholder="Customer or company" value={f.name} onChange={(e) => set({ name: e.target.value })} />
                    </label>
                    <label className={rg.label}>
                      Phone or TIN
                      <input
                        className={`${rg.input} ${rg.inputMono}`}
                        placeholder="+251 9… or TIN"
                        value={f.phoneOrTin}
                        onChange={(e) => set({ phoneOrTin: e.target.value })}
                      />
                    </label>
                  </div>
                )}
              </>
            ) : linked ? (
              <div className={rr.fromInvoice}>
                <span>
                  <span>From INV-{linked.number}:</span> <b>{linked.buyer.name}</b>
                </span>
                <span>{linked.buyer.tin ? `TIN ${linked.buyer.tin}` : linked.buyer.idType ? `${linked.buyer.idType} ${linked.buyer.id}` : ""}</span>
              </div>
            ) : (
              <span className={rg.hint}>The customer comes from the linked invoice.</span>
            )}
          </section>

          <section className={rg.section} style={{ gap: 16 }}>
            <div className={rg.sectionHead}>
              <span className={rg.stepNo}>{n++}</span>
              <span className={rg.sectionTitle}>Payment method</span>
            </div>
            <div className={rr.pmCards}>
              {PAYMENT_METHODS.map((m) => (
                <button
                  key={m.code}
                  type="button"
                  className={rr.pmCard}
                  aria-pressed={f.paymentMethod === m.code}
                  onClick={() => set({ paymentMethod: m.code, reference: "" })}
                >
                  {m.label}
                </button>
              ))}
            </div>
            {f.paymentMethod === "CASH" && (
              <>
                <div className={rr.two} style={{ alignItems: "end" }}>
                  <label className={rg.label}>
                    Amount tendered
                    <input
                      className={`${rg.input} ${rg.inputMono} ${rr.right}`}
                      inputMode="decimal"
                      placeholder={fmt2(grand)}
                      value={f.tendered}
                      onChange={(e) => set({ tendered: numeric(e.target.value) })}
                    />
                  </label>
                  <div className={rr.change}>
                    <span>Change due</span>
                    <b>{f.tendered && tendered >= grand ? fmt2(tendered - grand) : "0.00"}</b>
                  </div>
                </div>
                {roundUps.length > 0 && (
                  <div className={rc.chips}>
                    {roundUps.map((v, i) => (
                      <button key={v} type="button" className={rr.tenderChip} onClick={() => set({ tendered: v.toFixed(2) })}>
                        {i === 0 ? `Exact ${fmt2(v)}` : fmt2(v)}
                      </button>
                    ))}
                  </div>
                )}
              </>
            )}
            {f.paymentMethod === "MOBILE_MONEY" && (
              <div className={rc.chips} role="group" aria-label="Provider">
                {MOBILE_PROVIDERS.map((p) => (
                  <button key={p} type="button" className={rc.chip} aria-pressed={f.provider === p} onClick={() => set({ provider: p })}>
                    {p}
                  </button>
                ))}
              </div>
            )}
            {ref && (
              <label className={rg.label}>
                {ref[0]}
                <input className={`${rg.input} ${rg.inputMono}`} placeholder={ref[1]} value={f.reference} onChange={(e) => set({ reference: e.target.value })} />
              </label>
            )}
          </section>
        </div>

        <div className={rg.previewCol}>
          <div className={rg.preview}>
            <div className={rg.previewHead}>
              <span className={rg.previewLabel}>
                PREVIEW · {RECEIPT_TYPE_LABEL[f.receiptType].toUpperCase()} · {PAYMENT_LABEL[f.paymentMethod].toUpperCase()}
              </span>
              <span className={rg.draft}>Draft</span>
            </div>
            <div className={rg.billTo}>
              <span>{refund ? "Refund to" : "Received from"}</span>
              <b>{withItems ? (f.walkIn ? "Walk-in customer" : f.name || "—") : (linked?.buyer.name ?? "—")}</b>
            </div>
            <div className={rg.previewLines}>
              {withItems
                ? t.lines.map((l) => (
                    <div key={l.itemCode} className={rg.pl}>
                      <span>
                        {l.quantity} × {l.description}
                      </span>
                      <span>{fmt2(l.preTax)}</span>
                    </div>
                  ))
                : linked && (
                    <div className={rg.pl}>
                      <span>
                        {refund ? "Refund on " : "Payment for "}INV-{linked.number}
                      </span>
                      <span>{fmt2(grand)}</span>
                    </div>
                  )}
            </div>
            <div className={rg.previewTax}>
              {withItems ? (
                <>
                  <div className={rg.pl}>
                    <span>Taxable</span>
                    <span>{fmt2(t.subtotal)}</span>
                  </div>
                  {t.taxes.map((x) => (
                    <div key={x.code} className={rg.pl}>
                      <span>{x.label}</span>
                      <span>{fmt2(x.tax)}</span>
                    </div>
                  ))}
                </>
              ) : (
                <div className={rg.pl}>
                  <span>Tax</span>
                  <span>On invoice</span>
                </div>
              )}
            </div>
            <div className={rg.previewGrand}>
              <b>{refund ? "Refund total" : "Total received"}</b>
              <span style={refund ? { color: "var(--danger)" } : undefined}>
                {refund ? "−" : ""}
                {etb(grand)}
              </span>
            </div>
            <div className={rg.ready} data-ready={ok}>
              <span className={rg.readyDot} />
              {ok ? "Ready to register. MoR returns an IRN and signed QR." : `To register: ${problems[0]}.`}
            </div>
            <button type="button" className={rg.submit} disabled={!ok} onClick={submit}>
              Register with MoR
              <Icon name="arrowRight" size={18} />
            </button>
          </div>
        </div>
      </div>
    </main>
  );
}
