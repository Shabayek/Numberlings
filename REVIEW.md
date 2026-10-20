# Numberlings — External Adversarial Review

**Package:** `com.vividoai.numberlings`  ·  **Build reviewed:** `app-debug.apk` built from HEAD (`36ccc02`), reinstalled on the device before the final test pass
**Device:** Android emulator `Medium_Phone_API_37.0` (1080×2400, API 37), gesture navigation
**Method:** black-box UI driving via `adb` (taps, swipes, key events) + white-box source audit + DB/DataStore pulls as ground truth. No app source was modified.
**Session log:** `C:\Users\User\AppData\Local\Temp\opencode\review_logcat_full.txt` (370 331 lines; continuous capture with two documented gaps — the emulator reboot and a capture restart — both re-verified afterwards)
**Evidence:** all `rev_*.png` screenshots, `ds_*.bin` DataStore pulls, `db_*.db` SQLite pulls and `solver_log*.txt` in `C:\Users\User\AppData\Local\Temp\opencode\`

---

## 1. Executive summary

Numberlings is a small, carefully written offline maths game for pre-readers, and it is stable: across the whole session (including forced kills, a wiped-data first run, a purchase attempt on an emulator without Play, a share flow and roughly 120 puzzle interactions) the app produced **no ANRs, no StrictMode violations and exactly one uncaught exception in the whole capture** — a null-pointer in the background progress-write path that terminated the process once (F-22; it did not reproduce under normal interaction, and the only other `FATAL EXCEPTION` records in the file belong to the `uiautomator` CLI, not to the app). The visual language is consistent and the wrong-answer path is genuinely kind ("Not quite yet — the Numberlings will count it together with you").

The bugs that do exist cluster around three themes.

1. **The pictures sometimes lie about the maths.** The most serious finding is that the counting artwork can only ever draw 18 objects, while the engine is allowed to ask for 19 or 20. A child who counts the dots (the only method the app teaches) taps the number the picture shows and is told they are wrong — an unwinnable puzzle inside the app's core skill. I reproduced this live and repeatedly: 18 dots drawn, option "18" rejected 11 times in a row (16 in the second solver run). Two related visuals (unknown addend, remainder, per-plate share) draw the answer itself.
2. **The grown-up controls promise more than the code delivers.** Of the five audio knobs, three never reach the audio layer at all: "Soft sound effects", "Sound level" and "Voice pace" are persisted perfectly to DataStore and then ignored, because the only method that would apply them (`GameHub.apply`) has no caller in the codebase. The purchase surface ("Unlock all six meadows", "Apply Community unlock") writes no state anywhere and unlocks nothing — every meadow is already opened by *progress*, never by purchase — while the UI tells the family it "opens everything".
3. **Honesty gaps.** The in-app privacy text promises "no internet permission", but the shipped APK requests `INTERNET` and `ACCESS_NETWORK_STATE` (injected by the Play Billing library merge), and the manifest's own comment ("Deliberate: no INTERNET permission") is contradicted by its own build output. A sticker is announced as earned ("Thank you! Sticker 5 of 30 is yours") when the album silently refuses it because the child already owns it — and after the 30-sticker cycle repeats, *every* later Market Day becomes a no-op.

Persistence itself is in good shape: every grown-up setting survived a three-way cross-check (UI ↔ DataStore ↔ restart), progress survives force-stop, and a fresh install behaves correctly end to end.

Severity tally: **1 Critical, 5 Major, 9 Minor, 7 Polish** (22 findings), plus 16 verified-clean areas.

---

## 2. Findings log

### F-01 · CRITICAL · Counting artwork caps at 18 objects, so tasks asking for 19–20 cannot be solved by counting

**Repro (live, HEAD build)**
1. Open *Counting Meadow* (block 65+, mastery 84 % → tier 2).
2. A task appears: *"Count what you see, then tap the number."* with options **22 / 18 / 17 / 19**.
3. Count the picture: **18 dots** (3 rows of 6).
4. Tap **18** → the app replies *"Not quite yet — the Numberlings will count it together with you."*

**Observed:** the picture shows 18 objects; the true answer is 19; the number the child counts is always rejected.
**Expected:** the picture shows exactly as many objects as the answer requires — the app's own source comment says "a hidden dot would make the puzzle a lie".

**Evidence**
- `rev_count19.png` — 18 dots drawn, options 22/18/17/19, comfort message visible after tapping 18.
- `solver_log.txt` / `solver_log2.txt` — the automated driver tapped the counted value on 16 consecutive rounds of the same task (`dots=18 → tapped=18 → wrong=True` every time; 16 such records in `solver_log2.txt` alone); the puzzle never accepted the counted answer.
- Source: `PuzzleArt.kt:87-99` `concentricPrefix()` — `rows = maxOf(1, (count + 5) / 6).coerceAtMost(yList.size)`; for `count > 12` the caller passes a 3-row `yList` (`PuzzleArt.kt:79-83`), so at most 3 × 6 = **18** dots can ever be drawn.
- Source: `TaskFactory.kt:63-74` `count()` — tier 2 → `nextUBTo(top = 20)` → `Random.nextInt(3, 21)` → **19 and 20 are reachable**.

**Confidence:** High — reproduced live on the shipped build, and the cap is provable from the drawing code.

**Root-cause sketch:** the row budget for the skittle layout was fixed at three rows (six per row) while the number range was widened to 20; `concentricPrefix` silently drops the overflow instead of adding a row or shrinking the dots. Fix: allow `rows = ceil(count / 6)` up to the canvas height, or cap the COUNT range at 18, or scale dot radius so 19–20 fit.

---

### F-02 · MAJOR · Market Day announces a sticker that is silently discarded when the child already owns it

**Repro (live, HEAD build)**
1. Progress: blocks 64 → complete one block (blocks = 65). Market Day opens ("The stall keeper would love exactly 7 apples").
2. Put 7 apples in the basket, tap **Ready!**
3. The app says **"Thank you! Sticker 5 of 30 is yours."**

**Observed:** the album still reads *"Earned so far: 3 of 30"*, and the database `stickersCsv` stays `'4,9,24'`. The child is thanked for a sticker they already had and nothing is added.
**Expected:** either the album grows, or the app says "you already have this one".

**Evidence**
- `rev_md3.png` — the message "Thank you! Sticker 5 of 30 is yours.".
- `rev_album2.png` — album header "Earned so far: 3 of 30" (unchanged).
- `db_md.db` (SQLite, WAL-checkpointed) — `('village', '0:91;1:84;2:80;3:44', '4,9,24', 65, 65)`: `stickersCsv` identical to before the award, while `hearts`/`blocksCompleted` advanced.
- Source: `StickerAlbum.indexFor(blocks) = (blocks - 1) % 30` — indices repeat every 30 blocks; `grantSticker()` adds to a `Set`, so a duplicate is dropped without notice; the award message is printed unconditionally.
- Fresh-install control: on a wiped install the *same* code path correctly granted a new sticker (`rev_p2_md2.png`, album "Earned so far: 1 of 30", `db_p2b.db` → `stickersCsv = '4'`), proving the drop happens only on duplicates.

**Confidence:** High — three-way evidence (UI + album UI + DB row), plus a working control case.

**Root-cause sketch / blast radius:** `indexFor` cycles with period 30 while Market Day recurs every 5 blocks (index % 5 == 4), so from block 35 onwards **every** Market Day re-awards an already-owned sticker. The entire sticker economy stops producing anything new after the first 30 blocks. Fix: compute the next *unowned* index, or announce "already yours" instead of "is yours".

---

### F-03 · MAJOR · "Soft sound effects", "Sound level" and "Voice pace" are dead controls

**Repro (live, HEAD build)**
1. Grown-ups zone → *Soft sound effects* → OFF. The switch flips and the value persists (`sound=0` in the DataStore).
2. *Sound level* → drag to 0/100; *Voice pace* → drag to either end; the label changes to **"2.0x voice"**.
3. Play a puzzle: sound cues still play at the original volume, and narration is still spoken at the original pace.

**Observed:** the switches move and the values are saved, but the audio layer never changes.
**Expected:** turning "Soft sound effects" off silences the cues; "Sound level" sets their volume; "Voice pace" changes the narration speed (the UI advertises up to 2.0×).

**Evidence**
- `ds_h1.bin`, `ds_h2.bin`, `ds_h6.bin`, `ds_h7.bin` — the values *are* persisted (narration 1→0→1, sound 1→0→1, voice_speed 5 and 15, all binary-safe protobuf pulls).
- `rev_vs_max.png` — the UI claims "2.0x voice" at the maximum.
- Source: `GameHub.kt:28-35` `apply(grownUps)` is the only writer of `speaker.rate` / `sound.setVolume(...)` — and `grep -rn "GameHub.apply"` finds **no call site** in `main/`; `grep -rn "setVolume"` finds only the definition in `SoundCue.kt:28`.
- Source: `Speaker.kt:18-22` — `rate` is clamped to `0.5f..1.5f` and defaults to `0.9f`, so even if `apply()` were called the advertised 2.0× would be unreachable.
- Source: `SoundCue.kt:14,24,36-39` — `setVolume()` only updates a field; the `ToneGenerator` is created lazily with the volume captured at first use, and the mute checks read that never-changing field.

**Confidence:** High for the wiring defect (provable by call-graph: zero call sites). The *runtime* consequence is code-level only: Android exposes no channel to observe `ToneGenerator` output from `adb` (attempted: `dumpsys media.audio_flinger`, `dumpsys audio` — no attributable track), so "the cues still play" is inferred from the code, not heard.

**Root-cause sketch:** the repository that owns the settings never notifies the audio layer; `GameHub.apply()` was written but never hooked to `settings.onEach` in `VillageRepository.init`. Note also that `SoundCue.setVolume()` is ineffective even when called, because the synthesiser is already constructed.

---

### F-04 · MAJOR · The purchase and "Community unlock" unlock nothing (and say they do)

**Repro (live, HEAD build + fresh install)**
1. Grown-ups zone → **Apply Community unlock** → snackbar: *"Community unlock applied — welcome to the whole village."*
2. Check every store: DataStore is unchanged, the database has no ownership column, the six meadows are exactly as before.
3. *Unlock all six meadows — one time, no extras* → snackbar: *"Google Play isn't reachable here."*

**Observed:** two controls that promise to open the whole village, neither of which writes any state; and a purchase path that is hard-wired off (`billingConfigured = false`).
**Expected:** either the unlock works, or the copy does not promise it.

**Evidence**
- `rev_p2_community.png` — fresh install (wiped data), community unlock tapped, snackbar shown.
- `rev_head_community3.png` — same on the seeded install.
- `ds_p2c.bin`, `ds_comm.bin` — DataStore after the tap: identical to before (no unlock key exists in the schema at all).
- `db_p2b.db` — the only table (`progress`) has no ownership column.
- Source: `VillageScreen.kt:79,115` — the only unlock logic is `blocks >= unlockBarrier(index)` with `unlockBarrier(i) = 12 * i` (progress gates at 12/24/36/48/60). **No purchase state is consulted anywhere**, so paying could not change what is open.
- Source: `UnlockStore.kt:34,67` — `billingConfigured = false` ⇒ `purchase()` always returns `NOT_ON_THIS_DEVICE`.

**Confidence:** High — verified on both a fresh install and a seeded install, with the absence of any state confirmed by reading the DataStore schema, the DB schema and the source.

**Root-cause sketch / risk:** the feature is a UI-only shell. A family that taps through to a real Play purchase (once `billingConfigured` is flipped) would pay for nothing, and the community copy currently asserts an effect that does not exist. Fix: either persist an ownership flag and apply it to `unlockBarrier`, or remove/soften the copy until it is implemented.

---

### F-05 · MAJOR · The shipped APK requests INTERNET, contradicting the in-app promise and the manifest comment

**Repro**
```
aapt2 dump permissions app-debug.apk
  uses-permission: name='com.android.vending.BILLING'
  uses-permission: name='android.permission.ACCESS_NETWORK_STATE'
  uses-permission: name='android.permission.INTERNET'
