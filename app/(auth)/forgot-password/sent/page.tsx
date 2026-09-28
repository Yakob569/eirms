"use client";

import Link from "next/link";
import { useState } from "react";
import { useAuthFlow } from "@/components/auth/AuthFlow";
import f from "@/components/auth/form.module.css";
import { ArrowRight, Mail } from "@/components/icons";
import { RESET_TTL_MINUTES, maskEmail, requestPasswordReset, verifyResetToken } from "@/lib/auth";

// The real link arrives by email; this shortcut only exists while the API is mocked.
const SHOW_DEMO_LINK = process.env.NODE_ENV !== "production";
const DEMO_TOKEN = "demo";

export default function ResetLinkSentPage() {
  const { resetEmail, runStep, update } = useAuthFlow();
  const [resend, setResend] = useState<"idle" | "sending" | "sent">("idle");
  const [error, setError] = useState<string | null>(null);

  async function resendLink() {
    if (!resetEmail) return;
    setResend("sending");
    setError(null);
    try {
      await requestPasswordReset(resetEmail);
      setResend("sent");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not resend the link.");
      setResend("idle");
    }
  }

  async function openDemoLink() {
    try {
      await runStep(
        "Checking your link",
        "Validating the reset token",
        async () => {
          const { email } = await verifyResetToken(DEMO_TOKEN);
          update({ resetEmail: email, resetToken: DEMO_TOKEN });
        },
        `/reset-password?token=${DEMO_TOKEN}`,
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : "That link didn't work.");
    }
  }

  return (
    <div className={f.stack} style={{ gap: 24 }}>
      <div className={f.badgeWrap}>
        <div className={f.badgeRing} />
        <div className={f.badgeCore}>
          <Mail size={32} strokeWidth={2.2} />
        </div>
      </div>

      <div className={f.head} style={{ gap: 10 }}>
        <h1 className={`serif ${f.title}`} style={{ fontSize: 48 }}>
          Check your <i className="accent">inbox.</i>
        </h1>
        <p className={f.sub}>
          If an account exists for <span className={`mono ${f.ink}`}>{resetEmail ? maskEmail(resetEmail) : "that email"}</span>, a
          reset link is on its way. It expires in {RESET_TTL_MINUTES} minutes.
        </p>
      </div>

      {error && <div className={f.error}>{error}</div>}

      <ol className={f.nextSteps}>
        <li>
          <span className="mono">01</span>
          <span>
            Open the email from <b>no-reply@eirspay.et</b>
          </span>
        </li>
        <li>
          <span className="mono">02</span>
          <span>
            Tap <b>Reset password</b>
          </span>
        </li>
        <li>
          <span className="mono">03</span>
          <span>Choose a new password and sign in</span>
        </li>
      </ol>

      {SHOW_DEMO_LINK ? (
        <button type="button" className={f.primary} onClick={openDemoLink}>
          Open reset link (prototype)
          <ArrowRight />
        </button>
      ) : (
        <Link href="/signin" className={f.primary} style={{ color: "#fff" }}>
          Back to sign in
          <ArrowRight />
        </Link>
      )}

      <div className={f.codeMeta}>
        <span>
          No email?{" "}
          <button type="button" className={f.linkButton} onClick={resendLink} disabled={!resetEmail || resend !== "idle"}>
            {resend === "sending" ? "Sending…" : resend === "sent" ? "Sent again" : "Resend link"}
          </button>
        </span>
        <Link href="/forgot-password" className={f.quiet}>
          Use another email
        </Link>
      </div>
    </div>
  );
}
