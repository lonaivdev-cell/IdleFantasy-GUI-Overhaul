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
import com.fantasyidler.data.json.AgilityCourseData
import com.fantasyidler.ui.components.foundation.ChunkySheet
import com.fantasyidler.ui.screen.skills.ActivityDetailDialog
import com.fantasyidler.ui.screen.skills.ActivityRow
import com.fantasyidler.ui.theme.fantasy.FantasyPreviewSurface
import com.fantasyidler.ui.theme.fantasy.LocalFantasyTokens
import com.fantasyidler.util.GameStrings

/**
 * Agility activity-selection bottom sheet. Lists courses sorted by level
 * required.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgilitySheet(
    courses: Map<String, AgilityCourseData>,
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
        AgilitySheetBody(
            courses           = courses,
            isStarting        = isStarting,
            hasActiveSession  = hasActiveSession,
            isQueueFull       = isQueueFull,
            sessionDurationMs = sessionDurationMs,
            onSelect          = onSelect,
        )
    }
}

@Composable
internal fun AgilitySheetBody(
    courses: Map<String, AgilityCourseData>,
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
            text  = stringResource(R.string.skill_agility_desc),
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
            courses.entries
                .sortedBy { it.value.levelRequired }
                .forEach { (key, course) ->
                    ActivityRow(
                        name             = course.displayName,
                        detail           = stringResource(
                            R.string.skills_course_detail,
                            course.levelRequired,
                            course.xpPerSuccess,
                        ),
                        isStarting       = isStarting,
                        hasActiveSession = hasActiveSession,
                        isQueueFull      = isQueueFull,
                        onClick          = { selectedKey = key },
                    )
                }
        }
    }

    selectedKey?.let { key ->
        val course = courses[key] ?: return@let
        ActivityDetailDialog(
            name             = course.displayName,
            detail           = stringResource(
                R.string.skills_course_detail,
                course.levelRequired,
                course.xpPerSuccess,
            ),
            description      = GameStrings.agilityCourseDesc(context, key),
            hasActiveSession = hasActiveSession,
            isQueueFull      = isQueueFull,
            onConfirm        = { onSelect(key) },
            onDismiss        = { selectedKey = null },
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewAgilitySheet() {
    FantasyPreviewSurface {
        AgilitySheetBody(
            courses = mapOf(
                "beginner_course" to AgilityCourseData(
                    name          = "beginner_course",
                    displayName   = "Beginner Course",
                    levelRequired = 1,
                    xpPerSuccess  = 30,
                ),
                "novice_course" to AgilityCourseData(
                    name          = "novice_course",
                    displayName   = "Novice Course",
                    levelRequired = 10,
                    xpPerSuccess  = 50,
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
