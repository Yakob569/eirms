"use client";

import Link from "next/link";
import { useState } from "react";
import { useAuthFlow } from "@/components/auth/AuthFlow";
import f from "@/components/auth/form.module.css";
import { ArrowRight } from "@/components/icons";
import { signIn } from "@/lib/auth";

export default function SigninPage() {
  const { runStep } = useAuthFlow();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await runStep("Signing you in", "Loading your merchant workspace", () => signIn(email, password), "/dashboard");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not sign in.");
    }
  }

  return (
    <form className={f.stack} onSubmit={onSubmit} noValidate>
      <div className={f.head}>
        <h1 className={`serif ${f.title}`}>Welcome back</h1>
        <p className={f.sub}>Sign in to your merchant account.</p>
      </div>

      {error && <div className={f.error}>{error}</div>}

      <div className={f.fields}>
        <label className={f.label}>
          Email address
          <input
            className={f.input}
            type="email"
            autoComplete="email"
            placeholder="you@business.et"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </label>
        <label className={f.label}>
          <span className={f.labelRow}>
            Password <Link href="#">Forgot password?</Link>
          </span>
          <input
            className={f.input}
            type="password"
            autoComplete="current-password"
            placeholder="••••••••"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </label>
      </div>

      <button type="submit" className={f.primary}>
        Sign in
        <ArrowRight />
      </button>

      <div className={f.foot}>
        New to EIRSPay?{" "}
        <Link href="/signup" style={{ fontWeight: 600 }}>
          Create account
        </Link>
      </div>
    </form>
  );
}
