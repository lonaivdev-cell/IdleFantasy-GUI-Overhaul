package com.fantasyidler.ui.scene

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
        modifier = modifier.background(tokens.colors.surfaceVariant),
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
