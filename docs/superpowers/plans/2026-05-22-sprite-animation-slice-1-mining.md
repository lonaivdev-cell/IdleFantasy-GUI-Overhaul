# Sprite Animation — Slice 1 (Foundation + Mining) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Stage + Layer + Effect + SceneEventBus scene engine and ship the first end-to-end animated experience: tap the active Mining session banner → a sheet rises showing a pickaxe swinging at an ore node, with ores popping out and arcing off-screen on each `PRODUCE` tick.

**Architecture:** A small Compose-native scene engine (under `ui/scene/`). `Stage` is a Box that hosts positioned `Layer`s (entityId + idle behavior + tag). `Effect`s are transient overlays triggered by `SceneEvent`s on a `SceneEventBus` (a `MutableSharedFlow`). A `SkillSceneAdapter` synthesizes per-tick `Produce` events client-side from the existing per-minute session frames — no simulator change. Mining is the only surface wired in Slice 1.

**Tech Stack:** Kotlin 2.0, Jetpack Compose (BOM 2024.06.00), Coroutines/Flow, Hilt (already in app — used only via existing ViewModels, no new Hilt modules in Slice 1), JUnit4 + kotlinx-coroutines-test (NEW — bootstrapped in Task 1).

**Source spec:** `docs/superpowers/specs/2026-05-22-sprite-animation-system-design.md`.

---

## File Structure

### New files (10)

| Path | Responsibility |
|---|---|
| `app/src/main/kotlin/com/fantasyidler/ui/scene/SceneEvent.kt` | Sealed class of event types (Hit, Dodge, Produce, Attempt, LevelUp, SessionEnd). |
| `app/src/main/kotlin/com/fantasyidler/ui/scene/SceneEventBus.kt` | `MutableSharedFlow<SceneEvent>` wrapper with `emit`/`tryEmit`. |
| `app/src/main/kotlin/com/fantasyidler/ui/scene/Layer.kt` | `Layer` data class + `LayerPosition` enum + `IdleBehavior` enum. Pure data. |
| `app/src/main/kotlin/com/fantasyidler/ui/scene/Effect.kt` | `Effect` sealed class (6 implementations: HitFlash, Shake, DamagePopup, ArcOut, Burst, Glow) + `ShakeMagnitude` enum. |
| `app/src/main/kotlin/com/fantasyidler/ui/scene/Stage.kt` | The `@Composable fun Stage(config, bus, modifier)` host — renders layers with idle behaviors and reacts to events with effect compositions. |
| `app/src/main/kotlin/com/fantasyidler/ui/scene/SceneConfig.kt` | `SceneConfig` data class: layers, event→effects mapping, optional background entityId. |
| `app/src/main/kotlin/com/fantasyidler/ui/scene/SceneCatalog.kt` | Catalog of declared scenes. Slice 1 ships `MINING` only. |
| `app/src/main/kotlin/com/fantasyidler/ui/scene/SessionSceneSheet.kt` | `@Composable fun SessionSceneSheet(config, bus, open, onDismiss, statsContent)` — wraps `ChunkySheet` around a `Stage` with stat mirror + action buttons. |
| `app/src/main/kotlin/com/fantasyidler/ui/scene/adapter/SkillSceneAdapter.kt` | Tick synthesizer. Given a per-minute frame's item list, emits N evenly-spaced (jittered) `Produce` events. |
| `app/src/test/kotlin/com/fantasyidler/ui/scene/SceneEventBusTest.kt` + `adapter/SkillSceneAdapterTest.kt` | Unit tests for pure-Kotlin pieces. |

### Modified files (3)

| Path | Change |
|---|---|
| `gradle/libs.versions.toml` | Add `junit` + `kotlinxCoroutinesTest` versions + library aliases. |
| `app/build.gradle.kts` | Add `testImplementation` deps for JUnit + coroutines-test. |
| `scripts/generate_art_manifest.py` | Add 11 ore-node entity IDs. |
| `app/src/main/kotlin/com/fantasyidler/ui/screen/skills/ActiveSessionBanner.kt` | Add `onTap` callback parameter + visual tap affordance. |
| `app/src/main/kotlin/com/fantasyidler/ui/screen/SkillsScreen.kt` | Wire `SessionSceneSheet` for Mining sessions only. |

### Testing strategy

This codebase has no tests today. We bootstrap minimal JUnit + coroutines-test infrastructure (Task 1) and unit-test the **pure-Kotlin pieces** — the event bus and the tick synthesizer. The Compose UI pieces (`Stage`, `Layer`, `Effect` renderers, `SessionSceneSheet`) are verified via **Compose `@Preview` composables** that the user can visually inspect in Android Studio's Preview pane (no instrumented tests in Slice 1). This matches the project's current rhythm and avoids dragging in Espresso/Compose-UI-test until there's a sustained testing culture.

---

## Task 1: Bootstrap test infrastructure

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Create: `app/src/test/kotlin/.gitkeep` (forces dir into git so future tests have somewhere to land)

- [ ] **Step 1: Add JUnit + coroutines-test to the version catalog**

Edit `gradle/libs.versions.toml`. In the `[versions]` block (after `coil = "2.7.0"`), add:

```toml
junit                  = "4.13.2"
kotlinxCoroutinesTest  = "1.8.1"
```

In the `[libraries]` block (after the Coil entry, before `[plugins]`), add:

```toml
# Testing
junit                       = { group = "junit",                    name = "junit",                       version.ref = "junit" }
kotlinx-coroutines-test     = { group = "org.jetbrains.kotlinx",    name = "kotlinx-coroutines-test",     version.ref = "kotlinxCoroutinesTest" }
```

- [ ] **Step 2: Wire the test deps in the app module**

Edit `app/build.gradle.kts`. In the `dependencies { ... }` block (anywhere — convention is at the end), add:

```kotlin
    // Testing (Slice 1: unit tests for scene engine pure-Kotlin pieces)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
```

- [ ] **Step 3: Create the test source set directory**

Run:

```bash
mkdir -p app/src/test/kotlin/com/fantasyidler/ui/scene/adapter
touch app/src/test/kotlin/.gitkeep
```

- [ ] **Step 4: Verify the build still configures**

Run:

```bash
./gradlew help -q 2>&1 | tail -10
```

