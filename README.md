# EIRSPay — merchant web app

Next.js (App Router, TypeScript) frontend for EIRSPay: landing page, the
merchant sign-in, sign-up and verification flow, and the merchant console
(dashboard, invoices, Settings › Setup) that sign-in lands on.

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
| `/invoices`       | Registered invoices, filterable by branch, status, type, text |
| `/invoices/new`   | Register invoice / credit note / debit note with live preview |
| `/invoices/[id]`  | Invoice detail: proof of registration (IRN + QR), parties, lines, cancel |
| `/invoices/[id]/receipt` | Printable fiscal receipt, A4 or 80mm     |
| `/canceled-invoices` | Canceled invoices, cancel reason first        |
| `/settings`       | Settings › Setup — fiscal-readiness checklist    |

## Where things live

- `app/globals.css` — brand tokens (teal) and shared animations. Green and
  blue alternates are there too; set `data-palette="green"` on `<html>` to try one.
- `components/auth/AuthShell.tsx` — the full-screen split layout and the
  left panel (receipt on sign in, step progress everywhere else).
- `components/auth/AuthFlow.tsx` — flow state shared between steps and
  `runStep()`, which shows the see-through brand loader between screens.
- `components/console/` — the console shell (sidebar, top bar with branch
  picker and user menu, see-through loader) and its pages. Shared console
  state (branch, time range, session-registered and canceled documents)
  lives in `ConsoleProvider.tsx`.
- `lib/merchant.ts` — **mock** merchant API (`listInvoices`,
  `registerInvoice`, `cancelInvoice`, `lookupTin`, `readiness`) plus invoice
  math (totals, tax breakdown, IRN/QR, amount in words).
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
Documents you register or cancel are kept in session storage for the tab.
Set `readiness()` in `lib/merchant.ts` below 5 to see the setup notice.
Not built yet: Terminal, Receipts, Items, the other Settings pages, Export,
Download PDF and Register receipt. The Amharic receipt labels need a
native-speaker check.
