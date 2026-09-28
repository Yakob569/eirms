"use client";

import { Check, Eye, EyeOff } from "@/components/icons";
import { PASSWORD_RULES, passwordScore } from "@/lib/auth";
import f from "./form.module.css";

type FieldProps = {
  label: string;
  value: string;
  onChange: (value: string) => void;
  /** Shared across a form's fields, so one toggle reveals them all. */
  visible: boolean;
  onToggle: () => void;
  placeholder?: string;
  autoComplete: "current-password" | "new-password";
  invalid?: boolean;
  autoFocus?: boolean;
};

export function PasswordField({ label, value, onChange, visible, onToggle, placeholder, autoComplete, invalid, autoFocus }: FieldProps) {
  return (
    <label className={f.label}>
      {label}
      <span className={f.pwWrap}>
        <input
          className={f.input}
          type={visible ? "text" : "password"}
          autoComplete={autoComplete}
          placeholder={placeholder}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          aria-invalid={invalid || undefined}
          autoFocus={autoFocus}
        />
        <button
          type="button"
          className={f.eye}
          onClick={onToggle}
          aria-label={visible ? "Hide passwords" : "Show passwords"}
          aria-pressed={visible}
        >
          {visible ? <EyeOff /> : <Eye />}
        </button>
      </span>
    </label>
  );
}

const STRENGTH = [
  { label: "", color: "var(--line)" },
  { label: "Weak", color: "var(--danger)" },
  { label: "Fair", color: "#c98a00" },
  { label: "Good", color: "var(--brand)" },
  { label: "Strong", color: "var(--brand-dark)" },
];

/** 4-bar strength meter plus the live rule checklist. */
export function PasswordMeter({ password }: { password: string }) {
  const score = passwordScore(password);
  const s = STRENGTH[score];

  return (
    <div className={f.meter}>
      <div className={f.meterRow}>
        <div className={f.bars} aria-hidden>
          {[0, 1, 2, 3].map((i) => (
            <div key={i} className={f.bar} style={i < score ? { background: s.color } : undefined} />
          ))}
        </div>
        <span className={f.strength} style={{ color: s.color }} aria-live="polite">
          {password ? s.label : ""}
        </span>
      </div>
      <ul className={f.rules}>
        {PASSWORD_RULES.map((r) => {
          const ok = r.test(password);
          return (
            <li key={r.label} className={f.rule} data-ok={ok}>
              <span className={f.ruleDot}>
                <Check size={9} strokeWidth={4} />
              </span>
              {r.label}
              <span className="sr-only">{ok ? " — met" : " — not met"}</span>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
