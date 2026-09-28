"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { RANGES, RECENT_ACTIVITY, dashboardStats, type ActivityKind } from "@/lib/dashboard";
import { BRANCHES, branchName, etb } from "@/lib/merchant";
import { useConsole } from "../ConsoleProvider";
import { Icon } from "../Icon";
import ui from "../ui.module.css";
import { SalesChart } from "./SalesChart";
import d from "./dashboard.module.css";

const pct = (a: number) => `${Math.round(a * 1000) / 10}%`;
const TT_COLORS = ["var(--brand)", "var(--ink)", "#5FA8AE", "#9CC9CD", "#C9E5E7"];
const ACT_FILTERS: { key: "all" | Lowercase<ActivityKind>; label: string }[] = [
  { key: "all", label: "All" },
  { key: "invoice", label: "Invoices" },
  { key: "receipt", label: "Receipts" },
  { key: "canceled", label: "Canceled" },
];

export function Dashboard() {
  const { range, setRange, branch, setBranch, setupDone, invoices } = useConsole();
  const [act, setAct] = useState<(typeof ACT_FILTERS)[number]["key"]>("all");
  const stats = useMemo(() => dashboardStats(range, branch), [range, branch]);
  const left = 5 - setupDone;

  const tiles = [
    { label: "Tax collected", value: etb(stats.tax).replace("ETB ", ""), sub: `ETB · ${pct(stats.tax / stats.sales)} of gross` },
    { label: "Invoices issued", value: stats.invoices.toLocaleString(), sub: `avg ${etb((stats.sales * 0.62) / stats.invoices)}` },
    { label: "Receipts issued", value: stats.receipts.toLocaleString(), sub: `avg ${etb((stats.sales * 0.38) / stats.receipts)}` },
    { label: "Canceled", value: String(stats.canceled), sub: `${etb(stats.canceledValue)} lost`, danger: true },
  ];

  const activity = RECENT_ACTIVITY.filter(
    (a) => (act === "all" || a.kind.toLowerCase() === act) && (branch === "all" || branchName(branch) === a.branch),
  );

  return (
    <main className={ui.page}>
      {setupDone < 5 && (
        <div className={d.notice}>
          <span className={d.noticeIcon}>
            <Icon name="info" size={16} />
          </span>
          <span className={d.noticeText}>
            <b>
              {left} setup step{left === 1 ? "" : "s"}
            </b>{" "}
            left before live invoicing. Figures below come from DEV documents.
          </span>
          <Link href="/settings" className={d.noticeLink}>
            Finish setup →
          </Link>
        </div>
      )}

      <div className={ui.pageHead}>
        <div className={ui.titleBlock}>
          <span className={ui.eyebrow}>OVERVIEW · {branchName(branch) ?? "All branches"}</span>
          <h1 className={ui.title}>
            Sales <i>overview.</i>
          </h1>
          <p className={ui.sub}>Sales and tax registered with EIRS, {stats.range.long}.</p>
        </div>
        <div className={d.controls}>
          <div className={ui.seg} role="group" aria-label="Time range">
            {RANGES.map((r) => (
              <button key={r.key} type="button" className={ui.segOpt} aria-pressed={range === r.key} onClick={() => setRange(r.key)}>
                {r.label}
              </button>
            ))}
          </div>
          <button type="button" className={ui.btn}>
            <Icon name="download" size={15} />
            Export
          </button>
        </div>
      </div>

      <div className={d.kpis}>
        <div className={`${d.tile} ${d.hero}`}>
          <span className={d.tileLabel}>Total sales</span>
          <span className={d.tileValue}>{etb(stats.sales)}</span>
          <span className={d.delta}>
            <b>▲ {stats.range.delta}</b>
            {stats.range.cmp}
          </span>
        </div>
        {tiles.map((t) => (
          <div key={t.label} className={d.tile}>
            <span className={d.tileLabel}>{t.label}</span>
            <span className={d.tileValue} style={t.danger ? { color: "var(--danger)" } : undefined}>
              {t.value}
            </span>
            <span className={d.tileSub}>{t.sub}</span>
          </div>
        ))}
      </div>

      <div className={d.row21}>
        <SalesChart key={`${range}-${branch}`} stats={stats} />

        <div className={`${ui.card} ${d.panel}`}>
          <div className={ui.stack2}>
            <span className={ui.cardTitle}>Tax by code</span>
            <span className={ui.cardSub}>Registered with EIRS</span>
          </div>
          <div className={d.codes}>
            {stats.taxCodes.map((c) => (
              <div key={c.code} className={d.codeRow}>
                <div className={d.codeHead}>
                  <span className={d.codeName}>
                    <span className={ui.code}>{c.code}</span>
                    {c.label}
                  </span>
                  <span className={d.amt}>{etb(c.tax)}</span>
                </div>
                <div className={d.bar}>
                  <div style={{ width: `${Math.max(2, (c.share / 0.117) * 100)}%` }} />
                </div>
                <span className={d.small}>Taxable {etb(c.taxable)}</span>
              </div>
            ))}
          </div>
          <div className={d.withholding}>
            <div className={ui.stack2}>
              <b>Withholding</b>
              <span className={d.small}>Withheld by B2B / B2G buyers</span>
            </div>
            <span className={d.amt}>{etb(stats.withholding)}</span>
          </div>
        </div>
      </div>

      <div className={d.row11}>
        <div className={`${ui.card} ${d.panel}`}>
          <div className={ui.cardHead}>
            <span className={ui.cardTitle}>By transaction type</span>
            <span className={ui.cardSub}>Share of gross sales</span>
          </div>
          <div className={d.stacked}>
            {stats.transactionTypes.map((t, i) => (
              <div key={t.code} style={{ width: `${t.share * 100}%`, background: TT_COLORS[i] }} />
            ))}
          </div>
          <div>
            {stats.transactionTypes.map((t, i) => (
              <div key={t.code} className={d.ttRow}>
                <span className={d.ttSwatch} style={{ background: TT_COLORS[i] }} />
                <span className={d.ttName}>
                  <b>{t.code}</b>
                  <span>{t.name}</span>
                </span>
                <span className={d.ttDocs}>
                  {t.docs} docs · {Math.round(t.share * 100)}%
                </span>
                <span className={d.ttAmt}>{etb(t.amount)}</span>
              </div>
            ))}
          </div>
        </div>

        <div className={`${ui.card} ${d.panel}`}>
          <div className={ui.cardHead}>
            <span className={ui.cardTitle}>By branch</span>
            <span className={ui.cardSub}>Via source system</span>
          </div>
          <div className={d.branches}>
            {stats.branches.map((b) => {
              const meta = BRANCHES.find((x) => x.key === b.key)!;
              const on = branch === b.key;
              return (
                <button
                  key={b.key}
                  type="button"
                  className={d.branch}
                  aria-pressed={on}
                  data-dim={branch !== "all" && !on}
                  onClick={() => setBranch(on ? "all" : b.key)}
                >
                  <span className={d.branchHead}>
                    <span className={d.branchName}>
                      {meta.name}
                      {meta.isDefault && <span className={d.defaultTag}>DEFAULT</span>}
                    </span>
                    <span className={d.amt} style={{ fontSize: 14 }}>
                      {etb(b.amount)}
                    </span>
                  </span>
                  <span className={d.bar}>
                    <span style={{ width: `${(b.share / 0.58) * 100}%` }} />
                  </span>
                  <span className={d.branchFoot}>
                    <span>{meta.systems}</span>
                    <span>
                      {b.docs} docs · tax {etb(b.tax)}
                    </span>
                  </span>
                </button>
              );
            })}
          </div>
        </div>
      </div>

      <div className={d.row21}>
        <div className={`${ui.card} ${d.activity}`}>
          <div className={`${ui.cardHead} ${d.activityHead}`}>
            <span className={ui.cardTitle}>Recent activity</span>
            <div className={d.chips}>
              {ACT_FILTERS.map((f) => (
                <button key={f.key} type="button" className={d.chip} aria-pressed={act === f.key} onClick={() => setAct(f.key)}>
                  {f.label}
                </button>
              ))}
            </div>
          </div>
          <div className={`${d.actCols} ${d.actLabels}`}>
            <span>TYPE</span>
            <span>DOCUMENT</span>
            <span>CUSTOMER</span>
            <span>ISSUED</span>
            <span style={{ textAlign: "right" }}>TOTAL</span>
            <span />
          </div>
          {activity.map((a) => {
            const match = invoices.find((v) => `INV-${v.number}` === a.number);
            const cells = (
              <>
                <span className={d.kind} data-kind={a.kind}>
                  {a.kind}
                </span>
                <span className={d.cell}>
                  <span style={{ fontWeight: 500 }}>{a.number}</span>
                  <span className={d.irn}>test-{a.irn}…</span>
                </span>
                <span className={d.cell}>
                  <span>{a.customer}</span>
                  <span className={d.small}>
                    {a.branch} · {a.tt}
                  </span>
                </span>
                <span className={d.when}>{a.when}</span>
                <span className={`${d.total} ${a.kind === "Canceled" ? d.struck : ""}`}>{etb(a.amount)}</span>
                <Icon name="chevronRight" size={14} stroke="var(--muted)" />
              </>
            );
            // Receipts don't have a detail page in the console yet.
            return match ? (
              <Link key={a.number} href={`/invoices/${match.id}`} className={`${d.actCols} ${d.actRow}`}>
                {cells}
              </Link>
            ) : (
              <div key={a.number} className={`${d.actCols} ${d.actRow}`}>
                {cells}
              </div>
            );
          })}
          <Link href="/invoices" className={d.viewAll}>
            View all invoices →
          </Link>
        </div>

        <div className={d.side}>
          <div className={d.quick}>
            <span className={d.quickTitle}>QUICK ACTIONS</span>
            <div className={d.quickList}>
              <Link href="/invoices/new" className={`${d.action} ${d.actionPrimary}`}>
                <span className={d.actionIcon}>
                  <Icon name="fileAdd" size={18} stroke="#fff" strokeWidth={1.8} />
                </span>
                <span className={d.actionText}>
                  <b>Register invoice</b>
                  <span>B2B, B2G and credit / debit notes</span>
                </span>
                <Icon name="arrowRight" size={16} stroke="#fff" />
              </Link>
              {/* Receipt registration isn't designed in the console yet. */}
              <button type="button" className={d.action}>
                <span className={d.actionIcon}>
                  <Icon name="receipt" size={18} stroke="#fff" strokeWidth={1.8} />
                </span>
                <span className={d.actionText}>
                  <b>Register receipt</b>
                  <span>Walk-in and point-of-sale</span>
                </span>
                <Icon name="arrowRight" size={16} stroke="#fff" />
              </button>
            </div>
          </div>

          <div className={`${ui.card} ${d.panel}`} style={{ gap: 14 }}>
            <span className={ui.cardTitle}>Canceled {stats.range.short}</span>
            <span className={d.lost}>{etb(stats.canceledValue)}</span>
            <span className={d.lostLine}>
              {stats.canceled} invoice{stats.canceled === 1 ? "" : "s"} canceled, {pct(stats.canceled / stats.invoices)} of invoices issued. Value no
              longer counted in sales.
            </span>
            <dl className={d.reasons}>
              {stats.cancelReasons.map((r) => (
                <div key={r.label}>
                  <dt>{r.label}</dt>
                  <dd>{Math.round(r.share * 100)}%</dd>
                </div>
              ))}
            </dl>
          </div>
        </div>
      </div>
    </main>
  );
}
