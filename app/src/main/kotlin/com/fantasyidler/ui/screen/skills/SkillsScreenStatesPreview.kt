package com.fantasyidler.ui.screen.skills

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.fantasyidler.R
import com.fantasyidler.data.model.Skills
import com.fantasyidler.ui.components.SectionHeader
import com.fantasyidler.ui.theme.fantasy.FantasyPreviewSurface
import com.fantasyidler.ui.theme.fantasy.LocalFantasyTokens

/**
 * Preview-only fixtures + composites so designers can scrub the three
 * top-level Skills screen states (loading / empty / populated) without
 * running the app or wiring a Hilt view-model. Mirrors what the orchestrator
 * mounts at runtime.
 */
private data class PreviewSkillsState(
    val isLoading: Boolean,
    val skillLevels: Map<String, Int>,
    val skillXp: Map<String, Long>,
    val activeSkill: String? = null,
)

private val PopulatedFixture = PreviewSkillsState(
    isLoading   = false,
    skillLevels = mapOf(
        Skills.MINING      to 42,
        Skills.FISHING     to 35,
        Skills.WOODCUTTING to 58,
        Skills.FARMING     to 12,
        Skills.FIREMAKING  to 28,
        Skills.AGILITY     to 19,
        Skills.PRAYER      to 22,
    ),
    skillXp = mapOf(
        Skills.MINING      to 125_000L,
        Skills.FISHING     to 80_000L,
        Skills.WOODCUTTING to 273_742L,
        Skills.FARMING     to 8_000L,
        Skills.FIREMAKING  to 42_000L,
        Skills.AGILITY     to 14_500L,
        Skills.PRAYER      to 19_300L,
    ),
    activeSkill = Skills.WOODCUTTING,
)

@Composable
private fun PreviewSkillsBody(state: PreviewSkillsState) {
    val tokens = LocalFantasyTokens.current
    Column(modifier = Modifier.fillMaxWidth()) {
        if (state.activeSkill != null) {
            ActiveSessionBanner(
                skillName     = state.activeSkill.replaceFirstChar { it.uppercase() },
                skillEmoji    = "🪓",
                activityKey   = "yew_tree",
                endsAt        = System.currentTimeMillis() + 600_000L,
                completed     = false,
                onCollect     = {},
                onAbandon     = {},
                onDebugFinish = {},
            )
            Spacer(Modifier.height(tokens.spacing.m))
        }
        SectionHeader(stringResource(R.string.label_gathering_skills))
        Skills.GATHERING.forEach { key ->
            SkillRow(
                skillKey = key,
                level    = state.skillLevels[key] ?: 1,
                xp       = state.skillXp[key] ?: 0L,
                isActive = state.activeSkill == key,
                onClick  = {},
            )
        }
        SectionHeader(stringResource(R.string.label_prayer))
        SkillRow(
            skillKey = Skills.PRAYER,
            level    = state.skillLevels[Skills.PRAYER] ?: 1,
            xp       = state.skillXp[Skills.PRAYER] ?: 0L,
            isActive = state.activeSkill == Skills.PRAYER,
            onClick  = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewSkillsScreenLoading() {
    FantasyPreviewSurface {
        SkillsLoadingState()
    }
}

@PreviewLightDark
@Composable
private fun PreviewSkillsScreenEmpty() {
    FantasyPreviewSurface {
        SkillsEmptyState()
    }
}

@PreviewLightDark
@Composable
private fun PreviewSkillsScreenPopulated() {
    FantasyPreviewSurface {
        PreviewSkillsBody(PopulatedFixture)
    }
}

@PreviewLightDark
@Composable
private fun PreviewSkillsScreenError() {
    FantasyPreviewSurface {
        SkillsErrorDialog(
            message   = stringResource(R.string.skills_error_desc),
            onRetry   = {},
            onDismiss = {},
        )
    }
}
