"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import type { RangeKey } from "@/lib/dashboard";
import type { BranchKey, Cancellation, Invoice } from "@/lib/merchant";
import { INITIAL_INVOICES, readiness } from "@/lib/merchant";

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
};

type ConsoleValue = Persisted & {
  /** False until session state has been restored — detail pages wait for it. */
  ready: boolean;
  /** Loader message while a MoR call is in flight; the shell renders it over the content column. */
  busy: { message: string; detail: string } | null;
  invoices: Invoice[];
  getInvoice: (id: string) => Invoice | undefined;
  setupDone: number;
  setBranch: (b: BranchKey) => void;
  setRange: (r: RangeKey) => void;
  toggleSidebar: () => void;
  addInvoice: (inv: Invoice) => void;
  markCanceled: (id: string, c: Cancellation) => void;
  /** Shows the see-through loader while `task` runs (at least WORK_MS). */
  work: <T>(message: string, detail: string, task: () => Promise<T>) => Promise<T>;
};

const STORAGE_KEY = "eirs.console";
const initial: Persisted = { branch: "all", range: "d7", collapsed: false, created: [], cancels: {} };

const ConsoleContext = createContext<ConsoleValue | null>(null);

export function ConsoleProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<Persisted>(initial);
  const [ready, setReady] = useState(false);
  const [busy, setBusy] = useState<{ message: string; detail: string } | null>(null);

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

  const work = useCallback(async <T,>(message: string, detail: string, task: () => Promise<T>) => {
    setBusy({ message, detail });
    try {
      const [result] = await Promise.all([task(), new Promise((r) => setTimeout(r, WORK_MS))]);
      return result;
    } finally {
      setBusy(null);
    }
  }, []);

  const value = useMemo<ConsoleValue>(
    () => ({
      ...state,
      ready,
      busy,
      invoices,
      getInvoice: (id) => invoices.find((v) => v.id === id),
      setupDone: Math.max(0, Math.min(5, readiness().done)),
      setBranch: (branch) => patch({ branch }),
      setRange: (range) => patch({ range }),
      toggleSidebar: () => patch((s) => ({ collapsed: !s.collapsed })),
      addInvoice: (inv) => patch((s) => ({ created: [inv, ...s.created] })),
      markCanceled: (id, c) => patch((s) => ({ cancels: { ...s.cancels, [id]: c } })),
      work,
    }),
    [state, ready, busy, invoices, patch, work],
  );

  return <ConsoleContext.Provider value={value}>{children}</ConsoleContext.Provider>;
}

export function useConsole() {
  const ctx = useContext(ConsoleContext);
  if (!ctx) throw new Error("useConsole must be used inside ConsoleProvider");
  return ctx;
}
