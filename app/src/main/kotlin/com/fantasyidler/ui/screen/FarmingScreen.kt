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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.hilt.navigation.compose.hiltViewModel
import com.fantasyidler.R
import com.fantasyidler.data.json.CropData
import com.fantasyidler.data.model.FarmingPatch
import com.fantasyidler.simulator.XpTable
import com.fantasyidler.ui.components.EmptyState
import com.fantasyidler.ui.components.foundation.ChunkyButton
import com.fantasyidler.ui.components.foundation.ChunkyButtonVariant
import com.fantasyidler.ui.components.foundation.ChunkyCard
import com.fantasyidler.ui.components.foundation.ChunkyDialog
import com.fantasyidler.ui.components.foundation.ChunkySheet
import com.fantasyidler.ui.components.foundation.ClaimBadge
import com.fantasyidler.ui.components.foundation.HeroBlock
import com.fantasyidler.ui.components.foundation.IconDisk
import com.fantasyidler.ui.theme.fantasy.FantasyPreviewSurface
import com.fantasyidler.ui.theme.fantasy.LocalFantasyTokens
import com.fantasyidler.ui.viewmodel.FarmingUiState
import com.fantasyidler.ui.viewmodel.FarmingViewModel
import com.fantasyidler.ui.viewmodel.remainingMs
import com.fantasyidler.util.GameStrings
import com.fantasyidler.util.formatDurationMs
import com.fantasyidler.util.formatXp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmingScreen(
    onBack: () -> Unit,
    viewModel: FarmingViewModel = hiltViewModel(),
) {
    val tokens            = LocalFantasyTokens.current
    val state             by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context           = LocalContext.current
    var detailPatch       by remember { mutableStateOf<Int?>(null) }

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
                        text  = stringResource(R.string.skill_farming_name),
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
                    containerColor    = tokens.colors.surface,
                    titleContentColor = tokens.colors.onSurface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.isLoading -> FarmingLoading()
            state.patchCount == 0 -> Box(
                modifier         = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                FarmingEmpty()
            }
            else -> LazyColumn(
                modifier            = Modifier.fillMaxSize(),
                contentPadding      = PaddingValues(
                    start  = tokens.spacing.l,
                    end    = tokens.spacing.l,
                    top    = padding.calculateTopPadding() + tokens.spacing.m + tokens.spacing.s,
                    bottom = padding.calculateBottomPadding() + tokens.spacing.xl,
                ),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.m + tokens.spacing.s),
            ) {
                item { FarmingXpHero(state) }

                val patches = state.patches.associateBy { it.patchNumber }
                items(state.patchCount) { index ->
                    val patchNumber = index + 1
                    PatchCard(
                        patchNumber = patchNumber,
                        patch       = patches[patchNumber],
                        crops       = state.availableCrops.associateBy { it.id },
                        now         = state.now,
                        onTap       = { detailPatch = patchNumber },
                    )
                }
            }
        }
    }

    detailPatch?.let { patchNum ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val patches    = state.patches.associateBy { it.patchNumber }
        val cropsById  = state.availableCrops.associateBy { it.id }
        ChunkySheet(
            onDismissRequest = { detailPatch = null },
            sheetState       = sheetState,
        ) {
            PatchDetailSheet(
                patchNumber = patchNum,
                patch       = patches[patchNum],
                crops       = state.availableCrops,
                cropsById   = cropsById,
                inventory   = state.inventory,
                now         = state.now,
                onPlant     = { crop ->
                    viewModel.plantCrop(patchNum, crop)
                    detailPatch = null
                },
                onHarvest   = {
                    viewModel.harvestPatch(patchNum)
                    detailPatch = null
                },
                onClear     = {
                    viewModel.clearPatch(patchNum)
                    detailPatch = null
                },
                onDismiss   = { detailPatch = null },
            )
        }
    }

    state.harvestResult?.let { result ->
        ChunkyDialog(
            title            = stringResource(R.string.farming_harvested, result.cropName),
            onDismissRequest = viewModel::harvestResultConsumed,
            body = {
                Column {
                    Text(
                        text       = stringResource(R.string.farming_xp_gained, result.xpGained.formatXp()),
                        style      = tokens.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = tokens.colors.primary,
                    )
                    if (result.itemsGained.isNotEmpty()) {
                        Spacer(Modifier.height(tokens.spacing.m))
                        result.itemsGained.forEach { (key, qty) ->
                            Text(
                                text  = "${GameStrings.cropName(context, key)}: ×$qty",
                                style = tokens.typography.bodyMedium,
                                color = tokens.colors.onSurface,
                            )
                        }
                    }
                }
            },
            actions = {
                ChunkyButton(
                    text    = stringResource(R.string.btn_close),
                    onClick = viewModel::harvestResultConsumed,
                    variant = ChunkyButtonVariant.Primary,
                )
            },
        )
    }
}

// ---------------------------------------------------------------------------
// XP hero
// ---------------------------------------------------------------------------

