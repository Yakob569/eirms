// Merchant API boundary for the console. Like lib/auth.ts these are mocks with
// sample data from the design — swap the bodies of the async functions (and the
// sample arrays) for real backend calls; the screens only depend on the types
// and signatures below.

export type Status = "ISSUED" | "PAID" | "WITHHELD" | "CANCELED";
export type DocType = "INV" | "CRE" | "DEB";
export type TransactionType = "B2B" | "B2C" | "B2G" | "G2B" | "G2C";
export type TaxCode = "VAT15" | "VAT0" | "EXC10";
export type BranchName = "Main Branch" | "Bole" | "Piassa";
export type BranchKey = "all" | "main" | "bole" | "piassa";
export type IdType = "NID" | "KID" | "SID" | "WID" | "PST" | "DLS" | "MRS";

export type Party = {
  name: string;
  tin?: string;
  vat?: string;
  idType?: IdType;
  id?: string;
  phone?: string;
  email?: string;
  addr?: string;
};

export type LineItem = {
  description: string;
  itemCode: string;
  nature: "Goods" | "Service";
  unit: string;
  quantity: number;
  unitPrice: number;
  taxCode: TaxCode;
};

export type Cancellation = { code: string; reason: string; canceledAt: string };

export type Invoice = {
  id: string;
  number: string;
  docType: DocType;
  transactionType: TransactionType;
  status: Status;
  /** Status before cancellation (canceled documents only). */
  originalStatus?: Status;
  cancel?: Cancellation;
  /** yyyy-mm-dd */
  date: string;
  /** HH:MM */
  time: string;
  branch: BranchName;
  sourceSystem: string;
  buyer: Party;
  payment: { mode: string; term: string };
  items: LineItem[];
  /** Credit / debit notes: why, and the invoice being amended. */
  amends?: { reason: string; previousId: string };
};

// ─── Reference data ────────────────────────────────────────────────────────

export const TAX_RATES: Record<TaxCode, { rate: number; label: string }> = {
  VAT15: { rate: 0.15, label: "VAT 15%" },
  VAT0: { rate: 0, label: "VAT 0%" },
  EXC10: { rate: 0.1, label: "Excise 10%" },
};

export const TAX_CODES: TaxCode[] = ["VAT15", "VAT0", "EXC10"];

export const DOC_NAMES: Record<DocType, string> = { INV: "Invoice", CRE: "Credit note", DEB: "Debit note" };

export const TRANSACTION_TYPES: { code: TransactionType; name: string }[] = [
  { code: "B2C", name: "Business to consumer" },
  { code: "B2B", name: "Business to business" },
  { code: "B2G", name: "Business to government" },
  { code: "G2B", name: "Government to business" },
  { code: "G2C", name: "Government to consumer" },
];

/** B2C / G2C are consumer sales with lighter buyer-ID requirements. */
export const isConsumerSale = (tt: TransactionType) => tt === "B2C" || tt === "G2C";

export const ID_TYPES: { code: IdType; name: string }[] = [
  { code: "NID", name: "National ID" },
  { code: "KID", name: "Kebele ID" },
  { code: "SID", name: "Student ID" },
  { code: "WID", name: "Work ID" },
  { code: "PST", name: "Passport" },
  { code: "DLS", name: "Driving licence" },
  { code: "MRS", name: "Military / residence" },
];

export const CANCEL_REASONS: { code: string; label: string }[] = [
  { code: "01", label: "Customer request" },
  { code: "02", label: "Wrong amount entered" },
  { code: "03", label: "Duplicate document" },
  { code: "04", label: "Wrong buyer details" },
];

export const BRANCHES: { key: Exclude<BranchKey, "all">; name: BranchName; sub: string; systems: string; isDefault: boolean }[] = [
  { key: "main", name: "Main Branch", sub: "Default · 2 source systems", systems: "2 source systems", isDefault: true },
  { key: "bole", name: "Bole", sub: "1 source system", systems: "1 source system", isDefault: false },
  { key: "piassa", name: "Piassa", sub: "1 source system", systems: "1 source system", isDefault: false },
];

export function branchName(key: BranchKey): BranchName | null {
  return BRANCHES.find((b) => b.key === key)?.name ?? null;
}

