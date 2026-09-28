"use client";

import Link from "next/link";
import { useState } from "react";
import { useAuthFlow } from "@/components/auth/AuthFlow";
import f from "@/components/auth/form.module.css";
import { ArrowRight, Check } from "@/components/icons";
import { signUp } from "@/lib/auth";

export default function SignupPage() {
  const { runStep, update } = useAuthFlow();
  const [form, setForm] = useState({ firstName: "", lastName: "", email: "", phone: "", password: "", confirm: "" });
  const [terms, setTerms] = useState(true);
  const [touched, setTouched] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm((s) => ({ ...s, [k]: e.target.value }));

  const errors = {
    firstName: !form.firstName.trim() && "Required",
    lastName: !form.lastName.trim() && "Required",
    email: !/^\S+@\S+\.\S+$/.test(form.email) && "Enter a valid email",
    phone: form.phone.replace(/\D/g, "").length < 9 && "Enter a 9-digit number",
    password: form.password.length < 8 && "At least 8 characters",
    confirm: form.confirm !== form.password && "Passwords don't match",
  };
  const valid = !Object.values(errors).some(Boolean);
  const show = (k: keyof typeof errors) => (touched ? errors[k] || null : null);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setTouched(true);
    setError(null);
    if (!terms || !valid) return;
    const { confirm: _confirm, ...data } = form;
    try {
      await runStep(
        "Creating your account",
        "Registering your merchant profile",
        async () => {
          await signUp(data);
          update({ account: { firstName: data.firstName, lastName: data.lastName, email: data.email, phone: data.phone } });
        },
        "/verify",
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not create your account.");
    }
  }

  return (
    <form className={f.stack} style={{ gap: 22 }} onSubmit={onSubmit} noValidate>
      <div className={f.head}>
        <h1 className={`serif ${f.title}`} style={{ fontSize: 42 }}>
          Create your merchant account
        </h1>
        <p className={f.sub}>Register with your business contact details to start onboarding.</p>
      </div>

      {error && <div className={f.error}>{error}</div>}

      <div className={f.grid}>
        <Field label="First name" error={show("firstName")}>
          <input className={f.input} autoComplete="given-name" placeholder="Yishak" value={form.firstName} onChange={set("firstName")} aria-invalid={!!show("firstName")} />
        </Field>
        <Field label="Last name" error={show("lastName")}>
          <input className={f.input} autoComplete="family-name" placeholder="Girma" value={form.lastName} onChange={set("lastName")} aria-invalid={!!show("lastName")} />
        </Field>
        <Field label="Email address" error={show("email")} full>
          <input className={f.input} type="email" autoComplete="email" placeholder="you@business.et" value={form.email} onChange={set("email")} aria-invalid={!!show("email")} />
        </Field>
        <Field label="Phone number" error={show("phone")} full>
          <div className={f.phone}>
            <span className={`mono ${f.phonePrefix}`}>+251</span>
            <input type="tel" inputMode="tel" autoComplete="tel-national" placeholder="9 12 34 56 78" value={form.phone} onChange={set("phone")} aria-invalid={!!show("phone")} />
          </div>
        </Field>
        <Field label="Password" error={show("password")}>
          <input className={f.input} type="password" autoComplete="new-password" placeholder="8+ characters" value={form.password} onChange={set("password")} aria-invalid={!!show("password")} />
        </Field>
        <Field label="Confirm password" error={show("confirm")}>
          <input className={f.input} type="password" autoComplete="new-password" placeholder="Repeat password" value={form.confirm} onChange={set("confirm")} aria-invalid={!!show("confirm")} />
        </Field>
      </div>

      <label className={f.checkbox}>
        <input type="checkbox" className={f.checkInput} checked={terms} onChange={(e) => setTerms(e.target.checked)} />
        <span className={f.box}>
          <Check strokeWidth={3} />
        </span>
        <span>
          I agree to the <Link href="#">Terms of Service</Link> and <Link href="#">Privacy Policy</Link>
        </span>
      </label>

      <button type="submit" className={f.primary} disabled={!terms}>
        Create account
        <ArrowRight />
      </button>

      <div className={f.foot}>
        Already registered?{" "}
        <Link href="/signin" style={{ fontWeight: 600 }}>
          Sign in
        </Link>
      </div>
    </form>
  );
}

function Field({ label, error, full, children }: { label: string; error: string | null; full?: boolean; children: React.ReactNode }) {
  return (
    <label className={`${f.label} ${full ? f.full : ""}`}>
      {label}
      {children}
      {error && <span className={f.fieldError}>{error}</span>}
    </label>
  );
}
