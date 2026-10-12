"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { Fragment, useEffect, useState } from "react";
import { BRANCHES, CURRENT_USER, MERCHANT, branchName, type BranchKey } from "@/lib/merchant";
import { useConsole } from "./ConsoleProvider";
import { Icon } from "./Icon";
import { useConsoleRoute } from "./route";
import s from "./shell.module.css";

const BRANCH_OPTIONS: { key: BranchKey; label: string; sub: string }[] = [
  { key: "all", label: "All branches", sub: `Combined view · ${BRANCHES.length} branches` },
  ...BRANCHES.map((b) => ({ key: b.key, label: b.name, sub: b.sub })),
];

export function Topbar() {
  const router = useRouter();
  const { branch, setBranch, cred } = useConsole();
  const { crumb1, crumb2 } = useConsoleRoute();
  const [open, setOpen] = useState<"branch" | "user" | null>(null);

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && setOpen(null);
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open]);

  const toggle = (menu: "branch" | "user") => setOpen((o) => (o === menu ? null : menu));

  return (
    <header className={s.header}>
      <div className={s.crumbs}>
        <span>{crumb1}</span>
        {crumb2 && (
          <>
            <span className={s.crumbSep}>/</span>
            <b>{crumb2}</b>
          </>
        )}
      </div>
      <div className={s.spacer} />

      {open && <div className={s.scrim} onClick={() => setOpen(null)} />}

      <div className={s.menuAnchor}>
        <button
          type="button"
          className={s.branchBtn}
          data-filtered={branch !== "all"}
          aria-expanded={open === "branch"}
          aria-haspopup="menu"
          onClick={() => toggle("branch")}
        >
          <Icon name="building" size={15} stroke="var(--muted)" />
          <b>{branchName(branch) ?? "All branches"}</b>
          <Icon name="chevronDown" size={14} stroke="var(--muted)" />
        </button>
        {open === "branch" && (
          <div className={s.menu} role="menu">
            <span className={s.menuTitle}>VIEW DATA FOR</span>
            {BRANCH_OPTIONS.map((o, i) => (
              <Fragment key={o.key}>
                {i === 1 && <div className={s.menuSep} />}
                <button
                  type="button"
                  role="menuitemradio"
                  aria-checked={branch === o.key}
                  className={s.option}
                  onClick={() => {
                    setBranch(o.key);
                    setOpen(null);
                  }}
                >
                  <span className={s.optionText}>
                    <b>{o.label}</b>
                    <span>{o.sub}</span>
                  </span>
                  {branch === o.key && <Icon name="check" size={15} stroke="var(--brand)" strokeWidth={2.6} />}
                </button>
              </Fragment>
            ))}
          </div>
        )}
      </div>

      <div className={s.env}>
        <span className={s.envDot} />
        MoR connected · {cred.env}
      </div>

      <div className={s.userAnchor}>
        <button
          type="button"
          className={s.userBtn}
          aria-expanded={open === "user"}
          aria-haspopup="menu"
          onClick={() => toggle("user")}
        >
          <span className={s.avatar}>{CURRENT_USER.initials}</span>
          <span className={s.userText}>
            <b>{CURRENT_USER.name}</b>
            <span>{CURRENT_USER.role}</span>
          </span>
          <Icon name="chevronDown" size={14} stroke="var(--muted)" className={s.chevron} />
        </button>
        {open === "user" && (
          <div className={s.userMenu} role="menu">
            <div className={s.userHead}>
              <span className={s.avatar}>{CURRENT_USER.initials}</span>
              <div className={s.userHeadText}>
                <b>{CURRENT_USER.name}</b>
                <span>{CURRENT_USER.email}</span>
              </div>
            </div>
            <dl className={s.userFacts} style={{ margin: 0 }}>
              <div>
                <dt>Merchant</dt>
                <dd>{MERCHANT.name}</dd>
              </div>
              <div>
                <dt>Merchant ID</dt>
                <dd className="mono">{MERCHANT.id}</dd>
              </div>
              <div>
                <dt>User ID</dt>
                <dd className="mono">{CURRENT_USER.userId}</dd>
              </div>
              <div>
                <dt>Role</dt>
                <dd>
                  <span className={s.role}>{CURRENT_USER.role.toUpperCase()}</span>
                </dd>
              </div>
            </dl>
            <div className={s.userActions}>
              <Link href="/settings/account" className={s.userAction} role="menuitem" onClick={() => setOpen(null)}>
                <Icon name="user" size={16} strokeWidth={1.8} />
                Account settings
              </Link>
              <Link href="/settings/account" className={s.userAction} role="menuitem" onClick={() => setOpen(null)}>
                <Icon name="lock" size={16} strokeWidth={1.8} />
                Change password
              </Link>
              <button type="button" className={s.userAction} role="menuitem">
                <Icon name="globe" size={16} strokeWidth={1.8} />
                Language
                <em>English</em>
              </button>
              <div className={s.menuSep} />
              <button
                type="button"
                className={`${s.userAction} ${s.signOut}`}
                role="menuitem"
                onClick={() => {
                  setOpen(null);
                  router.push("/signin");
                }}
              >
                <Icon name="logout" size={16} strokeWidth={1.8} />
                Sign out
              </button>
            </div>
          </div>
        )}
      </div>
    </header>
  );
}
