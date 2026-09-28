"use client";

import { usePathname, useRouter } from "next/navigation";
import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import type { Session, VerifyVia } from "@/lib/auth";
import { BrandLoader } from "./BrandLoader";

/** Minimum time the brand loader stays up between steps. */
export const LOADER_MS = 1800;

type Account = { firstName: string; lastName: string; email: string; phone: string };

type FlowState = {
  account: Account | null;
  via: VerifyVia;
  codeSentAt: number | null;
  /** Signed-in user, set by sign in (the forced password change reads it). */
  session: Session | null;
  /** Email the password reset link was requested for / confirmed by the link. */
  resetEmail: string | null;
  /** Reset token already checked by verifyResetToken — resetEmail belongs to it. */
  resetToken: string | null;
};

type Loading = { message: string; detail: string } | null;

type AuthFlowValue = FlowState & {
  update: (patch: Partial<FlowState>) => void;
  /**
   * Shows the brand loader while `task` runs (at least LOADER_MS), then navigates to `next`.
   * Pass a function as `next` when the destination depends on what the task returned.
   */
  runStep: (message: string, detail: string, task: () => Promise<void>, next: string | (() => string)) => Promise<void>;
};

const STORAGE_KEY = "eirs.authflow";
const initial: FlowState = { account: null, via: "sms", codeSentAt: null, session: null, resetEmail: null, resetToken: null };

const AuthFlowContext = createContext<AuthFlowValue | null>(null);

export function AuthFlowProvider({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const [state, setState] = useState<FlowState>(initial);
  const [loading, setLoading] = useState<Loading>(null);

  // Restore across reloads within the tab.
  useEffect(() => {
    try {
      const saved = sessionStorage.getItem(STORAGE_KEY);
      if (saved) setState({ ...initial, ...JSON.parse(saved) });
    } catch {}
  }, []);

  // The loader stays up until the next screen has actually rendered.
  useEffect(() => setLoading(null), [pathname]);

  const update = useCallback((patch: Partial<FlowState>) => {
    setState((s) => {
      const next = { ...s, ...patch };
      try {
        sessionStorage.setItem(STORAGE_KEY, JSON.stringify(next));
      } catch {}
      return next;
    });
  }, []);

  const runStep = useCallback<AuthFlowValue["runStep"]>(
    async (message, detail, task, next) => {
      setLoading({ message, detail });
      try {
        await Promise.all([task(), new Promise((r) => setTimeout(r, LOADER_MS))]);
      } catch (err) {
        setLoading(null);
        throw err;
      }
      router.push(typeof next === "function" ? next() : next);
    },
    [router],
  );

  const value = useMemo(() => ({ ...state, update, runStep }), [state, update, runStep]);

  return (
    <AuthFlowContext.Provider value={value}>
      {children}
      {loading && <BrandLoader message={loading.message} detail={loading.detail} durationMs={LOADER_MS} />}
    </AuthFlowContext.Provider>
  );
}

export function useAuthFlow() {
  const ctx = useContext(AuthFlowContext);
  if (!ctx) throw new Error("useAuthFlow must be used inside AuthFlowProvider");
  return ctx;
}
