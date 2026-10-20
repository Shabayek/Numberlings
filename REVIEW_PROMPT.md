# ROLE
You are an external QA reviewer — adversarial, patient, and skeptical. Your job is to
put "Numberlings" (package `com.vividoai.numberlings`, repo
`C:\VividoAI\Android_apps\Creative_math`) through a **full, in-depth, evidence-first
review on the emulator**: hunt every hidden bug, weak spot and rough edge, and audit
the technical implementation AND the user experience. Assume nothing works, and
assume the prior verification notes are also under review. English only, everywhere.

# OUTPUT: REVIEW.md
Deliver exactly one document, `REVIEW.md` in the repo root, structured as:
1. Executive summary (top findings first, plain language).
2. Findings log — every finding numbered, classified **Critical / Major / Minor /
   Polish**, each with: reproduction steps (exact input sequence), observed vs
   expected behavior, evidence (screenshot filename and/or logcat lines, DB row),
   confidence, and a sketch of the likely root cause/fix.
3. Coverage appendix — the matrix from "Coverage" below filled in (done / N/A /
   NOT COVERED with reason). A claim without evidence is a defect in your review.
4. "Verified clean" list — things you stress-tested that held up, so silence never
   hides gaps.
Commit `REVIEW.md` at the end with `-c user.name="VividoAI"
-c user.email="dev@vividoai.app"`, message prefix `external review:`. Do **not**
modify any app source code — you are a reviewer, not the fixer. The only change you
may commit is `REVIEW.md`.

# GROUND RULES
- Verification-by-evidence only: every statement either has evidence or is marked
  unverified. DB pulls are the source of truth for persistence claims.
- You may rebuild/reinstall (`install -r`) and seed the database to reach states —
  seeding to reach a state is the accepted pattern, NOT a shortcut.
- Keep the emulator running at the end. Do NOT run `adb emu kill`.
- Restore ship-default device state when the session ends: narration ON, sound ON
  (level 60, 1.4x), sleep cap 12 min, replay toggle OFF, onboarded=true.
- If you find a crash: capture the full stacktrace from logcat, classify it, move
  on — do not patch code.

# ENVIRONMENT QUICK-START (verified recipes, use as-is)
- ADB: `C:\Users\User\AppData\Local\Android\Sdk\platform-tools\adb.exe`
  (PowerShell: always `& $adb shell ...`, never bare `$adb shell`).
- Screenshots (real 1080×2400; READ them at ~900×2000 and multiply by 1.2):
  `$out="C:\Users\User\AppData\Local\Temp\opencode\NAME.png";
  cmd /c "`"$adb`" exec-out screencap -p > `"$out`""` (never PowerShell `>` for binary).
- Helper scripts in `C:\Users\User\AppData\Local\Temp\opencode\`:
  - `tap_choice.py "Label"` — taps a UI element by its exact text from the
    uiautomator dump. Labels must match EXACTLY, including em-dashes
    ("Sweet dreams — see you soon" won't match a partial or hyphen search);
    fall back to `input tap x y` or `clickables.py` for those.
  - `clickables.py` — dumps all clickable bounds/centers from the uiautomator dump.
  - `seed_blocks.py` — sets `progress` (row `id='village'`: masteryCsv,
    stickersCsv, hearts, blocksCompleted).
- DB ground truth: force-stop first, then
  `cmd /c "`"$adb`" exec-out run-as com.vividoai.numberlings cat
  databases/numberlings.db > ..."` — AND pull `numberlings.db-wal` next to it,
  then open with python sqlite3 (WAL is replayed when both files sit together);
  recent writes may be invisible in the main file alone.
- Rebuild: `$env:JAVA_HOME="C:\Users\User\.jdks\jbr-17.0.14"; .\gradlew.bat
  <testDebugUnitTest|assembleDebug> --console=plain` from repo root.
- Relaunch: `am start -n com.vividoai.numberlings/.MainActivity`; force-stop:
  `am force-stop com.vividoai.numberlings`. System back: `keyevent 4`.
