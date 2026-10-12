"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import {
  ADDRESS_FIELDS,
  DOC_TYPES,
  EXCISE,
  NATURES,
  OPERATIONS,
  PERMISSIONS,
  TX_TYPES,
  blankBranch,
  blankSystem,
  maskedKey,
  plural,
  systemTax,
  type OrgBranch,
} from "@/lib/org";
import { useConsole } from "../ConsoleProvider";
import { Icon } from "../Icon";
import ui from "../ui.module.css";
import { ChipMultiSelect } from "./ChipMultiSelect";
import { Crumbs, Field, SectionCard, SettingsHead, Switch } from "./kit";
import k from "./kit.module.css";
import st from "./settings.module.css";
import { StatusPill } from "./StatusPill";

const COLS = { gridTemplateColumns: "minmax(0,1.5fr) minmax(0,1fr) minmax(0,1.1fr) 150px 80px 16px" };
const RULE_CAPTION = "Source systems, API keys and users under this branch are limited to what's selected here.";

export function BranchList() {
  const router = useRouter();
  const { org, updateOrg } = useConsole();

  function addBranch() {
    const b = blankBranch(org.branches.length);
    updateOrg((o) => ({ ...o, branches: [...o.branches, b] }));
    router.push(`/settings/branches/${b.id}`);
  }

  return (
    <main className={k.page}>
      <SettingsHead
        eyebrow={`ORGANIZATION · ${plural(org.branches.length, "branch", "branches")}`}
        title="Your"
        accent="branches."
        lede="Each branch sets the rules its source systems and API keys inherit."
      >
        <button type="button" className={`${k.primary} ${k.primarySm}`} onClick={addBranch}>
          <Icon name="plus" size={15} strokeWidth={2.2} />
          Add branch
        </button>
      </SettingsHead>

      <div className={k.table}>
        <div className={k.tableHead} style={COLS}>
          <span>BRANCH</span>
          <span>MANAGER</span>
          <span>CITY · REGION</span>
          <span>LINKED</span>
          <span>STATUS</span>
          <span />
        </div>
        {org.branches.map((b) => {
          const n = org.systems.filter((s) => s.branchId === b.id).length;
          return (
            <Link key={b.id} href={`/settings/branches/${b.id}`} className={k.tableRow} style={COLS}>
              <span className={k.nameCell}>
                <span className={k.headIcon}>
                  <Icon name="building" size={17} strokeWidth={1.8} />
                </span>
                <span className={k.nameText}>
                  <b>
                    {b.name}
                    {b.main && <StatusPill status="Main branch" size="sm" />}
                  </b>
                  <span className={k.subMono}>{b.code}</span>
                </span>
              </span>
              <span className={k.cellText}>{b.manager || "Unassigned"}</span>
              <span className={k.cellMuted}>
                {b.address.city || "—"} · {b.address.region || "—"}
              </span>
              <span className={k.countBadge}>{plural(n, "source system")}</span>
              <StatusPill status={b.active ? "Active" : "Inactive"} />
              <Icon name="chevronRight" size={15} stroke="var(--muted)" />
            </Link>
          );
        })}
      </div>
    </main>
  );
}

export function BranchDetail({ id }: { id: string }) {
  const { org, ready } = useConsole();
  const b = org.branches.find((x) => x.id === id);
  if (!b) return ready ? <Missing what="branch" href="/settings/branches" back="Branches" /> : null;
  return <BranchForm key={b.id} initial={b} />;
}

