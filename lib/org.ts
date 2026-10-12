// Settings data for the console: account, branches, source systems, API keys,
// MoR credentials and the seller profile. Like lib/merchant.ts these are mocks
// with sample data — swap the async bodies for real backend calls.
//
// Hierarchy: Branch → Source system → API key. A source system belongs to one
// branch; an API key is bound to one source system and can never do more than
// that system and its branch allow.

import { CURRENT_USER, SELLER } from "./merchant";

// ─── Vocabularies ──────────────────────────────────────────────────────────

export const TX_TYPES = ["B2C", "B2B", "B2G", "G2B"];
export const DOC_TYPES = ["INV", "CRE", "DEB", "RTN", "INT", "FIN", "RCT", "WHT"];
export const NATURES = ["GOODS", "SERVICE"];
export const OPERATIONS = ["Single Register", "Bulk Register", "Single Cancel", "Bulk Cancel", "Receipt Issue", "Withholding Issue"];
export const PERMISSIONS = ["Issue invoices", "Cancel invoices", "Issue receipts", "Issue withholding", "View reports", "Manage items"];
/** Tax types a source system or key can be granted. EXCISE is derived from the merchant, never picked. */
export const SYSTEM_TAX = ["VAT0", "VAT15", "VATEX"];
export const EXCISE = "EXCISE";
export const SYSTEM_TYPES = ["POS", "ERP", "E-COMMERCE", "MANUAL"];

export const ADDRESS_FIELDS = [
  ["city", "City"],
  ["subCity", "Sub-city"],
  ["wereda", "Wereda"],
  ["kebele", "Kebele"],
  ["house", "House number"],
  ["locality", "Locality"],
  ["zone", "Zone"],
  ["region", "Region"],
  ["country", "Country"],
] as const;

export type AddressField = (typeof ADDRESS_FIELDS)[number][0];
export type Address = Record<AddressField, string>;

// ─── Types ─────────────────────────────────────────────────────────────────

export type Verification = "Verified" | "Failed" | "Unverified";

export type OrgBranch = {
  /** Existing branches use the console's branch key (main / bole / piassa). */
  id: string;
  name: string;
  code: string;
  main?: boolean;
  active: boolean;
  manager: string;
  address: Address;
  /** Business rules — the ceiling for every source system, key and user below. */
  tt: string[];
  dt: string[];
  ns: string[];
  ops: string[];
  /** Access ceiling for per-user grants. */
  perms: string[];
};

export type SourceSystem = {
  /** The system number MoR knows it by; printed on documents. */
  id: string;
  label: string;
  type: string;
  branchId: string;
  active: boolean;
  ver: Verification;
  verAt: string;
  tax: string[];
  cashier: string;
  sales: string;
  lastDoc: string;
};

export type ApiKey = {
  id: string;
  label: string;
  systemId: string;
  active: boolean;
  created: string;
  ops: string[];
  tt: string[];
  tax: string[];
  /** Last four characters; the full key is shown once at creation. */
  tail: string;
  users: string[];
};

export type Session = { id: string; device: string; fingerprint: string; where: string; last: string; current?: boolean };

export type Org = {
  staff: string[];
  branches: OrgBranch[];
  systems: SourceSystem[];
  keys: ApiKey[];
  sessions: Session[];
  /** Merchant-wide excise eligibility. When on, EXCISE is merged into every source system. */
  excise: boolean;
};

export type Credentials = { clientId: string; tin: string; secret: string; apiKey: string; env: "DEV" | "PROD" };

export type SellerProfile = {
  tin: string;
  legal: string;
  vat: string;
  email: string;
  phone: string;
  region: string;
  city: string;
  wereda: string;
  subCity: string;
  zone: string;
  kebele: string;
  locality: string;
  house: string;
  country: string;
};

// ─── Sample data ───────────────────────────────────────────────────────────

const addr = (city: string, subCity: string, wereda: string, kebele: string, house: string, locality: string, zone: string, region: string): Address => ({
  city,
  subCity,
  wereda,
  kebele,
  house,
  locality,
  zone,
  region,
  country: "Ethiopia",
});

