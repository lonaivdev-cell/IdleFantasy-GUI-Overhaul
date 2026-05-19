package com.fantasyidler.ui.screen.skills

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.fantasyidler.R
import com.fantasyidler.data.model.Skills
import com.fantasyidler.ui.components.foundation.ChunkyCard
import com.fantasyidler.ui.components.foundation.ClaimBadge
import com.fantasyidler.ui.components.foundation.IconDisk
import com.fantasyidler.ui.theme.fantasy.FantasyPreviewSurface
import com.fantasyidler.ui.theme.fantasy.LocalFantasyTokens
import com.fantasyidler.ui.viewmodel.xpProgressFraction
import com.fantasyidler.util.GameStrings
import com.fantasyidler.util.formatXp

/**
 * Per-skill row used by the main skills list. The level badge sits inside the
 * [IconDisk]; the XP bar lives below the title row. When the skill is
 * actively training the row highlights and the trailing slot becomes a
 * pulsing [ClaimBadge] instead of an XP readout.
 *
 * Accessibility: the row collapses into a single talk-back node combining
 * the skill name, level, and active-state.
 */
@Composable
fun SkillRow(
    skillKey: String,
    level: Int,
    xp: Long,
    isActive: Boolean,
    onClick: () -> Unit,
    toolEfficiency: Float = 1.0f,
    modifier: Modifier = Modifier,
) {
    val tokens   = LocalFantasyTokens.current
    val context  = LocalContext.current
    val name     = GameStrings.skillName(context, skillKey)
    val emoji    = GameStrings.skillEmoji(skillKey)
    val progress = xpProgressFraction(xp)
    val state    = if (isActive) stringResource(R.string.label_training)
                   else stringResource(R.string.label_xp_gained)

    ChunkyCard(
        onClick   = onClick,
        highlight = isActive,
        modifier  = modifier
            .padding(horizontal = tokens.spacing.l, vertical = tokens.spacing.s)
            .semantics(mergeDescendants = true) {
                contentDescription = "$name, level $level, ${xp.formatXp()} XP, $state"
            },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(tokens.spacing.xxl + tokens.spacing.l)) {
                IconDisk(
                    emoji      = emoji,
                    size       = tokens.spacing.xxl + tokens.spacing.l,
                    background = if (isActive) tokens.colors.primary.copy(alpha = 0.30f)
                                 else tokens.colors.primary.copy(alpha = 0.14f),
                )
                Text(
                    text       = level.toString(),
                    style      = tokens.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color      = tokens.colors.onSurface,
                    modifier   = Modifier
                        .align(Alignment.BottomEnd)
                        .background(
                            color = tokens.colors.surface,
                            shape = tokens.shapes.badge,
                        )
                        .padding(horizontal = tokens.spacing.s, vertical = tokens.spacing.xs),
                )
            }

            Spacer(Modifier.width(tokens.spacing.m + tokens.spacing.xs))

            Column(Modifier.weight(1f)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text(
                        text       = name,
                        style      = tokens.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color      = tokens.colors.onSurface,
                    )
                    if (isActive) {
                        ClaimBadge(text = stringResource(R.string.label_training))
                    } else {
                        Text(
                            text  = "${xp.formatXp()} XP",
                            style = tokens.typography.labelSmall,
                            color = tokens.colors.onSurfaceMuted,
                        )
                    }
                }
                Spacer(Modifier.height(tokens.spacing.s + tokens.spacing.xs))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(tokens.spacing.m)
                        .clip(RoundedCornerShape(tokens.spacing.s)),
                    color    = tokens.colors.primary,
                )
                if (toolEfficiency > 1.0f) {
                    Spacer(Modifier.height(tokens.spacing.xs))
                    Text(
                        text  = stringResource(R.string.skills_tool_bonus, "%.2f".format(toolEfficiency)),
                        style = tokens.typography.labelSmall,
                        color = tokens.colors.primary,
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewSkillRow() {
    FantasyPreviewSurface {
        SkillRow(
            skillKey = Skills.MINING,
            level    = 42,
            xp       = 125_000,
            isActive = false,
            onClick  = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewSkillRowActive() {
    FantasyPreviewSurface {
        SkillRow(
            skillKey       = Skills.WOODCUTTING,
            level          = 60,
            xp             = 273_742,
            isActive       = true,
            onClick        = {},
            toolEfficiency = 1.25f,
        )
    }
}