```

**Observed:** the shipped package holds network permissions.
**Expected:** the in-app promise reads *"1. Nothing is collected. The app has no internet permission, so progress, settings and drawings never leave this device."* and `AndroidManifest.xml` states *"Deliberate: no INTERNET permission. Nothing leaves the device."*

**Evidence**
- `aapt2 dump permissions` output above (build artefact `app/build/outputs/apk/debug/app-debug.apk`).
- `strings.xml:14` (the privacy text shown in the Grown-ups zone) and `AndroidManifest.xml:4` (the source comment).
- Root cause: the Play Billing library's manifest contributes `INTERNET` and `ACCESS_NETWORK_STATE` during the merge; the app's own manifest never asked for them, so the comment reads true of the source and false of the artefact.

**Confidence:** High — measured directly from the artefact that ships.

**Note:** the app itself still makes no network calls that I could observe (no `TrafficStats`/socket activity attributable to the app in logcat), so this is a *truthfulness* defect rather than a data-leak finding. Fix: add `tools:node="remove"` for the two permissions in the app manifest, or amend the copy to describe the billing library honestly.

---

### F-06 · MAJOR · Some pictures draw the answer itself (unknown addend, remainder, per-plate share)

**Repro (live, HEAD build)**
1. *Addition Field* → *"9 + ? = 12"* → the picture shows 9 orange objects **and 3 blue objects**; the options are 3/1/2/6. Counting the blue group gives the answer without any addition.
2. *Subtraction Brook* → *"6 − 2 = ?"* → the picture shows 4 green objects (the remainder) and 2 pink (the taken-away); the options are 4/7/6/3.
3. *Share Fair* → *"Share 4 cakes on 2 plates — cakes per plate?"* → the picture shows 2 plates with 2 cakes each; the answer is countable.

**Observed:** the unknown quantity is rendered.
**Expected:** for the missing-addend task, the picture should show the 9 that are known and leave the unknown unpictured, so the child must work it out.

**Evidence**
- `rev_times1.png` (9 + ? = 12 with 3 blue dots), `rev_sub1.png` (6 − 2 = ? with 4 green dots), `rev_sharefair.png` (2 cakes per plate).
- Source: `TaskFactory.addMissing()` builds a `JOIN` visual with both groups; `PuzzleArt` draws both.

**Confidence:** High for the missing-addend case (reproduced on three separate puzzles earlier in the session — `rev_gate10.png`, `rev_cap_min4.png`, `rev_times1.png`). **Medium** for the subtraction/share cases: showing the remainder or the equal share is defensible pre-reader pedagogy, so I report them as the same class but not necessarily as defects; the missing-addend case is unambiguous.

---

### F-07 · MINOR · The grown-up gate gives no visible feedback for a wrong answer

**Repro (live, HEAD build)**
1. Grown-ups → hold 3 s → question "8 × 8".
2. Type `abc` (or `999999999999`, or `-5`) → tap **Check**.

**Observed:** nothing changes on screen — no error, no shake, no hint; the only feedback is a faint tone. A grown-up cannot tell whether Check registered, nor whether the answer was rejected.
**Expected:** a visible "not quite, try again" state for a wrong entry (an empty entry correctly does nothing, but a wrong one should say so).

**Evidence**
- `rev_head_gate_letters.png` (letters accepted, no feedback), `rev_gate_huge.png`, `rev_gate_neg.png`, `rev_gate_letters2.png`.
- Source: `GrownUpsGate.kt:150-156` — the wrong branch only calls `sound?.maybe()`; `var shaky by remember { mutableStateOf(false) }` (line 133) is declared and **never used** (dead code — a shake animation was clearly intended).
- Robustness confirmed: no crash for letters, a 12-digit number, or a negative value (`isRight()` uses `trim().toIntOrNull()`, so all of these are simply "wrong").

**Confidence:** High.

---

### F-08 · MINOR · The comfort line is spoken only once per puzzle

**Repro / code**
`TaskScreen.kt:254-256` speaks the comfort line from a `LaunchedEffect(task)` **inside** the "encouraged" block. The effect is keyed on `task`, so the first wrong answer speaks "Let's count together." and a *second* wrong answer on the same puzzle re-composes the same key → no new speech.

**Observed:** the on-screen comfort text stays visible across repeated wrong taps (`rev_wrong1.png` → `rev_wrong2.png`), but the audio does not repeat.
**Expected:** each new wrong attempt should be acknowledged, especially for a child who is trying again.

**Evidence:** `rev_wrong1.png`, `rev_wrong2.png` (text present both times), `TaskScreen.kt:242-257`.
**Confidence:** Medium-High — the text behaviour is live-confirmed; the audio claim is code-level (no channel to hear TTS from `adb`).

---

### F-09 · MINOR · The sleep invitation is a one-shot: "A few more" never re-arms it

**Repro (live, HEAD build)**
1. Grown-ups → *Sleep invitation after* → set to the minimum (5 minutes) → Back to play.
2. The invitation appears after the cap elapses (observed at 20:09:30 for a session started at 20:04:25 — the timer itself works).
3. Tap **A few more puzzles, please** → the invitation closes and the map returns.
4. Keep the app in the foreground and watch for a further full interval: **no new invitation appears** (watched continuously until 20:18:06 — 8½ minutes after the dismissal, well past the 5-minute cap).

**Observed:** the rest prompt fires once per cap value and never again in that session; a child who asks for "a few more" gets silence for the rest of the session.
**Expected:** choosing "a few more" should re-arm the timer for another interval.

**Evidence:** `rev_sleep_invite.png` (invitation shown at the 5-minute cap), `rev_sleep_no_rearm.png` (the app sitting on the map at the end of the 8½-minute watch window, no second invitation), `ds_sleep5.bin` (`session_cap=5` while testing), watcher transcript (device clock 20:09:30 → 20:18:06).
**Source:** `AppRoot.kt:62-66` — `LaunchedEffect(settings.sessionCapMinutes)` arms a single `delay(cap * 60_000)` that sets `sleepy = true` once; `SleepyInvitation(onPlayMore = { sleepy = false })` (line 78) clears the flag but nothing re-arms the effect, because the effect's key (the cap) has not changed.

**Confidence:** High — reproduced live (invitation fires; it does not fire again after "a few more"), and the code path is a one-shot by construction. Note the related design gap: the cap is the *only* trigger, so the invitation also never returns if the cap value is changed but the app is not restarted.

---

### F-10 · MINOR · Mid-block progress is lost on process death while the answers are already banked

**Repro (live, HEAD build)**
1. Counting Meadow, solve one task ("solved 1" visible).
2. Force-stop the app (`am force-stop`) and relaunch.
3. The app opens on the map with no block in progress, but the mastery row in the database already contains the answer that was given before the kill.

**Observed:** the two halves of the session disagree — the child's answer counts, their place in the block does not.
**Expected:** either both are saved or neither is.

**Evidence**
- `db_before_dbl.db` → `('village', '0:84;1:67;2:80;3:44', '4,9,24', 64, 64)` (mastery already updated by the pre-kill answer) while the UI returned to "solved 0" of a fresh block.
- Source: `TaskScreen.kt:65` (`var solvedInBlock by remember { mutableIntStateOf(0) }`) and `AppRoot.kt:46` (`var destination by remember { mutableStateOf<Destination>(...) }`) — plain `remember`, not `rememberSaveable`.

**Confidence:** High.

---

### F-11 · MINOR · A single wrong tap costs ~15 mastery points, and every retry counts again

**Repro (live, HEAD build)**
1. Counting Meadow, answer a compare task wrongly once.
2. Read the database: the COMPARE skill's mastery moved from **82 → 67** (one tap).
3. Answer wrongly twice more: each tap is recorded again, with no attempt cap.

**Observed:** one exploratory tap costs as much as five wrong answers would in a gentler model, and the child is punished repeatedly for the same puzzle.
**Expected:** a first-try miss should be worth less than a repeated miss, or repeated misses on the same puzzle should be damped.

**Evidence:** `db_before_dbl.db` (`1:67` after one wrong tap) vs the pre-test pull (`1:82`); `rev_wrong1.png`.
**Source:** `TaskScreen.kt:155-178` — every tap calls `GameHub.repository.record(task.skill, right)`; `Adaptive` applies a fixed penalty per wrong answer.

**Confidence:** High.

---

### F-12 · MINOR · The skip-counting picture has no numbers, no start and no hop marks

**Repro (live, HEAD build)**
1. *Times Orchard* → *"Skip count by 2 — land after 3 hops."* → the picture is a bare green line with one orange landing circle.

**Observed:** nothing on the line marks where the counting starts (the start is 0, in the code only), how big the hops are, or where 0 is. The task is unanswerable from the picture; the prompt alone does not state the starting number.
**Expected:** a number line for pre-readers needs a visible start (0) and visible hop marks.

**Evidence:** `rev_times2.png`; `PuzzleArt.kt:150-168` `drawHopLine()` draws only a line and a landing circle (the halo radius is the only hop signal and is not interpretable); `TaskFactory.kt:166-179` sets `from = 0` in the visual but never in the prompt.

**Confidence:** High.

---

### F-13 · MINOR · A double tap on an answer can score the *next* puzzle

**Repro (live, HEAD build)**
1. Answer a compare task correctly — the next puzzle is prepared immediately.
2. A second tap ~100 ms later lands on the freshly prepared puzzle and is scored as an answer to it (wrong if it hits the other group).

**Observed:** the second tap is delivered to the new task; in my run it landed on a counting task's canvas and was absorbed (no score), but the same gesture on a compare task hits a live button.
**Expected:** a burst of taps from one intent should be attributed to one attempt.

**Evidence:** `rev_dbl1.png` → `rev_dbl2.png` (after the double tap the screen shows the *next* task, "solved 1"); `db_after_dbl.db` (`1:72` — exactly one record, so no double counting of the first answer, but the second tap was delivered to the new puzzle).
**Source:** `TaskScreen.kt:158-173` — the correct branch calls `preparePuzzle()` synchronously, so the next task is live within the same frame; there is no input debounce.

**Confidence:** Medium — the delivery mechanism is proven, the "wrong answer recorded on the new puzzle" outcome is code-level (not reproduced as a wrong score in my runs).

---

### F-14 · MINOR · The grown-up gate re-arms on every app start

**Repro (live, HEAD build)**
1. Unlock the Grown-ups zone (hold + arithmetic).
2. Force-stop and relaunch the app.
3. The Grown-ups entrance demands the 3-second hold **and** a new arithmetic question again.

**Observed:** a parent who just unlocked the zone must re-do the ritual after every restart (e.g. after the OS reclaims the app, or after switching children).
**Expected:** a short session grace period, or re-use of the unlocked state until the app has been away for a while. (Onboarding, by contrast, is explicitly "never re-gated" — the inconsistency is the point.)

**Evidence:** `rev_cap_recheck.png` (gate shown again immediately after a successful unlock + restart), `rev_gate9.png` … `rev_head_gate.png` (a fresh question each session: "8 × 10", "5 × 6", "9 × 6", "9 × 12", "8 × 8", "11 × 12", "7 × 11").
**Source:** `GrownUpsGate.kt:47-60` — the passed state is `remember`ed inside the composable, so it dies with the process.

**Confidence:** High.

---

### F-15 · POLISH · The hold-to-validate ring keeps its last fill after release

**Repro:** In the gate, hold the round button for ~1 s and release: the progress ring stays partially filled, implying progress that is no longer happening; it only resets on the next hold.

**Evidence:** `rev_gate3.png`, `rev_gate4.png`.
**Source:** `GrownUpsGate.kt:67-78` — `fill` is reset only inside `LaunchedEffect(holding)` when a new hold *starts*.
**Confidence:** High (visual), Medium (whether a reset on release is desired is a product decision).

---

### F-16 · POLISH · Consecutive snackbars queue, so the second message is delayed

**Repro:** Tap *Unlock all six meadows* and then, ~1 s later, *Apply Community unlock*: the screen still shows the purchase message; the community message only appears after the first one times out (~4 s later).

**Evidence:** `rev_head_purchase.png` and `rev_head_community.png` show the *same* text; `rev_head_community3.png` (captured after the queue drained) shows the community text.
**Source:** `GrownUpsHome.kt:46,124` — a single `SnackbarHostState`; `showSnackbar` suspends until dismissal, so the second call queues behind the first.
**Confidence:** High.

---

### F-17 · POLISH · The purchase button shows no price, and cannot work

**Repro:** The CTA reads "Unlock all six meadows — one time, no extras" with no amount; tapping it can only ever produce "Google Play isn't reachable here.".

**Evidence:** `rev_head_purchase.png`; `UnlockStore.kt:34` (`billingConfigured = false`), `UnlockStore.kt:67`.
**Confidence:** High.

---

### F-18 · POLISH · `Destination.Sleeping` is unreachable dead code

**Evidence:** `AppRoot.kt:121-124` defines a `Destination.Sleeping` branch that renders `SleepyInvitation`, but the invitation is only ever shown through the `sleepy` flag in the `Destination.Village` branch (`AppRoot.kt:76-80`). No code assigns `Destination.Sleeping`.
**Confidence:** High (code-level, exhaustively grepped).

---

### F-19 · POLISH · Progress writes are ordered by luck, not by construction

**Evidence:** `VillageRepository.kt:102-126` — each `persistSnapshot()` captures values on the caller thread (good) and then launches a coroutine on `Dispatchers.IO`; the `Mutex` serialises the *writes*, but the coroutine *start order* is not guaranteed, so an older snapshot can be written after a newer one. The comment claims "their order follows the calls".
**Confidence:** Medium — the ordering hazard is real but unobserved in this session (every cross-check passed); a single-writer, single-queue design would remove it.

---

### F-20 · POLISH · The narrator is killed on activity destruction and never revived

**Evidence:** `MainActivity.kt:23-26` calls `GameHub.shutter()` in `onDestroy()`, which calls `speaker.shutdown()`; `GameHub.wire()` (`GameHub.kt:21-26`) returns early when the repository is already initialised, so after any activity recreation the `TextToSpeech` engine is dead and is never re-created — narration silently stops for the rest of the process lifetime.
**Confidence:** High (code-level). Live reproduction is not possible in-app (the activity is portrait-locked, so rotation never happens); the exposure is an OS-initiated recreation (theme/language change, or restore-after-process-death in the same process). Marked **unverified at runtime**.

---

### F-21 · POLISH · Long blank "cream beat" at start-up

**Repro:** Cold-start the app on this emulator: the screen is a flat cream rectangle for several seconds before the map appears (measured 1.3 s–12.6 s across launches; the blank frame is visible in `rev_onb1.png` and `rev_restored.png`).

**Evidence:** `rev_onb1.png`, `rev_restored.png`; `AppRoot.kt:48-59` — the UI returns an empty `Box` while `settings` is `null`.
**Expected:** a branded splash or the map shell, so a child never faces a blank screen. (Cold start on a slower device is likely worse than 12 s.)
**Confidence:** High.

---

### F-22 · MINOR · An unhandled exception in the background progress-write path kills the whole app

**Observed (once, in the logcat capture)**
```
10-20 21:11:24.958 E/AndroidRuntime( 1481): FATAL EXCEPTION: DefaultDispatcher-worker-2
10-20 21:11:24.958 E/AndroidRuntime( 1481): Process: com.vividoai.numberlings, PID: 1481
10-20 21:11:24.958 E/AndroidRuntime( 1481): java.lang.NullPointerException: Attempt to invoke interface
    method 'kotlinx.coroutines.channels.ChannelIterator kotlinx.coroutines.channels.Channel.iterator()'
    on a null object reference
