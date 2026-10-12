"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import type { RangeKey } from "@/lib/dashboard";
import type { BranchKey, Cancellation, CatalogItem, Invoice, Receipt, WithholdingRecord } from "@/lib/merchant";
import { INITIAL_CATALOG, INITIAL_INVOICES, INITIAL_RECEIPTS, INITIAL_WITHHOLDING, readiness } from "@/lib/merchant";
import type { Credentials, Org, SellerProfile } from "@/lib/org";
import { INITIAL_CREDENTIALS, INITIAL_ORG, INITIAL_SELLER, runSetupVerification } from "@/lib/org";

/** Minimum time the see-through loader stays up while talking to MoR. */
export const WORK_MS = 1700;

type Persisted = {
  branch: BranchKey;
  range: RangeKey;
  collapsed: boolean;
  /** Documents registered in this session (the mock API has no store). */
  created: Invoice[];
  /** Cancellations recorded in this session, by invoice id. */
  cancels: Record<string, Cancellation>;
  /** Receipts registered in this session. */
  createdReceipts: Receipt[];
  /** The items catalog, including edits made in this session. */
  catalog: CatalogItem[];
  /** Certificates marked received in this session, by withholding id. */
  certs: Record<string, boolean>;
  /** Settings: branches, source systems, API keys, sessions and merchant excise. */
  org: Org;
  cred: Credentials;
  seller: SellerProfile;
  /** Setup verification passed in this session (completes setup, unlocks API keys). */
  verified: boolean;
};

type ConsoleValue = Persisted & {
  /** False until session state has been restored — detail pages wait for it. */
  ready: boolean;
  /** Loader message while a MoR call is in flight; the shell renders it over the content column. */
  busy: { message: string; detail: string } | null;
  invoices: Invoice[];
  getInvoice: (id: string) => Invoice | undefined;
  /** Receipts, newest first; linked receipts carry the invoice's buyer. */
  receipts: ConsoleReceipt[];
  getReceipt: (id: string) => ConsoleReceipt | undefined;
  withholding: WithholdingRecord[];
  setupDone: number;
  setBranch: (b: BranchKey) => void;
  setRange: (r: RangeKey) => void;
  toggleSidebar: () => void;
  addInvoice: (inv: Invoice) => void;
  markCanceled: (id: string, c: Cancellation) => void;
  addReceipt: (r: Receipt) => void;
  /** Replaces the item with the same code, or adds it at the top. */
  putItem: (item: CatalogItem) => void;
  markCertificate: (id: string) => void;
  updateOrg: (fn: (o: Org) => Org) => void;
  setCred: (c: Credentials) => void;
  setSeller: (p: SellerProfile) => void;
  /** True while the setup probe is in flight. */
  verifying: boolean;
  /** Runs the setup probe (~1.6s); passing completes setup. */
  runVerification: () => void;
  /** A key secret created this session, shown once on that key's page. */
  reveal: { id: string; secret: string } | null;
  setReveal: (r: { id: string; secret: string } | null) => void;
  /** Shows the see-through loader while `task` runs (at least WORK_MS). */
  work: <T>(message: string, detail: string, task: () => Promise<T>) => Promise<T>;
};

/** A receipt with its buyer resolved (linked receipts take the invoice's buyer). */
export type ConsoleReceipt = Receipt & { buyer: NonNullable<Receipt["buyer"]> };

const STORAGE_KEY = "eirs.console";
const initial: Persisted = {
  branch: "all",
  range: "d7",
  collapsed: false,
  created: [],
  cancels: {},
  createdReceipts: [],
  catalog: INITIAL_CATALOG,
  certs: {},
  org: INITIAL_ORG,
  cred: INITIAL_CREDENTIALS,
  seller: INITIAL_SELLER,
  verified: false,
};

const ConsoleContext = createContext<ConsoleValue | null>(null);