export function branchKey(name: BranchName): Exclude<BranchKey, "all"> {
  return BRANCHES.find((b) => b.name === name)!.key;
}

// ─── Signed-in user and merchant ───────────────────────────────────────────

export const CURRENT_USER = {
  name: "Yakob Girma",
  initials: "YG",
  email: "yakobgirmad@gmail.com",
  role: "Owner",
  userId: "UID-E0F6L4A99C",
};

export const MERCHANT = { name: "Addispay Financial Tech.", initials: "AF", id: "MRC-KL7Z" };

export const SELLER: Party = {
  name: "Addispay Financial Technology S.C",
  tin: "4082549522",
  vat: "43256663343256663322",
  phone: "+251921280809",
  email: "cto@addispay.et",
  addr: "Addis Ababa · Wereda 13",
};

// ─── Fiscal readiness (Settings › Setup) ───────────────────────────────────

export const SETUP_STEPS = [
  { title: "MoR credentials", desc: "Save your EIRMS client ID, secret, API key, TIN and environment." },
  { title: "Seller profile", desc: "Legal name, TIN, VAT number, wereda and region." },
  { title: "Source system", desc: "Register at least one POS or storefront system." },
  { title: "Verify setup", desc: "Run a signed probe against the MoR endpoint." },
  { title: "API keys unlocked", desc: "Issue live keys once every step above is green." },
];

/**
 * Number of setup steps completed (0–5). Mocked as fully set up; set this lower
 * to see the dashboard notice and the "Continue setup" sidebar card.
 */
export function readiness(): { done: number } {
  return { done: 5 };
}

// ─── Invoices ──────────────────────────────────────────────────────────────

const item = (
  description: string,
  itemCode: string,
  nature: LineItem["nature"],
  unit: string,
  quantity: number,
  unitPrice: number,
  taxCode: TaxCode,
): LineItem => ({ description, itemCode, nature, unit, quantity, unitPrice, taxCode });

