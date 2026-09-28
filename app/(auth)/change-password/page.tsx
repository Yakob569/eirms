"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { useAuthFlow } from "@/components/auth/AuthFlow";
import { PasswordField, PasswordMeter } from "@/components/auth/PasswordField";
import f from "@/components/auth/form.module.css";
import { ArrowRight, Lock } from "@/components/icons";
import { changePassword, passwordAcceptable, signOut } from "@/lib/auth";

export default function ChangePasswordPage() {
  const router = useRouter();
  const { session, runStep, update } = useAuthFlow();
  const [cur, setCur] = useState("");
  const [pw, setPw] = useState("");
  const [pw2, setPw2] = useState("");
  const [visible, setVisible] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const mismatch = pw2.length > 0 && pw !== pw2;
  const same = cur.length > 0 && pw.length > 0 && cur === pw;
  const valid = cur.length >= 4 && passwordAcceptable(pw) && pw2.length > 0 && !mismatch && !same;
  const toggle = () => setVisible((v) => !v);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!valid) return;
    setError(null);
    try {
      await runStep(
        "Saving your password",
        "Securing your account",
        async () => {
          await changePassword(cur, pw);
          if (session) update({ session: { ...session, mustChangePassword: false } });
        },
        "/change-password/done",
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not change your password.");
    }
  }

  async function onSignOut() {
    await signOut();
    update({ session: null });
    router.push("/signin");
  }

  return (
    <form className={f.stack} style={{ gap: 22 }} onSubmit={onSubmit} noValidate>
      <div className={f.warn}>
        <Lock />
        <span>
          Signed in{session ? (
            <>
              {" "}as <b>{session.name}</b>
            </>
          ) : null}{" "}
          with a temporary password. Set your own before you continue.
        </span>
      </div>

      <div className={f.head}>
        <h1 className={`serif ${f.title}`}>Change your password</h1>
        <p className={f.sub}>Your temporary password came from your account owner or EIRSPay support.</p>
      </div>

      {error && <div className={f.error}>{error}</div>}

      <PasswordField
        label="Temporary password"
        value={cur}
        onChange={setCur}
        visible={visible}
        onToggle={toggle}
        placeholder="From your invite"
        autoComplete="current-password"
        autoFocus
      />
      <div className={f.divider} />
      <PasswordField
        label="New password"
        value={pw}
        onChange={setPw}
        visible={visible}
        onToggle={toggle}
        placeholder="8+ characters"
        autoComplete="new-password"
        invalid={same}
      />
      <PasswordMeter password={pw} />
      <PasswordField
        label="Confirm new password"
        value={pw2}
        onChange={setPw2}
        visible={visible}
        onToggle={toggle}
        placeholder="Repeat password"
        autoComplete="new-password"
        invalid={mismatch}
      />
      {mismatch && <span className={f.inlineError}>Passwords don&apos;t match.</span>}
      {same && <span className={f.inlineError}>New password must differ from the temporary one.</span>}

      <button type="submit" className={f.primary} disabled={!valid}>
        Save and continue
        <ArrowRight />
      </button>

      <div className={f.foot}>
        Not you?{" "}
        <button type="button" className={f.linkButton} onClick={onSignOut}>
          Sign out
        </button>
      </div>
    </form>
  );
}
