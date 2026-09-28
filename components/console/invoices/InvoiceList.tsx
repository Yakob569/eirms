"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import type { BranchKey, Status, TransactionType } from "@/lib/merchant";
import { branchName, etb, fmt2, fmtIssued, irnFor, shortIrn, totals } from "@/lib/merchant";
import { useConsole } from "../ConsoleProvider";
import { Icon } from "../Icon";
import ui from "../ui.module.css";
import iv from "./invoices.module.css";
import { partyIdLabel, useCopy } from "./shared";

const BRANCH_SEG: { key: BranchKey; label: string }[] = [
  { key: "all", label: "All branches" },
  { key: "main", label: "Main" },
  { key: "bole", label: "Bole" },
  { key: "piassa", label: "Piassa" },
];

const STATUS_SEG: { key: "all" | Exclude<Status, "CANCELED">; label: string }[] = [
  { key: "all", label: "All" },
  { key: "ISSUED", label: "Issued" },
  { key: "PAID", label: "Paid" },
  { key: "WITHHELD", label: "Withheld" },
];

const TT_SEG: ("all" | TransactionType)[] = ["all", "B2B", "B2C", "B2G", "G2B", "G2C"];

/** Registered invoices, or (mode="canceled") the ones voided with MoR, with the cancel reason up front. */
export function InvoiceList({ mode }: { mode: "active" | "canceled" }) {
  const router = useRouter();
  const { invoices, branch, setBranch } = useConsole();
  const [q, setQ] = useState("");
  const [status, setStatus] = useState<(typeof STATUS_SEG)[number]["key"]>("all");
  const [tt, setTt] = useState<(typeof TT_SEG)[number]>("all");
  const [copied, copy] = useCopy();
  const canceled = mode === "canceled";

  const scoped = invoices.filter((v) => branch === "all" || v.branch === branchName(branch));
  const pool = scoped.filter((v) => (canceled ? v.status === "CANCELED" : v.status !== "CANCELED"));
  const needle = q.trim().toLowerCase();
  const rows = pool
    .filter(
      (v) =>
        (canceled || status === "all" || v.status === status) &&
        (tt === "all" || v.transactionType === tt) &&
        (!needle || [v.number, v.buyer.name, v.buyer.tin ?? "", v.buyer.id ?? "", irnFor(v.id)].join(" ").toLowerCase().includes(needle)),
    )
    .map((v) => ({ v, t: totals(v), irn: irnFor(v.id) }));

  const gross = rows.reduce((a, r) => a + r.t.grandTotal, 0);
  const tax = rows.reduce((a, r) => a + r.t.taxTotal, 0);
  const stats = canceled
    ? [
        { l: "Canceled documents", v: String(rows.length), c: "var(--danger)" },
        { l: "Value voided", v: etb(gross) },
        { l: "Tax reversed", v: etb(tax) },
        { l: "Share of all invoices", v: `${Math.round((rows.length / Math.max(1, scoped.length)) * 100)}%` },
      ]
    : [
        { l: "Documents", v: String(rows.length) },
        { l: "Gross value", v: etb(gross) },
        { l: "Tax registered", v: etb(tax) },
        { l: "Withheld", v: String(rows.filter((r) => r.v.status === "WITHHELD").length), c: "#8A5A00" },
      ];

  return (
    <main className={ui.page} style={{ gap: 20 }}>
      <div className={ui.pageHead} style={{ flexWrap: "nowrap" }}>
        <div className={ui.titleBlock}>
          <span className={ui.eyebrow}>
            {canceled ? "CANCELED" : "BROWSE"} · {branchName(branch) ?? "All branches"}
          </span>
          <h1 className={ui.title}>
            {canceled ? "Canceled " : "Sales "}
            <i>invoices.</i>
          </h1>
          <p className={ui.sub} style={{ maxWidth: 620 }}>
            {canceled
              ? "Invoices voided with MoR. The original IRN stays on record with the reason and time of cancellation."
              : "MoR-signed invoices, credit and debit notes registered from your source systems."}
          </p>
        </div>
        <Link href="/invoices/new" className={ui.primary}>
          <Icon name="plus" size={16} strokeWidth={2.2} />
          Register invoice
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
        <label className={iv.search}>
          <Icon name="search" size={16} stroke="var(--muted)" />
          <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Filter by number, IRN, customer, TIN" aria-label="Filter invoices" />
        </label>
        <div className={ui.seg} role="group" aria-label="Branch">
          {BRANCH_SEG.map((b) => (
            <button key={b.key} type="button" className={`${ui.segOpt} ${iv.segSm}`} aria-pressed={branch === b.key} onClick={() => setBranch(b.key)}>
              {b.label}
            </button>
          ))}
        </div>
        {!canceled && (
          <div className={ui.seg} role="group" aria-label="Status">
            {STATUS_SEG.map((s) => (
              <button key={s.key} type="button" className={`${ui.segOpt} ${iv.segSm}`} aria-pressed={status === s.key} onClick={() => setStatus(s.key)}>
                {s.label}
              </button>
            ))}
          </div>
        )}
        <div className={iv.ttChips} role="group" aria-label="Transaction type">
          {TT_SEG.map((k) => (
            <button key={k} type="button" className={iv.ttChip} aria-pressed={tt === k} onClick={() => setTt(k)}>
              {k === "all" ? "Any type" : k}
            </button>
          ))}
        </div>
      </div>

      <div className={`${iv.cols} ${iv.colLabels}`} data-mode={mode}>
        <span>DOCUMENT</span>
        <span>CUSTOMER</span>
        <span>{canceled ? "CANCEL REASON" : "IRN"}</span>
        <span>{canceled ? "CANCELED" : "ISSUED"}</span>
        <span style={{ textAlign: "right" }}>{canceled ? "VALUE" : "TOTAL"}</span>
        <span />
      </div>

      <div className={iv.rows}>
        {rows.map(({ v, t, irn }) => {
          const href = `/invoices/${v.id}`;
          return (
            <div
              key={v.id}
              role="link"
              tabIndex={0}
              className={`${iv.cols} ${iv.row}`}
              data-mode={mode}
              onClick={() => router.push(href)}
              onKeyDown={(e) => e.key === "Enter" && router.push(href)}
            >
              <span className={iv.cell} style={{ gap: 6 }}>
                <span className={iv.docNo}>
                  <b>{v.number}</b>
                  <span className={iv.docType}>{v.docType}</span>
                </span>
                <span className={ui.pill} data-status={v.status}>
                  {v.status}
                </span>
              </span>
              <span className={iv.cell}>
                <span className={`${iv.party} ${iv.ellipsis}`}>{v.buyer.name}</span>
                <span className={`${iv.partyId} ${iv.ellipsis}`}>
                  <b>{v.transactionType}</b> · {partyIdLabel(v.buyer)}
                </span>
              </span>
              {canceled ? (
                <span className={iv.cell}>
                  <span className={iv.reason}>{v.cancel?.reason}</span>
                  <span className={iv.reasonCode}>
                    Code {v.cancel?.code} · was {v.originalStatus}
                  </span>
                </span>
              ) : (
                <span className={iv.irnCell}>
                  <span className={`${iv.irnShort} ${iv.ellipsis}`}>{shortIrn(irn)}</span>
                  <button
                    type="button"
                    className={iv.copy}
                    data-copied={copied === v.id}
                    title="Copy IRN"
                    onClick={(e) => {
                      e.stopPropagation();
                      copy(v.id, irn);
                    }}
                    onKeyDown={(e) => e.stopPropagation()}
                  >
                    <Icon name="copy" size={12} />
                    {copied === v.id ? "Copied" : "Copy"}
                  </button>
                </span>
              )}
              <span className={iv.cell}>
                <span className={iv.when}>{canceled ? v.cancel?.canceledAt : fmtIssued(v.date, v.time)}</span>
                <span className={iv.whenSub}>{canceled ? `Issued ${fmtIssued(v.date, v.time).split(",")[0]}` : v.branch}</span>
              </span>
              <span className={`${iv.cell} ${iv.amount}`}>
                <b className={canceled ? iv.struck : undefined}>{etb(t.grandTotal)}</b>
                <span>tax {fmt2(t.taxTotal)}</span>
              </span>
              <Icon name="chevronRight" size={15} stroke="var(--muted)" />
            </div>
          );
        })}
        {rows.length === 0 && <div className={ui.empty}>No documents match these filters.</div>}
      </div>
    </main>
  );
}
