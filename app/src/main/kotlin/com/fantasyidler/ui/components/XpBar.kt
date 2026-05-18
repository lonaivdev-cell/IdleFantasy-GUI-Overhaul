package com.fantasyidler.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun XpBar(
    progress: Float,
    modifier: Modifier = Modifier,
    label: String? = null,
    valueText: String? = null,
) {
    val clamped = progress.coerceIn(0f, 1f)
    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null || valueText != null) {
            Row(modifier = Modifier.fillMaxWidth()) {
                if (label != null) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (valueText != null) {
                    Text(
                        text = valueText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
        }
        LinearProgressIndicator(
            progress = { clamped },
            modifier = Modifier.fillMaxWidth().height(6.dp),
        )
    }
}
