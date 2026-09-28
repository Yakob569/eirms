"use client";

import Link from "next/link";
import { useState } from "react";
import { useAuthFlow } from "@/components/auth/AuthFlow";
import f from "@/components/auth/form.module.css";
import { ArrowLeft, ArrowRight } from "@/components/icons";
import { maskEmail, requestPasswordReset } from "@/lib/auth";

const looksLikeEmail = (v: string) => /.+@.+\..+/.test(v);

export default function ForgotPasswordPage() {
  const { resetEmail, runStep, update } = useAuthFlow();
  const [email, setEmail] = useState(resetEmail ?? "");
  const [error, setError] = useState<string | null>(null);
  const valid = looksLikeEmail(email.trim());

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!valid) return;
    const addr = email.trim();
    setError(null);
    try {
      await runStep(
        "Sending reset link",
        `To ${maskEmail(addr)}`,
        async () => {
          await requestPasswordReset(addr);
          update({ resetEmail: addr, resetToken: null });
        },
        "/forgot-password/sent",
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not send the reset link.");
    }
  }

  return (
    <form className={f.stack} style={{ gap: 24 }} onSubmit={onSubmit} noValidate>
      <Link href="/signin" className={f.back}>
        <ArrowLeft />
        Back to sign in
      </Link>

      <div className={f.head}>
        <h1 className={`serif ${f.title}`}>Forgot your password?</h1>
        <p className={f.sub}>Enter the email on your merchant account. We&apos;ll send a link to set a new password.</p>
      </div>

      {error && <div className={f.error}>{error}</div>}

      <label className={f.label}>
        Email address
        <input
          className={f.input}
          type="email"
          autoComplete="email"
          placeholder="you@business.et"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          autoFocus
        />
      </label>

      <button type="submit" className={f.primary} disabled={!valid}>
        Send reset link
        <ArrowRight />
      </button>

      <div className={f.foot}>
        Remembered it?{" "}
        <Link href="/signin" style={{ fontWeight: 600 }}>
          Sign in
        </Link>
      </div>
    </form>
  );
}
