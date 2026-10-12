"use client";

import { useState } from "react";
import { MERCHANT, SELLER } from "@/lib/merchant";
import { ACCOUNT, SECURITY_ACTIVITY, initials, type SecurityEvent } from "@/lib/org";
import { useConsole } from "../ConsoleProvider";
import { Icon, type IconName } from "../Icon";
import { Field, SectionCard, SettingsHead } from "./kit";
import k from "./kit.module.css";
import st from "./settings.module.css";
import { StatusPill } from "./StatusPill";

const EVENT_ICON: Record<SecurityEvent["kind"], IconName> = { in: "login", bad: "alert", out: "logout", reset: "reset" };

export function Account() {
  const { org, updateOrg } = useConsole();
  const [pwOpen, setPwOpen] = useState(false);
  const [pwDone, setPwDone] = useState(false);
  const [pw, setPw] = useState({ current: "", next: "", confirm: "" });
  const others = org.sessions.filter((s) => !s.current).length;
  const pwError = pw.next && pw.next.length < 8 ? "At least 8 characters." : pw.confirm && pw.confirm !== pw.next ? "Passwords don't match." : "";
  const pwValid = !!pw.current && pw.next.length >= 8 && pw.confirm === pw.next;
  const name = `${ACCOUNT.firstName} ${ACCOUNT.lastName}`;

  const signOut = (id: string) => updateOrg((o) => ({ ...o, sessions: o.sessions.filter((s) => s.id !== id) }));

  return (
    <main className={k.page}>
      <SettingsHead
        eyebrow={`ACCOUNT · ${ACCOUNT.type}`}
        title="Your"
        accent="account."
        lede="Sign-in identity, password and every device that holds an EIRSPay session."
      />

      <section className={k.card}>
        <div className={st.identity}>
          <span className={st.identityMark}>{initials(name)}</span>
          <span className={st.identityText}>
            <b>{name}</b>
            <span>
              {MERCHANT.name} · TIN {SELLER.tin}
            </span>
          </span>
          <StatusPill status={ACCOUNT.type} />
        </div>
        <dl className={st.identityGrid}>
          <div>
            <dt>First name</dt>
            <dd>{ACCOUNT.firstName}</dd>
          </div>
          <div>
            <dt>Last name</dt>
            <dd>{ACCOUNT.lastName}</dd>
          </div>
          <div>
            <dt>Email</dt>
            <dd>
              {ACCOUNT.email}
              <StatusPill status="Verified" size="sm" />
            </dd>
          </div>
          <div>
            <dt>Phone</dt>
            <dd>
              <span className={k.mono} style={{ fontSize: 14 }}>
                {ACCOUNT.phone}
              </span>
              <StatusPill status="Verified" size="sm" />
            </dd>
          </div>
        </dl>
      </section>

      <SectionCard
        icon="lock"
        title="Change password"
        sub={pwDone ? "Changed just now" : "Last changed 42 days ago"}
        flush={!pwOpen}
        action={
          <button
            type="button"
            className={k.link}
            onClick={() => {
              setPwOpen(!pwOpen);
              setPw({ current: "", next: "", confirm: "" });
            }}
          >
            {pwOpen ? "Cancel" : "Change"}
          </button>
        }
      >
        {pwOpen && (
          <div className={st.pwForm}>
            <div className={st.pwGrid}>
              <Field label="Current password" type="password" placeholder="••••••••" value={pw.current} onValue={(v) => setPw({ ...pw, current: v })} />
              <Field label="New password" type="password" placeholder="At least 8 characters" value={pw.next} onValue={(v) => setPw({ ...pw, next: v })} />
              <Field label="Confirm new password" type="password" placeholder="Repeat new password" value={pw.confirm} onValue={(v) => setPw({ ...pw, confirm: v })} />
            </div>
            <div className={k.footer} style={{ paddingTop: 0 }}>
              <button
                type="button"
                className={`${k.primary} ${k.primarySm}`}
                disabled={!pwValid}
                onClick={() => {
                  setPwOpen(false);
                  setPwDone(true);
                }}
              >
                Update password
              </button>
              <span className={k.note} style={pwError ? { color: "var(--danger)" } : undefined}>
                {pwError || "Other sessions stay signed in unless you sign them out below."}
              </span>
            </div>
          </div>
        )}
      </SectionCard>

      <SectionCard
        icon="monitor"
        title="Active sessions"
        sub="Devices signed in to this account, identified by fingerprint."
        action={
          others > 0 ? (
            <button
              type="button"
              className={`${k.ghost} ${k.ghostSm} ${k.ghostDanger}`}
              onClick={() => updateOrg((o) => ({ ...o, sessions: o.sessions.filter((s) => s.current) }))}
            >
              Sign out all other sessions
            </button>
          ) : undefined
        }
      >
        {org.sessions.map((s) => (
          <div key={s.id} className={k.row}>
            <span className={k.dot} />
            <span className={k.stack} style={{ gap: 3 }}>
              <span className={st.sessionName}>
                {s.device}
                {s.current && <StatusPill status="This device" size="sm" />}
              </span>
              <span className={k.subMono}>
                {s.fingerprint} · {s.where} · {s.last}
              </span>
            </span>
            {!s.current ? (
              <button type="button" className={k.dangerLink} onClick={() => signOut(s.id)}>
                Sign out
              </button>
            ) : (
              <span />
            )}
          </div>
        ))}
      </SectionCard>

      <SectionCard
        icon="shieldPlain"
        title="Recent security activity"
        sub="Sign-ins, failed attempts and password changes."
        action={
          <button type="button" className={k.link} title="Full log isn't built yet">
            View all
          </button>
        }
      >
        {[...(pwDone ? [{ kind: "reset" as const, event: "Password changed", meta: "From Settings › Account", when: "Just now" }] : []), ...SECURITY_ACTIVITY].map((a, i) => (
          <div key={i} className={k.row} style={{ padding: "11px 24px" }}>
            <span className={st.eventIcon} data-bad={a.kind === "bad"}>
              <Icon name={EVENT_ICON[a.kind]} size={14} />
            </span>
            <span className={st.event}>
              <b>{a.event}</b>
              <span>{a.meta}</span>
            </span>
            <span className={k.subMono} style={{ whiteSpace: "nowrap" }}>
              {a.when}
            </span>
          </div>
        ))}
      </SectionCard>
    </main>
  );
}
