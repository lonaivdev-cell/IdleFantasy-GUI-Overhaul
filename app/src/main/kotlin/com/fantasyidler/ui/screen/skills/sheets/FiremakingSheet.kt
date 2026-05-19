package com.fantasyidler.ui.screen.skills.sheets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.fantasyidler.R
import com.fantasyidler.data.json.LogData
import com.fantasyidler.ui.components.foundation.ChunkySheet
import com.fantasyidler.ui.screen.skills.ActivityDetailDialog
import com.fantasyidler.ui.screen.skills.ActivityRow
import com.fantasyidler.ui.theme.fantasy.FantasyPreviewSurface
import com.fantasyidler.ui.theme.fantasy.LocalFantasyTokens
import com.fantasyidler.util.GameStrings

/**
 * Firemaking bottom sheet. Lists logs currently in inventory; when none are
 * available shows an empty state inline.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiremakingSheet(
    availableLogs: Map<String, LogData>,
    isStarting: Boolean,
    hasActiveSession: Boolean,
    isQueueFull: Boolean,
    sessionDurationMs: Long,
    onSelect: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ChunkySheet(
        onDismissRequest = onDismissRequest,
        sheetState       = sheetState,
    ) {
        FiremakingSheetBody(
            availableLogs     = availableLogs,
            isStarting        = isStarting,
            hasActiveSession  = hasActiveSession,
            isQueueFull       = isQueueFull,
            sessionDurationMs = sessionDurationMs,
            onSelect          = onSelect,
        )
    }
}

@Composable
internal fun FiremakingSheetBody(
    availableLogs: Map<String, LogData>,
    isStarting: Boolean,
    hasActiveSession: Boolean,
    isQueueFull: Boolean,
    sessionDurationMs: Long,
    onSelect: (String) -> Unit,
) {
    val tokens  = LocalFantasyTokens.current
    val context = LocalContext.current
    var selectedKey by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text       = stringResource(R.string.label_choose_activity),
            style      = tokens.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color      = tokens.colors.onSurface,
        )
        Spacer(Modifier.height(tokens.spacing.s))
        Text(
            text  = stringResource(R.string.skill_firemaking_desc),
            style = tokens.typography.bodyMedium,
            color = tokens.colors.onSurfaceMuted,
        )
        if (sessionDurationMs > 0) {
            Spacer(Modifier.height(tokens.spacing.xs))
            Text(
                text  = stringResource(R.string.skills_session_duration, sessionDurationMs / 60_000),
                style = tokens.typography.labelSmall,
                color = tokens.colors.onSurfaceMuted,
            )
        }
        Spacer(Modifier.height(tokens.spacing.m))
        HorizontalDivider(color = tokens.colors.primary.copy(alpha = 0.18f))

        if (availableLogs.isEmpty()) {
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .padding(vertical = tokens.spacing.xxl),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text  = stringResource(R.string.skills_no_logs),
                    style = tokens.typography.bodyMedium,
                    color = tokens.colors.onSurfaceMuted,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(top = tokens.spacing.s),
            ) {
                availableLogs.entries
                    .sortedBy { it.value.levelRequired }
                    .forEach { (key, log) ->
                        ActivityRow(
                            name             = GameStrings.itemName(context, key),
                            detail           = stringResource(R.string.skills_log_desc, log.levelRequired, log.xpPerLog),
                            isStarting       = isStarting,
                            hasActiveSession = hasActiveSession,
                            isQueueFull      = isQueueFull,
                            onClick          = { selectedKey = key },
                        )
                    }
            }
        }
    }

    selectedKey?.let { key ->
        val log = availableLogs[key] ?: return@let
        ActivityDetailDialog(
            name             = GameStrings.itemName(context, key),
            detail           = stringResource(R.string.skills_log_desc, log.levelRequired, log.xpPerLog),
            description      = GameStrings.itemDesc(context, key),
            hasActiveSession = hasActiveSession,
            isQueueFull      = isQueueFull,
            onConfirm        = { onSelect(key) },
            onDismiss        = { selectedKey = null },
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewFiremakingSheetPopulated() {
    FantasyPreviewSurface {
        FiremakingSheetBody(
            availableLogs = mapOf(
                "logs"     to LogData(displayName = "Logs",     levelRequired = 1,  xpPerLog = 40),
                "oak_logs" to LogData(displayName = "Oak Logs", levelRequired = 15, xpPerLog = 60),
            ),
            isStarting        = false,
            hasActiveSession  = false,
            isQueueFull       = false,
            sessionDurationMs = 1_800_000L,
            onSelect          = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewFiremakingSheetEmpty() {
    FantasyPreviewSurface {
        FiremakingSheetBody(
            availableLogs     = emptyMap(),
            isStarting        = false,
            hasActiveSession  = false,
            isQueueFull       = false,
            sessionDurationMs = 1_800_000L,
            onSelect          = {},
        )
    }
}