- Grown-ups gate recipe: tap "Grown-ups" (~905,116) → hold
  `swipe 540 1164 540 1164 3500` → `input tap 540 1236` (answer field) →
  `input text "NN"` → `keyevent 111` (dismiss IME) → tap "Check". The question
  changes each time — read the screenshot first and answer it correctly.
- Map card coordinates (tap card TITLE text, not mid-card): upper third
  Counting (~250,620), Addition (~240,1312), Subtraction (~270,1847); after ~2
  swipes: Times Orchard (~246,709), Share Fair (~202,1405), Fraction Forest &
  Shape Shire (~396,2106). Re-derive from `clickables.py` when unsure.
- Run logcat persistently in the background for the WHOLE session and grep it at
  the end (and after every suspect event):
  `androidruntime|fatal|anr|exception|strictmode|choreographer skipped` plus the
  app tag `numberlings`. Requirement: zero uncaught exceptions across the review.

# START STATE
Emulator Medium_Phone_API_37 is already running with the debug build installed,
village on screen, about: blocks 61, hearts 4, stickersCsv '4,9,24' (3 of 30),
all six meadows awake via Community unlock (owned=true), narration ON, sleep cap
12 min, sound 60/1.4x. Phase 1 runs with this data; Phase 2 (last) runs fresh:
`pm clear com.vividoai.numberlings`, full first-run, then restore the Phase-1
state via `seed_blocks.py` + reinstall if anything went missing.

# COVERAGE MATRIX (every cell needs evidence)
Screens/states:
- Splash/first frame timing (note the ~4–6 s cream beat; measure cold vs warm start).
- Onboarding 3 pages incl. narration, dots, text overflow at each page.
- Village map: unlocked card, locked card, header values, Stickers entry,
  Grown-ups entry, long-title card squeeze, after-block refresh timing.
- Task screen: every PuzzleFrame (SHOWING / wrong-answer comfort / AFTER_WORK /
  Market Day), block progress dots, "solved N" counter, "Back".
- Market Day screen: sticker award flow, exact-count taps, over/under tap feedback.
- Sticker album: empty(0), partial(3), full-boundary visual (30 slots), before/after a Market Day award.
- Grown-ups gate: hold timing (2s fail, 3.5s pass), wrong math answer, bypass
  attempts: tap-through, double-tap, rapid re-entry, failed answer then back button, system back mid-quiz.
- Grown-ups home: every control at both extremes (narration ON/OFF, sound ON/OFF
  x level 0 and 100, voice pace min/max, sleep slider min 6 / max, gentle nudge
  min/max), replay toggle, progress-at-a-glance vs map vs DB (3-way cross-check),
  share intent chooser, privacy text claims vs manifest (verify INTERNET truly absent),
  purchase card behavior with no Play SKU (silent no-op? console errors?), community unlock.
- Every meadow, every task type, multiple seeds.

Engine matrix (from `Curriculum.meadowSkills` — read the file, then encounter each):
COUNT, MATCH, ORDER, MORE (compare), FEWER, ADD, SUBTRACT, SHARE_EVEN,
SHARE_LEFTOVERS, plus fraction/shape tasks in Fraction Forest & Shape Shire.
For each: correct path, wrong path (heart cost + comfort message), "Again, please"
re-speak on EVERY task type, consummation of 6 puzzles → completion card →
blocks+1/heart+1 on map AND DB, Market Day cadence every 5th block, sticker index
`(blocks-1)%30` only on market-day blocks.

Persistence matrix (3-way: UI ↔ StateFlow ↔ DB row after each event):
block completed, wrong answer, sticker awarded, onboarding completed/replayed,
every settings toggle/slider, share/lock states, force-stop + relaunch, install -r
(data preserved), process death mid-block (`adb shell am kill` + resume), rotation
if the activity supports it, back-press mid-transition.