10-20 21:11:24.958 E/AndroidRuntime( 1481): 	at com.vividoai.numberlings.core.data.VillageRepository$3.invokeSuspend(VillageRepository.kt:120)
```

**Observed:** a write of the progress row (the `persistSnapshot()` coroutine) threw, and because it runs on a plain `scope.launch` with no exception handler, the whole process was killed — a background bookkeeping failure takes down the child's session.
**Expected:** a failed progress write should be caught (and retried or ignored), not fatal.

**Evidence**
- `app_crash_211124.txt` — the full stack excerpt; `review_logcat_full.txt` (21:11:24) for the original.
- Source: `VillageRepository.kt:111-123` — `scope.launch { saveMutex.withLock { … database.progressDao().upsert(…) } }` with no `try/catch` and no `CoroutineExceptionHandler`.
- **Not reproduced in normal use:** a deliberate reproduction attempt on the same build (answer a puzzle correctly → the same `persistSnapshot()` write path) completed without a crash, and the rest of the session contains no second app crash.

**Confidence:** Medium — the crash is a fact in the log, but the trigger is ambiguous. It happened in the emulator instance that had just been re-created, where the app's database file was replaced underneath the running process by external tooling (the same class of operation this review used to seed the database). Room's cached connection then pointed at a replaced file. That makes a harness-induced cause plausible, so this is reported as a *robustness* defect (an unguarded background write that can terminate the app) rather than a proven user-reachable crash.

**Root-cause sketch:** wrap the write in `runCatching`/a `CoroutineExceptionHandler` and let the in-memory state stay the source of truth (which the class comment already claims it is); optionally reopen the database if the underlying file was replaced.

---

## 3. Coverage matrix

Legend: **DONE** = exercised on the device; **N/A** = not applicable to this build; **NOT COVERED** = not exercised, with the reason.

| # | Area | Status | Evidence / reason |
|---|------|--------|--------------------|
| 1 | App start → village map | DONE | `rev_reinstall1.png`, `rev_restored2.png` ("hearts 64 · puzzles 64") |
| 2 | Cold start latency / blank screen | DONE | `rev_onb1.png`, `rev_restored.png` (F-21) |
| 3 | Onboarding (3 pages, first run) | DONE | `rev_p2_onb1.png`, `rev_p2_onb2.png`, `rev_p2_onb3.png`, `rev_p2_map.png` |
| 4 | Onboarding replay from Grown-ups | DONE | `rev_replay2.png` (story restarts at page 1), `ds_replay.bin` (`onboarded=0`) |
| 5 | Meadow map: all six cards | DONE | `rev_restored2.png`, `rev_chaos2.png`, `ui_p2h.xml` (all six meadows present) |
| 6 | Progress-barrier copy (12/24/36/48/60) | DONE | `VillageScreen.kt:115`; barriers not walked one-by-one (NOT COVERED for each boundary individually — see #12) |
| 7 | Grown-ups gate: hold step | DONE | `rev_gate16.png`, `rev_head_gate.png` (hold ≥ 3 s passes; 2 s fails — earlier in session) |
| 8 | Grown-ups gate: arithmetic challenge | DONE | `rev_gate17.png` … `rev_head_gate2.png` (fresh question each session) |
| 9 | Gate input robustness (empty, letters, huge, negative, leading zeros, double-tap Check) | DONE | `rev_gate6_empty.png`, `rev_head_gate_letters.png`, `rev_head_gate_huge.png`, `rev_gate_neg.png`, `rev_gate8a.png` ("00080" accepted), `rev_gate_dbl.png` (single clean unlock) |
| 10 | Gate: wrong-answer feedback | DONE | `rev_head_gate_letters2.png` (no visible feedback — F-07) |
| 11 | Grown-ups: narration / sound toggles | DONE (persistence) | `ds_h1.bin`, `ds_h2.bin` (1→0→1 both ways) |
| 12 | Grown-ups: sound level / voice pace / sleep cap sliders | DONE | `ds_h3.bin` (cap 5), `ds_h5.bin` (cap 60), `ds_h6.bin` (voice 5), `ds_h7.bin` (voice 15), `rev_vs_max.png` ("2.0x voice") |
| 13 | Grown-ups: gentle difficulty nudge slider | DONE (persistence only) | `ds_h*.bin` (bias stays 0 in every pull; a non-zero value was not exercised live) |
| 14 | Grown-ups: purchase button | DONE | `rev_head_purchase.png` (honest snackbar, no crash) |
| 15 | Grown-ups: Community unlock | DONE | `rev_head_community3.png`, `rev_p2_community.png` (F-04) |
| 16 | Grown-ups: share progress | DONE | `rev_head_share2.png` (chooser + CSV `hearts,64 / blocks_completed,64` matching the DB) |
| 17 | Grown-ups: progress-at-a-glance vs map vs DB | DONE | `rev_head_community3.png` ("Hearts earned: 64 — puzzle blocks: 64"), `rev_restored2.png`, `db_md.db` |
| 18 | Grown-ups: privacy text vs shipped permissions | DONE | `aapt2 dump permissions` (F-05) |
| 19 | Puzzle loop: COUNT tasks | DONE | `solver_log.txt` (n = 5,6,7,10,11,12,14,15,18 — F-01 at 19) |
| 20 | Puzzle loop: COMPARE tasks (tap-the-group) | DONE | `rev_reinstall2.png`, `rev_chaos3.png`, `solver_log.txt` |
| 21 | Puzzle loop: ADD_JOIN / ADD_MISSING | DONE | `rev_times1.png` (F-06), `rev_gate10.png` |
| 22 | Puzzle loop: SUB_TAKE / SUB_DIFFERENCE | DONE | `rev_sub1.png`; a "how many more" difference task was seen but not solved separately |
| 23 | Puzzle loop: SKIP_COUNT | DONE | `rev_times2.png` (F-12) |
| 24 | Puzzle loop: TIMES_GROUPS | NOT COVERED | the times-table variant ("rows × cols = ?") was never generated in the observed sessions — Times Orchard produced SKIP_COUNT tasks instead (`rev_times2.png`); the array art is therefore unverified |
| 25 | Puzzle loop: SHARE_EVEN | DONE | `rev_sharefair.png` (F-06) |
| 26 | Puzzle loop: SHARE_LEFTOVERS | NOT COVERED | not generated during the sweeps (skill pool is randomised per session) |
| 27 | Puzzle loop: PARTITION (pizza) | DONE | `rev_shape1.png` ("A pizza has 4 slices… one half" → 2) |
| 28 | Puzzle loop: SHAPE_HUNT | NOT COVERED | not generated in the observed sessions |
| 29 | Wrong-answer comfort path (text + face) | DONE | `rev_wrong1.png`, `rev_wrong2.png` |
| 30 | Comfort audio repeats on a second miss | DONE (code-level) | `TaskScreen.kt:254-256` (F-08) — audio not observable via adb |
| 31 | "Again, please" re-reads the prompt | DONE (code-level) | `TaskScreen.kt:214-216`; the button was present on every task screen but its audio effect is not observable |
| 32 | Block completion → 6-dot progress, celebration | DONE | `rev_dbl2.png` ("solved 1" + progress dots), `solver_log.txt` (`NEXT_BLOCK` transitions, i.e. the 6-dot row completing and the celebration screen appearing) |
| 33 | Market Day: basket counting, Ready! | DONE | `rev_md1.png` → `rev_md2.png` (7 apples) |
| 34 | Market Day: sticker award (new vs duplicate) | DONE | new: `rev_p2_md2.png` + `db_p2b.db`; duplicate: `rev_md3.png` + `db_md.db` (F-02) |
| 35 | Sticker album contents | DONE | `rev_album2.png` (3 of 30), `rev_p2_album.png` (1 of 30) |
| 36 | Back button routing (task → map) | DONE | `rev_back_new1.png` (HEAD) — note: an earlier "back exits the app" observation was made on a **stale pre-reinstall build** and is *not* a finding for HEAD |
| 37 | Process death mid-block | DONE | F-10 (mastery banked, block position lost) |
| 38 | Force-stop right after a save | DONE | DB rows matched the UI after every force-stop in the session |
| 39 | Home / background during a task | DONE (partial) | the app was backgrounded and re-entered repeatedly (share sheet, Chrome, launcher) with no crash; **NOT COVERED**: a deliberate HOME-press *during* narration with a subsequent TTS assertion (audio not observable) |
| 40 | Sleep invitation (cap timer, "a few more" re-arm) | DONE | `rev_sleep_invite.png` (invitation appears at the 5-minute cap), `rev_sleep_no_rearm.png` (no second invitation after another full interval) — see F-09 |
| 41 | Airplane mode mid-play | NOT COVERED | the emulator's radio state was not toggled; the app makes no network calls, so the only expected effect would be a TTS-availability change, which is not observable here |
| 41 | Rotation / configuration change | N/A | `AndroidManifest.xml:16` — `android:screenOrientation="portrait"` (locked) |
| 42 | Sticker boundary blocks 29/30/31 | NOT COVERED | reached only by grinding 30 blocks; the arithmetic behind the wrap is proven in code (F-02) and the duplicate case was reached by seeding |
| 43 | Process-death resume of a *partly solved* block | DONE | same as #37 (F-10) |
| 44 | Rapid double/tap storms on every button | DONE (partial) | `rev_gate_dbl.png` (gate Check), `rev_dbl2.png` (answer options), Market Day apples tapped in bursts, meadow cards tapped in bursts — no crash, no double scoring observed; **NOT COVERED**: triple-tap on "Keep exploring" and on the album/share button |
| 45 | Choreographer/GC/jank metrics | NOT COVERED | no "Skipped frames" warnings attributable to the app process in the whole logcat capture (204 k lines) — i.e. no jank signal was produced to measure |
| 46 | Fresh install end-to-end (Phase 2) | DONE | `pm clear` → onboarding → 5 blocks → Market Day → sticker → `db_p2b.db` |
| 47 | Uncaught exceptions for the whole session | DONE | `review_logcat_full.txt` (370 331 lines): no `ANR in`, no `StrictMode`, no app-process crash **except one** (F-22, `app_crash_211124.txt` — an unguarded Room write in `persistSnapshot()`, not reproduced on a deliberate retry). The other `FATAL EXCEPTION` records are the `uiautomator` CLI aborting when two of my own dumps overlapped ("UiAutomationService … already registered!"); the app-tagged lines are otherwise only ART "failed lock verification" warnings from a debug build |
| 48 | Device state restored at the end | DONE | `ds_restored.bin` (narration 1, sound 1, volume 60, voice 9, cap 12, bias 0, onboarded 1), DB `('village','0:84;1:82;2:80;3:44','4,9,24',64,64)`, `rev_restored2.png`; emulator left running |

---

## 4. Verified clean (no defect found)

1. **No ANRs, no StrictMode violations, and a single uncaught exception in the app process** across 370 331 logcat lines and ~120 puzzle interactions, a wiped-data first run, forced kills, a purchase attempt and a share flow. The one app crash is F-22 (an unguarded background Room write); the only other `FATAL EXCEPTION` records in the capture belong to the `uiautomator` CLI — "UiAutomationService … already registered!" — because two of my own dumps ran concurrently.
2. **Grown-up settings persistence (3-way):** narration, sound, sound level, voice pace and sleep cap all round-trip UI → DataStore → restart; extremes confirmed (cap 5 and 60, voice 5 and 15) and restored.
3. **DataStore integrity:** the file is a valid protobuf; every pull parses cleanly and shows only the seven known keys; no corrupt writes, no duplicated keys.
4. **Gate input robustness:** empty input, letters, a 12-digit number, a negative number, leading zeros and a double tap on Check all behave (no crash; leading zeros accepted; one clean unlock).
5. **Back navigation on HEAD:** from a meadow task the hardware back returns to the village map (an earlier contrary observation came from a stale binary and is explicitly excluded).
6. **Compare redesign:** "Tap the group with more/fewer" works, with the two drawn groups as the answer targets.
7. **Answer options:** exactly four distinct choices, the correct value always present, never two correct options (checked on ~30 generated tasks).
8. **Progress persistence:** hearts/blocks/mastery in the database always matched the UI after every restart, force-stop and fresh install.
9. **Fresh first run:** three onboarding pages, `onboarded=1` persisted, empty database row created (`('village','','',0,0)`), zero-grown-up defaults.
10. **Fresh sticker grant:** on a wiped install the Market Day award really lands (`stickersCsv='4'`, album "1 of 30") — the grant path itself is sound; only the duplicate case is broken (F-02).
11. **Share flow:** the system chooser opens, the payload matches the database, cancelling returns to the app with no crash.
12. **Purchase button:** fails honestly ("Google Play isn't reachable here") rather than silently or with a crash.
13. **Wrong-answer path:** friendly face plus a kind sentence, no scolding, no sound sting, no progress loss beyond the mastery point (F-11).
14. **Six-dot block progress:** visible, fills one dot per solved task, resets on the next block.
15. **Market Day mechanics:** the basket counts up, the "Ready!" state is correct, "Wait — start again" resets the basket.
16. **Uninstalled/reinstalled build parity:** reinstalling HEAD with `-r` preserved all data (hearts/blocks/mastery/stickers unchanged) — see the "stale binary" note in F-13/F-16 methodology.

---

## 5. Method notes & caveats (read before acting on this report)

- **Build integrity:** the emulator rollback restored an **older installed APK**; every observation gathered after that point was re-verified against a freshly built and reinstalled HEAD build (`assembleDebug` → `install -r`, data preserved). Findings that were *not* re-verified after the reinstall are marked as such, and one early observation (hardware back exiting the app) was **discarded** because it was an artefact of the stale binary.
- **Two emulator instances:** near the end of the session the emulator was re-created (adb serial `emulator-5554` → `emulator-5556`), which rolled the app's data back to the device's original snapshot (`hearts 2 · puzzles 5`). The final state was therefore re-seeded and re-verified on the live instance (`ds_finalstate.bin`, `db_verify.db`). The single app crash (F-22) occurred in that re-created instance, in the window where the database file had just been replaced under the running process — which is why it is reported as a robustness defect with an explicit "not reproduced under normal use" caveat rather than as a confirmed user-reachable crash.
- **Evidence discipline:** database state is always read from a WAL-complete pull (`numberlings.db` + `-wal` + `-shm`); DataStore is read binary-safely (`adb exec-out` + byte-exact redirect) and parsed as a protobuf. Two of my own tooling errors (a PowerShell text-redirect that mangled `0x0A` into `0x0D 0x0A`, and an off-by-lookahead in the protobuf parser) initially looked like app bugs and were root-caused as tooling faults — neither is reported as a finding.
- **Not observable from `adb`:** `ToneGenerator` output and TTS speech itself. Findings F-03 and F-08 are therefore marked code-level for the audio half; their persistence halves are live-verified.
- **Severity is calibrated to this product:** a pre-reader maths game. A puzzle that cannot be solved by the method the app teaches is Critical; a promise the app cannot keep is Major; friction and inconsistency are Minor/Polish.
- **No app source was modified** during this review; only `REVIEW.md` is added to the repository.
