package com.fantasyidler.ui.scene

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fantasyidler.ui.components.foundation.ChunkySheet
import com.fantasyidler.ui.scene.adapter.SkillSceneAdapter
import kotlinx.coroutines.coroutineScope

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
@OptIn(ExperimentalMaterial3Api::class)
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

    // ChunkySheet uses onDismissRequest (not onDismiss/open); the `if (!open) return`
    // guard above ensures this composable only enters the tree when open == true.
    ChunkySheet(onDismissRequest = onDismiss) {
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
