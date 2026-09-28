"use client";

import { useState } from "react";
import { TAX_CODES, TAX_CODE_NAMES, UNITS, etb, fmt2, grossPrice, saveItem, type CatalogItem, type Nature } from "@/lib/merchant";
import { useConsole } from "../ConsoleProvider";
import { Icon } from "../Icon";
import iv from "../invoices/invoices.module.css";
import rg from "../invoices/register.module.css";
import ui from "../ui.module.css";
import it from "./items.module.css";

type Form = Omit<CatalogItem, "unitPrice"> & { unitPrice: string; mode: "new" | "edit" };

const NEW_ITEM: Form = {
  mode: "new",
  code: "",
  description: "",
  nature: "Goods",
  hsCode: "",
  taxCode: "VAT15",
  unit: "PCS",
  unitPrice: "",
  active: true,
  category: "",
};

export function ItemCatalog() {
  const { catalog, putItem } = useConsole();
  const [q, setQ] = useState("");
  const [nature, setNature] = useState<"all" | Nature>("all");
  const [status, setStatus] = useState<"all" | "active" | "inactive">("all");
  const [form, setForm] = useState<Form | null>(null);

  const needle = q.trim().toLowerCase();
  const rows = catalog.filter(
    (c) =>
      (nature === "all" || c.nature === nature) &&
      (status === "all" || (status === "active") === c.active) &&
      (!needle || `${c.code} ${c.description}`.toLowerCase().includes(needle)),
  );

  return (
    <main className={ui.page} style={{ gap: 20 }}>
      <div className={ui.pageHead} style={{ flexWrap: "nowrap" }}>
        <div className={ui.titleBlock}>
          <span className={ui.eyebrow}>
            CATALOG · {catalog.length} ITEMS · {catalog.filter((c) => c.active).length} ACTIVE
          </span>
          <h1 className={ui.title}>
            Items <i>catalog.</i>
          </h1>
          <p className={ui.sub} style={{ maxWidth: 640 }}>
            Products and services your team picks from when registering invoices and receipts. Each carries its tax code, so lines are priced
            correctly every time.
          </p>
        </div>
        <button type="button" className={ui.primary} onClick={() => setForm(NEW_ITEM)}>
          <Icon name="plus" size={16} strokeWidth={2.2} />
          New item
        </button>
      </div>

      <div className={iv.filters}>
        <label className={iv.search} style={{ flexBasis: 320 }}>
          <Icon name="search" size={16} stroke="var(--muted)" />
          <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Code or description" aria-label="Filter items" />
        </label>
        <div className={ui.seg} role="group" aria-label="Nature of supply">
          {(
            [
              ["all", "All"],
              ["Goods", "Goods"],
              ["Service", "Services"],
            ] as const
          ).map(([k, label]) => (
            <button key={k} type="button" className={`${ui.segOpt} ${iv.segSm}`} aria-pressed={nature === k} onClick={() => setNature(k)}>
              {label}
            </button>
          ))}
        </div>
        <div className={ui.seg} role="group" aria-label="Status">
          {(
            [
              ["all", "Any status"],
              ["active", "Active"],
              ["inactive", "Inactive"],
            ] as const
          ).map(([k, label]) => (
            <button key={k} type="button" className={`${ui.segOpt} ${iv.segSm}`} aria-pressed={status === k} onClick={() => setStatus(k)}>
              {label}
            </button>
          ))}
        </div>
      </div>

      <div className={`${it.cols} ${iv.colLabels}`}>
        <span>CODE</span>
        <span>DESCRIPTION</span>
        <span>NATURE</span>
        <span>HS CODE</span>
        <span>TAX</span>
        <span>UNIT</span>
        <span style={{ textAlign: "right" }}>UNIT PRICE</span>
        <span>STATUS</span>
      </div>

      <div className={iv.rows}>
        {rows.map((c) => {
          const edit = () => setForm({ ...c, unitPrice: String(c.unitPrice), mode: "edit" });
          return (
            <div
              key={c.code}
              role="button"
              tabIndex={0}
              className={`${it.cols} ${iv.row} ${it.row}`}
              data-active={c.active}
              onClick={edit}
              onKeyDown={(e) => e.key === "Enter" && edit()}
            >
              <span className={it.code}>{c.code}</span>
              <span className={iv.cell}>
                <span className={`${iv.party} ${iv.ellipsis}`}>{c.description}</span>
                <span className={it.cat}>{c.category}</span>
              </span>
              <span>
                <span className={it.nature} data-nature={c.nature}>
                  {c.nature}
                </span>
              </span>
              <span className={it.hs} data-empty={!c.hsCode}>
                {c.hsCode || "—"}
              </span>
              <span>
                <span className={ui.code} style={{ fontSize: 10 }}>
                  {c.taxCode}
                </span>
              </span>
              <span className={it.unit}>{c.unit}</span>
              <span className={`${iv.cell} ${it.price}`}>
                <b>{fmt2(c.unitPrice)}</b>
                <span>incl. tax {fmt2(grossPrice(c))}</span>
              </span>
              <span className={it.status} data-active={c.active}>
                <button
                  type="button"
                  role="switch"
                  aria-checked={c.active}
                  aria-label={`${c.active ? "Deactivate" : "Activate"} ${c.description}`}
                  className={it.switch}
                  onClick={(e) => {
                    e.stopPropagation();
                    putItem({ ...c, active: !c.active });
                  }}
                  onKeyDown={(e) => e.stopPropagation()}
                />
                {c.active ? "Active" : "Inactive"}
              </span>
            </div>
          );
        })}
        {rows.length === 0 && <div className={ui.empty}>No items match these filters.</div>}
      </div>

      {form && (
        <ItemDrawer
          form={form}
          taken={(code) => catalog.some((c) => c.code.toUpperCase() === code.trim().toUpperCase())}
          onClose={() => setForm(null)}
          onSave={async (item) => {
            putItem(await saveItem(item));
            setForm(null);
          }}
        />
      )}
    </main>
  );
}

