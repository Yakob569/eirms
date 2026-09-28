"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import {
  ID_TYPES,
  TAX_CODES,
  TAX_RATES,
  TRANSACTION_TYPES,
  branchName,
  etb,
  fmt2,
  isConsumerSale,
  lookupTin,
  registerInvoice,
  type CatalogItem,
  type DocType,
  type InvoiceDraft,
  type TaxCode,
} from "@/lib/merchant";
import { useConsole } from "../ConsoleProvider";
import { Icon } from "../Icon";
import ui from "../ui.module.css";
import rg from "./register.module.css";

const DOC_WORD: Record<DocType, string> = { INV: "invoice.", CRE: "credit note.", DEB: "debit note." };

const PAY_MODES = [
  ["CASH", "Cash"],
  ["BANK TRANSFER", "Bank transfer"],
  ["MOBILE MONEY", "Mobile money"],
  ["CREDIT", "Credit"],
] as const;

const TERMS = [
  ["IMMEDIATE", "Immediate"],
  ["NET 30", "Net 30"],
] as const;

type Line = InvoiceDraft["lines"][number];
const emptyLine = (): Line => ({ description: "", quantity: "1", unitPrice: "", taxCode: "VAT15" });

const EMPTY: InvoiceDraft = {
  docType: "INV",
  transactionType: "B2C",
  walkIn: true,
  idType: "NID",
  idNumber: "",
  phone: "",
  tin: "",
  name: "",
  vat: "",
  email: "",
  addr: "",
  previousIrn: "",
  amendReason: "",
  paymentMode: "CASH",
  paymentTerm: "IMMEDIATE",
  lines: [emptyLine()],
};

const numeric = (s: string) => s.replace(/[^0-9.]/g, "");