function BranchForm({ initial }: { initial: OrgBranch }) {
  const router = useRouter();
  const { org, updateOrg } = useConsole();
  const [b, setB] = useState(initial);
  const [open, setOpen] = useState<string | null>(null);
  const set = (patch: Partial<OrgBranch>) => setB((x) => ({ ...x, ...patch }));

  const systems = org.systems.filter((s) => s.branchId === initial.id);
  const keyCount = org.keys.filter((key) => systems.some((s) => s.id === key.systemId)).length;
  const elig = [...new Set(systems.flatMap((s) => systemTax(org, s)))];
  const deleteTip = b.main ? "The main branch can't be deleted" : systems.length ? `Move or delete its ${plural(systems.length, "source system")} first` : "";

  function save() {
    updateOrg((o) => ({ ...o, branches: o.branches.map((x) => (x.id === b.id ? b : x)) }));
    router.push("/settings/branches");
  }

  function addSystem() {
    const s = blankSystem(initial.id);
    updateOrg((o) => ({ ...o, systems: [...o.systems, s] }));
    router.push(`/settings/source-systems/${s.id}`);
  }

  function remove() {
    if (deleteTip) return;
    updateOrg((o) => ({ ...o, branches: o.branches.filter((x) => x.id !== b.id) }));
    router.push("/settings/branches");
  }

  return (
    <main className={k.page}>
      <div className={k.titleStack}>
        <Crumbs trail={[{ label: "Branches", href: "/settings/branches" }, { label: b.name || "Untitled" }]} />
        <h1 className={ui.title}>
          {b.name || "Untitled"} <i>branch.</i>
        </h1>
        <span className={k.subMono}>
          {b.code} · {b.main ? "Main branch · " : ""}
          {plural(systems.length, "source system")} · {plural(keyCount, "API key")}
        </span>
      </div>

      <SectionCard icon="building" title="Basic info" sub="Name, code, status and who runs this branch.">
        <div className={k.grid2}>
          <Field label="Name" value={b.name} onValue={(v) => set({ name: v })} />
          <Field label="Code" mono value={b.code} onValue={(v) => set({ code: v })} />
          <div className={`${k.field} ${k.span2}`}>
            <span className={k.fieldLabel}>Status</span>
            <Switch
              on={b.active}
              onToggle={() => set({ active: !b.active })}
              disabled={b.main}
              tip={b.main ? "The main branch is always active" : undefined}
              label={b.active ? "Active" : "Inactive"}
              note={b.main ? "The main branch is always active" : b.active ? "Accepting documents" : "Paused — its source systems can't issue"}
            />
          </div>
          <div className={`${k.field} ${k.span2}`}>
            <span className={k.fieldLabel}>Branch manager</span>
            <ChipMultiSelect single options={org.staff} value={b.manager ? [b.manager] : []} onChange={(v) => set({ manager: v[0] })} ariaLabel="Branch manager" />
          </div>
        </div>
      </SectionCard>

      <SectionCard icon="mapPin" title="Address" sub="Printed on every invoice and receipt issued from this branch.">
        <div className={k.grid2}>
          {ADDRESS_FIELDS.map(([f, label]) => (
            <Field key={f} label={label} value={b.address[f]} onValue={(v) => set({ address: { ...b.address, [f]: v } })} />
          ))}
        </div>
      </SectionCard>

      <SectionCard icon="sliders" tone="brand" title="Business rules" sub="The ceiling for everything below this branch.">
        {(
          [
            ["Allowed transaction types", TX_TYPES, "tt"],
            ["Allowed document types", DOC_TYPES, "dt"],
            ["Allowed nature of supplies", NATURES, "ns"],
            ["Allowed operations", OPERATIONS, "ops"],
          ] as const
        ).map(([title, options, field]) => (
          <div key={field} className={k.rule}>
            <ChipMultiSelect title={title} options={options} value={b[field]} onChange={(v) => set({ [field]: v })} caption={RULE_CAPTION} />
          </div>
        ))}
      </SectionCard>

      <div className={k.twoCards}>
        <SectionCard icon="withholding" title="Tax eligibility" sub="Read-only">
          <div className={k.body} style={{ gap: 10 }}>
            {elig.length ? (
              <ChipMultiSelect
                options={elig.filter((t) => t !== EXCISE)}
                value={elig}
                derived={elig.includes(EXCISE) ? [{ value: EXCISE, tip: "Derived from your merchant excise status" }] : undefined}
                ariaLabel="Tax eligibility"
              />
            ) : (
              <ChipMultiSelect options={["None yet"]} value={[]} ariaLabel="Tax eligibility" />
            )}
            <span className={k.caption}>Derived from this branch&apos;s source systems and the merchant&apos;s excise status — not set here.</span>
          </div>
        </SectionCard>
        <SectionCard icon="shieldPlain" title="Access ceiling" sub="Most a user here can be granted">
          <div className={k.body}>
            <ChipMultiSelect
              options={PERMISSIONS}
              value={b.perms}
              onChange={(v) => set({ perms: v })}
              caption="Per-user grants on this branch can't go beyond this list."
              ariaLabel="Permission ceiling"
            />
          </div>
        </SectionCard>
      </div>

      <SectionCard
        icon="link"
        title="Linked resources"
        sub="Source systems on this branch and the API keys bound to each."
        action={
          <button type="button" className={k.link} onClick={addSystem}>
            + Add
          </button>
        }
      >
        {systems.length === 0 && <div className={k.empty}>No source systems yet.</div>}
        {systems.map((s) => {
          const keys = org.keys.filter((key) => key.systemId === s.id);
          const ex = open === s.id;
          return (
            <div key={s.id} className={st.linkedItem}>
              <div className={st.linkedRow}>
                <button
                  type="button"
                  className={st.expand}
                  aria-expanded={ex}
                  aria-label={`${ex ? "Hide" : "Show"} API keys for ${s.label}`}
                  onClick={() => setOpen(ex ? null : s.id)}
                >
                  <Icon name="chevronRight" size={14} strokeWidth={2.2} />
                </button>
                <span className={k.stack}>
                  <b style={{ fontSize: 14, fontWeight: 600 }}>{s.label}</b>
                  <span className={k.subMono}>
                    {s.type} · {plural(keys.length, "API key")}
                  </span>
                </span>
                <StatusPill status={s.ver} />
                <StatusPill status={s.active ? "Active" : "Inactive"} />
                <Link href={`/settings/source-systems/${s.id}`} className={k.link}>
                  Open
                </Link>
              </div>
              {ex && (
                <div className={st.linkedKeys}>
                  {keys.length === 0 && <span className={k.note}>No API keys bound.</span>}
                  {keys.map((key) => (
                    <Link key={key.id} href={`/settings/api-keys/${key.id}`} className={st.linkedKey}>
                      <Icon name="key" size={14} stroke="var(--brand)" />
                      <b>{key.label}</b>
                      <span className={k.subMono}>{maskedKey(key.tail)}</span>
                      <StatusPill status={key.active ? "Active" : "Deactivated"} size="sm" />
                    </Link>
                  ))}
                </div>
              )}
            </div>
          );
        })}
      </SectionCard>

      <div className={k.footer}>
        <button type="button" className={k.primary} onClick={save}>
          Save branch
        </button>
        <Link href="/settings/branches" className={k.ghost}>
          Cancel
        </Link>
        <span className={k.footerSpacer} />
        <span title={deleteTip || undefined}>
          <button type="button" className={k.dangerLink} disabled={!!deleteTip} onClick={remove}>
            Delete branch
          </button>
        </span>
      </div>
    </main>
  );
}

/** Detail page for an id that doesn't exist (or was deleted). */
export function Missing({ what, href, back }: { what: string; href: string; back: string }) {
  return (
    <main className={k.page}>
      <Link href={href} className={ui.back}>
        <Icon name="arrowLeft" size={15} />
        {back}
      </Link>
      <div className={ui.empty}>This {what} could not be found.</div>
    </main>
  );
}
