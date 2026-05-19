package com.fantasyidler.ui.screen.skills.sheets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.fantasyidler.R
import com.fantasyidler.ui.components.foundation.ChunkySheet
import com.fantasyidler.ui.theme.fantasy.FantasyPreviewSurface
import com.fantasyidler.ui.theme.fantasy.LocalFantasyTokens

/**
 * Placeholder bottom sheet shown when a skill's flow hasn't been wired up
 * yet (e.g. the orchestrator's fallback for unhandled [com.fantasyidler.ui.viewmodel.SheetState]
 * cases). Keeps the user from being stuck on a half-implemented screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComingSoonSheet(
    onDismissRequest: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ChunkySheet(
        onDismissRequest = onDismissRequest,
        sheetState       = sheetState,
    ) {
        ComingSoonSheetBody()
    }
}

@Composable
internal fun ComingSoonSheetBody() {
    val tokens = LocalFantasyTokens.current
    Box(
        modifier         = Modifier
            .fillMaxWidth()
            .padding(vertical = tokens.spacing.xxl),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text  = stringResource(R.string.label_coming_soon),
            style = tokens.typography.bodyLarge,
            color = tokens.colors.onSurfaceMuted,
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewComingSoonSheet() {
    FantasyPreviewSurface {
        ComingSoonSheetBody()
    }
}
