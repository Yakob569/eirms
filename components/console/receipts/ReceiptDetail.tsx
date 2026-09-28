"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { PAYMENT_LABEL, RECEIPT_TYPE_LABEL, etb, fmt2, fmtIssued, irnFor, receiptTotals, totals } from "@/lib/merchant";
import { useConsole, type ConsoleReceipt } from "../ConsoleProvider";
import { Icon } from "../Icon";
import iv from "../invoices/invoices.module.css";
import { SignedQr, useCopy } from "../invoices/shared";
import ui from "../ui.module.css";
import { signed } from "./ReceiptList";
import rc from "./receipts.module.css";

export function ReceiptDetail({ id }: { id: string }) {
  const { getReceipt, ready } = useConsole();
  const r = getReceipt(id);
  if (!r) {
    if (!ready) return null;
    return (
      <main className={ui.page}>
        <Link href="/receipts" className={ui.back}>
          <Icon name="arrowLeft" size={15} />
          Receipts
        </Link>
        <div className={ui.empty}>This receipt could not be found.</div>
      </main>
    );
  }
  return <Detail r={r} />;
}

function Kv({ rows, className }: { rows: [string, string][]; className?: string }) {
  return (
    <dl className={`${iv.kv} ${className ?? ""}`}>
      {rows.map(([k, v]) => (
        <div key={k}>
          <dt>{k}</dt>
          <dd>{v}</dd>
        </div>
      ))}
    </dl>
  );
}

