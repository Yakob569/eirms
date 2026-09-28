"use client";

import Link from "next/link";
import { useEffect } from "react";
import { useAuthFlow } from "@/components/auth/AuthFlow";
import f from "@/components/auth/form.module.css";
import { ArrowRight, Check } from "@/components/icons";

export default function ResetDonePage() {
  const { update } = useAuthFlow();

  // The link is spent; don't let a later visit reuse it.
  useEffect(() => update({ resetEmail: null, resetToken: null }), [update]);

  return (
    <div className={f.stack}>
      <div className={f.badgeWrap}>
        <div className={f.badgeRing} />
        <div className={f.badgeCore}>
          <Check size={32} strokeWidth={2.2} />
        </div>
      </div>

      <div className={f.head} style={{ gap: 10 }}>
        <h1 className={`serif ${f.title}`} style={{ fontSize: 48 }}>
          Password <i className="accent">updated.</i>
        </h1>
        <p className={f.sub} style={{ maxWidth: 400 }}>
          You&apos;ve been signed out of other devices. Sign in with your new password to continue.
        </p>
      </div>

      <Link href="/signin" className={f.primary} style={{ color: "#fff" }}>
        Continue to sign in
        <ArrowRight />
      </Link>
    </div>
  );
}
