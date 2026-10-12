"use client";

import { Fragment, useEffect, useRef, useState } from "react";
import { Icon } from "./Icon";
import iv from "./invoices/invoices.module.css";

export type RowAction = {
  label: string;
  onSelect: () => void;
  danger?: boolean;
  /** Draw a divider above this action. */
  separated?: boolean;
  /** Visible but unavailable; the text is the tooltip. */
  disabledReason?: string;
};

/**
 * Trailing ⋮ menu on a list row. Clicks inside never reach the row (which opens
 * the detail page); clicking outside or pressing Escape closes it.
 */
export function RowMenu({ actions, label, onOpenChange }: { actions: RowAction[]; label: string; onOpenChange?: (open: boolean) => void }) {
  const [open, setOpenState] = useState(false);
  const ref = useRef<HTMLSpanElement>(null);
  const setOpen = (o: boolean) => {
    setOpenState(o);
    onOpenChange?.(o);
  };

  useEffect(() => {
    if (!open) return;
    const onDown = (e: MouseEvent) => {
      if (!ref.current?.contains(e.target as Node)) {
        setOpenState(false);
        onOpenChange?.(false);
      }
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        setOpenState(false);
        onOpenChange?.(false);
      }
    };
    document.addEventListener("mousedown", onDown);
    window.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDown);
      window.removeEventListener("keydown", onKey);
    };
  }, [open, onOpenChange]);

  return (
    <span ref={ref} className={iv.kebabWrap} onClick={(e) => e.stopPropagation()} onKeyDown={(e) => e.stopPropagation()}>
      <button
        type="button"
        className={iv.kebab}
        aria-label={`Actions for ${label}`}
        aria-haspopup="menu"
        aria-expanded={open}
        title="Actions"
        onClick={() => setOpen(!open)}
      >
        <Icon name="more" size={16} strokeWidth={3.2} />
      </button>
      {open && (
        <div className={iv.rowMenu} role="menu" aria-label={`Actions for ${label}`}>
          {actions.map((a) => (
            <Fragment key={a.label}>
              {a.separated && <div className={iv.rowMenuSep} />}
              <button
                type="button"
                role="menuitem"
                className={iv.rowMenuItem}
                data-danger={a.danger}
                aria-disabled={!!a.disabledReason}
                title={a.disabledReason}
                onClick={() => {
                  if (a.disabledReason) return;
                  setOpen(false);
                  a.onSelect();
                }}
              >
                {a.label}
              </button>
            </Fragment>
          ))}
        </div>
      )}
    </span>
  );
}