const SAMPLE_INVOICES: Invoice[] = [
  {
    id: "INV-FP95G9D7JG", number: "104", docType: "INV", transactionType: "B2C", status: "ISSUED",
    date: "2026-09-24", time: "10:42", branch: "Main Branch", sourceSystem: "2EC77ABF63",
    buyer: { name: "Jhone Doe", idType: "NID", id: "FAN 4521 8890 1123", phone: "+251911442210" },
    payment: { mode: "CASH", term: "IMMEDIATE" },
    items: [item("Laptop stand, aluminium", "ITM-0012", "Goods", "PCS", 1, 23213, "VAT15")],
  },
  {
    id: "INV-K2L8M3N4PQ", number: "103", docType: "INV", transactionType: "B2C", status: "CANCELED", originalStatus: "ISSUED",
    cancel: { code: "02", reason: "Wrong amount entered", canceledAt: "24 Sep 2026, 11:05" },
    date: "2026-09-24", time: "09:58", branch: "Main Branch", sourceSystem: "2EC77ABF63",
    buyer: { name: "Jhone Doe", idType: "NID", id: "FAN 4521 8890 1123" },
    payment: { mode: "CASH", term: "IMMEDIATE" },
    items: [item("Wireless keyboard", "ITM-0031", "Goods", "PCS", 1, 1678, "VAT15")],
  },
  {
    id: "INV-7HJ2K9D0LA", number: "102", docType: "INV", transactionType: "B2C", status: "CANCELED", originalStatus: "ISSUED",
    cancel: { code: "01", reason: "Customer request", canceledAt: "23 Sep 2026, 16:20" },
    date: "2026-09-23", time: "15:03", branch: "Main Branch", sourceSystem: "2EC77ABF63",
    buyer: { name: "Jhone Doe", idType: "NID", id: "FAN 4521 8890 1123" },
    payment: { mode: "CASH", term: "IMMEDIATE" },
    items: [item("Monitor arm", "ITM-0044", "Goods", "PCS", 2, 1500, "VAT15")],
  },
  {
    id: "INV-R4T6Y8U1IO", number: "101", docType: "INV", transactionType: "B2G", status: "ISSUED",
    date: "2026-09-24", time: "09:12", branch: "Main Branch", sourceSystem: "2EC77ABF63",
    buyer: { name: "Ethio Telecom", tin: "0000030603", vat: "1087650012", phone: "+251115515000", email: "procurement@ethiotelecom.et", addr: "Addis Ababa · Wereda 05" },
    payment: { mode: "BANK TRANSFER", term: "NET 30" },
    items: [
      item("Network cabling installation", "SRV-0102", "Service", "JOB", 1, 30000, "VAT15"),
      item("Cat6 cable, 305m box", "ITM-0210", "Goods", "BOX", 10, 1200, "VAT15"),
    ],
  },
  {
    id: "INV-B3N5M7Q9WE", number: "100", docType: "INV", transactionType: "B2B", status: "PAID",
    date: "2026-09-23", time: "17:20", branch: "Bole", sourceSystem: "5054087CDF",
    buyer: { name: "Dashen Trading PLC", tin: "0041527796", vat: "2231009874", phone: "+251116612233", email: "ap@dashentrading.et", addr: "Addis Ababa · Wereda 03" },
    payment: { mode: "BANK TRANSFER", term: "IMMEDIATE" },
    items: [
      item("Ergonomic office chair", "ITM-0301", "Goods", "PCS", 5, 1800, "VAT15"),
      item("Delivery and assembly", "SRV-0009", "Service", "JOB", 1, 2300, "VAT0"),
    ],
  },
  {
    id: "INV-Z8X6C4V2BN", number: "099", docType: "INV", transactionType: "B2B", status: "WITHHELD",
    date: "2026-09-23", time: "11:02", branch: "Main Branch", sourceSystem: "2EC77ABF63",
    buyer: { name: "Awash Logistics S.C", tin: "0063321458", vat: "3345120098", phone: "+251114408877", email: "finance@awashlogistics.et", addr: "Addis Ababa · Wereda 11" },
    payment: { mode: "CREDIT", term: "NET 30" },
    items: [item("POS integration consulting", "SRV-0110", "Service", "HRS", 1, 8582.96, "VAT15")],
  },
  {
    id: "CRE-Q1W2E3R4TY", number: "098", docType: "CRE", transactionType: "B2B", status: "ISSUED",
    date: "2026-09-22", time: "14:10", branch: "Bole", sourceSystem: "5054087CDF",
    amends: { reason: "Returned 1 chair, damaged in delivery", previousId: "INV-B3N5M7Q9WE" },
    buyer: { name: "Dashen Trading PLC", tin: "0041527796", vat: "2231009874", addr: "Addis Ababa · Wereda 03" },
    payment: { mode: "BANK TRANSFER", term: "IMMEDIATE" },
    items: [item("Ergonomic office chair (return)", "ITM-0301", "Goods", "PCS", 1, 1800, "VAT15")],
  },
  {
    id: "INV-P0O9I8U7YT", number: "097", docType: "INV", transactionType: "B2C", status: "ISSUED",
    date: "2026-09-22", time: "12:31", branch: "Piassa", sourceSystem: "9A1C44B0E2",
    buyer: { name: "Selam Bekele", idType: "KID", id: "AA-114-22871" },
    payment: { mode: "MOBILE MONEY", term: "IMMEDIATE" },
    items: [
      item("Yirgacheffe coffee beans, 1kg", "ITM-0508", "Goods", "KG", 4, 650, "VAT15"),
      item("Printed notebooks", "ITM-0612", "Goods", "PCS", 2, 350, "VAT0"),
    ],
  },
  {
    id: "INV-L5K4J3H2GF", number: "096", docType: "INV", transactionType: "B2B", status: "ISSUED",
    date: "2026-09-21", time: "10:05", branch: "Main Branch", sourceSystem: "2EC77ABF63",
    buyer: { name: "Zemen Bank S.C", tin: "0005581342", vat: "1122334455", email: "ops@zemenbank.com", addr: "Addis Ababa · Wereda 01" },
    payment: { mode: "BANK TRANSFER", term: "NET 30" },
    items: [
      item("Thermal receipt paper rolls", "ITM-0701", "Goods", "PCS", 40, 85, "VAT15"),
      item("Imported soft drinks, case", "ITM-0815", "Goods", "CASE", 12, 150, "EXC10"),
    ],
  },
];