Expected: no error; gradle prints task summary. If a dependency-resolution error appears, fix the version in `libs.versions.toml`.

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/test/kotlin/.gitkeep
git commit -m "chore(test): bootstrap JUnit + coroutines-test for scene engine"
```

---

## Task 2: SceneEvent + SceneEventBus

**Files:**
- Create: `app/src/main/kotlin/com/fantasyidler/ui/scene/SceneEvent.kt`
- Create: `app/src/main/kotlin/com/fantasyidler/ui/scene/SceneEventBus.kt`
- Create: `app/src/test/kotlin/com/fantasyidler/ui/scene/SceneEventBusTest.kt`

- [ ] **Step 1: Write the failing test first**

Create `app/src/test/kotlin/com/fantasyidler/ui/scene/SceneEventBusTest.kt`:

```kotlin
package com.fantasyidler.ui.scene

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneEventBusTest {

    @Test
    fun `emitted events are delivered to a subscriber`() = runTest(StandardTestDispatcher()) {
        val bus = SceneEventBus()
        val collected = mutableListOf<SceneEvent>()
        val job = launch { bus.events.take(2).toList(collected) }

        bus.emit(SceneEvent.Produce(item = "copper_ore", fromTag = "output"))
        bus.emit(SceneEvent.Attempt(toolTag = "tool"))
        advanceUntilIdle()
        job.join()

        assertEquals(2, collected.size)
        assertEquals("copper_ore", (collected[0] as SceneEvent.Produce).item)
        assertEquals("tool", (collected[1] as SceneEvent.Attempt).toolTag)
    }

    @Test
    fun `tryEmit returns true when buffer has capacity`() {
        val bus = SceneEventBus()
        // Buffer capacity is 16 — no subscribers needed for tryEmit to succeed.
        repeat(16) {
            assertTrue(bus.tryEmit(SceneEvent.Attempt("tool")))
        }
    }
}
```

- [ ] **Step 2: Run the test to confirm it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "com.fantasyidler.ui.scene.SceneEventBusTest" 2>&1 | tail -20
```

Expected: compilation FAIL — `SceneEvent` and `SceneEventBus` unresolved.

- [ ] **Step 3: Create `SceneEvent.kt`**

Create `app/src/main/kotlin/com/fantasyidler/ui/scene/SceneEvent.kt`:

```kotlin
package com.fantasyidler.ui.scene

/**
 * Tick-level events the scene engine reacts to. Adapters emit these; the Stage
 * translates them into Effects on tagged Layers via SceneConfig.eventMap.
 */
sealed class SceneEvent {

    /** An attack landed. [amount] is positive; 0 means a miss (use Dodge for that). */
    data class Hit(val attacker: String, val target: String, val amount: Int) : SceneEvent()

    /** A potential hit was avoided. */
    data class Dodge(val target: String) : SceneEvent()

    /** An item was produced by an action. [fromTag] is the layer it should appear to come from. */
    data class Produce(val item: String, val fromTag: String) : SceneEvent()

    /** Skill or tool action fired but produced nothing this tick. Keeps the stage alive on dry minutes. */
    data class Attempt(val toolTag: String) : SceneEvent()

    /** Player levelled up the [skill] mid-session. */
    data class LevelUp(val skill: String) : SceneEvent()

    /** Session terminated. [outcome] is freeform ("complete", "abandoned", "died"). */
    data class SessionEnd(val outcome: String) : SceneEvent()
}
```

- [ ] **Step 4: Create `SceneEventBus.kt`**

Create `app/src/main/kotlin/com/fantasyidler/ui/scene/SceneEventBus.kt`:

```kotlin
package com.fantasyidler.ui.scene

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Per-session event channel. Adapters emit; the Stage subscribes.
 *
 * Buffer capacity of 16 means tryEmit() never drops events under normal pacing
 * (combat is one event every ~100ms worst case; skills synthesize ≤ 60/minute).
 *
 * One bus per active session. Created in SessionSceneSheet's DisposableEffect,
 * dropped when the sheet closes.
 */
class SceneEventBus {
    private val _events = MutableSharedFlow<SceneEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<SceneEvent> = _events.asSharedFlow()

    suspend fun emit(event: SceneEvent) = _events.emit(event)
    fun tryEmit(event: SceneEvent): Boolean = _events.tryEmit(event)
}
```

- [ ] **Step 5: Run the test to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "com.fantasyidler.ui.scene.SceneEventBusTest" 2>&1 | tail -10
```

Expected: PASS, 2 tests.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/kotlin/com/fantasyidler/ui/scene/SceneEvent.kt \
        app/src/main/kotlin/com/fantasyidler/ui/scene/SceneEventBus.kt \
        app/src/test/kotlin/com/fantasyidler/ui/scene/SceneEventBusTest.kt
git commit -m "feat(scene): SceneEvent + SceneEventBus"
```

---

## Task 3: Layer + LayerPosition + IdleBehavior

**Files:**
- Create: `app/src/main/kotlin/com/fantasyidler/ui/scene/Layer.kt`

- [ ] **Step 1: Create `Layer.kt`**

Create `app/src/main/kotlin/com/fantasyidler/ui/scene/Layer.kt`:

```kotlin
package com.fantasyidler.ui.scene

/**
 * One visual element on a Stage. Pure data — rendering lives in Stage.kt.
 *
 * - [tag] is the targetable identifier ("player", "tool", "target", "output", ...).
 *   Effects reference layers by tag.
 * - [entityId] is the snake_case sprite id resolved by EntityIcon. Null means
 *   the layer is purely a decorative slot (e.g., a Compose-drawn primitive).
 * - [position] is a relative anchor; Stage maps these to actual offsets.
 * - [idleBehavior] runs whenever no event-triggered animation is active on
 *   this layer.
 * - [visible] is the initial visibility — output layers start hidden and only
 *   appear during a Produce event.
 */
data class Layer(
    val tag: String,
    val entityId: String?,
    val position: LayerPosition,
    val idleBehavior: IdleBehavior = IdleBehavior.None,
    val visible: Boolean = true,
)

/**
 * Where on the Stage a Layer is anchored. Stage.kt translates these into
 * dp offsets relative to the Stage size; consumers don't pick pixel coords.
 */
enum class LayerPosition {
    LEFT_GROUND,
    RIGHT_GROUND,
    CENTER_GROUND,
    LEFT_HOLD,
    RIGHT_HOLD,
    CENTER_HOLD,
    RIGHT_ACTOR,
    LEFT_ACTOR,
    OFF_TOP,
    OFF_BOTTOM,
}

/**
 * Looping property animation a Layer runs when nothing else is happening.
 * Drives a simple Modifier transform inside Stage.kt; no per-behavior file.
 */
enum class IdleBehavior {
    None,
    Bob,      // vertical sway, ~3dp amplitude, 1.2s cycle
    Breath,   // gentle scale 1.0 ↔ 1.04, 1.8s cycle
    Swing,    // rotation -8° ↔ +8°, 0.9s cycle (used by tools)
    Wobble,   // tiny rotation + horizontal jitter, 0.7s cycle (fishing rod)
    Drift,    // slow horizontal translation, used for Agility hero (Slice 4)
}
```

- [ ] **Step 2: Verify the file compiles**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -5
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/com/fantasyidler/ui/scene/Layer.kt
git commit -m "feat(scene): Layer data + LayerPosition + IdleBehavior"
```

---

## Task 4: Effect sealed class

**Files:**
- Create: `app/src/main/kotlin/com/fantasyidler/ui/scene/Effect.kt`

- [ ] **Step 1: Create `Effect.kt`**

Create `app/src/main/kotlin/com/fantasyidler/ui/scene/Effect.kt`:

```kotlin
package com.fantasyidler.ui.scene

import androidx.compose.ui.graphics.Color

