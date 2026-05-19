package com.fantasyidler.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.hilt.navigation.compose.hiltViewModel
import com.fantasyidler.R
import com.fantasyidler.ui.components.EmptyState
import com.fantasyidler.ui.components.SectionHeader
import com.fantasyidler.ui.components.foundation.BigStepper
import com.fantasyidler.ui.components.foundation.ChunkyButton
import com.fantasyidler.ui.components.foundation.ChunkyButtonVariant
import com.fantasyidler.ui.components.foundation.ChunkyCard
import com.fantasyidler.ui.components.foundation.ChunkySheet
import com.fantasyidler.ui.components.foundation.ClaimBadge
import com.fantasyidler.ui.components.foundation.EntityIconDisk
import com.fantasyidler.ui.theme.fantasy.FantasyPreviewSurface
import com.fantasyidler.ui.theme.fantasy.LocalFantasyTokens
import com.fantasyidler.ui.viewmodel.ShopEntry
import com.fantasyidler.ui.viewmodel.ShopTransaction
import com.fantasyidler.ui.viewmodel.ShopViewModel
import com.fantasyidler.util.GameStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    onBack: () -> Unit,
    viewModel: ShopViewModel = hiltViewModel(),
) {
    val tokens            = LocalFantasyTokens.current
    val state             by viewModel.uiState.collectAsState()
    val context           = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.snackbarConsumed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text  = stringResource(R.string.label_shop),
                        style = tokens.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back),
                            tint               = tokens.colors.primary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor   = tokens.colors.surface,
                    titleContentColor = tokens.colors.onSurface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        var subTab by remember { mutableIntStateOf(0) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ShopTabRow(subTab) { subTab = it }
            when {
                state.isLoading -> ShopLoading()
                subTab == 0 -> BuyList(
                    entries       = viewModel.buyEntries,
                    coins         = state.coins,
                    xpBoostActive = state.xpBoostActive,
                    onBuy         = viewModel::openBuy,
                )
                else -> SellList(
                    inventory          = state.inventory,
                    equipped           = state.equipped,
                    context            = context,
                    priceFor           = viewModel::sellPriceFor,
                    categoryFor        = viewModel::sellCategoryFor,
                    onSell             = { key -> viewModel.openSell(key, GameStrings.itemName(context, key)) },
                    onSellJunk         = viewModel::sellJunk,
                    onSellOldEquipment = viewModel::sellOldEquipment,
                )
            }
        }
    }

    state.transaction?.let { t ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ChunkySheet(
            onDismissRequest = viewModel::dismissTransaction,
            sheetState       = sheetState,
        ) {
            TransactionSheet(
                transaction = t,
                coins       = state.coins,
                onSetQty    = viewModel::setTransactionQty,
                onConfirm   = viewModel::confirmTransaction,
                onDismiss   = viewModel::dismissTransaction,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Tabs
// ---------------------------------------------------------------------------

@Composable
private fun ShopTabRow(selected: Int, onSelect: (Int) -> Unit) {
    val tokens = LocalFantasyTokens.current
    TabRow(
        selectedTabIndex = selected,
        containerColor   = tokens.colors.surface,
        contentColor     = tokens.colors.primary,
    ) {
        Tab(
            selected = selected == 0,
            onClick  = { onSelect(0) },
            text     = { Text(stringResource(R.string.btn_buy),  style = tokens.typography.labelSmall) },
        )
        Tab(
            selected = selected == 1,
            onClick  = { onSelect(1) },
            text     = { Text(stringResource(R.string.btn_sell), style = tokens.typography.labelSmall) },
        )
    }
}

// ---------------------------------------------------------------------------
// Buy list
// ---------------------------------------------------------------------------

@Composable
private fun BuyList(
    entries: List<ShopEntry>,
    coins: Long,
    xpBoostActive: Boolean,
    onBuy: (ShopEntry) -> Unit,
) {
    val tokens  = LocalFantasyTokens.current
    val grouped = remember(entries) { entries.groupBy { it.categoryName } }

    if (entries.isEmpty()) {
        ShopEmpty(
            title = stringResource(R.string.shop_empty_buy_title),
            desc  = stringResource(R.string.shop_empty_buy_desc),
        )
        return
    }

    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(horizontal = tokens.spacing.l, vertical = tokens.spacing.m + tokens.spacing.s),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.m + tokens.spacing.xs),
    ) {
        grouped.forEach { (category, categoryEntries) ->
            item(key = "hdr_$category") {
                Box(modifier = Modifier.padding(top = tokens.spacing.s, bottom = tokens.spacing.xs)) {
                    SectionHeader(category)
                }
            }
            items(categoryEntries, key = { it.key }) { entry ->
                BuyRow(entry = entry, coins = coins, xpBoostActive = xpBoostActive, onBuy = onBuy)
            }
        }
        item { Spacer(Modifier.height(tokens.spacing.l)) }
    }
}

@Composable
private fun BuyRow(
    entry: ShopEntry,
    coins: Long,
    xpBoostActive: Boolean,
    onBuy: (ShopEntry) -> Unit,
) {
    val tokens        = LocalFantasyTokens.current
    val canAfford     = coins >= entry.price
    val isXpBoost     = entry.key == ShopViewModel.XP_BOOST_KEY
    val isActiveBoost = isXpBoost && xpBoostActive
    val dimColor      = tokens.colors.onSurface.copy(alpha = 0.38f)

    ChunkyCard(
        onClick   = { onBuy(entry) },
        highlight = isActiveBoost,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EntityIconDisk(
                entityId           = entry.key,
                contentDescription = entry.displayName,
                size               = tokens.spacing.xxl + tokens.spacing.m + tokens.spacing.xs,
            )
            Spacer(Modifier.width(tokens.spacing.m + tokens.spacing.s))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text       = entry.displayName,
                        style      = tokens.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color      = if (canAfford) tokens.colors.onSurface else dimColor,
                    )
                    if (isActiveBoost) {
                        Spacer(Modifier.width(tokens.spacing.s + tokens.spacing.xs))
                        ClaimBadge(text = stringResource(R.string.shop_active), pulse = true)
                    }
                }
                if (entry.description.isNotBlank()) {
                    Text(
                        text  = entry.description,
                        style = tokens.typography.bodyMedium,
                        color = tokens.colors.onSurfaceMuted.copy(alpha = if (canAfford) 1f else 0.5f),
                    )
                }
            }
            Spacer(Modifier.width(tokens.spacing.m))
            PricePill(price = entry.price.toLong(), enabled = canAfford)
        }
    }
}

