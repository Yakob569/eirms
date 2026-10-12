// Merchant API boundary for the console. Like lib/auth.ts these are mocks with
// sample data from the design — swap the bodies of the async functions (and the
// sample arrays) for real backend calls; the screens only depend on the types
// and signatures below.

export type Status = "ISSUED" | "PAID" | "WITHHELD" | "CANCELED";
export type DocType = "INV" | "CRE" | "DEB" | "RTN" | "INT" | "FIN";
/** Document types the standalone Register invoice page issues. */
export type RegisterDocType = "INV" | "CRE" | "DEB";
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
  /** Credit / debit notes: why, and the invoice being amended. Interim / final invoices: the retainer they draw on. */
  amends?: { reason: string; previousId: string };
};

// ─── Reference data ────────────────────────────────────────────────────────

export const TAX_RATES: Record<TaxCode, { rate: number; label: string }> = {
  VAT15: { rate: 0.15, label: "VAT 15%" },
  VAT0: { rate: 0, label: "VAT 0%" },
  EXC10: { rate: 0.1, label: "Excise 10%" },
};

export const TAX_CODES: TaxCode[] = ["VAT15", "VAT0", "EXC10"];

export const DOC_NAMES: Record<DocType, string> = {
  INV: "Invoice",
  CRE: "Credit note",
  DEB: "Debit note",
  RTN: "Retainer",
  INT: "Interim invoice",
  FIN: "Final invoice",
};

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
  {
    id: "RTN-H7G6F5D4SA", number: "095", docType: "RTN", transactionType: "B2B", status: "ISSUED",
    date: "2026-09-20", time: "09:40", branch: "Main Branch", sourceSystem: "2EC77ABF63",
    buyer: { name: "Abyssinia Consulting PLC", tin: "0072214598", vat: "4410098812", phone: "+251116674400", email: "accounts@abyssiniaconsult.et", addr: "Addis Ababa · Wereda 08" },
    payment: { mode: "BANK TRANSFER", term: "NET 30" },
    items: [item("Retainer, Q4 POS support", "SRV-0200", "Service", "JOB", 1, 45000, "VAT15")],
  },
];

const delay = (ms: number) => new Promise((r) => setTimeout(r, ms));

/** All registered documents, including canceled ones, newest first. */
export async function listInvoices(): Promise<Invoice[]> {
  return SAMPLE_INVOICES;
}

/** Synchronous copy of the sample list, used to seed client state without a loading flash. */
export const INITIAL_INVOICES = SAMPLE_INVOICES;

/** One editable line in the item builder (Register invoice, interim / final invoice panel). */
export type DraftLine = {
  itemCode: string;
  description: string;
  nature: LineItem["nature"];
  unit: string;
  quantity: string;
  unitPrice: string;
  taxCode: TaxCode;
};

export const emptyDraftLine = (): DraftLine => ({ itemCode: "", description: "", nature: "Goods", unit: "PCS", quantity: "1", unitPrice: "", taxCode: "VAT15" });

/** Priced lines become line items; blank codes get a placeholder. */
export const draftLineItems = (lines: DraftLine[]): LineItem[] =>
  lines
    .filter((l) => l.description.trim() && parseFloat(l.unitPrice) > 0)
    .map((l, i) =>
      item(l.description.trim(), l.itemCode.trim() || `ITM-${900 + i}`, l.nature, l.unit.trim() || "PCS", parseFloat(l.quantity) || 1, parseFloat(l.unitPrice), l.taxCode),
    );

export type InvoiceDraft = {
  docType: RegisterDocType;
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
  lines: DraftLine[];
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
    items: draftLineItems(draft.lines),
  };
}

/** What a credit / debit note or an interim / final invoice carries; the rest comes from MoR. */
export type DocumentRequest = Pick<Invoice, "docType" | "transactionType" | "branch" | "sourceSystem" | "buyer" | "payment" | "items" | "amends">;