/**
 * Transient visual overlay triggered by a SceneEvent. Stage.kt renders each
 * Effect for its declared duration then disposes it.
 *
 * Effects always target a [tag] that must match a Layer.tag in the active
 * SceneConfig. Unknown tags log a warning and skip — never crash.
 */
sealed class Effect {
    abstract val durationMs: Int

    /** Brightness + hue pulse on the tagged layer. Used on hits/produces. */
    data class HitFlash(
        val tag: String,
        override val durationMs: Int = 240,
    ) : Effect()

    /** Translational jitter. */
    data class Shake(
        val tag: String,
        val magnitude: ShakeMagnitude = ShakeMagnitude.Small,
        override val durationMs: Int = 280,
    ) : Effect()

    /** Floating "+N" text rising from the tagged layer. */
    data class DamagePopup(
        val tag: String,
        val amount: Int,
        val color: Color,
        override val durationMs: Int = 700,
    ) : Effect()

    /**
     * Spawn [entityId] at the [fromTag] layer and arc to [toTag]. If [toTag] is
     * a special sentinel ("OFF_TOP", "OFF_BOTTOM"), Stage arcs to that screen
     * edge. Used for "ore pops out and flies to inventory".
     */
    data class ArcOut(
        val fromTag: String,
        val toTag: String,
        val entityId: String,
        override val durationMs: Int = 600,
    ) : Effect()

    /** Radial particle pop centred on the layer. */
    data class Burst(
        val tag: String,
        val count: Int = 6,
        override val durationMs: Int = 400,
    ) : Effect()

    /** Coloured halo behind the layer for a window. */
    data class Glow(
        val tag: String,
        val color: Color,
        override val durationMs: Int = 800,
    ) : Effect()
}

enum class ShakeMagnitude(val dp: Float) {
    Small(2f),
    Medium(4f),
    Large(8f),
}
```

- [ ] **Step 2: Verify the file compiles**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -5
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/com/fantasyidler/ui/scene/Effect.kt
git commit -m "feat(scene): Effect sealed class + ShakeMagnitude"
```

---

## Task 5: SceneConfig + minimal SceneCatalog (Mining only)

**Files:**
- Create: `app/src/main/kotlin/com/fantasyidler/ui/scene/SceneConfig.kt`
- Create: `app/src/main/kotlin/com/fantasyidler/ui/scene/SceneCatalog.kt`

- [ ] **Step 1: Create `SceneConfig.kt`**

Create `app/src/main/kotlin/com/fantasyidler/ui/scene/SceneConfig.kt`:

```kotlin
package com.fantasyidler.ui.scene

import kotlin.reflect.KClass

/**
 * A complete scene declaration: which layers exist, what background to use,
 * and how each event type should compose into effects.
 *
 * Pure data — instances live in SceneCatalog.kt and are referenced by
 * SessionSceneSheet to drive a specific surface (Mining, Combat, Fishing, …).
 *
 * @property id Stable id used for logging + analytics (e.g., "mining").
 * @property backgroundEntityId Optional entityId rendered as a faded full-bleed
 *           sprite behind the layers (e.g., dungeon background for Combat,
 *           null for skills).
 * @property layers Initial layer set. Output-type layers should set visible=false.
 * @property eventMap Maps an event type to a factory that, given the event,
 *           produces zero or more Effects to play. Factories let us read
 *           per-event payload (e.g., the produced item's entityId for ArcOut).
 */
data class SceneConfig(
    val id: String,
    val backgroundEntityId: String?,
    val layers: List<Layer>,
    val eventMap: Map<KClass<out SceneEvent>, (SceneEvent) -> List<Effect>>,
)
```

- [ ] **Step 2: Create `SceneCatalog.kt` with the Mining scene**

Create `app/src/main/kotlin/com/fantasyidler/ui/scene/SceneCatalog.kt`:

```kotlin
package com.fantasyidler.ui.scene

/**
 * The complete catalog of declared scenes. Slice 1 ships MINING only;
 * Slices 2–4 add Combat + the rest of the 14 scenes.
 *
 * Adding a new scene = adding a `val` here. No new composables needed
 * unless the scene introduces a brand-new archetype.
 */
object SceneCatalog {

    /**
     * Archetype 2 — Tool + node. Pickaxe (RIGHT_ACTOR, idle Swing) strikes the
     * ore node (CENTER_HOLD). On each Produce event, the node shakes, a burst
     * fires, and the produced ore arcs off-top to suggest inventory pickup.
     *
     * The `tool` layer's entityId is overridden at construction time by the
     * host (see SessionSceneSheet) with the player's best-equipped pickaxe id;
     * the catalog default is "bronze_pickaxe".
     *
     * The `target` layer's entityId is overridden with the selected ore's node
     * id (e.g., "copper_ore_node").
     */
    val MINING: SceneConfig = SceneConfig(
        id = "mining",
        backgroundEntityId = null,
        layers = listOf(
            Layer(
                tag = "target",
                entityId = "copper_ore_node",  // overridable
                position = LayerPosition.CENTER_HOLD,
                idleBehavior = IdleBehavior.None,
            ),
            Layer(
                tag = "tool",
                entityId = "bronze_pickaxe",  // overridable
                position = LayerPosition.RIGHT_ACTOR,
                idleBehavior = IdleBehavior.Swing,
            ),
            Layer(
                tag = "output",
                entityId = null,  // populated per event
                position = LayerPosition.CENTER_HOLD,
                idleBehavior = IdleBehavior.None,
                visible = false,
            ),
        ),
        eventMap = mapOf(
            SceneEvent.Produce::class to { event ->
                val produce = event as SceneEvent.Produce
                listOf(
                    Effect.Shake(tag = "target", magnitude = ShakeMagnitude.Small),
                    Effect.Burst(tag = "target", count = 6),
                    Effect.ArcOut(
                        fromTag = "target",
                        toTag = "OFF_TOP",
                        entityId = produce.item,
                    ),
                )
            },
            SceneEvent.Attempt::class to { _ ->
                // Dry minute — just a small shake so the stage doesn't look frozen.
                listOf(Effect.Shake(tag = "target", magnitude = ShakeMagnitude.Small))
            },
            SceneEvent.LevelUp::class to { _ ->
                listOf(
                    Effect.Burst(tag = "tool", count = 12),
                    Effect.HitFlash(tag = "tool"),
                )
            },
        ),
    )

    /**
     * Returns a copy of [MINING] with the tool and target entity ids
     * overridden per-session.
     */
    fun mining(pickaxeId: String, oreNodeId: String): SceneConfig {
        return MINING.copy(
            layers = MINING.layers.map { layer ->
                when (layer.tag) {
                    "tool" -> layer.copy(entityId = pickaxeId)
                    "target" -> layer.copy(entityId = oreNodeId)
                    else -> layer
                }
            },
        )
    }
}
```

- [ ] **Step 3: Verify it compiles**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -5
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/fantasyidler/ui/scene/SceneConfig.kt \
        app/src/main/kotlin/com/fantasyidler/ui/scene/SceneCatalog.kt
