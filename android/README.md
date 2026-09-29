# EIRSPay — Android

Native Android app for EIRSPay (Kotlin + Jetpack Compose), built from the Claude Design handoff in
`../project/EIRSPay Mobile.dc.html` (with `EIRSPay POS.dc.html` for terminal mode).

## Open & run
1. Open this `android/` folder in Android Studio (Ladybug or newer). JDK 17.
2. Let Gradle sync (AGP 8.11, Kotlin 2.2, Compose BOM 2025.08). Android Studio may offer newer versions — safe to accept.
3. Run the `app` configuration on a device/emulator (minSdk 26).

Everything runs offline against in-memory sample data, with a simulated 1.8 s MoR round-trip so the
brand loader shows exactly like the prototype.

**Demo flows:** any email/password signs in (the owner account starts with a temporary password, so
you land on *Change password* first, as in the design). Any 6-digit code verifies. In debug builds
"Open reset link (prototype)" opens the reset screen; real links are `eirspay://reset?token=…`.

## Architecture (clean architecture, 3 modules)

```
:app     Compose UI + ViewModels (feature/*), design system (ui/designsystem, ui/theme),
         navigation (navigation/), Hilt wiring (di/)
   │ depends on
:domain  Pure Kotlin — models, repository interfaces, use cases. No Android, no frameworks.
   ▲ implemented by
:data    Repository implementations. Today: in-memory fakes seeded with the design's sample data (fake/, seed/).
```

- Dependency rule: `app → domain ← data`. Feature code only talks to use cases / repository interfaces.
- Screens are callback-driven (no `NavController` inside features); `navigation/AppNavHost.kt` is the single map of flows.
- Money is `BigDecimal`; tax totals are computed in `domain/model/TaxCalculator.kt`.

### Hooking up the backend later
1. Add API-backed implementations in `:data` (e.g. `data/remote/…` with Retrofit/Ktor + kotlinx-serialization) that implement the interfaces in `domain/repository/Repositories.kt`.
2. Switch the bindings in `app/src/main/java/com/eirspay/mobile/di/DataModule.kt`.
3. Nothing in the UI changes. Delete `FakeLatency` usage once real calls exist.

Notes for the backend:
- `AnalyticsRepository` generates the dashboard series today (no aggregate endpoint exists yet). Computing it on-device from
  `listInvoices()/listReceipts()` is fine at current volumes; an aggregate endpoint is worth adding as volume grows.
- `PreferencesRepository` is in-memory; back it with DataStore to remember the onboarding flag and branch filter.
- Document QR codes are real and encode `https://portal.mor.gov.et/verify?irn=…`; pass MoR's `signed_qr` payload once available.

## Features
Splash · onboarding · sign in / sign up / verify (SMS or email) / forgot, reset and forced change password ·
dashboard (range + branch filters, chart scrubbing, tax/transaction-type/branch breakdowns, recent activity) ·
invoices (list, detail with proof-of-registration QR, thermal receipt view, cancel with reason, register form) ·
receipts (list, detail, register) · items catalog (list, create/edit) · withholding (list, detail) ·
More / fiscal readiness · terminal mode (full-screen, hold-to-exit, phone + tablet layouts).

## Tests
`./gradlew :data:test` — totals, cancellation, withholding netting, password policy, amount in words, analytics.

## Fonts
Geist, Geist Mono and Instrument Serif are bundled in `app/src/main/res/font` (SIL OFL — see `licenses/`).
