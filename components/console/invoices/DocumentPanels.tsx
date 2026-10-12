"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import {
  DOC_NAMES,
  SELLER,
  TAX_RATES,
  emptyDraftLine,
  etb,
  fmt2,
  fmtDay,
  registerDocument,
  totals,
  type DocType,
  type DocumentRequest,
  type DraftLine,
  type Invoice,
} from "@/lib/merchant";
import { useConsole } from "../ConsoleProvider";
import { Icon } from "../Icon";
import ui from "../ui.module.css";
import { LineItemsEditor, draftTotals } from "./LineItems";
import p from "./panel.module.css";
import { PAY_MODES, TERMS } from "./RegisterInvoice";
import rg from "./register.module.css";
import { partyIdLabel } from "./shared";

/** A document raised from an invoice row: credit / debit note from an INV, interim / final invoice from an RTN. */
export type PanelKind = Extract<DocType, "CRE" | "DEB" | "INT" | "FIN">;

const numeric = (s: string) => s.replace(/[^0-9.]/g, "");

/** Registers the new document, adds it to the console and opens its detail page. */
function useSubmitDocument() {
  const router = useRouter();
  const { invoices, addInvoice, work } = useConsole();
  return async (req: DocumentRequest, close: () => void) => {
    close();
    const nextNumber = String(Math.max(...invoices.map((v) => +v.number || 0)) + 1).padStart(3, "0");
    const doc = await work("Registering with EIRS", `Waiting for MoR to sign the ${DOC_NAMES[req.docType].toLowerCase()}`, () =>
      registerDocument(req, { nextNumber }),
    );
    addInvoice(doc);
    router.push(`/invoices/${doc.id}`);
  };
}

