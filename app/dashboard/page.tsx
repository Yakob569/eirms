import Link from "next/link";
import { Logo } from "@/components/Logo";

// Placeholder until the internal dashboard is designed (next design round).
export default function DashboardPage() {
  return (
    <main style={{ minHeight: "100vh", background: "var(--bg)", padding: "40px 24px" }}>
      <div style={{ maxWidth: 1120, margin: "0 auto", display: "flex", flexDirection: "column", gap: 48 }}>
        <Logo size="sm" />
        <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
          <span className="mono" style={{ fontSize: 12, letterSpacing: ".14em", color: "var(--brand-dark)" }}>
            MERCHANT WORKSPACE
          </span>
          <h1 className="serif" style={{ margin: 0, fontSize: 52, lineHeight: 1 }}>
            Dashboard <i className="accent">coming next.</i>
          </h1>
          <p style={{ margin: 0, fontSize: 16, color: "var(--muted)", maxWidth: 520, lineHeight: 1.5 }}>
            You&apos;re signed in. The internal dashboard is the next design round.
          </p>
        </div>
        <Link href="/signin" style={{ fontWeight: 600 }}>
          ← Back to sign in
        </Link>
      </div>
    </main>
  );
}
