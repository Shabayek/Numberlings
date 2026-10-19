# ACCEPTANCE.md — how this build satisfies the charter

| Charter rule | Evidence in this repo |
| --- | --- |
| COPPA / GDPR-K / Play Families | No INTERNET permission; Room on-device only; no third-party telemetry. See `STORE_READY.md` Data Safety answers. |
| No ads | No ad SDKs in `app/build.gradle.kts`; nothing to load. |
| No loot boxes / random rewards | Rewards are deterministic (`StickerAlbum.indexFor(blocksCompleted)` + Market Day cadence; hearts only otherwise); seeds drive puzzle difficulty, never prizes. |
| Purchases behind a parental gate | `ui/grownups/GrownUpsGate.kt` (3s hold + mental arithmetic) is the only path to `GrownUpsHome` and its `UnlockButton`. |
| One-time purchase only | `UnlockStore` wires a single INAPP product; `billingConfigured=false` until Play Console SKU exists (documented). |
| Community unlock | Buttons live in `GrownUpsHome` — no stigma, no questions, same unlock. |
| No dark patterns | No streak counters that punish, no timers, no skip-with-money; `PuzzleFrame` respects wrong answers with a kind pause and "Again, please". |
| Offline playable | No network calls anywhere; airplane-mode safe. |
| Pre-reader friendly | Every task has `spoken` text; `Speaker` (system TTS) reads it; "Again, please" repeats it. |
| APK ≤ 25 MB | Verified: `assembleRelease` (R8 + resource shrink) produces a **1.49 MB** APK (`app-release-unsigned.apk`); debug build 17.8 MB. |
| Wellbeing | `SleepyInvitation` shows after the grown-up cap (12 min default), always dismissible. |
| Adaptive without pressure | `AdaptiveState.nextSkill` picks the weakest skill, tiers move with mastery, bias knob never punishes. |

## Manual test plan (host device or emulator)
1. Fresh install → three-page welcome story narrates itself, dots advance, "Let's play" opens the village; second launch skips the welcome entirely.
2. Tap the first meadow → six puzzles → heart card appears.
3. Tap wrong answer → gentle cue, a sad-but-kind Numberling comforts ("we'll count it together"), same puzzle stays, no lock-outs, no score fall.
4. "Again, please" re-speaks the puzzle (voice setting on).
5. Village cards each show their meadow illustration; locked meadows keep sleeping.
6. Every 5th block ends in Market Day → tap exactly the asked number of apples → sticker lands in the album.
7. Grown-ups button → hold 3 s → adult question → settings opens; child cannot pass by tapping fast.
8. Unlock button without Play Store → honest "not reachable" message + Community Unlock.
9. Airplane mode on → everything above still works identically.
10. Session cap 5 min → rest card appears after ~5 minutes and never forces anything.
11. `gradle test` → engine tests green (see `app/src/test`).

## Known limitations, documented honestly
- The single IAP requires one product ID configured in Play Console before release
  (`UnlockStore.billingConfigured`); until then the Community Unlock always works.
- Voice narration depends on the system TTS voice availability.
- Art is vector/Canvas placeholders of the intended "paper cut-out" style (see ASSETS.md).
