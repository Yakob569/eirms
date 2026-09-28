"use client";

import Link from "next/link";
import { useState } from "react";
import { SELLER, amountInWords, etb, fmt2, irnFor, isConsumerSale, totals, type Invoice } from "@/lib/merchant";
import { useConsole } from "../ConsoleProvider";
import { Icon } from "../Icon";
import ui from "../ui.module.css";
import { MissingInvoice } from "./InvoiceDetail";
import r from "./receipt.module.css";
import { SignedQr, buyerRows, sellerRows } from "./shared";

// Amharic labels need a native-speaker check before this ships.

export function InvoiceReceipt({ id }: { id: string }) {
  const { getInvoice, ready } = useConsole();
  const inv = getInvoice(id);
  if (!inv) return <MissingInvoice ready={ready} />;
  return <Receipt inv={inv} />;
}

function Receipt({ inv }: { inv: Invoice }) {
  const [paper, setPaper] = useState<"a4" | "thermal">("a4");
  const t = totals(inv);
  const irn = irnFor(inv.id);
  const isCanceled = inv.status === "CANCELED";
  const title =
    inv.docType === "CRE" ? "Credit note · VAT" : isConsumerSale(inv.transactionType) ? "Cash sales invoice · VAT" : "Tax invoice · VAT";
  const sub = `የሽያጭ ደረሰኝ · ${inv.transactionType}${t.taxes.some((x) => x.code === "EXC10") ? " · EXCISE" : ""}`;

  const parties = [
    { role: "ሻጭ / SELLER (FROM)", name: SELLER.name, rows: sellerRows(SELLER) },
    { role: "ገዢ / BUYER (TO)", name: inv.buyer.name, rows: buyerRows(inv.buyer) },
  ];

  return (
    <main className={r.wrap}>
      <div className={r.toolbar}>
        <Link href={`/invoices/${inv.id}`} className={ui.back}>
          <Icon name="arrowLeft" size={15} />
          Back to invoice {inv.number}
        </Link>
        <div className={r.tools}>
          <div className={ui.seg} role="group" aria-label="Paper size">
            {(
              [
                ["a4", "A4"],
                ["thermal", "80mm"],
              ] as const
            ).map(([k, label]) => (
              <button key={k} type="button" className={`${ui.segOpt} ${r.segOpt}`} aria-pressed={paper === k} onClick={() => setPaper(k)}>
                {label}
              </button>
            ))}
          </div>
          <button type="button" className={ui.btn} onClick={() => window.print()}>
            <Icon name="printer" size={15} />
            Print
          </button>
          <button type="button" className={`${ui.primary} ${r.download}`} style={{ gap: 8, padding: "0 14px" }}>
            <Icon name="download" size={15} />
            Download PDF
          </button>
        </div>
      </div>

      <div className={r.paper} data-paper={paper}>
        {isCanceled && <div className={r.watermark}>CANCELED</div>}

        <div className={r.head}>
          <div className={r.seller}>
            <div className={r.sellerMark}>
              <Icon name="shield" size={20} />
            </div>
            <div className={r.sellerText}>
              <b>{SELLER.name}</b>
              <span className={r.muted}>
                TIN {SELLER.tin} · VAT {SELLER.vat}
              </span>
              <span className={r.muted}>{SELLER.addr} · +251 92 128 0809</span>
            </div>
          </div>
          <dl className={r.docMeta}>
            <dt>የሰነድ ቁጥር / Document No</dt>
            <dd>{inv.number}</dd>
            <dt>ቀን / Date</dt>
            <dd>{inv.date}</dd>
            <dt>ሰዓት / Time</dt>
            <dd>{inv.time}:00</dd>
          </dl>
        </div>

        <div className={r.titleBlock}>
          <span className={r.receiptTitle}>{title}</span>
          <span className={r.receiptSub}>{sub}</span>
        </div>

        <div className={r.qrBox}>
          <SignedQr irn={irn} size={92} padding={6} radius={6} />
          <div className={r.qrText}>
            <span className={r.muted}>IRN</span>
            <span className={r.irn}>{irn}</span>
            <span className={r.muted}>
              System No <b>{inv.sourceSystem}</b> · Verify at portal.mor.gov.et
            </span>
          </div>
        </div>

        <div className={r.parties}>
          {parties.map((p) => (
            <div key={p.role} className={r.party}>
              <span className={r.partyRole}>{p.role}</span>
              <b>{p.name}</b>
              {p.rows.map(([k, v]) => (
                <div key={k} className={r.partyRow}>
                  <span className={r.muted}>{k}</span>
                  <span>{v}</span>
                </div>
              ))}
            </div>
          ))}
        </div>

        <div className={r.table}>
          <div className={`${r.tcols} ${r.thead}`}>
            <span>
              ተ.ቁ
              <br />
              No.
            </span>
            <span>
              መግለጫ
              <br />
              Description
            </span>
            <span className={r.r}>
              ብዛት
              <br />
              Qty
            </span>
            <span className={r.r}>
              ዋጋ
              <br />
              Price
            </span>
            <span>
              ግብር
              <br />
              Tax
            </span>
            <span className={r.r}>
              ጠቅላላ
              <br />
              Total
            </span>
          </div>
          {t.lines.map((l) => (
            <div key={l.n} className={`${r.tcols} ${r.trow}`}>
              <span className={r.m}>{l.n}</span>
              <span>{l.description}</span>
              <span className={`${r.r} ${r.m}`}>{l.quantity}</span>
              <span className={`${r.r} ${r.m}`}>{fmt2(l.unitPrice)}</span>
              <span className={r.m} style={{ fontSize: 10 }}>
                {l.taxCode}
              </span>
              <span className={`${r.r} ${r.m}`}>{fmt2(l.total)}</span>
            </div>
          ))}
        </div>

        <div className={r.foot}>
          <div className={r.words}>
            <span className={r.muted}>ጠቅላላ ዋጋ በፊደል / Total in words</span>
            <span>{amountInWords(t.grandTotal)}</span>
          </div>
          <div className={r.sums}>
            <div>
              <span className={r.muted}>Taxable total</span>
              <span className={r.m}>{fmt2(t.subtotal)}</span>
            </div>
            {t.taxes.map((x) => (
              <div key={x.code}>
                <span className={r.muted}>{x.label}</span>
                <span className={r.m}>{fmt2(x.tax)}</span>
              </div>
            ))}
            <div className={r.sumTotal}>
              <b>ጠቅላላ / Total incl. tax</b>
              <b className={r.m}>{etb(t.grandTotal)}</b>
            </div>
          </div>
        </div>

        <div className={r.payment}>
          <span>
            <span className={r.muted}>የክፍያ ሁኔታ / Payment</span>{" "}
            <b>
              {inv.payment.mode} · {inv.payment.term}
            </b>
          </span>
          <span>
            <span className={r.muted}>Receiver</span> <b>{inv.buyer.name}</b>
          </span>
        </div>
      </div>
    </main>
  );
}
