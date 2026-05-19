package com.fantasyidler.ui.screen.skills.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.fantasyidler.R
import com.fantasyidler.ui.components.foundation.ChunkyButton
import com.fantasyidler.ui.components.foundation.ChunkyButtonVariant
import com.fantasyidler.ui.components.foundation.ChunkySheet
import com.fantasyidler.ui.theme.fantasy.FantasyPreviewSurface
import com.fantasyidler.ui.theme.fantasy.LocalFantasyTokens

/**
 * Fishing bottom sheet. Single CTA — the catch table is implicit in the
 * player's level, so there's no activity picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FishingSheet(
    fishingLevel: Int,
    sessionDurationMs: Long,
    isStarting: Boolean,
    hasActiveSession: Boolean,
    isQueueFull: Boolean,
    onStart: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ChunkySheet(
        onDismissRequest = onDismissRequest,
        sheetState       = sheetState,
    ) {
        FishingSheetBody(
            fishingLevel      = fishingLevel,
            sessionDurationMs = sessionDurationMs,
            isStarting        = isStarting,
            hasActiveSession  = hasActiveSession,
            isQueueFull       = isQueueFull,
            onStart           = onStart,
        )
    }
}

@Composable
internal fun FishingSheetBody(
    fishingLevel: Int,
    sessionDurationMs: Long,
    isStarting: Boolean,
    hasActiveSession: Boolean,
    isQueueFull: Boolean,
    onStart: () -> Unit,
) {
    val tokens = LocalFantasyTokens.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text       = stringResource(R.string.skill_fishing_name),
            style      = tokens.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color      = tokens.colors.onSurface,
        )
        Spacer(Modifier.height(tokens.spacing.s))
        Text(
            text  = stringResource(R.string.skill_fishing_desc),
            style = tokens.typography.bodyMedium,
            color = tokens.colors.onSurfaceMuted,
        )
        Spacer(Modifier.height(tokens.spacing.xs))
        Text(
            text  = stringResource(R.string.skills_fishing_desc, fishingLevel),
            style = tokens.typography.bodyMedium,
            color = tokens.colors.onSurface,
        )
        if (sessionDurationMs > 0) {
            Spacer(Modifier.height(tokens.spacing.xs))
            Text(
                text  = stringResource(R.string.skills_session_duration, sessionDurationMs / 60_000),
                style = tokens.typography.labelSmall,
                color = tokens.colors.onSurfaceMuted,
            )
        }
        Spacer(Modifier.height(tokens.spacing.l))

        val cta = stringResource(
            if (hasActiveSession) R.string.skills_add_to_queue else R.string.btn_start_session
        )
        ChunkyButton(
            text     = if (isStarting) stringResource(R.string.skills_loading) else cta,
            onClick  = onStart,
            enabled  = !isStarting && !(hasActiveSession && isQueueFull),
            variant  = ChunkyButtonVariant.Primary,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = tokens.spacing.xxl + tokens.spacing.l),
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewFishingSheet() {
    FantasyPreviewSurface {
        FishingSheetBody(
            fishingLevel      = 45,
            sessionDurationMs = 3_600_000L,
            isStarting        = false,
            hasActiveSession  = false,
            isQueueFull       = false,
            onStart           = {},
        )
    }
}