@Composable
private fun FarmingXpHero(state: FarmingUiState) {
    val tokens   = LocalFantasyTokens.current
    val level    = state.farmingLevel
    val xp       = state.farmingXp
    val progress = XpTable.progressFraction(xp)

    HeroBlock(
        title    = stringResource(R.string.farming_title, level),
        subtitle = stringResource(R.string.farming_patches, state.patchCount),
        leading  = { IconDisk(emoji = GameStrings.skillEmoji("farming"), size = tokens.spacing.xxl + tokens.spacing.xl) },
        trailing = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text       = stringResource(R.string.farming_xp_value, xp.formatXp()),
                    style      = tokens.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color      = tokens.colors.primary,
                )
            }
        },
        content = {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(tokens.spacing.m)
                    .clip(tokens.shapes.chip),
                color = tokens.colors.primary,
            )
        },
    )
}

// ---------------------------------------------------------------------------
// Patch card (tap → PatchDetailSheet)
// ---------------------------------------------------------------------------

@Composable
private fun PatchCard(
    patchNumber: Int,
    patch: FarmingPatch?,
    crops: Map<String, CropData>,
    now: Long,
    onTap: () -> Unit,
) {
    val tokens    = LocalFantasyTokens.current
    val isEmpty   = patch == null || patch.cropType == null
    val cropData  = patch?.cropType?.let { crops[it] }
    val remaining = if (isEmpty) Long.MAX_VALUE else patch!!.remainingMs(crops, now)
    val isReady   = !isEmpty && remaining <= 0
    val isGrowing = !isEmpty && !isReady

    ChunkyCard(highlight = isReady, onClick = onTap) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconDisk(
                    emoji = when {
                        isEmpty -> "➕"
                        isReady -> cropData?.emoji ?: "🌾"
                        else    -> cropData?.emoji ?: "🌱"
                    },
                    size = tokens.spacing.xxl + tokens.spacing.l,
                )
                Spacer(Modifier.width(tokens.spacing.m + tokens.spacing.s))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = stringResource(R.string.format_patch_number, patchNumber),
                        style      = tokens.typography.labelSmall,
                        color      = tokens.colors.onSurfaceMuted,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text       = when {
                            isEmpty -> stringResource(R.string.label_empty)
                            else    -> cropData?.displayName ?: patch.cropType.orEmpty()
                        },
                        style      = tokens.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color      = tokens.colors.onSurface,
                    )
                }
                when {
                    isReady   -> ClaimBadge(text = stringResource(R.string.farming_harvest_badge))
                    isGrowing -> Text(
                        text       = remaining.formatDurationMs(),
                        style      = tokens.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = tokens.colors.onSurfaceMuted,
                    )
                    else      -> {}
                }
            }

            if (isGrowing) {
                val growthMs  = cropData?.growthTimeMs ?: 1L
                val elapsed   = growthMs - remaining
                val progress  = (elapsed.toFloat() / growthMs).coerceIn(0f, 1f)
                Spacer(Modifier.height(tokens.spacing.m + tokens.spacing.s))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(tokens.spacing.m)
                        .clip(tokens.shapes.chip),
                    color    = tokens.colors.primary,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Patch detail sheet (plant / harvest / clear interactions)
// ---------------------------------------------------------------------------

@Composable
private fun PatchDetailSheet(
    patchNumber: Int,
    patch: FarmingPatch?,
    crops: List<CropData>,
    cropsById: Map<String, CropData>,
    inventory: Map<String, Int>,
    now: Long,
    onPlant: (CropData) -> Unit,
    onHarvest: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens     = LocalFantasyTokens.current
    val isEmpty    = patch == null || patch.cropType == null
    val cropData   = patch?.cropType?.let { cropsById[it] }
    val remaining  = if (isEmpty) Long.MAX_VALUE else patch!!.remainingMs(cropsById, now)
    val isReady    = !isEmpty && remaining <= 0
    val isGrowing  = !isEmpty && !isReady
    var showClear  by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = tokens.spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.m + tokens.spacing.s),
    ) {
        Text(
            text       = stringResource(R.string.format_patch_number, patchNumber),
            style      = tokens.typography.labelSmall,
            color      = tokens.colors.onSurfaceMuted,
            fontWeight = FontWeight.Bold,
        )

        when {
            isEmpty -> {
                Text(
                    text       = stringResource(R.string.farming_choose_crop),
                    style      = tokens.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = tokens.colors.onSurface,
                )
                if (crops.isEmpty()) {
                    EmptyState(
                        title       = stringResource(R.string.farming_no_crops_title),
                        description = stringResource(R.string.farming_no_crops_desc),
                    )
                } else {
                    crops.forEach { crop ->
                        CropPickRow(crop = crop, inventory = inventory, onPlant = { onPlant(crop) })
                    }
                }
            }
            isGrowing -> {
                Text(
                    text       = cropData?.displayName ?: patch!!.cropType.orEmpty(),
                    style      = tokens.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = tokens.colors.onSurface,
                )
                Text(
                    text  = stringResource(R.string.farming_growing_ready_in, remaining.formatDurationMs()),
                    style = tokens.typography.bodyMedium,
                    color = tokens.colors.onSurfaceMuted,
                )
                ChunkyButton(
                    text     = stringResource(R.string.btn_clear),
                    onClick  = { showClear = true },
                    variant  = ChunkyButtonVariant.Destructive,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            isReady -> {
                Text(
                    text       = cropData?.displayName ?: patch!!.cropType.orEmpty(),
                    style      = tokens.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = tokens.colors.onSurface,
                )
                Text(
                    text  = stringResource(R.string.farming_ready_to_harvest_desc),
                    style = tokens.typography.bodyMedium,
                    color = tokens.colors.primary,
                )
                ClaimBadge(text = stringResource(R.string.farming_harvest_badge))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.m),
                ) {
                    ChunkyButton(
                        text     = stringResource(R.string.btn_harvest),
                        onClick  = onHarvest,
                        variant  = ChunkyButtonVariant.Primary,
                        modifier = Modifier.weight(1f),
                    )
                    ChunkyButton(
                        text     = stringResource(R.string.btn_clear),
                        onClick  = { showClear = true },
                        variant  = ChunkyButtonVariant.Destructive,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        ChunkyButton(
            text     = stringResource(R.string.btn_close),
            onClick  = onDismiss,
            variant  = ChunkyButtonVariant.Ghost,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (showClear) {
        ChunkyDialog(
            title            = stringResource(R.string.farming_clear_patch),
            onDismissRequest = { showClear = false },
            body             = {
                Text(
                    text  = stringResource(R.string.farming_clear_desc),
                    style = LocalFantasyTokens.current.typography.bodyMedium,
                    color = LocalFantasyTokens.current.colors.onSurface,
                )
            },
            actions = {
                ChunkyButton(
                    text    = stringResource(R.string.btn_cancel),
                    onClick = { showClear = false },
                    variant = ChunkyButtonVariant.Secondary,
                )
                ChunkyButton(
                    text    = stringResource(R.string.btn_clear),
                    onClick = { showClear = false; onClear() },
                    variant = ChunkyButtonVariant.Destructive,
                )
            },
        )
    }
}

@Composable
private fun CropPickRow(
    crop: CropData,
    inventory: Map<String, Int>,
    onPlant: () -> Unit,
) {
    val tokens    = LocalFantasyTokens.current
    val seedCount = inventory[crop.seedName] ?: 0
    val enabled   = seedCount > 0
    val dimColor  = tokens.colors.onSurface.copy(alpha = 0.5f)

    ChunkyCard(onClick = onPlant, enabled = enabled) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconDisk(emoji = crop.emoji, size = tokens.spacing.xxl + tokens.spacing.m + tokens.spacing.xs)
            Spacer(Modifier.width(tokens.spacing.m + tokens.spacing.s))
            Column(Modifier.weight(1f)) {
                Text(
                    text       = crop.displayName,
                    style      = tokens.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = if (enabled) tokens.colors.onSurface else dimColor,
                )
                Text(
                    text  = stringResource(
                        R.string.farming_crop_meta,
                        crop.levelRequired,
                        crop.growthTimeHours,
                        crop.harvestXp,
                    ),
                    style = tokens.typography.bodyMedium,
                    color = tokens.colors.onSurfaceMuted,
                )
            }
            Spacer(Modifier.width(tokens.spacing.m))
            ClaimBadge(
                text  = "×$seedCount",
                pulse = false,
                color = if (enabled) tokens.colors.primary else tokens.colors.error,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// State containers
// ---------------------------------------------------------------------------

@Composable
private fun FarmingLoading() {
    val tokens = LocalFantasyTokens.current
    Box(
        modifier         = Modifier.fillMaxSize().padding(tokens.spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = tokens.colors.primary)
    }
}

@Composable
private fun FarmingEmpty() {
    EmptyState(
        title       = stringResource(R.string.farming_empty_title),
        description = stringResource(R.string.farming_empty_desc),
    )
}

@Composable
private fun FarmingError(message: String) {
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
                text       = stringResource(R.string.farming_error_title),
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
private fun PreviewFarmingLoading() {
    FantasyPreviewSurface {
        Box(modifier = Modifier.size(LocalFantasyTokens.current.spacing.xxl * 6)) {
            FarmingLoading()
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewFarmingEmpty() {
    FantasyPreviewSurface { FarmingEmpty() }
}

@PreviewLightDark
@Composable
private fun PreviewFarmingError() {
    FantasyPreviewSurface { FarmingError("Patches unavailable.") }
}

@PreviewLightDark
@Composable
private fun PreviewPatchCardEmpty() {
    FantasyPreviewSurface {
        PatchCard(
            patchNumber = 1,
            patch       = null,
            crops       = emptyMap(),
            now         = 0L,
            onTap       = {},
        )
    }
}
