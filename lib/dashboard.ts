// Sales / tax analytics for the dashboard. Mocked: the series below is sample
// data generated to match the design. When wired to the backend, compute these
// from listInvoices() / listReceipts() / listCanceledInvoices() for the range,
// or add an aggregate endpoint once document volume grows — the dashboard only
// depends on the DashboardStats shape.

import type { BranchKey, TaxCode, TransactionType } from "./merchant";
import { BRANCHES, TRANSACTION_TYPES } from "./merchant";

export type RangeKey = "today" | "d7" | "d30" | "month" | "year";

export const RANGES: { key: RangeKey; label: string }[] = [
  { key: "today", label: "Today" },
  { key: "d7", label: "7 days" },
  { key: "d30", label: "30 days" },
  { key: "month", label: "This month" },
  { key: "year", label: "This year" },
];

const MONTHS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
/** Last day of sample data. */
const END = new Date(2026, 8, 25);

const dayLabel = (back: number) => {
  const d = new Date(END);
  d.setDate(d.getDate() - back);
  return `${MONTHS[d.getMonth()]} ${d.getDate()}`;
};

type RangeCfg = {
  n: number;
  label: (i: number) => string;
  base: number;
  seed: number;
  delta: string;
  cmp: string;
  /** "Sales and tax registered with EIRS, {long}." */
  long: string;
  /** "Canceled {short}" */
  short: string;
  sub: string;
};

const CFG: Record<RangeKey, RangeCfg> = {
  today: { n: 12, label: (i) => `${String(8 + i).padStart(2, "0")}:00`, base: 9200, seed: 3, delta: "8.2%", cmp: "vs yesterday", long: "today", short: "today", sub: "Hourly, 08:00 – 19:00" },
  d7: { n: 7, label: (i) => dayLabel(6 - i), base: 68000, seed: 11, delta: "12.4%", cmp: "vs previous 7 days", long: "last 7 days", short: "this week", sub: "Daily, Sep 19 – Sep 25" },
  d30: { n: 30, label: (i) => dayLabel(29 - i), base: 64000, seed: 23, delta: "6.1%", cmp: "vs previous 30 days", long: "last 30 days", short: "last 30 days", sub: "Daily, Aug 27 – Sep 25" },
  month: { n: 25, label: (i) => `Sep ${i + 1}`, base: 66000, seed: 31, delta: "9.7%", cmp: "vs Aug 1 – 25", long: "this month", short: "this month", sub: "Daily, Sep 1 – Sep 25" },
  year: { n: 9, label: (i) => MONTHS[i], base: 1850000, seed: 47, delta: "31.5%", cmp: "vs Jan – Sep 2025", long: "this year", short: "this year", sub: "Monthly, Jan – Sep 2026" },
};

/** Share of combined sales per branch. */
const BRANCH_SHARE: Record<Exclude<BranchKey, "all">, number> = { main: 0.58, bole: 0.29, piassa: 0.13 };

/** Effective tax on gross: gross = taxable × 1.125. */
const GROSS_FACTOR = 1.125;

export type DashboardStats = {
  range: { long: string; short: string; sub: string; delta: string; cmp: string };
  /** Gross sales per bucket, with its axis label. */
  series: { label: string; sales: number; tax: number }[];
  sales: number;
  tax: number;
  taxable: number;
  invoices: number;
  receipts: number;
  canceled: number;
  canceledValue: number;
  withholding: number;
  taxCodes: { code: TaxCode; label: string; taxable: number; tax: number; share: number }[];
  transactionTypes: { code: TransactionType; name: string; share: number; amount: number; docs: number }[];
  /** Per-branch split of the all-branches total (so the cards stay comparable when one is selected). */
  branches: { key: Exclude<BranchKey, "all">; share: number; amount: number; tax: number; docs: number }[];
  cancelReasons: { label: string; share: number }[];
};

