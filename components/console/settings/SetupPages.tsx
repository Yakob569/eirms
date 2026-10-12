"use client";

import { useState } from "react";
import type { Credentials, SellerProfile } from "@/lib/org";
import { useConsole } from "../ConsoleProvider";
import { Icon } from "../Icon";
import ui from "../ui.module.css";
import { Field, SectionCard, SettingsHead } from "./kit";
import k from "./kit.module.css";
import st from "./settings.module.css";
import { StatusPill } from "./StatusPill";
import { VerifyPanel } from "./VerifyPanel";

/** Edit ↔ Save button for forms that are read-only until edited. */
function EditButton({ editing, onClick }: { editing: boolean; onClick: () => void }) {
  return (
    <div style={{ display: "flex", justifyContent: "flex-end", padding: "0 24px 20px" }}>
      <button type="button" className={editing ? `${k.primary} ${k.primarySm}` : `${k.ghost} ${k.ghostSm}`} style={{ height: 40 }} onClick={onClick}>
        <Icon name="edit" size={14} />
        {editing ? "Save" : "Edit"}
      </button>
    </div>
  );
}

// ─── MoR credentials ───────────────────────────────────────────────────────

export function MorCredentials() {
  const { cred, setCred, setupDone } = useConsole();
  const [draft, setDraft] = useState<Credentials | null>(null);
  const [show, setShow] = useState(false);
  const editing = !!draft;
  const c = draft ?? cred;
  const set = (patch: Partial<Credentials>) => draft && setDraft({ ...draft, ...patch });
  const on = (f: keyof Credentials) => (editing ? (v: string) => set({ [f]: v }) : undefined);
  const verified = setupDone >= 5;

  return (
    <main className={k.page}>
      <SettingsHead
        eyebrow="SETUP · OWNER"
        title="MoR"
        accent="credentials."
        lede="These secrets sign every receipt registered with Ethiopia's Ministry of Revenues. Stored encrypted at rest."
      >
        <StatusPill status={verified ? "Verified with MoR" : "Not verified"}>
          <Icon name="shieldPlain" size={13} />
        </StatusPill>
      </SettingsHead>

      <SectionCard
        icon="key"
        title="EIRMS client"
        sub="Issued by MoR when your business was onboarded to EIRMS."
        action={
          <button type="button" className={k.link} onClick={() => setShow(!show)}>
            {show ? "Hide secrets" : "Show secrets"}
          </button>
        }
      >
        <div className={k.body} style={{ gap: 16 }}>
          <div className={k.grid2} style={{ padding: 0 }}>
            <Field label="Client ID" mono value={c.clientId} onValue={on("clientId")} />
            <Field label="TIN" mono value={c.tin} onValue={on("tin")} />
          </div>
          <Field label="Client secret" mono type={show ? "text" : "password"} value={c.secret} onValue={on("secret")} />
          <Field label="API key" mono type={show ? "text" : "password"} value={c.apiKey} onValue={on("apiKey")} />
          <div className={k.field}>
            <span className={k.fieldLabel}>Environment</span>
            <div className={k.switchRow} style={{ gap: 14 }}>
              <div className={st.envSeg} role="group" aria-label="Environment">
                {(["DEV", "PROD"] as const).map((e) => (
                  <button key={e} type="button" aria-pressed={c.env === e} disabled={!editing} onClick={() => set({ env: e })}>
                    {e}
                  </button>
                ))}
              </div>
              <span className={k.note}>{c.env === "DEV" ? "Test documents only — nothing is filed with MoR." : "Live — every document is filed with MoR."}</span>
            </div>
          </div>
        </div>
        <EditButton
          editing={editing}
          onClick={() => {
            if (draft) setCred(draft);
            setDraft(draft ? null : cred);
          }}
        />
      </SectionCard>
    </main>
  );
}

// ─── Seller profile ────────────────────────────────────────────────────────

const ADDRESS: [keyof SellerProfile, string, boolean?][] = [
  ["region", "Region", true],
  ["city", "City"],
  ["wereda", "Wereda", true],
  ["subCity", "Sub city"],
  ["zone", "Zone"],
  ["kebele", "Kebele"],
  ["locality", "Locality"],
  ["house", "House number"],
  ["country", "Country"],
];

