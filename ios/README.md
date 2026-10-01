# EIRSPay — iOS

Native iOS app for EIRSPay (Swift + SwiftUI), built from the Claude Design handoff
(`EIRSPay Mobile` + `EIRSPay POS`). It mirrors the Android app in `../android` screen for screen.

## Open & run
Requires a Mac with **Xcode 15.4+** (iOS 17 SDK).

```bash
brew install xcodegen
cd ios
xcodegen generate        # creates EIRSPay.xcodeproj from project.yml
open EIRSPay.xcodeproj
```
Pick an iPhone or iPad simulator and press Run. (Set your team under *Signing & Capabilities* to run on a device.)

Everything runs offline against in-memory sample data with a simulated 1.8 s MoR round-trip, so the brand
loader shows exactly like the prototype.

**Demo flows:** any email/password signs in; the owner account starts with a temporary password, so you
land on *Change password* first (as in the design). Any 6-digit code verifies. In Debug builds
"Open reset link (prototype)" opens the reset screen; real links look like `eirspay://reset?token=…`.

## Architecture

```
EIRSPayKit (Swift package, Foundation only — builds & tests without Xcode, even on Linux)
├─ EIRSDomain   models, repository protocols, use cases      (no UI, no frameworks)
└─ EIRSData     repository implementations — today in-memory fakes + sample data
EIRSPay (app target)
├─ App/            AppContainer (composition root), Router, RootView (navigation graph), @main
├─ DesignSystem/   theme tokens, fonts, icons, components, inputs, motion, loader, sheets
└─ Features/       Splash · Onboarding · Auth · Main · Home · More · Invoices · Receipts · Items · Withholding · Terminal
```

- Dependency rule: `App/Features → EIRSDomain ← EIRSData`. Features depend on use cases / repository protocols only.
- Each screen is a `@MainActor` SwiftUI view that owns an `@Observable` model; navigation is callbacks only —
  `App/RootView.swift` is the single map from callbacks to destinations.
- Money is `Decimal`; tax totals come from `EIRSDomain/Models/TaxCalculator.swift`.
- Terminal mode is a full-screen cover (no swipe-back); only press-and-hold Exit leaves it. Phone and iPad layouts.

### Hooking up the backend later
1. Add API-backed implementations in `EIRSData` (URLSession + Codable) that conform to the protocols in
   `EIRSDomain/Repositories/Repositories.swift`.
2. Swap them in `AppContainer.live()`. The UI doesn't change. Remove `FakeLatency` once real calls exist.

Notes for the backend: `AnalyticsRepository` generates the dashboard series today (no aggregate endpoint yet);
`PreferencesRepository` is in-memory (back it with `UserDefaults`/Keychain); document QR codes are real and encode
`https://portal.mor.gov.et/verify?irn=…` — pass MoR's `signed_qr` payload once available.

## Tests
`swift test --package-path EIRSPayKit` — totals, cancellation, withholding netting, password policy, amount in
words, masking, analytics. CI (`.github/workflows/ios.yml`) runs them and builds the app on every change under `ios/`.

## Fonts
Geist, Geist Mono and Instrument Serif are bundled in `EIRSPay/Resources/Fonts` (SIL OFL — see `licenses/`).
