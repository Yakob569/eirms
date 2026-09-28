"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import type { Invoice, Party } from "@/lib/merchant";
import { qrCells } from "@/lib/merchant";

/** Copies text and remembers which key was copied for ~1.4s, for the "Copied" state. */
export function useCopy(): [string | null, (key: string, text: string) => void] {
  const [copied, setCopied] = useState<string | null>(null);
  const timer = useRef<ReturnType<typeof setTimeout>>(undefined);
  useEffect(() => () => clearTimeout(timer.current), []);
  const copy = useCallback((key: string, text: string) => {
    navigator.clipboard?.writeText(text).catch(() => {});
    setCopied(key);
    clearTimeout(timer.current);
    timer.current = setTimeout(() => setCopied(null), 1400);
  }, []);
  return [copied, copy];
}

/** The MoR-signed QR as a 21×21 grid on white. */
export function SignedQr({ irn, size, padding, radius }: { irn: string; size: number; padding: number; radius: number }) {
  const cells = qrCells(irn);
  return (
    <div
      aria-hidden
      style={{
        width: size,
        height: size,
        flex: "none",
        display: "grid",
        gridTemplateColumns: "repeat(21, 1fr)",
        padding,
        background: "#fff",
        borderRadius: radius,
      }}
    >
      {cells.map((on, i) => (
        <div key={i} style={{ background: on ? "var(--ink)" : "transparent" }} />
      ))}
    </div>
  );
}

/** "TIN 0041527796", "NID FAN 4521 …" or "Walk-in". */
export function partyIdLabel(b: Party): string {
  if (b.tin) return `TIN ${b.tin}`;
  if (b.idType) return `${b.idType} ${b.id}`;
  return "Walk-in";
}

/** Rows shown for the buyer: business buyers by TIN, consumers by ID. */
export function buyerRows(b: Party): [string, string][] {
  return b.tin
    ? [
        ["TIN", b.tin],
        ["VAT number", b.vat || "—"],
        ["Address", b.addr || "—"],
        ["Phone", b.phone || "—"],
        ["Email", b.email || "—"],
      ]
    : [
        ["ID type", b.idType || "Walk-in"],
        ["ID number", b.id || "—"],
        ["Phone", b.phone || "—"],
      ];
}

export function sellerRows(s: Party): [string, string][] {
  return [
    ["TIN", s.tin ?? "—"],
    ["VAT number", s.vat ?? "—"],
    ["Address", s.addr ?? "—"],
    ["Phone", s.phone ?? "—"],
    ["Email", s.email ?? "—"],
  ];
}

/** Where "back" goes from a document: canceled ones live in their own list. */
export const listHrefFor = (inv: Invoice) => (inv.status === "CANCELED" ? "/canceled-invoices" : "/invoices");
