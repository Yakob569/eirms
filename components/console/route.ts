"use client";

import { usePathname } from "next/navigation";
import { useConsole } from "./ConsoleProvider";

export type Section = "dashboard" | "invoices" | "canceled" | "settings";

/** Which sidebar item is active and what the top-bar breadcrumb reads, from the URL. */
export function useConsoleRoute(): { section: Section; crumb1: string; crumb2: string | null } {
  const path = usePathname();
  const { getInvoice } = useConsole();

  if (path.startsWith("/settings")) return { section: "settings", crumb1: "Settings", crumb2: "Setup" };
  if (path.startsWith("/canceled-invoices")) return { section: "canceled", crumb1: "Canceled invoices", crumb2: null };
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