// ---------------------------------------------------------------------------
// Sell list
// ---------------------------------------------------------------------------

private val SELL_CATEGORY_ORDER = listOf("Weapons", "Armor", "Tools", "Food", "Materials", "Misc")

@Composable
private fun SellList(
    inventory: Map<String, Int>,
    equipped: Map<String, String?>,
    context: android.content.Context,
    priceFor: (String) -> Int,
    categoryFor: (String) -> String,
    onSell: (String) -> Unit,
    onSellJunk: () -> Unit,
    onSellOldEquipment: () -> Unit,
) {
    val tokens  = LocalFantasyTokens.current
    val grouped = remember(inventory) {
        inventory.entries
            .groupBy { categoryFor(it.key) }
            .entries
            .sortedBy { SELL_CATEGORY_ORDER.indexOf(it.key).let { i -> if (i < 0) Int.MAX_VALUE else i } }
    }

    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(horizontal = tokens.spacing.l, vertical = tokens.spacing.m + tokens.spacing.s),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.m + tokens.spacing.xs),
    ) {
        item {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.m),
            ) {
                ChunkyButton(
                    text     = stringResource(R.string.shop_sell_junk),
                    onClick  = onSellJunk,
                    variant  = ChunkyButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                )
                ChunkyButton(
                    text     = stringResource(R.string.shop_sell_old_gear),
                    onClick  = onSellOldEquipment,
                    variant  = ChunkyButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (inventory.isEmpty()) {
            item {
                ShopEmpty(
                    title = stringResource(R.string.shop_empty_sell_title),
                    desc  = stringResource(R.string.shop_empty_sell_desc),
                )
            }
        } else {
            grouped.forEach { (category, entries) ->
                item(key = "sell_hdr_$category") {
                    Box(modifier = Modifier.padding(top = tokens.spacing.s, bottom = tokens.spacing.xs)) {
                        SectionHeader(category)
                    }
                }
                items(entries, key = { it.key }) { (key, qty) ->
                    val sellPrice   = priceFor(key)
                    val isEquipped  = equipped.values.any { it == key }
                    val displayName = GameStrings.itemName(context, key)

                    ChunkyCard(onClick = { onSell(key) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            EntityIconDisk(
                                entityId           = key,
                                contentDescription = displayName,
                                size               = tokens.spacing.xxl + tokens.spacing.m + tokens.spacing.xs,
                            )
                            Spacer(Modifier.width(tokens.spacing.m + tokens.spacing.s))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text       = displayName,
                                        style      = tokens.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color      = tokens.colors.onSurface,
                                    )
                                    if (isEquipped) {
                                        Spacer(Modifier.width(tokens.spacing.s + tokens.spacing.xs))
                                        Text(
                                            text  = stringResource(R.string.shop_equipped_label),
                                            style = tokens.typography.labelSmall,
                                            color = tokens.colors.onSurfaceMuted,
                                        )
                                    }
                                }
                                Text(
                                    text  = stringResource(R.string.shop_qty_in_inv, qty),
                                    style = tokens.typography.bodyMedium,
                                    color = tokens.colors.onSurfaceMuted,
                                )
                            }
                            Spacer(Modifier.width(tokens.spacing.m))
                            PricePill(price = sellPrice.toLong(), enabled = true)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(tokens.spacing.l)) }
    }
}

// ---------------------------------------------------------------------------
// Price pill
// ---------------------------------------------------------------------------

