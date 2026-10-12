import Link from "next/link";
import { Fragment } from "react";
import { Icon, type IconName } from "../Icon";
import ui from "../ui.module.css";
import k from "./kit.module.css";

/** Eyebrow, serif headline with one italic accent word, and a one-line lede. */
export function SettingsHead({
  eyebrow,
  title,
  accent,
  lede,
  children,
}: {
  eyebrow: string;
  title: string;
  accent: string;
  lede?: string;
  children?: React.ReactNode;
}) {
  return (
    <div className={k.head}>
      <div className={ui.titleBlock}>
        <span className={ui.eyebrow}>{eyebrow}</span>
        <h1 className={ui.title}>
          {title} <i>{accent}</i>
        </h1>
        {lede && <p className={k.lede}>{lede}</p>}
      </div>
      {children}
    </div>
  );
}

/** Breadcrumb for the Branch → Source system → API key nesting. The last crumb is the current page. */
export function Crumbs({ trail }: { trail: { label: string; href?: string }[] }) {
  return (
    <nav className={k.crumbs} aria-label="Breadcrumb">
      {trail.map((c, i) => (
        <Fragment key={`${c.label}-${i}`}>
          {i > 0 && <span className={k.crumbSep}>/</span>}
          {c.href ? <Link href={c.href}>{c.label}</Link> : <b>{c.label}</b>}
        </Fragment>
      ))}
    </nav>
  );
}

/** Card with a leading icon chip, bold title + muted description, and an optional trailing action. */
export function SectionCard({
  icon,
  title,
  sub,
  action,
  tone,
  flush,
  children,
}: {
  icon: IconName;
  title: string;
  sub?: React.ReactNode;
  action?: React.ReactNode;
  tone?: "brand";
  /** No divider under the head (content brings its own rules). */
  flush?: boolean;
  children?: React.ReactNode;
}) {
  return (
    <section className={k.card} data-tone={tone}>
      <div className={k.cardHead} data-flush={flush || !children}>
        <span className={k.headIcon}>
          <Icon name={icon} size={17} strokeWidth={1.8} />
        </span>
        <span className={k.headText}>
          <b>{title}</b>
          {sub && <span>{sub}</span>}
        </span>
        {action ?? <span />}
      </div>
      {children}
    </section>
  );
}

type FieldProps = Omit<React.InputHTMLAttributes<HTMLInputElement>, "onChange"> & {
  label: string;
  mono?: boolean;
  onValue?: (v: string) => void;
  wrapClassName?: string;
};

/** Label over a 40px input. Without `onValue` the input is read-only. */
export function Field({ label, mono, onValue, wrapClassName, className, ...rest }: FieldProps) {
  return (
    <label className={`${k.field} ${wrapClassName ?? ""}`}>
      <span className={k.fieldLabel}>{label}</span>
      <input
        {...rest}
        readOnly={!onValue}
        onChange={onValue ? (e) => onValue(e.target.value) : undefined}
        className={`${k.input} ${mono ? k.mono : ""} ${className ?? ""}`}
      />
    </label>
  );
}

/** On / off switch with its label and a muted note. */
export function Switch({
  on,
  onToggle,
  disabled,
  tip,
  label,
  note,
}: {
  on: boolean;
  onToggle?: () => void;
  disabled?: boolean;
  tip?: string;
  label?: string;
  note?: string;
}) {
  return (
    <div className={k.switchRow}>
      <button
        type="button"
        role="switch"
        aria-checked={on}
        aria-label={label}
        className={k.switch}
        disabled={disabled}
        title={tip || undefined}
        onClick={onToggle}
      />
      {label && <b>{label}</b>}
      {note && <span className={k.note}>{note}</span>}
    </div>
  );
}
