package com.fantasyidler.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fantasyidler.R
import com.fantasyidler.ui.screen.CharacterSetupSheet
import com.fantasyidler.ui.viewmodel.SessionSummary
import com.fantasyidler.util.formatCoins

/**
 * The four global flows that used to live inside HomeScreen.kt and only fired
 * when the player was on the Home tab. Hoisted to the root so they fire from
 * any destination:
 *
 *  - Session-summary dialog (after a Claim).
 *  - "What's new" dialog (first launch after an upgrade).
 *  - Character-setup sheet (first launch — blocking).
 *  - (Reserved) pending-collect surfacing — currently routed through the
 *    HUD's session pill which calls collectSession() when there are
 *    pending sessions.
 *
 * Stateless: the caller (AppNavigation) owns the state + callbacks.
 */
@Composable
fun GlobalGameOverlay(
    sessionSummary: SessionSummary?,
    showWhatsNew: Boolean,
    characterSetupDone: Boolean,
    characterName: String,
    onSummaryConsumed: () -> Unit,
    onDismissWhatsNew: () -> Unit,
    onSaveCharacter: (name: String, gender: String, race: String) -> Unit,
    onDismissCharacterSetup: () -> Unit,
) {
    sessionSummary?.let { summary ->
        SessionSummaryDialog(summary, onSummaryConsumed)
    }

    if (showWhatsNew) {
        WhatsNewDialog(onDismissWhatsNew)
    }

    if (!characterSetupDone) {
        CharacterSetupSheet(
            isFirstTime = true,
            initialName = characterName,
            onSave      = onSaveCharacter,
            onDismiss   = onDismissCharacterSetup,
        )
    }
}

@Composable
private fun SessionSummaryDialog(
    summary: SessionSummary,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = summary.title, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier            = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (summary.died) {
                    Text(
                        text  = stringResource(R.string.home_died_message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                if (summary.boostWasActive) {
                    Text(
                        text  = stringResource(R.string.home_xp_boost_was_active),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                if (summary.xpLines.isNotEmpty()) {
                    SummarySection(stringResource(R.string.label_xp_gained))
                    summary.xpLines.forEach { (skill, label) -> SummaryRow(skill, label) }
                } else if (summary.totalXpLabel.isNotEmpty()) {
                    SummaryRow(stringResource(R.string.label_xp_gained), summary.totalXpLabel)
                }
                if (summary.killLines.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    SummarySection(stringResource(R.string.label_kills))
                    summary.killLines.forEach { (enemy, kills) -> SummaryRow(enemy, kills) }
                }
                if (summary.itemLines.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    SummarySection(stringResource(R.string.home_loot))
                    summary.itemLines.forEach { (item, qty) -> SummaryRow(item, qty) }
                }
                if (summary.coinsGained > 0) {
                    SummaryRow("Coins", "+${summary.coinsGained.formatCoins()}")
                }
                if (summary.foodConsumedLines.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    SummarySection(stringResource(R.string.home_food_consumed))
                    summary.foodConsumedLines.forEach { (food, qty) -> SummaryRow(food, qty) }
                }
                if (summary.boneBuriedLabel.isNotEmpty()) {
                    SummaryRow(stringResource(R.string.home_bones_buried), summary.boneBuriedLabel)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.btn_close))
            }
        },
    )
}

@Composable
private fun WhatsNewDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val changelogText = remember {
        runCatching { context.assets.open("changelog.txt").bufferedReader().readText().trim() }
            .getOrElse { "" }
    }
    if (changelogText.isEmpty()) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_whats_new)) },
        text  = {
            Text(text = changelogText, style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.home_got_it)) }
        },
    )
}

@Composable
private fun SummarySection(title: String) {
    Text(
        text  = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            text       = value,
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