/** Registers a document raised from an existing one (row actions on the invoice list). */
export async function registerDocument(req: DocumentRequest, ctx: { nextNumber: string }): Promise<Invoice> {
  await delay(400);
  const now = new Date();
  return {
    ...req,
    id: `${req.docType}-NEW${Date.now().toString(36).toUpperCase()}`,
    number: ctx.nextNumber,
    status: "ISSUED",
    date: isoDate(now),
    time: now.toTimeString().slice(0, 5),
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

// ─── Items catalog ─────────────────────────────────────────────────────────

export type Nature = LineItem["nature"];

export type CatalogItem = {
  code: string;
  description: string;
  nature: Nature;
  /** Harmonization (HS) code — required for goods. */
  hsCode: string;
  taxCode: TaxCode;
  unit: string;
  /** Excluding tax. */
  unitPrice: number;
  active: boolean;
  category: string;
};

export const UNITS = ["PCS", "KG", "L", "BOX", "CASE", "HRS", "JOB"];

export const TAX_CODE_NAMES: Record<TaxCode, string> = { VAT15: "Standard VAT 15%", VAT0: "Zero-rated", EXC10: "Excise 10%" };

const cat = (
  code: string,
  description: string,
  nature: Nature,
  hsCode: string,
  taxCode: TaxCode,
  unit: string,
  unitPrice: number,
  active: boolean,
  category: string,
): CatalogItem => ({ code, description, nature, hsCode, taxCode, unit, unitPrice, active, category });

const SAMPLE_CATALOG: CatalogItem[] = [
  cat("ITM-0012", "Laptop stand, aluminium", "Goods", "7616.99.90", "VAT15", "PCS", 23213, true, "Electronics"),
  cat("ITM-0031", "Wireless keyboard", "Goods", "8471.60.00", "VAT15", "PCS", 1678, true, "Electronics"),
  cat("ITM-0044", "Monitor arm", "Goods", "8473.30.00", "VAT15", "PCS", 1500, true, "Electronics"),
  cat("ITM-0210", "Cat6 cable, 305m box", "Goods", "8544.49.00", "VAT15", "BOX", 1200, true, "Electronics"),
  cat("ITM-0301", "Ergonomic office chair", "Goods", "9401.30.00", "VAT15", "PCS", 1800, true, "Furniture"),
  cat("ITM-0508", "Yirgacheffe coffee beans, 1kg", "Goods", "0901.11.00", "VAT15", "KG", 650, true, "Grocery"),
  cat("ITM-0612", "Printed notebooks", "Goods", "4820.10.00", "VAT0", "PCS", 350, true, "Stationery"),
  cat("ITM-0701", "Thermal receipt paper rolls", "Goods", "4811.90.00", "VAT15", "PCS", 85, true, "Stationery"),
  cat("ITM-0815", "Imported soft drinks, case", "Goods", "2202.10.00", "EXC10", "CASE", 150, true, "Beverages"),
  cat("SRV-0009", "Delivery and assembly", "Service", "", "VAT0", "JOB", 2300, true, "Services"),
  cat("SRV-0102", "Network cabling installation", "Service", "", "VAT15", "JOB", 30000, true, "Services"),
  cat("SRV-0110", "POS integration consulting", "Service", "", "VAT15", "HRS", 1250, true, "Services"),
  cat("ITM-0099", "Fax machine toner", "Goods", "8443.99.00", "VAT15", "PCS", 2400, false, "Electronics"),
];

export async function listItems(): Promise<CatalogItem[]> {
  return SAMPLE_CATALOG;
}

export const INITIAL_CATALOG = SAMPLE_CATALOG;

/** Creates or updates a catalog item. Registered documents keep their original lines. */
export async function saveItem(item: CatalogItem): Promise<CatalogItem> {
  await delay(200);
  return item;
}

export const grossPrice = (it: Pick<CatalogItem, "unitPrice" | "taxCode">) => it.unitPrice * (1 + TAX_RATES[it.taxCode].rate);

// ─── Receipts ──────────────────────────────────────────────────────────────

export type ReceiptType = "SALES" | "PAYMENT" | "ADVANCE" | "REFUND";
export type PaymentMethod = "CASH" | "CHEQUE" | "BANK_TRANSFER" | "MOBILE_MONEY" | "CARD";

export type Receipt = {
  id: string;
  number: string;
  receiptType: ReceiptType;
  paymentMethod: PaymentMethod;
  /** Mobile-money provider (telebirr, M-Pesa…). */
  provider?: string;
  /** Cheque no., transfer reference, mobile-money transaction ID or masked card. */
  reference?: string;
  /** Cash handed over, for change due. */
  tendered?: number;
  /** Payment / refund receipts: amount against the linked invoice. */
  amount?: number;
  linkedInvoiceId?: string;
  date: string;
  time: string;
  branch: BranchName;
  sourceSystem: string;
  /** Omitted on linked receipts — the buyer comes from the invoice. */
  buyer?: Party;
  /** Sales and advance receipts carry their own lines. */
  items?: LineItem[];
};

export const RECEIPT_TYPES: { code: ReceiptType; label: string; desc: string }[] = [
  { code: "SALES", label: "Sales", desc: "Paid at the counter for goods or services" },
  { code: "PAYMENT", label: "Payment", desc: "Settles an invoice issued on credit" },
  { code: "ADVANCE", label: "Advance", desc: "Deposit before goods or services are delivered" },
  { code: "REFUND", label: "Refund", desc: "Money returned against an earlier invoice" },
];

export const RECEIPT_TYPE_LABEL = Object.fromEntries(RECEIPT_TYPES.map((t) => [t.code, t.label])) as Record<ReceiptType, string>;

export const PAYMENT_METHODS: { code: PaymentMethod; label: string }[] = [
  { code: "CASH", label: "Cash" },
  { code: "CHEQUE", label: "Cheque" },
  { code: "BANK_TRANSFER", label: "Bank transfer" },
  { code: "MOBILE_MONEY", label: "Mobile money" },
  { code: "CARD", label: "Card" },
];

export const PAYMENT_LABEL = Object.fromEntries(PAYMENT_METHODS.map((m) => [m.code, m.label])) as Record<PaymentMethod, string>;

export const MOBILE_PROVIDERS = ["telebirr", "M-Pesa", "CBE Birr", "HelloCash"];

/** Sales and advance receipts list items; payment and refund receipts point at an invoice. */
export const receiptHasItems = (t: ReceiptType) => t === "SALES" || t === "ADVANCE";

const WALK_IN: Party = { name: "Walk-in Customer" };

const SAMPLE_RECEIPTS: Receipt[] = [
  {
    id: "RCP-A2291", number: "2291", receiptType: "SALES", paymentMethod: "CASH", tendered: 1000,
    date: "2026-09-24", time: "10:31", branch: "Bole", sourceSystem: "5054087CDF", buyer: WALK_IN,
    items: [
      item("Yirgacheffe coffee beans, 1kg", "ITM-0508", "Goods", "KG", 1, 650, "VAT15"),
      item("Printed notebooks", "ITM-0612", "Goods", "PCS", 1, 100, "VAT0"),
    ],
  },
  {
    id: "RCP-A2290", number: "2290", receiptType: "SALES", paymentMethod: "MOBILE_MONEY", provider: "telebirr", reference: "TB7Q2K91XZ",
    date: "2026-09-24", time: "08:47", branch: "Piassa", sourceSystem: "9A1C44B0E2", buyer: WALK_IN,
    items: [
      item("Imported soft drinks, case", "ITM-0815", "Goods", "CASE", 2, 150, "EXC10"),
      item("Thermal receipt paper rolls", "ITM-0701", "Goods", "PCS", 1, 85, "VAT15"),
    ],
  },
  {
    id: "RCP-A2289", number: "2289", receiptType: "PAYMENT", paymentMethod: "BANK_TRANSFER", reference: "FT26267AB91C", linkedInvoiceId: "INV-B3N5M7Q9WE",
    date: "2026-09-23", time: "14:36", branch: "Bole", sourceSystem: "5054087CDF",
  },
  {
    id: "RCP-A2288", number: "2288", receiptType: "SALES", paymentMethod: "CARD", reference: "•••• 4417",
    date: "2026-09-23", time: "10:15", branch: "Bole", sourceSystem: "5054087CDF", buyer: { name: "Hanna Tesfaye", phone: "+251911238840" },
    items: [item("Wireless keyboard", "ITM-0031", "Goods", "PCS", 1, 1678, "VAT15")],
  },
  {
    id: "RCP-A2287", number: "2287", receiptType: "ADVANCE", paymentMethod: "CHEQUE", reference: "CBE 00482913",
    date: "2026-09-22", time: "16:02", branch: "Main Branch", sourceSystem: "2EC77ABF63", buyer: { name: "Zemen Bank S.C", tin: "0005581342" },
    items: [item("Advance: network cabling, phase 1", "SRV-0102", "Service", "JOB", 1, 15000, "VAT15")],
  },
  {
    id: "RCP-A2286", number: "2286", receiptType: "REFUND", paymentMethod: "CASH", amount: 747.5, linkedInvoiceId: "INV-P0O9I8U7YT",
    date: "2026-09-22", time: "15:20", branch: "Piassa", sourceSystem: "9A1C44B0E2",
  },
  {
    id: "RCP-A2285", number: "2285", receiptType: "PAYMENT", paymentMethod: "CHEQUE", reference: "Awash 11029384", linkedInvoiceId: "INV-Z8X6C4V2BN",
    date: "2026-09-24", time: "11:40", branch: "Main Branch", sourceSystem: "2EC77ABF63",
  },
];

export async function listReceipts(): Promise<Receipt[]> {
  return SAMPLE_RECEIPTS;
}

export const INITIAL_RECEIPTS = SAMPLE_RECEIPTS;

export type ReceiptDraft = {
  receiptType: ReceiptType;
  paymentMethod: PaymentMethod;
  /** Catalog lines (sales / advance). */
  lines: { code: string; quantity: number }[];
  /** Linked invoice (payment / refund). */
  linkedInvoiceId: string | null;
  amount: string;
  walkIn: boolean;
  name: string;
  /** Phone, or a 10-digit TIN. */
  phoneOrTin: string;
  reference: string;
  provider: string;
  tendered: string;
};

/** Submits a receipt to MoR; resolves with the signed document. */
export async function registerReceipt(
  draft: ReceiptDraft,
  ctx: { branch: BranchName; nextNumber: string; catalog: CatalogItem[]; total: number },
): Promise<Receipt> {
  await delay(400);
  const now = new Date();
  const withItems = receiptHasItems(draft.receiptType);
  const byCode = new Map(ctx.catalog.map((c) => [c.code, c]));
  const id = draft.phoneOrTin.trim();
  return {
    id: `RCP-N${Date.now().toString(36).toUpperCase()}`,
    number: ctx.nextNumber,
    receiptType: draft.receiptType,
    paymentMethod: draft.paymentMethod,
    provider: draft.paymentMethod === "MOBILE_MONEY" ? draft.provider : undefined,
    reference: draft.reference.trim() || undefined,
    tendered: draft.paymentMethod === "CASH" ? parseFloat(draft.tendered) || ctx.total : undefined,
    amount: withItems ? undefined : ctx.total,
    linkedInvoiceId: withItems ? undefined : draft.linkedInvoiceId ?? undefined,
    date: isoDate(now),
    time: now.toTimeString().slice(0, 5),
    branch: ctx.branch,
    sourceSystem: "2EC77ABF63",
    buyer: withItems
      ? draft.walkIn
        ? WALK_IN
        : /^\d{10}$/.test(id)
          ? { name: draft.name.trim(), tin: id }
          : { name: draft.name.trim(), phone: id || undefined }
      : undefined,
    items: withItems
      ? draft.lines.map((l) => {
          const c = byCode.get(l.code)!;
          return item(c.description, c.code, c.nature, c.unit, l.quantity, c.unitPrice, c.taxCode);
        })
      : undefined,
  };
}

// ─── Withholding ───────────────────────────────────────────────────────────

export type WithholdingRecord = {
  /** Certificate number. */
  id: string;
  invoiceId: string;
  rate: number;
  date: string;
  period: string;
  certificateReceived: boolean;
};

const SAMPLE_WITHHOLDING: WithholdingRecord[] = [
  { id: "WHT-2026-004417", invoiceId: "INV-Z8X6C4V2BN", rate: 0.02, date: "2026-09-24", period: "Sep 2026", certificateReceived: true },
  { id: "WHT-2026-004392", invoiceId: "INV-R4T6Y8U1IO", rate: 0.02, date: "2026-09-24", period: "Sep 2026", certificateReceived: false },
  { id: "WHT-2026-004310", invoiceId: "INV-B3N5M7Q9WE", rate: 0.02, date: "2026-09-23", period: "Sep 2026", certificateReceived: true },
  { id: "WHT-2026-004188", invoiceId: "INV-L5K4J3H2GF", rate: 0.02, date: "2026-09-21", period: "Sep 2026", certificateReceived: false },
];

export async function listWithholding(): Promise<WithholdingRecord[]> {
  return SAMPLE_WITHHOLDING;
}

export const INITIAL_WITHHOLDING = SAMPLE_WITHHOLDING;

export async function markCertificateReceived(id: string): Promise<void> {
  await delay(200);
  void id;
}

/** Withheld on the taxable base (excl. VAT). */
export const withheldAmount = (w: WithholdingRecord, inv: Pick<Invoice, "items">) => totals(inv).subtotal * w.rate;

/**
 * What a receipt is worth. Item receipts total their lines; linked receipts use
 * their amount, or default to the invoice total less anything the buyer withheld.
 */
export function receiptTotals(
  r: Receipt,
  getInvoice: (id: string) => Invoice | undefined,
  withholding: WithholdingRecord[],
): Totals & { invoice?: Invoice; withholding?: WithholdingRecord; withheld: number } {
  if (r.items) return { ...totals({ items: r.items }), withheld: 0 };
  const invoice = r.linkedInvoiceId ? getInvoice(r.linkedInvoiceId) : undefined;
  const w = r.receiptType === "PAYMENT" ? withholding.find((x) => x.invoiceId === r.linkedInvoiceId) : undefined;
  const withheld = w && invoice ? withheldAmount(w, invoice) : 0;
  const amt = r.amount ?? (invoice ? totals(invoice).grandTotal - withheld : 0);
  return { lines: [], subtotal: amt, taxTotal: 0, grandTotal: amt, taxes: [], invoice, withholding: w, withheld };
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

/** "24 Sep 2026" */
export function fmtDay(date: string): string {
  const [y, m, d] = date.split("-");
  return `${+d} ${MONTHS[+m - 1]} ${y}`;
}

function fmtDateTime(d: Date): string {
  return `${d.getDate()} ${MONTHS[d.getMonth()]} ${d.getFullYear()}, ${d.toTimeString().slice(0, 5)}`;
}

function isoDate(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}
