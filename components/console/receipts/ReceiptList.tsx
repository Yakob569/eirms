"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import type { BranchKey, PaymentMethod, ReceiptType } from "@/lib/merchant";
import { PAYMENT_LABEL, PAYMENT_METHODS, RECEIPT_TYPES, RECEIPT_TYPE_LABEL, branchName, etb, fmt2, fmtIssued, receiptTotals } from "@/lib/merchant";
import { useConsole, type ConsoleReceipt } from "../ConsoleProvider";
import { Icon } from "../Icon";
import iv from "../invoices/invoices.module.css";
import { RowMenu } from "../RowMenu";
import ui from "../ui.module.css";
import rc from "./receipts.module.css";

const BRANCH_SEG: { key: BranchKey; label: string }[] = [
  { key: "all", label: "All branches" },
  { key: "main", label: "Main" },
  { key: "bole", label: "Bole" },
  { key: "piassa", label: "Piassa" },
];

/** "TIN 0005581342", "+251 9…", "NID …" or "Walk-in". */
export function receiptPartyId(b: ConsoleReceipt["buyer"]): string {
  if (b.tin) return `TIN ${b.tin}`;
  if (b.phone) return b.phone;
  if (b.idType) return `${b.idType} ${b.id}`;
  return "Walk-in";
}

/** Second line under the payment method: provider + ID, tendered cash, or the reference. */
export function paymentSub(r: ConsoleReceipt, total: number): string {
  if (r.paymentMethod === "MOBILE_MONEY") return `${r.provider} · ${r.reference}`;
  if (r.paymentMethod === "CASH") return `Tendered ${fmt2(r.tendered ?? total)}`;
  return r.reference ?? "—";
}

export const signed = (r: { receiptType: ReceiptType }, n: number) => (r.receiptType === "REFUND" ? "−" : "") + etb(n);

