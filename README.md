# EIRSPay — merchant web app

Next.js (App Router, TypeScript) frontend for EIRSPay: landing page plus the
merchant sign-in, sign-up and verification flow from the round-1 redesign.

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
| `/dashboard`      | Placeholder until the dashboard is designed      |

## Where things live

- `app/globals.css` — brand tokens (teal) and shared animations. Green and
  blue alternates are there too; set `data-palette="green"` on `<html>` to try one.
- `components/auth/AuthShell.tsx` — the full-screen split layout and the
  left panel (receipt on sign in, step progress everywhere else).
- `components/auth/AuthFlow.tsx` — flow state shared between steps and
  `runStep()`, which shows the see-through brand loader between screens.
- `lib/auth.ts` — **mock** auth API (`signIn`, `signUp`, `sendCode`,
  `verifyCode`). Replace these bodies with real backend calls; the screens
  only depend on the function signatures.

## Placeholders

Receipt contents (Abebe Coffee House, IRN, totals) and the landing-page
stats are sample content from the design. Any 6-digit code verifies while
`lib/auth.ts` is mocked. The language switch is display-only.
