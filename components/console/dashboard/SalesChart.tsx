"use client";

import { useState } from "react";
import type { DashboardStats } from "@/lib/dashboard";
import { etb } from "@/lib/merchant";
import ui from "../ui.module.css";
import d from "./dashboard.module.css";

const W = 720;
const H = 220;

/** Compact axis label: 1.2M, 68K, 950. */
const compact = (n: number) =>
  n >= 1e6 ? (n / 1e6).toFixed(2).replace(/\.?0+$/, "") + "M" : n >= 1e3 ? Math.round(n / 1e3) + "K" : String(Math.round(n));

export function SalesChart({ stats }: { stats: DashboardStats }) {
  const [hover, setHover] = useState<number | null>(null);
  const s = stats.series;
  const max = Math.max(...s.map((p) => p.sales)) * 1.12;

  const pts = s.map((p, i) => [s.length === 1 ? W / 2 : (i / (s.length - 1)) * W, H - (p.sales / max) * H] as const);
  // Tax is drawn at 4× scale so its shape reads against gross sales.
  const taxPts = s.map((p, i) => [pts[i][0], H - (p.tax / max) * H * 4] as const);
  const line = (p: readonly (readonly [number, number])[]) => p.map((q, i) => `${i ? "L" : "M"}${q[0].toFixed(1)} ${q[1].toFixed(1)}`).join(" ");
  const lineD = line(pts);
  const areaD = `${lineD} L${W} ${H} L0 ${H} Z`;

  const step = Math.max(1, Math.ceil(s.length / 7));
  const last = s.length - 1;
  const xTicks = s
    .map((_, i) => i)
    .filter((i) => i % step === 0 || i === last)
    // Drop the tick just before the last one when they'd collide.
    .filter((i, k, arr) => !(i !== last && last - i < step * 0.6 && k === arr.length - 2));

  const hi = hover == null ? null : Math.min(last, hover);

  function onMove(e: React.MouseEvent<HTMLDivElement>) {
    const r = e.currentTarget.getBoundingClientRect();
    const i = Math.max(0, Math.round(((e.clientX - r.left) / r.width) * last));
    if (i !== hover) setHover(i);
  }

  return (
    <div className={`${ui.card} ${d.chartCard}`}>
      <div className={ui.cardHead}>
        <div className={ui.stack2}>
          <span className={ui.cardTitle}>Sales &amp; tax over time</span>
          <span className={ui.cardSub}>{stats.range.sub}</span>
        </div>
        <div className={d.legend}>
          <span>
            <span className={d.swatch} />
            Gross sales
          </span>
          <span>
            <span className={d.dash} />
            Tax
          </span>
        </div>
      </div>
      <div className={d.chartBody}>
        <div className={d.yAxis}>
          {[max, (max * 2) / 3, max / 3, 0].map((y, i) => (
            <span key={i}>{compact(y)}</span>
          ))}
        </div>
        <div className={d.plotCol}>
          <div className={d.plot}>
            <div className={d.grid}>
              <div />
              <div />
              <div />
              <div />
            </div>
            <svg viewBox={`0 0 ${W} ${H}`} preserveAspectRatio="none" className={d.svg} aria-hidden>
              <defs>
                <linearGradient id="sales-area" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0" style={{ stopColor: "var(--brand)", stopOpacity: 0.28 }} />
                  <stop offset="1" style={{ stopColor: "var(--brand)", stopOpacity: 0 }} />
                </linearGradient>
              </defs>
              <path d={areaD} fill="url(#sales-area)" />
              <path d={lineD} fill="none" stroke="var(--brand)" strokeWidth={2.5} strokeLinejoin="round" vectorEffect="non-scaling-stroke" />
              <path d={line(taxPts)} fill="none" stroke="var(--ink)" strokeWidth={1.5} strokeDasharray="5 4" vectorEffect="non-scaling-stroke" />
            </svg>
            {hi != null && (
              <>
                <div className={d.cursor} style={{ left: `${(pts[hi][0] / W) * 100}%` }} />
                <div className={d.dot} style={{ left: `${(pts[hi][0] / W) * 100}%`, top: `${(pts[hi][1] / H) * 100}%` }} />
                <div
                  className={d.tip}
                  style={{
                    left: `${(pts[hi][0] / W) * 100}%`,
                    transform: `translateX(${pts[hi][0] > W * 0.75 ? "calc(-100% - 12px)" : "12px"})`,
                  }}
                >
                  <span className={d.tipLabel}>{s[hi].label}</span>
                  <span className={d.tipRow}>
                    <span>Sales</span>
                    <b>{etb(s[hi].sales)}</b>
                  </span>
                  <span className={d.tipRow}>
                    <span>Tax</span>
                    <b>{etb(s[hi].tax)}</b>
                  </span>
                </div>
              </>
            )}
            <div className={d.hit} onMouseMove={onMove} onMouseLeave={() => setHover(null)} />
          </div>
          <div className={d.xAxis}>
            {xTicks.map((i) => (
              <span key={i} style={{ left: `${(pts[i][0] / W) * 100}%` }}>
                {s[i].label}
              </span>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
