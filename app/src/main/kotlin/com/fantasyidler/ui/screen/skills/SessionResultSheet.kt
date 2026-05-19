package com.fantasyidler.ui.screen.skills

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.fantasyidler.R
import com.fantasyidler.data.model.Skills
import com.fantasyidler.ui.components.foundation.ChunkyButton
import com.fantasyidler.ui.components.foundation.ChunkySheet
import com.fantasyidler.ui.theme.fantasy.FantasyPreviewSurface
import com.fantasyidler.ui.theme.fantasy.LocalFantasyTokens
import com.fantasyidler.ui.viewmodel.SessionResult
import com.fantasyidler.util.GameStrings
import com.fantasyidler.util.formatXp

/**
 * The "session ended" recap modal. Triggered when the view-model populates
 * [com.fantasyidler.ui.viewmodel.SkillsUiState.sessionResult]; auto-dismisses
 * via the supplied callback after the user taps Close.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionResultSheet(
    result: SessionResult,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ChunkySheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
    ) {
        SessionResultBody(result = result, onDismiss = onDismiss)
    }
}

@Composable
private fun SessionResultBody(
    result: SessionResult,
    onDismiss: () -> Unit,
) {
    val tokens  = LocalFantasyTokens.current
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text       = stringResource(R.string.label_session_results),
            style      = tokens.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color      = tokens.colors.onSurface,
        )
        Text(
            text  = GameStrings.skillName(context, result.skillName),
            style = tokens.typography.bodyMedium,
            color = tokens.colors.onSurfaceMuted,
        )
        Spacer(Modifier.height(tokens.spacing.l + tokens.spacing.xs))

        ResultRow(
            label      = stringResource(R.string.label_xp_gained),
            value      = "+${result.xpGained.formatXp()} XP",
            valueColor = tokens.colors.primary,
        )

        if (result.levelUps.isNotEmpty()) {
            Spacer(Modifier.height(tokens.spacing.m + tokens.spacing.xs))
            Text(
                text  = stringResource(R.string.label_level_ups),
                style = tokens.typography.labelSmall,
                color = tokens.colors.onSurfaceMuted,
            )
            result.levelUps.forEach { lvl ->
                Text(
                    text       = "  " + stringResource(R.string.skills_level_reached, lvl),
                    style      = tokens.typography.bodyMedium,
                    color      = tokens.colors.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        if (result.itemsGained.isNotEmpty()) {
            Spacer(Modifier.height(tokens.spacing.m + tokens.spacing.xs))
            Text(
                text  = stringResource(R.string.label_items_collected),
                style = tokens.typography.labelSmall,
                color = tokens.colors.onSurfaceMuted,
            )
            result.itemsGained.entries
                .sortedByDescending { it.value }
                .forEach { (key, qty) ->
                    ResultRow(
                        label      = GameStrings.itemName(context, key),
                        value      = "×$qty",
                        valueColor = tokens.colors.onSurface,
                    )
                }
        }

        Spacer(Modifier.height(tokens.spacing.xl))
        ChunkyButton(
            text    = stringResource(R.string.btn_close),
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ResultRow(
    label: String,
    value: String,
    valueColor: Color,
) {
    val tokens = LocalFantasyTokens.current
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = tokens.spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text  = label,
            style = tokens.typography.bodyMedium,
            color = tokens.colors.onSurface,
        )
        Text(
            text       = value,
            style      = tokens.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color      = valueColor,
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewSessionResultSheetSimple() {
    FantasyPreviewSurface {
        SessionResultBody(
            result = SessionResult(
                skillName    = Skills.MINING,
                xpGained     = 1_240,
                itemsGained  = mapOf("iron_ore" to 18, "coal" to 6),
                levelUps     = emptyList(),
            ),
            onDismiss = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewSessionResultSheetLevelUp() {
    FantasyPreviewSurface {
        SessionResultBody(
            result = SessionResult(
                skillName    = Skills.WOODCUTTING,
                xpGained     = 4_350,
                itemsGained  = mapOf("yew_logs" to 24),
                levelUps     = listOf(58, 59),
            ),
            onDismiss = {},
        )
    }
}