const delay = (ms: number) => new Promise((r) => setTimeout(r, ms));

/** All registered documents, including canceled ones, newest first. */
export async function listInvoices(): Promise<Invoice[]> {
  return SAMPLE_INVOICES;
}

/** Synchronous copy of the sample list, used to seed client state without a loading flash. */
export const INITIAL_INVOICES = SAMPLE_INVOICES;

export type InvoiceDraft = {
  docType: DocType;
  transactionType: TransactionType;
  walkIn: boolean;
  idType: IdType;
  idNumber: string;
  phone: string;
  tin: string;
  name: string;
  vat: string;
  email: string;
  addr: string;
  previousIrn: string;
  amendReason: string;
  paymentMode: string;
  paymentTerm: string;
  lines: { description: string; quantity: string; unitPrice: string; taxCode: TaxCode }[];
};

/** Submits a draft to MoR's real-time registration endpoint; resolves with the signed document. */
export async function registerInvoice(draft: InvoiceDraft, ctx: { branch: BranchName; nextNumber: string }): Promise<Invoice> {
  await delay(400);
  const now = new Date();
  const consumer = isConsumerSale(draft.transactionType);
  const buyer: Party = consumer
    ? draft.walkIn
      ? { name: "Walk-in Customer" }
      : { name: draft.name || "Consumer", idType: draft.idType, id: draft.idNumber || "—", phone: draft.phone || undefined }
    : { name: draft.name, tin: draft.tin || "0041527796", vat: draft.vat || "—", email: draft.email || undefined, addr: draft.addr || "—" };
  return {
    id: `${draft.docType}-NEW${Date.now().toString(36).toUpperCase()}`,
    number: ctx.nextNumber,
    docType: draft.docType,
    transactionType: draft.transactionType,
    status: "ISSUED",
    date: isoDate(now),
    time: now.toTimeString().slice(0, 5),
    branch: ctx.branch,
    sourceSystem: "2EC77ABF63",
    amends: draft.docType !== "INV" ? { reason: draft.amendReason || "Amendment", previousId: "INV-FP95G9D7JG" } : undefined,
    buyer,
    payment: { mode: draft.paymentMode, term: draft.paymentTerm },
    items: draft.lines
      .filter((l) => l.description && parseFloat(l.unitPrice) > 0)
      .map((l, i) => item(l.description, `ITM-${900 + i}`, "Goods", "PCS", parseFloat(l.quantity) || 1, parseFloat(l.unitPrice), l.taxCode)),
  };
}

/** Records a cancellation against the invoice's IRN with MoR. */
export async function cancelInvoice(id: string, reasonCode: string): Promise<Cancellation> {
  await delay(400);
  void id;
  const reason = CANCEL_REASONS.find((r) => r.code === reasonCode)?.label ?? "Canceled";
  return { code: reasonCode, reason, canceledAt: fmtDateTime(new Date()) };
}

/** Looks a buyer up by TIN in the MoR taxpayer registry. */
export async function lookupTin(tin: string): Promise<Pick<Party, "name" | "vat" | "addr">> {
  await delay(300);
  void tin;
  return { name: "Dashen Trading PLC", vat: "2231009874", addr: "Addis Ababa · 03" };
}

// ─── Derived values ────────────────────────────────────────────────────────

export type Totals = {
  lines: (LineItem & { n: number; preTax: number; tax: number; total: number })[];
  subtotal: number;
  taxTotal: number;
  grandTotal: number;
  taxes: { code: TaxCode; label: string; taxable: number; tax: number }[];
};