@Composable
private fun PricePill(price: Long, enabled: Boolean) {
    val tokens = LocalFantasyTokens.current
    Surface(
        shape = tokens.shapes.chip,
        color = tokens.colors.primary.copy(alpha = if (enabled) 0.20f else 0.08f),
    ) {
        Text(
            text       = "$price",
            modifier   = Modifier.padding(horizontal = tokens.spacing.m + tokens.spacing.xs, vertical = tokens.spacing.s),
            style      = tokens.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color      = if (enabled) tokens.colors.primary else tokens.colors.primary.copy(alpha = 0.5f),
        )
    }
}

// ---------------------------------------------------------------------------
// Transaction sheet
// ---------------------------------------------------------------------------

@Composable
private fun TransactionSheet(
    transaction: ShopTransaction,
    coins: Long,
    onSetQty: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = LocalFantasyTokens.current
    val qty    = transaction.qty
    val total  = transaction.priceEach.toLong() * qty
    val maxQty = transaction.maxQty.coerceAtLeast(1)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = tokens.spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EntityIconDisk(
            entityId           = transaction.key,
            contentDescription = transaction.displayName,
            size               = tokens.spacing.xxl + tokens.spacing.xxl + tokens.spacing.m,
        )
        Spacer(Modifier.height(tokens.spacing.m + tokens.spacing.s))
        Text(
            text       = if (transaction.isBuy) stringResource(R.string.shop_buy_prefix, transaction.displayName)
                         else stringResource(R.string.shop_sell_prefix, transaction.displayName),
            style      = tokens.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color      = tokens.colors.onSurface,
        )
        Spacer(Modifier.height(tokens.spacing.xs))
        Text(
            text  = stringResource(R.string.shop_price_each_long, transaction.priceEach.toString()),
            style = tokens.typography.bodyMedium,
            color = tokens.colors.onSurfaceMuted,
        )
        Spacer(Modifier.height(tokens.spacing.l + tokens.spacing.s))

        if (maxQty > 1) {
            BigStepper(
                value         = qty,
                onValueChange = onSetQty,
                minValue      = 1,
                maxValue      = maxQty,
                onMax         = { onSetQty(maxQty) },
            )
            Spacer(Modifier.height(tokens.spacing.l))
        }

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                text  = if (transaction.isBuy) stringResource(R.string.shop_total_cost)
                        else stringResource(R.string.shop_youll_receive),
                style = tokens.typography.bodyMedium,
                color = tokens.colors.onSurfaceMuted,
            )
            PricePill(price = total, enabled = !transaction.isBuy || coins >= total)
        }

        if (transaction.isBuy && coins < total) {
            Spacer(Modifier.height(tokens.spacing.m))
            Text(
                text  = stringResource(R.string.error_not_enough_coins),
                style = tokens.typography.bodyMedium,
                color = tokens.colors.error,
            )
        }

        Spacer(Modifier.height(tokens.spacing.l + tokens.spacing.s))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.m + tokens.spacing.s),
        ) {
            ChunkyButton(
                text     = stringResource(R.string.btn_cancel),
                onClick  = onDismiss,
                variant  = ChunkyButtonVariant.Secondary,
                modifier = Modifier.weight(1f),
            )
            ChunkyButton(
                text     = if (transaction.isBuy) stringResource(R.string.btn_buy)
                           else stringResource(R.string.btn_sell),
                onClick  = onConfirm,
                variant  = ChunkyButtonVariant.Primary,
                enabled  = !transaction.isBuy || coins >= total,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// State containers
// ---------------------------------------------------------------------------

@Composable
private fun ShopLoading() {
    val tokens = LocalFantasyTokens.current
    Box(
        modifier         = Modifier.fillMaxSize().padding(tokens.spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = tokens.colors.primary)
    }
}

@Composable
private fun ShopEmpty(title: String, desc: String) {
    EmptyState(title = title, description = desc)
}

@Composable
private fun ShopError(message: String) {
    val tokens = LocalFantasyTokens.current
    Surface(
        color    = tokens.colors.error.copy(alpha = 0.12f),
        shape    = tokens.shapes.card,
        modifier = Modifier
            .fillMaxWidth()
            .padding(tokens.spacing.l),
    ) {
        Column(
            modifier            = Modifier.padding(tokens.spacing.l),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.s),
        ) {
            Text(
                text       = stringResource(R.string.shop_error_title),
                style      = tokens.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = tokens.colors.error,
            )
            Text(
                text  = message,
                style = tokens.typography.bodyMedium,
                color = tokens.colors.onSurface,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@PreviewLightDark
@Composable
private fun PreviewShopLoading() {
    FantasyPreviewSurface {
        Box(modifier = Modifier.size(LocalFantasyTokens.current.spacing.xxl * 6)) {
            ShopLoading()
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewShopEmpty() {
    FantasyPreviewSurface {
        ShopEmpty(title = "Inventory empty", desc = "Train to fill it.")
    }
}

@PreviewLightDark
@Composable
private fun PreviewShopError() {
    FantasyPreviewSurface { ShopError("Network unreachable.") }
}

@PreviewLightDark
@Composable
private fun PreviewPricePill() {
    FantasyPreviewSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(LocalFantasyTokens.current.spacing.m)) {
            PricePill(price = 250, enabled = true)
            PricePill(price = 999, enabled = false)
        }
    }
}