export function ReceiptList() {
  const router = useRouter();
  const { receipts, branch, setBranch, getInvoice, withholding } = useConsole();
  const [q, setQ] = useState("");
  const [type, setType] = useState<"all" | ReceiptType>("all");
  const [pm, setPm] = useState<"all" | PaymentMethod>("all");
  const [menuFor, setMenuFor] = useState<string | null>(null);

  const needle = q.trim().toLowerCase();
  const rows = receipts
    .filter((r) => branch === "all" || r.branch === branchName(branch))
    .map((r) => {
      const inv = r.linkedInvoiceId ? getInvoice(r.linkedInvoiceId) : undefined;
      return { r, inv, t: receiptTotals(r, getInvoice, withholding) };
    })
    .filter(
      ({ r, inv }) =>
        (type === "all" || r.receiptType === type) &&
        (pm === "all" || r.paymentMethod === pm) &&
        (!needle || [r.number, r.buyer.name, r.reference ?? "", inv?.number ?? ""].join(" ").toLowerCase().includes(needle)),
    );

  let collected = 0;
  let refunded = 0;
  for (const { r, t } of rows) {
    if (r.receiptType === "REFUND") refunded += t.grandTotal;
    else collected += t.grandTotal;
  }
  const stats = [
    { l: "Receipts", v: String(rows.length) },
    { l: "Collected", v: etb(collected) },
    { l: "Refunded", v: etb(refunded), c: "var(--danger)" },
    { l: "Linked to invoices", v: String(rows.filter((x) => x.r.linkedInvoiceId).length) },
  ];

  return (
    <main className={ui.page} style={{ gap: 20 }}>
      <div className={ui.pageHead} style={{ flexWrap: "nowrap" }}>
        <div className={ui.titleBlock}>
          <span className={ui.eyebrow}>BROWSE · {branchName(branch) ?? "All branches"}</span>
          <h1 className={ui.title}>
            Sales <i>receipts.</i>
          </h1>
          <p className={ui.sub} style={{ maxWidth: 640 }}>
            MoR-signed receipts for payments taken at the counter, against invoices, as advances or as refunds.
          </p>
        </div>
        <Link href="/receipts/new" className={ui.primary}>
          <Icon name="plus" size={16} strokeWidth={2.2} />
          Register receipt
        </Link>
      </div>

      <div className={iv.stats}>
        {stats.map((s) => (
          <div key={s.l} className={iv.stat}>
            <span>{s.l}</span>
            <b style={s.c ? { color: s.c } : undefined}>{s.v}</b>
          </div>
        ))}
      </div>

      <div className={iv.filters}>
        <label className={iv.search} style={{ flexBasis: 320 }}>
          <Icon name="search" size={16} stroke="var(--muted)" />
          <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Receipt no., customer, reference" aria-label="Filter receipts" />
        </label>
        <div className={ui.seg} role="group" aria-label="Branch">
          {BRANCH_SEG.map((b) => (
            <button key={b.key} type="button" className={`${ui.segOpt} ${iv.segSm}`} aria-pressed={branch === b.key} onClick={() => setBranch(b.key)}>
              {b.label}
            </button>
          ))}
        </div>
        <div className={ui.seg} role="group" aria-label="Receipt type">
          {[{ code: "all" as const, label: "All" }, ...RECEIPT_TYPES].map((t) => (
            <button key={t.code} type="button" className={`${ui.segOpt} ${iv.segSm}`} aria-pressed={type === t.code} onClick={() => setType(t.code)}>
              {t.label}
            </button>
          ))}
        </div>
      </div>
      <div className={rc.chips} role="group" aria-label="Payment method">
        {[{ code: "all" as const, label: "Any method" }, ...PAYMENT_METHODS].map((m) => (
          <button key={m.code} type="button" className={rc.chip} aria-pressed={pm === m.code} onClick={() => setPm(m.code)}>
            {m.label}
          </button>
        ))}
      </div>

      <div className={`${rc.cols} ${iv.colLabels}`}>
        <span>RECEIPT</span>
        <span>CUSTOMER</span>
        <span>PAYMENT</span>
        <span>INVOICE</span>
        <span>ISSUED</span>
        <span style={{ textAlign: "right" }}>AMOUNT</span>
        <span />
      </div>

      <div className={iv.rows}>
        {rows.map(({ r, inv, t }) => {
          const href = `/receipts/${r.id}`;
          return (
            <div
              key={r.id}
              role="link"
              tabIndex={0}
              className={`${rc.cols} ${iv.row}`}
              data-menu-open={menuFor === r.id}
              onClick={() => router.push(href)}
              onKeyDown={(e) => e.key === "Enter" && router.push(href)}
            >
              <span className={iv.cell} style={{ gap: 6 }}>
                <b className={rc.no}>RCP-{r.number}</b>
                <span className={rc.type} data-type={r.receiptType}>
                  {RECEIPT_TYPE_LABEL[r.receiptType]}
                </span>
              </span>
              <span className={iv.cell}>
                <span className={`${iv.party} ${iv.ellipsis}`}>{r.buyer.name}</span>
                <span className={`${iv.partyId} ${iv.ellipsis}`}>{receiptPartyId(r.buyer)}</span>
              </span>
              <span className={iv.cell}>
                <span className={rc.pm}>{PAYMENT_LABEL[r.paymentMethod]}</span>
                <span className={`${rc.pmSub} ${iv.ellipsis}`}>{paymentSub(r, t.grandTotal)}</span>
              </span>
              <span className={iv.cell}>
                {inv ? (
                  <Link
                    href={`/invoices/${inv.id}`}
                    className={rc.link}
                    onClick={(e) => e.stopPropagation()}
                    onKeyDown={(e) => e.stopPropagation()}
                  >
                    INV-{inv.number} ↗
                  </Link>
                ) : (
                  <span className={rc.none}>—</span>
                )}
              </span>
              <span className={iv.cell}>
                <span className={iv.when}>{fmtIssued(r.date, r.time)}</span>
                <span className={iv.whenSub}>{r.branch}</span>
              </span>
              <span className={`${iv.cell} ${iv.amount}`}>
                <b className={r.receiptType === "REFUND" ? rc.refund : undefined}>{signed(r, t.grandTotal)}</b>
                <span>{t.taxTotal ? `tax ${fmt2(t.taxTotal)}` : r.linkedInvoiceId ? "tax on invoice" : "no tax"}</span>
              </span>
              <RowMenu
                label={`RCP-${r.number}`}
                actions={[
                  { label: "View details", onSelect: () => router.push(href) },
                  // Only rows that already show a linked-invoice chip get this.
                  ...(inv ? [{ label: `View linked invoice · INV-${inv.number}`, onSelect: () => router.push(`/invoices/${inv.id}`) }] : []),
                ]}
                onOpenChange={(open) => setMenuFor((cur) => (open ? r.id : cur === r.id ? null : cur))}
              />
            </div>
          );
        })}
        {rows.length === 0 && <div className={ui.empty}>No receipts match these filters.</div>}
      </div>
    </main>
  );
}