git commit -m "feat(scene): SceneConfig + Mining scene declaration"
```

---

## Task 6: SkillSceneAdapter (the tick synthesizer) + tests

**Files:**
- Create: `app/src/main/kotlin/com/fantasyidler/ui/scene/adapter/SkillSceneAdapter.kt`
- Create: `app/src/test/kotlin/com/fantasyidler/ui/scene/adapter/SkillSceneAdapterTest.kt`

- [ ] **Step 1: Write the failing test first**

Create `app/src/test/kotlin/com/fantasyidler/ui/scene/adapter/SkillSceneAdapterTest.kt`:

```kotlin
package com.fantasyidler.ui.scene.adapter

import com.fantasyidler.ui.scene.SceneEvent
import com.fantasyidler.ui.scene.SceneEventBus
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SkillSceneAdapterTest {

    @Test
    fun `playFrame emits one Produce per item`() = runTest {
        val bus = SceneEventBus()
        val collected = mutableListOf<SceneEvent>()
        val collectorJob = launch { bus.events.toList(collected) }

        val adapter = SkillSceneAdapter(
            bus = bus,
            random = Random(seed = 42),
            jitterFraction = 0f,  // deterministic intervals
        )

        adapter.playFrame(items = listOf("copper_ore", "tin_ore", "copper_ore"), durationMs = 3000L)
        advanceUntilIdle()
        collectorJob.cancel()

        val produces = collected.filterIsInstance<SceneEvent.Produce>()
        assertEquals(3, produces.size)
        assertEquals("copper_ore", produces[0].item)
        assertEquals("tin_ore", produces[1].item)
        assertEquals("copper_ore", produces[2].item)
    }

    @Test
    fun `playFrame emits one Attempt when items is empty`() = runTest {
        val bus = SceneEventBus()
        val collected = mutableListOf<SceneEvent>()
        val collectorJob = launch { bus.events.toList(collected) }

        val adapter = SkillSceneAdapter(bus = bus, random = Random(0))
        adapter.playFrame(items = emptyList(), durationMs = 1000L)
        advanceUntilIdle()
        collectorJob.cancel()

        assertEquals(1, collected.size)
        assertTrue(collected[0] is SceneEvent.Attempt)
    }

    @Test
    fun `playFrame respects total durationMs even with jitter`() = runTest {
        val bus = SceneEventBus()
        val collectorJob = launch { bus.events.toList(mutableListOf()) }

        val adapter = SkillSceneAdapter(
            bus = bus,
            random = Random(seed = 1),
            jitterFraction = 0.15f,
        )

        val start = currentTime
        adapter.playFrame(items = listOf("copper_ore", "copper_ore"), durationMs = 2000L)
        val elapsed = currentTime - start
        collectorJob.cancel()

        assertEquals(2000L, elapsed)
    }
}
```

- [ ] **Step 2: Run the test to confirm it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "com.fantasyidler.ui.scene.adapter.SkillSceneAdapterTest" 2>&1 | tail -20
```

Expected: compilation FAIL — `SkillSceneAdapter` unresolved.

- [ ] **Step 3: Create `SkillSceneAdapter.kt`**

Create `app/src/main/kotlin/com/fantasyidler/ui/scene/adapter/SkillSceneAdapter.kt`:

```kotlin
package com.fantasyidler.ui.scene.adapter

import com.fantasyidler.ui.scene.SceneEvent
import com.fantasyidler.ui.scene.SceneEventBus
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Synthesizes per-tick SceneEvents from the skill simulator's per-minute frames.
 *
 * The skill simulator emits one frame per minute carrying `items: List<Drop>` and
 * `xpGain`. There's no intra-minute granularity in persisted data. This adapter
 * spreads each frame's N items evenly across the minute (with small jitter so
 * "pop, pop, pop" doesn't feel robotic) and emits a Produce per item.
 *
 * Zero memory at rest — the adapter is created per sheet open, runs as a
 * coroutine, and dies when the sheet closes.
 *
 * @property toolTag Layer tag for the active tool (default "tool").
 * @property outputTag Layer tag the produced item should appear from
 *                    (default "output" — overridden to "target" for Mining
 *                    where ore comes out of the node, not the pickaxe).
 * @property jitterFraction ±fraction applied to each interval. 0.15 = ±15%.
 *                          Set to 0f in tests for deterministic timing.
 */
class SkillSceneAdapter(
    private val bus: SceneEventBus,
    private val toolTag: String = "tool",
    private val outputTag: String = "target",
    private val random: Random = Random.Default,
    private val jitterFraction: Float = 0.15f,
) {

    /**
     * Play one minute-frame's worth of events, blocking for [durationMs].
     *
     * - Empty [items]: emit one Attempt mid-frame and wait the rest.
     * - Non-empty [items]: emit one Produce per item at evenly-spaced
     *   intervals with optional jitter, padding to [durationMs] total.
     */
    suspend fun playFrame(
        items: List<String>,
        durationMs: Long = 60_000L,
    ) {
        if (items.isEmpty()) {
            delay(durationMs / 2)
            bus.emit(SceneEvent.Attempt(toolTag))
            delay(durationMs - durationMs / 2)
            return
        }

        val n = items.size
        val baseInterval = durationMs / n
        var elapsed = 0L

        for (item in items) {
            val rawJitter = baseInterval * jitterFraction * (random.nextFloat() * 2f - 1f)
            val wait = (baseInterval + rawJitter.toLong()).coerceAtLeast(50L)
            // Don't overshoot the frame.
            val safeWait = if (elapsed + wait > durationMs) (durationMs - elapsed).coerceAtLeast(0L) else wait
            delay(safeWait)
            elapsed += safeWait
            bus.emit(SceneEvent.Produce(item = item, fromTag = outputTag))
        }

        val remainder = durationMs - elapsed
        if (remainder > 0) delay(remainder)
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

```bash
./gradlew :app:testDebugUnitTest --tests "com.fantasyidler.ui.scene.adapter.SkillSceneAdapterTest" 2>&1 | tail -15
```

Expected: PASS, 3 tests.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/fantasyidler/ui/scene/adapter/SkillSceneAdapter.kt \
        app/src/test/kotlin/com/fantasyidler/ui/scene/adapter/SkillSceneAdapterTest.kt
git commit -m "feat(scene): SkillSceneAdapter with jittered tick synthesis + tests"
```

---

## Task 7: Stage composable (static layer rendering)

**Files:**
- Create: `app/src/main/kotlin/com/fantasyidler/ui/scene/Stage.kt`

- [ ] **Step 1: Create the initial `Stage.kt` — layer placement only, no effects yet**

Create `app/src/main/kotlin/com/fantasyidler/ui/scene/Stage.kt`:

