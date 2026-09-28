"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { useAuthFlow } from "@/components/auth/AuthFlow";
import f from "@/components/auth/form.module.css";
import { ArrowLeft, ArrowRight } from "@/components/icons";
import { CODE_TTL_SECONDS, DEMO_ACCOUNT, maskEmail, maskPhone, sendCode, verifyCode } from "@/lib/auth";

function formatTime(secs: number) {
  return `${Math.floor(secs / 60)}:${String(secs % 60).padStart(2, "0")}`;
}

export default function VerifyCodePage() {
  const { account, via, codeSentAt, runStep, update } = useAuthFlow();
  const a = account ?? DEMO_ACCOUNT;
  const sentTo = via === "email" ? maskEmail(a.email) : maskPhone(a.phone);

  const [code, setCode] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [resending, setResending] = useState(false);
  const [now, setNow] = useState(() => Date.now());
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    const iv = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(iv);
  }, []);

  const sentAt = codeSentAt ?? now;
  const secs = Math.max(0, CODE_TTL_SECONDS - Math.floor((now - sentAt) / 1000));
  const expired = secs === 0;
  const complete = code.length === 6;

  async function resend() {
    setResending(true);
    setError(null);
    try {
      await sendCode(via, via === "email" ? a.email : a.phone);
      update({ codeSentAt: Date.now() });
      setCode("");
      inputRef.current?.focus();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not resend the code.");
    } finally {
      setResending(false);
    }
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!complete || expired) return;
    setError(null);
    try {
      await runStep("Verifying your code", "Confirming with EIRS", () => verifyCode(code), "/verify/success");
    } catch (err) {
      setError(err instanceof Error ? err.message : "That code didn't work.");
      setCode("");
    }
  }

  return (
    <form className={f.stack} style={{ gap: 24 }} onSubmit={onSubmit}>
      <Link href="/verify" className={f.back}>
        <ArrowLeft />
        Change method
      </Link>

      <div className={f.head}>
        <h1 className={`serif ${f.title}`}>Enter the 6-digit code</h1>
        <p className={f.sub}>
          We sent it to <span className={`mono ${f.ink}`}>{sentTo}</span>
        </p>
      </div>

      {error && <div className={f.error}>{error}</div>}

      <div className={f.codeWrap}>
        <div className={f.cells} aria-hidden>
          {Array.from({ length: 6 }, (_, i) => (
            <div
              key={i}
              className={`mono ${f.cell}`}
              data-filled={!!code[i]}
              data-active={i === Math.min(code.length, 5) && !complete}
            >
              {code[i] ?? ""}
            </div>
          ))}
        </div>
        <input
          ref={inputRef}
          className={f.codeInput}
          value={code}
          onChange={(e) => setCode(e.target.value.replace(/\D/g, "").slice(0, 6))}
          inputMode="numeric"
          autoComplete="one-time-code"
          maxLength={6}
          autoFocus
          aria-label="6-digit verification code"
        />
      </div>

      <div className={f.codeMeta}>
        <span>
          {expired ? (
            "Code expired"
          ) : (
            <>
              Expires in <span className={`mono ${f.ink}`}>{formatTime(secs)}</span>
            </>
          )}
        </span>
        <span>
          Didn&apos;t get it?{" "}
          <button type="button" className={f.linkButton} onClick={resend} disabled={resending}>
            {resending ? "Sending…" : "Resend code"}
          </button>
        </span>
      </div>

      <button type="submit" className={f.primary} disabled={!complete || expired}>
        Verify
        <ArrowRight />
      </button>
    </form>
  );
}
