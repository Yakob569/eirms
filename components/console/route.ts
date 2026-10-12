"use client";

import { usePathname } from "next/navigation";
import { useConsole } from "./ConsoleProvider";
import { settingsPage } from "./settings/SettingsLayout";

export type Section = "dashboard" | "invoices" | "receipts" | "canceled" | "items" | "withholding" | "settings";

/** Which sidebar item is active and what the top-bar breadcrumb reads, from the URL. */
export function useConsoleRoute(): { section: Section; crumb1: string; crumb2: string | null } {
  const path = usePathname();
  const { getInvoice, getReceipt } = useConsole();

  if (path.startsWith("/settings")) return { section: "settings", crumb1: "Settings", crumb2: settingsPage(path)?.label ?? "Setup" };
  if (path.startsWith("/canceled-invoices")) return { section: "canceled", crumb1: "Canceled invoices", crumb2: null };
  if (path === "/receipts/new") return { section: "receipts", crumb1: "Receipts", crumb2: "Register" };
  const rm = /^\/receipts\/([^/]+)/.exec(path);
  if (rm) {
    const r = getReceipt(decodeURIComponent(rm[1]));
    return { section: "receipts", crumb1: "Receipts", crumb2: r ? `Receipt RCP-${r.number}` : "Receipt" };
  }
  if (path.startsWith("/receipts")) return { section: "receipts", crumb1: "Receipts", crumb2: null };
  if (path.startsWith("/items")) return { section: "items", crumb1: "Items", crumb2: null };
  const wm = /^\/withholding\/([^/]+)/.exec(path);
  if (wm) return { section: "withholding", crumb1: "Withholding", crumb2: decodeURIComponent(wm[1]) };
  if (path.startsWith("/withholding")) return { section: "withholding", crumb1: "Withholding", crumb2: null };
  if (path === "/invoices/new") return { section: "invoices", crumb1: "Invoices", crumb2: "Register" };

  const m = /^\/invoices\/([^/]+)(\/receipt)?/.exec(path);
  if (m) {
    const inv = getInvoice(decodeURIComponent(m[1]));
    const canceled = inv?.status === "CANCELED";
    return {
      section: canceled ? "canceled" : "invoices",
      crumb1: canceled ? "Canceled invoices" : "Invoices",
      crumb2: m[2] ? "Receipt" : `Invoice ${inv?.number ?? ""}`.trim(),
    };
  }
  if (path.startsWith("/invoices")) return { section: "invoices", crumb1: "Invoices", crumb2: null };
  return { section: "dashboard", crumb1: "Overview", crumb2: "Dashboard" };
}