```kotlin
package com.fantasyidler.ui.scene

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.fantasyidler.ui.components.EntityIcon
import com.fantasyidler.ui.theme.fantasy.FantasyTheme
import com.fantasyidler.ui.theme.fantasy.LocalFantasyTokens
import kotlinx.coroutines.flow.collectLatest

/**
 * The host composable. Renders the layers of a [SceneConfig] with idle
 * behaviours, and reacts to events from [bus] by playing the effects declared
 * in `config.eventMap` against tagged layers.
 *
 * Sized for the tap-to-expand sheet — caller controls overall size via
 * [modifier], default fills width with a 280dp height.
 */
@Composable
fun Stage(
    config: SceneConfig,
    bus: SceneEventBus,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(280.dp),
) {
    val tokens = LocalFantasyTokens.current

    Box(
        modifier = modifier.background(tokens.colors.surfaceContainer),
    ) {
        // Background sprite (optional)
        config.backgroundEntityId?.let { bg ->
            EntityIcon(
                entityId = bg,
                size = 280.dp,
                modifier = Modifier.fillMaxSize().alpha(0.35f),
            )
        }

        // Layers
        for (layer in config.layers) {
            LayerBox(layer = layer)
        }
    }
}

@Composable
private fun LayerBox(layer: Layer) {
    if (!layer.visible || layer.entityId == null) return

    val alignment = layer.position.toAlignment()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(IntOffset.Zero)
                }
            },
        contentAlignment = alignment,
    ) {
        EntityIcon(
            entityId = layer.entityId,
            size = 88.dp,
        )
    }
}

private fun LayerPosition.toAlignment(): Alignment = when (this) {
    LayerPosition.LEFT_GROUND -> Alignment.BottomStart
    LayerPosition.RIGHT_GROUND -> Alignment.BottomEnd
    LayerPosition.CENTER_GROUND -> Alignment.BottomCenter
    LayerPosition.LEFT_HOLD -> Alignment.CenterStart
    LayerPosition.RIGHT_HOLD -> Alignment.CenterEnd
    LayerPosition.CENTER_HOLD -> Alignment.Center
    LayerPosition.LEFT_ACTOR -> Alignment.CenterStart
    LayerPosition.RIGHT_ACTOR -> Alignment.CenterEnd
    LayerPosition.OFF_TOP -> Alignment.TopCenter
    LayerPosition.OFF_BOTTOM -> Alignment.BottomCenter
}

@Preview(showBackground = true, name = "Mining stage — static")
@Composable
private fun MiningStagePreview() {
    FantasyTheme {
        val bus = remember { SceneEventBus() }
        Stage(
            config = SceneCatalog.mining(pickaxeId = "bronze_pickaxe", oreNodeId = "copper_ore_node"),
            bus = bus,
        )
    }
}
```

- [ ] **Step 2: Verify it compiles**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL.
If `FantasyTheme` or `LocalFantasyTokens` aren't found at those exact paths, run `grep -rn "object FantasyTheme\|val LocalFantasyTokens" app/src/main/kotlin/com/fantasyidler/ui/theme/fantasy/` to confirm imports, then adjust.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/com/fantasyidler/ui/scene/Stage.kt
git commit -m "feat(scene): Stage composable with static layer placement"
```

---

## Task 8: Stage idle behaviours

**Files:**
- Modify: `app/src/main/kotlin/com/fantasyidler/ui/scene/Stage.kt`

- [ ] **Step 1: Replace `LayerBox` with an idle-animated version**

Edit `app/src/main/kotlin/com/fantasyidler/ui/scene/Stage.kt`. Replace the existing `LayerBox` function with this implementation, and add the new imports at the top of the file.

Add to imports (next to the existing `androidx.compose.*` imports):

```kotlin
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
```

Replace the `LayerBox` function (and remove its older body):

```kotlin
@Composable
private fun LayerBox(layer: Layer) {
    if (!layer.visible || layer.entityId == null) return

    val alignment = layer.position.toAlignment()
    val transition = rememberInfiniteTransition(label = "idle_${layer.tag}")

    val rotation: Float = when (layer.idleBehavior) {
        IdleBehavior.Swing -> transition.animateFloat(
            initialValue = -8f,
            targetValue = 8f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 450, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "swing",
        ).value
        IdleBehavior.Wobble -> transition.animateFloat(
            initialValue = -4f,
            targetValue = 4f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 350, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "wobble",
        ).value
        else -> 0f
    }

    val translationY: Float = when (layer.idleBehavior) {
        IdleBehavior.Bob -> transition.animateFloat(
            initialValue = 0f,
            targetValue = -8f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "bob",
        ).value
        else -> 0f
    }

    val scale: Float = when (layer.idleBehavior) {
        IdleBehavior.Breath -> transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "breath",
        ).value
        else -> 1f
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = alignment,
    ) {
        EntityIcon(
            entityId = layer.entityId,
            size = 88.dp,
            modifier = Modifier.graphicsLayer(
                rotationZ = rotation,
                translationY = translationY,
                scaleX = scale,
                scaleY = scale,
            ),
        )
    }
}
```

- [ ] **Step 2: Verify it compiles**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -5
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Visually verify in Android Studio**

Open Android Studio → `Stage.kt` → click the Split view → the `MiningStagePreview` should show the pickaxe rocking back and forth via `Swing`. If the preview hangs (animations sometimes don't play in static preview), tap the "Start animation preview" play icon at the bottom of the preview pane.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/fantasyidler/ui/scene/Stage.kt
git commit -m "feat(scene): idle behaviours on Stage layers (Bob/Breath/Swing/Wobble)"
```

---

## Task 9: Stage event → effect dispatch

**Files:**
- Modify: `app/src/main/kotlin/com/fantasyidler/ui/scene/Stage.kt`

- [ ] **Step 1: Add an active-effects state list + event subscription to `Stage`**

Edit `Stage.kt`. Add these imports if not already present:

```kotlin
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.unit.IntOffset
import kotlin.random.Random
```

Replace the `Stage` composable body to subscribe to the bus, accumulate active effects, and render them as an overlay:

