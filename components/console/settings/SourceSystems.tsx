"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { SELLER } from "@/lib/merchant";
import { EXCISE, SYSTEM_TAX, SYSTEM_TYPES, blankSystem, plural, systemTax, verifySourceSystem, type SourceSystem } from "@/lib/org";
import { useConsole } from "../ConsoleProvider";
import { Icon } from "../Icon";
import ui from "../ui.module.css";
import { Missing } from "./Branches";
import { ChipMultiSelect } from "./ChipMultiSelect";
import { Crumbs, Field, SectionCard, SettingsHead, Switch } from "./kit";
import k from "./kit.module.css";
import st from "./settings.module.css";
import { StatusPill } from "./StatusPill";

const COLS = { gridTemplateColumns: "minmax(0,1.5fr) minmax(0,.9fr) minmax(0,1.3fr) 96px 90px 16px" };

/** Merchant-wide excise switch; sits above the source-system list. */
function ExciseBanner() {
  const { org, updateOrg } = useConsole();
  return (
    <div className={st.excise}>
      <span className={k.headIcon} data-solid="true">
        <Icon name="withholding" size={17} strokeWidth={1.8} />
      </span>
      <span className={k.headText} style={{ gap: 3 }}>
        <b style={{ display: "flex", alignItems: "center", gap: 10 }}>
          Merchant excise
          <StatusPill status={org.excise ? "Enabled" : "Not enabled"} size="sm" />
        </b>
        <span>
          {org.excise
            ? "EXCISE is now merged into every source system's tax types and can be granted to API keys."
            : "Excise eligibility applies to your whole business, not per till. When on, EXCISE is added to every source system automatically."}
        </span>
      </span>
      <span className={k.fieldLabel}>Business-wide</span>
      <Switch on={org.excise} onToggle={() => updateOrg((o) => ({ ...o, excise: !o.excise }))} />
    </div>
  );
}

export function SourceSystemList() {
  const router = useRouter();
  const { org, updateOrg } = useConsole();
  const branchName = (id: string) => org.branches.find((b) => b.id === id)?.name ?? "—";

  function add() {
    const s = blankSystem(org.branches.find((b) => b.main)?.id ?? org.branches[0]?.id ?? "main");
    updateOrg((o) => ({ ...o, systems: [...o.systems, s] }));
    router.push(`/settings/source-systems/${s.id}`);
  }

  return (
    <main className={k.page}>
      <SettingsHead
        eyebrow={`ACCOUNT · ${plural(org.systems.length, "source system")}`}
        title="Source"
        accent="systems."
        lede="Every till, ERP or storefront that sends documents to EIRS. Each belongs to one branch."
      >
        <button type="button" className={`${k.primary} ${k.primarySm}`} onClick={add}>
          <Icon name="plus" size={15} strokeWidth={2.2} />
          Add source system
        </button>
      </SettingsHead>

      <ExciseBanner />

      <div className={k.table}>
        <div className={k.tableHead} style={COLS}>
          <span>SOURCE SYSTEM</span>
          <span>BRANCH</span>
          <span>TAX TYPES</span>
          <span>VERIFIED</span>
          <span>STATUS</span>
          <span />
        </div>
        {org.systems.map((s) => {
          const tax = systemTax(org, s);
          return (
            <Link key={s.id} href={`/settings/source-systems/${s.id}`} className={k.tableRow} style={COLS}>
              <span className={k.nameCell}>
                <span className={k.headIcon}>
                  <Icon name="server" size={17} strokeWidth={1.8} />
                </span>
                <span className={k.nameText}>
                  <b>{s.label}</b>
                  <span className={k.subMono}>
                    {s.type} · {s.id}
                  </span>
                </span>
              </span>
              <span className={k.cellText}>{branchName(s.branchId)}</span>
              <ChipMultiSelect
                size="sm"
                options={tax.filter((t) => t !== EXCISE)}
                value={tax}
                derived={tax.includes(EXCISE) ? [{ value: EXCISE, tip: "Derived from your merchant excise status" }] : undefined}
                ariaLabel="Tax types"
              />
              <StatusPill status={s.ver} />
              <StatusPill status={s.active ? "Active" : "Inactive"} />
              <Icon name="chevronRight" size={15} stroke="var(--muted)" />
            </Link>
          );
        })}
      </div>
    </main>
  );
}

export function SourceSystemDetail({ id }: { id: string }) {
  const { org, ready } = useConsole();
  const s = org.systems.find((x) => x.id === id);
  if (!s) return ready ? <Missing what="source system" href="/settings/source-systems" back="Source systems" /> : null;
  return <SystemForm key={s.id} initial={s} />;
}

