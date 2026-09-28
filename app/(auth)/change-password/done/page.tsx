"use client";

import { useAuthFlow } from "@/components/auth/AuthFlow";
import f from "@/components/auth/form.module.css";
import { ArrowRight, ShieldCheck } from "@/components/icons";

export default function ChangeDonePage() {
  const { session, runStep } = useAuthFlow();

  return (
    <div className={f.stack}>
      <div className={f.badgeWrap}>
        <div className={f.badgeRing} />
        <div className={f.badgeCore}>
          <ShieldCheck size={32} strokeWidth={2.2} />
        </div>
      </div>

      <div className={f.head} style={{ gap: 10 }}>
        <h1 className={`serif ${f.title}`} style={{ fontSize: 48 }}>
          You&apos;re <i className="accent">all set.</i>
        </h1>
        <p className={f.sub} style={{ maxWidth: 400 }}>
          Your password is saved. Next time, sign in with it instead of the temporary one.
        </p>
      </div>

      {session && (
        <dl className={f.summary} style={{ margin: 0 }}>
          <div>
            <dt>Account</dt>
            <dd>{session.name}</dd>
          </div>
          <div>
            <dt>Merchant</dt>
            <dd>{session.merchant}</dd>
          </div>
        </dl>
      )}

      <button
        type="button"
        className={f.primary}
        onClick={() => runStep("Opening your workspace", "Loading your merchant dashboard", async () => {}, "/dashboard")}
      >
        Go to dashboard
        <ArrowRight />
      </button>
    </div>
  );
}