export function totals(inv: Pick<Invoice, "items">): Totals {
  let subtotal = 0;
  const by: Partial<Record<TaxCode, { taxable: number; tax: number }>> = {};
  const lines = inv.items.map((it, i) => {
    const preTax = it.quantity * it.unitPrice;
    const tax = preTax * TAX_RATES[it.taxCode].rate;
    subtotal += preTax;
    const b = (by[it.taxCode] ??= { taxable: 0, tax: 0 });
    b.taxable += preTax;
    b.tax += tax;
    return { ...it, n: i + 1, preTax, tax, total: preTax + tax };
  });
  const taxes = (Object.entries(by) as [TaxCode, { taxable: number; tax: number }][]).map(([code, o]) => ({
    code,
    label: TAX_RATES[code].label,
    taxable: o.taxable,
    tax: o.tax,
  }));
  const taxTotal = taxes.reduce((a, t) => a + t.tax, 0);
  return { lines, subtotal, taxTotal, grandTotal: subtotal + taxTotal, taxes };
}

/** MoR's fiscal signature for a document. Sample data derives a stable 64-hex IRN from the id. */
export function irnFor(id: string): string {
  let s = 0;
  for (const c of id) s = (s * 31 + c.charCodeAt(0)) % 233280;
  let o = "";
  for (let i = 0; i < 64; i++) {
    s = (s * 9301 + 49297) % 233280;
    o += "0123456789abcdef"[Math.floor((s / 233280) * 16)];
  }
  return o;
}

export const shortIrn = (irn: string) => `ETH-${irn.slice(0, 8)}··${irn.slice(-6)}`;

/** 21×21 QR-style grid seeded by the IRN: three finder squares plus a deterministic pattern. */
export function qrCells(irn: string): boolean[] {
  let seed = 0;
  for (const ch of irn) seed = (seed * 17 + parseInt(ch, 16)) % 233280;
  const finder = (r: number, c: number) => r >= 0 && r < 7 && c >= 0 && c < 7;
  const cells: boolean[] = [];
  for (let r = 0; r < 21; r++) {
    for (let c = 0; c < 21; c++) {
      if (finder(r, c) || finder(r, c - 14) || finder(r - 14, c)) {
        const rr = r >= 14 ? r - 14 : r;
        const cc = c >= 14 ? c - 14 : c;
        cells.push(rr === 0 || rr === 6 || cc === 0 || cc === 6 || (rr >= 2 && rr <= 4 && cc >= 2 && cc <= 4));
      } else {
        seed = (seed * 9301 + 49297) % 233280;
        cells.push(seed / 233280 > 0.5);
      }
    }
  }
  return cells;
}

/** "Twenty-six thousand six hundred ninety-four birr and ninety-five cents" */
export function amountInWords(n: number): string {
  const a = ["", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"];
  const t = ["", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"];
  const hundreds = (x: number) => {
    let r = "";
    if (x >= 100) {
      r += a[Math.floor(x / 100)] + " hundred";
      x %= 100;
      if (x) r += " ";
    }
    if (x >= 20) {
      r += t[Math.floor(x / 10)];
      if (x % 10) r += "-" + a[x % 10];
    } else if (x) r += a[x];
    return r;
  };
  const whole = (x: number) => {
    if (!x) return "zero";
    const p: string[] = [];
    if (x >= 1e6) {
      p.push(hundreds(Math.floor(x / 1e6)) + " million");
      x %= 1e6;
    }
    if (x >= 1000) {
      p.push(hundreds(Math.floor(x / 1000)) + " thousand");
      x %= 1000;
    }
    if (x) p.push(hundreds(x));
    return p.join(" ");
  };
  const rounded = Math.round(n * 100) / 100;
  const birr = Math.floor(rounded);
  const cents = Math.round((rounded - birr) * 100);
  const s = whole(birr) + " birr" + (cents ? " and " + whole(cents) + " cents" : "");
  return s.charAt(0).toUpperCase() + s.slice(1);
}

// ─── Formatting ────────────────────────────────────────────────────────────

const MONTHS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

export const fmt2 = (n: number) => n.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
export const etb = (n: number) => `ETB ${fmt2(n)}`;

/** "24 Sep 2026, 10:42" */
export function fmtIssued(date: string, time: string): string {
  const [y, m, d] = date.split("-");
  return `${+d} ${MONTHS[+m - 1]} ${y}, ${time}`;
}

function fmtDateTime(d: Date): string {
  return `${d.getDate()} ${MONTHS[d.getMonth()]} ${d.getFullYear()}, ${d.toTimeString().slice(0, 5)}`;
}

function isoDate(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}
