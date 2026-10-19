# Numberlings — design & ethics log

A cozy Android game that wakes up a village of little creatures by practising real
math. Built for children roughly 4–8, playable before reading, and monetised only
through a single parent-gated purchase.

## Teaching map

| Meadow | Skills | Curriculum aim (≈K–Grade 3) |
| --- | --- | --- |
| Counting Meadow | Count, Compare | counting 3–20, comparing groups |
| Addition Field | Join, Missing addend | sums within 8 → 20, missing addend |
| Subtraction Brook | Take away, Difference | subtraction within 6 → 14 |
| Times Orchard | Skip count, Groups | 2×,5×,10×, then 3×,4× as arrays |
| Share Fair | Even share, Leftovers | fair sharing, remainders |
| Fraction Forest & Shape Shire | Pizza parts, Shape names | halves/quarters, shape recognition |

## Adaptive engine
- `SkillScore` keeps a warm mastery value per skill (0→1).
- `AdaptiveState.nextSkill` serves the **weakest unlocked skill**, skipping the one just
  answered, so nothing repeats like a metronome and hidden review stays alive.
- Tiers (0–2) grow with mastery; a grown-up bias knob can simplify for accessibility
  — never as a punishment (see `SkillScore.tier`).
- Wrong answers cost nothing: same puzzle stays, "Again, please" re-speaks it.

## Monetization (the sole line)
- **One-time** non-consumable unlock of meadows 3–6 (`UnlockStore.PRODUCT_ID`).
- Lives only inside a Grown-ups gate (3-second hold + mental-arithmetic check).
- Billing wiring flags on at launch: see `UnlockStore.billingConfigured` + STORE_READY.md.
- *Community unlock* — a no-questions-asked switch in the same zone — is part of the
  product, not a promo.

## Ethics decisions (kept on record because rules can be forgotten faster than code)
1. **No INTERNET permission.** Nothing is collected, nothing is shared; progress is a
   single local Room table plus DataStore preferences.
2. **No ads, even in the Grown-ups zone** — revenue is never allowed behind the gate in
   a form that would tempt the developer to open it wider.
3. **No randomized rewards.** Every celebration is effort. No loot boxes at any tier.
4. **No "read to play" walls:** TTS (system, offline) reads everything; children with no
   TTS voice can still play, since voice is an enhancement, not a gate.
5. **Sleep invitation, not a lockout:** 12 minutes on by default, dismissed by one tap.
6. **The purchase button cannot lie if Play Billing is broken** — it says plainly the
   store isn't reachable and points at the Community Unlock instead of retry loops.
7. **Seed-based randomness** keeps puzzles reproducible (playtests, reviews, QA), and
   since only difficulty is random (never rewards), there is no "gacha" pressure loop.

## Build & run
1. Open this folder in **Android Studio Ladybug (2024.2.1) or newer** (it provides JDK 17
   + Android SDK automatically), or run with a local Gradle ≥ 8.9 + JDK 17 + Android SDK 35.
2. First invocation may need the Gradle wrapper bootstrap chore (see "Wrapper" below).
3. Run the unit tests: `gradle test`.

## Wrapper
This repo ships `gradle-wrapper.jar` (see note) and `gradlew.bat`/`gradlew`. If your
clone lost the jar, run once with any local Gradle ≥ 8.9:

```
gradle wrapper --gradle-version 8.10.2
```

Or let Android Studio repair the wrapper at first Sync (Tools → Gradle → *Sync Project*).