```kotlin
@Composable
fun Stage(
    config: SceneConfig,
    bus: SceneEventBus,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(280.dp),
) {
    val tokens = LocalFantasyTokens.current
    val activeEffects = remember { mutableStateListOf<ActiveEffect>() }

    LaunchedEffect(bus) {
        bus.events.collectLatest { event ->
            val factories = config.eventMap[event::class] ?: return@collectLatest
            val newEffects = factories(event)
            val now = System.currentTimeMillis()
            newEffects.forEach { effect ->
                activeEffects.add(ActiveEffect(effect = effect, startedAt = now, id = Random.nextLong()))
            }
        }
    }

    // Tick to expire effects whose durations have elapsed.
    LaunchedEffect(activeEffects) {
        while (true) {
            kotlinx.coroutines.delay(50L)
            val now = System.currentTimeMillis()
            activeEffects.removeAll { now - it.startedAt > it.effect.durationMs }
        }
    }

    Box(modifier = modifier.background(tokens.colors.surfaceContainer)) {
        config.backgroundEntityId?.let { bg ->
            EntityIcon(
                entityId = bg,
                size = 280.dp,
                modifier = Modifier.fillMaxSize().alpha(0.35f),
            )
        }

        for (layer in config.layers) {
            val shakeForTag = activeEffects.firstOrNull {
                it.effect is Effect.Shake && (it.effect as Effect.Shake).tag == layer.tag
            }?.effect as? Effect.Shake
            LayerBox(layer = layer, shake = shakeForTag)
        }

        // Overlay effects (popups, arcs, bursts)
        for (active in activeEffects) {
            key(active.id) { ActiveEffectOverlay(active, config) }
        }
    }
}

private data class ActiveEffect(
    val effect: Effect,
    val startedAt: Long,
    val id: Long,
)

@Composable
private fun ActiveEffectOverlay(active: ActiveEffect, config: SceneConfig) {
    val alpha = remember { Animatable(1f) }
    LaunchedEffect(active.id) {
        alpha.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = active.effect.durationMs))
    }

    when (val effect = active.effect) {
        is Effect.DamagePopup -> {
            val translation = remember { Animatable(0f) }
            LaunchedEffect(active.id) {
                translation.animateTo(targetValue = -48f, animationSpec = tween(durationMillis = effect.durationMs))
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(alpha.value),
                contentAlignment = config.layerAlignmentFor(effect.tag) ?: Alignment.Center,
            ) {
                Text(
                    text = "+${effect.amount}",
                    color = effect.color,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.graphicsLayer(translationY = translation.value),
                )
            }
        }
        is Effect.ArcOut -> {
            val progress = remember { Animatable(0f) }
            LaunchedEffect(active.id) {
                progress.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = effect.durationMs))
            }
            val targetY = when (effect.toTag) {
                "OFF_TOP" -> -300f
                "OFF_BOTTOM" -> 300f
                else -> 0f
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(1f - progress.value),
                contentAlignment = config.layerAlignmentFor(effect.fromTag) ?: Alignment.Center,
            ) {
                EntityIcon(
                    entityId = effect.entityId,
                    size = 48.dp,
                    modifier = Modifier.graphicsLayer(
                        translationY = targetY * progress.value,
                        scaleX = 1f - 0.3f * progress.value,
                        scaleY = 1f - 0.3f * progress.value,
                    ),
                )
            }
        }
        is Effect.Burst -> {
            val progress = remember { Animatable(0f) }
            LaunchedEffect(active.id) {
                progress.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = effect.durationMs))
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(1f - progress.value),
                contentAlignment = config.layerAlignmentFor(effect.tag) ?: Alignment.Center,
            ) {
                Text(
                    text = "✦".repeat(effect.count).take(effect.count),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.graphicsLayer(
                        scaleX = 1f + progress.value,
                        scaleY = 1f + progress.value,
                    ),
                )
            }
        }
        is Effect.HitFlash, is Effect.Glow -> {
            // Rendered as alpha + tint on the LayerBox itself in a future pass;
            // visible in Slice 1 only via overlays already present (Burst).
        }
        is Effect.Shake -> {
            // Consumed by LayerBox directly (see Stage body) — no overlay.
        }
    }
}

private fun SceneConfig.layerAlignmentFor(tag: String): Alignment? {
    val pos = layers.firstOrNull { it.tag == tag }?.position ?: return null
    return pos.toAlignment()
}
```

Replace the `LayerBox` signature + body to consume the optional Shake:

```kotlin
@Composable
private fun LayerBox(layer: Layer, shake: Effect.Shake? = null) {
    if (!layer.visible || layer.entityId == null) return

    val alignment = layer.position.toAlignment()
    val transition = rememberInfiniteTransition(label = "idle_${layer.tag}")

    val rotation: Float = when (layer.idleBehavior) {
        IdleBehavior.Swing -> transition.animateFloat(
            initialValue = -8f, targetValue = 8f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 450, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ), label = "swing",
        ).value
        IdleBehavior.Wobble -> transition.animateFloat(
            initialValue = -4f, targetValue = 4f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 350, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ), label = "wobble",
        ).value
        else -> 0f
    }

    val translationY: Float = when (layer.idleBehavior) {
        IdleBehavior.Bob -> transition.animateFloat(
            initialValue = 0f, targetValue = -8f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ), label = "bob",
        ).value
        else -> 0f
    }

    val scale: Float = when (layer.idleBehavior) {
        IdleBehavior.Breath -> transition.animateFloat(
            initialValue = 1f, targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ), label = "breath",
        ).value
        else -> 1f
    }

    // Shake overlay on top of idle rotation/translation.
    val shakeOffset: Float = if (shake != null) {
        val shakeTransition = rememberInfiniteTransition(label = "shake_${layer.tag}")
        shakeTransition.animateFloat(
            initialValue = -shake.magnitude.dp, targetValue = shake.magnitude.dp,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 60, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ), label = "shake",
        ).value
    } else 0f

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = alignment) {
        EntityIcon(
            entityId = layer.entityId,
            size = 88.dp,
            modifier = Modifier.graphicsLayer(
                rotationZ = rotation,
                translationY = translationY,
                translationX = shakeOffset,
                scaleX = scale,
                scaleY = scale,
            ),
        )
    }
}
```

Update the preview at the bottom of the file to drive a few events so the static preview shows the effects pipeline working:

```kotlin
@Preview(showBackground = true, name = "Mining stage — animated")
@Composable
private fun MiningStagePreview() {
    FantasyTheme {
        val bus = remember { SceneEventBus() }
        LaunchedEffect(bus) {
            while (true) {
                kotlinx.coroutines.delay(1200L)
                bus.emit(SceneEvent.Produce(item = "copper_ore", fromTag = "target"))
            }
        }
        Stage(
            config = SceneCatalog.mining(pickaxeId = "bronze_pickaxe", oreNodeId = "copper_ore_node"),
            bus = bus,
        )
    }
}
```

- [ ] **Step 2: Verify it compiles**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Visually verify the preview**

Open Android Studio → preview pane for `Stage.kt` → start animation preview. Expected behaviour: pickaxe swings continuously; every ~1.2s the ore node shakes briefly, a ✦ burst pops at its position, and a small ore sprite arcs upward and fades out. (Sprites resolve to tier-coloured placeholders if `copper_ore_node` isn't a real drawable yet — that's expected; it confirms the EntityIcon fallback works.)

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/fantasyidler/ui/scene/Stage.kt
git commit -m "feat(scene): event-driven effect overlays (Shake/Burst/ArcOut/Popup)"
```

---

## Task 10: SessionSceneSheet

**Files:**
- Create: `app/src/main/kotlin/com/fantasyidler/ui/scene/SessionSceneSheet.kt`

- [ ] **Step 1: Confirm `ChunkySheet` signature**

```bash
sed -n '25,45p' app/src/main/kotlin/com/fantasyidler/ui/components/foundation/ChunkySheet.kt
```

Confirm parameters: `open: Boolean`, `onDismiss: () -> Unit`, and a trailing `content: @Composable () -> Unit`. If the signature differs, adjust the call below accordingly.

- [ ] **Step 2: Create `SessionSceneSheet.kt`**

Create `app/src/main/kotlin/com/fantasyidler/ui/scene/SessionSceneSheet.kt`:

```kotlin
package com.fantasyidler.ui.scene

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fantasyidler.ui.components.foundation.ChunkySheet
import com.fantasyidler.ui.scene.adapter.SkillSceneAdapter
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

