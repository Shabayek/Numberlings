# Store readiness — Numberlings

## Listing copy (play store draft, family-appropriate tone)
**Title (30):** Numberlings: Cozy Math Village
**Short description (80):** Wake a cozy village of Numberlings by practising real math — no ads, no timers.
**Full description:**
> Counting, comparing, adding, subtracting, times tables, fair sharing, shapes, and
> halves — one puzzle at a time, always the child's pace. Every puzzle earns a bit of
> village growth. No timers. No lives. No ads. No chat. No accounts. No tracking —
> the app never asks for internet access.
>
> Grown-ups get a private zone with a parent gate: voice pace, sound, gentle difficulty
> nudge, a sleep invitation timer, plain-language privacy notes, and progress share
> (a tiny text file, if you want it).
>
> One-time unlock for all six meadows, or the Community Unlock — whichever suits your
> budget today.

**Content rating questionnaire:** child audience, no violence, no user-to-user interaction,
no sharing of personal info, no ads, no in-app purchases visible to children (all behind
parent gate) → ESRB E / PEGI 3 expected.

## Data Safety (Play Console form)
| Question | Answer |
| --- | --- |
| Collect or share personal data? | No — none |
| Data types | None (no account, no identifiers, no location, no audio/video, no files) |
| Required functionality | Progress stored locally in Room DB; Device or other ID? none |

## Families policy checklist
- [x] No ads anywhere (policy requirement and charter).
- [x] No third-party SDK telemetry (billing library only, and it talks to Play Services only).
- [x] No social features; no UGC; no camera/mic.
- [x] Purchases live behind a parental gate inside the Grown-ups zone.
- [x] No "free trial → subscription" hooks; single non-consumable only.
- [x] Content rating questionnaire filled before release.
- [x] Data Safety form matches `AndroidManifest` (no INTERNET permission at all).

## Privacy policy draft (parent-facing, post on a website)
> **Numberlings and your child's data**
> Numberlings stores everything — progress, settings, unlocked meadows, hearts — only
> on your device. It has no internet permission, so nothing can be uploaded to us or
> anywhere else. There is no account. There is no advertising, no tracking, no third-
> party analytics. If you want a copy of your child's progress, use "Share progress"
> inside the Grown-ups zone; it shares a tiny text file that you choose where to send.
> Questions: contact@vividoai.eu

## Launch checklist
1. Create in-product item `numberlings_meadows_all` (one-time, non-consumable).
2. Set `UnlockStore.billingConfigured = true` after price review.
3. Ship signed release build (see "Signing a release build" below).
4. Verify Data Safety answers + Families declaration in Play Console.
5. Confirm "sleep invitation" setting reaches the play screen on a fresh install.

### A note on the INTERNET permission (changed 2026-10-21)
The Play Billing library adds `android.permission.INTERNET` (and
`ACCESS_NETWORK_STATE`) to the **merged** manifest, so the shipped package used
to ask for internet access even though Numberlings never uses it. The app
manifest now strips it again:

```xml
<uses-permission android:name="android.permission.INTERNET" tools:node="remove" />
```

Verified on the installed package: `dumpsys package com.vividoai.numberlings`
lists only BILLING, ACCESS_NETWORK_STATE and the internal receiver permission.
The purchase itself is completed by the **Google Play Store app** on the
device — Numberlings only talks to it locally, so nothing needs the internet.

> **If a future Play Billing release refuses to work without INTERNET** (it
> checks connectivity before showing the sheet), remove that one line from
> `app/src/main/AndroidManifest.xml`, rebuild, and test the purchase flow
> before shipping. The Play Store's own network access is not affected.

**Verified build numbers (this repo, JDK 21 / AGP 9.4.1):**
- `assembleDebug` → ~20 MB, `assembleRelease` (R8) → **1.69 MB** unsigned APK.
- Release build passes `lintVital` (Play submission lint gate).
- Google Play prefers an **AAB** for uploads — run `.\gradlew.bat bundleRelease` with the
  signing config from the guide above before uploading to the listing.

### Toolchain (upgraded 2026-10-21, re-verified on device)
| Tool | Version | Note |
| --- | --- | --- |
| Android Gradle Plugin | 9.4.1 | new DSL enabled (no `android.newDsl=false` opt-out) |
| Gradle | 9.8.2 | wrapper updated |
| Kotlin | 2.4.21 | AGP 9's **built-in Kotlin**; the standalone `org.jetbrains.kotlin.android` plugin is no longer applied (AGP 9's new DSL rejects it) |
| KSP | 2.3.12 | Room's code generator |
| Compose BOM | 2026.09.00 | Compose 1.12.1 — needs `compileSdk 37` |
| Room | 2.8.5 | `fallbackToDestructiveMigration(dropAllTables = true)` |
| Play Billing | 9.1.0 | suspend `queryProductDetails` / `acknowledgePurchase` |
| androidx | core-ktx 1.19.1, activity-compose 1.13.0, lifecycle 2.11.0, datastore 1.2.1 | unused navigation + viewmodel dependencies dropped |
| compileSdk / targetSdk | 37 / 36 | `android-37.0` platform; Play currently expects 36 for new apps |

Build with **JDK 21** (`C:\Users\User\.jdks\jbr-21.0.11`); JDK 17 still works.
The shipped package still requests **no INTERNET** permission after the upgrade.

## Signing a release build (keystore guide)
The debug build signs itself. A release build needs YOUR keystore — make it
once, guard it forever, and lose it never (a lost keystore cannot push updates
to the same listing unless Play App Signing is enrolled).

1. Generate a keystore (one command, run from the project root; replace the
   two `...` entries with your details):
   ```
   & "C:\Users\User\.jdks\jbr-17.0.14\bin\keytool" -genkeypair -v `
     -keystore numberlings-release.jks -alias numberlings -keyalg RSA `
     -keysize 4096 -validity 10950 -storetype PKCS12
   ```
   Choose a strong store password. `numberlings-release.jks` must NEVER be
   committed (it is already in `.gitignore` via `*.keystore` — rename your file
   accordingly or add it) and never inside the repo folder if you can help it.
2. Wire the signing config in `app/build.gradle.kts` through a local file that
   stays out of git (e.g. `keystore.properties` in the repo root, also ignored):
   ```
   storeFile=C:/path/to/numberlings-release.jks
   storePassword=...
   keyAlias=numberlings
   keyPassword=...
   ```
   and read it in `app/build.gradle.kts` (loadFileOrNull pattern), falling back
   to a plain unsigned release when the file is absent — so `assembleRelease`
   always succeeds on a clean clone.
3. Enroll in **Play App Signing** in Play Console and upload the release AAB/APK
   once; Google then holds the true signing key and your keystore becomes a
   long-lived backup. This is the single best protection against keystore loss.
4. Build: `$env:JAVA_HOME = "C:\Users\User\.jdks\jbr-17.0.14"; .\gradlew.bat assembleRelease`
   — R8 shrink runs automatically (per the project's `isMinifyEnabled` config).
   Expect the release APK to be noticeably smaller than the debug build.
5. Sanity: install the release APK on a device → welcome story → first meadow →
   Market Day → Grown-ups gate, all with airplane mode on.
