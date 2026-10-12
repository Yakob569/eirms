"use client";

import Link from "next/link";
import { SETUP_STEPS } from "@/lib/merchant";
import { useConsole } from "../ConsoleProvider";
import { Icon, type IconName } from "../Icon";
import ui from "../ui.module.css";
import st from "./settings.module.css";
import { VerifyPanel } from "./VerifyPanel";

/** Where each setup step is done, in SETUP_STEPS order. */
const STEP_HREFS = ["/settings/mor-credentials", "/settings/seller-profile", "/settings/source-systems", "/settings/verify", "/settings/api-keys"];

const SHORTCUTS: { title: string; sub: string; icon: IconName; href?: string }[] = [
  { title: "Team", sub: "Invite staff and manage access.", icon: "team" },
  { title: "Roles", sub: "Compose permission bundles.", icon: "shieldPlain" },
  { title: "API keys", sub: "Issue keys for your systems.", icon: "key", href: "/settings/api-keys" },
];

const RING = 2 * Math.PI * 15.5;

export function Setup() {
  const { setupDone: done } = useConsole();
  const left = 5 - done;
  const ready = done === 5;

  return (
    <main className={st.main}>
      <div className={ui.pageHead} style={{ flexWrap: "nowrap" }}>
        <div className={ui.titleBlock}>
          <span className={ui.eyebrow}>SETUP · OWNER</span>
          <h1 className={ui.title}>
            Fiscal <i>readiness.</i>
          </h1>
          <p className={ui.sub} style={{ maxWidth: 560, lineHeight: 1.5 }}>
            A calm, guided path from onboarding to signed EIRS receipts. Finish setup once and every invoice becomes verifiable.
          </p>
        </div>
        <span className={st.ownerTag}>
          <Icon name="lock" size={13} />
          Owner only
        </span>
      </div>

      <div className={st.ribbon}>
        <div className={st.ring}>
          <svg viewBox="0 0 36 36" aria-hidden>
            <circle cx="18" cy="18" r="15.5" fill="none" stroke="var(--tint-2)" strokeWidth="3" />
            <circle
              cx="18"
              cy="18"
              r="15.5"
              fill="none"
              stroke="var(--brand)"
              strokeWidth="3"
              strokeLinecap="round"
              strokeDasharray={`${(done / 5) * RING} ${RING}`}
              style={{ transition: "stroke-dasharray .4s" }}
            />
          </svg>
          <div className={st.ringLabel}>{done}/5</div>
        </div>
        <div className={st.ribbonText}>
          <span className={st.ribbonEyebrow}>FISCAL READINESS · {done * 20}% COMPLETE</span>
          <div className={st.ribbonTitle}>
            {ready ? (
              <>
                You&apos;re <i>ready.</i> Live invoicing is unlocked.
              </>
            ) : (
              <>
                <i>
                  {left} step{left === 1 ? "" : "s"}
                </i>{" "}
                left before live invoicing.
              </>
            )}
          </div>
        </div>
        <Link href={ready ? "/dashboard" : STEP_HREFS[done]} className={ui.primary}>
          {ready ? "Go to dashboard" : "Continue setup"}
          <Icon name="arrowRight" size={16} />
        </Link>
      </div>

      <div className={`${ui.card} ${st.steps}`}>
        {SETUP_STEPS.map((s, i) => {
          const state = i < done ? "done" : i === done ? "next" : "locked";
          return (
            <div key={s.title} className={st.step} data-state={state}>
              <div className={st.badge}>{state === "done" ? <Icon name="check" size={18} strokeWidth={2.6} /> : String(i + 1).padStart(2, "0")}</div>
              <div className={st.stepText}>
                <b>{s.title}</b>
                <span>{s.desc}</span>
              </div>
              <span className={st.chip}>{state === "done" ? "Complete" : state === "next" ? "Next step" : "Locked"}</span>
              {state === "locked" ? (
                <span className={st.stepLink}>—</span>
              ) : (
                <Link href={STEP_HREFS[i]} className={st.stepLink}>
                  {state === "done" ? "Review" : "Continue →"}
                </Link>
              )}
            </div>
          );
        })}
      </div>

      <VerifyPanel layout="row" />

      <div className={st.shortcuts}>
        {SHORTCUTS.map((sc) => {
          const inner = (
            <>
              <span className={st.shortcutIcon}>
                <Icon name={sc.icon} size={17} strokeWidth={1.8} />
              </span>
              <span className={st.shortcutText}>
                <b>{sc.title}</b>
                <span>{sc.sub}</span>
              </span>
              <Icon name="arrowRight" size={15} stroke="var(--muted)" />
            </>
          );
          return sc.href ? (
            <Link key={sc.title} href={sc.href} className={st.shortcut}>
              {inner}
            </Link>
          ) : (
            <div key={sc.title} className={st.shortcut} data-soon="true" title="Not built yet">
              {inner}
            </div>
          );
        })}
      </div>
    </main>
  );
}