function SystemForm({ initial }: { initial: SourceSystem }) {
  const router = useRouter();
  const { org, updateOrg } = useConsole();
  const [s, setS] = useState(initial);
  const [hsn, setHsn] = useState("");
  const [running, setRunning] = useState(false);
  const set = (patch: Partial<SourceSystem>) => setS((x) => ({ ...x, ...patch }));

  const branch = org.branches.find((b) => b.id === s.branchId);
  const keys = org.keys.filter((key) => key.systemId === initial.id);
  const deleteTip = keys.length ? `Remove its ${plural(keys.length, "API key")} first` : "";
  const saved = org.systems.find((x) => x.id === initial.id) ?? initial;

  function save() {
    updateOrg((o) => ({ ...o, systems: o.systems.map((x) => (x.id === s.id ? s : x)) }));
    router.push("/settings/source-systems");
  }

  async function verify() {
    setRunning(true);
    const result = await verifySourceSystem(s.id, hsn);
    setRunning(false);
    set(result);
    // The verification result is MoR's, so it's recorded even if the form isn't saved.
    updateOrg((o) => ({ ...o, systems: o.systems.map((x) => (x.id === s.id ? { ...x, ...result } : x)) }));
  }

  function remove() {
    if (deleteTip) return;
    updateOrg((o) => ({ ...o, systems: o.systems.filter((x) => x.id !== s.id) }));
    router.push("/settings/source-systems");
  }

  return (
    <main className={k.page}>
      <div className={k.titleStack}>
        <Crumbs
          trail={[
            { label: branch?.name ?? "Branch", href: branch ? `/settings/branches/${branch.id}` : undefined },
            { label: "Source systems", href: "/settings/source-systems" },
            { label: saved.label || "Untitled" },
          ]}
        />
        <h1 className={ui.title}>
          {s.label || "Untitled"} <i>system.</i>
        </h1>
        <div className={k.subline}>
          Belongs to {branch ? <Link href={`/settings/branches/${branch.id}`}>{branch.name}</Link> : "—"} branch
          <StatusPill status={s.ver} size="sm" />
        </div>
      </div>

      <SectionCard icon="server" title="Basic info" sub="What this system is and which branch it issues for.">
        <div className={k.body}>
          <Field label="Label" wrapClassName={k.narrow} value={s.label} onValue={(v) => set({ label: v })} />
          <div className={k.field}>
            <span className={k.fieldLabel}>System type</span>
            <ChipMultiSelect single options={SYSTEM_TYPES} value={[s.type]} onChange={(v) => set({ type: v[0] })} ariaLabel="System type" />
          </div>
          <div className={k.field}>
            <span className={k.fieldLabel}>Branch</span>
            <ChipMultiSelect
              single
              options={org.branches.map((b) => ({ value: b.id, label: b.name }))}
              value={[s.branchId]}
              onChange={(v) => set({ branchId: v[0] })}
              locked={Object.fromEntries(org.branches.filter((b) => !b.active && b.id !== s.branchId).map((b) => [b.id, "Branch is inactive"]))}
              ariaLabel="Branch"
            />
          </div>
          <div className={k.field}>
            <span className={k.fieldLabel}>Status</span>
            <Switch on={s.active} onToggle={() => set({ active: !s.active })} label={s.active ? "Active" : "Inactive"} note={s.active ? "Can issue documents" : "Paused"} />
          </div>
        </div>
      </SectionCard>

      <SectionCard icon="team" title="Defaults" sub="Pre-filled on documents when the system doesn't send them.">
        <div className={k.grid2}>
          <Field label="Default cashier name" placeholder="e.g. Selam Girma" value={s.cashier} onValue={(v) => set({ cashier: v })} />
          <Field label="Default salesperson" placeholder="e.g. Hana Tesfaye" value={s.sales} onValue={(v) => set({ sales: v })} />
        </div>
      </SectionCard>

      <SectionCard icon="withholding" title="Tax types" sub="Which taxes this system may charge." flush>
        <div className={k.rule}>
          <ChipMultiSelect
            ariaLabel="Tax types"
            options={SYSTEM_TAX}
            value={s.tax}
            onChange={(v) => set({ tax: v })}
            derived={org.excise ? [{ value: EXCISE, tip: "Derived from your merchant excise status" }] : undefined}
            caption={
              org.excise
                ? "EXCISE is derived from your merchant excise status — not set here."
                : "EXCISE is added here automatically once excise is enabled for your business."
            }
          />
        </div>
      </SectionCard>

      <SectionCard icon="checkCircle" title="Verification" sub={s.verAt} action={<StatusPill status={s.ver} />}>
        <div className={k.body} style={{ flexDirection: "row", alignItems: "flex-end", gap: 14, flexWrap: "wrap" }}>
          <Field label="Test transaction HSN code" mono placeholder="8471" wrapClassName="" style={{ width: 220 }} value={hsn} onValue={setHsn} />
          <button type="button" className={`${k.primary} ${k.primarySm}`} disabled={running} onClick={verify}>
            {running ? "Verifying…" : "Run verification"}
          </button>
          <span className={k.note} style={{ flex: 1, minWidth: 200 }}>
            Sends a zero-value test document to MoR and checks the signed response.
          </span>
        </div>
      </SectionCard>

      <div className={k.seq} aria-label="Sequence info">
        {(
          [
            ["SYSTEM ID", s.id],
            ["SELLER TIN", SELLER.tin ?? "—"],
            ["LAST DOCUMENT NO.", s.lastDoc],
          ] as const
        ).map(([label, v]) => (
          <div key={label}>
            <span>{label}</span>
            <b>{v}</b>
          </div>
        ))}
      </div>

      <div className={k.footer}>
        <button type="button" className={k.primary} onClick={save}>
          Save source system
        </button>
        <Link href="/settings/source-systems" className={k.ghost}>
          Cancel
        </Link>
        <span className={k.footerSpacer} />
        <span title={deleteTip || undefined}>
          <button type="button" className={k.dangerLink} disabled={!!deleteTip} onClick={remove}>
            Delete
          </button>
        </span>
      </div>
    </main>
  );
}