/**
 * The tap-to-expand sheet that hosts a Stage during an active session.
 *
 * Slice 1 wires this for Mining only — [framesFlow] is a stream of (items,
 * durationMs) pairs supplied by the caller (`SkillsViewModel.miningFramesFor(...)`
 * in the typical case). Each emission is one minute-frame.
 *
 * Closing and reopening the sheet creates a fresh [SceneEventBus]. The adapter
 * resumes from the current minute-frame index — no backlog replay.
 *
 * @param config The SceneConfig to host (e.g., SceneCatalog.mining(...)).
 * @param framesFlow Sequential per-minute frames for the current session. Each
 *                   pair is (item ids produced this minute, frame duration in
 *                   millis — usually 60_000). When the flow completes, the
 *                   sheet shows the "complete" state in [statsContent].
 * @param open Whether the sheet is currently open.
 * @param onDismiss Callback when the user dismisses (back press, drag-down).
 * @param statsContent Composable rendered below the Stage — typically a copy
 *                     of the banner's HP bars or countdown.
 */
@Composable
fun SessionSceneSheet(
    config: SceneConfig,
    framesFlow: suspend (suspend (items: List<String>, durationMs: Long) -> Unit) -> Unit,
    open: Boolean,
    onDismiss: () -> Unit,
    statsContent: @Composable () -> Unit,
) {
    if (!open) return

    val bus = remember { SceneEventBus() }
    val adapter = remember { SkillSceneAdapter(bus = bus, outputTag = "target") }

    // Drive the adapter from the supplied frames stream while the sheet is open.
    LaunchedEffect(bus, adapter) {
        coroutineScope {
            framesFlow { items, durationMs ->
                adapter.playFrame(items = items, durationMs = durationMs)
            }
            bus.emit(SceneEvent.SessionEnd(outcome = "complete"))
        }
    }

    ChunkySheet(open = open, onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Stage(config = config, bus = bus)
            Spacer(modifier = Modifier.height(8.dp))
            statsContent()
        }
    }
}
```

- [ ] **Step 3: Verify it compiles**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -10
```

