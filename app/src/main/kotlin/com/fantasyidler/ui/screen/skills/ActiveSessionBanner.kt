package com.fantasyidler.ui.screen.skills

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.fantasyidler.BuildConfig
import com.fantasyidler.R
import com.fantasyidler.ui.components.foundation.ChunkyButton
import com.fantasyidler.ui.components.foundation.ChunkyButtonVariant
import com.fantasyidler.ui.components.foundation.ClaimBadge
import com.fantasyidler.ui.components.foundation.HeroBlock
import com.fantasyidler.ui.components.foundation.IconDisk
import com.fantasyidler.ui.theme.fantasy.FantasyPreviewSurface
import com.fantasyidler.ui.theme.fantasy.LocalFantasyTokens
import com.fantasyidler.util.toCountdown
import kotlinx.coroutines.delay

/**
 * Highlighted hero card shown at the top of the Skills list while a session
 * is in flight. Composed of [HeroBlock] + [IconDisk] + [ClaimBadge]; ticks
 * once a second to keep the countdown live. When the session completes the
 * trailing slot flips to a "Collect" badge and the bottom row becomes a
 * full-width collect button.
 */
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
    modifier: Modifier = Modifier,
) {
    val tokens = LocalFantasyTokens.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(endsAt) {
        while (System.currentTimeMillis() < endsAt) {
            delay(1_000L)
            now = System.currentTimeMillis()
        }
    }

    val activitySuffix = if (activityKey.isNotEmpty())
        activityKey.replace('_', ' ').replaceFirstChar { it.uppercase() }
    else ""
    val title = buildString {
        append(skillName)
        if (activitySuffix.isNotEmpty()) append(" — ").append(activitySuffix)
    }
    val collectLabel = stringResource(R.string.btn_collect_results)
    val countdownLabel = remember(now, endsAt) { endsAt.toCountdown() }

    HeroBlock(
        title    = title,
        subtitle = if (completed) stringResource(R.string.label_session_complete) else countdownLabel,
        leading  = {
            IconDisk(
                emoji      = skillEmoji.ifEmpty { "✨" },
                size       = tokens.spacing.xxl + tokens.spacing.xl,
                background = tokens.colors.primary.copy(alpha = 0.30f),
            )
        },
        trailing = if (completed) {
            { ClaimBadge(text = collectLabel) }
        } else null,
        content = {
            if (!completed) {
                Row {
                    ChunkyButton(
                        text    = stringResource(R.string.btn_abandon_session),
                        onClick = onAbandon,
                        variant = ChunkyButtonVariant.Ghost,
                        modifier = Modifier
                            .defaultMinSize(minHeight = tokens.spacing.xxl + tokens.spacing.l),
                    )
                    if (BuildConfig.DEBUG) {
                        Spacer(Modifier.width(tokens.spacing.m))
                        ChunkyButton(
                            text    = "[Debug] Finish Now",
                            onClick = onDebugFinish,
                            variant = ChunkyButtonVariant.Ghost,
                            modifier = Modifier
                                .defaultMinSize(minHeight = tokens.spacing.xxl + tokens.spacing.l),
                        )
                    }
                }
            } else {
                ChunkyButton(
                    text    = collectLabel,
                    onClick = onCollect,
                    variant = ChunkyButtonVariant.Primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = tokens.spacing.xxl + tokens.spacing.l),
                )
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = tokens.spacing.l, vertical = tokens.spacing.m),
    )
}

@PreviewLightDark
@Composable
private fun PreviewActiveSessionBannerRunning() {
    FantasyPreviewSurface {
        ActiveSessionBanner(
            skillName     = "Mining",
            skillEmoji    = "⛏",
            activityKey   = "iron_ore",
            endsAt        = System.currentTimeMillis() + 3_600_000L,
            completed     = false,
            onCollect     = {},
            onAbandon     = {},
            onDebugFinish = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewActiveSessionBannerComplete() {
    FantasyPreviewSurface {
        ActiveSessionBanner(
            skillName   = "Fishing",
            skillEmoji  = "🎣",
            activityKey = "",
            endsAt      = System.currentTimeMillis() - 1_000L,
            completed   = true,
            onCollect   = {},
            onAbandon   = {},
        )
    }
}
