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
| `/forgot-password`      | Request a reset link by email                |
| `/forgot-password/sent` | "Check your inbox", resend / use another email |
| `/reset-password?token=…` | New password + confirm, from the emailed link |
| `/reset-password/done`  | Password updated → sign in                   |
| `/change-password`      | Forced change after signing in with a temporary password |
| `/change-password/done` | All set → dashboard                          |
| `/dashboard`      | Placeholder until the dashboard is designed      |

## Where things live

- `app/globals.css` — brand tokens (teal) and shared animations. Green and
  blue alternates are there too; set `data-palette="green"` on `<html>` to try one.
- `components/auth/AuthShell.tsx` — the full-screen split layout and the
  left panel (receipt on sign in and the password screens, step progress
  during onboarding).
- `components/auth/PasswordField.tsx` — password input with show/hide, and the
  4-bar strength meter + rule checklist used by reset and change password.
- `components/auth/AuthFlow.tsx` — flow state shared between steps and
  `runStep()`, which shows the see-through brand loader between screens.
- `lib/auth.ts` — **mock** auth API (`signIn`, `signUp`, `sendCode`,
  `verifyCode`, `requestPasswordReset`, `verifyResetToken`, `resetPassword`,
  `changePassword`, `signOut`) plus the password rules. Replace these bodies with real backend calls; the screens
  only depend on the function signatures.

## Placeholders

Receipt contents (Abebe Coffee House, IRN, totals) and the landing-page
stats are sample content from the design. Any 6-digit code verifies while
`lib/auth.ts` is mocked. While mocked, signing in with a password that contains
"temp" (e.g. `Temp-1234`) acts like a temporary password and opens the forced
change screen; `signIn` returns `mustChangePassword` for the real backend to
set. The "Open reset link (prototype)" button on the inbox screen only shows in
development, standing in for the emailed link. The language switch is display-only.
