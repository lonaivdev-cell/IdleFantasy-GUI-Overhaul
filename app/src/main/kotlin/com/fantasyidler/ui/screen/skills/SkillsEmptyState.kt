package com.fantasyidler.ui.screen.skills

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.fantasyidler.R
import com.fantasyidler.ui.components.foundation.ChunkyButton
import com.fantasyidler.ui.components.foundation.ChunkyButtonVariant
import com.fantasyidler.ui.components.foundation.ChunkyCard
import com.fantasyidler.ui.components.foundation.ChunkyDialog
import com.fantasyidler.ui.theme.fantasy.FantasyPreviewSurface
import com.fantasyidler.ui.theme.fantasy.LocalFantasyTokens

/**
 * Pulse-animated skeleton list shown while [com.fantasyidler.ui.viewmodel.SkillsUiState.isLoading]
 * is true. Mirrors the 4–6 ChunkyCard rows the real list will render so the
 * layout doesn't reflow when data arrives.
 */
@Composable
fun SkillsLoadingState(modifier: Modifier = Modifier) {
    val tokens = LocalFantasyTokens.current
    val transition = rememberInfiniteTransition(label = "skills-loading")
    val pulse by transition.animateFloat(
        initialValue  = 0.55f,
        targetValue   = 1.0f,
        animationSpec = tokens.motion.idle,
        label         = "skills-loading-alpha",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = tokens.spacing.l, vertical = tokens.spacing.m),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.m),
    ) {
        repeat(5) {
            ChunkyCard(modifier = Modifier.alpha(pulse)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(tokens.spacing.xxl + tokens.spacing.l)
                            .alpha(pulse * 0.6f),
                    )
                    Spacer(Modifier.size(tokens.spacing.m))
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = 0.55f)
                                .height(tokens.spacing.l)
                                .alpha(pulse * 0.6f),
                        )
                        Spacer(Modifier.height(tokens.spacing.s))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(tokens.spacing.m)
                                .alpha(pulse * 0.4f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Shown when the skill list resolves to an empty set — usually only ever true
 * if the save file is corrupted or the game data didn't ship. The parchment
 * scroll glyph leans into the fantasy theme; the body text suggests the user
 * check their save.
 */
@Composable
fun SkillsEmptyState(modifier: Modifier = Modifier) {
    val tokens = LocalFantasyTokens.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = tokens.spacing.xl, vertical = tokens.spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text  = "📜",
            style = tokens.typography.displayLarge,
        )
        Spacer(Modifier.height(tokens.spacing.l))
        Text(
            text       = stringResource(R.string.skills_empty_title),
            style      = tokens.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color      = tokens.colors.onSurface,
            textAlign  = TextAlign.Center,
        )
        Spacer(Modifier.height(tokens.spacing.s))
        Text(
            text      = stringResource(R.string.skills_empty_desc),
            style     = tokens.typography.bodyMedium,
            color     = tokens.colors.onSurfaceMuted,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Error dialog with a retry button. Driven by orchestrator-local state so it
 * can be summoned independently of the view-model contract (which doesn't
 * currently surface load errors as state).
 */
@Composable
fun SkillsErrorDialog(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = LocalFantasyTokens.current
    ChunkyDialog(
        title            = stringResource(R.string.skills_error_title),
        onDismissRequest = onDismiss,
        body = {
            Text(
                text  = message,
                style = tokens.typography.bodyMedium,
                color = tokens.colors.onSurface,
            )
        },
        actions = {
            ChunkyButton(
                text    = stringResource(R.string.btn_cancel),
                onClick = onDismiss,
                variant = ChunkyButtonVariant.Secondary,
            )
            ChunkyButton(
                text    = stringResource(R.string.btn_retry),
                onClick = { onRetry(); onDismiss() },
                variant = ChunkyButtonVariant.Primary,
            )
        },
    )
}

@PreviewLightDark
@Composable
private fun PreviewSkillsLoadingState() {
    FantasyPreviewSurface {
        SkillsLoadingState()
    }
}

@PreviewLightDark
@Composable
private fun PreviewSkillsEmptyState() {
    FantasyPreviewSurface {
        SkillsEmptyState()
    }
}

@PreviewLightDark
@Composable
private fun PreviewSkillsErrorDialog() {
    FantasyPreviewSurface {
        SkillsErrorDialog(
            message   = "Couldn't load your save.",
            onRetry   = {},
            onDismiss = {},
        )
    }
}
