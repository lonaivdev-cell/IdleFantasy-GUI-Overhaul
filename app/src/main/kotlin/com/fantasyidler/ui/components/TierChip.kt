package com.fantasyidler.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fantasyidler.ui.theme.TierAdamant
import com.fantasyidler.ui.theme.TierBronze
import com.fantasyidler.ui.theme.TierDragon
import com.fantasyidler.ui.theme.TierIron
import com.fantasyidler.ui.theme.TierMithril
import com.fantasyidler.ui.theme.TierRune
import com.fantasyidler.ui.theme.TierSteel

enum class Tier { BRONZE, IRON, STEEL, MITHRIL, ADAMANT, RUNE, DRAGON }

fun Tier.color(): Color = when (this) {
    Tier.BRONZE -> TierBronze
    Tier.IRON -> TierIron
    Tier.STEEL -> TierSteel
    Tier.MITHRIL -> TierMithril
    Tier.ADAMANT -> TierAdamant
    Tier.RUNE -> TierRune
    Tier.DRAGON -> TierDragon
}

fun tierFromKey(key: String): Tier? = when {
    key.contains("bronze", ignoreCase = true) -> Tier.BRONZE
    key.contains("iron", ignoreCase = true) -> Tier.IRON
    key.contains("steel", ignoreCase = true) -> Tier.STEEL
    key.contains("mithril", ignoreCase = true) -> Tier.MITHRIL
    key.contains("adamant", ignoreCase = true) -> Tier.ADAMANT
    key.contains("rune", ignoreCase = true) -> Tier.RUNE
    key.contains("dragon", ignoreCase = true) -> Tier.DRAGON
    else -> null
}

@Composable
fun TierChip(
    tier: Tier,
    label: String,
    modifier: Modifier = Modifier,
) {
    val tint = tier.color()
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = tint.copy(alpha = 0.18f),
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = tint,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
