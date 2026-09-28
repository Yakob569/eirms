"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Check, Globe } from "../icons";
import { Logo } from "../Logo";
import { QrCode } from "../QrCode";
import styles from "./AuthShell.module.css";

const STEPS = [
  { n: "01", label: "Create account", sub: "Business contact details" },
  { n: "02", label: "Verify identity", sub: "One-time code by email or SMS" },
  { n: "03", label: "Business details", sub: "TIN, address and receipt setup" },
];

function stepFor(pathname: string) {
  if (pathname.startsWith("/verify/success")) return 2;
  if (pathname.startsWith("/verify")) return 1;
  return 0;
}

/** Screens outside onboarding: they get the headline + receipt panel, not the step tracker. */
const HERO_ROUTES = ["/signin", "/forgot-password", "/reset-password", "/change-password"];

export function AuthShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const isHero = HERO_ROUTES.some((r) => pathname.startsWith(r));
  const step = stepFor(pathname);

  return (
    <div className={styles.shell}>
      <aside className={styles.panel}>
        <div className={styles.circleLg} />
        <div className={styles.circleSm} />
        <Logo />

        {isHero ? <SigninHero /> : <StepProgress step={step} />}

        <div className={`mono ${styles.legal}`}>
          <Link href="#">PRIVACY</Link>
          <Link href="#">TERMS</Link>
        </div>
      </aside>

      <main className={styles.main}>
        <div className={styles.topbar}>
          <span className={styles.mobileLogo}>
            <Logo size="sm" />
          </span>
          {!isHero && <span className={`mono ${styles.mobileStep}`}>STEP {step + 1} OF 3</span>}
          <div className={styles.lang}>
            <Globe />
            <span>አማርኛ</span>
            <span className={styles.langSep}>/</span>
            <span className={styles.langActive}>English</span>
          </div>
        </div>
        <div className={styles.center}>
          {/* key re-runs the entrance animation on every screen change */}
          <div key={pathname} className={styles.content}>
            {children}
          </div>
        </div>
      </main>
    </div>
  );
}

function SigninHero() {
  return (
    <div className={styles.panelBody} style={{ gap: 36 }}>
      <h2 className={`serif ${styles.heroTitle}`}>
        Invoicing that Ethiopia&apos;s <i className="accent">tax authority</i> recognizes.
      </h2>
      <div className={styles.receipt}>
        <div className={styles.receiptHead}>
          <span className="mono">RECEIPT · #004218</span>
          <span className={styles.badge}>
            <Check />
            Registered
          </span>
        </div>
        <div className={styles.receiptRow}>
          <QrCode size={72} />
          <div className={styles.receiptMeta}>
            <span className={styles.merchant}>Abebe Coffee House</span>
            <span className={styles.place}>Bole, Addis Ababa</span>
            <span className={`mono ${styles.irn}`}>IRN ETH-2026-0F4A··9C21</span>
          </div>
        </div>
        <div className={styles.rule} />
        <div className={styles.total}>
          <span>Total incl. VAT 15%</span>
          <span className="mono">ETB 862.50</span>
        </div>
      </div>
    </div>
  );
}

function StepProgress({ step }: { step: number }) {
  return (
    <div className={styles.panelBody} style={{ gap: 48 }}>
      <div className={styles.stepIntro}>
        <div className={`mono ${styles.eyebrow}`}>MERCHANT ONBOARDING · STEP {step + 1} OF 3</div>
        <h2 className={`serif ${styles.stepTitle}`}>
          From signup to compliant invoice in <i className="accent">under five minutes.</i>
        </h2>
      </div>
      <ol className={styles.steps}>
        {STEPS.map((s, i) => {
          const state = i < step ? "done" : i === step ? "active" : "todo";
          return (
            <li key={s.n} className={styles.step} data-state={state}>
              <div className={styles.rail}>
                <div className={`mono ${styles.dot}`}>{state === "done" ? <Check size={16} strokeWidth={2.6} /> : s.n}</div>
                {i < STEPS.length - 1 && <div className={styles.line} />}
              </div>
              <div className={styles.stepText}>
                <span className={styles.stepLabel}>{s.label}</span>
                <span className={styles.stepSub}>{s.sub}</span>
              </div>
            </li>
          );
        })}
      </ol>
    </div>
  );
}