export function RegisterInvoice() {
  const router = useRouter();
  const { branch, invoices, catalog, addInvoice, work } = useConsole();
  // One tap adds an active catalog item as a priced line.
  const quickAdd = catalog.filter((c) => c.active).slice(0, 6);
  const [f, setF] = useState<InvoiceDraft>(EMPTY);
  const set = (patch: Partial<InvoiceDraft>) => setF((s) => ({ ...s, ...patch }));
  const setLine = (i: number, patch: Partial<Line>) => setF((s) => ({ ...s, lines: s.lines.map((l, j) => (j === i ? { ...l, ...patch } : l)) }));

  const consumer = isConsumerSale(f.transactionType);
  // New documents are registered against the selected branch (the default branch when viewing all).
  const branchForDoc = branchName(branch) ?? "Main Branch";

  let subtotal = 0;
  const byCode: Partial<Record<TaxCode, number>> = {};
  const lineTotals = f.lines.map((l) => {
    const pre = (parseFloat(l.quantity) || 0) * (parseFloat(l.unitPrice) || 0);
    const tax = pre * TAX_RATES[l.taxCode].rate;
    subtotal += pre;
    if (pre) byCode[l.taxCode] = (byCode[l.taxCode] ?? 0) + tax;
    return pre + tax;
  });
  const taxLines = (Object.entries(byCode) as [TaxCode, number][]).map(([k, v]) => ({ label: TAX_RATES[k].label, value: v }));
  const grand = subtotal + taxLines.reduce((a, t) => a + t.value, 0);

  const hasPricedLine = f.lines.some((l) => l.description && parseFloat(l.unitPrice) > 0);
  const valid = hasPricedLine && (consumer ? f.walkIn || !!f.name : !!f.name);

  function addFromCatalog(c: CatalogItem) {
    const line: Line = { description: c.description, quantity: "1", unitPrice: String(c.unitPrice), taxCode: c.taxCode };
    setF((s) => {
      const empty = s.lines.findIndex((l) => !l.description && !l.unitPrice);
      return { ...s, lines: empty >= 0 ? s.lines.map((l, j) => (j === empty ? line : l)) : [...s.lines, line] };
    });
  }

  async function lookup() {
    const found = await lookupTin(f.tin);
    set({ name: found.name, vat: found.vat ?? "", addr: found.addr ?? "" });
  }

  async function submit() {
    if (!valid) return;
    const nextNumber = String(Math.max(...invoices.map((v) => +v.number || 0)) + 1);
    const inv = await work("Registering with EIRS", "Waiting for MoR to sign the invoice", () =>
      registerInvoice(f, { branch: branchForDoc, nextNumber }),
    );
    addInvoice(inv);
    router.push(`/invoices/${inv.id}`);
  }

  return (
    <main className={`${ui.page} ${rg.page}`}>
      <Link href="/invoices" className={ui.back}>
        <Icon name="arrowLeft" size={15} />
        Invoices
      </Link>
      <div className={ui.titleBlock}>
        <span className={ui.eyebrow}>NEW DOCUMENT · {branchForDoc}</span>
        <h1 className={ui.title}>
          Register <i>{DOC_WORD[f.docType]}</i>
        </h1>
        <p className={ui.sub}>Submitted to MoR in real time. You get a signed IRN and QR back in seconds.</p>
      </div>

      <div className={rg.grid}>
        <div className={rg.steps}>
          {/* 1 · Document */}
          <section className={rg.section}>
            <div className={rg.sectionHead}>
              <span className={rg.stepNo}>1</span>
              <span className={rg.sectionTitle}>Document</span>
            </div>
            <div className={rg.field}>
              <span className={rg.fieldLabel}>Document type</span>
              <div className={rg.docSeg} role="group" aria-label="Document type">
                {(
                  [
                    ["INV", "Invoice"],
                    ["CRE", "Credit note"],
                    ["DEB", "Debit note"],
                  ] as const
                ).map(([k, label]) => (
                  <button key={k} type="button" aria-pressed={f.docType === k} onClick={() => set({ docType: k })}>
                    {label}
                  </button>
                ))}
              </div>
            </div>
            <div className={rg.field}>
              <span className={rg.fieldLabel}>Transaction type</span>
              <div className={rg.ttCards}>
                {TRANSACTION_TYPES.map((t) => (
                  <button key={t.code} type="button" className={rg.ttCard} aria-pressed={f.transactionType === t.code} onClick={() => set({ transactionType: t.code })}>
                    <b>{t.code}</b>
                    <span>{t.name}</span>
                  </button>
                ))}
              </div>
              <span className={rg.hint}>
                {consumer
                  ? "Consumer sale: buyer ID is optional for walk-in customers."
                  : "Business or government sale: buyer TIN and legal name are required by MoR."}
              </span>
            </div>
            {f.docType !== "INV" && (
              <div className={rg.amend}>
                <label className={rg.label}>
                  Previous IRN
                  <input
                    className={`${rg.input} ${rg.inputMono}`}
                    placeholder="Paste the IRN being amended"
                    value={f.previousIrn}
                    onChange={(e) => set({ previousIrn: e.target.value })}
                  />
                </label>
                <label className={rg.label}>
                  Reason
                  <input className={rg.input} placeholder="e.g. Returned goods" value={f.amendReason} onChange={(e) => set({ amendReason: e.target.value })} />
                </label>
              </div>
            )}
          </section>

          {/* 2 · Buyer */}
          <section className={rg.section}>
            <div className={rg.sectionHead}>
              <span className={rg.stepNo}>2</span>
              <span className={rg.sectionTitle}>Buyer</span>
              <span className={rg.mode}>{consumer ? "Consumer · light ID" : "Business · TIN required"}</span>
            </div>
            {consumer ? (
              <>
                <button type="button" role="switch" aria-checked={f.walkIn} className={rg.toggle} onClick={() => set({ walkIn: !f.walkIn })}>
                  <span className={rg.switch} />
                  <span className={rg.toggleText}>
                    <b>Walk-in customer</b>
                    <span>No buyer ID needed for consumer sales under the threshold</span>
                  </span>
                </button>
                {!f.walkIn && (
                  <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
                    <div className={rg.field}>
                      <span className={rg.fieldLabel}>ID type</span>
                      <div className={rg.opts}>
                        {ID_TYPES.map((t) => (
                          <button
                            key={t.code}
                            type="button"
                            title={t.name}
                            className={`${rg.opt} ${rg.optCode}`}
                            aria-pressed={f.idType === t.code}
                            onClick={() => set({ idType: t.code })}
                          >
                            {t.code}
                          </button>
                        ))}
                      </div>
                    </div>
                    <div className={rg.three}>
                      <label className={rg.label}>
                        Full name
                        <input className={rg.input} placeholder="Selam Bekele" value={f.name} onChange={(e) => set({ name: e.target.value })} />
                      </label>
                      <label className={rg.label}>
                        {f.idType} number
                        <input className={`${rg.input} ${rg.inputMono}`} placeholder="ID number" value={f.idNumber} onChange={(e) => set({ idNumber: e.target.value })} />
                      </label>
                      <label className={rg.label}>
                        Phone (optional)
                        <input className={`${rg.input} ${rg.inputMono}`} placeholder="+251 9…" value={f.phone} onChange={(e) => set({ phone: e.target.value })} />
                      </label>
                    </div>
                  </div>
                )}
              </>
            ) : (
              <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
                <div className={rg.tinRow}>
                  <label className={rg.label} style={{ flex: 1 }}>
                    Buyer TIN *
                    <input
                      className={`${rg.input} ${rg.inputMono}`}
                      style={{ fontSize: 14 }}
                      placeholder="10-digit TIN"
                      inputMode="numeric"
                      value={f.tin}
                      onChange={(e) => set({ tin: e.target.value.replace(/\D/g, "").slice(0, 10) })}
                    />
                  </label>
                  <button type="button" className={rg.lookup} onClick={lookup}>
                    <Icon name="search" size={15} />
                    Look up on MoR
                  </button>
                </div>
                <div className={rg.business}>
                  <label className={rg.label}>
                    Legal name *
                    <input className={rg.input} placeholder="Dashen Trading PLC" value={f.name} onChange={(e) => set({ name: e.target.value })} />
                  </label>
                  <label className={rg.label}>
                    VAT number
                    <input className={`${rg.input} ${rg.inputMono}`} placeholder="Optional" value={f.vat} onChange={(e) => set({ vat: e.target.value })} />
                  </label>
                  <label className={rg.label}>
                    Email
                    <input className={rg.input} type="email" placeholder="finance@company.et" value={f.email} onChange={(e) => set({ email: e.target.value })} />
                  </label>
                  <label className={rg.label}>
                    Region · Wereda
                    <input className={rg.input} placeholder="Addis Ababa · 08" value={f.addr} onChange={(e) => set({ addr: e.target.value })} />
                  </label>
                </div>
              </div>
            )}
          </section>

          {/* 3 · Line items */}
          <section className={rg.section} style={{ gap: 16 }}>
            <div className={rg.sectionHead}>
              <span className={rg.stepNo}>3</span>
              <span className={rg.sectionTitle}>Line items</span>
              <span className={rg.hintRight}>Quick add from Items</span>
            </div>
            <div className={rg.quick}>
              {quickAdd.map((c) => (
                <button key={c.code} type="button" className={rg.quickChip} onClick={() => addFromCatalog(c)}>
                  + {c.description} <span>{fmt2(c.unitPrice)}</span>
                </button>
              ))}
            </div>
            <div className={`${rg.lineCols} ${rg.lineLabels}`}>
              <span>DESCRIPTION</span>
              <span style={{ textAlign: "right" }}>QTY</span>
              <span style={{ textAlign: "right" }}>UNIT PRICE</span>
              <span>TAX CODE</span>
              <span style={{ textAlign: "right" }}>LINE TOTAL</span>
              <span />
            </div>
            {f.lines.map((l, i) => (
              <div key={i} className={rg.lineCols}>
                <input
                  className={`${rg.input} ${rg.lineInput}`}
                  placeholder="Item or service"
                  aria-label={`Line ${i + 1} description`}
                  value={l.description}
                  onChange={(e) => setLine(i, { description: e.target.value })}
                />
                <input
                  className={`${rg.input} ${rg.lineInput} ${rg.num}`}
                  inputMode="decimal"
                  aria-label={`Line ${i + 1} quantity`}
                  value={l.quantity}
                  onChange={(e) => setLine(i, { quantity: numeric(e.target.value) })}
                />
                <input
                  className={`${rg.input} ${rg.lineInput} ${rg.num}`}
                  inputMode="decimal"
                  aria-label={`Line ${i + 1} unit price`}
                  value={l.unitPrice}
                  onChange={(e) => setLine(i, { unitPrice: numeric(e.target.value) })}
                />
                <div className={rg.codes} role="group" aria-label={`Line ${i + 1} tax code`}>
                  {TAX_CODES.map((c) => (
                    <button key={c} type="button" aria-pressed={l.taxCode === c} onClick={() => setLine(i, { taxCode: c })}>
                      {c}
                    </button>
                  ))}
                </div>
                <span className={rg.num}>{fmt2(lineTotals[i])}</span>
                <button
                  type="button"
                  className={rg.remove}
                  aria-label={`Remove line ${i + 1}`}
                  onClick={() => setF((s) => ({ ...s, lines: s.lines.length > 1 ? s.lines.filter((_, j) => j !== i) : [emptyLine()] }))}
                >
                  <Icon name="x" size={14} />
                </button>
              </div>
            ))}
            <button type="button" className={rg.addLine} onClick={() => setF((s) => ({ ...s, lines: [...s.lines, emptyLine()] }))}>
              <Icon name="plus" size={15} strokeWidth={2.2} />
              Add line
            </button>
          </section>

          {/* 4 · Payment */}
          <section className={rg.section} style={{ gap: 16 }}>
            <div className={rg.sectionHead}>
              <span className={rg.stepNo}>4</span>
              <span className={rg.sectionTitle}>Payment</span>
            </div>
            <div className={rg.payment}>
              <div className={rg.field}>
                <span className={rg.fieldLabel}>Mode</span>
                <div className={rg.opts}>
                  {PAY_MODES.map(([k, label]) => (
                    <button key={k} type="button" className={rg.opt} aria-pressed={f.paymentMode === k} onClick={() => set({ paymentMode: k })}>
                      {label}
                    </button>
                  ))}
                </div>
              </div>
              <div className={rg.field}>
                <span className={rg.fieldLabel}>Term</span>
                <div className={rg.opts}>
                  {TERMS.map(([k, label]) => (
                    <button key={k} type="button" className={rg.opt} aria-pressed={f.paymentTerm === k} onClick={() => set({ paymentTerm: k })}>
                      {label}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          </section>
        </div>

        <div className={rg.previewCol}>
          <div className={rg.preview}>
            <div className={rg.previewHead}>
              <span className={rg.previewLabel}>
                PREVIEW · {f.docType} · {f.transactionType}
              </span>
              <span className={rg.draft}>Draft</span>
            </div>
            <div className={rg.billTo}>
              <span>Bill to</span>
              <b>{consumer && f.walkIn ? "Walk-in customer" : f.name || "—"}</b>
            </div>
            <div className={rg.previewLines}>
              {f.lines
                .filter((l) => l.description || l.unitPrice)
                .map((l, i) => (
                  <div key={i} className={rg.pl}>
                    <span>
                      {l.quantity || 0} × {l.description || "Untitled"}
                    </span>
                    <span>{fmt2((parseFloat(l.quantity) || 0) * (parseFloat(l.unitPrice) || 0))}</span>
                  </div>
                ))}
            </div>
            <div className={rg.previewTax}>
              <div className={rg.pl}>
                <span>Taxable</span>
                <span>{fmt2(subtotal)}</span>
              </div>
              {taxLines.map((t) => (
                <div key={t.label} className={rg.pl}>
                  <span>{t.label}</span>
                  <span>{fmt2(t.value)}</span>
                </div>
              ))}
            </div>
            <div className={rg.previewGrand}>
              <b>Grand total</b>
              <span>{etb(grand)}</span>
            </div>
            <div className={rg.ready} data-ready={valid}>
              <span className={rg.readyDot} />
              {valid
                ? "Ready to register. MoR returns an IRN and signed QR."
                : consumer
                  ? "Add at least one priced line item."
                  : "Add buyer legal name and one priced line item."}
            </div>
            <button type="button" className={rg.submit} disabled={!valid} onClick={submit}>
              Register with MoR
              <Icon name="arrowRight" size={18} />
            </button>
          </div>
        </div>
      </div>
    </main>
  );
}
