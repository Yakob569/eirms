"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { EXCISE, OPERATIONS, SYSTEM_TAX, TX_TYPES, createApiKey, initials, maskedKey, plural, systemTax, today, type ApiKey } from "@/lib/org";
import { useConsole } from "../ConsoleProvider";
import { Icon } from "../Icon";
import { useCopy } from "../invoices/shared";
import ui from "../ui.module.css";
import { Missing } from "./Branches";
import { ChipMultiSelect } from "./ChipMultiSelect";
import { Crumbs, Field, SectionCard, SettingsHead } from "./kit";
import k from "./kit.module.css";
import st from "./settings.module.css";
import { StatusPill } from "./StatusPill";

const COLS = { gridTemplateColumns: "minmax(0,1.5fr) minmax(0,1.2fr) minmax(0,1.3fr) 100px 100px 16px" };

/** Shown while setup isn't verified: keys can't be issued yet. */
function LockedNotice() {
  return (
    <div className={st.locked}>
      <span className={k.headIcon} style={{ background: "#fff", color: "#7a4e00" }}>
        <Icon name="lock" size={17} strokeWidth={1.8} />
      </span>
      <span>
        <b>API keys are locked until setup is verified</b>
        <span>Finish MoR credentials, seller profile and a source system, then run verification.</span>
      </span>
      <Link href="/settings" className={k.link} style={{ color: "#7a4e00" }}>
        Go to setup →
      </Link>
    </div>
  );
}

export function ApiKeyList() {
  const { org, setupDone } = useConsole();
  const locked = setupDone < 5;
  const system = (id: string) => org.systems.find((s) => s.id === id);
  const branchName = (id?: string) => org.branches.find((b) => b.id === id)?.name ?? "—";

  return (
    <main className={k.page}>
      <SettingsHead
        eyebrow={`ACCESS · ${plural(org.keys.filter((x) => x.active).length, "active key")}`}
        title="API"
        accent="keys."
        lede="Each key is bound to one source system and can only do what that system's branch allows."
      >
        {locked ? (
          <button type="button" className={`${k.primary} ${k.primarySm}`} disabled title="Verify setup first">
            <Icon name="plus" size={15} strokeWidth={2.2} />
            Create API key
          </button>
        ) : (
          <Link href="/settings/api-keys/new" className={`${k.primary} ${k.primarySm}`}>
            <Icon name="plus" size={15} strokeWidth={2.2} />
            Create API key
          </Link>
        )}
      </SettingsHead>

      {locked && <LockedNotice />}

      <div className={k.table}>
        <div className={k.tableHead} style={COLS}>
          <span>KEY</span>
          <span>SOURCE SYSTEM</span>
          <span>CAPABILITIES</span>
          <span>CREATED</span>
          <span>STATUS</span>
          <span />
        </div>
        {org.keys.map((key) => {
          const s = system(key.systemId);
          return (
            <Link key={key.id} href={`/settings/api-keys/${key.id}`} className={k.tableRow} style={COLS} data-dim={!key.active}>
              <span className={k.nameCell}>
                <span className={k.headIcon}>
                  <Icon name="key" size={17} strokeWidth={1.8} />
                </span>
                <span className={k.nameText}>
                  <b>{key.label}</b>
                  <span className={k.subMono}>{maskedKey(key.tail)}</span>
                </span>
              </span>
              <span className={k.stack}>
                <span className={k.cellText}>{s?.label ?? "—"}</span>
                <small>{branchName(s?.branchId)} branch</small>
              </span>
              <span className={k.cellMuted} style={{ fontSize: 13 }}>
                {plural(key.ops.length, "operation")} · {plural(key.tt.length, "transaction type")}
              </span>
              <span className={k.subMono}>{key.created}</span>
              <StatusPill status={key.active ? "Active" : "Deactivated"} />
              <Icon name="chevronRight" size={15} stroke="var(--muted)" />
            </Link>
          );
        })}
      </div>
    </main>
  );
}

export function NewApiKey() {
  const { org, ready } = useConsole();
  if (!ready) return null;
  const first = org.systems.find((s) => s.active) ?? org.systems[0];
  const draft: ApiKey = {
    id: "new",
    label: "",
    systemId: first?.id ?? "",
    active: true,
    created: "",
    ops: ["Single Register", "Receipt Issue"],
    tt: ["B2C"],
    tax: ["VAT15"],
    tail: "",
    users: [],
  };
  return <KeyForm initial={draft} isNew />;
}

