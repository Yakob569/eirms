import { Icon } from "../Icon";
import k from "./kit.module.css";

export type ChipOption = string | { value: string; label?: string };

type Props = {
  options: ChipOption[];
  value: string[];
  /** Omit for a read-only chip list. */
  onChange?: (next: string[]) => void;
  /** Pick exactly one (pickers: system type, branch, manager, bound source system). */
  single?: boolean;
  /**
   * What the parent allows. Options outside it are greyed out, can't be picked
   * and show as unselected even if the value still holds them.
   */
  allowed?: string[];
  /** Tooltip for an option outside `allowed`. */
  disallowedTip?: (value: string) => string;
  /** Options that can't be changed here, each with its own tooltip. */
  locked?: Record<string, string>;
  /** Read-only chips merged in from elsewhere, drawn dashed (e.g. EXCISE from the merchant). */
  derived?: { value: string; tip: string }[];
  /** Group frame: title with an "n of m" count, and a caption underneath. */
  title?: string;
  caption?: string;
  size?: "sm";
  ariaLabel?: string;
};

/** The single chip multi-select used across Settings — business rules, tax types, key capabilities, pickers. */
export function ChipMultiSelect({ options, value, onChange, single, allowed, disallowedTip, locked, derived, title, caption, size, ariaLabel }: Props) {
  const opts = options.map((o) => (typeof o === "string" ? { value: o, label: o } : { value: o.value, label: o.label ?? o.value }));
  const isOut = (v: string) => !!allowed && !allowed.includes(v);
  const isOn = (v: string) => value.includes(v) && !isOut(v);
  const readOnly = !onChange;
  const cls = `${k.chip} ${size === "sm" ? k.chipSm : ""}`;

  function toggle(v: string) {
    if (!onChange) return;
    if (single) onChange([v]);
    else onChange(value.includes(v) ? value.filter((x) => x !== v) : [...value.filter((x) => !isOut(x)), v]);
  }

  const chips = (
    <div className={k.chips} role="group" aria-label={ariaLabel ?? title}>
      {opts.map((o) => {
        const on = isOn(o.value);
        const out = isOut(o.value);
        const lock = locked?.[o.value];
        const tip = out ? (disallowedTip?.(o.value) ?? "Not allowed here") : (lock ?? "");
        const check = on && size !== "sm" && <Icon name="check" size={12} strokeWidth={3} />;
        return readOnly ? (
          <span key={o.value} className={cls} data-on={on} title={tip || undefined}>
            {check}
            {o.label}
          </span>
        ) : (
          <button
            key={o.value}
            type="button"
            className={cls}
            data-on={on}
            aria-pressed={on}
            disabled={out || !!lock}
            title={tip || undefined}
            onClick={() => toggle(o.value)}
          >
            {check}
            {o.label}
          </button>
        );
      })}
      {derived?.map((d) => (
        <span key={d.value} className={cls} data-on="true" data-derived="true" title={d.tip}>
          {size !== "sm" && <Icon name="check" size={12} strokeWidth={3} />}
          {d.value}
        </span>
      ))}
    </div>
  );

  if (!title && !caption) return chips;
  const count = opts.filter((o) => isOn(o.value)).length;
  return (
    <div className={k.chipGroup}>
      {title && (
        <div className={k.chipGroupHead}>
          <b>{title}</b>
          <span className={k.chipCount}>
            {count} of {opts.length}
          </span>
        </div>
      )}
      {chips}
      {caption && <span className={k.caption}>{caption}</span>}
    </div>
  );
}
