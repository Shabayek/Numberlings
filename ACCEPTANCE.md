# ACCEPTANCE.md — how this build satisfies the charter

| Charter rule | Evidence in this repo |
| --- | --- |
| COPPA / GDPR-K / Play Families | No INTERNET permission; Room on-device only; no third-party telemetry. See `STORE_READY.md` Data Safety answers. |
| No ads | No ad SDKs in `app/build.gradle.kts`; nothing to load. |
| No loot boxes / random rewards | Rewards are deterministic (`VillageRepository.celebrateAndRest`, hearts only); seeds are for puzzle difficulty, never for prizes. |
| Purchases behind a parental gate | `ui/grownups/GrownUpsGate.kt` (3s hold + mental arithmetic) is the only path to `GrownUpsHome` and its `UnlockButton`. |
| One-time purchase only | `UnlockStore` wires a single INAPP product; `billingConfigured=false` until Play Console SKU exists (documented). |
| Community unlock | Buttons live in `GrownUpsHome` — no stigma, no questions, same unlock. |
| No dark patterns | No streak counters that punish, no timers, no skip-with-money; `PuzzleFrame` respects wrong answers with a kind pause and "Again, please". |
| Offline playable | No network calls anywhere; airplane-mode safe. |
| Pre-reader friendly | Every task has `spoken` text; `Speaker` (system TTS) reads it; "Again, please" repeats it. |
| Wellbeing | `SleepyInvitation` shows after the grown-up cap (12 min default), always dismissible. |
| Adaptive without pressure | `AdaptiveState.nextSkill` picks the weakest skill, tiers move with mastery, bias knob never punishes. |

## Manual test plan (host device or emulator)
1. Fresh install → tap the first meadow → six puzzles → heart card appears.
2. Tap wrong answer → gentle cue, same puzzle stays, no lock-outs, no score fall.
3. "Again, please" re-speaks the puzzle (voice setting on).
4. Grown-ups button → hold 3 s → adult question → settings opens; child cannot pass by tapping fast.
5. Unlock button without Play Store → honest "not reachable" message + Community Unlock.
6. Airplane mode on → everything above still works identically.
7. Session cap 5 min → rest card appears after ~5 minutes and never forces anything.
8. `gradle test` → engine tests green (see `app/src/test`).

## Known limitations, documented honestly
- The single IAP requires one product ID configured in Play Console before release
  (`UnlockStore.billingConfigured`); until then the Community Unlock always works.
- Voice narration depends on the system TTS voice availability.
- Art is vector/Canvas placeholders of the intended "paper cut-out" style (see ASSETS.md).
