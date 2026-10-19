# ASSETS.md — art & audio inventory (shipping now → what a studio pass adds)

Everything in the current build is drawn with Compose Canvas (vector-only) and
synthesised sound tokens, so **the APK is intentionally tiny and licence-clean**.

## Already shipping (no files needed)
| Thing | Where it lives |
| --- | --- |
| Puzzle objects, group arrays, plates, pizzas, shapes, hop lines | `ui/puzzle/PuzzleArt.kt` |
| Sound taps / successful / think-again cues | `core/audio/SoundCue.kt` (built-in tone synthesis) |
| All narration | system text-to-speech, offline; no voice files |
| Launcher icon (placeholder) | `res/drawable/app_icon.xml` |

## Next pass (hand or studio art to track)
1. **Numberling characters** — creatures that live in each meadow: bodies + eyes +
   tiny expressions. Suggested: keep them vector; export as `res/drawable/numberling_*.xml`.
2. **Meadow backgrounds** — soft gradient hills per meadow, one SVG-style vector each.
3. **Celebration sparkles** — particle sheet, purely decorative.
4. **Bonus gentle melody** — one short loop for the Grown-ups zone only; never autoplay
   in child screens, per the no-dark-patterns charter.
5. **Sticker / badge art** — 30 deterministic designs (5 per meadow).

## Hard rules for any art added later
- No rasterised text baked into images (translation + accessibility).
- No timers, no "scare faces", no consumer pressure (BUSY-style FOMO banners).
- Any font must be bundled as an offline file or use system sans.
