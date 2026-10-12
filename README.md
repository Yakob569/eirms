# EIRSPay — merchant web app

Next.js (App Router, TypeScript) frontend for EIRSPay: landing page, the
merchant sign-in, sign-up and verification flow, and the merchant console
(dashboard, invoices, receipts, items, withholding, Settings) that sign-in
lands on.

## Run it

```bash
npm install
npm run dev        # http://localhost:3000
npm run build      # production build
```

## Routes

| Route             | Screen                                           |
| ----------------- | ------------------------------------------------ |
| `/`               | Landing page                                     |
| `/signin`         | Sign in (left panel: receipt + QR)               |
| `/signup`         | Create account — step 1 of 3                     |
| `/verify`         | Choose email or SMS — step 2 of 3                |
| `/verify/code`    | Enter the 6-digit code, with expiry countdown    |
| `/verify/success` | Verified — step 3 of 3 is next                   |
| `/dashboard`      | Sales & tax overview: KPIs, chart, breakdowns, recent activity |
| `/invoices`       | Registered invoices, filterable by branch, status, type, text. Row ⋮ menu: view, credit / debit note (side panel), cancel; on retainers (RTN) interim / final invoice |
| `/invoices/new`   | Register invoice / credit note / debit note with live preview |
| `/invoices/[id]`  | Invoice detail: proof of registration (IRN + QR), parties, lines, cancel (`?cancel=1` opens the cancel dialog) |
| `/invoices/[id]/receipt` | Printable fiscal receipt, A4 or 80mm     |
| `/canceled-invoices` | Canceled invoices, cancel reason first        |
| `/receipts`       | Sales receipts, filterable by branch, type, payment method, text. Row ⋮ menu: view, view linked invoice |
| `/receipts/new`   | Register receipt: sales / advance from the catalog, payment / refund against an invoice |
| `/receipts/[id]`  | Receipt detail: payment details, linked invoice and withholding, lines, IRN + QR |
| `/items`          | Items catalog with active switch; create / edit in a side drawer |
| `/withholding`    | Withholding records with certificate status  |
| `/withholding/[id]` | Calculation, related invoice and payment receipt, mark certificate received |
| `/settings`       | Settings › Setup — fiscal-readiness checklist, each step links to its page; Run verification |
| `/settings/account` | Identity, change password, active sessions, security activity |
| `/settings/mor-credentials` | EIRMS client ID / TIN / secrets / environment, read-only until Edit |
| `/settings/seller-profile` | Legal identity, contact and address, read-only until Edit |
| `/settings/verify` | Probe invoice preview and Run verification (~1.6s, simulated) |
| `/settings/source-systems` | Merchant excise switch and source systems (branch, tax types, verification) |
| `/settings/source-systems/[id]` | Source system: basic info, defaults, tax types, verification, sequence info |
| `/settings/branches` | Branches with manager, location, linked systems, status |
| `/settings/branches/[id]` | Branch: info, address, business rules, tax eligibility, access ceiling, linked systems and keys |
| `/settings/api-keys` | API keys bound to source systems (locked until setup is verified) |
| `/settings/api-keys/new` | Create a key; the full key is shown once on its page afterwards |
| `/settings/api-keys/[id]` | Key: identity, capabilities within its system / branch, assigned users, revoke |

## Where things live

- `app/globals.css` — brand tokens (teal) and shared animations. Green and
  blue alternates are there too; set `data-palette="green"` on `<html>` to try one.
- `components/auth/AuthShell.tsx` — the full-screen split layout and the
  left panel (receipt on sign in, step progress everywhere else).
- `components/auth/AuthFlow.tsx` — flow state shared between steps and
  `runStep()`, which shows the see-through brand loader between screens.
- `components/console/` — the console shell (sidebar, top bar with branch
  picker and user menu, see-through loader) and its pages. Shared console
  state (branch, time range, session-registered and canceled documents,
  settings data and setup verification) lives in `ConsoleProvider.tsx`.
- `components/console/settings/` — Settings pages. They share one
  `ChipMultiSelect` (business rules, tax types, key capabilities and the
  single-pick pickers) and one `StatusPill`; `kit.tsx` has the section card,
  field, switch and breadcrumb. `SettingsLayout.tsx` is the sub-navigation.
- `components/console/RowMenu.tsx` — the ⋮ row menu on invoice and receipt
  lists. `invoices/DocumentPanels.tsx` has the credit / debit note and
  interim / final invoice side panels; `invoices/LineItems.tsx` is the item
  builder shared with Register invoice.
- `lib/merchant.ts` — **mock** merchant API (`listInvoices`,
  `registerInvoice`, `cancelInvoice`, `lookupTin`, `readiness`, `listReceipts`,
  `registerReceipt`, `listItems`, `saveItem`, `listWithholding`,
  `markCertificateReceived`) plus document math (totals, tax breakdown,
  receipt amounts net of withholding, IRN/QR, amount in words).
- `lib/org.ts` — **mock** settings data and calls: branches → source systems
  → API keys (a system belongs to one branch, a key is bound to one system
  and can't exceed it), sessions, MoR credentials, seller profile,
  `createApiKey`, `verifySourceSystem`, `runSetupVerification`.
- `lib/dashboard.ts` — **mock** `dashboardStats(range, branch)`. Replace with
  aggregation over invoices / receipts / cancellations, or an aggregate
  endpoint once volume grows.
- `lib/auth.ts` — **mock** auth API (`signIn`, `signUp`, `sendCode`,
  `verifyCode`). Replace these bodies with real backend calls; the screens
  only depend on the function signatures.

## Placeholders

Receipt contents (Abebe Coffee House, IRN, totals) and the landing-page
stats are sample content from the design. Any 6-digit code verifies while
`lib/auth.ts` is mocked. The language switch is display-only.

Console figures, invoices, buyers, TINs and reason codes are sample data.
Documents you register or cancel, catalog edits and certificates you mark
received are kept in session storage for the tab. Receipts, withholding
records, the 2% rate and HS codes are sample data too.
Set `readiness()` in `lib/merchant.ts` below 5 to see the setup notice; with
3 or more steps done, Run verification on Settings › Setup completes setup and
unlocks API keys. Settings edits (branches, systems, keys, excise, credentials,
seller profile) are kept in session storage like documents. RTN-095 is a sample
retainer for trying interim / final invoices.
The branch terminal is a separate product and isn't part of this console.
Not built yet: Settings › Departments, Team and Roles, Export, Download PDF and a printable
receipt layout (Print receipt uses the browser's print). The Amharic receipt labels need a
native-speaker check.