If `ChunkySheet`'s real signature uses a different parameter order or extra params (e.g., `title` or a `modifier`), fix the call site and re-run.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/fantasyidler/ui/scene/SessionSceneSheet.kt
git commit -m "feat(scene): SessionSceneSheet ChunkySheet host"
```

---

## Task 11: Add 11 ore-node entity IDs to the manifest

**Files:**
- Modify: `scripts/generate_art_manifest.py`
- Modify: `ART_MANIFEST.md` (regenerated)
- Modify: `art_manifest.json` (regenerated)

- [ ] **Step 1: Locate where mining ores are derived in the manifest script**

```bash
grep -n "ore\|mining" scripts/generate_art_manifest.py | head -10
```

Find the function that emits the `Ores` category (likely iterating over `assets/data/skills/mining.json` or a constant list).

- [ ] **Step 2: Add an `ore_node` parallel category**

Edit `scripts/generate_art_manifest.py`. Where the script defines its category list (look for `categories = [` or a function that returns a list of `{name, description, ids}` dicts), add a new entry after the existing `Ores` entry:

```python
# 11 mining-scene targets (ore in the rock). One per ore type — the visual the
# pickaxe strikes. Distinct from <ore>.png which is the popped-out item.
SCENE_TARGETS = {
    "name": "Scene targets",
    "description": "Resource-node sprites struck during skill-action animations.",
    "ids": [
        "copper_ore_node",
        "tin_ore_node",
        "iron_ore_node",
        "coal_node",
        "silver_ore_node",
        "gold_ore_node",
        "mithril_ore_node",
        "adamantite_ore_node",
        "runite_ore_node",
        "platinum_ore_node",
        "rune_essence_node",
    ],
}
```

Then append `SCENE_TARGETS` to whatever list the script aggregates categories into.

If the script's structure is unfamiliar (it may build categories dynamically from the JSON data files), instead grep for the line that prints the final `total = ` count and add a literal `extra_categories: list[dict]` argument that gets unioned in.

- [ ] **Step 3: Run the manifest generator**

```bash
python3 scripts/generate_art_manifest.py 2>&1 | tail -10
```

Expected: writes `ART_MANIFEST.md` + `art_manifest.json` with the new category and total = 698.

- [ ] **Step 4: Verify audit picks them up**

```bash
python3 scripts/audit_art.py 2>&1 | grep -A1 "Scene targets"
```

Expected output: `=== Scene targets — 0/11 done ===` followed by the missing list.

- [ ] **Step 5: Commit**

```bash
git add scripts/generate_art_manifest.py ART_MANIFEST.md art_manifest.json
git commit -m "feat(manifest): add 11 ore-node scene-target entity IDs"
```

---

## Task 12: Wire tap-to-expand on Mining

**Files:**
- Modify: `app/src/main/kotlin/com/fantasyidler/ui/screen/skills/ActiveSessionBanner.kt`
- Modify: `app/src/main/kotlin/com/fantasyidler/ui/screen/SkillsScreen.kt`

- [ ] **Step 1: Add an `onTap` parameter to `ActiveSessionBanner`**

Edit `app/src/main/kotlin/com/fantasyidler/ui/screen/skills/ActiveSessionBanner.kt`. Add a new parameter to the `ActiveSessionBanner` signature (line 38–48), defaulting to null so existing call sites keep working:

```kotlin
@Composable
fun ActiveSessionBanner(
    skillName: String,
    skillEmoji: String,
    activityKey: String,
    endsAt: Long,
    completed: Boolean,
    onCollect: () -> Unit,
    onAbandon: () -> Unit,
    onDebugFinish: () -> Unit = {},
    onTapHero: (() -> Unit)? = null,   // NEW
    modifier: Modifier = Modifier,
) {
```

Inside the function body where `HeroBlock(...)` is built, wrap or modify the `leading` IconDisk so it becomes tappable when `onTapHero` is non-null. The simplest change is to add a `Modifier.clickable` on the IconDisk's wrapping Box. If `IconDisk` doesn't expose a click handler, wrap it:

```kotlin
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box

// ... inside HeroBlock leading = { ... }:
leading = {
    Box(
        modifier = if (onTapHero != null) Modifier.clickable { onTapHero() } else Modifier,
    ) {
        IconDisk(
            emoji      = skillEmoji.ifEmpty { "✨" },
            size       = tokens.spacing.xxl + tokens.spacing.xl,
            background = tokens.colors.primary.copy(alpha = 0.30f),
        )
    }
},
```

- [ ] **Step 2: Wire `SessionSceneSheet` into `SkillsScreen` for Mining sessions**

Edit `app/src/main/kotlin/com/fantasyidler/ui/screen/SkillsScreen.kt`. Find where `ActiveSessionBanner` is invoked. Locate the active-session block — it likely passes `skillName`, `endsAt`, etc. Around that block add:

```kotlin
import com.fantasyidler.ui.scene.SceneCatalog
import com.fantasyidler.ui.scene.SessionSceneSheet
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

// Inside the SkillsScreen composable body, alongside other remember state:
var sceneOpen by remember { mutableStateOf(false) }

// In the ActiveSessionBanner invocation, add:
ActiveSessionBanner(
    // ... existing args
    onTapHero = if (currentSkillKey == "mining") {
        { sceneOpen = true }
    } else null,
)

// At the end of the same composable scope, after ActiveSessionBanner:
if (currentSkillKey == "mining" && sceneOpen) {
    val pickaxeId = currentBestPickaxeId ?: "bronze_pickaxe"
    val oreNodeId = "${selectedOreId}_node"  // e.g., "copper_ore_node"

    SessionSceneSheet(
        config = SceneCatalog.mining(pickaxeId = pickaxeId, oreNodeId = oreNodeId),
        framesFlow = { onFrame ->
            // Drive one frame per real minute. Frame items come from the
            // already-simulated session — read them from the ViewModel.
            val frames = viewModel.currentMiningFrames()  // List<List<String>>
            for (items in frames) {
                onFrame(items, 60_000L)
            }
        },
        open = sceneOpen,
        onDismiss = { sceneOpen = false },
        statsContent = {
            // Mirror of the banner's countdown/items-this-minute, kept tight.
            Text(text = "Mining…", style = MaterialTheme.typography.bodyMedium)
        },
    )
}
```

If `currentSkillKey`, `currentBestPickaxeId`, `selectedOreId`, or `viewModel.currentMiningFrames()` don't exist on the SkillsScreen's existing model, do a small grep to find the equivalents:

```bash
grep -n "skillKey\|pickaxe\|miningFrames\|currentOre" \
  app/src/main/kotlin/com/fantasyidler/ui/screen/SkillsScreen.kt \
  app/src/main/kotlin/com/fantasyidler/ui/viewmodel/SkillsViewModel.kt
```

Adjust the references to match what's actually available. The goal is: detect that the active session is a mining one, derive a pickaxe id (default "bronze_pickaxe" if unknown), derive an ore-node id (selected ore + "_node"), and walk the session's per-minute frames feeding their `items` lists into `onFrame`.

If a method like `currentMiningFrames()` doesn't exist, add a minimal one to `SkillsViewModel`:

```kotlin
/** Returns the per-minute item-id lists for the currently active mining session, in order. */
fun currentMiningFrames(): List<List<String>> {
    val session = activeSession.value ?: return emptyList()
    return session.frames.map { frame -> frame.items.map { it.itemId } }
}
```

(The exact `frame.items` shape depends on the existing `SessionFrame` definition — adapt the field path accordingly.)

- [ ] **Step 3: Verify the app compiles**

```bash
./gradlew :app:assembleDebug 2>&1 | tail -15
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/fantasyidler/ui/screen/skills/ActiveSessionBanner.kt \
        app/src/main/kotlin/com/fantasyidler/ui/screen/SkillsScreen.kt \
        app/src/main/kotlin/com/fantasyidler/ui/viewmodel/SkillsViewModel.kt
git commit -m "feat(skills): tap-to-expand SessionSceneSheet for Mining sessions"
```

---

## Task 13: End-to-end manual verification

This task has no code changes — it's a verification gate before declaring Slice 1 done.

- [ ] **Step 1: Build a debug APK**

```bash
./gradlew :app:assembleDebug 2>&1 | tail -5
```

Expected: BUILD SUCCESSFUL. APK at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 2: Install on a device or emulator**

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

(If no device, push the branch and let the GitHub Actions release workflow produce a signed APK to download — see `.github/workflows/release.yml`.)

- [ ] **Step 3: Walk the user flow**

1. Launch the app.
2. Navigate to Skills → Mining.
3. Start a Mining session (any ore).
4. On the Skills screen, the `ActiveSessionBanner` shows the countdown as before.
5. Tap the leading hero icon (the IconDisk).
6. The `SessionSceneSheet` rises from the bottom.
7. Verify visible on the stage:
   - Pickaxe sprite on the right, swinging back and forth (idle Swing).
   - Ore-node sprite in the centre (or its tier-coloured placeholder if `<ore>_node.png` doesn't exist yet).
   - Every few seconds: the ore node shakes briefly, a ✦ burst appears at its position, and a small ore item arcs upward off the stage and fades.
8. Dismiss the sheet (back press or drag down). The banner is still showing the countdown.
9. Reopen the sheet. Animations restart from t=0 (no backlog replay).
10. Let the session complete. The sheet's stats area updates accordingly; the underlying banner shows the Collect button.

- [ ] **Step 4: Run the audit**

```bash
python3 scripts/audit_art.py 2>&1 | grep -E "Total|Scene targets"
```

Expected: `Scene targets — 0/11 done` and the headline total now reads `408/698` (or similar; the 19 new IDs from Slice 1 only include the 11 ore nodes — tree stumps and fishing_spot land in Slice 3).

- [ ] **Step 5: Final commit (if any docs need updating)**

If you touched anything outside the planned files during verification, commit those tidies:

```bash
git status --short
# review, then:
git add -p
git commit -m "chore(slice-1): verification tidies"
```

- [ ] **Step 6: Push the branch**

The plan executor should create the branch (`feat/sprite-animation-slice-1-mining`) before Task 1 if using subagent-driven execution; if using inline execution on the spec branch, just push at the end:

```bash
git push -u origin feat/sprite-animation-slice-1-mining
```

---

## What this slice deliberately does NOT do

- **No combat wiring** — `CombatHudCard` is untouched. Slice 2 plan handles it.
- **No other skill scenes** — only Mining's `SceneConfig` exists in `SceneCatalog`. Slices 3–4 add the other 12.
- **No sprite sheets / Coil / `SpriteSheetIcon`** — Slice 5 handles hero entities. Static `EntityIcon` + property animation only here.
- **No Compose-drawn primitives** (`SceneShapes.kt`) — Mining doesn't need any. Slice 4 introduces them when archetypes 4/5/6 land.
- **No sound** — `SfxBridge` is a Slice 6 concern.
- **No reduce-motion branch** — landed in Slice 6 alongside the perf pass; for Slice 1, idle animations always run.
- **No instrumented Compose UI tests** — Slice 6 (or later) decides whether to invest in that infra.

---

## Risk register

| Risk | Mitigation in this slice |
|---|---|
| `EntityIcon` lookups for `*_node` IDs return null and break layout | EntityIcon already falls back to a tier-coloured block — confirmed in §2 of the spec; verified in Task 13 step 3. |
| `currentMiningFrames()` shape doesn't match what `SkillsViewModel` exposes | Task 12 step 2 calls out grepping and adapting; do not paper over with a wrong cast. |
| `ChunkySheet` API differs from the call I assumed | Task 10 step 1 explicitly greps for the real signature before writing the call site. |
| Effect overlay positioning looks off-centre on real devices | Task 13 step 3 visual check is the gate; if positions feel wrong, adjust `LayerPosition.toAlignment()` mapping in Task 7's file. |
| Existing skills test infra absent — JUnit dep alone could break builds with strict variants | Task 1 step 4 validates the build configures before continuing. |