function ItemDrawer({
  form,
  taken,
  onClose,
  onSave,
}: {
  form: Form;
  taken: (code: string) => boolean;
  onClose: () => void;
  onSave: (item: CatalogItem) => void;
}) {
  const [f, setF] = useState(form);
  const set = (p: Partial<Form>) => setF((s) => ({ ...s, ...p }));
  const isNew = f.mode === "new";
  const price = parseFloat(f.unitPrice) || 0;
  const goods = f.nature === "Goods";
  const dup = isNew && !!f.code.trim() && taken(f.code);
  const missing = [!f.code.trim() && "code", !f.description.trim() && "description", goods && !f.hsCode && "HS code", !(price > 0) && "price"].filter(Boolean);
  const ok = !missing.length && !dup;

  function save() {
    if (!ok) return;
    onSave({
      code: f.code.trim(),
      description: f.description.trim(),
      nature: f.nature,
      hsCode: f.hsCode,
      taxCode: f.taxCode,
      unit: f.unit,
      unitPrice: price,
      active: f.active,
      category: f.category || (f.nature === "Service" ? "Services" : "General"),
    });
  }

  return (
    <>
      <div className={it.scrim} onClick={onClose} />
      <div className={it.drawer} role="dialog" aria-modal="true" aria-labelledby="item-drawer-title">
        <div className={it.drawerHead}>
          <div className={ui.titleBlock} style={{ gap: 6 }}>
            <span id="item-drawer-title" className={it.drawerTitle}>
              {isNew ? "New item" : "Edit item"}
            </span>
            <span className={it.drawerSub}>
              {isNew ? "Adds to the catalog your team picks from." : "Changes apply to new documents. Registered invoices keep their original lines."}
            </span>
          </div>
          <button type="button" className={it.close} onClick={onClose} aria-label="Close">
            <Icon name="x" size={16} />
          </button>
        </div>

        <div className={it.drawerBody}>
          {isNew ? (
            <label className={rg.label}>
              Item code *
              <input
                className={`${rg.input} ${rg.inputMono} ${it.upper}`}
                placeholder="ITM-0000"
                value={f.code}
                onChange={(e) => set({ code: e.target.value.toUpperCase() })}
                autoFocus
              />
            </label>
          ) : (
            <div className={it.fixed}>
              <span>Item code</span>
              <b>{f.code}</b>
            </div>
          )}
          <label className={rg.label}>
            Description *
            <input className={rg.input} placeholder="As it should print on the receipt" value={f.description} onChange={(e) => set({ description: e.target.value })} />
          </label>
          <div className={rg.field}>
            <span className={rg.fieldLabel}>Nature of supply</span>
            <div className={ui.seg} role="group" aria-label="Nature of supply" style={{ alignSelf: "flex-start" }}>
              {(["Goods", "Service"] as const).map((k) => (
                <button key={k} type="button" className={`${ui.segOpt} ${iv.segSm}`} aria-pressed={f.nature === k} onClick={() => set({ nature: k })}>
                  {k}
                </button>
              ))}
            </div>
          </div>
          <div className={rg.field} style={{ gap: 6 }}>
            <label className={rg.label}>
              Harmonization (HS) code{goods ? " *" : ""}
              <input
                className={`${rg.input} ${rg.inputMono}`}
                placeholder="e.g. 0901.11.00"
                value={f.hsCode}
                onChange={(e) => set({ hsCode: e.target.value.replace(/[^0-9.]/g, "") })}
              />
            </label>
            <span className={rg.hint}>{goods ? "Required for goods. Used in MoR and customs reporting." : "Optional for services."}</span>
          </div>
          <div className={rg.field}>
            <span className={rg.fieldLabel}>Tax code</span>
            <div className={it.taxCards}>
              {TAX_CODES.map((code) => (
                <button key={code} type="button" className={it.taxCard} aria-pressed={f.taxCode === code} onClick={() => set({ taxCode: code })}>
                  <b>{code}</b>
                  <span>{TAX_CODE_NAMES[code]}</span>
                </button>
              ))}
            </div>
          </div>
          <div className={rg.field}>
            <span className={rg.fieldLabel}>Unit</span>
            <div className={rg.opts}>
              {UNITS.map((u) => (
                <button key={u} type="button" className={it.unitChip} aria-pressed={f.unit === u} onClick={() => set({ unit: u })}>
                  {u}
                </button>
              ))}
            </div>
          </div>
          <label className={rg.label}>
            Unit price (excl. tax) *
            <input
              className={`${rg.input} ${rg.inputMono}`}
              style={{ textAlign: "right" }}
              inputMode="decimal"
              placeholder="0.00"
              value={f.unitPrice}
              onChange={(e) => set({ unitPrice: e.target.value.replace(/[^0-9.]/g, "") })}
            />
          </label>
          <div className={it.gross}>
            <span>Customer pays</span>
            <b>
              {etb(grossPrice({ unitPrice: price, taxCode: f.taxCode }))} per {f.unit}
            </b>
          </div>
          <div className={it.activeRow}>
            <button type="button" role="switch" aria-checked={f.active} aria-label="Active" className={it.switch} onClick={() => set({ active: !f.active })} />
            <span className={ui.stack2}>
              <b>Active</b>
              <span>Inactive items stay on old documents but can&apos;t be picked for new ones</span>
            </span>
          </div>
        </div>

        <div className={it.drawerFoot}>
          <span>{dup ? "That code is already in the catalog." : missing.length ? `Missing: ${missing.join(", ")}` : "Ready to save."}</span>
          <button type="button" className={ui.btn} style={{ height: 42, padding: "0 16px" }} onClick={onClose}>
            Cancel
          </button>
          <button type="button" className={it.save} disabled={!ok} onClick={save}>
            Save item
          </button>
        </div>
      </div>
    </>
  );
}