export function ApiKeyDetail({ id }: { id: string }) {
  const { org, ready } = useConsole();
  const key = org.keys.find((x) => x.id === id);
  if (!key) return ready ? <Missing what="API key" href="/settings/api-keys" back="API keys" /> : null;
  return <KeyForm key={key.id} initial={key} />;
}

function KeyForm({ initial, isNew }: { initial: ApiKey; isNew?: boolean }) {
  const router = useRouter();
  const { org, updateOrg, reveal, setReveal, setupDone, work } = useConsole();
  const [key, setKey] = useState(initial);
  const [assignOpen, setAssignOpen] = useState(false);
  const [copied, copy] = useCopy();
  // The full secret is only on screen right after creation; leaving the page drops it.
  const [secret] = useState(() => (reveal && reveal.id === initial.id ? reveal.secret : ""));
  useEffect(() => {
    if (secret) setReveal(null);
  }, [secret, setReveal]);

  const set = (patch: Partial<ApiKey>) => setKey((x) => ({ ...x, ...patch }));
  const system = org.systems.find((s) => s.id === key.systemId);
  const branch = org.branches.find((b) => b.id === system?.branchId);
  const allowedTax = system ? systemTax(org, system) : [];
  const revoked = !isNew && !key.active;
  const editable = !revoked;
  const persisted = org.keys.find((x) => x.id === initial.id);
  const canCreate = setupDone >= 5 && !!system;

  /** Drop anything the bound system or its branch no longer allows. */
  const clamp = (x: ApiKey): ApiKey => ({
    ...x,
    label: x.label.trim() || "Untitled key",
    ops: x.ops.filter((v) => branch?.ops.includes(v)),
    tt: x.tt.filter((v) => branch?.tt.includes(v)),
    tax: x.tax.filter((v) => allowedTax.includes(v)),
  });

  async function create() {
    if (!canCreate) return;
    const { id, secret } = await work("Issuing API key", `Binding it to ${system?.label}`, createApiKey);
    const created: ApiKey = { ...clamp(key), id, created: today(), tail: secret.slice(-4) };
    updateOrg((o) => ({ ...o, keys: [created, ...o.keys] }));
    setReveal({ id, secret });
    router.push(`/settings/api-keys/${id}`);
  }

  function save() {
    updateOrg((o) => ({ ...o, keys: o.keys.map((x) => (x.id === key.id ? clamp(key) : x)) }));
    router.push("/settings/api-keys");
  }

  function revoke() {
    set({ active: false });
    updateOrg((o) => ({ ...o, keys: o.keys.map((x) => (x.id === key.id ? { ...x, active: false } : x)) }));
  }

  return (
    <main className={k.page}>
      <div className={k.titleStack}>
        <Crumbs trail={[{ label: "API keys", href: "/settings/api-keys" }, { label: isNew ? "New key" : persisted?.label || "Untitled key" }]} />
        <h1 className={ui.title}>
          {isNew ? "New API" : key.label || "Untitled"} <i>key.</i>
        </h1>
        <div className={k.subline}>
          {system ? (
            <span>
              Bound to <Link href={`/settings/source-systems/${system.id}`}>{system.label}</Link> ·{" "}
              {branch ? <Link href={`/settings/branches/${branch.id}`}>{branch.name}</Link> : "—"} branch
            </span>
          ) : (
            "No source system yet"
          )}
          {!isNew && <StatusPill status={key.active ? "Active" : "Deactivated"} size="sm" />}
        </div>
      </div>

      {isNew && setupDone < 5 && <LockedNotice />}

      {secret && (
        <div className={st.reveal} role="alert">
          <div className={st.revealWarn}>
            <Icon name="alert" size={18} stroke="#F5B94A" />
            This is shown once — store it now.
          </div>
          <div className={st.revealRow}>
            <div className={st.revealKey}>{secret}</div>
            <button type="button" className={st.revealCopy} onClick={() => copy("secret", secret)}>
              <Icon name="copy" size={15} />
              {copied === "secret" ? "Copied" : "Copy"}
            </button>
          </div>
          <span className={st.revealNote}>After you leave this page only the last four characters are kept.</span>
        </div>
      )}

      <SectionCard icon="key" title="Identity" sub="A name you'll recognise and the source system it's bound to.">
        <div className={k.body}>
          <Field
            label="Label"
            wrapClassName={k.narrow}
            placeholder="e.g. Front Counter · Till 2"
            value={key.label}
            onValue={editable ? (v) => set({ label: v }) : undefined}
          />
          <div className={k.field}>
            <span className={k.fieldLabel}>Source system</span>
            <ChipMultiSelect
              single
              options={org.systems.map((s) => ({ value: s.id, label: `${s.label} · ${org.branches.find((b) => b.id === s.branchId)?.name ?? "—"}` }))}
              value={[key.systemId]}
              onChange={isNew ? (v) => set({ systemId: v[0] }) : undefined}
              locked={
                isNew
                  ? Object.fromEntries(org.systems.filter((s) => !s.active).map((s) => [s.id, "Source system is inactive"]))
                  : undefined
              }
              ariaLabel="Source system"
            />
            {!isNew && <span className={k.caption}>A key stays bound to the source system it was created for.</span>}
          </div>
        </div>
      </SectionCard>

      <SectionCard icon="sliders" title="Capabilities" sub="Narrower than or equal to the bound source system and its branch." flush>
        <div className={k.rule}>
          <ChipMultiSelect
            title="Operations"
            options={OPERATIONS}
            value={key.ops}
            onChange={editable ? (v) => set({ ops: v }) : undefined}
            allowed={branch?.ops ?? []}
            disallowedTip={() => `Not allowed on ${branch?.name ?? "this"} branch`}
            caption={`Greyed chips are outside what ${branch?.name ?? "the"} branch and ${system?.label ?? "the source system"} allow.`}
          />
        </div>
        <div className={k.rule}>
          <ChipMultiSelect
            title="Transaction types"
            options={TX_TYPES}
            value={key.tt}
            onChange={editable ? (v) => set({ tt: v }) : undefined}
            allowed={branch?.tt ?? []}
            disallowedTip={() => `Not allowed on ${branch?.name ?? "this"} branch`}
          />
        </div>
        <div className={k.rule}>
          <ChipMultiSelect
            title="Tax types"
            options={[...SYSTEM_TAX, EXCISE]}
            value={key.tax}
            onChange={editable ? (v) => set({ tax: v }) : undefined}
            allowed={allowedTax}
            disallowedTip={(v) => (v === EXCISE && !org.excise ? "Enable excise on the merchant account first" : `Not enabled on ${system?.label ?? "the source system"}`)}
          />
        </div>
      </SectionCard>

      {!isNew && !secret && (
        <SectionCard
          icon="lock"
          title="Key"
          sub={<span className={k.mono} style={{ fontSize: 14, color: "var(--ink)" }}>{maskedKey(key.tail)}</span>}
          action={<span className={k.note}>Full key was shown once at creation</span>}
        />
      )}

      <SectionCard
        icon="team"
        title="Assigned users"
        sub="Staff who can sign requests with this key."
        action={
          editable ? (
            <button type="button" className={k.link} onClick={() => setAssignOpen(!assignOpen)}>
              {assignOpen ? "Done" : "+ Assign"}
            </button>
          ) : undefined
        }
      >
        <div className={k.body} style={{ gap: 14, paddingTop: 18, paddingBottom: 18 }}>
          {key.users.length === 0 && <span className={k.note} style={{ fontSize: 14 }}>No one assigned yet.</span>}
          {key.users.length > 0 && (
            <div className={st.users}>
              {key.users.map((u) => (
                <span key={u} className={st.user}>
                  <span className={st.userMark}>{initials(u)}</span>
                  {u}
                  {editable && (
                    <button type="button" className={st.userRemove} aria-label={`Remove ${u}`} onClick={() => set({ users: key.users.filter((x) => x !== u) })}>
                      <Icon name="x" size={11} strokeWidth={2.6} />
                    </button>
                  )}
                </span>
              ))}
            </div>
          )}
          {assignOpen && editable && (
            <div className={st.assign}>
              <span className={k.fieldLabel}>Add staff</span>
              {org.staff.some((u) => !key.users.includes(u)) ? (
                <ChipMultiSelect
                  options={org.staff.filter((u) => !key.users.includes(u))}
                  value={[]}
                  onChange={(v) => set({ users: [...key.users, ...v] })}
                  ariaLabel="Add staff"
                />
              ) : (
                <span className={k.note}>Everyone on staff is assigned.</span>
              )}
            </div>
          )}
        </div>
      </SectionCard>

      <div className={k.footer}>
        {isNew ? (
          <button type="button" className={k.primary} disabled={!canCreate} onClick={create}>
            Create key
          </button>
        ) : (
          editable && (
            <button type="button" className={k.primary} onClick={save}>
              Save key
            </button>
          )
        )}
        <Link href="/settings/api-keys" className={k.ghost}>
          Back to keys
        </Link>
        <span className={k.footerSpacer} />
        {!isNew && key.active && (
          <button type="button" className={k.dangerLink} onClick={revoke}>
            Revoke key
          </button>
        )}
        {revoked && <span className={k.note}>Revoked — kept as deactivated for audit.</span>}
      </div>
    </main>
  );
}
