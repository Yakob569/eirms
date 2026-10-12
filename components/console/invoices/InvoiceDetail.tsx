"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { CANCEL_REASONS, DOC_NAMES, SELLER, cancelInvoice, etb, fmt2, fmtIssued, irnFor, totals, type Invoice } from "@/lib/merchant";
import { useConsole } from "../ConsoleProvider";
import { Icon } from "../Icon";
import ui from "../ui.module.css";
import iv from "./invoices.module.css";
import { SignedQr, buyerRows, listHrefFor, sellerRows, useCopy } from "./shared";

/** Shown while session state restores, or when the id doesn't exist. */
export function MissingInvoice({ ready }: { ready: boolean }) {
  if (!ready) return null;
  return (
    <main className={ui.page}>
      <Link href="/invoices" className={ui.back}>
        <Icon name="arrowLeft" size={15} />
        Invoices
      </Link>
      <div className={ui.empty}>This document could not be found.</div>
    </main>
  );
}

export function InvoiceDetail({ id, openCancel }: { id: string; openCancel?: boolean }) {
  const { getInvoice, ready } = useConsole();
  const inv = getInvoice(id);
  if (!inv) return <MissingInvoice ready={ready} />;
  return <Detail inv={inv} openCancel={openCancel} />;
}

function Detail({ inv, openCancel }: { inv: Invoice; openCancel?: boolean }) {
  const router = useRouter();
  const { getInvoice, markCanceled, work } = useConsole();
  const [copied, copy] = useCopy();
  const t = totals(inv);
  const irn = irnFor(inv.id);
  const isCanceled = inv.status === "CANCELED";
  const [cancelOpen, setCancelOpen] = useState(!!openCancel && !isCanceled);
  const backHref = listHrefFor(inv);
  const related = inv.amends ? getInvoice(inv.amends.previousId) : undefined;

  // Drop ?cancel=1 once the dialog is up, so a reload doesn't reopen it.
  useEffect(() => {
    if (openCancel) router.replace(`/invoices/${inv.id}`, { scroll: false });
  }, [openCancel, inv.id, router]);

  const side: { title: string; rows: [string, string][] }[] = [
    {
      title: "DOCUMENT",
      rows: [
        ["Document no.", inv.number],
        ["Counter", String(+inv.number + 1000)],
        ["Type", `${inv.docType} · ${DOC_NAMES[inv.docType]}`],
        ["Transaction", inv.transactionType],
        ["Currency", "ETB"],
        ["Status", inv.status],
      ],
    },
    ...(inv.amends
      ? [
          {
            title: related?.docType === "RTN" ? "AGAINST RETAINER" : "AMENDS",
            rows: [
              ["Reason", inv.amends.reason],
              ["Previous IRN", `ETH-${irnFor(inv.amends.previousId).slice(0, 10)}…`],
              ["Related doc", related ? `${related.docType}-${related.number}` : "—"],
            ] as [string, string][],
          },
        ]
      : []),
    {
      title: "SOURCE SYSTEM",
      rows: [
        ["Branch", inv.branch],
        ["System type", "POS"],
        ["System no.", inv.sourceSystem],
      ],
    },
    {
      title: "PAYMENT",
      rows: [
        ["Mode", inv.payment.mode],
        ["Term", inv.payment.term],
      ],
    },
  ];

  const parties = [
    { role: "SELLER", name: SELLER.name, rows: sellerRows(SELLER) },
    { role: `BUYER · ${inv.transactionType}`, name: inv.buyer.name, rows: buyerRows(inv.buyer) },
  ];

  async function confirmCancel(code: string) {
    setCancelOpen(false);
    const c = await work("Canceling with MoR", `Recording against IRN ETH-${irn.slice(0, 8)}`, () => cancelInvoice(inv.id, code));
    markCanceled(inv.id, c);
  }

  return (
    <main className={`${ui.page} ${iv.detail}`}>
      <Link href={backHref} className={ui.back}>
        <Icon name="arrowLeft" size={15} />
        {isCanceled ? "Canceled invoices" : "Invoices"}
      </Link>

      <div className={ui.pageHead} style={{ flexWrap: "nowrap" }}>
        <div className={ui.titleBlock} style={{ gap: 10 }}>
          <span className={ui.eyebrow}>
            {inv.docType} · {inv.transactionType} · {isCanceled ? "CANCELED" : "REGISTERED WITH EIRS"}
          </span>
          <div className={iv.detailTitle}>
            <h1 className={ui.title}>
              {DOC_NAMES[inv.docType]} <i>{inv.number}</i>
            </h1>
            <span className={ui.pill} data-status={inv.status} style={{ alignSelf: "center" }}>
              {inv.status}
            </span>
          </div>
          <span className={iv.meta}>
            Issued {fmtIssued(inv.date, inv.time)} · {inv.branch} · POS {inv.sourceSystem}
          </span>
        </div>
        <div className={iv.actions}>
          {!isCanceled && (
            <button type="button" className={ui.danger} onClick={() => setCancelOpen(true)}>
              <Icon name="ban" size={15} />
              Cancel invoice
            </button>
          )}
          <Link href={`/invoices/${inv.id}/receipt`} className={ui.primary} style={{ height: 42, gap: 8 }}>
            <Icon name="printer" size={15} />
            View receipt
          </Link>
        </div>
      </div>

      {isCanceled && inv.cancel && (
        <div className={iv.banner}>
          <span className={iv.bannerIcon}>
            <Icon name="ban" size={18} />
          </span>
          <span className={iv.bannerText}>
            <b>Canceled · {inv.cancel.reason}</b>
            <span>
              Reason code {inv.cancel.code} · canceled {inv.cancel.canceledAt} · original status {inv.originalStatus}. The IRN stays on record as
              proof of the cancellation.
            </span>
          </span>
          <span className={iv.bannerAmt}>{etb(t.grandTotal)}</span>
        </div>
      )}

      <div className={iv.detailGrid}>
        <div className={iv.detailMain}>
          <div className={iv.parties}>
            {parties.map((p) => (
              <div key={p.role} className={`${ui.card} ${iv.partyCard}`}>
                <span className={iv.label}>{p.role}</span>
                <span className={iv.partyName}>{p.name}</span>
                <dl className={iv.kv}>
                  {p.rows.map(([k, v]) => (
                    <div key={k}>
                      <dt>{k}</dt>
                      <dd>{v}</dd>
                    </div>
                  ))}
                </dl>
              </div>
            ))}
          </div>

          <div className={`${ui.card} ${iv.items}`}>
            <div className={`${ui.cardHead} ${iv.itemsHead}`}>
              <span className={ui.cardTitle}>Line items</span>
              <span className={ui.cardSub}>
                {t.lines.length} line{t.lines.length === 1 ? "" : "s"}
              </span>
            </div>
            <div className={`${iv.itemCols} ${iv.itemLabels}`}>
              <span>#</span>
              <span>DESCRIPTION</span>
              <span style={{ textAlign: "right" }}>QTY</span>
              <span style={{ textAlign: "right" }}>UNIT PRICE</span>
              <span>TAX</span>
              <span style={{ textAlign: "right" }}>TAX AMT</span>
              <span style={{ textAlign: "right" }}>LINE TOTAL</span>
            </div>
            {t.lines.map((l) => (
              <div key={l.n} className={`${iv.itemCols} ${iv.itemRow}`}>
                <span className={ui.mono} style={{ color: "var(--muted)" }}>
                  {l.n}
                </span>
                <span className={iv.cell}>
                  <span style={{ fontWeight: 500 }}>{l.description}</span>
                  <span className={`${iv.itemMeta} ${iv.ellipsis}`}>
                    {l.itemCode} · {l.nature}
                  </span>
                </span>
                <span className={iv.num}>
                  {l.quantity} <span className={iv.unit}>{l.unit}</span>
                </span>
                <span className={iv.num}>{fmt2(l.unitPrice)}</span>
                <span>
                  <span className={ui.code} style={{ fontSize: 10 }}>
                    {l.taxCode}
                  </span>
                </span>
                <span className={iv.num}>{fmt2(l.tax)}</span>
                <span className={iv.num} style={{ fontWeight: 500 }}>
                  {fmt2(l.total)}
                </span>
              </div>
            ))}
            <div className={iv.summary}>
              <div className={iv.taxLines}>
                <span className={iv.label}>TAX BREAKDOWN</span>
                {t.taxes.map((x) => (
                  <div key={x.code} className={iv.taxLine}>
                    <span>{x.code}</span>
                    <span style={{ whiteSpace: "nowrap" }}>{x.label}</span>
                    <span>on {fmt2(x.taxable)}</span>
                    <span>{fmt2(x.tax)}</span>
                  </div>
                ))}
              </div>
              <dl className={iv.sums} style={{ margin: 0 }}>
                <div>
                  <dt>Subtotal (taxable)</dt>
                  <dd>{fmt2(t.subtotal)}</dd>
                </div>
                <div>
                  <dt>Discount</dt>
                  <dd>0.00</dd>
                </div>
                <div style={{ borderBottom: "1px solid var(--line)" }}>
                  <dt>Tax total</dt>
                  <dd>{fmt2(t.taxTotal)}</dd>
                </div>
                <div className={iv.grand}>
                  <dt>Grand total</dt>
                  <dd style={isCanceled ? { textDecoration: "line-through" } : undefined}>{etb(t.grandTotal)}</dd>
                </div>
              </dl>
            </div>
          </div>
        </div>

        <div className={iv.aside}>
          <div className={iv.proof}>
            <div className={iv.proofHead}>
              <span className={iv.label}>PROOF OF REGISTRATION</span>
              <span className={iv.proofBadge} data-canceled={isCanceled}>
                {isCanceled ? "Canceled" : "Signed by MoR"}
              </span>
            </div>
            <div className={iv.proofScan}>
              <SignedQr irn={irn} size={112} padding={8} radius={10} />
              <span>
                Scan to verify at <b>portal.mor.gov.et</b>. Anyone can confirm this invoice is signed by MoR.
              </span>
            </div>
            <div className={iv.irnBlock}>
              <span>Invoice Registration Number (IRN)</span>
              <span className={iv.irnFull}>{irn}</span>
              <button type="button" className={iv.copyDark} onClick={() => copy("detail", irn)}>
                <Icon name="copy" size={13} />
                {copied === "detail" ? "Copied" : "Copy IRN"}
              </button>
            </div>
          </div>
          {side.map((sd) => (
            <div key={sd.title} className={`${ui.card} ${iv.sideCard}`}>
              <span className={iv.label}>{sd.title}</span>
              <dl className={iv.kv}>
                {sd.rows.map(([k, v]) => (
                  <div key={k}>
                    <dt>{k}</dt>
                    <dd>{v}</dd>
                  </div>
                ))}
              </dl>
            </div>
          ))}
        </div>
      </div>

      {cancelOpen && <CancelDialog number={inv.number} onClose={() => setCancelOpen(false)} onConfirm={confirmCancel} />}
    </main>
  );
}

