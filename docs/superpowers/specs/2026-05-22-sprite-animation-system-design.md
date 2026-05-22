# Sprite animation system — design

**Date:** 2026-05-22
**Status:** Draft (brainstorm complete, awaiting implementation plan)
**Scope:** Unified design covering combat *and* all 13 skill surfaces. Implementation ships in slices (see §7).

## Context

311 AI-generated sprites have landed in `app/src/main/res/drawable-*dpi/` across 33 categories (PR #17). Today they show up in `ItemTile`/`DungeonCard`/`RecipeCard` static rows via `EntityIcon`, but nothing in the game is *animated* with them. Active sessions render as text-heavy hero cards — combat shows HP bars and a damage log; skill actions show a countdown. The sprites sit unused during the moments that matter most.

The user's vision is concrete: **combat should play out as an automated Pokémon-style battle** the player can watch; **skill actions should show the tool acting on the resource** (pickaxe hits silver geode → silver ore pops out; fishing rod wobbles → fish pops out). This spec defines a single, unified animation system that delivers both.

## Decisions made during brainstorm

| Decision | Choice | Notes |
|---|---|---|
| Spec scope | Unified — combat + all skills in one design | Implementation slices independently |
| Placement | Tap-to-expand `ChunkySheet` from the existing session banner | Banner text stays; sheet hosts the stage |
| Fidelity | Hybrid — property animation baseline + ~20 sprite sheets for hero moments | No new sprites blocking v1 |
| Architecture | Stage + Layer + Effect + SceneEventBus (a small composable scene engine) | One catalog file, one config per surface |
| Tick source — combat | Wrap the existing 500ms decode loop in `CombatSceneAdapter` | Zero simulator change |
| Tick source — skills | Synthesize N evenly-spaced events client-side from per-minute frames | Zero simulator change |

## Goals

- Make active sessions feel **alive** without making them gate gameplay (you can still ignore the sheet).
- Reuse the existing sprite library; new sprite work is opt-in via the hero registry.
- Land in vertical slices, starting with Mining (CLAUDE.md's endorsed first slice).
- Zero refactors of `CombatSimulator` or `SkillSimulator` — the spec is purely presentation-layer.

## Non-goals

- Sound design / SFX selection. `SfxBridge` is a stub; real audio is its own future spec.
- Authoring the sprite sheets themselves. The spec defines the file contract; the art pipeline grows a `sheet_export` stage separately.
- Replacing the existing text banner. Stats stay visible; the sheet is additive.
- A sprite-animation editor or per-entity tuning UI. Content lives in the scene catalog by design.
- Interactive timing-game mechanics — animations are watchable, not playable.

---

## 1. Core primitives

Four pieces in a new package, `app/src/main/kotlin/com/fantasyidler/ui/scene/`:

### `Stage`
A Compose `Box` that hosts N positioned children, sized for the tap-to-expand sheet (~280–320dp tall). One stage per surface (`MiningStage`, `CombatStage`, …) is a thin wrapper that supplies a `SceneConfig`. The Stage subscribes to its surface's `SceneEventBus` and translates events into effect compositions on targeted layers.

### `Layer`
A single visual element on the stage. Carries:
- a **renderable**: either `EntityIcon(entityId)` (static + property animation) or `SpriteSheetIcon(entityId)` (Coil frame loop). Chosen at draw time by `HeroSpriteRegistry` — same call site, different renderer.
- a **position**: a relative anchor (`LEFT_GROUND`, `RIGHT_GROUND`, `CENTER_HOLD`, `OFF_TOP`, etc.) so scenes read declaratively.
- an **idle behavior**: `None`, `Bob`, `Breath`, `Swing`, `Wobble`, or `Drift`. Loops via `FantasyMotion.Idle` whenever no event animation is active on the layer.
- a **tag**: `String` (e.g., `"player"`, `"enemy"`, `"tool"`, `"target"`, `"output"`) that `Effect`s target.

### `Effect`
A transient overlay triggered by an event. Six baseline effects cover virtually everything in the catalog:

| Effect | Drives |
|---|---|
| `HitFlash(tag)` | brief brightness/hue flip on a layer |
| `Shake(tag, magnitude)` | translational jitter |
| `DamagePopup(tag, amount, color)` | floating "+N" text drifting up + fading |
| `ArcOut(fromTag, toTag, entityId)` | spawn an entity sprite at `from` and arc it to `to` (or off-screen) |
| `Burst(tag, count)` | radial particle pop |
| `Glow(tag, color, durationMs)` | a colored halo behind the layer |

Each effect consumes a `FantasyMotion` spec (`Snappy`/`Bouncy`/`Smooth`). No inline `tween(...)` anywhere in the scene code.

### `SceneEventBus`
A `MutableSharedFlow<SceneEvent>` per active session. Event types:

- `HIT(attacker: String, target: String, amount: Int)`
- `DODGE(target: String)`
- `PRODUCE(item: String, fromTag: String)`
- `LEVEL_UP(skill: String)`
- `ATTEMPT(toolTag: String)` — used by `SkillSceneAdapter` for "swing but no item this tick"
- `SESSION_END(outcome: SessionOutcome)`

The Stage's `SceneConfig` carries a `Map<SceneEvent.Type, List<EffectFactory>>` that produces the actual effects when an event arrives. This mapping is what makes combat react to `HIT` differently than mining reacts to `PRODUCE` — same engine, different config.

The whole engine is ~400 lines and depends only on Compose + the existing `FantasyMotion` vocabulary. No game-loop framework, no global state, no DI changes.

## 2. Data model — how events get into the bus

Two adapters bridge session/frame data to `SceneEvent`s. They are the only pieces that know about session shape; everything downstream is data-format-agnostic.

### `CombatSceneAdapter` (zero simulator change)
`CombatSessionBanner` already decodes 60 frames × 25 ticks and steps `tickInFrame` every 2400ms. We wrap the existing decode loop:

- For each `tickInFrame` advance, if `playerHits[tick] > 0` → emit `HIT(attacker="player", target="enemy", amount=playerHits[tick])`. Mirror for enemy.
- If both are 0 → emit `DODGE(target="enemy")` (visual breather; cheap variety).
- For each entry in `frame.items[]` → emit `PRODUCE(item, fromTag="enemy")`.
- On the existing completion callback → emit `SESSION_END(outcome)`.
- When `frame.hpAfter` drives an enemy to 0 → emit a derived `ENEMY_DEFEATED` (handled as a stage-level layer swap, not a generic effect).

~40 lines in `ui/scene/adapter/CombatSceneAdapter.kt`. Bolts onto the existing banner without touching `CombatSimulator`.

### `SkillSceneAdapter` (synthesized ticks)
Skill simulator emits one frame per minute with `items: List<Drop>` and `xpGain`. The adapter synthesizes per-tick events client-side:

- At the start of each minute-frame, count `N = items.size`, compute `intervalMs = 60_000 / max(N, 1)`.
- Schedule N `PRODUCE` events at `t0 + 0, t0 + intervalMs, t0 + 2*intervalMs, …` via a coroutine `delay` loop tied to the sheet's lifecycle.
- If `N == 0` (low level / unlucky minute), emit one `ATTEMPT("tool")` mid-frame so the stage doesn't look frozen.
- If `frame.leveledUp` → append `LEVEL_UP(skill)` near frame end.
- On session completion → `SESSION_END`.

A 5-minute mining run producing 8 ores per minute → 40 evenly-paced `PRODUCE` events. Zero memory at rest. Zero simulator change.

### Why synthesize instead of refactoring the simulator
The pre-simulated 60-frame model is load-bearing for offline session completion (the `SessionCompletionWorker` computes a 60-minute outcome upfront and persists it). Adding per-tick granularity to the simulator would bloat persisted session data and the offline code path. Synthesizing on the client is invisible to anything that consumes session frames today.

### Edge cases
| Case | Behavior |
|---|---|
| Session paused / sheet closed | Adapter pauses event emission. Stage's idle behaviors keep ticking — they're pure Compose animations driven by the composition clock. |
| Sheet reopened mid-session | Adapter resumes from current wall-clock tick. **No backlog replay** — it would feel wrong to see 40 catch-up pops. |
| App backgrounded | Underlying session keeps simulating (timestamps + pre-computed frames). When app foregrounds, Compose recomposition picks up at the current wall-clock position. |
| Reduce-motion enabled | Adapter still emits events; Stage downgrades how it visualizes them (see §4). |

## 3. Scene catalog

14 surfaces (1 combat + 13 skills) collapse into **six scene archetypes**. Adding a future skill is a 10–15 line `SceneConfig` declaration, not new composables.

### Archetype 1 — Duel (Combat)
- **Layers**: `player` (LEFT_GROUND, idle `Breath`), `enemy` (RIGHT_GROUND, idle `Bob`). Background: the existing dungeon sprite from `dungeons/*.png`.
- **Event map**:
  - `HIT(attacker="player")` → `Shake("enemy", small)` + `HitFlash("enemy")` + `DamagePopup("enemy", amount, red)`
  - `HIT(attacker="enemy")` → mirror on `"player"`
  - `DODGE(target)` → small `Drift` sidestep on the target layer
  - `PRODUCE(item)` → `ArcOut("enemy" → off-screen-bottom, entityId=item)`
  - enemy defeated → layer swap to next encounter's entityId + `Burst("enemy", 12)`

### Archetype 2 — Tool + node (Mining, Woodcutting, Fishing)
- **Layers**: `tool` (RIGHT_ACTOR, idle `Swing` for mining/woodcutting, `Wobble` for fishing), `target` (CENTER_HOLD, no idle), `output` (hidden until PRODUCE).
- **Event map**: `PRODUCE(item)` → `Shake("target", small)` + `Burst("target", 6)` + spawn `output` layer with `entityId=item` then `ArcOut("output" → off-screen-top)` to suggest inventory pickup.
- Sub-scenes differ only in entity IDs and idle behavior.

### Archetype 3 — Transform (Firemaking, Prayer, Runecrafting)
- **Layers**: `material` (CENTER_HOLD, idle `Idle` breath), `aura` (a `Glow` effect parented to material). No "tool".
- **Event map**: `PRODUCE(item)` → `Burst("material", 8)` + crossfade `material.entityId` to the produced item, then `ArcOut → off-top`.
- `aura` color is per-skill: fire-orange (Firemaking), bone-gold (Prayer), per-rune elemental (Runecrafting).

### Archetype 4 — Workbench (Smithing, Fletching, Crafting, Cooking, Herblore)
- **Layers**: `input_a` (LEFT_HOLD), `input_b` (RIGHT_HOLD, optional), `bench` (CENTER_GROUND — Compose-drawn primitive; see §6), `tool` (above bench, idle `Swing`), `output` (hidden until PRODUCE).
- **Event map**: `PRODUCE(item)` → `input_a/b` blink+fade, `Burst("bench", 8)`, output spawns and `ArcOut`s.

### Archetype 5 — Plot (Farming)
- **Layers**: `plot` (CENTER_GROUND — Compose-drawn furrow), `crop_stage` (above plot, `entityId` swaps through `<crop>_seedling` → `<crop>_growing` → `<crop>` over the session duration).
- **Event map**: `PRODUCE(item)` → `Burst("plot", 8)` + `ArcOut`.
- *Note:* Farming is time-driven, not event-driven — stage progresses on a clock derived from session duration.

### Archetype 6 — Course (Agility)
- **Layers**: `hero` (LEFT_GROUND, idle `Drift` rightward across the stage on a loop), `course` (full background — one of the 15 placed `*_course.png` sprites).
- **Event map**: each `PRODUCE("agility_xp")` = one lap completion → tiny `Burst("hero", 4)` at finish line; loop restarts.
- No item arcs; XP-only skills get a quieter celebration.

### Scene config file
All 14 configs live in `ui/scene/SceneCatalog.kt` as `val MINING = SceneConfig(...)`-style declarations — pure data, no animation logic. Future skills slot in as a single value.

## 4. UX integration

### The sheet
Animation lives in a `ChunkySheet` (the existing foundation primitive used by `DungeonInfoSheet`, `FoodPickerSheet`, etc.). One sheet composable, `SessionSceneSheet`, configured by which `SceneConfig` to host.

### Entry points
- `ActiveSessionBanner` (skills) and `CombatHudCard` (combat) each get one new affordance: the hero region becomes tappable, with a subtle `Swing` icon overlay in the corner (`IconDisk` pattern, ~24dp, idle breath).
- Tap → sheet rises from bottom, scene starts. The text banner stays as-is; no info moves into the sheet.

### Sheet contents (top to bottom)
1. Drag handle + close affordance (standard `ChunkySheet` header).
2. **Stage** (~280–320dp tall, fills width minus 16dp side padding) — the only visual change vs. today.
3. Mirror of the banner's key stats — HP bars (combat) / countdown + items-this-minute (skills).
4. Two-button row — `Abandon` + `Collect` (existing semantics).

### Lifecycle
`SceneEventBus` is created when the sheet opens and disposed on close, via `DisposableEffect(sessionId)`. Closing and reopening creates a fresh bus tied to wall-clock — idle behaviors restart from t=0 (feels alive), event effects don't replay backlog.

### Backgrounded session
Underlying session keeps simulating (timestamp-based). When the user returns with sheet still open, Compose recomposition picks up at current wall-clock; the adapter starts emitting from now.

### Navigation
No new screen, no nav-graph change. Sheets are part of the host screen's composition. Back closes the sheet (existing `ChunkySheet` behavior).

### Reduce-motion / accessibility
When `LocalAccessibilityManager.isReduceMotionEnabled` is true, the Stage downgrades inline:
- idle behaviors disable
- effect durations halve
- `Shake`/`Burst` are skipped
- `HitFlash` and `DamagePopup` still fire (information preserved)

One branch deep inside `Stage`, not a separate code path.

### Sound
Out of scope for v1. `SfxBridge` interface is defined but no-op by default. `Effect`s may call `sfx.play(soundId)` for forward compatibility.

## 5. Sprite-sheet plumbing (the ~20 hero sheets)

### File contract (already set by CLAUDE.md)
- **Sheet**: `app/src/main/res/drawable-nodpi/<entity>_sheet.png` — single horizontal strip, N equal-width frames.
- **Metadata**: `app/src/main/assets/sprite_sheets/<entity>.json` — `{ frameCount, frameDurationMs, loopPolicy }`.

### `SpriteSheetIcon` composable
- Reads the JSON sidecar once per entity (cached in a `remember`-scoped map).
- Loads the strip via Coil 2.7.0 (`ImageRequest`).
- Uses `produceState` ticker to compute current frame index from wall-clock + `frameDurationMs`.
- Renders the cropped sub-rect at requested `size` with `FilterQuality.None`.
- ~80 lines, in `ui/scene/SpriteSheetIcon.kt`.

### `HeroSpriteRegistry`
A `Set<String>` of entity IDs that have a sheet. `Layer`'s renderer checks the registry: in → `SpriteSheetIcon`, out → `EntityIcon`. Adding a sheet later is two changes: drop PNG+JSON in the right folders, add the entity ID to the registry. No scene-config edits.

### The ~20 hero picks
| Group | Count | Entities |
|---|---|---|
| Raid bosses | 4 | `balrog`, `demon_lord`, `king_black_dragon`, `kraken` |
| Skill action tools | 7 | `bronze_pickaxe_swing`, `bronze_axe_chop`, `bronze_fishing_rod_wobble`, `bronze_hammer_strike` (smithing), `bronze_hoe_till` (farming), `agility_runner`, `bronze_fletching_knife` |
| Hero combat enemies | ~5 | `dragon`, `demon`, `troll`, `dark_wizard`, `wild_dog` (visually distinctive late-game) |
| Headroom | 3–5 | Reserved as the game evolves |

### Tier templating for tool sheets
Tool sheets are authored once at bronze tier; runtime palette recolor produces the other six tiers. When `Mining` archetype's `tool` layer requests `iron_pickaxe`, the registry first looks for `iron_pickaxe_swing` (specific). If absent, it loads `bronze_pickaxe_swing` and applies the iron tier color from `FantasyColors`. **One source sheet per tool action → seven tier outputs at zero authoring cost.**

### Asset workflow
The art pipeline grows one new stage, `sheet_export`, that packs frame-by-frame source (Aseprite / PixelLab export) into the standard strip + JSON. Out of scope for this spec — separate art-pipeline work. This spec defines the file contract so the two efforts land independently.

### Fallback
If an entity is registered as hero but its sheet PNG hasn't landed yet, `SpriteSheetIcon` falls back to its static `EntityIcon` form and logs once. Means we can register hero entities in code before any sheet exists — no missing-asset crashes during rollout.

## 6. Asset & manifest changes

### New entity IDs in `art_manifest.json`
- **Resource nodes (11)** — `copper_ore_node`, `tin_ore_node`, `iron_ore_node`, `coal_node`, `silver_ore_node`, `gold_ore_node`, `mithril_ore_node`, `adamantite_ore_node`, `runite_ore_node`, `platinum_ore_node`, `rune_essence_node`. Visually: rock shape with the ore's color showing through. Distinct from existing `<ore>.png` (the popped-out item is a loose chunk).
- **Tree stumps (7)** — one per tree in the manifest: `tree_stump`, `oak_stump`, `willow_stump`, `maple_stump`, `yew_stump`, `magic_stump`, `redwood_stump`. The Woodcutting archetype swaps `target` to the stump on `PRODUCE` and fades back to the standing tree.
- **Fishing water-patch (1)** — `fishing_spot`. Small bubbling water tile under the rod; generic across fishing tiers.

**Total: 19 new IDs.** Added via `scripts/generate_art_manifest.py` — either extending the existing `Ores`/`Trees` categories (and a tiny home for `fishing_spot`) or as a new "Scene targets" category, whichever the implementation prefers. ~15 lines of script change. Sprite generation is a follow-up; until PNGs land, `EntityIcon` shows the tier-colored fallback, which is fine for v1.

### Compose-drawn primitives (no sprites, no manifest entries)
Geometric scene props live in `ui/scene/primitives/SceneShapes.kt` as ~8 small `@Composable fun DrawAnvil(modifier, tier: Tier)`-style helpers using `Canvas` + `drawPath`:
- `DrawAnvil` (smithing bench)
- `DrawCookpot` (cooking bench)
- `DrawWorkbench` (fletching, crafting, herblore)
- `DrawPlotFurrow` (farming plot)
- `DrawAgilityTrackLine` (agility ground stripe)
- `DrawCombatGroundShadow` (combat ground oval)

Each is <30 lines, sources tier colors from `FantasyColors`. Rationale: a flat dark anvil shape is faster to draw than to author at 5 densities, and these are decorative — the interesting sprite is always the input/output entity on top.

### What does NOT change
- `EntityIcon` API or behavior — still resolves `entityId` → `R.drawable.<id>` with tier-colored fallback.
- `Color.kt` / `palette.py` — tier templating consumes existing 7 tier colors as-is.
- `CombatSimulator` / `SkillSimulator` — adapters wrap existing outputs; simulators are untouched.

### Audit impact
When the 19 new IDs land, total entity count rises from 687 to 706 and headline progress drops from 408/687 (59%) to 408/706 (58%). Expected.

## 7. Implementation slicing

The unified spec implements in six independently mergeable slices. Each slice ships a demonstrable, non-broken experience.

### Slice 1 — Foundation + Mining (the proof)
- Build `Stage`/`Layer`/`Effect`/`SceneEventBus` primitives.
- Build `SkillSceneAdapter` with client-side tick synthesis.
- Build `SessionSceneSheet` host.
- Mining `SceneConfig` (Archetype 2).
- Wire tap-to-expand on `ActiveSessionBanner` *for Mining only*.
- Add 11 ore-node entity IDs to `art_manifest.json` (sprite-less; tier-colored fallback).
- Property animation only — no Coil, no sheets, no `SpriteSheetIcon`.
- **Estimated: ~600 lines net.** This is CLAUDE.md's endorsed first vertical slice and proves every architectural piece end-to-end.

### Slice 2 — Combat
- `CombatSceneAdapter` wrapping the existing decode loop.
- Combat `SceneConfig` (Archetype 1).
- Wire tap-to-expand on `CombatHudCard`.
- **Estimated: ~300 lines.** Delivers the Pokémon-battle vision.

### Slice 3 — Other gathering skills
- Fishing and Woodcutting `SceneConfig`s (reuse Archetype 2).
- Banner wire-ups.
- Add tree stumps + fishing-spot entity IDs to manifest.
- **Estimated: ~50 lines per skill.**

### Slice 4 — Transform, workbench, plot, course skills
- Firemaking, Cooking, Smithing, Fletching, Crafting, Prayer, Runecrafting, Herblore, Farming, Agility `SceneConfig`s.
- `SceneShapes.kt` with the 6 Compose-drawn primitives.
- **Estimated: ~400 lines total.**

### Slice 5 — Hero sprite sheets (Hybrid fidelity layer)
- `SpriteSheetIcon` + Coil wiring.
- `HeroSpriteRegistry`.
- Tier-template palette recolor for multi-frame strips.
- Wire `Layer` renderer to consult the registry.
- Author/import first 4 raid boss sheets.
- Existing scenes upgrade silently — no scene-config changes when an entity gets added to the registry.
- **Estimated: ~200 lines Kotlin + parallel art-pipeline work for sheet authoring.**

### Slice 6 — Polish
- `SfxBridge` stub interface (no-op default).
- TalkBack verification.
- Performance pass on lowest-density devices (target: 60fps with ≤5 active layers).
- Frame-budget assertion in debug builds.

### Risk register
| Risk | Mitigation |
|---|---|
| Property animation on 20+ layered sprites at 60fps | Most scenes have 3–5 active layers; Slice 6 perf pass catches regressions on cheap devices. |
| Synthesized ticks feel robotic ("8 evenly-spaced pops per minute") | `SkillSceneAdapter` can add ±15% jitter to interval timing. Trivial to add if needed. |
| Tier recolor on multi-frame strips | Not previously done in this codebase. Slice 5 includes a small spike before authoring real sheets. |
| Sheet open during a long offline-progress completion | Adapter detects the catch-up frames and emits at most one summary `PRODUCE` per skill type, not 40 backlogged pops. |

### Out of scope
- Sound design / SFX selection.
- Sprite sheet authoring itself (art pipeline's `sheet_export` is its own spec).
- Sheet animation editor or per-entity tuning UI.
- AnimatedVectorDrawable UI flourishes (XP sparkle, level-up burst) — those exist via `RewardBurst` and are adjacent, not part of this system.
- Interactive timing-game mechanics.

---

## Appendix — file layout

```
app/src/main/kotlin/com/fantasyidler/ui/scene/
├── Stage.kt                          # the Box host + event subscription
├── Layer.kt                          # Layer data class + renderer dispatch
├── Effect.kt                         # 6 effect implementations
├── SceneEventBus.kt                  # MutableSharedFlow<SceneEvent>
├── SceneConfig.kt                    # data class + event-map definition
├── SceneCatalog.kt                   # all 14 SceneConfig values
├── SessionSceneSheet.kt              # ChunkySheet host
├── SpriteSheetIcon.kt                # Coil-driven frame renderer (Slice 5)
├── HeroSpriteRegistry.kt             # Set<String> of hero entities (Slice 5)
├── primitives/
│   └── SceneShapes.kt                # Compose-drawn anvil/cookpot/etc (Slice 4)
└── adapter/
    ├── CombatSceneAdapter.kt         # Slice 2
    └── SkillSceneAdapter.kt          # Slice 1
```

```
app/src/main/res/drawable-nodpi/
└── <entity>_sheet.png                # Slice 5 sheets, horizontal strips

app/src/main/assets/sprite_sheets/
└── <entity>.json                     # Slice 5 frame metadata
```
