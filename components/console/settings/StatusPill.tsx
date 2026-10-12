import k from "./kit.module.css";

type Tone = "ok" | "warn" | "bad" | "ink" | "neutral";

// One status vocabulary for every Settings page.
const TONES: Record<string, Tone> = {
  Active: "ok",
  Verified: "ok",
  "This device": "ok",
  Enabled: "ok",
  "Verified with MoR": "ok",
  Unverified: "warn",
  Failed: "bad",
  "Main branch": "ink",
  OWNER: "ink",
};

/** The single status pill used across Settings (Active / Inactive, Verified / Failed / Unverified, …). */
export function StatusPill({ status, size, children }: { status: string; size?: "sm"; children?: React.ReactNode }) {
  return (
    <span className={k.pill} data-tone={TONES[status] ?? "neutral"} data-size={size}>
      {children}
      {status}
    </span>
  );
}
