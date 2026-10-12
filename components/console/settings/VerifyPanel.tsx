"use client";

import Link from "next/link";
import { useConsole } from "../ConsoleProvider";
import { Icon } from "../Icon";
import k from "./kit.module.css";
import st from "./settings.module.css";

/**
 * Setup verification: sends the signed probe invoice to MoR. Runnable once the
 * first three setup steps are done; passing completes setup and unlocks API keys.
 */
export function useVerification() {
  const { setupDone: done, verifying, runVerification } = useConsole();
  const ok = done >= 5;
  const canRun = done >= 3 && !verifying;
  return {
    ok,
    running: verifying,
    canRun,
    run: () => canRun && runVerification(),
    titleA: ok ? "Verified with " : verifying ? "Verifying with " : "Ready to verify with ",
    desc: ok
      ? "Your merchant is cleared for live invoicing. API keys are unlocked."
      : verifying
        ? "Sending the signed probe invoice and checking the response…"
        : done >= 3
          ? "Sends a signed probe invoice built from your credentials, seller profile and source system."
          : "Finish MoR credentials, seller profile and a source system first.",
    button: ok ? "Re-run verification" : verifying ? "Verifying…" : "Run verification",
  };
}

/** Row layout at the bottom of Setup; centred layout on Verify setup. */
export function VerifyPanel({ layout }: { layout: "row" | "center" }) {
  const v = useVerification();
  const icon = (
    <span className={st.verifyIcon} data-ok={v.ok}>
      <Icon name="shield" size={layout === "row" ? 22 : 24} strokeWidth={1.8} />
    </span>
  );
  const title = (
    <span className={st.verifyTitle}>
      {v.titleA}
      <i>MoR.</i>
    </span>
  );
  const button = (
    <button type="button" className={k.primary} disabled={!v.canRun} onClick={v.run} style={layout === "center" ? { marginTop: 6 } : undefined}>
      {v.button}
    </button>
  );

  if (layout === "center")
    return (
      <div className={st.verifyCenter} aria-live="polite">
        {icon}
        {title}
        <span>{v.desc}</span>
        {button}
      </div>
    );

  return (
    <div className={st.verify} aria-live="polite">
      {icon}
      <span className={st.verifyText}>
        {title}
        <span>{v.desc}</span>
      </span>
      <span className={st.verifyActions}>
        <Link href="/settings/verify" className={k.link}>
          Review probe
        </Link>
        {button}
      </span>
    </div>
  );
}