export function ConsoleProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<Persisted>(initial);
  const [ready, setReady] = useState(false);
  const [busy, setBusy] = useState<{ message: string; detail: string } | null>(null);
  const [verifying, setVerifying] = useState(false);
  const [reveal, setReveal] = useState<{ id: string; secret: string } | null>(null);

  // Restore across reloads within the tab.
  useEffect(() => {
    try {
      const saved = sessionStorage.getItem(STORAGE_KEY);
      if (saved) setState({ ...initial, ...JSON.parse(saved) });
    } catch {}
    setReady(true);
  }, []);

  const patch = useCallback((p: Partial<Persisted> | ((s: Persisted) => Partial<Persisted>)) => {
    setState((s) => {
      const next = { ...s, ...(typeof p === "function" ? p(s) : p) };
      try {
        sessionStorage.setItem(STORAGE_KEY, JSON.stringify(next));
      } catch {}
      return next;
    });
  }, []);

  const invoices = useMemo(
    () =>
      [...state.created, ...INITIAL_INVOICES].map((v) =>
        state.cancels[v.id] ? { ...v, status: "CANCELED" as const, originalStatus: v.status, cancel: state.cancels[v.id] } : v,
      ),
    [state.created, state.cancels],
  );

  const receipts = useMemo(
    () =>
      [...state.createdReceipts, ...INITIAL_RECEIPTS].map((r) => ({
        ...r,
        buyer: r.buyer ?? invoices.find((v) => v.id === r.linkedInvoiceId)?.buyer ?? { name: "—" },
      })),
    [state.createdReceipts, invoices],
  );

  const withholding = useMemo(
    () => INITIAL_WITHHOLDING.map((w) => ({ ...w, certificateReceived: state.certs[w.id] ?? w.certificateReceived })),
    [state.certs],
  );

  const work = useCallback(async <T,>(message: string, detail: string, task: () => Promise<T>) => {
    setBusy({ message, detail });
    try {
      const [result] = await Promise.all([task(), new Promise((r) => setTimeout(r, WORK_MS))]);
      return result;
    } finally {
      setBusy(null);
    }
  }, []);

  const runVerification = useCallback(() => {
    setVerifying(true);
    runSetupVerification()
      .then((ok) => ok && patch({ verified: true }))
      .finally(() => setVerifying(false));
  }, [patch]);

  const value = useMemo<ConsoleValue>(
    () => ({
      ...state,
      ready,
      busy,
      invoices,
      getInvoice: (id) => invoices.find((v) => v.id === id),
      receipts,
      getReceipt: (id) => receipts.find((r) => r.id === id),
      withholding,
      setupDone: state.verified ? 5 : Math.max(0, Math.min(5, readiness().done)),
      setBranch: (branch) => patch({ branch }),
      setRange: (range) => patch({ range }),
      toggleSidebar: () => patch((s) => ({ collapsed: !s.collapsed })),
      addInvoice: (inv) => patch((s) => ({ created: [inv, ...s.created] })),
      markCanceled: (id, c) => patch((s) => ({ cancels: { ...s.cancels, [id]: c } })),
      addReceipt: (r) => patch((s) => ({ createdReceipts: [r, ...s.createdReceipts] })),
      putItem: (item) =>
        patch((s) => ({
          catalog: s.catalog.some((x) => x.code === item.code) ? s.catalog.map((x) => (x.code === item.code ? item : x)) : [item, ...s.catalog],
        })),
      markCertificate: (id) => patch((s) => ({ certs: { ...s.certs, [id]: true } })),
      updateOrg: (fn) => patch((s) => ({ org: fn(s.org) })),
      setCred: (cred) => patch({ cred }),
      setSeller: (seller) => patch({ seller }),
      verifying,
      runVerification,
      reveal,
      setReveal,
      work,
    }),
    [state, ready, busy, invoices, receipts, withholding, patch, work, verifying, runVerification, reveal],
  );

  return <ConsoleContext.Provider value={value}>{children}</ConsoleContext.Provider>;
}

export function useConsole() {
  const ctx = useContext(ConsoleContext);
  if (!ctx) throw new Error("useConsole must be used inside ConsoleProvider");
  return ctx;
}
