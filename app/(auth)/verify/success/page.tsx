"use client";

import Link from "next/link";
import { useAuthFlow } from "@/components/auth/AuthFlow";
import f from "@/components/auth/form.module.css";
import { ArrowRight, Check } from "@/components/icons";
import { DEMO_ACCOUNT, maskEmail, maskPhone } from "@/lib/auth";

export default function VerifySuccessPage() {
  const { account, via } = useAuthFlow();
  const a = account ?? DEMO_ACCOUNT;
  const sentTo = via === "email" ? maskEmail(a.email) : maskPhone(a.phone);

  return (
    <div className={f.stack}>
      <div className={f.badgeWrap}>
        <div className={f.badgeRing} />
        <div className={f.badgeCore}>
          <Check size={34} strokeWidth={2.4} />
        </div>
      </div>

      <div className={f.head} style={{ gap: 10 }}>
        <h1 className={`serif ${f.title}`} style={{ fontSize: 48 }}>
          You&apos;re <i className="accent">verified.</i>
        </h1>
        <p className={f.sub} style={{ maxWidth: 400 }}>
          Your merchant account is ready. Add your business details next to start issuing EIRS-registered receipts.
        </p>
      </div>

      <dl className={f.summary} style={{ margin: 0 }}>
        <div>
          <dt>Account</dt>
          <dd>
            {a.firstName} {a.lastName}
          </dd>
        </div>
        <div>
          <dt>Verified via</dt>
          <dd className="mono">{sentTo}</dd>
        </div>
      </dl>

      {/* Business details (step 3) isn't designed yet — this lands on the dashboard placeholder for now. */}
      <Link href="/dashboard" className={f.primary} style={{ color: "#fff" }}>
        Continue to business details
        <ArrowRight />
      </Link>
    </div>
  );
}