export function dashboardStats(rangeKey: RangeKey, branch: BranchKey): DashboardStats {
  const cfg = CFG[rangeKey];
  let seed = cfg.seed;
  const rnd = () => (seed = (seed * 9301 + 49297) % 233280) / 233280;
  const share = branch === "all" ? 1 : BRANCH_SHARE[branch];
  const values = Array.from(
    { length: cfg.n },
    (_, i) => Math.round(share * cfg.base * (0.6 + 0.7 * rnd()) * (0.85 + (0.3 * i) / cfg.n) * 100) / 100,
  );

  const sales = values.reduce((a, b) => a + b, 0);
  const taxable = sales / GROSS_FACTOR;
  const tax = sales - taxable;
  const docs = Math.round(sales / 3850);
  const invoices = Math.round(docs * 0.38);
  const receipts = docs - invoices;
  const canceled = Math.max(1, Math.round(docs * 0.015));
  const canceledValue = canceled * (sales / docs) * 1.3;

  const combined = branch === "all" ? sales : sales / share;
  const combinedTax = combined - combined / GROSS_FACTOR;
  const combinedDocs = Math.round(combined / 3850);

  const TT_SHARE: Record<TransactionType, number> = { B2C: 0.52, B2B: 0.31, B2G: 0.11, G2B: 0.04, G2C: 0.02 };

  return {
    range: { long: cfg.long, short: cfg.short, sub: cfg.sub, delta: cfg.delta, cmp: cfg.cmp },
    series: values.map((v, i) => ({ label: cfg.label(i), sales: v, tax: v - v / GROSS_FACTOR })),
    sales,
    tax,
    taxable,
    invoices,
    receipts,
    canceled,
    canceledValue,
    withholding: taxable * 0.0084,
    taxCodes: (
      [
        ["VAT15", "Standard VAT", 0.78, 0.15],
        ["EXC10", "Excise", 0.08, 0.1],
        ["VAT0", "Zero-rated", 0.14, 0],
      ] as const
    ).map(([code, label, sh, rate]) => ({ code, label, taxable: taxable * sh, tax: taxable * sh * rate, share: sh * rate })),
    transactionTypes: TRANSACTION_TYPES.map(({ code, name }) => ({
      code,
      name,
      share: TT_SHARE[code],
      amount: sales * TT_SHARE[code],
      docs: Math.max(1, Math.round(docs * TT_SHARE[code])),
    })),
    branches: BRANCHES.map((b) => ({
      key: b.key,
      share: BRANCH_SHARE[b.key],
      amount: combined * BRANCH_SHARE[b.key],
      tax: combinedTax * BRANCH_SHARE[b.key],
      docs: Math.round(combinedDocs * BRANCH_SHARE[b.key]),
    })),
    cancelReasons: [
      { label: "Customer request", share: 0.52 },
      { label: "Wrong amount", share: 0.31 },
      { label: "Duplicate", share: 0.17 },
    ],
  };
}

export type ActivityKind = "Invoice" | "Receipt" | "Canceled";

/** Latest documents across invoices, receipts and cancellations. */
export const RECENT_ACTIVITY: {
  kind: ActivityKind;
  number: string;
  customer: string;
  branch: string;
  tt: TransactionType;
  when: string;
  amount: number;
  irn: string;
}[] = [
  { kind: "Invoice", number: "INV-104", customer: "Jhone Doe", branch: "Main Branch", tt: "B2C", when: "Today, 10:42", amount: 26694.95, irn: "e9c88ce999fbfa5c33457b" },
  { kind: "Receipt", number: "RCP-2291", customer: "Walk-in Customer", branch: "Bole", tt: "B2C", when: "Today, 10:31", amount: 862.5, irn: "7a1f02c8b5e4d91a0c6b33" },
  { kind: "Canceled", number: "INV-103", customer: "Jhone Doe", branch: "Main Branch", tt: "B2C", when: "Today, 09:58", amount: 1929.7, irn: "dfa7175fb567d45c88acb9" },
  { kind: "Invoice", number: "INV-101", customer: "Ethio Telecom", branch: "Main Branch", tt: "B2G", when: "Today, 09:12", amount: 48300, irn: "31b0e7aa6f4c29d8e05c14" },
  { kind: "Receipt", number: "RCP-2290", customer: "Walk-in Customer", branch: "Piassa", tt: "B2C", when: "Today, 08:47", amount: 415, irn: "c2f4910b7ee3a85d6d0a72" },
  { kind: "Invoice", number: "INV-100", customer: "Dashen Trading PLC", branch: "Bole", tt: "B2B", when: "Yesterday, 17:20", amount: 12650, irn: "8b6d2e0f4a11c7d9b3e521" },
  { kind: "Canceled", number: "INV-102", customer: "Jhone Doe", branch: "Main Branch", tt: "B2C", when: "Yesterday, 15:03", amount: 3450, irn: "0e0a858ccb9ca9a61dafc2" },
  { kind: "Receipt", number: "RCP-2289", customer: "Walk-in Customer", branch: "Main Branch", tt: "B2C", when: "Yesterday, 14:36", amount: 1265, irn: "5a93c0d1e7f2b4a68c9e10" },
  { kind: "Invoice", number: "INV-099", customer: "Awash Logistics", branch: "Main Branch", tt: "B2B", when: "Yesterday, 11:02", amount: 9870.4, irn: "f1e2d3c4b5a697887766aa" },
  { kind: "Receipt", number: "RCP-2288", customer: "Walk-in Customer", branch: "Bole", tt: "B2C", when: "Yesterday, 10:15", amount: 690, irn: "94ab01cd23ef4567a8b9c0" },
];