export function SellerProfilePage() {
  const { seller, setSeller } = useConsole();
  const [draft, setDraft] = useState<SellerProfile | null>(null);
  const editing = !!draft;
  const p = draft ?? seller;
  const on = (f: keyof SellerProfile) => (draft ? (v: string) => setDraft({ ...draft, [f]: v }) : undefined);

  return (
    <main className={k.page}>
      <SettingsHead eyebrow="SETUP · OWNER" title="Seller" accent="profile." lede="The legal identity printed on every receipt issued through EIRMS." />
      <section className={k.card}>
        <div className={k.cardHead}>
          <span className={k.headIcon}>
            <Icon name="building" size={17} strokeWidth={1.8} />
          </span>
          <span className={k.headText}>
            <b>Legal identity</b>
            <span>Must match your MoR registration exactly.</span>
          </span>
          <span />
        </div>
        <div className={k.grid2}>
          <Field label="TIN *" mono value={p.tin} onValue={on("tin")} />
          <Field label="Legal name *" value={p.legal} onValue={on("legal")} />
          <Field label="VAT number *" mono value={p.vat} onValue={on("vat")} />
        </div>
        <div className={k.cardHead} style={{ borderTop: "1px solid var(--line)" }}>
          <span className={k.headIcon}>
            <Icon name="phone" size={17} strokeWidth={1.8} />
          </span>
          <span className={k.headText}>
            <b>Contact</b>
            <span>Where MoR and your buyers reach the business.</span>
          </span>
          <span />
        </div>
        <div className={k.grid2}>
          <Field label="Email *" placeholder="finance@company.et" value={p.email} onValue={on("email")} />
          <Field label="Phone *" mono placeholder="+251 9…" value={p.phone} onValue={on("phone")} />
        </div>
        <div className={k.cardHead} style={{ borderTop: "1px solid var(--line)" }}>
          <span className={k.headIcon}>
            <Icon name="mapPin" size={17} strokeWidth={1.8} />
          </span>
          <span className={k.headText}>
            <b>Address</b>
            <span>Registered business address.</span>
          </span>
          <span />
        </div>
        <div className={k.grid3}>
          {ADDRESS.map(([f, label, req]) => (
            <Field key={f} label={`${label}${req ? " *" : ""}`} value={p[f]} onValue={on(f)} />
          ))}
        </div>
        <EditButton
          editing={editing}
          onClick={() => {
            if (draft) setSeller(draft);
            setDraft(draft ? null : seller);
          }}
        />
      </section>
    </main>
  );
}

// ─── Verify setup ──────────────────────────────────────────────────────────

const PROBE_ITEMS = [
  ["1", "Item VAT15", "1 PCS", "VAT15", "1,150.00 ETB"],
  ["2", "Item VAT0", "1 PCS", "VAT0", "500.00 ETB"],
];

export function VerifySetup() {
  const { seller, org } = useConsole();
  const system = org.systems[0];
  const cards: { label: string; rows: [string, string][] }[] = [
    {
      label: "SELLER DETAILS",
      rows: [
        ["Legal name", seller.legal],
        ["TIN", seller.tin],
        ["VAT number", seller.vat],
        ["Region", seller.region],
        ["Wereda", seller.wereda],
      ],
    },
    {
      label: "SOURCE SYSTEM",
      rows: [
        ["System type", system?.type ?? "—"],
        ["System number", system?.id ?? "—"],
        ["Cashier", system?.cashier || "—"],
        ["Sales person", system?.sales || "—"],
      ],
    },
    {
      label: "BUYER",
      rows: [
        ["Name", "Walk-in Customer"],
        ["ID type", "KID"],
      ],
    },
    {
      label: "VALUE",
      rows: [
        ["Tax", "150.00 ETB"],
        ["Total", "1,650.00 ETB"],
        ["Currency", "ETB"],
      ],
    },
  ];

  return (
    <main className={k.page} style={{ gap: 20 }}>
      <div style={{ marginBottom: 4 }}>
        <SettingsHead
          eyebrow="SETUP · OWNER"
          title="Verify"
          accent="setup."
          lede="Review the probe invoice that will be sent to Ethiopia's Ministry of Revenues, then run verification."
        />
      </div>
      <div className={st.probeGrid}>
        {cards.map((c) => (
          <div key={c.label} className={st.probeCard}>
            <span className={st.probeLabel}>{c.label}</span>
            {c.rows.map(([key, v]) => (
              <div key={key} className={st.probeRow}>
                <span>{key}</span>
                <span>{v || "—"}</span>
              </div>
            ))}
          </div>
        ))}
      </div>
      <section className={k.card}>
        <div className={st.probeLabel} style={{ padding: "16px 22px 10px" }}>
          PROBE ITEMS
        </div>
        <div className={st.probeItems} data-head="true" style={{ borderTop: 0 }}>
          <span>#</span>
          <span>Description</span>
          <span>Qty</span>
          <span>Tax</span>
          <span style={{ textAlign: "right" }}>Amount</span>
        </div>
        {PROBE_ITEMS.map(([n, d, q, t, a]) => (
          <div key={n} className={st.probeItems}>
            <span style={{ color: "var(--muted)" }}>{n}</span>
            <span>{d}</span>
            <span>{q}</span>
            <span className={ui.mono}>{t}</span>
            <span className={ui.mono} style={{ textAlign: "right" }}>
              {a}
            </span>
          </div>
        ))}
      </section>
      <VerifyPanel layout="center" />
    </main>
  );
}