export const INITIAL_ORG: Org = {
  staff: [CURRENT_USER.name, "Hana Tesfaye", "Dawit Mekonnen", "Selam Girma", "Meron Alemu"],
  branches: [
    {
      id: "main", name: "Main Branch", code: "BR-001", main: true, active: true, manager: CURRENT_USER.name,
      address: addr("Addis Ababa", "Kirkos", "13", "05", "412", "Kazanchis", "Zone 1", "Addis Ababa"),
      tt: ["B2C", "B2B", "B2G"], dt: ["INV", "CRE", "DEB", "RTN", "INT", "FIN", "RCT", "WHT"], ns: ["GOODS", "SERVICE"],
      ops: [...OPERATIONS], perms: [...PERMISSIONS],
    },
    {
      id: "bole", name: "Bole", code: "BR-002", active: true, manager: "Hana Tesfaye",
      address: addr("Addis Ababa", "Bole", "03", "14", "88", "Atlas", "Zone 1", "Addis Ababa"),
      tt: ["B2C", "B2B"], dt: ["INV", "CRE", "DEB", "RCT"], ns: ["GOODS", "SERVICE"],
      ops: ["Single Register", "Single Cancel", "Receipt Issue"], perms: ["Issue invoices", "Cancel invoices", "Issue receipts", "View reports"],
    },
    {
      id: "piassa", name: "Piassa", code: "BR-003", active: true, manager: "Dawit Mekonnen",
      address: addr("Addis Ababa", "Arada", "01", "06", "17", "Piassa", "Zone 1", "Addis Ababa"),
      tt: ["B2C"], dt: ["INV", "CRE", "RCT"], ns: ["GOODS"],
      ops: ["Single Register", "Receipt Issue"], perms: ["Issue invoices", "Issue receipts"],
    },
  ],
  systems: [
    {
      id: "2EC77ABF63", label: "Front Counter POS", type: "POS", branchId: "main", active: true, ver: "Verified",
      verAt: "Verified 9 Oct 2026, 14:02 · HSN 8471", tax: ["VAT15", "VAT0"], cashier: "Selam Girma", sales: CURRENT_USER.name, lastDoc: "INV-104",
    },
    {
      id: "7B3D91E0A4", label: "Odoo ERP", type: "ERP", branchId: "main", active: true, ver: "Verified",
      verAt: "Verified 2 Sep 2026, 10:41 · HSN 3004", tax: ["VAT15", "VATEX"], cashier: "", sales: "Meron Alemu", lastDoc: "INV-101",
    },
    {
      id: "5054087CDF", label: "Bole Till 1", type: "POS", branchId: "bole", active: true, ver: "Verified",
      verAt: "Verified 21 Sep 2026, 08:15 · HSN 9401", tax: ["VAT15", "VAT0"], cashier: "Hana Tesfaye", sales: "", lastDoc: "CRE-098",
    },
    {
      id: "9A1C44B0E2", label: "Piassa Till 2", type: "POS", branchId: "piassa", active: true, ver: "Failed",
      verAt: "Failed 11 Oct 2026, 09:12 · HSN 8471 rejected by MoR", tax: ["VAT15"], cashier: "Dawit Mekonnen", sales: "", lastDoc: "INV-097",
    },
  ],
  keys: [
    {
      id: "key-till1", label: "Front Counter · Till 1", systemId: "2EC77ABF63", active: true, created: "14 Aug 2026",
      ops: ["Single Register", "Single Cancel", "Receipt Issue"], tt: ["B2C", "B2B"], tax: ["VAT15", "VAT0"], tail: "3f2a", users: ["Selam Girma", CURRENT_USER.name],
    },
    {
      id: "key-odoo", label: "Odoo nightly sync", systemId: "7B3D91E0A4", active: true, created: "2 Sep 2026",
      ops: ["Bulk Register", "Bulk Cancel", "Withholding Issue"], tt: ["B2B", "B2G"], tax: ["VAT15", "VATEX"], tail: "91c0", users: [CURRENT_USER.name],
    },
    {
      id: "key-piassa", label: "Piassa Till 2", systemId: "9A1C44B0E2", active: true, created: "20 Sep 2026",
      ops: ["Single Register", "Receipt Issue"], tt: ["B2C"], tax: ["VAT15"], tail: "b7e4", users: ["Dawit Mekonnen", "Meron Alemu"],
    },
    {
      id: "key-odoo-test", label: "Odoo test key", systemId: "7B3D91E0A4", active: false, created: "3 Jun 2026",
      ops: ["Single Register"], tt: ["B2B"], tax: ["VAT15"], tail: "0d18", users: [],
    },
  ],
  sessions: [
    { id: "x1", device: "Chrome · macOS", fingerprint: "fp_9c1e…a07b", where: "Addis Ababa", last: "Active now", current: true },
    { id: "x2", device: "EIRSPay iOS · iPhone 14", fingerprint: "fp_31d0…e5c2", where: "Addis Ababa", last: "2 hours ago" },
    { id: "x3", device: "EIRSPay POS · Tablet", fingerprint: "fp_77ab…0f19", where: "Main Branch", last: "Yesterday, 18:40" },
    { id: "x4", device: "Firefox · Windows", fingerprint: "fp_c4e8…9d31", where: "Bole", last: "28 Aug 2026" },
  ],
  excise: false,
};

export const INITIAL_CREDENTIALS: Credentials = {
  clientId: "8d7fa3ed-c649-4efb-ba9d-9792db3a3476",
  tin: SELLER.tin ?? "",
  secret: "cs_4f9a1d7e22b0c8a5e61f3d90b7c2a4e8",
  apiKey: "ak_9be20c7714fa3d58a0e6c1b2f9d4e7a3",
  env: "DEV",
};

