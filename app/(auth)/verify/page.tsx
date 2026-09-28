"use client";

import { useState } from "react";
import { useAuthFlow } from "@/components/auth/AuthFlow";
import f from "@/components/auth/form.module.css";
import { ArrowRight, CheckCircle, Mail, Phone } from "@/components/icons";
import { DEMO_ACCOUNT, maskEmail, maskPhone, sendCode, type VerifyVia } from "@/lib/auth";

export default function VerifyMethodPage() {
  const { account, runStep, update } = useAuthFlow();
  const [error, setError] = useState<string | null>(null);
  const a = account ?? DEMO_ACCOUNT;
  const email = maskEmail(a.email);
  const phone = maskPhone(a.phone);

  async function pick(via: VerifyVia) {
    setError(null);
    try {
      await runStep(
        "Sending your code",
        via === "email" ? `To ${email}` : `By SMS to ${phone}`,
        async () => {
          await sendCode(via, via === "email" ? a.email : a.phone);
          update({ via, codeSentAt: Date.now() });
        },
        "/verify/code",
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not send the code.");
    }
  }

  return (
    <div className={f.stack} style={{ gap: 24 }}>
      <div className={f.notice}>
        <CheckCircle />
        Account created for {a.firstName} {a.lastName}
      </div>

      <div className={f.head}>
        <h1 className={`serif ${f.title}`}>Verify your account</h1>
        <p className={f.sub}>Choose where you want to receive your verification code.</p>
      </div>

      {error && <div className={f.error}>{error}</div>}

      <div className={f.options}>
        <button type="button" className={f.option} onClick={() => pick("email")}>
          <span className={f.optionIcon}>
            <Mail />
          </span>
          <span className={f.optionText}>
            <span className={f.optionTitle}>Email</span>
            <span className={`mono ${f.optionDest}`}>{email}</span>
          </span>
          <ArrowRight stroke="var(--muted)" />
        </button>
        <button type="button" className={f.option} onClick={() => pick("sms")}>
          <span className={f.optionIcon}>
            <Phone />
          </span>
          <span className={f.optionText}>
            <span className={f.optionTitle}>
              Phone (SMS)<span className={f.pill}>Faster</span>
            </span>
            <span className={`mono ${f.optionDest}`}>{phone}</span>
          </span>
          <ArrowRight stroke="var(--muted)" />
        </button>
      </div>
    </div>
  );
}
