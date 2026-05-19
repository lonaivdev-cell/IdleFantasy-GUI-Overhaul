package com.fantasyidler.ui.screen.skills

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.fantasyidler.R
import com.fantasyidler.ui.components.foundation.ChunkyButton
import com.fantasyidler.ui.components.foundation.ChunkyButtonVariant
import com.fantasyidler.ui.components.foundation.ChunkyCard
import com.fantasyidler.ui.components.foundation.ChunkyDialog
import com.fantasyidler.ui.components.foundation.ClaimBadge
import com.fantasyidler.ui.theme.fantasy.FantasyPreviewSurface
import com.fantasyidler.ui.theme.fantasy.LocalFantasyTokens

/**
 * One row inside an activity-selection sheet. Wraps [ChunkyCard] with the
 * activity's name + detail line and a trailing [ClaimBadge] CTA. When a
 * session is already running the CTA flips to "Add to Queue"; when starting
 * is in flight the card is disabled and the badge becomes a spinner.
 *
 * Accessibility: the whole card collapses into a single talk-back node via
 * `semantics(mergeDescendants = true)` so the user hears one combined label
 * instead of name, detail, badge as three separate utterances.
 */
@Composable
fun ActivityRow(
    name: String,
    detail: String,
    isStarting: Boolean,
    hasActiveSession: Boolean,
    isQueueFull: Boolean,
    onClick: () -> Unit,
) {
    val tokens = LocalFantasyTokens.current
    val queueBlocked = hasActiveSession && isQueueFull
    val cta = stringResource(
        if (hasActiveSession) R.string.skills_add_to_queue else R.string.btn_start_session
    )

    ChunkyCard(
        onClick  = onClick,
        enabled  = !isStarting && !queueBlocked,
        modifier = Modifier
            .padding(horizontal = tokens.spacing.l, vertical = tokens.spacing.s)
            .semantics(mergeDescendants = true) {
                contentDescription = "$name. $detail. $cta"
            },
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
            modifier              = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text       = name,
                    style      = tokens.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = tokens.colors.onSurface,
                )
                Spacer(Modifier.height(tokens.spacing.xs))
                Text(
                    text  = detail,
                    style = tokens.typography.bodyMedium,
                    color = tokens.colors.onSurfaceMuted,
                )
            }
            Spacer(Modifier.width(tokens.spacing.m))
            if (isStarting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(tokens.spacing.l),
                    color    = tokens.colors.primary,
                )
            } else {
                ClaimBadge(
                    text  = cta,
                    pulse = !hasActiveSession,
                    color = if (queueBlocked) tokens.colors.surfaceVariant else tokens.colors.primary,
                )
            }
        }
    }
}

/**
 * Confirmation dialog shown after picking an activity. Uses [ChunkyDialog]
 * for the gold-bordered chunky surface; the confirm button text switches to
 * "Add to Queue" when a session is already running.
 */
@Composable
fun ActivityDetailDialog(
    name: String,
    detail: String,
    description: String,
    hasActiveSession: Boolean,
    isQueueFull: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = LocalFantasyTokens.current
    val queueBlocked = hasActiveSession && isQueueFull
    val confirmLabel = stringResource(
        if (hasActiveSession) R.string.skills_add_queue_short else R.string.btn_start_session
    )

    ChunkyDialog(
        title            = name,
        onDismissRequest = onDismiss,
        body = {
            Column {
                Text(
                    text  = detail,
                    style = tokens.typography.bodyMedium,
                    color = tokens.colors.onSurfaceMuted,
                )
                if (description.isNotBlank()) {
                    Spacer(Modifier.height(tokens.spacing.m))
                    Text(
                        text  = description,
                        style = tokens.typography.bodyMedium,
                        color = tokens.colors.onSurface,
                    )
                }
            }
        },
        actions = {
            ChunkyButton(
                text    = stringResource(R.string.btn_cancel),
                onClick = onDismiss,
                variant = ChunkyButtonVariant.Secondary,
            )
            ChunkyButton(
                text    = confirmLabel,
                onClick = { onConfirm(); onDismiss() },
                enabled = !queueBlocked,
                variant = ChunkyButtonVariant.Primary,
            )
        },
    )
}

@PreviewLightDark
@Composable
private fun PreviewActivityRow() {
    FantasyPreviewSurface {
        ActivityRow(
            name             = "Iron Ore",
            detail           = "Lv. 15  •  35 XP/ore",
            isStarting       = false,
            hasActiveSession = false,
            isQueueFull      = false,
            onClick          = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewActivityRowStarting() {
    FantasyPreviewSurface {
        ActivityRow(
            name             = "Iron Ore",
            detail           = "Lv. 15  •  35 XP/ore",
            isStarting       = true,
            hasActiveSession = false,
            isQueueFull      = false,
            onClick          = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewActivityRowQueue() {
    FantasyPreviewSurface {
        ActivityRow(
            name             = "Mithril Ore",
            detail           = "Lv. 55  •  80 XP/ore",
            isStarting       = false,
            hasActiveSession = true,
            isQueueFull      = false,
            onClick          = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewActivityDetailDialog() {
    FantasyPreviewSurface {
        ActivityDetailDialog(
            name             = "Iron Ore",
            detail           = "Lv. 15  •  35 XP/ore",
            description      = "A common ore that smelts into iron bars.",
            hasActiveSession = false,
            isQueueFull      = false,
            onConfirm        = {},
            onDismiss        = {},
        )
    }
}
