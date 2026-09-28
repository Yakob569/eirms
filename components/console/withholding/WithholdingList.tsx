"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import type { BranchKey } from "@/lib/merchant";
import { branchName, etb, fmtDay, totals, withheldAmount } from "@/lib/merchant";
import { useConsole } from "../ConsoleProvider";
import { Icon } from "../Icon";
import iv from "../invoices/invoices.module.css";
import ui from "../ui.module.css";
import wh from "./withholding.module.css";

const BRANCH_SEG: { key: BranchKey; label: string }[] = [
  { key: "all", label: "All branches" },
  { key: "main", label: "Main" },
  { key: "bole", label: "Bole" },
  { key: "piassa", label: "Piassa" },
];

export const certLabel = (received: boolean) => (received ? "Certificate received" : "Awaiting certificate");

export function WithholdingList() {
  const router = useRouter();
  const { withholding, getInvoice, branch, setBranch } = useConsole();
  const [cert, setCert] = useState<"all" | "no" | "yes">("all");

  const rows = withholding
    .flatMap((w) => {
      const inv = getInvoice(w.invoiceId);
      return inv ? [{ w, inv, t: totals(inv), amount: withheldAmount(w, inv) }] : [];
    })
    .filter(({ inv }) => branch === "all" || inv.branch === branchName(branch))
    .filter(({ w }) => cert === "all" || (cert === "yes") === w.certificateReceived);

  const total = rows.reduce((a, r) => a + r.amount, 0);
  const net = rows.reduce((a, r) => a + r.t.grandTotal - r.amount, 0);
  const stats = [
    { l: "Records", v: String(rows.length) },
    { l: "Total withheld", v: etb(total) },
    { l: "Awaiting certificate", v: String(rows.filter((r) => !r.w.certificateReceived).length), c: "#8A5A00" },
    { l: "Net paid to you", v: etb(net) },
  ];

  return (
    <main className={ui.page} style={{ gap: 20 }}>
      <div className={ui.titleBlock}>
        <span className={ui.eyebrow}>TAX WITHHELD · {branchName(branch) ?? "All branches"}</span>
        <h1 className={ui.title}>
          Withholding <i>records.</i>
        </h1>
        <p className={ui.sub} style={{ maxWidth: 640 }}>
          Invoices where a business or government buyer withheld part of the payment and remitted it to MoR for you. Collect the certificate to
          claim it against your tax.
        </p>
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
        <div className={ui.seg} role="group" aria-label="Branch">
          {BRANCH_SEG.map((b) => (
            <button key={b.key} type="button" className={`${ui.segOpt} ${iv.segSm}`} aria-pressed={branch === b.key} onClick={() => setBranch(b.key)}>
              {b.label}
            </button>
          ))}
        </div>
        <div className={ui.seg} role="group" aria-label="Certificate">
          {(
            [
              ["all", "All"],
              ["no", "Awaiting certificate"],
              ["yes", "Certificate received"],
            ] as const
          ).map(([k, label]) => (
            <button key={k} type="button" className={`${ui.segOpt} ${iv.segSm}`} aria-pressed={cert === k} onClick={() => setCert(k)}>
              {label}
            </button>
          ))}
        </div>
      </div>

      <div className={`${wh.cols} ${iv.colLabels}`}>
        <span>CERTIFICATE</span>
        <span>WITHHELD BY</span>
        <span>INVOICE</span>
        <span style={{ textAlign: "right" }}>TAXABLE BASE</span>
        <span style={{ textAlign: "right" }}>RATE</span>
        <span style={{ textAlign: "right" }}>WITHHELD</span>
        <span />
      </div>

      <div className={iv.rows}>
        {rows.map(({ w, inv, t, amount }) => {
          const href = `/withholding/${w.id}`;
          return (
            <div
              key={w.id}
              role="link"
              tabIndex={0}
              className={`${wh.cols} ${iv.row}`}
              onClick={() => router.push(href)}
              onKeyDown={(e) => e.key === "Enter" && router.push(href)}
            >
              <span className={iv.cell} style={{ gap: 6 }}>
                <b className={wh.certNo}>{w.id}</b>
                <span className={wh.cert} data-received={w.certificateReceived}>
                  {certLabel(w.certificateReceived)}
                </span>
              </span>
              <span className={iv.cell}>
                <span className={`${iv.party} ${iv.ellipsis}`}>{inv.buyer.name}</span>
                <span className={iv.partyId}>
                  <b>{inv.transactionType}</b> · TIN {inv.buyer.tin ?? "—"}
                </span>
              </span>
              <span>
                <Link
                  href={`/invoices/${inv.id}`}
                  className={wh.invLink}
                  onClick={(e) => e.stopPropagation()}
                  onKeyDown={(e) => e.stopPropagation()}
                >
                  INV-{inv.number} ↗
                </Link>
              </span>
              <span className={`${iv.cell} ${wh.base}`}>
                <b>{etb(t.subtotal)}</b>
                <span>{fmtDay(w.date)}</span>
              </span>
              <span className={wh.num}>{Math.round(w.rate * 100)}%</span>
              <span className={wh.withheld}>{etb(amount)}</span>
              <Icon name="chevronRight" size={15} stroke="var(--muted)" />
            </div>
          );
        })}
        {rows.length === 0 && <div className={ui.empty}>No withholding records for these filters.</div>}
      </div>
    </main>
  );
}
