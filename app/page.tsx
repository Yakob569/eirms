import Link from "next/link";
import { ArrowRight, Check } from "@/components/icons";
import { Logo } from "@/components/Logo";
import { QrCode } from "@/components/QrCode";
import s from "./landing.module.css";

const FEATURES = [
  { n: "QR", t: "QR on every receipt", d: "Customers verify authenticity in one scan." },
  { n: "አ/A", t: "Amharic + English", d: "Ship receipts in the language your customers read." },
  { n: "+251", t: "Ethiopian phone flows", d: "OTP over SMS to +251 numbers, out of the box." },
  { n: "RLS", t: "Secure by default", d: "Time-limited tokens, encrypted at rest, RLS-enforced." },
];

const FLOW = [
  { n: "01", t: "Create your account", d: "Register with your business contact details." },
  { n: "02", t: "Verify your identity", d: "Confirm your email or phone with a one-time code." },
  { n: "03", t: "Issue compliant invoices", d: "Every receipt is registered with EIRS automatically." },
];

const STATS = [
  { l: "MEDIAN IRN ROUND-TRIP", v: "< 400ms" },
  { l: "EIRS SUBMISSION SUCCESS", v: "99.98%" },
  { l: "AMHARIC + ENGLISH UI", v: "2 langs" },
  { l: "MANUAL FILINGS REQUIRED", v: "0" },
];

const BEFORE = [
  "Hand-written receipts, unclear compliance status",
  "Manual monthly VAT reconciliation",
  "No verifiable trail for customers",
  "Weeks of back-and-forth with EIRS",
];

const AFTER = [
  "Every invoice signed with an IRN in real time",
  "Reconciliation is automatic and audit-ready",
  "QR-verifiable at portal.mor.gov.et",
  "Live in under five minutes",
];

const CITIES = ["Addis Ababa", "Dire Dawa", "Bahir Dar", "Hawassa", "Mekelle"];

const LINE_ITEMS = [
  ["Macchiato × 2", "180.00"],
  ["Injera combo × 1", "420.00"],
  ["Bottled water × 3", "150.00"],
];

