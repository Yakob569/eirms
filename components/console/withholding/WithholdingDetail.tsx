"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { PAYMENT_LABEL, etb, fmtDay, fmtIssued, irnFor, markCertificateReceived, receiptTotals, totals, withheldAmount } from "@/lib/merchant";
import { useConsole } from "../ConsoleProvider";
import { Icon } from "../Icon";
import iv from "../invoices/invoices.module.css";
import ui from "../ui.module.css";
import { certLabel } from "./WithholdingList";
import wh from "./withholding.module.css";

export function WithholdingDetail({ id }: { id: string }) {
  const router = useRouter();
  const { withholding, getInvoice, receipts, markCertificate, ready } = useConsole();
  const w = withholding.find((x) => x.id === id);
  const inv = w && getInvoice(w.invoiceId);

  if (!w || !inv) {
    if (!ready) return null;
    return (
      <main className={ui.page}>
        <Link href="/withholding" className={ui.back}>
          <Icon name="arrowLeft" size={15} />
          Withholding
        </Link>
        <div className={ui.empty}>This withholding record could not be found.</div>
      </main>
    );
  }

  const t = totals(inv);
  const amount = withheldAmount(w, inv);
  const status = certLabel(w.certificateReceived);
  const payment = receipts.find((r) => r.linkedInvoiceId === inv.id && r.receiptType === "PAYMENT");
  const b = inv.buyer;
  const go = (href: string) => ({
    role: "link" as const,
    tabIndex: 0,
    onClick: () => router.push(href),
    onKeyDown: (e: React.KeyboardEvent) => e.key === "Enter" && router.push(href),
  });

  async function markReceived() {
    await markCertificateReceived(w!.id);
    markCertificate(w!.id);
  }

  const kv = (rows: [string, string][]) => (
    <dl className={iv.kv}>
      {rows.map(([k, v]) => (
        <div key={k}>
          <dt>{k}</dt>
          <dd>{v}</dd>
        </div>
      ))}
    </dl>
  );

  return (
    <main className={`${ui.page} ${iv.detail}`}>
      <Link href="/withholding" className={ui.back}>
        <Icon name="arrowLeft" size={15} />
        Withholding
      </Link>

      <div className={ui.titleBlock} style={{ gap: 10 }}>
        <span className={ui.eyebrow}>
          WHT · {inv.transactionType} · {status.toUpperCase()}
        </span>
        <div className={iv.detailTitle}>
          <h1 className={ui.title}>
            Certificate <i>{w.id}</i>
          </h1>
          <span className={wh.cert} data-received={w.certificateReceived} style={{ alignSelf: "center" }}>
            {status}
          </span>
        </div>
        <span className={iv.meta}>
          Withheld {fmtDay(w.date)} by {b.name}
        </span>
      </div>

      {!w.certificateReceived && (
        <div className={wh.awaiting}>
          <Icon name="fileAlert" size={20} />
          <span>
            No certificate yet. Ask {b.name} for the withholding certificate so you can claim {etb(amount)} against your tax.
          </span>
          <button type="button" className={wh.markBtn} onClick={markReceived}>
            Mark certificate received
          </button>
        </div>
      )}

      <div className={wh.grid}>
        <div className={iv.detailMain}>
          <div className={`${ui.card} ${wh.calc}`}>
            <span className={wh.label}>CALCULATION</span>
            <dl style={{ margin: 0 }}>
              <div className={wh.calcRow}>
                <dt>Taxable base (excl. VAT)</dt>
                <dd>{etb(t.subtotal)}</dd>
              </div>
              <div className={wh.calcRow}>
                <dt>× Withholding rate</dt>
                <dd>{Math.round(w.rate * 100)}%</dd>
              </div>
            </dl>
            <div className={wh.calcResult}>
              <b>Withheld and remitted to MoR</b>
              <span>{etb(amount)}</span>
            </div>
            <dl className={wh.net}>
              <div>
                <dt>Invoice grand total</dt>
                <dd>{etb(t.grandTotal)}</dd>
              </div>
              <div>
                <dt>− Withheld</dt>
                <dd>{etb(amount)}</dd>
              </div>
              <div>
                <dt>Net paid to you</dt>
                <dd>{etb(t.grandTotal - amount)}</dd>
              </div>
            </dl>
          </div>

          <div className={`${ui.card} ${wh.card} ${wh.related}`} {...go(`/invoices/${inv.id}`)}>
            <div className={ui.titleBlock} style={{ gap: 6 }}>
              <span className={wh.label}>RELATED INVOICE</span>
              <span className={wh.relatedTitle}>
                <span>INV-{inv.number}</span>
                <span className={ui.pill} data-status={inv.status}>
                  {inv.status}
                </span>
              </span>
              <span className={wh.relatedMeta}>
                {fmtIssued(inv.date, inv.time)} · {inv.transactionType} · {inv.items.length} line{inv.items.length === 1 ? "" : "s"}
              </span>
            </div>
            <span className={wh.open}>Open invoice →</span>
          </div>

          {payment && (
            <div className={`${ui.card} ${wh.card} ${wh.payment}`} {...go(`/receipts/${payment.id}`)}>
              <span>
                <span className={wh.mu}>Net payment received on</span> <b>RCP-{payment.number}</b>{" "}
                <span className={wh.mu}>· {PAYMENT_LABEL[payment.paymentMethod]}</span>
              </span>
              <span>{etb(receiptTotals(payment, getInvoice, withholding).grandTotal)} ↗</span>
            </div>
          )}
        </div>

        <div className={iv.aside}>
          <div className={`${ui.card} ${wh.agent}`}>
            <span className={wh.label}>WITHHOLDING AGENT</span>
            <span className={wh.agentName}>{b.name}</span>
            {kv([
              ["TIN", b.tin || "—"],
              ["VAT number", b.vat || "—"],
              ["Address", b.addr || "—"],
              ["Email", b.email || "—"],
            ])}
          </div>
          <div className={`${ui.card} ${iv.sideCard}`}>
            <span className={iv.label}>RECORD</span>
            {kv([
              ["Certificate no.", w.id],
              ["Withheld on", fmtDay(w.date)],
              ["Tax period", w.period],
              ["Related IRN", `ETH-${irnFor(inv.id).slice(0, 12)}…`],
              ["Status", status],
            ])}
          </div>
        </div>
      </div>
    </main>
  );
}
