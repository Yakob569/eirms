"use client";

import Link from "next/link";
import { Fragment } from "react";
import { SETUP_STEPS } from "@/lib/merchant";
import { useConsole } from "../ConsoleProvider";
import { Icon, type IconName } from "../Icon";
import ui from "../ui.module.css";
import st from "./settings.module.css";

// Settings navigation. Only Setup is designed so far; the rest are placeholders.
const NAV: { label: string; group?: string; step?: number }[] = [
  { label: "Setup" },
  { label: "Account", group: "Account" },
  { label: "MoR Credentials", step: 0 },
  { label: "Seller Profile", step: 1 },
  { label: "Source Systems", step: 2 },
  { label: "Verify Setup", step: 3 },
  { label: "Branches", group: "Organization" },
  { label: "Departments" },
  { label: "API Keys", group: "Access" },
  { label: "Team" },
  { label: "Roles" },
];

const SHORTCUTS: { title: string; sub: string; icon: IconName }[] = [
  { title: "Team", sub: "Invite staff and manage access.", icon: "team" },
  { title: "Roles", sub: "Compose permission bundles.", icon: "shieldPlain" },
  { title: "API keys", sub: "Issue keys for your systems.", icon: "key" },
];

const RING = 2 * Math.PI * 15.5;

export function Setup() {
  const { setupDone: done } = useConsole();
  const left = 5 - done;
  const ready = done === 5;

  return (
    <div className={st.layout}>
      <nav className={st.subnav} aria-label="Settings">
        <span className={st.subnavTitle}>SETTINGS</span>
        {NAV.map((n) => (
          <Fragment key={n.label}>
            {n.group && <span className={st.group}>{n.group}</span>}
            {n.label === "Setup" ? (
              <span className={st.item} aria-current="page">
                <span>Setup</span>
              </span>
            ) : (
              <span className={st.item}>
                <span>{n.label}</span>
                {n.step != null && n.step < done && <Icon name="check" size={14} stroke="var(--brand)" strokeWidth={2.6} />}
              </span>
            )}
          </Fragment>
        ))}
      </nav>

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
          {ready ? (
            <Link href="/dashboard" className={ui.primary}>
              Go to dashboard
              <Icon name="arrowRight" size={16} />
            </Link>
          ) : (
            <button type="button" className={ui.primary}>
              Continue setup
              <Icon name="arrowRight" size={16} />
            </button>
          )}
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
                <span className={st.stepLink}>{state === "done" ? "Review" : state === "next" ? "Continue →" : "—"}</span>
              </div>
            );
          })}
        </div>

        <div className={st.shortcuts}>
          {SHORTCUTS.map((sc) => (
            <div key={sc.title} className={st.shortcut}>
              <span className={st.shortcutIcon}>
                <Icon name={sc.icon} size={17} strokeWidth={1.8} />
              </span>
              <span className={st.shortcutText}>
                <b>{sc.title}</b>
                <span>{sc.sub}</span>
              </span>
              <Icon name="arrowRight" size={15} stroke="var(--muted)" />
            </div>
          ))}
        </div>
      </main>
    </div>
  );
}