/** Full-height panel on the right with a scrim over the whole viewport. */
function SidePanel({
  eyebrow,
  title,
  srcNo,
  headExtra,
  onClose,
  footer,
  children,
}: {
  eyebrow: string;
  title: string;
  srcNo: string;
  headExtra?: React.ReactNode;
  onClose: () => void;
  footer: React.ReactNode;
  children: React.ReactNode;
}) {
  const [mounted, setMounted] = useState(false);
  useEffect(() => {
    setMounted(true);
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);
  if (!mounted) return null;

  return createPortal(
    <>
      <div className={p.scrim} onClick={onClose} />
      <aside className={p.panel} role="dialog" aria-modal="true" aria-label={`${title} ${srcNo}`}>
        <div className={p.head}>
          <div className={p.headText}>
            <span className={p.eyebrow}>{eyebrow}</span>
            <h2 className={p.title}>
              {title} <i>{srcNo}</i>
            </h2>
            {headExtra}
          </div>
          <button type="button" className={p.close} aria-label="Close" onClick={onClose}>
            <Icon name="x" size={17} />
          </button>
        </div>
        <div className={p.body}>{children}</div>
        <div className={p.foot}>{footer}</div>
      </aside>
    </>,
    document.body,
  );
}

function Footer({
  rows,
  totalLabel,
  total,
  credit,
  ready,
  hint,
  submitLabel,
  onSubmit,
}: {
  rows: [string, string][];
  totalLabel: string;
  total: string;
  credit?: boolean;
  ready: boolean;
  hint: string;
  submitLabel: string;
  onSubmit: () => void;
}) {
  return (
    <>
      <div className={p.sums}>
        {rows.map(([k, v]) => (
          <div key={k} className={p.sum}>
            <span>{k}</span>
            <span>{v}</span>
          </div>
        ))}
        <div className={p.total} data-credit={credit}>
          <b>{totalLabel}</b>
          <span>{total}</span>
        </div>
      </div>
      <div className={p.submitRow}>
        <span className={p.hint} data-ready={ready}>
          <span className={p.hintDot} />
          {hint}
        </span>
        <button type="button" className={ui.primary} disabled={!ready} style={ready ? undefined : { opacity: 0.45, cursor: "not-allowed" }} onClick={onSubmit}>
          {submitLabel}
          <Icon name="arrowRight" size={16} />
        </button>
      </div>
    </>
  );
}

// ─── Credit / debit note: a constrained subtraction from the original ─────

type Picked = Record<number, { q: string; p: string }>;

export function AmendPanel({ kind, source, onClose }: { kind: "CRE" | "DEB"; source: Invoice; onClose: () => void }) {
  const submitDoc = useSubmitDocument();
  const [sel, setSel] = useState<Picked>({});
  const [reason, setReason] = useState("");
  const credit = kind === "CRE";
  const name = DOC_NAMES[kind];
  const srcNo = `${source.docType}-${source.number}`;

  let sub = 0;
  let tax = 0;
  const lines = source.items.map((it, i) => {
    const s = sel[i];
    const rate = TAX_RATES[it.taxCode].rate;
    const q = s ? parseFloat(s.q) || 0 : 0;
    const pr = s ? parseFloat(s.p) || 0 : 0;
    if (s) {
      sub += q * pr;
      tax += q * pr * rate;
    }
    return { it, s, rate, newTotal: q * pr * (1 + rate) };
  });
  const picked = Object.keys(sel).length;
  const ready = picked > 0 && !!reason.trim() && lines.every((l) => !l.s || (parseFloat(l.s.q) > 0 && parseFloat(l.s.p) > 0));
  const hint = ready
    ? "Ready to register. MoR returns an IRN and signed QR."
    : picked === 0
      ? "Tick at least one line to adjust."
      : !reason.trim()
        ? "Add a reason."
        : "Each adjusted line needs a quantity and price.";

  /** Credit notes can't go above the original line. */
  const cap = (v: string, max: number) => {
    const clean = numeric(v);
    return credit && parseFloat(clean) > max ? String(max) : clean;
  };

  const summary: [string, string, string][] = [
    ["SELLER", SELLER.name, `TIN ${SELLER.tin}`],
    ["BUYER", source.buyer.name, partyIdLabel(source.buyer)],
    ["BRANCH", source.branch, `POS ${source.sourceSystem}`],
    ["ORIGINAL", etb(totals(source).grandTotal), `${source.transactionType} · ${fmtDay(source.date)}`],
  ];

  function submit() {
    if (!ready) return;
    submitDoc(
      {
        docType: kind,
        transactionType: source.transactionType,
        branch: source.branch,
        sourceSystem: source.sourceSystem,
        buyer: source.buyer,
        payment: source.payment,
        amends: { reason: reason.trim(), previousId: source.id },
        items: source.items.flatMap((it, i) => (sel[i] ? [{ ...it, quantity: parseFloat(sel[i].q), unitPrice: parseFloat(sel[i].p) }] : [])),
      },
      onClose,
    );
  }

  return (
    <SidePanel
      eyebrow={`${name.toUpperCase()} · AMENDS ${srcNo}`}
      title={`${name} for`}
      srcNo={srcNo}
      onClose={onClose}
      footer={
        <Footer
          rows={[
            ["Subtotal", etb(sub)],
            ["Tax", etb(tax)],
          ]}
          totalLabel="New total"
          total={`${credit ? "−" : "+"}${etb(sub + tax)}`}
          credit={credit}
          ready={ready}
          hint={hint}
          submitLabel={`Submit ${name.toLowerCase()}`}
          onSubmit={submit}
        />
      }
    >
      <div className={p.lockLine}>
        <Icon name="lock" size={14} />
        <span>Copied from {srcNo}. Buyer must match the original exactly.</span>
      </div>
      <div className={p.summary}>
        {summary.map(([k, a, b]) => (
          <div key={k}>
            <span className={p.summaryKey}>{k}</span>
            <b title={a}>{a}</b>
            <span className={p.summaryMeta}>{b}</span>
          </div>
        ))}
      </div>

      <div className={p.lines}>
        <div className={p.sectionHead}>
          <b>Lines to adjust</b>
          <span>
            {picked} of {source.items.length} lines adjusted
          </span>
        </div>
        {lines.map(({ it, s, rate, newTotal }, i) => (
          <div key={i} className={p.line} data-on={!!s}>
            <button
              type="button"
              className={p.lineToggle}
              role="checkbox"
              aria-checked={!!s}
              onClick={() => {
                const next = { ...sel };
                if (s) delete next[i];
                else next[i] = { q: String(it.quantity), p: String(it.unitPrice) };
                setSel(next);
              }}
            >
              <span className={p.box}>{s && <Icon name="check" size={12} strokeWidth={3.2} />}</span>
              <span className={p.lineText}>
                <b>{it.description}</b>
                <span className={p.lineMeta}>
                  {it.itemCode} · {it.taxCode} · orig. {it.quantity} {it.unit} × {fmt2(it.unitPrice)}
                </span>
              </span>
              <span className={p.lineOrig}>
                {etb(it.quantity * it.unitPrice * (1 + rate))}
                {!s && <em>Not adjusted</em>}
              </span>
            </button>
            {s && (
              <>
                <div className={p.adjust}>
                  <label>
                    New qty ({it.unit})
                    <input
                      className={p.numInput}
                      inputMode="decimal"
                      value={s.q}
                      onChange={(e) => setSel({ ...sel, [i]: { ...s, q: cap(e.target.value, it.quantity) } })}
                    />
                  </label>
                  <label>
                    New unit price
                    <input
                      className={p.numInput}
                      inputMode="decimal"
                      value={s.p}
                      onChange={(e) => setSel({ ...sel, [i]: { ...s, p: cap(e.target.value, it.unitPrice) } })}
                    />
                  </label>
                  <span className={p.adjustTotal}>
                    Line total incl. tax
                    <b>{etb(newTotal)}</b>
                  </span>
                </div>
                {credit && (
                  <span className={p.cap}>
                    Up to {it.quantity} {it.unit} at {fmt2(it.unitPrice)} — a credit note can&apos;t exceed the original line.
                  </span>
                )}
              </>
            )}
          </div>
        ))}
      </div>

      <label className={p.reason}>
        Reason *
        <textarea
          className={p.textarea}
          rows={3}
          placeholder={credit ? "e.g. 1 chair returned, damaged in delivery" : "e.g. Delivery charge not billed on the original"}
          value={reason}
          onChange={(e) => setReason(e.target.value)}
        />
      </label>
    </SidePanel>
  );
}

// ─── Interim / final invoice: the full invoice form, RTN ref + type locked ─

type Buyer = { tin: string; name: string; vat: string; phone: string; email: string; region: string; city: string; wereda: string };

const BUYER_FIELDS: [keyof Buyer, string, boolean?][] = [
  ["tin", "Buyer TIN", true],
  ["name", "Legal name *"],
  ["vat", "VAT number", true],
  ["phone", "Phone", true],
  ["email", "Email"],
  ["region", "Region"],
  ["city", "City"],
  ["wereda", "Wereda", true],
];

export function RetainerPanel({ kind, source, onClose }: { kind: "INT" | "FIN"; source: Invoice; onClose: () => void }) {
  const submitDoc = useSubmitDocument();
  const b = source.buyer;
  const [region, wereda] = (b.addr ?? "").split("·").map((x) => x.trim());
  const [buyer, setBuyer] = useState<Buyer>({
    tin: b.tin ?? "",
    name: b.name,
    vat: b.vat ?? "",
    phone: b.phone ?? "",
    email: b.email ?? "",
    region: region || "Addis Ababa",
    city: region || "Addis Ababa",
    wereda: (wereda ?? "").replace(/\D/g, ""),
  });
  const [lines, setLines] = useState<DraftLine[]>(() =>
    source.items.length
      ? source.items.map((it) => ({
          itemCode: it.itemCode,
          description: it.description,
          nature: it.nature,
          unit: it.unit,
          quantity: String(it.quantity),
          unitPrice: String(it.unitPrice),
          taxCode: it.taxCode,
        }))
      : [emptyDraftLine()],
  );
  const [mode, setMode] = useState(source.payment.mode);
  const [term, setTerm] = useState(source.payment.term);
  const name = DOC_NAMES[kind];
  const srcNo = `${source.docType}-${source.number}`;
  const { subtotal, taxLines, grand } = draftTotals(lines);
  const hasLine = lines.some((l) => l.description.trim() && parseFloat(l.unitPrice) > 0);
  const ready = !!buyer.name.trim() && hasLine;

  function submit() {
    if (!ready) return;
    const items = lines
      .filter((l) => l.description.trim() && parseFloat(l.unitPrice) > 0)
      .map((l) => ({
        description: l.description.trim(),
        itemCode: l.itemCode.trim() || "ITM-NEW",
        nature: l.nature,
        unit: l.unit || "PCS",
        quantity: parseFloat(l.quantity) || 1,
        unitPrice: parseFloat(l.unitPrice),
        taxCode: l.taxCode,
      }));
    submitDoc(
      {
        docType: kind,
        transactionType: source.transactionType,
        branch: source.branch,
        sourceSystem: source.sourceSystem,
        buyer: {
          name: buyer.name.trim(),
          tin: buyer.tin || undefined,
          vat: buyer.vat || undefined,
          phone: buyer.phone || undefined,
          email: buyer.email || undefined,
          addr: [buyer.city || buyer.region, buyer.wereda && `Wereda ${buyer.wereda}`].filter(Boolean).join(" · ") || undefined,
        },
        payment: { mode, term },
        amends: { reason: `${name} against retainer ${srcNo}`, previousId: source.id },
        items,
      },
      onClose,
    );
  }

  return (
    <SidePanel
      eyebrow={`${name.toUpperCase()} · DOCUMENT TYPE ${kind}`}
      title={`${name} for`}
      srcNo={srcNo}
      onClose={onClose}
      headExtra={
        <div className={p.lockedChips}>
          <span className={p.lockedChip}>
            <Icon name="lock" size={12} stroke="var(--muted)" strokeWidth={2.2} />
            Retainer {srcNo}
          </span>
          <span className={p.lockedChip}>
            <Icon name="lock" size={12} stroke="var(--muted)" strokeWidth={2.2} />
            Type {kind}
          </span>
          <span className={p.lockedNote}>Everything else is editable.</span>
        </div>
      }
      footer={
        <Footer
          rows={[["Subtotal", etb(subtotal)], ...taxLines.map((t) => [t.label, etb(t.value)] as [string, string])]}
          totalLabel="New total"
          total={etb(grand)}
          ready={ready}
          hint={ready ? "Ready to register. MoR returns an IRN and signed QR." : !buyer.name.trim() ? "Add the buyer's legal name." : "Add at least one priced line item."}
          submitLabel={`Submit ${name.toLowerCase()}`}
          onSubmit={submit}
        />
      }
    >
      <section className={p.card}>
        <div className={p.cardHead}>
          <b>Buyer details</b>
          <span>Pre-filled from {srcNo}</span>
        </div>
        <div className={p.buyerGrid}>
          {BUYER_FIELDS.map(([f, label, mono]) => (
            <label key={f} className={rg.label}>
              {label}
              <input
                className={`${rg.input} ${mono ? rg.inputMono : ""}`}
                value={buyer[f]}
                inputMode={f === "tin" ? "numeric" : undefined}
                onChange={(e) => setBuyer({ ...buyer, [f]: f === "tin" ? e.target.value.replace(/\D/g, "").slice(0, 10) : e.target.value })}
              />
            </label>
          ))}
        </div>
      </section>

      <section className={p.card} style={{ gap: 12 }}>
        <div className={p.cardHead}>
          <b>Items</b>
          <span>
            {lines.length} line{lines.length === 1 ? "" : "s"}
          </span>
        </div>
        <LineItemsEditor detailed lines={lines} onChange={setLines} />
      </section>

      <section className={p.card}>
        <div className={p.cardHead}>
          <b>Payment details</b>
        </div>
        <div className={rg.field}>
          <span className={rg.fieldLabel}>Mode</span>
          <div className={rg.opts}>
            {PAY_MODES.map(([k, label]) => (
              <button key={k} type="button" className={rg.opt} aria-pressed={mode === k} onClick={() => setMode(k)}>
                {label}
              </button>
            ))}
          </div>
        </div>
        <div className={rg.field}>
          <span className={rg.fieldLabel}>Term</span>
          <div className={rg.opts}>
            {TERMS.map(([k, label]) => (
              <button key={k} type="button" className={rg.opt} aria-pressed={term === k} onClick={() => setTerm(k)}>
                {label}
              </button>
            ))}
          </div>
        </div>
      </section>
    </SidePanel>
  );
}

/** Opens the right panel for a row action. */
export function DocumentPanel({ kind, source, onClose }: { kind: PanelKind; source: Invoice; onClose: () => void }) {
  return kind === "CRE" || kind === "DEB" ? (
    <AmendPanel kind={kind} source={source} onClose={onClose} />
  ) : (
    <RetainerPanel kind={kind} source={source} onClose={onClose} />
  );
}
