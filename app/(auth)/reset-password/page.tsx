"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import { useAuthFlow } from "@/components/auth/AuthFlow";
import { PasswordField, PasswordMeter } from "@/components/auth/PasswordField";
import f from "@/components/auth/form.module.css";
import { ArrowRight, CheckCircle } from "@/components/icons";
import { maskEmail, passwordAcceptable, resetPassword, verifyResetToken } from "@/lib/auth";

export default function ResetPasswordPage() {
  return (
    <Suspense>
      <ResetPassword />
    </Suspense>
  );
}

function ResetPassword() {
  const token = useSearchParams().get("token") ?? "";
  const { resetEmail, resetToken, runStep, update } = useAuthFlow();
  const confirmed = !!token && resetToken === token;

  // Landing straight from the email: check the token before showing the form.
  const [linkError, setLinkError] = useState<string | null>(token ? null : "This reset link is invalid or has expired.");
  useEffect(() => {
    if (!token || confirmed) return;
    let live = true;
    verifyResetToken(token)
      .then(({ email }) => live && update({ resetEmail: email, resetToken: token }))
      .catch((err) => live && setLinkError(err instanceof Error ? err.message : "This reset link is invalid or has expired."));
    return () => {
      live = false;
    };
  }, [token, confirmed, update]);

  const [pw, setPw] = useState("");
  const [pw2, setPw2] = useState("");
  const [visible, setVisible] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const mismatch = pw2.length > 0 && pw !== pw2;
  const valid = passwordAcceptable(pw) && pw2.length > 0 && !mismatch && confirmed;

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!valid) return;
    setError(null);
    try {
      await runStep("Updating password", "Signing out other sessions", () => resetPassword(token, pw), "/reset-password/done");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not reset your password.");
    }
  }

  if (linkError) {
    return (
      <div className={f.stack} style={{ gap: 24 }}>
        <div className={f.head}>
          <h1 className={`serif ${f.title}`}>Link expired</h1>
          <p className={f.sub}>{linkError} Request a new one to set your password.</p>
        </div>
        <Link href="/forgot-password" className={f.primary} style={{ color: "#fff" }}>
          Send a new link
          <ArrowRight />
        </Link>
      </div>
    );
  }

  return (
    <form className={f.stack} style={{ gap: 22 }} onSubmit={onSubmit} noValidate>
      <div className={f.notice}>
        <CheckCircle />
        {confirmed && resetEmail ? `Reset link confirmed for ${maskEmail(resetEmail)}` : "Checking your reset link…"}
      </div>

      <div className={f.head}>
        <h1 className={`serif ${f.title}`}>Set a new password</h1>
        <p className={f.sub}>Pick something you haven&apos;t used on EIRSPay before.</p>
      </div>

      {error && <div className={f.error}>{error}</div>}

      <PasswordField
        label="New password"
        value={pw}
        onChange={setPw}
        visible={visible}
        onToggle={() => setVisible((v) => !v)}
        placeholder="8+ characters"
        autoComplete="new-password"
        autoFocus
      />
      <PasswordMeter password={pw} />
      <PasswordField
        label="Confirm new password"
        value={pw2}
        onChange={setPw2}
        visible={visible}
        onToggle={() => setVisible((v) => !v)}
        placeholder="Repeat password"
        autoComplete="new-password"
        invalid={mismatch}
      />
      {mismatch && <span className={f.inlineError}>Passwords don&apos;t match.</span>}

      <button type="submit" className={f.primary} disabled={!valid}>
        Reset password
        <ArrowRight />
      </button>
    </form>
  );
}