function Detail({ r }: { r: ConsoleReceipt }) {
  const router = useRouter();
  const { getInvoice, withholding } = useConsole();
  const [copied, copy] = useCopy();
  const t = receiptTotals(r, getInvoice, withholding);
  const inv = t.invoice;
  const irn = irnFor(r.id);
  const refund = r.receiptType === "REFUND";
  const typeLabel = RECEIPT_TYPE_LABEL[r.receiptType];

  const change = r.paymentMethod === "CASH" && r.tendered ? r.tendered - t.grandTotal : 0;
  const payRows: [string, string][] = {
    CASH: [
      ["Method", "Cash"],
      ["Tendered", fmt2(r.tendered ?? t.grandTotal)],
      ["Change", fmt2(Math.max(0, change))],
    ] as [string, string][],
    MOBILE_MONEY: [
      ["Method", `Mobile money · ${r.provider}`],
      ["Transaction ID", r.reference ?? "—"],
    ] as [string, string][],
    BANK_TRANSFER: [
      ["Method", "Bank transfer"],
      ["Reference", r.reference ?? "—"],
    ] as [string, string][],
    CHEQUE: [
      ["Method", "Cheque"],
      ["Cheque no.", r.reference ?? "—"],
    ] as [string, string][],
    CARD: [
      ["Method", "Card"],
      ["Card", r.reference ?? "—"],
    ] as [string, string][],
  }[r.paymentMethod];

  const b = r.buyer;
  const buyerRows: [string, string][] = b.tin
    ? [
        ["TIN", b.tin],
        ["VAT number", b.vat || "—"],
        ["Address", b.addr || "—"],
      ]
    : b.phone
      ? [["Phone", b.phone]]
      : b.idType
        ? [["ID", `${b.idType} ${b.id}`]]
        : [["Type", "Walk-in, no details"]];

  return (
    <main className={`${ui.page} ${iv.detail}`}>
      <Link href="/receipts" className={ui.back}>
        <Icon name="arrowLeft" size={15} />
        Receipts
      </Link>

      <div className={ui.pageHead} style={{ flexWrap: "nowrap" }}>
        <div className={ui.titleBlock} style={{ gap: 10 }}>
          <span className={ui.eyebrow}>RCP · {typeLabel.toUpperCase()} · REGISTERED WITH EIRS</span>
          <div className={iv.detailTitle}>
            <h1 className={ui.title}>
              Receipt <i>RCP-{r.number}</i>
            </h1>
            <span className={rc.type} data-type={r.receiptType} style={{ alignSelf: "center" }}>
              {typeLabel}
            </span>
          </div>
          <span className={iv.meta}>
            Issued {fmtIssued(r.date, r.time)} · {r.branch} · POS {r.sourceSystem}
          </span>
        </div>
        <button type="button" className={ui.btn} style={{ height: 42 }} onClick={() => window.print()}>
          <Icon name="printer" size={15} />
          Print receipt
        </button>
      </div>

      <div className={rc.detailGrid}>
        <div className={iv.detailMain}>
          <div className={rc.pair}>
            <div className={`${ui.card} ${rc.box}`} style={{ gap: 10 }}>
              <span className={iv.label}>{refund ? "REFUNDED" : "RECEIVED"}</span>
              <span className={`${rc.amount} ${refund ? rc.refund : ""}`}>{signed(r, t.grandTotal)}</span>
              <Kv rows={payRows} className={rc.boxKv} />
            </div>
            {inv ? (
              <div
                role="link"
                tabIndex={0}
                className={`${ui.card} ${rc.box} ${rc.linked}`}
                onClick={() => router.push(`/invoices/${inv.id}`)}
                onKeyDown={(e) => e.key === "Enter" && router.push(`/invoices/${inv.id}`)}
              >
                <div className={rc.linkedHead}>
                  <span className={iv.label}>LINKED INVOICE</span>
                  <span className={ui.pill} data-status={inv.status}>
                    {inv.status}
                  </span>
                </div>
                <span className={rc.linkedNo}>INV-{inv.number}</span>
                <span style={{ fontSize: 14 }}>{inv.buyer.name}</span>
                <div className={rc.linkedMeta}>
                  <span>{fmtIssued(inv.date, inv.time)}</span>
                  <span>{etb(totals(inv).grandTotal)}</span>
                </div>
                {t.withholding && (
                  <button
                    type="button"
                    className={rc.wht}
                    onKeyDown={(e) => e.stopPropagation()}
                    onClick={(e) => {
                      e.stopPropagation();
                      router.push(`/withholding/${t.withholding!.id}`);
                    }}
                  >
                    Buyer withheld {etb(t.withheld)} ({Math.round(t.withholding.rate * 100)}%) · {t.withholding.id} ↗
                  </button>
                )}
                <span className={rc.open}>Open invoice →</span>
              </div>
            ) : (
              <div className={`${ui.card} ${rc.box}`}>
                <span className={iv.label}>RECEIVED FROM</span>
                <span className={rc.receivedName}>{b.name}</span>
                <Kv rows={buyerRows} className={rc.boxKv} />
              </div>
            )}
          </div>

          {t.lines.length > 0 ? (
            <div className={`${ui.card} ${iv.items}`}>
              <div className={`${ui.cardHead} ${iv.itemsHead}`}>
                <span className={ui.cardTitle}>Line items</span>
                <span className={ui.cardSub}>
                  {t.lines.length} line{t.lines.length === 1 ? "" : "s"}
                </span>
              </div>
              <div className={`${rc.itemCols} ${iv.itemLabels}`}>
                <span>#</span>
                <span>DESCRIPTION</span>
                <span style={{ textAlign: "right" }}>QTY</span>
                <span style={{ textAlign: "right" }}>UNIT PRICE</span>
                <span>TAX</span>
                <span style={{ textAlign: "right" }}>LINE TOTAL</span>
              </div>
              {t.lines.map((l) => (
                <div key={l.n} className={`${rc.itemCols} ${iv.itemRow}`}>
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
                  <span className={iv.num} style={{ fontWeight: 500 }}>
                    {fmt2(l.total)}
                  </span>
                </div>
              ))}
              <div className={rc.totalsWrap}>
                <dl className={rc.totalsBox}>
                  <div>
                    <dt>Taxable</dt>
                    <dd>{fmt2(t.subtotal)}</dd>
                  </div>
                  {t.taxes.map((x) => (
                    <div key={x.code}>
                      <dt>{x.label}</dt>
                      <dd>{fmt2(x.tax)}</dd>
                    </div>
                  ))}
                  <div className={rc.grand}>
                    <dt>Total</dt>
                    <dd>{etb(t.grandTotal)}</dd>
                  </div>
                </dl>
              </div>
            </div>
          ) : (
            <div className={rc.note}>
              <Icon name="info" size={18} />
              {r.receiptType === "PAYMENT"
                ? "VAT was registered on the linked invoice. This receipt records the payment only."
                : "Refund against the linked invoice. MoR reverses the tax on the refunded amount."}
            </div>
          )}
        </div>

        <div className={iv.aside}>
          <div className={iv.proof}>
            <div className={iv.proofHead}>
              <span className={iv.label}>PROOF OF REGISTRATION</span>
              <span className={iv.proofBadge}>Signed by MoR</span>
            </div>
            <div className={iv.proofScan}>
              <SignedQr irn={irn} size={104} padding={7} radius={10} />
              <span>
                Scan to verify at <b>portal.mor.gov.et</b>.
              </span>
            </div>
            <span className={iv.irnFull}>{irn}</span>
            <button type="button" className={iv.copyDark} onClick={() => copy("detail", irn)}>
              <Icon name="copy" size={13} />
              {copied === "detail" ? "Copied" : "Copy IRN"}
            </button>
          </div>
          {inv && (
            <div className={`${ui.card} ${iv.sideCard}`}>
              <span className={iv.label}>RECEIVED FROM</span>
              <Kv rows={[["Name", b.name], ...buyerRows]} />
            </div>
          )}
          <div className={`${ui.card} ${iv.sideCard}`}>
            <span className={iv.label}>DOCUMENT</span>
            <Kv
              rows={[
                ["Receipt no.", `RCP-${r.number}`],
                ["Type", typeLabel],
                ["Payment", PAYMENT_LABEL[r.paymentMethod]],
                ["Currency", "ETB"],
                ["Branch", r.branch],
                ["System no.", r.sourceSystem],
              ]}
            />
          </div>
        </div>
      </div>
    </main>
  );
}