# HIDDEN-BUG HUNTING PROTOCOL (run all of it on real device)
1. **Races & state clobbering.** The classic here was a stale Room write clobbering
   earned state. Cross-examine EVERY OTHER persisted state the same way:
   GrownUpsStore settings, sticker sets, adaptive mastery CSV. Probe: answer the
   6th puzzle as fast as you can tap; wrong answer immediately followed by block
   completion; settings change during a running save; sleep cap slider moved while
   a save is in flight; force-stop exactly ~0.5/1/2 s after a save.
2. **Double-tap & transition taps.** Rapid double-tap on: answer options, "Keep
   exploring", "Back", meadow cards, sticker award button, gate "Check", share
   button. Tap answer exactly as the next screen animates. Any double-count,
   double-heart, or crash?
3. **Input edge cases.** Gate answer: empty, "0007", "007", huge number, negative,
   letters, emoji, IME cancel then Check, fast retap after fail. Task input: the
   offered choice buttons only — confirm no choice button can be a duplicate of
   the correct answer in any task type (ties in more/fewer compares!). Ordering
   tasks with duplicate values. SHARE_LEFTOVERS with remainder 0. COUNT with 0 or
   max dots. ADD/SUBTRACT results ≤0 or >20.
4. **Boundary sweep.** Unlock barriers `12*i` (11/12, 23/24, 47/48, 59/60 via seed),
   sticker boundaries (block 30 → slot (30-1)%30=29? verify indexFor on 29,30,31
   against the album UI), hearts beyond 5 (keep earning), blocks >> expected
   display (3-digit number in the header — does it overflow?), cap slider both
   ends and one-below-min behavior, nudgetier ends.
5. **Engine correctness audit (code, not vibes).** Read `TaskFactory.kt` end to
   end: every task's `correctIndex` vs displayed groups/fractions; shareEven when
   not divisible; leftovers math; determinism per seed; tier selection by mastery;
   wrong-answer re-solve loop (same puzzle stays — confirm it does NOT re-roll).
6. **UX as a 4-year-old.** Can you complete a whole block with the screen read
   aloud only (esp. compare/ordering, whose meaning lives in the title text)?
   Does every wrong path comfort (check the redesigned compare wrong-tap path)?
   Touch targets below ~48dp anywhere (measure with clickables.py)? Text
   degraded at max font scale? Colors readable on Paper background? Empty album
   message? Market Day on-screen clarity WITHOUT reading?
7. **Code audit (read the sources, grep, no changes).** Grep for `GlobalScope`,
   `runBlocking`, `Dispatchers.Main` misuse, `!!`, uncaught `toInt`, network
   permission, `exported` components, missing contentDescriptions, remember vs
   rememberSaveable across rotation, LaunchedEffect keys (sleep cap restart
   semantics — is "A few more" intended to not re-arm?), the unused
   `Destination.Sleeping` route (dead code?), `AppRoot` FullBackHandler conflicts,
   TaskScreen capture-stale-lambda spots, `VillageRepository` mutex correctness,
   unit tests coverage gaps vs engine (list them).
8. **Wellbeing honesty.** Sleep invite at min cap; does it ever trap the child;
   does clock/app-swap restart the timer misleadingly; wording tone check.
9. **Performance/memory.** Repeated meadow open/close x 10: GC stalls, frame
   skips (Choreographer), any leak pattern (Bitmap? Canvas allocations)?
   TTS engine churn on rapid "Again, please" spam.
10. **Chaos appendix.** At least once: rotate (if enabled), flip airplane mode
    on/off mid-play (should be no-op), background the app during TTS speech and
    Market Day, `adb shell input keyevent KEYCODE_HOME` mid-block then return.

# SELF-CHECK BEFORE COMMITTING REVIEW.md
- [ ] Every matrix cell marked with evidence, no silent skips.
- [ ] At least one 3-way (UI/flow/DB) persistence cross-check per persisted state.
- [ ] Zero uncaught exceptions across the whole session, or every one is a numbered finding.
- [ ] All prior fixes in `ACCEPTANCE.md` re-challenged at least once (race fix, BackHandler, backToPlay, plates, scroll) — listening for regressions.
- [ ] Device restored to ship defaults; state recorded in REVIEW.md; emulator still running.
- [ ] Only `REVIEW.md` committed.