export default function LandingPage() {
  return (
    <div className={s.page}>
      <header className={s.nav}>
        <div className={`${s.wrap} ${s.navInner}`}>
          <Logo size="sm" />
          <nav className={s.navLinks}>
            <a href="#features">Features</a>
            <a href="#how">How it works</a>
            <a href="#compliance">Compliance</a>
          </nav>
          <div className={s.navActions}>
            <span className={s.muted}>አማርኛ</span>
            <Link href="/signin" className={s.navSignin}>
              Sign in
            </Link>
            <Link href="/signup" className={s.navCta}>
              Get started
            </Link>
          </div>
        </div>
      </header>

      <section className={s.hero}>
        <div className={`${s.wrap} ${s.heroGrid}`}>
          <div className={s.heroCopy}>
            <span className={`mono ${s.chip}`}>EIRS CERTIFIED WORKFLOW</span>
            <h1 className={`serif ${s.heroTitle}`}>
              Invoicing that Ethiopia&apos;s tax authority <i className="accent">actually recognizes.</i>
            </h1>
            <p className={s.heroSub}>
              Register your business, verify your identity, and issue legally compliant electronic receipts through
              Ethiopia&apos;s Ministry of Revenues — IRN and QR on every invoice.
            </p>
            <div className={s.heroCtas}>
              <Link href="/signup" className={s.btnPrimary}>
                Start onboarding
                <ArrowRight size={16} />
              </Link>
              <Link href="/signin" className={s.btnSecondary}>
                Sign in
              </Link>
            </div>
          </div>

          <div className={s.heroArt}>
            <div className={s.halo} />
            <div className={s.receipt}>
              <div className={s.receiptHead}>
                <span className="mono">RECEIPT · #004218</span>
                <span className={s.badge}>
                  <Check />
                  Registered
                </span>
              </div>
              <div className={s.receiptMerchant}>
                <span>Abebe Coffee House</span>
                <span>Bole, Addis Ababa · TIN 0034••••12</span>
              </div>
              <div className={s.lines}>
                {LINE_ITEMS.map(([item, amt]) => (
                  <div key={item}>
                    <span>{item}</span>
                    <span className="mono">{amt}</span>
                  </div>
                ))}
                <div className={s.muted}>
                  <span>VAT 15%</span>
                  <span className="mono">112.50</span>
                </div>
              </div>
              <div className={s.receiptTotal}>
                <span>Total</span>
                <span className="mono">ETB 862.50</span>
              </div>
              <div className={s.irnBox}>
                <QrCode size={64} bordered={false} />
                <div className={s.irnText}>
                  <span className="mono">IRN</span>
                  <span className="mono">ETH-2026-0F4A··9C21</span>
                  <span>Verify at portal.mor.gov.et</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <div className={s.trust}>
        <div className={`${s.wrap} ${s.trustInner}`}>
          <span className="mono">TRUSTED BY MERCHANTS ACROSS</span>
          <div className={`serif ${s.cities}`}>
            {CITIES.map((c) => (
              <span key={c}>{c}</span>
            ))}
          </div>
        </div>
      </div>

      <section id="features" className={s.section}>
        <div className={s.wrap}>
          <div className={s.sectionHead}>
            <span className={`mono ${s.eyebrow}`}>THE STACK</span>
            <h2 className={`serif ${s.h2}`}>
              One workflow, every <i className="accent">receipt compliant.</i>
            </h2>
            <p className={s.lead}>
              A focused set of primitives so your storefront, POS, or back office can issue verified invoices without a
              compliance team.
            </p>
          </div>
          <div className={s.bento}>
            <div className={s.darkCard}>
              <div className={s.darkCopy}>
                <span className={`mono ${s.darkChip}`}>PORTAL.MOR.GOV.ET</span>
                <div className="serif" style={{ fontSize: 34 }}>
                  EIRS-registered receipts
                </div>
                <p>Every invoice is submitted to the Ministry of Revenues in real time and returns with a signed IRN.</p>
              </div>
              <div className={`mono ${s.code}`}>
                <div className={s.codeHead}>
                  <span>POST /invoices</span>
                  <span className={s.ok}>200 OK</span>
                </div>
                <div>&quot;irn&quot;: &quot;ETH-2026-0F4A··9C21&quot;</div>
                <div>&quot;status&quot;: &quot;REGISTERED&quot;</div>
                <div>&quot;verify_url&quot;: &quot;portal.mor.gov.et/v/…&quot;</div>
              </div>
            </div>
            {FEATURES.map((f) => (
              <div key={f.t} className={s.feature}>
                <span className={`mono ${s.featureMark}`}>{f.n}</span>
                <span className="serif" style={{ fontSize: 24 }}>
                  {f.t}
                </span>
                <span className={s.featureText}>{f.d}</span>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section id="how" className={`${s.section} ${s.tinted}`}>
        <div className={s.wrap}>
          <div className={s.flowHead}>
            <div className={s.sectionHead}>
              <span className={`mono ${s.eyebrow}`}>WORKFLOW</span>
              <h2 className={`serif ${s.h2}`} style={{ maxWidth: 620 }}>
                From signup to compliant invoice in <i className="accent">under five minutes.</i>
              </h2>
            </div>
            <span className={s.muted} style={{ fontSize: 14 }}>
              More onboarding steps land after verification.
            </span>
          </div>
          <div className={s.flow}>
            {FLOW.map((w) => (
              <div key={w.n} className={s.flowCard}>
                <span className={`serif ${s.flowNum}`}>{w.n}</span>
                <span className={s.flowTitle}>{w.t}</span>
                <span className={s.featureText}>{w.d}</span>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section id="compliance" className={s.section}>
        <div className={s.wrap}>
          <div className={s.numbers}>
            <div className={s.sectionHead}>
              <span className={`mono ${s.eyebrow}`} style={{ color: "var(--tint-2)" }}>
                THE NUMBERS
              </span>
              <h2 className="serif" style={{ margin: 0, fontSize: 48, lineHeight: 1 }}>
                Compliance, <i style={{ color: "var(--tint-2)" }}>measured.</i>
              </h2>
            </div>
            <div className={s.stats}>
              {STATS.map((st) => (
                <div key={st.l} className={s.stat}>
                  <span className="mono">{st.l}</span>
                  <span className="serif">{st.v}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      <section className={s.section} style={{ paddingTop: 0 }}>
        <div className={s.wrap}>
          <div className={s.sectionHead} style={{ marginBottom: 32 }}>
            <span className={`mono ${s.eyebrow}`}>BEFORE / AFTER</span>
            <h2 className={`serif ${s.h2}`}>
              Trade the paperwork for a <i className="accent">signed receipt.</i>
            </h2>
          </div>
          <div className={s.compare}>
            <div className={s.before}>
              <span className="mono">BEFORE EIRSPAY</span>
              {BEFORE.map((b) => (
                <del key={b}>{b}</del>
              ))}
            </div>
            <div className={s.after}>
              <span className="mono">WITH EIRSPAY</span>
              {AFTER.map((x) => (
                <span key={x} className={s.afterItem}>
                  <span className={s.tick}>
                    <Check strokeWidth={3} />
                  </span>
                  {x}
                </span>
              ))}
            </div>
          </div>
        </div>
      </section>

      <section className={s.cta}>
        <div className={`${s.wrap} ${s.ctaInner}`}>
          <div className={s.ctaCopy}>
            <h2 className="serif">Start onboarding your merchant account today</h2>
            <span>Join Ethiopian merchants issuing EIRS-compliant receipts.</span>
          </div>
          <div className={s.ctaActions}>
            <Link href="/signup" className={s.ctaBtn}>
              Create account
              <ArrowRight size={16} />
            </Link>
            <span>
              Already have an account? <Link href="/signin">Sign in</Link>
            </span>
          </div>
        </div>
      </section>

      <footer className={s.footer}>
        <div className={`${s.wrap} ${s.footerInner}`}>
          <div className={s.footerBrand}>
            <span style={{ fontWeight: 600 }}>EIRSPay</span>
            <span className={s.muted}>Electronic invoicing for Ethiopia</span>
            <span className={s.muted} style={{ fontSize: 13, marginTop: 16 }}>
              © 2026 EIRSPay. All rights reserved.
            </span>
          </div>
          <div className={s.footerCols}>
            <div>
              <span className="mono">ACCOUNT</span>
              <Link href="/signup">Sign up</Link>
              <Link href="/signin">Sign in</Link>
            </div>
            <div>
              <span className="mono">LEGAL</span>
              <Link href="#">Terms</Link>
              <Link href="#">Privacy</Link>
            </div>
            <div>
              <span className="mono">SUPPORT</span>
              <Link href="#">Contact</Link>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}
