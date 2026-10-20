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
> **Status: the flows below were executed on an emulator (Medium_Phone API 37, 1080×2400)**
> **on 2026-10-19 against v0.9.9-sources. Screenshots reviewed step by step.**
> Bank of findings fixed in the same pass: village header overflow clipped the
> Grown-ups button on 1080-wide screens (now two slim rows), and the gate hold
> button rendered as a broken capsule (now a clean 176 dp circle).

1. Fresh install → three-page welcome story narrates itself, dots advance, "Let's play" opens the village; second launch skips the welcome entirely. **(emulator-verified)**
2. Tap the first meadow → six puzzles → heart card appears. **(emulator-verified)**
3. Tap wrong answer → gentle cue, a sad-but-kind Numberling comforts ("we'll count it together"), same puzzle stays, no lock-outs, no score fall. **(emulator-verified)**
4. "Again, please" re-speaks the puzzle (voice setting on).
5. Village cards each show their meadow illustration; locked meadows keep sleeping. **(emulator-verified)**
6. Every 5th block ends in Market Day → tap exactly the asked number of apples → sticker lands in the album. **(emulator-verified end-to-end)** Full run executed on-device: block 5 opened Market Day, the stall keeper asked 7 apples (= 3 + stickerIndex % 5), exactly 7 taps, "Ready!" granted "Sticker 5 of 30", slot 5 filled meadow-green in the album, and it **survived a full app restart** ("Earned so far: 1 of 30"). Found and fixed during this run: Market Day's apple plates overflowed a single row (apples 6–9 unreachable on any phone — now a wrapping FlowRow), and stickers were never written to disk (the album reset to 0/30 on relaunch — now persisted in `persistSnapshot`).
7. Grown-ups button → gate → 3 s hold → adult question (11 × 9) → settings opens; child cannot pass by tapping fast. **(emulator-verified)**
8. Unlock button without Play Store → honest "not reachable" message + Community Unlock.
9. Airplane mode on → everything above still works identically. **(partially covered: emulator has no SIM; Play not signed in for the sideloaded install)**
10. Session cap 5 min → rest card appears after ~5 minutes and never forces anything.
11. `gradle test` → engine tests green (see `app/src/test`). **(verified locally)**

## Night verification run — 2026-10-20 (v0.9.9-sources + fixes)
All flows below were executed live on the same emulator (API 37). State at the end
of the run: blocks 61, hearts 4, stickers 3 of 30 (slots 5/10/25), all six meadows
awake via Community unlock, narration ON, sleep cap 12 min — all persisted.

1. **System back never leaves the child's world anymore.** Hardware back from a
   meadow block, from the sticker album and from the Grown-ups zone all return to
   the village; system back from the village itself still exits normally. Found and
   fixed during this run: back exited the whole app from any non-village
   destination (added `BackHandler` in `AppRoot.kt`), and `GrownUpsHome` never
   wired its `backToPlay` callback, leaving the "Back to play" path dead (added the
   header TextButton). **(emulator-verified: screenshots from both destinations)**
2. **Welcome-story replay works from settings.** Toggling "Replay the welcome
   story" then "Back to play" lands on story page 1 of 3; completing all three
   pages returns to the village with progress untouched and the toggle OFF again.
   **(emulator-verified end-to-end)**
3. **Heart/block progress race — found late, fixed, retested.** A completed block
   granted "one more heart", but the village header still read `hearts 3 ·
   puzzles 60`; instrumented logs caught the cause: the Room observer echoed every
   persisted row straight back into memory, so a stale snapshot write could
   clobber the freshly earned heart/block (and late persist coroutines made the
   final DB row stale too). Fix in `VillageRepository.kt`: the database is read
   **once** at startup, in-memory state is the truth during play, snapshot values
   are captured on the caller's thread at call time, and writes are serialized
   with a `Mutex`. Retested: full block → `hearts 4 · puzzles 61` on the map,
   "Hearts earned: 4 — puzzle blocks: 61" in the grown-ups zone, and the row on
   disk (`hearts=4, blocksCompleted=61`, mastery grown) survives a force-stop +
   relaunch including WAL replay. **(emulator-verified)**
4. **Sleep invitation, both paths.** With the cap at its tested value the
   invitation fires on schedule ("The village is getting sleepy. Your meadows will
   still be here later — no rush, ever."). "A few more puzzles, please" returns to
   the village; "Sweet dreams — see you soon" also returns to the village — no
   lockout, no forced exit, no shaming. Nothing navigates the child out of the
   book (the unused `Destination.Sleeping` route presents the same invitation).
   **(emulator-verified)**
5. **Share progress intent fires.** "Share progress with a grown-up" opens the
   system share sheet pre-filled with "Numberlings progress" + the skill mastery
   CSV (`skill,mastery_percent`, 0,96 / 1,95 / … matching on-device mastery). No
   crash without accounts configured. **(emulator-verified)**
6. **Narration toggle verified both ways.** With narration ON, entering a meadow
   triggers TTS synthesis (visible in logcat); with it OFF, entering a meadow is
   completely silent. Toggle restored to ON afterwards. **(emulator-verified via
   logcat)**
7. **Wrong answer → heart cost confirmed live:** map header moved 4 → 3 after one
   wrong answer; mid-block exit afterwards preserved both progress numbers.
   **(emulator-verified)**
8. **Sticker album display-only by design** (waiting for Market Day slots to
   fill); tapping a sticker currently gives no visual/audio feedback — noted below
   as a polish item, not a defect.

### Minor observations, cosmetic / polish (no action taken at this pass)
- First launch after install/rebuild holds a blank cream beat ~4–6 s (splash +
  DataStore init) before the village draws; subsequent launches are faster.
- Times Orchard's faint square boxes behind the leaf clusters are intentional
  placeholder art (see ASSETS.md), reading as gentle texture rather than a glitch.
- "Fraction Forest & Shape Shire" is a long card title; it wraps slightly snug on
  narrow cards but never truncates or overflows.
- The sleep timer counts one continuous composer lifetime; choosing "A few more
  puzzles, please" does not re-arm a fresh countdown — acceptable for now (the
  invitation is advisory, not a lock), recorded here for honesty.

## Known limitations, documented honestly
- The single IAP requires one product ID configured in Play Console before release
  (`UnlockStore.billingConfigured`); until then the Community Unlock always works.
- Voice narration depends on the system TTS voice availability.
- Art is vector/Canvas placeholders of the intended "paper cut-out" style (see ASSETS.md).
