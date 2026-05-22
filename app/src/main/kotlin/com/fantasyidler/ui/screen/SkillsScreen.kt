package com.fantasyidler.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.fantasyidler.data.model.Skills
import com.fantasyidler.ui.components.SectionHeader
import com.fantasyidler.ui.screen.skills.ActiveSessionBanner
import com.fantasyidler.ui.screen.skills.SessionResultSheet
import com.fantasyidler.ui.screen.skills.SkillRow
import com.fantasyidler.ui.screen.skills.SkillsEmptyState
import com.fantasyidler.ui.screen.skills.SkillsErrorDialog
import com.fantasyidler.ui.screen.skills.SkillsLoadingState
import com.fantasyidler.ui.screen.skills.sheets.AgilitySheet
import com.fantasyidler.ui.screen.skills.sheets.ComingSoonSheet
import com.fantasyidler.ui.screen.skills.sheets.FiremakingSheet
import com.fantasyidler.ui.screen.skills.sheets.FishingSheet
import com.fantasyidler.ui.screen.skills.sheets.MiningSheet
import com.fantasyidler.ui.screen.skills.sheets.PrayerSheet
import com.fantasyidler.ui.screen.skills.sheets.RunecraftingSheet
import com.fantasyidler.ui.screen.skills.sheets.WoodcuttingSheet
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.fantasyidler.R
import com.fantasyidler.ui.viewmodel.SheetState
import com.fantasyidler.ui.viewmodel.SkillsViewModel
import com.fantasyidler.ui.scene.SceneCatalog
import com.fantasyidler.ui.scene.SessionSceneSheet
import com.fantasyidler.util.GameStrings

/**
 * Top-level Skills screen. Pure orchestrator: collects [com.fantasyidler.ui.viewmodel.SkillsUiState],
 * iterates the gathering + crafting + combat skill ladder, and mounts the
 * matching extracted sheet from [com.fantasyidler.ui.screen.skills] when an
 * activity picker is open. Crafting and non-prayer combat rows are read-only
 * level cards that redirect to the dedicated Crafting / Combat tabs on tap.
 */
@Composable
fun SkillsScreen(
    onNavigateToFarming: () -> Unit = {},
    onNavigateToCrafting: () -> Unit = {},
    onNavigateToCombat: () -> Unit = {},
    viewModel: SkillsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var errorDismissed by remember { mutableStateOf(false) }
    var miningSceneOpen by remember { mutableStateOf(false) }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.snackbarConsumed()
        }
    }

    val showErrorDialog =
        !state.isLoading && state.skillLevels.isEmpty() && state.skillXp.isEmpty() && !errorDismissed

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                SkillsLoadingState()
            }
            state.skillLevels.isEmpty() && state.skillXp.isEmpty() -> Box(
                modifier         = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                SkillsEmptyState()
            }
            else -> SkillsList(
                state               = state,
                paddingValues       = padding,
                onSkillTap          = { key ->
                    when {
                        key == Skills.FARMING            -> onNavigateToFarming()
                        key in Skills.CRAFTING_SKILLS    -> onNavigateToCrafting()
                        key == Skills.PRAYER             -> viewModel.onSkillTapped(key)
                        key in Skills.COMBAT             -> onNavigateToCombat()
                        else                             -> viewModel.onSkillTapped(key)
                    }
                },
                onCollect           = viewModel::collectSession,
                onAbandon           = viewModel::abandonSession,
                onDebugFinish       = viewModel::debugFinishSession,
                onTapMiningHero     = { miningSceneOpen = true },
            )
        }
    }

    if (showErrorDialog) {
        SkillsErrorDialog(
            message   = stringResource(R.string.skills_error_desc),
            onRetry   = { errorDismissed = false },
            onDismiss = { errorDismissed = true },
        )
    }

    state.sessionResult?.let { result ->
        SessionResultSheet(result = result, onDismiss = viewModel::resultConsumed)
    }

    state.sheetSkill?.let { sheet ->
        val onDismiss = viewModel::dismissSheet
        when (sheet) {
            is SheetState.Mining -> MiningSheet(
                ores              = sheet.ores,
                isStarting        = state.startingSession,
                hasActiveSession  = state.anySessionActive,
                isQueueFull       = state.queueSize >= 3,
                sessionDurationMs = state.sessionDurationMs,
                onSelect          = viewModel::startMiningSession,
                onDismissRequest  = onDismiss,
            )
            is SheetState.Woodcutting -> WoodcuttingSheet(
                trees             = sheet.trees,
                isStarting        = state.startingSession,
                hasActiveSession  = state.anySessionActive,
                isQueueFull       = state.queueSize >= 3,
                sessionDurationMs = state.sessionDurationMs,
                onSelect          = viewModel::startWoodcuttingSession,
                onDismissRequest  = onDismiss,
            )
            SheetState.Fishing -> FishingSheet(
                fishingLevel      = state.skillLevels[Skills.FISHING] ?: 1,
                sessionDurationMs = state.sessionDurationMs,
                isStarting        = state.startingSession,
                hasActiveSession  = state.anySessionActive,
                isQueueFull       = state.queueSize >= 3,
                onStart           = viewModel::startFishingSession,
                onDismissRequest  = onDismiss,
            )
            is SheetState.Agility -> AgilitySheet(
                courses           = sheet.courses,
                isStarting        = state.startingSession,
                hasActiveSession  = state.anySessionActive,
                isQueueFull       = state.queueSize >= 3,
                sessionDurationMs = state.sessionDurationMs,
                onSelect          = viewModel::startAgilitySession,
                onDismissRequest  = onDismiss,
            )
            is SheetState.Firemaking -> FiremakingSheet(
                availableLogs     = sheet.availableLogs,
                isStarting        = state.startingSession,
                hasActiveSession  = state.anySessionActive,
                isQueueFull       = state.queueSize >= 3,
                sessionDurationMs = state.sessionDurationMs,
                onSelect          = viewModel::startFiremakingSession,
                onDismissRequest  = onDismiss,
            )
            is SheetState.Runecrafting -> RunecraftingSheet(
                availableRunes    = sheet.availableRunes,
                essenceQty        = sheet.essenceQty,
                isStarting        = state.startingSession,
                hasActiveSession  = state.anySessionActive,
                isQueueFull       = state.queueSize >= 3,
                sessionDurationMs = state.sessionDurationMs,
                onStart           = viewModel::startRunecraftingSession,
                onDismissRequest  = onDismiss,
            )
            is SheetState.Prayer -> PrayerSheet(
                availableBones    = sheet.availableBones,
                inventory         = sheet.inventory,
                prayerLevel       = state.skillLevels[Skills.PRAYER] ?: 1,
                isStarting        = state.startingSession,
                hasActiveSession  = state.anySessionActive,
                isQueueFull       = state.queueSize >= 3,
                sessionDurationMs = state.sessionDurationMs,
                onStart           = viewModel::startPrayerSession,
                onDismissRequest  = onDismiss,
            )
            is SheetState.Crafting -> ComingSoonSheet(onDismissRequest = onDismiss)
            SheetState.ComingSoon  -> ComingSoonSheet(onDismissRequest = onDismiss)
        }
    }

    // Tap-to-expand mining scene sheet — only when a mining session is active.
    val activeMiningSession = state.activeSession?.takeIf { it.skillName == Skills.MINING }
    if (activeMiningSession != null && miningSceneOpen) {
        val pickaxeId = state.equippedPickaxeId
        val oreNodeId = "${activeMiningSession.activityKey}_node"
        val frames = remember(activeMiningSession.sessionId) { viewModel.currentMiningFrames() }

        SessionSceneSheet(
            config = SceneCatalog.mining(pickaxeId = pickaxeId, oreNodeId = oreNodeId),
            framesFlow = { onFrame ->
                for (items in frames) {
                    onFrame(items, 60_000L)
                }
            },
            open = miningSceneOpen,
            onDismiss = { miningSceneOpen = false },
            statsContent = {
                androidx.compose.material3.Text(
                    text = "${GameStrings.skillName(context, Skills.MINING)} — ${activeMiningSession.activityKey.replace('_', ' ')}",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                )
            },
        )
    }
}

