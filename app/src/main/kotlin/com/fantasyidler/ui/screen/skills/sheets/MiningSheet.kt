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
import com.fantasyidler.data.json.OreData
import com.fantasyidler.ui.components.foundation.ChunkySheet
import com.fantasyidler.ui.screen.skills.ActivityDetailDialog
import com.fantasyidler.ui.screen.skills.ActivityRow
import com.fantasyidler.ui.theme.fantasy.FantasyPreviewSurface
import com.fantasyidler.ui.theme.fantasy.LocalFantasyTokens
import com.fantasyidler.util.GameStrings

/**
 * Mining activity-selection bottom sheet. Lists ores sorted by level required;
 * tapping a row opens an [ActivityDetailDialog] for confirmation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiningSheet(
    ores: Map<String, OreData>,
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
        MiningSheetBody(
            ores              = ores,
            isStarting        = isStarting,
            hasActiveSession  = hasActiveSession,
            isQueueFull       = isQueueFull,
            sessionDurationMs = sessionDurationMs,
            onSelect          = onSelect,
        )
    }
}

@Composable
internal fun MiningSheetBody(
    ores: Map<String, OreData>,
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
            text  = stringResource(R.string.skill_mining_desc),
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
            ores.entries
                .sortedBy { it.value.levelRequired }
                .forEach { (key, ore) ->
                    ActivityRow(
                        name             = GameStrings.itemName(context, key),
                        detail           = stringResource(R.string.skills_level_req_xp, ore.levelRequired, ore.xpPerOre),
                        isStarting       = isStarting,
                        hasActiveSession = hasActiveSession,
                        isQueueFull      = isQueueFull,
                        onClick          = { selectedKey = key },
                    )
                }
        }
    }

    selectedKey?.let { key ->
        val ore = ores[key] ?: return@let
        ActivityDetailDialog(
            name             = GameStrings.itemName(context, key),
            detail           = stringResource(R.string.skills_level_req_xp, ore.levelRequired, ore.xpPerOre),
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
private fun PreviewMiningSheet() {
    FantasyPreviewSurface {
        MiningSheetBody(
            ores = mapOf(
                "copper_ore"  to OreData(displayName = "Copper Ore",  levelRequired = 1,  xpPerOre = 5,  timePerOre = 4),
                "iron_ore"    to OreData(displayName = "Iron Ore",    levelRequired = 15, xpPerOre = 35, timePerOre = 5),
                "mithril_ore" to OreData(displayName = "Mithril Ore", levelRequired = 55, xpPerOre = 80, timePerOre = 8),
            ),
            isStarting        = false,
            hasActiveSession  = false,
            isQueueFull       = false,
            sessionDurationMs = 3_600_000L,
            onSelect          = {},
        )
    }
}
