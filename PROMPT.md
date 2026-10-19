# Build "Numberlings" — a cozy, ethical Android math game for kids

## Role
You are an experienced Android game developer and children's learning designer. Implement, from scratch, a complete and shippable Android app described below. Optimize for a warm, safe, delightful experience — not engagement-maximization.

## One-liner
A cozy village of little creatures (the **Numberlings**) has gone quiet. Kids wake it back up by solving playful math puzzles across six themed meadows — every solved puzzle visibly feeds a Numberling, mends a bridge, or grows the village. **No timers, no lives, no ads, no guilt.**

## Target users
- Primary: ages 5–8; secondary: 4 and 9–10.
- Must be playable by **pre-readers** (voice narration + iconography for everything a child needs).
- Parents/caregivers get a separate "Grown-ups" zone, visibly distinct in tone.

## Non-negotiable ethical charter (overrides all other requirements)
Priority stack — if a feature violates a higher tier, cut it:
1. Child wellbeing → 2. Privacy & legal compliance → 3. Learning quality → 4. Revenue → 5. Feature ambition.

Hard rules:
- **COPPA, GDPR-K, and Google Play Families policy compliant by design.**
- **No ads. Ever.** No ad SDKs, no third-party analytics that transmit data.
- **No loot boxes or randomized rewards** — every reward is deterministic and visible.
- **All purchases behind a parental gate** (hold 3s + a written task a small child can't pass). One-time unlock only: no consumables, no subscriptions, no battle passes. No urgency or scarcity copy.
- **No accounts, no social features, no chat, no UGC sharing.** Progress stored on-device only.
- **No dark patterns:** no streak-loss punishment, no FOMO timers, no "come back or lose progress," no nagging purchase prompts.
- Any time a rule and revenue conflict, the rule wins — and note the decision in README "Ethics decisions."

## Core loop
1. A Numberling states a concrete need ("My bread is done — how much flour did I use?").
2. Kid completes a 4–6 task puzzle block (tap, drag, compare, build numbers).
3. Immediate visual payoff: village builds, garden blooms, creature celebration — always earned by effort, never by payment.
4. After 8–10 blocks, an optional "Market Day" mini-game grants a sticker for the album.

## Six meadows = curriculum map (roughly K–Grade 3)
1. **Count & Sense** — counting 1–20, subitizing, compare & order.
2. **Add Meadow** — addition within 10 → 20, visual models (blocks, number line), missing addend.
3. **Subtract Strand** — subtraction within 20, take-away and difference stories.
4. **Times Orchard** — skip counting, then ×2, ×5, ×10, then ×3, ×4 via groups/arrays.
5. **Share Fair** — division as fair sharing, remainders as "leftovers."
6. **Fraction Forest & Shape Shire** — halves/quarters, shape recognition, simple partitions.

Every meadow has 30–60 generated tasks, tiered by difficulty; the world unlocks meadow-by-meadow, and each is fully playable end-to-end once unlocked.

## Learning design
- **Adaptive engine:** per-skill mastery tracking; serve tasks in the learner's zone (~70–85% expected success); never repeat the exact failed task immediately; mix in succeeded skills to spiral practice.
- **Hint ladder per task:** nudge → visual scaffold → worked example. Free stems, never shaming.
- **Wrong answers cost nothing:** warm, specific feedback ("Almost — this group has one more"), no red X scores, no "game over" state.
- **Growth-mindset copy rules:** praise effort and strategy, not speed or IQ; never compare kids to others.

## Wellbeing
- No reading required through Meadow 2 (full voice-over).
- No timers anywhere; latency of a child's thinking is respected, never punished.
- Around 12 minutes of play, the village gently "goes to sleep" — a soft invitation to finish, never a lockout.
- Returning after days away: the village welcomes, never guilt-trips, never resets, never punishes.

## Monetization (ethically capped)
- **Free tier:** Meadows 1–2 fully complete and unlimited — a real product, not a demo.
- **One-time "Unlock all meadows" IAP**, plainly priced and described; at most one gentle, non-interrupting mention per session; purchasable only in the Grown-ups zone.
- **Community unlock:** a no-questions-asked free unlock switch in the Grown-ups zone for families who can't pay.
- Purchases restore across reinstalls where technically possible (Play billing "restore"), and the app stays fully functional offline and without ever purchasing.

## Grown-ups zone
Behind the parental gate only: per-skill progress summary, difficulty nudge, session-length cap toggle, sound/music/volume, read-aloud speed, purchase + restore, plain-language privacy summary, and progress export (CSV, local share intent).

## Privacy (hard requirements)
- **No INTERNET permission.** No Play Billing except the single IAP; no other third-party SDKs.
- Progress in a local Room database; no telemetry; no identifiers; Data Safety form answers: "no data collected, no data shared."
- Kids' screens contain no text-only affordances they must read to navigate.

## Technical requirements
- Kotlin + Jetpack Compose, Material 3 with a custom cozy theme; single Activity.
- minSdk 24, target latest stable; runs well on low-end devices; APK ≤ ~25 MB (vector art; list any needed raster assets separately).
- Tasks generated procedurally from parameterized skill definitions (seeded, deterministic, anti-frustration filters).
- Unit tests for the math engine, adaptivity, and parental gate; Compose UI tests for the gate and purchase flow.
- i18n-ready strings from day one (English first); web-safe offline fonts only.
- Modular feature packages so new meadows can be added later without refactors.

## Deliverables
1. Complete Android Studio project that builds clean (Gradle wrapper included) with instructions.
2. `README.md`: design doc — pillars, curriculum map, adaptivity model, monetization rationale, "Ethics decisions" log.
3. `ASSETS.md`: art/audio needed, with placeholders created (vector/programmatic) so the app looks finished even before custom art.
4. Play Store readiness: listing copy, Families-policy checklist, Data Safety form entries, and a plain-language parent-facing privacy policy draft.
5. Verified acceptance checklist, including: every task reachable without reading (M1–M2); every purchase gated; no randomized rewards; no timers; wrong answers never penalize; airplane-mode full functionality; all tests green.

## If anything is ambiguous
Choose the more protective, more honest option for children, document it, and keep building. Start by producing the design doc and project skeleton, then implement feature by feature, testing as you go.