@Composable
private fun SkillsList(
    state: com.fantasyidler.ui.viewmodel.SkillsUiState,
    paddingValues: androidx.compose.foundation.layout.PaddingValues,
    onSkillTap: (String) -> Unit,
    onCollect: () -> Unit,
    onAbandon: () -> Unit,
    onDebugFinish: () -> Unit,
    onTapMiningHero: (() -> Unit)? = null,
) {
    val context = LocalContext.current

    LazyColumn(
        contentPadding = paddingValues,
        modifier       = Modifier.fillMaxSize(),
    ) {
        state.activeSession?.let { session ->
            item(key = "active-session") {
                ActiveSessionBanner(
                    skillName     = GameStrings.skillName(context, session.skillName),
                    skillEmoji    = GameStrings.skillEmoji(session.skillName),
                    activityKey   = session.activityKey,
                    endsAt        = session.endsAt,
                    completed     = session.completed,
                    onCollect     = onCollect,
                    onAbandon     = onAbandon,
                    onDebugFinish = onDebugFinish,
                    onTapHero     = if (session.skillName == Skills.MINING) onTapMiningHero else null,
                )
            }
        }

        item(key = "section-gathering") {
            SectionHeader(stringResource(R.string.label_gathering_skills))
        }
        items(items = Skills.GATHERING, key = { it }) { key ->
            val efficiency = when (key) {
                Skills.MINING      -> state.miningEfficiency
                Skills.WOODCUTTING -> state.woodcuttingEfficiency
                Skills.FISHING     -> state.fishingEfficiency
                else               -> 1.0f
            }
            SkillRow(
                skillKey       = key,
                level          = state.skillLevels[key] ?: 1,
                xp             = state.skillXp[key] ?: 0L,
                isActive       = state.activeSession?.skillName == key && state.activeSession?.completed == false,
                onClick        = { onSkillTap(key) },
                toolEfficiency = efficiency,
            )
        }

        item(key = "section-crafting") {
            SectionHeader(stringResource(R.string.label_crafting_skills))
        }
        items(items = Skills.CRAFTING_SKILLS, key = { it }) { key ->
            SkillRow(
                skillKey = key,
                level    = state.skillLevels[key] ?: 1,
                xp       = state.skillXp[key] ?: 0L,
                isActive = state.activeSession?.skillName == key && state.activeSession?.completed == false,
                onClick  = { onSkillTap(key) },
            )
        }

        item(key = "section-combat") {
            SectionHeader(stringResource(R.string.label_combat))
        }
        items(items = Skills.COMBAT, key = { it }) { key ->
            SkillRow(
                skillKey = key,
                level    = state.skillLevels[key] ?: 1,
                xp       = state.skillXp[key] ?: 0L,
                isActive = state.activeSession?.skillName == key && state.activeSession?.completed == false,
                onClick  = { onSkillTap(key) },
            )
        }
    }
}
