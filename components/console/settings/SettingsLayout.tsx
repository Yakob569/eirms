"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Fragment } from "react";
import { useConsole } from "../ConsoleProvider";
import { Icon } from "../Icon";
import st from "./settings.module.css";

type NavItem = { label: string; href?: string; group?: string; step?: number };

// Departments, Team and Roles aren't designed yet, so they aren't links.
export const SETTINGS_NAV: NavItem[] = [
  { label: "Setup", href: "/settings" },
  { label: "Account", href: "/settings/account", group: "Account" },
  { label: "MoR Credentials", href: "/settings/mor-credentials", step: 0 },
  { label: "Seller Profile", href: "/settings/seller-profile", step: 1 },
  { label: "Source Systems", href: "/settings/source-systems", step: 2 },
  { label: "Verify Setup", href: "/settings/verify", step: 3 },
  { label: "Branches", href: "/settings/branches", group: "Organization" },
  { label: "Departments" },
  { label: "API Keys", href: "/settings/api-keys", group: "Access" },
  { label: "Team" },
  { label: "Roles" },
];

/** Which Settings page a path belongs to (detail pages count as their list). */
export function settingsPage(path: string): NavItem | undefined {
  return [...SETTINGS_NAV].reverse().find((n) => n.href && (path === n.href || (n.href !== "/settings" && path.startsWith(`${n.href}/`))));
}

/** Settings sub-navigation grouped Account / Organization / Access, beside the page. */
export function SettingsLayout({ children }: { children: React.ReactNode }) {
  const path = usePathname();
  const { setupDone: done } = useConsole();
  const current = settingsPage(path);

  return (
    <div className={st.layout}>
      <nav className={st.subnav} aria-label="Settings">
        <span className={st.subnavTitle}>SETTINGS</span>
        {SETTINGS_NAV.map((n) => (
          <Fragment key={n.label}>
            {n.group && <span className={st.group}>{n.group}</span>}
            {n.href ? (
              <Link href={n.href} className={st.item} aria-current={current === n ? "page" : undefined}>
                <span>{n.label}</span>
                {n.step != null && n.step < done && <Icon name="check" size={14} stroke="var(--brand)" strokeWidth={2.6} />}
              </Link>
            ) : (
              <span className={st.item} data-soon="true" title="Not built yet">
                <span>{n.label}</span>
              </span>
            )}
          </Fragment>
        ))}
      </nav>
      {children}
    </div>
  );
}