function CancelDialog({ number, onClose, onConfirm }: { number: string; onClose: () => void; onConfirm: (code: string) => void }) {
  const [code, setCode] = useState("02");
  return (
    <>
      <div className={iv.dialogScrim} onClick={onClose} />
      <div className={iv.dialog} role="dialog" aria-modal="true" aria-labelledby="cancel-title">
        <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
          <span id="cancel-title" className={iv.dialogTitle}>
            Cancel invoice <i>{number}</i>
          </span>
          <span className={iv.dialogText}>
            MoR records the cancellation against the original IRN. This can&apos;t be undone. Issue a credit note instead if goods were returned.
          </span>
        </div>
        <div role="radiogroup" aria-label="Reason" style={{ display: "flex", flexDirection: "column", gap: 8 }}>
          <span style={{ fontSize: 13, fontWeight: 500 }}>Reason</span>
          {CANCEL_REASONS.map((r) => (
            <button key={r.code} type="button" role="radio" aria-checked={code === r.code} className={iv.reasonOpt} onClick={() => setCode(r.code)}>
              <code>{r.code}</code>
              <span>{r.label}</span>
              {code === r.code && <Icon name="check" size={15} stroke="var(--danger)" strokeWidth={2.6} />}
            </button>
          ))}
        </div>
        <div className={iv.dialogActions}>
          <button type="button" className={ui.btn} style={{ height: 42, padding: "0 16px" }} onClick={onClose}>
            Keep invoice
          </button>
          <button type="button" className={iv.confirmDanger} onClick={() => onConfirm(code)}>
            Cancel with MoR
          </button>
        </div>
      </div>
    </>
  );
}