export const INITIAL_SELLER: SellerProfile = {
  tin: SELLER.tin ?? "",
  legal: SELLER.name,
  vat: SELLER.vat ?? "",
  email: SELLER.email ?? "",
  phone: SELLER.phone ?? "",
  region: "Addis Ababa",
  city: "Addis Ababa",
  wereda: "13",
  subCity: "Kirkos",
  zone: "Zone 1",
  kebele: "05",
  locality: "Kazanchis",
  house: "412",
  country: "ET",
};

/** The signed-in user's identity card on Settings › Account. */
export const ACCOUNT = {
  firstName: CURRENT_USER.name.split(" ")[0],
  lastName: CURRENT_USER.name.split(" ").slice(1).join(" "),
  email: CURRENT_USER.email,
  phone: "+251 921 280 809",
  type: CURRENT_USER.role.toUpperCase() as "OWNER" | "STAFF",
};

export type SecurityEvent = { kind: "in" | "bad" | "out" | "reset"; event: string; meta: string; when: string };

export const SECURITY_ACTIVITY: SecurityEvent[] = [
  { kind: "in", event: "Signed in", meta: "Chrome · macOS · Addis Ababa", when: "2 min ago" },
  { kind: "bad", event: "Failed sign-in attempt", meta: "Wrong password · 196.188.4.x", when: "Yesterday, 22:14" },
  { kind: "in", event: "Signed in", meta: "EIRSPay iOS · iPhone 14", when: "Yesterday, 08:30" },
  { kind: "out", event: "Signed out", meta: "EIRSPay POS · Tablet", when: "9 Oct 2026" },
  { kind: "reset", event: "Password reset", meta: "Via email link", when: "30 Aug 2026" },
  { kind: "bad", event: "3 failed attempts", meta: "Sign-in paused for 15 minutes", when: "27 Aug 2026" },
  { kind: "out", event: "Signed out", meta: "Firefox · Windows", when: "26 Aug 2026" },
];

// ─── Derived values ────────────────────────────────────────────────────────

/** A system's tax types with EXCISE merged in when the merchant has excise enabled. */
export const systemTax = (org: Pick<Org, "excise">, s: Pick<SourceSystem, "tax">) => (org.excise ? [...s.tax, EXCISE] : s.tax);

export const plural = (n: number, word: string, many = `${word}s`) => `${n} ${n === 1 ? word : many}`;

export const maskedKey = (tail: string) => `eirs_live_••••••••${tail}`;

export const initials = (name: string) =>
  name
    .split(" ")
    .map((w) => w[0])
    .join("")
    .slice(0, 2);

/** "12 Oct 2026" */
export function today(): string {
  const d = new Date();
  return `${d.getDate()} ${d.toLocaleString("en-US", { month: "short" })} ${d.getFullYear()}`;
}

// ─── Mock API ──────────────────────────────────────────────────────────────

const delay = (ms: number) => new Promise((r) => setTimeout(r, ms));

/** Issues a key; the full secret comes back exactly once. */
export async function createApiKey(): Promise<{ id: string; secret: string }> {
  await delay(300);
  const h = () => Math.random().toString(16).slice(2, 10).padEnd(8, "0");
  return { id: `key-${Date.now().toString(36)}`, secret: `eirs_live_${h()}${h()}${h()}${h()}` };
}

/** Sends a zero-value test document for one source system. */
export async function verifySourceSystem(id: string, hsn: string): Promise<Pick<SourceSystem, "ver" | "verAt">> {
  await delay(1200);
  void id;
  return { ver: "Verified", verAt: `Verified just now · HSN ${hsn.trim() || "8471"}` };
}

/** Sends the signed probe invoice built from credentials, seller profile and source system (~1.6s). */
export async function runSetupVerification(): Promise<boolean> {
  await delay(1600);
  return true;
}

// ─── New records ───────────────────────────────────────────────────────────

const hex = (n: number) =>
  Array.from({ length: n }, () => "0123456789ABCDEF"[Math.floor(Math.random() * 16)]).join("");

export function blankBranch(count: number): OrgBranch {
  return {
    id: `br-${Date.now().toString(36)}`,
    name: "New branch",
    code: `BR-${String(count + 1).padStart(3, "0")}`,
    active: true,
    manager: "",
    address: { city: "", subCity: "", wereda: "", kebele: "", house: "", locality: "", zone: "", region: "", country: "Ethiopia" },
    tt: ["B2C"],
    dt: ["INV", "RCT"],
    ns: ["GOODS"],
    ops: ["Single Register", "Receipt Issue"],
    perms: ["Issue invoices", "Issue receipts"],
  };
}

export function blankSystem(branchId: string): SourceSystem {
  return {
    id: hex(10),
    label: "New source system",
    type: "POS",
    branchId,
    active: true,
    ver: "Unverified",
    verAt: "Not verified yet",
    tax: ["VAT15"],
    cashier: "",
    sales: "",
    lastDoc: "—",
  };
}
