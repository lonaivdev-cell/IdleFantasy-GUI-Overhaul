package com.fantasyidler.ui.screen.skills.sheets

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.fantasyidler.R
import com.fantasyidler.data.json.TreeData
import com.fantasyidler.ui.components.foundation.ChunkySheet
import com.fantasyidler.ui.screen.skills.ActivityDetailDialog
import com.fantasyidler.ui.screen.skills.ActivityRow
import com.fantasyidler.ui.theme.fantasy.FantasyPreviewSurface
import com.fantasyidler.ui.theme.fantasy.LocalFantasyTokens
import com.fantasyidler.util.GameStrings

/**
 * Woodcutting activity-selection bottom sheet. Lists trees sorted by level
 * required; tapping opens a confirmation [ActivityDetailDialog].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WoodcuttingSheet(
    trees: Map<String, TreeData>,
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
        WoodcuttingSheetBody(
            trees             = trees,
            isStarting        = isStarting,
            hasActiveSession  = hasActiveSession,
            isQueueFull       = isQueueFull,
            sessionDurationMs = sessionDurationMs,
            onSelect          = onSelect,
        )
    }
}

@Composable
internal fun WoodcuttingSheetBody(
    trees: Map<String, TreeData>,
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
            text  = stringResource(R.string.skill_woodcutting_desc),
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

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(top = tokens.spacing.s),
        ) {
            trees.entries
                .sortedBy { it.value.levelRequired }
                .forEach { (key, tree) ->
                    ActivityRow(
                        name             = GameStrings.itemName(context, tree.logName),
                        detail           = stringResource(R.string.skills_log_desc, tree.levelRequired, tree.xpPerLog),
                        isStarting       = isStarting,
                        hasActiveSession = hasActiveSession,
                        isQueueFull      = isQueueFull,
                        onClick          = { selectedKey = key },
                    )
                }
        }
    }

    selectedKey?.let { key ->
        val tree = trees[key] ?: return@let
        ActivityDetailDialog(
            name             = GameStrings.itemName(context, tree.logName),
            detail           = stringResource(R.string.skills_log_desc, tree.levelRequired, tree.xpPerLog),
            description      = GameStrings.itemDesc(context, tree.logName),
            hasActiveSession = hasActiveSession,
            isQueueFull      = isQueueFull,
            onConfirm        = { onSelect(key) },
            onDismiss        = { selectedKey = null },
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewWoodcuttingSheet() {
    FantasyPreviewSurface {
        WoodcuttingSheetBody(
            trees = mapOf(
                "regular_tree" to TreeData(
                    displayName    = "Tree",
                    logName        = "logs",
                    logDisplayName = "Logs",
                    levelRequired  = 1,
                    xpPerLog       = 25,
                    timePerLog     = 3,
                ),
                "oak_tree" to TreeData(
                    displayName    = "Oak Tree",
                    logName        = "oak_logs",
                    logDisplayName = "Oak Logs",
                    levelRequired  = 15,
                    xpPerLog       = 38,
                    timePerLog     = 4,
                ),
            ),
            isStarting        = false,
            hasActiveSession  = false,
            isQueueFull       = false,
            sessionDurationMs = 3_600_000L,
            onSelect          = {},
        )
    }
}
