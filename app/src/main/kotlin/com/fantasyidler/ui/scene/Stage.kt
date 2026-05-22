package com.fantasyidler.ui.scene

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
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
    val activeEffects = remember { mutableStateListOf<ActiveEffect>() }

    LaunchedEffect(bus) {
        bus.events.collectLatest { event ->
            val factory = config.eventMap[event::class] ?: return@collectLatest
            val newEffects = factory(event)
            val now = System.currentTimeMillis()
            newEffects.forEach { effect ->
                activeEffects.add(ActiveEffect(effect = effect, startedAt = now, id = kotlin.random.Random.nextLong()))
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

    Box(modifier = modifier.background(tokens.colors.surfaceVariant)) {
        config.backgroundEntityId?.let { bg ->
            EntityIcon(
                entityId = bg,
                size = 280.dp,
                modifier = Modifier.fillMaxSize().alpha(0.35f),
            )
        }

        for (layer in config.layers) {
            val shakeForTag = activeEffects
                .firstOrNull { it.effect is Effect.Shake && (it.effect as Effect.Shake).tag == layer.tag }
                ?.effect as? Effect.Shake
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
