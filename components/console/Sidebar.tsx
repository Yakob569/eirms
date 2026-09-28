"use client";

import Link from "next/link";
import { Fragment, useMemo } from "react";
import { dashboardStats } from "@/lib/dashboard";
import { MERCHANT } from "@/lib/merchant";
import { useConsole } from "./ConsoleProvider";
import { Icon, type IconName } from "./Icon";
import { useConsoleRoute, type Section } from "./route";
import s from "./shell.module.css";

type NavItem = { key: Section | "terminal" | "receipts" | "items"; label: string; icon: IconName; href?: string; count?: number };

export function Sidebar() {
  const { collapsed, toggleSidebar, range, branch, setupDone } = useConsole();
  const { section } = useConsoleRoute();
  const stats = useMemo(() => dashboardStats(range, branch), [range, branch]);
  const expanded = !collapsed;

  // Terminal, Receipts and Items aren't designed yet, so they don't navigate.
  const nav: NavItem[] = [
    { key: "dashboard", label: "Dashboard", icon: "dashboard", href: "/dashboard" },
    { key: "terminal", label: "Terminal", icon: "terminal" },
    { key: "invoices", label: "Invoices", icon: "invoice", href: "/invoices", count: stats.invoices },
    { key: "receipts", label: "Receipts", icon: "receipt", count: stats.receipts },
    { key: "canceled", label: "Canceled invoices", icon: "canceled", href: "/canceled-invoices" },
    { key: "items", label: "Items", icon: "items" },
    { key: "settings", label: "Settings", icon: "settings", href: "/settings" },
  ];

  return (
    <aside className={s.aside} data-collapsed={collapsed}>
      <div className={s.brandRow}>
        <Link href="/dashboard" className={s.brand}>
          <span className={s.mark}>
            <Icon name="shield" size={17} />
          </span>
          {expanded && <span className={s.word}>EIRSPay</span>}
        </Link>
        {expanded && (
          <button type="button" className={s.iconBtn} onClick={toggleSidebar} aria-label="Collapse sidebar">
            <Icon name="panelClose" size={16} />
          </button>
        )}
      </div>

      {expanded ? (
        <div className={s.merchant}>
          <div className={s.merchantMark}>{MERCHANT.initials}</div>
          <div className={s.merchantText}>
            <span className={s.merchantName}>{MERCHANT.name}</span>
            <span className={s.merchantId}>{MERCHANT.id}</span>
          </div>
          <Icon name="chevronsUpDown" size={14} stroke="var(--muted)" />
        </div>
      ) : (
        <button type="button" className={s.expandBtn} onClick={toggleSidebar} aria-label="Expand sidebar">
          <Icon name="panelOpen" size={16} />
        </button>
      )}

      <nav className={s.nav}>
        {nav.map((n) => {
          const inner = (
            <>
              <Icon name={n.icon} size={18} strokeWidth={1.8} />
              {expanded && <span className={s.navLabel}>{n.label}</span>}
              {expanded && n.count != null && <span className={s.navCount}>{n.count}</span>}
            </>
          );
          return (
            <Fragment key={n.key}>
              {n.key === "settings" && <div className={s.navSep} />}
              {n.href ? (
                <Link href={n.href} className={s.navItem} data-active={section === n.key} title={n.label}>
                  {inner}
                </Link>
              ) : (
                <span className={s.navItem} aria-disabled title={n.label}>
                  {inner}
                </span>
              )}
            </Fragment>
          );
        })}
      </nav>

      <div style={{ flex: 1 }} />

      {expanded && (
        <div className={s.status}>
          <div className={s.statusHead}>
            <span>{setupDone < 5 ? "Fiscal readiness" : "Verified with MoR"}</span>
            <span className={s.statusCount}>{setupDone}/5</span>
          </div>
          <div className={s.track}>
            <div className={s.fill} style={{ width: `${setupDone * 20}%` }} />
          </div>
          <Link href="/settings" className={s.statusCta}>
            {setupDone < 5 ? "Continue setup →" : "View setup →"}
          </Link>
        </div>
      )}
    </aside>
  );
}
