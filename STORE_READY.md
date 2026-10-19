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
> Questions: hello@numberlings.app

## Launch checklist
1. Create in-product item `numberlings_meadows_all` (one-time, non-consumable).
2. Set `UnlockStore.billingConfigured = true` after price review.
3. Ship signed release build (see README build section for the keystore reminder).
4. Verify Data Safety answers + Families declaration in Play Console.
5. Confirm "sleep invitation" setting reaches the play screen on a fresh install.
