package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.GameAudio
import com.example.data.AiHelperRepository
import com.example.data.AiTipUiState
import com.example.data.CloudSavePayload
import com.example.data.RoomPlayer
import com.example.data.UserProfile
import com.example.game.GameEngine
import com.example.game.RemotePartnerState
import com.example.model.*
import com.example.notifications.CatNotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max

data class GameUiState(
  val gameStatus: GameStatus = GameStatus.READY,
  val currentMode: GameModeDef = ALL_40_MODES.first(),
  val isEndlessMode: Boolean = false,
  val catsCollected: Int = 0,
  val goalCats: Int = 10,
  val lives: Int = 3,
  val maxLives: Int = 3,
  val isDashing: Boolean = false,
  val dashCooldownRatio: Float = 0f,
  val sonicCooldownRatio: Float = 0f,
  // Active Item Buffs
  val catnipActive: Boolean = false,
  val goldenFishActive: Boolean = false,
  val shieldActive: Boolean = false,
  val magnetActive: Boolean = false,
  val giantMagnetActive: Boolean = false,
  val freezeActive: Boolean = false,
  val invisibilityActive: Boolean = false,
  val doubleCatActive: Boolean = false,
  val slowMoActive: Boolean = false,
  val turboOverdriveActive: Boolean = false,
  val sonicOverdriveActive: Boolean = false,
  val starInvincibleActive: Boolean = false,
  val isInvincible: Boolean = false,
  val isEffectiveGodMode: Boolean = false,
  val comboCount: Int = 0,
  val gameTimeSeconds: Float = 0f,
  val timeLimitRemaining: Float = 0f,
  val ghostDistance: Float = 9999f,
  val activeGhostsCount: Int = 1,
  val isGhostNear: Boolean = false,
  val isGhostFleeing: Boolean = false,
  // Levels & Scoreboard
  val matchLevel: Int = 1,
  val playerLevel: Int = 1,
  val playerXp: Int = 0,
  val xpToNextLevel: Int = 100,
  val currentScore: Int = 0,
  val bestHighScore: Int = 0,
  val lastXpEarned: Int = 0,
  // Career, 40 Achievements, 40 Items, 40 Modes
  val championCount: Int = 0,
  val bestTimeSec: Float = 0f,
  val totalCatsSaved: Int = 0,
  val bestEndlessCats: Int = 0,
  val totalGhostsScared: Int = 0,
  val totalItemsUsed: Int = 0,
  val totalDashesUsed: Int = 0,
  val totalSonicsUsed: Int = 0,
  val discoveredItems: Set<String> = emptySet(),
  val unlockedModesCount: Int = 1,
  val unlockedAchievements: Set<String> = emptySet(),
  val newlyUnlockedAchievement: Achievement? = null,
  val lastVictoryUnlocks: List<String> = emptyList(),
  // Milestones derived from wins
  val isEndlessUnlocked: Boolean = false, // 1+ wins
  val isGodModeUnlocked: Boolean = false, // 10+ wins
  // Rewards Blocking (God Mode OR Gameplay Modifications)
  val isRewardsBlocked: Boolean = false,
  val rewardsBlockedReason: String = "",
  // Gameplay Options, 3D Mode, 4D Mode, Custom Ghost Speed (1-1000), Skins & Creator
  val is3dModeEnabled: Boolean = false,
  val is4dModeEnabled: Boolean = false,
  val customGhostSpeed: Int = 250,
  val selectedSkinId: String = "classic_orange",
  val unlockedSkinsCount: Int = 1,
  val customMap: CustomMapConfig = CustomMapConfig(),
  val customItems: List<CustomItemConfig> = emptyList(),
  val spawnCustomItemsInAllModes: Boolean = true,
  val hasCreatedCustomContent: Boolean = false,
  val isGodModeEnabled: Boolean = false,
  val arenaTheme: ArenaTheme = ArenaTheme.PARQUE_NOTURNO,
  val ghostCount: Int = 1,
  val heroSpeedOption: HeroSpeedOption = HeroSpeedOption.NORMAL,
  val powerUpFrequency: PowerUpFrequency = PowerUpFrequency.NORMAL,
  val showSonicButton: Boolean = true,
  val controlType: ControlType = ControlType.JOYSTICK,
  val isSoundEnabled: Boolean = true,
  val isHapticEnabled: Boolean = true,
  val difficulty: Difficulty = Difficulty.NORMAL,
  // Elite / Player-as-Ghost, Bots Mode & Shop Economy (incl. Revives)
  val isPlayerGhost: Boolean = false,
  val aiRunnerLives: Int = 3,
  val aiRunnerCats: Int = 0,
  val isBotsMode: Boolean = false,
  val botCount: Int = 0,
  val playerBotRank: Int = 1,
  val leadingBotName: String = "",
  val leadingBotCats: Int = 0,
  val winnerBotName: String? = null,
  val spendablePoints: Int = 0,
  val reviveCount: Int = 0,
  val purchasedSkinIds: Set<String> = emptySet(),
  val purchasedModeIds: Set<String> = emptySet(),
  val purchasedItemIds: Set<String> = emptySet(),
  val tradedAwaySkinIds: Set<String> = emptySet(),
  // Auto-Update State
  val autoUpdateEnabled: Boolean = true,
  val appUpdateStatusText: String = "",
  val isUpdateAvailable: Boolean = false,
  val isUpdateDownloading: Boolean = false,
  val updateProgressPercent: Int = 0,
  // Multiplayer & Notifications & AI
  val activeRoomCode: String? = null,
  val partnerUsername: String? = null,
  val partnerScore: Int = 0,
  val multiplayerPlayerCount: Int = 0,
  val multiplayerMaxPlayers: Int = 100,
  val multiplayerRole: String = "cat",
  val roomWinnerUsername: String? = null,
  val notificationFeedback: String? = null,
  // Dialogs
  val showVictoryDialog: Boolean = false,
  val showGameOverDialog: Boolean = false,
  val showGalleryDialog: Boolean = false,
  val showSettingsDialog: Boolean = false,
  val showAchievementsDialog: Boolean = false,
  val showModesDialog: Boolean = false,
  val showItemsDialog: Boolean = false,
  val showSkinsDialog: Boolean = false,
  val showShopDialog: Boolean = false,
  val showCreatorDialog: Boolean = false,
  val showAiDialog: Boolean = false,
  val showOnlineDialog: Boolean = false,
  val showHelpDialog: Boolean = false
)

fun computeRewardsBlocked(
  state: GameUiState,
  activeCustomMap: Boolean = false
): Pair<Boolean, String> {
  val reasons = mutableListOf<String>()
  if (state.isGodModeEnabled || state.currentMode.isGodTrialMode) {
    reasons.add("Modo Deus ativo")
  }
  // Fácil (Difficulty.CALMO) no longer counts as a modification and earns full rewards!
  if (state.difficulty == Difficulty.CUSTOM) {
    reasons.add("Velocidade personalizada (1-1000)")
  }
  if (state.heroSpeedOption != HeroSpeedOption.NORMAL) {
    reasons.add("Velocidade do Herói modificada")
  }
  if (state.powerUpFrequency != PowerUpFrequency.NORMAL) {
    reasons.add("Chuva de Itens modificada")
  }
  if (activeCustomMap || state.currentMode.id == "custom_map") {
    reasons.add("Mapa personalizado")
  }
  if (state.spawnCustomItemsInAllModes && state.customItems.isNotEmpty()) {
    reasons.add("Itens personalizados ativos")
  }
  return if (reasons.isEmpty()) {
    false to ""
  } else {
    true to reasons.joinToString(" • ")
  }
}

class GameViewModel(application: Application) : AndroidViewModel(application) {
  private val prefs = application.getSharedPreferences("gato_campeao_prefs", Context.MODE_PRIVATE)
  val audio = GameAudio(application)
  val engine = GameEngine(audio = audio)
  private val aiHelperRepository = AiHelperRepository()

  private val _uiState = MutableStateFlow(GameUiState())
  val uiState = _uiState.asStateFlow()

  private val _aiTipState = MutableStateFlow<AiTipUiState>(AiTipUiState.Idle)
  val aiTipState = _aiTipState.asStateFlow()

  var inputVector = Vector2D(0f, 0f)

  private var gameLoopJob: Job? = null
  private var lastFrameTime = System.nanoTime()
  private var bannerDismissJob: Job? = null
  private var aiJob: Job? = null
  private var feedbackDismissJob: Job? = null

  private var lastSyncedGhostsScared = 0
  private var lastSyncedDashes = 0
  private var lastSyncedSonics = 0
  private var lastSyncedItemIndex = 0

  init {
    loadSavedPreferences()
    startGameLoop()
  }

  private fun xpRequiredForLevel(level: Int): Int = (level.coerceAtLeast(1) * 100)

  private fun loadSavedPreferences() {
    val champCount = prefs.getInt("champion_count", 0)
    val bestTime = prefs.getFloat("best_time_sec", 0f)
    val bestScore = prefs.getInt("best_high_score", 0)
    val playerLvl = prefs.getInt("player_level", 1).coerceAtLeast(1)
    val playerXp = prefs.getInt("player_xp", 0).coerceAtLeast(0)
    val totalSaved = prefs.getInt("total_cats_saved", 0)
    val bestEndless = prefs.getInt("best_endless_cats", 0)
    val totalScared = prefs.getInt("total_ghosts_scared", 0)
    val totalItems = prefs.getInt("total_items_used", 0)
    val totalDashes = prefs.getInt("total_dashes_used", 0)
    val totalSonics = prefs.getInt("total_sonics_used", 0)
    val savedItems = prefs.getStringSet("discovered_items", emptySet())?.toSet() ?: emptySet()
    val savedAchievements = prefs.getStringSet("unlocked_achievements", emptySet())?.toSet() ?: emptySet()
    val savedPoints = if (prefs.contains("spendable_points")) {
      prefs.getInt("spendable_points", 0).coerceAtLeast(0)
    } else if (champCount > 0 || totalSaved > 0 || bestScore > 0) {
      (champCount * 120 + totalSaved * 8 + bestScore / 10).coerceAtLeast(0)
    } else {
      0
    }
    val savedPurchasedSkins = prefs.getStringSet("purchased_skins", emptySet())?.toSet() ?: emptySet()
    val savedPurchasedModes = prefs.getStringSet("purchased_modes", emptySet())?.toSet() ?: emptySet()
    val savedPurchasedItems = prefs.getStringSet("purchased_items", emptySet())?.toSet() ?: emptySet()
    val savedTradedSkins = (prefs.getStringSet("traded_away_skins", emptySet())?.toSet() ?: emptySet()) - "classic_orange"
    val savedReviveCount = prefs.getInt("revive_count", 0).coerceIn(0, 9999)
    val autoUpdatePref = prefs.getBoolean("auto_update_enabled", true)

    val ctrlName = prefs.getString("control_type", ControlType.JOYSTICK.name) ?: ControlType.JOYSTICK.name
    val ctrl = try { ControlType.valueOf(ctrlName) } catch (_: Exception) { ControlType.JOYSTICK }
    val sound = prefs.getBoolean("sound_enabled", true)
    val haptic = prefs.getBoolean("haptic_enabled", true)
    val diffName = prefs.getString("difficulty", Difficulty.NORMAL.name) ?: Difficulty.NORMAL.name
    val diff = try { Difficulty.valueOf(diffName) } catch (_: Exception) { Difficulty.NORMAL }
    val customSpeed = prefs.getInt("custom_ghost_speed", 250).coerceIn(1, 1000)

    val is3d = prefs.getBoolean("mode_3d_enabled", false)
    val is4d = prefs.getBoolean("mode_4d_enabled", false)
    val savedSkin = prefs.getString("selected_skin_id", "classic_orange") ?: "classic_orange"
    val effectiveSavedSkin = if (savedTradedSkins.contains(savedSkin)) "classic_orange" else savedSkin
    val customMap = CustomMapConfig.fromJsonString(prefs.getString("custom_map_json", null))
    val customItems = CustomItemConfig.listFromJson(prefs.getString("custom_items_json", null))
    val spawnCustom = prefs.getBoolean("spawn_custom_items", true)
    val hasCreated = prefs.getBoolean("has_created_custom_content", false) || customItems.isNotEmpty()

    val themeName = prefs.getString("arena_theme", ArenaTheme.PARQUE_NOTURNO.name) ?: ArenaTheme.PARQUE_NOTURNO.name
    val theme = try { ArenaTheme.valueOf(themeName) } catch (_: Exception) { ArenaTheme.PARQUE_NOTURNO }

    val speedName = prefs.getString("hero_speed_option", HeroSpeedOption.NORMAL.name) ?: HeroSpeedOption.NORMAL.name
    val speedOpt = try { HeroSpeedOption.valueOf(speedName) } catch (_: Exception) { HeroSpeedOption.NORMAL }

    val freqName = prefs.getString("powerup_frequency", PowerUpFrequency.NORMAL.name) ?: PowerUpFrequency.NORMAL.name
    val freqOpt = try { PowerUpFrequency.valueOf(freqName) } catch (_: Exception) { PowerUpFrequency.NORMAL }

    val gCount = prefs.getInt("ghost_count", 1).coerceIn(1, 3)
    val showSonic = prefs.getBoolean("show_sonic_button", true)

    val godUnlocked = champCount >= 10
    val godEnabled = godUnlocked && prefs.getBoolean("god_mode_enabled", false)

    audio.isSoundEnabled = sound
    audio.isHapticEnabled = haptic
    engine.difficulty = diff
    engine.customGhostSpeed = customSpeed
    engine.arenaTheme = theme
    engine.ghostCount = gCount
    engine.heroSpeedMultiplier = speedOpt.multiplier
    engine.powerUpFrequency = freqOpt
    engine.godMode = godEnabled
    engine.spawnCustomItemsEnabled = spawnCustom
    engine.customItemsPool = customItems.mapIndexed { idx, item -> item.toGameItemDef(idx) }
    engine.resetGameForMode(ALL_40_MODES.first(), overrideGhostCount = gCount)

    val baseState = _uiState.value.copy(
      championCount = champCount,
      bestTimeSec = bestTime,
      bestHighScore = bestScore,
      spendablePoints = savedPoints,
      reviveCount = savedReviveCount,
      autoUpdateEnabled = autoUpdatePref,
      playerLevel = playerLvl,
      playerXp = playerXp,
      xpToNextLevel = xpRequiredForLevel(playerLvl),
      totalCatsSaved = totalSaved,
      bestEndlessCats = bestEndless,
      totalGhostsScared = totalScared,
      totalItemsUsed = totalItems,
      totalDashesUsed = totalDashes,
      totalSonicsUsed = totalSonics,
      discoveredItems = savedItems + savedPurchasedItems,
      unlockedAchievements = savedAchievements,
      purchasedSkinIds = savedPurchasedSkins,
      purchasedModeIds = savedPurchasedModes,
      purchasedItemIds = savedPurchasedItems,
      tradedAwaySkinIds = savedTradedSkins,
      isEndlessUnlocked = champCount >= 1 || savedPurchasedModes.contains("endless_infinite"),
      isGodModeUnlocked = godUnlocked,
      isGodModeEnabled = godEnabled,
      is3dModeEnabled = is3d,
      is4dModeEnabled = is4d,
      customGhostSpeed = customSpeed,
      selectedSkinId = effectiveSavedSkin,
      customMap = customMap,
      customItems = customItems,
      spawnCustomItemsInAllModes = spawnCustom,
      hasCreatedCustomContent = hasCreated,
      arenaTheme = theme,
      ghostCount = gCount,
      heroSpeedOption = speedOpt,
      powerUpFrequency = freqOpt,
      showSonicButton = showSonic,
      controlType = ctrl,
      isSoundEnabled = sound,
      isHapticEnabled = haptic,
      difficulty = diff,
      isPlayerGhost = engine.isPlayerGhost,
      aiRunnerLives = engine.aiRunnerCat?.lives ?: 3,
      aiRunnerCats = engine.aiRunnerCat?.catsCollected ?: 0
    )

    val (blocked, reason) = computeRewardsBlocked(baseState, engine.activeCustomMap != null)
    _uiState.value = syncDerivedProgress(
      baseState.copy(isRewardsBlocked = blocked, rewardsBlockedReason = reason),
      showBanner = false
    )
  }

  private fun refreshRewardsBlockedFlag(state: GameUiState = _uiState.value): GameUiState {
    val (blocked, reason) = computeRewardsBlocked(state, engine.activeCustomMap != null)
    val next = state.copy(isRewardsBlocked = blocked, rewardsBlockedReason = reason)
    _uiState.value = next
    return next
  }

  fun isModeEffectivelyUnlocked(
    mode: GameModeDef,
    st: GameUiState = _uiState.value,
    winsOverride: Int = st.championCount,
    totalCatsOverride: Int = st.totalCatsSaved,
    bestEndlessOverride: Int = st.bestEndlessCats,
    achievementsOverride: Int = st.unlockedAchievements.size,
    discoveredItemsOverride: Int = st.discoveredItems.size,
    ghostsScaredOverride: Int = st.totalGhostsScared
  ): Boolean {
    return st.purchasedModeIds.contains(mode.id) || mode.isUnlocked(
      wins = winsOverride,
      totalCats = totalCatsOverride,
      bestEndless = bestEndlessOverride,
      achievementsCount = achievementsOverride,
      discoveredItemsCount = discoveredItemsOverride,
      ghostsScared = ghostsScaredOverride
    )
  }

  fun isSkinEffectivelyUnlocked(
    skin: HeroSkinDef,
    st: GameUiState = _uiState.value,
    winsOverride: Int = st.championCount,
    totalCatsOverride: Int = st.totalCatsSaved,
    bestEndlessOverride: Int = st.bestEndlessCats,
    achievementsOverride: Int = st.unlockedAchievements.size,
    discoveredItemsOverride: Int = st.discoveredItems.size,
    ghostsScaredOverride: Int = st.totalGhostsScared,
    dashesOverride: Int = st.totalDashesUsed,
    modesUnlockedOverride: Int = st.unlockedModesCount,
    hasCreatedOverride: Boolean = st.hasCreatedCustomContent
  ): Boolean {
    if (skin.id == "classic_orange") return true
    if (st.tradedAwaySkinIds.contains(skin.id)) return false
    return st.purchasedSkinIds.contains(skin.id) || skin.isUnlocked(
      wins = winsOverride,
      totalCats = totalCatsOverride,
      bestEndless = bestEndlessOverride,
      achievementsCount = achievementsOverride,
      discoveredItemsCount = discoveredItemsOverride,
      ghostsScared = ghostsScaredOverride,
      dashesUsed = dashesOverride,
      modesUnlocked = modesUnlockedOverride,
      hasCreatedCustom = hasCreatedOverride
    )
  }

  fun tradableSkins(st: GameUiState = _uiState.value): List<HeroSkinDef> {
    return ALL_HERO_SKINS.filter { it.id != "classic_orange" && isSkinEffectivelyUnlocked(it, st) }
  }

  fun shopCostForSkin(skin: HeroSkinDef): Int = 180 + (skin.number * 35)
  fun shopCostForMode(mode: GameModeDef): Int = 140 + (mode.number * 25)
  fun shopCostForItem(item: GameItemDef): Int = 80 + (item.number * 12)

  private fun computeUnlockedModesCount(
    state: GameUiState,
    wins: Int,
    totalCats: Int,
    bestEndless: Int,
    achievementsCount: Int,
    discoveredItemsCount: Int,
    ghostsScared: Int
  ): Int {
    return ALL_40_MODES.count {
      isModeEffectivelyUnlocked(
        mode = it,
        st = state,
        winsOverride = wins,
        totalCatsOverride = totalCats,
        bestEndlessOverride = bestEndless,
        achievementsOverride = achievementsCount,
        discoveredItemsOverride = discoveredItemsCount,
        ghostsScaredOverride = ghostsScared
      )
    }
  }

  private fun computeUnlockedSkinsCount(
    state: GameUiState,
    wins: Int,
    totalCats: Int,
    bestEndless: Int,
    achievementsCount: Int,
    discoveredItemsCount: Int,
    ghostsScared: Int,
    dashesUsed: Int,
    modesUnlocked: Int,
    hasCreatedCustom: Boolean
  ): Int {
    return ALL_HERO_SKINS.count {
      isSkinEffectivelyUnlocked(
        skin = it,
        st = state,
        winsOverride = wins,
        totalCatsOverride = totalCats,
        bestEndlessOverride = bestEndless,
        achievementsOverride = achievementsCount,
        discoveredItemsOverride = discoveredItemsCount,
        ghostsScaredOverride = ghostsScared,
        dashesOverride = dashesUsed,
        modesUnlockedOverride = modesUnlocked,
        hasCreatedOverride = hasCreatedCustom
      )
    }
  }

  private fun syncDerivedProgress(state: GameUiState, showBanner: Boolean): GameUiState {
    val (blocked, reason) = computeRewardsBlocked(state, engine.activeCustomMap != null)
    if (blocked) {
      val fallbackModes = computeUnlockedModesCount(
        state = state,
        wins = state.championCount,
        totalCats = state.totalCatsSaved,
        bestEndless = state.bestEndlessCats,
        achievementsCount = state.unlockedAchievements.size,
        discoveredItemsCount = state.discoveredItems.size,
        ghostsScared = state.totalGhostsScared
      )
      val fallbackSkins = computeUnlockedSkinsCount(
        state = state,
        wins = state.championCount,
        totalCats = state.totalCatsSaved,
        bestEndless = state.bestEndlessCats,
        achievementsCount = state.unlockedAchievements.size,
        discoveredItemsCount = state.discoveredItems.size,
        ghostsScared = state.totalGhostsScared,
        dashesUsed = state.totalDashesUsed,
        modesUnlocked = fallbackModes,
        hasCreatedCustom = state.hasCreatedCustomContent
      )
      return state.copy(
        isRewardsBlocked = true,
        rewardsBlockedReason = reason,
        unlockedModesCount = fallbackModes,
        unlockedSkinsCount = fallbackSkins
      )
    }

    val updatedAchievements = state.unlockedAchievements.toMutableSet()
    var lastNewAch: Achievement? = null

    val liveCatsTotal = state.totalCatsSaved +
      if (engine.status == GameStatus.PLAYING && !engine.isPlayerGhost) engine.catsCollected else 0
    val liveEndlessBest = max(
      state.bestEndlessCats,
      if (engine.endlessMode && !engine.isPlayerGhost) engine.catsCollected else 0
    )

    repeat(2) {
      val modesUnlocked = computeUnlockedModesCount(
        state = state,
        wins = state.championCount,
        totalCats = liveCatsTotal,
        bestEndless = liveEndlessBest,
        achievementsCount = updatedAchievements.size,
        discoveredItemsCount = state.discoveredItems.size,
        ghostsScared = state.totalGhostsScared
      )

      for (ach in ALL_40_ACHIEVEMENTS) {
        if (updatedAchievements.contains(ach.id)) continue
        val met = when (ach.progressType) {
          AchievementProgressType.WINS -> state.championCount >= ach.targetValue
          AchievementProgressType.TOTAL_CATS -> liveCatsTotal >= ach.targetValue
          AchievementProgressType.ENDLESS_BEST -> liveEndlessBest >= ach.targetValue
          AchievementProgressType.ITEMS_DISCOVERED -> state.discoveredItems.size >= ach.targetValue
          AchievementProgressType.TOTAL_ITEMS_USED -> state.totalItemsUsed >= ach.targetValue
          AchievementProgressType.GHOSTS_SCARED -> state.totalGhostsScared >= ach.targetValue
          AchievementProgressType.DASHES_USED -> state.totalDashesUsed >= ach.targetValue
          AchievementProgressType.SONICS_USED -> state.totalSonicsUsed >= ach.targetValue
          AchievementProgressType.MODES_UNLOCKED -> modesUnlocked >= ach.targetValue
          AchievementProgressType.SINGLE_EVENT -> when (ach.id) {
            "powerup_fanatic" -> engine.powerUpsCollectedThisRun >= 5
            "combo_5" -> engine.maxComboThisRun >= 5
            else -> false
          }
        }
        if (met) {
          updatedAchievements.add(ach.id)
          lastNewAch = ach
        }
      }
    }

    val finalModesCount = computeUnlockedModesCount(
      state = state,
      wins = state.championCount,
      totalCats = liveCatsTotal,
      bestEndless = liveEndlessBest,
      achievementsCount = updatedAchievements.size,
      discoveredItemsCount = state.discoveredItems.size,
      ghostsScared = state.totalGhostsScared
    )

    val finalSkinsCount = computeUnlockedSkinsCount(
      state = state,
      wins = state.championCount,
      totalCats = liveCatsTotal,
      bestEndless = liveEndlessBest,
      achievementsCount = updatedAchievements.size,
      discoveredItemsCount = state.discoveredItems.size,
      ghostsScared = state.totalGhostsScared,
      dashesUsed = state.totalDashesUsed,
      modesUnlocked = finalModesCount,
      hasCreatedCustom = state.hasCreatedCustomContent
    )

    if (updatedAchievements.size != state.unlockedAchievements.size) {
      prefs.edit().putStringSet("unlocked_achievements", updatedAchievements).apply()
      if (showBanner && lastNewAch != null) {
        triggerAchievementBanner(lastNewAch!!)
      }
    }

    return state.copy(
      isRewardsBlocked = false,
      rewardsBlockedReason = "",
      unlockedAchievements = updatedAchievements,
      unlockedModesCount = finalModesCount,
      unlockedSkinsCount = finalSkinsCount,
      isEndlessUnlocked = state.championCount >= 1 || state.purchasedModeIds.contains("endless_infinite"),
      newlyUnlockedAchievement = if (showBanner && lastNewAch != null) lastNewAch else state.newlyUnlockedAchievement
    )
  }

  private fun triggerAchievementBanner(ach: Achievement) {
    audio.playAchievementSound()
    bannerDismissJob?.cancel()
    bannerDismissJob = viewModelScope.launch {
      delay(4000)
      _uiState.value = _uiState.value.copy(newlyUnlockedAchievement = null)
    }
  }

  fun restoreDefaultFairSettings() {
    engine.godMode = false
    engine.difficulty = Difficulty.NORMAL
    engine.heroSpeedMultiplier = HeroSpeedOption.NORMAL.multiplier
    engine.powerUpFrequency = PowerUpFrequency.NORMAL
    engine.spawnCustomItemsEnabled = false
    engine.activeCustomMap = null

    prefs.edit()
      .putBoolean("god_mode_enabled", false)
      .putString("difficulty", Difficulty.NORMAL.name)
      .putString("hero_speed_option", HeroSpeedOption.NORMAL.name)
      .putString("powerup_frequency", PowerUpFrequency.NORMAL.name)
      .putBoolean("spawn_custom_items", false)
      .apply()

    val next = _uiState.value.copy(
      isGodModeEnabled = false,
      difficulty = Difficulty.NORMAL,
      heroSpeedOption = HeroSpeedOption.NORMAL,
      powerUpFrequency = PowerUpFrequency.NORMAL,
      spawnCustomItemsInAllModes = false
    )
    val refreshed = refreshRewardsBlockedFlag(next)
    _uiState.value = syncDerivedProgress(refreshed, showBanner = false)
  }

  fun setControlType(type: ControlType) {
    _uiState.value = _uiState.value.copy(controlType = type)
    prefs.edit().putString("control_type", type.name).apply()
  }

  fun toggleSound() {
    val newState = !_uiState.value.isSoundEnabled
    audio.isSoundEnabled = newState
    _uiState.value = _uiState.value.copy(isSoundEnabled = newState)
    prefs.edit().putBoolean("sound_enabled", newState).apply()
  }

  fun toggleHaptic() {
    val newState = !_uiState.value.isHapticEnabled
    audio.isHapticEnabled = newState
    _uiState.value = _uiState.value.copy(isHapticEnabled = newState)
    prefs.edit().putBoolean("haptic_enabled", newState).apply()
  }

  fun toggle3dMode() {
    val newVal = !_uiState.value.is3dModeEnabled
    _uiState.value = _uiState.value.copy(is3dModeEnabled = newVal)
    prefs.edit().putBoolean("mode_3d_enabled", newVal).apply()
  }

  fun toggle4dMode() {
    val newVal = !_uiState.value.is4dModeEnabled
    _uiState.value = _uiState.value.copy(is4dModeEnabled = newVal)
    prefs.edit().putBoolean("mode_4d_enabled", newVal).apply()
  }

  fun setDifficulty(diff: Difficulty) {
    engine.difficulty = diff
    prefs.edit().putString("difficulty", diff.name).apply()
    if (engine.status == GameStatus.READY) {
      engine.resetGameForMode(engine.currentMode, overrideGhostCount = _uiState.value.ghostCount)
    }
    refreshRewardsBlockedFlag(
      _uiState.value.copy(
        difficulty = diff,
        isPlayerGhost = engine.isPlayerGhost,
        aiRunnerLives = engine.aiRunnerCat?.lives ?: 3,
        aiRunnerCats = engine.aiRunnerCat?.catsCollected ?: 0
      )
    )
  }

  fun setCustomGhostSpeed(speed: Int) {
    val clamped = speed.coerceIn(1, 1000)
    engine.customGhostSpeed = clamped
    engine.difficulty = Difficulty.CUSTOM
    prefs.edit()
      .putInt("custom_ghost_speed", clamped)
      .putString("difficulty", Difficulty.CUSTOM.name)
      .apply()
    refreshRewardsBlockedFlag(
      _uiState.value.copy(
        customGhostSpeed = clamped,
        difficulty = Difficulty.CUSTOM,
        isPlayerGhost = engine.isPlayerGhost
      )
    )
  }

  fun selectSkin(skinId: String) {
    val st = _uiState.value
    val skin = ALL_HERO_SKINS.find { it.id == skinId } ?: return
    if (!isSkinEffectivelyUnlocked(skin, st)) return
    _uiState.value = st.copy(selectedSkinId = skinId)
    prefs.edit().putString("selected_skin_id", skinId).apply()
  }

  fun buySkin(skinId: String, tradeSkinId: String? = null): Boolean {
    val st = _uiState.value
    val skin = ALL_HERO_SKINS.find { it.id == skinId } ?: return false
    if (isSkinEffectivelyUnlocked(skin, st)) {
      selectSkin(skinId)
      return true
    }

    var nextPoints = st.spendablePoints
    val nextPurchasedSkins = st.purchasedSkinIds.toMutableSet()
    val nextTradedAway = st.tradedAwaySkinIds.toMutableSet()
    var nextSelectedSkin = st.selectedSkinId

    if (!tradeSkinId.isNullOrBlank()) {
      val tradedSkin = ALL_HERO_SKINS.find { it.id == tradeSkinId } ?: return false
      if (tradedSkin.id == "classic_orange" || tradedSkin.id == skinId || !isSkinEffectivelyUnlocked(tradedSkin, st)) {
        return false
      }
      nextPurchasedSkins.remove(tradedSkin.id)
      nextTradedAway.add(tradedSkin.id)
      if (nextSelectedSkin == tradedSkin.id) nextSelectedSkin = skinId
    } else {
      val cost = shopCostForSkin(skin)
      if (nextPoints < cost) return false
      nextPoints -= cost
    }

    nextTradedAway.remove(skinId)
    nextPurchasedSkins.add(skinId)
    nextSelectedSkin = skinId

    prefs.edit()
      .putInt("spendable_points", nextPoints)
      .putStringSet("purchased_skins", nextPurchasedSkins)
      .putStringSet("traded_away_skins", nextTradedAway)
      .putString("selected_skin_id", nextSelectedSkin)
      .apply()

    val updated = st.copy(
      spendablePoints = nextPoints,
      purchasedSkinIds = nextPurchasedSkins,
      tradedAwaySkinIds = nextTradedAway,
      selectedSkinId = nextSelectedSkin
    )
    _uiState.value = syncDerivedProgress(updated, showBanner = false)
    showTransientFeedback("${skin.badgeEmoji} Skin '${skin.name}' desbloqueada e equipada!")
    return true
  }

  fun buyMode(modeId: String, tradeSkinId: String? = null): Boolean {
    val st = _uiState.value
    val mode = ALL_40_MODES.find { it.id == modeId } ?: return false
    if (isModeEffectivelyUnlocked(mode, st)) return true

    var nextPoints = st.spendablePoints
    val nextPurchasedSkins = st.purchasedSkinIds.toMutableSet()
    val nextTradedAway = st.tradedAwaySkinIds.toMutableSet()
    var nextSelectedSkin = st.selectedSkinId

    if (!tradeSkinId.isNullOrBlank()) {
      val tradedSkin = ALL_HERO_SKINS.find { it.id == tradeSkinId } ?: return false
      if (tradedSkin.id == "classic_orange" || !isSkinEffectivelyUnlocked(tradedSkin, st)) {
        return false
      }
      nextPurchasedSkins.remove(tradedSkin.id)
      nextTradedAway.add(tradedSkin.id)
      if (nextSelectedSkin == tradedSkin.id) nextSelectedSkin = "classic_orange"
    } else {
      val cost = shopCostForMode(mode)
      if (nextPoints < cost) return false
      nextPoints -= cost
    }

    val nextPurchasedModes = st.purchasedModeIds + modeId
    prefs.edit()
      .putInt("spendable_points", nextPoints)
      .putStringSet("purchased_skins", nextPurchasedSkins)
      .putStringSet("traded_away_skins", nextTradedAway)
      .putStringSet("purchased_modes", nextPurchasedModes)
      .putString("selected_skin_id", nextSelectedSkin)
      .apply()

    val updated = st.copy(
      spendablePoints = nextPoints,
      purchasedSkinIds = nextPurchasedSkins,
      tradedAwaySkinIds = nextTradedAway,
      purchasedModeIds = nextPurchasedModes,
      selectedSkinId = nextSelectedSkin
    )
    _uiState.value = syncDerivedProgress(updated, showBanner = true)
    showTransientFeedback("${mode.emoji} Modo '${mode.name}' comprado e liberado!")
    return true
  }

  fun buyItem(itemId: String, tradeSkinId: String? = null): Boolean {
    val st = _uiState.value
    val item = ALL_40_ITEMS.find { it.id == itemId } ?: return false
    if (st.purchasedItemIds.contains(itemId) || st.discoveredItems.contains(itemId)) return true

    var nextPoints = st.spendablePoints
    val nextPurchasedSkins = st.purchasedSkinIds.toMutableSet()
    val nextTradedAway = st.tradedAwaySkinIds.toMutableSet()
    var nextSelectedSkin = st.selectedSkinId

    if (!tradeSkinId.isNullOrBlank()) {
      val tradedSkin = ALL_HERO_SKINS.find { it.id == tradeSkinId } ?: return false
      if (tradedSkin.id == "classic_orange" || !isSkinEffectivelyUnlocked(tradedSkin, st)) {
        return false
      }
      nextPurchasedSkins.remove(tradedSkin.id)
      nextTradedAway.add(tradedSkin.id)
      if (nextSelectedSkin == tradedSkin.id) nextSelectedSkin = "classic_orange"
    } else {
      val cost = shopCostForItem(item)
      if (nextPoints < cost) return false
      nextPoints -= cost
    }

    val nextPurchasedItems = st.purchasedItemIds + itemId
    val nextDiscoveredItems = st.discoveredItems + itemId
    prefs.edit()
      .putInt("spendable_points", nextPoints)
      .putStringSet("purchased_skins", nextPurchasedSkins)
      .putStringSet("traded_away_skins", nextTradedAway)
      .putStringSet("purchased_items", nextPurchasedItems)
      .putStringSet("discovered_items", nextDiscoveredItems)
      .putString("selected_skin_id", nextSelectedSkin)
      .apply()

    val updated = st.copy(
      spendablePoints = nextPoints,
      purchasedSkinIds = nextPurchasedSkins,
      tradedAwaySkinIds = nextTradedAway,
      purchasedItemIds = nextPurchasedItems,
      discoveredItems = nextDiscoveredItems,
      selectedSkinId = nextSelectedSkin
    )
    _uiState.value = syncDerivedProgress(updated, showBanner = true)
    showTransientFeedback("${item.emoji} Item '${item.name}' desbloqueado na coleção!")
    return true
  }

  fun buyRevivePack(quantity: Int, pointCost: Int, tradeSkinId: String? = null): Boolean {
    if (quantity <= 0) return false
    val st = _uiState.value
    var nextPoints = st.spendablePoints
    val nextPurchasedSkins = st.purchasedSkinIds.toMutableSet()
    val nextTradedAway = st.tradedAwaySkinIds.toMutableSet()
    var nextSelectedSkin = st.selectedSkinId

    if (!tradeSkinId.isNullOrBlank()) {
      val tradedSkin = ALL_HERO_SKINS.find { it.id == tradeSkinId } ?: return false
      if (tradedSkin.id == "classic_orange" || !isSkinEffectivelyUnlocked(tradedSkin, st)) {
        return false
      }
      nextPurchasedSkins.remove(tradedSkin.id)
      nextTradedAway.add(tradedSkin.id)
      if (nextSelectedSkin == tradedSkin.id) nextSelectedSkin = "classic_orange"
    } else {
      if (nextPoints < pointCost) return false
      nextPoints -= pointCost
    }

    val nextRevives = (st.reviveCount + quantity).coerceAtMost(9999)
    prefs.edit()
      .putInt("spendable_points", nextPoints)
      .putInt("revive_count", nextRevives)
      .putStringSet("purchased_skins", nextPurchasedSkins)
      .putStringSet("traded_away_skins", nextTradedAway)
      .putString("selected_skin_id", nextSelectedSkin)
      .apply()

    val updated = st.copy(
      spendablePoints = nextPoints,
      reviveCount = nextRevives,
      purchasedSkinIds = nextPurchasedSkins,
      tradedAwaySkinIds = nextTradedAway,
      selectedSkinId = nextSelectedSkin
    )
    _uiState.value = syncDerivedProgress(updated, showBanner = false)
    showTransientFeedback("💖 +$quantity Revive(s) comprado(s)! Estoque: $nextRevives")
    return true
  }

  fun useRevive(): Boolean {
    val st = _uiState.value
    if (st.reviveCount <= 0) return false
    val revived = engine.revivePlayer()
    if (!revived) return false

    val nextRevives = (st.reviveCount - 1).coerceAtLeast(0)
    prefs.edit().putInt("revive_count", nextRevives).apply()

    _uiState.value = st.copy(
      reviveCount = nextRevives,
      gameStatus = engine.status,
      lives = engine.lives,
      maxLives = engine.maxLives,
      timeLimitRemaining = engine.timeLimitRemaining,
      isInvincible = engine.isInvincible,
      showGameOverDialog = false
    )
    showTransientFeedback("💖 Revive ativado! Restam $nextRevives revive(s).")
    return true
  }

  fun setAutoUpdateEnabled(enabled: Boolean) {
    prefs.edit().putBoolean("auto_update_enabled", enabled).apply()
    _uiState.value = _uiState.value.copy(autoUpdateEnabled = enabled)
  }

  fun syncAppUpdateUi(
    statusText: String,
    isAvailable: Boolean,
    isDownloading: Boolean,
    progressPercent: Int
  ) {
    _uiState.value = _uiState.value.copy(
      appUpdateStatusText = statusText,
      isUpdateAvailable = isAvailable,
      isUpdateDownloading = isDownloading,
      updateProgressPercent = progressPercent
    )
  }

  fun claimSharedSkin(skinId: String) {
    val st = _uiState.value
    val skin = ALL_HERO_SKINS.find { it.id == skinId } ?: return
    val nextTraded = st.tradedAwaySkinIds - skinId
    val nextPurchased = st.purchasedSkinIds + skinId
    prefs.edit()
      .putStringSet("traded_away_skins", nextTraded)
      .putStringSet("purchased_skins", nextPurchased)
      .putString("selected_skin_id", skinId)
      .apply()
    val updated = st.copy(
      tradedAwaySkinIds = nextTraded,
      purchasedSkinIds = nextPurchased,
      selectedSkinId = skinId
    )
    _uiState.value = syncDerivedProgress(updated, showBanner = false)
    showTransientFeedback("${skin.badgeEmoji} Skin '${skin.name}' recebida e equipada!")
  }

  fun saveCustomMap(mapConfig: CustomMapConfig, playNow: Boolean) {
    prefs.edit()
      .putString("custom_map_json", mapConfig.toJsonString())
      .putBoolean("has_created_custom_content", true)
      .apply()

    val nextState = _uiState.value.copy(
      customMap = mapConfig,
      hasCreatedCustomContent = true
    )
    _uiState.value = syncDerivedProgress(nextState, showBanner = true)

    if (playNow) {
      playCustomMap()
    }
  }

  fun playCustomMap() {
    val st = _uiState.value
    resetRunSyncCounters()
    engine.resetGameForCustomMap(st.customMap)
    engine.startPlaying()
    val next = st.copy(
      gameStatus = GameStatus.PLAYING,
      currentMode = engine.currentMode,
      isEndlessMode = engine.endlessMode,
      catsCollected = 0,
      goalCats = engine.goalCats,
      lives = engine.lives,
      maxLives = engine.maxLives,
      matchLevel = 1,
      currentScore = 0,
      lastXpEarned = 0,
      activeGhostsCount = engine.ghosts.size,
      timeLimitRemaining = 0f,
      showVictoryDialog = false,
      showGameOverDialog = false,
      showModesDialog = false,
      showCreatorDialog = false,
      lastVictoryUnlocks = emptyList()
    )
    refreshRewardsBlockedFlag(next)
  }

  fun saveCustomItem(item: CustomItemConfig) {
    val existing = _uiState.value.customItems.toMutableList()
    val idx = existing.indexOfFirst { it.id == item.id }
    if (idx >= 0) existing[idx] = item else existing.add(0, item)

    engine.customItemsPool = existing.mapIndexed { i, cfg -> cfg.toGameItemDef(i) }
    prefs.edit()
      .putString("custom_items_json", CustomItemConfig.listToJson(existing))
      .putBoolean("has_created_custom_content", true)
      .apply()

    val nextState = _uiState.value.copy(
      customItems = existing,
      hasCreatedCustomContent = true
    )
    val (blocked, reason) = computeRewardsBlocked(nextState, engine.activeCustomMap != null)
    _uiState.value = syncDerivedProgress(
      nextState.copy(isRewardsBlocked = blocked, rewardsBlockedReason = reason),
      showBanner = !blocked
    )
  }

  fun deleteCustomItem(itemId: String) {
    val updated = _uiState.value.customItems.filterNot { it.id == itemId }
    engine.customItemsPool = updated.mapIndexed { i, cfg -> cfg.toGameItemDef(i) }
    prefs.edit().putString("custom_items_json", CustomItemConfig.listToJson(updated)).apply()
    refreshRewardsBlockedFlag(_uiState.value.copy(customItems = updated))
  }

  fun toggleSpawnCustomItems() {
    val newVal = !_uiState.value.spawnCustomItemsInAllModes
    engine.spawnCustomItemsEnabled = newVal
    prefs.edit().putBoolean("spawn_custom_items", newVal).apply()
    refreshRewardsBlockedFlag(_uiState.value.copy(spawnCustomItemsInAllModes = newVal))
  }

  fun setArenaTheme(theme: ArenaTheme) {
    engine.arenaTheme = theme
    _uiState.value = _uiState.value.copy(arenaTheme = theme)
    prefs.edit().putString("arena_theme", theme.name).apply()
  }

  fun setGhostCount(count: Int) {
    val safeCount = count.coerceIn(1, 3)
    engine.ghostCount = safeCount
    _uiState.value = _uiState.value.copy(ghostCount = safeCount)
    prefs.edit().putInt("ghost_count", safeCount).apply()
  }

  fun setHeroSpeedOption(option: HeroSpeedOption) {
    engine.heroSpeedMultiplier = option.multiplier
    prefs.edit().putString("hero_speed_option", option.name).apply()
    refreshRewardsBlockedFlag(_uiState.value.copy(heroSpeedOption = option))
  }

  fun setPowerUpFrequency(freq: PowerUpFrequency) {
    engine.powerUpFrequency = freq
    prefs.edit().putString("powerup_frequency", freq.name).apply()
    refreshRewardsBlockedFlag(_uiState.value.copy(powerUpFrequency = freq))
  }

  fun toggleSonicButton() {
    val newVal = !_uiState.value.showSonicButton
    _uiState.value = _uiState.value.copy(showSonicButton = newVal)
    prefs.edit().putBoolean("show_sonic_button", newVal).apply()
  }

  fun toggleGodMode() {
    if (!_uiState.value.isGodModeUnlocked) return
    val newVal = !_uiState.value.isGodModeEnabled
    engine.godMode = newVal
    prefs.edit().putBoolean("god_mode_enabled", newVal).apply()
    refreshRewardsBlockedFlag(_uiState.value.copy(isGodModeEnabled = newVal))
  }

  fun onStartGame() {
    startNormalGame()
  }

  fun startNormalGame() {
    startMode(ALL_40_MODES[0])
  }

  fun startEndlessGame() {
    if (!_uiState.value.isEndlessUnlocked) return
    startMode(ALL_40_MODES[1])
  }

  fun startBotsGame() {
    startMode(BOTS_ARENA_MODE)
  }

  fun startMode(mode: GameModeDef) {
    val st = _uiState.value
    if (!mode.isBotsMode && !isModeEffectivelyUnlocked(mode, st)) return

    resetRunSyncCounters()
    engine.resetGameForMode(mode, overrideGhostCount = st.ghostCount)
    engine.startPlaying()
    val next = st.copy(
      gameStatus = GameStatus.PLAYING,
      currentMode = mode,
      isEndlessMode = mode.isEndless,
      isBotsMode = mode.isBotsMode,
      botCount = if (mode.isBotsMode) engine.bots.size else 0,
      playerBotRank = 1,
      leadingBotName = engine.bots.firstOrNull()?.name.orEmpty(),
      leadingBotCats = 0,
      winnerBotName = null,
      catsCollected = 0,
      goalCats = engine.goalCats,
      lives = engine.lives,
      maxLives = engine.maxLives,
      matchLevel = 1,
      currentScore = 0,
      lastXpEarned = 0,
      activeGhostsCount = engine.ghosts.size,
      timeLimitRemaining = engine.timeLimitRemaining,
      showVictoryDialog = false,
      showGameOverDialog = false,
      showModesDialog = false,
      showItemsDialog = false,
      showCreatorDialog = false,
      showOnlineDialog = false,
      lastVictoryUnlocks = emptyList()
    )
    refreshRewardsBlockedFlag(next)
  }

  fun restartCurrentMode() {
    if (engine.activeCustomMap != null || _uiState.value.currentMode.id == "custom_map") {
      playCustomMap()
    } else {
      startMode(_uiState.value.currentMode)
    }
  }

  private fun resetRunSyncCounters() {
    lastSyncedGhostsScared = 0
    lastSyncedDashes = 0
    lastSyncedSonics = 0
    lastSyncedItemIndex = 0
  }

  fun returnToMenu() {
    engine.status = GameStatus.READY
    val next = _uiState.value.copy(
      gameStatus = GameStatus.READY,
      showVictoryDialog = false,
      showGameOverDialog = false
    )
    refreshRewardsBlockedFlag(next)
  }

  fun onTogglePause() {
    engine.togglePause()
    _uiState.value = _uiState.value.copy(gameStatus = engine.status)
  }

  fun onTriggerDash() {
    engine.triggerDash()
  }

  fun onTriggerSonicWave() {
    engine.triggerSonicWave()
  }

  fun askAiTip(customQuestion: String?) {
    val st = _uiState.value
    val activeBuffs = buildList {
      if (st.shieldActive) add("Escudo de Leite")
      if (st.catnipActive) add("Velocidade")
      if (st.goldenFishActive) add("Fantasmas em pânico")
      if (st.freezeActive) add("Fantasmas congelados")
      if (st.magnetActive || st.giantMagnetActive) add("Ímã")
      if (st.invisibilityActive) add("Invisibilidade")
      if (st.doubleCatActive) add("Gatos x2")
    }.ifEmpty { listOf("Nenhum") }.joinToString(", ")

    val roleDesc = if (st.isPlayerGhost) {
      "Fantasma Caçador (Elite) — precisa capturar o gato corredor ${st.goalCats}x antes que ele resgate 10 gatos!"
    } else {
      "Herói Felino"
    }

    val telemetry = """
      Você é o 'Guia Felino', coach de IA em português (PT-BR) do jogo arcade 'Gato Campeão'.
      Estado atual da partida do jogador:
      - Papel: $roleDesc
      - Modo: ${st.currentMode.name} (${if (st.isEndlessMode) "Infinito" else "Meta ${st.goalCats}"})
      - Progresso na rodada: ${st.catsCollected}/${st.goalCats}
      - Pontuação atual: ${st.currentScore} (Recorde: ${st.bestHighScore}) | Pontos na Loja: ${st.spendablePoints}
      - Nível na partida: ${st.matchLevel} | Nível de carreira: ${st.playerLevel}
      - Vidas: ${st.lives}/${st.maxLives}
      - Dificuldade: ${st.difficulty.label} | Fantasmas: ${st.activeGhostsCount} | Distância do alvo/fantasma: ${st.ghostDistance.toInt()} px
      - Habilidades disponíveis: Turbo ⚡ (${if (st.dashCooldownRatio <= 0.01f) "Pronto" else "Recarregando"}), Onda Sônica 🔔 (${if (st.sonicCooldownRatio <= 0.01f) "Pronta" else "Recarregando"})
      - Buffs ativos: $activeBuffs
      - Modo Deus/Modificações bloqueando conquistas: ${if (st.isRewardsBlocked) "Sim (${st.rewardsBlockedReason})" else "Não"}
      Pergunta do jogador: ${customQuestion ?: "Me dê 2 ou 3 dicas práticas e curtas para eu vencer e subir de nível agora!"}
      Responda em português do Brasil, de forma objetiva, divertida e prática (máximo 5 linhas).
    """.trimIndent()

    aiJob?.cancel()
    aiJob = viewModelScope.launch {
      aiHelperRepository.streamGameTip(telemetry).collect { tipState ->
        _aiTipState.value = tipState
      }
    }
  }

  fun showTransientFeedback(message: String) {
    feedbackDismissJob?.cancel()
    _uiState.value = _uiState.value.copy(notificationFeedback = message)
    feedbackDismissJob = viewModelScope.launch {
      delay(3800)
      _uiState.value = _uiState.value.copy(notificationFeedback = null)
    }
  }

  fun sendMeowNotification(context: Context) {
    val posted = CatNotificationHelper.sendCatMeowNotification(context)
    audio.playMeowNotificationSound()
    showTransientFeedback(
      if (posted) {
        "🔔 Notificação enviada: \"meow meow meow\""
      } else {
        "🐾 Permita notificações para ver \"meow meow meow\" na barra!"
      }
    )
  }

  fun updateMultiplayerPlayers(
    roomCode: String?,
    players: List<RoomPlayer>,
    myUserId: String,
    myRole: String,
    maxPlayers: Int,
    winnerUsername: String?,
    leftPlayerMessage: String? = null
  ) {
    val others = players.filter { it.userId != myUserId }.map { p ->
      RemotePartnerState(
        userId = p.userId,
        username = p.username.ifBlank { "Jogador" },
        position = Vector2D(p.x.toFloat(), p.y.toFloat()),
        score = p.score.toInt(),
        role = p.role.ifBlank { "cat" },
        skinId = p.skinId.ifBlank { "classic_orange" }
      )
    }
    engine.setMultiplayerParticipants(
      active = roomCode != null,
      myRole = myRole,
      players = others,
      captureGoal = 3
    )
    val topPartner = others.maxByOrNull { it.score } ?: others.firstOrNull()
    _uiState.value = _uiState.value.copy(
      activeRoomCode = roomCode,
      partnerUsername = topPartner?.username,
      partnerScore = topPartner?.score ?: 0,
      multiplayerPlayerCount = players.size,
      multiplayerMaxPlayers = maxPlayers,
      multiplayerRole = myRole,
      roomWinnerUsername = winnerUsername,
      isPlayerGhost = engine.isPlayerGhost
    )
    if (!leftPlayerMessage.isNullOrBlank()) {
      showTransientFeedback(leftPlayerMessage)
    }
  }

  fun exportCloudSavePayload(): CloudSavePayload {
    val st = _uiState.value
    return CloudSavePayload(
      championCount = st.championCount,
      bestTimeSec = st.bestTimeSec,
      bestHighScore = st.bestHighScore,
      spendablePoints = st.spendablePoints,
      playerLevel = st.playerLevel,
      playerXp = st.playerXp,
      totalCatsSaved = st.totalCatsSaved,
      bestEndlessCats = st.bestEndlessCats,
      totalGhostsScared = st.totalGhostsScared,
      totalItemsUsed = st.totalItemsUsed,
      totalDashesUsed = st.totalDashesUsed,
      totalSonicsUsed = st.totalSonicsUsed,
      selectedSkinId = st.selectedSkinId,
      discoveredItemsCsv = st.discoveredItems.joinToString(","),
      unlockedAchievementsCsv = st.unlockedAchievements.joinToString(","),
      purchasedSkinsCsv = st.purchasedSkinIds.joinToString(","),
      purchasedModesCsv = st.purchasedModeIds.joinToString(","),
      purchasedItemsCsv = st.purchasedItemIds.joinToString(","),
      tradedAwaySkinsCsv = st.tradedAwaySkinIds.joinToString(","),
      reviveCount = st.reviveCount
    )
  }

  fun applyCloudSaveProfile(profile: UserProfile) {
    fun parseCsv(csv: String): Set<String> =
      csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()

    val st = _uiState.value
    val restoredWins = max(st.championCount, max(profile.championCount.toInt(), profile.wins.toInt()))
    val restoredBestTime = if (profile.bestTimeSec > 0.0 && (st.bestTimeSec <= 0f || profile.bestTimeSec < st.bestTimeSec)) {
      profile.bestTimeSec.toFloat()
    } else {
      st.bestTimeSec
    }
    val restoredHighScore = max(st.bestHighScore, profile.bestHighScore.toInt())
    val restoredPoints = max(st.spendablePoints, profile.spendablePoints.toInt())
    val restoredRevives = max(st.reviveCount, profile.reviveCount.toInt())
    val restoredLevel = max(st.playerLevel, max(profile.playerLevel.toInt(), profile.level.toInt())).coerceAtLeast(1)
    val restoredXp = if (restoredLevel > st.playerLevel) profile.playerXp.toInt() else max(st.playerXp, profile.playerXp.toInt())
    val restoredCats = max(st.totalCatsSaved, profile.totalCatsSaved.toInt())
    val restoredEndless = max(st.bestEndlessCats, profile.bestEndlessCats.toInt())
    val restoredScared = max(st.totalGhostsScared, profile.totalGhostsScared.toInt())
    val restoredItemsUsed = max(st.totalItemsUsed, profile.totalItemsUsed.toInt())
    val restoredDashes = max(st.totalDashesUsed, profile.totalDashesUsed.toInt())
    val restoredSonics = max(st.totalSonicsUsed, profile.totalSonicsUsed.toInt())

    val restoredPurchasedSkins = st.purchasedSkinIds + parseCsv(profile.purchasedSkinsCsv)
    val restoredPurchasedModes = st.purchasedModeIds + parseCsv(profile.purchasedModesCsv)
    val restoredPurchasedItems = st.purchasedItemIds + parseCsv(profile.purchasedItemsCsv)
    val restoredDiscoveredItems = st.discoveredItems + parseCsv(profile.discoveredItemsCsv) + restoredPurchasedItems
    val restoredAchievements = st.unlockedAchievements + parseCsv(profile.unlockedAchievementsCsv)
    val restoredTraded = (parseCsv(profile.tradedAwaySkinsCsv) - restoredPurchasedSkins) - "classic_orange"
    val restoredSkin = profile.selectedSkinId.ifBlank { st.selectedSkinId }

    prefs.edit()
      .putInt("champion_count", restoredWins)
      .putFloat("best_time_sec", restoredBestTime)
      .putInt("best_high_score", restoredHighScore)
      .putInt("spendable_points", restoredPoints)
      .putInt("revive_count", restoredRevives)
      .putInt("player_level", restoredLevel)
      .putInt("player_xp", restoredXp)
      .putInt("total_cats_saved", restoredCats)
      .putInt("best_endless_cats", restoredEndless)
      .putInt("total_ghosts_scared", restoredScared)
      .putInt("total_items_used", restoredItemsUsed)
      .putInt("total_dashes_used", restoredDashes)
      .putInt("total_sonics_used", restoredSonics)
      .putStringSet("discovered_items", restoredDiscoveredItems)
      .putStringSet("unlocked_achievements", restoredAchievements)
      .putStringSet("purchased_skins", restoredPurchasedSkins)
      .putStringSet("purchased_modes", restoredPurchasedModes)
      .putStringSet("purchased_items", restoredPurchasedItems)
      .putStringSet("traded_away_skins", restoredTraded)
      .putString("selected_skin_id", restoredSkin)
      .apply()

    val next = st.copy(
      championCount = restoredWins,
      bestTimeSec = restoredBestTime,
      bestHighScore = restoredHighScore,
      spendablePoints = restoredPoints,
      reviveCount = restoredRevives,
      playerLevel = restoredLevel,
      playerXp = restoredXp,
      xpToNextLevel = xpRequiredForLevel(restoredLevel),
      totalCatsSaved = restoredCats,
      bestEndlessCats = restoredEndless,
      totalGhostsScared = restoredScared,
      totalItemsUsed = restoredItemsUsed,
      totalDashesUsed = restoredDashes,
      totalSonicsUsed = restoredSonics,
      discoveredItems = restoredDiscoveredItems,
      unlockedAchievements = restoredAchievements,
      purchasedSkinIds = restoredPurchasedSkins,
      purchasedModeIds = restoredPurchasedModes,
      purchasedItemIds = restoredPurchasedItems,
      tradedAwaySkinIds = restoredTraded,
      selectedSkinId = restoredSkin,
      isEndlessUnlocked = restoredWins >= 1 || restoredPurchasedModes.contains("endless_infinite"),
      isGodModeUnlocked = restoredWins >= 10
    )
    _uiState.value = syncDerivedProgress(next, showBanner = true)
    showTransientFeedback("☁️ Todo seu progresso foi restaurado da conta Google!")
  }

  fun openGallery(open: Boolean) {
    _uiState.value = _uiState.value.copy(showGalleryDialog = open)
  }

  fun openSettings(open: Boolean) {
    _uiState.value = _uiState.value.copy(showSettingsDialog = open)
  }

  fun openAchievements(open: Boolean) {
    _uiState.value = _uiState.value.copy(showAchievementsDialog = open)
  }

  fun openModes(open: Boolean) {
    _uiState.value = _uiState.value.copy(showModesDialog = open)
  }

  fun openItems(open: Boolean) {
    _uiState.value = _uiState.value.copy(showItemsDialog = open)
  }

  fun openSkins(open: Boolean) {
    _uiState.value = _uiState.value.copy(showSkinsDialog = open)
  }

  fun openShop(open: Boolean) {
    _uiState.value = _uiState.value.copy(showShopDialog = open)
  }

  fun openCreator(open: Boolean) {
    _uiState.value = _uiState.value.copy(showCreatorDialog = open)
  }

  fun openAiDialog(open: Boolean) {
    _uiState.value = _uiState.value.copy(showAiDialog = open)
  }

  fun openOnlineDialog(open: Boolean) {
    _uiState.value = _uiState.value.copy(showOnlineDialog = open)
  }

  fun openHelp(open: Boolean) {
    _uiState.value = _uiState.value.copy(showHelpDialog = open)
  }

  fun closeVictoryDialog() {
    _uiState.value = _uiState.value.copy(showVictoryDialog = false)
  }

  fun closeGameOverDialog() {
    _uiState.value = _uiState.value.copy(showGameOverDialog = false)
  }

  fun dismissAchievementBanner() {
    _uiState.value = _uiState.value.copy(newlyUnlockedAchievement = null)
  }

  fun playEndlessAfterVictory() {
    engine.endlessMode = true
    while (engine.activeCollectibles.size < 12) {
      engine.spawnNextCat()
    }
    engine.status = GameStatus.PLAYING
    _uiState.value = _uiState.value.copy(
      gameStatus = GameStatus.PLAYING,
      isEndlessMode = true,
      showVictoryDialog = false
    )
  }

  private fun startGameLoop() {
    gameLoopJob?.cancel()
    lastFrameTime = System.nanoTime()
    gameLoopJob = viewModelScope.launch {
      while (isActive) {
        val now = System.nanoTime()
        val deltaSeconds = ((now - lastFrameTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        lastFrameTime = now

        if (engine.status == GameStatus.PLAYING) {
          engine.update(deltaSeconds, inputVector)
          syncIncrementalGameplayStats()

          if (engine.status == GameStatus.VICTORY && !_uiState.value.showVictoryDialog) {
            handleVictory()
          } else if (engine.status == GameStatus.GAME_OVER && !_uiState.value.showGameOverDialog) {
            handleGameOver()
          }
        }

        val minGhostDist = if (engine.isPlayerGhost) {
          engine.snapshotAiRunnerCat()?.let { (it.position - engine.heroPosition).length() }
            ?: engine.snapshotRemotePlayers().minOfOrNull { (it.position - engine.heroPosition).length() }
            ?: 9999f
        } else {
          engine.snapshotGhosts().minOfOrNull { (it.position - engine.heroPosition).length() } ?: 9999f
        }
        val anyFleeing = if (engine.isPlayerGhost) false else engine.snapshotGhosts().all { it.isFleeing || it.freezeTimerSec > 0f }
        val isNear = !engine.isPlayerGhost && minGhostDist < 320f && !anyFleeing && !engine.isEffectivelyGodMode && !engine.isEffectivelyInvisible
        val (botRank, leaderBotName, leaderBotCats) = engine.getBotRaceSummary()

        _uiState.value = _uiState.value.copy(
          gameStatus = engine.status,
          isEndlessMode = engine.endlessMode,
          isBotsMode = engine.currentMode.isBotsMode,
          botCount = engine.bots.size,
          playerBotRank = botRank,
          leadingBotName = leaderBotName,
          leadingBotCats = leaderBotCats,
          winnerBotName = engine.winnerBotName,
          isPlayerGhost = engine.isPlayerGhost,
          aiRunnerLives = engine.aiRunnerCat?.lives ?: 0,
          aiRunnerCats = engine.aiRunnerCat?.catsCollected ?: 0,
          catsCollected = engine.catsCollected,
          goalCats = engine.goalCats,
          lives = engine.lives,
          maxLives = engine.maxLives,
          matchLevel = engine.matchLevel,
          currentScore = engine.calculateScore(),
          isDashing = engine.isDashing,
          dashCooldownRatio = (engine.dashCooldownTimer / engine.dashCooldown).coerceIn(0f, 1f),
          sonicCooldownRatio = (engine.sonicCooldownTimer / engine.sonicCooldown).coerceIn(0f, 1f),
          catnipActive = engine.catnipTimer > 0f,
          goldenFishActive = engine.goldenFishTimer > 0f,
          shieldActive = engine.shieldActive,
          magnetActive = engine.magnetTimer > 0f,
          giantMagnetActive = engine.giantMagnetTimer > 0f,
          freezeActive = engine.freezeTimer > 0f,
          invisibilityActive = engine.invisibilityTimer > 0f,
          doubleCatActive = engine.doubleCatTimer > 0f,
          slowMoActive = engine.slowMotionTimer > 0f,
          turboOverdriveActive = engine.turboOverdriveTimer > 0f,
          sonicOverdriveActive = engine.sonicOverdriveTimer > 0f,
          starInvincibleActive = engine.starInvincibleTimer > 0f,
          isInvincible = engine.isInvincible,
          isEffectiveGodMode = engine.isEffectivelyGodMode,
          comboCount = engine.currentCombo,
          gameTimeSeconds = engine.gameTimeSeconds,
          timeLimitRemaining = engine.timeLimitRemaining,
          ghostDistance = minGhostDist,
          activeGhostsCount = engine.ghosts.size,
          isGhostNear = isNear,
          isGhostFleeing = anyFleeing
        )

        delay(16)
      }
    }
  }

  private fun syncIncrementalGameplayStats() {
    var state = _uiState.value
    val (blocked, reason) = computeRewardsBlocked(state, engine.activeCustomMap != null)

    val safeItemSize = engine.collectedItemIdsThisRun.size
    val safeStartIdx = lastSyncedItemIndex.coerceIn(0, safeItemSize)

    if (blocked) {
      // Advance run counters without persisting or unlocking any rewards/stats
      lastSyncedGhostsScared = engine.ghostsScaredThisRun
      lastSyncedDashes = engine.dashesUsedThisRun
      lastSyncedSonics = engine.sonicsUsedThisRun
      lastSyncedItemIndex = safeItemSize
      _uiState.value = state.copy(isRewardsBlocked = true, rewardsBlockedReason = reason)
      return
    }

    var dirtyPrefs = false
    val editor = prefs.edit()

    if (engine.ghostsScaredThisRun > lastSyncedGhostsScared) {
      val diff = engine.ghostsScaredThisRun - lastSyncedGhostsScared
      lastSyncedGhostsScared = engine.ghostsScaredThisRun
      val updated = state.totalGhostsScared + diff
      state = state.copy(totalGhostsScared = updated)
      editor.putInt("total_ghosts_scared", updated)
      dirtyPrefs = true
    }

    if (engine.dashesUsedThisRun > lastSyncedDashes) {
      val diff = engine.dashesUsedThisRun - lastSyncedDashes
      lastSyncedDashes = engine.dashesUsedThisRun
      val updated = state.totalDashesUsed + diff
      state = state.copy(totalDashesUsed = updated)
      editor.putInt("total_dashes_used", updated)
      dirtyPrefs = true
    }

    if (engine.sonicsUsedThisRun > lastSyncedSonics) {
      val diff = engine.sonicsUsedThisRun - lastSyncedSonics
      lastSyncedSonics = engine.sonicsUsedThisRun
      val updated = state.totalSonicsUsed + diff
      state = state.copy(totalSonicsUsed = updated)
      editor.putInt("total_sonics_used", updated)
      dirtyPrefs = true
    }

    if (safeItemSize > safeStartIdx) {
      val newIds = engine.collectedItemIdsThisRun.subList(safeStartIdx, safeItemSize).toList()
      lastSyncedItemIndex = safeItemSize
      val officialNewIds = newIds.filter { id -> ALL_40_ITEMS.any { it.id == id } }
      val updatedSet = state.discoveredItems + officialNewIds
      val updatedTotal = state.totalItemsUsed + newIds.size
      state = state.copy(discoveredItems = updatedSet, totalItemsUsed = updatedTotal)
      editor.putStringSet("discovered_items", updatedSet)
      editor.putInt("total_items_used", updatedTotal)
      dirtyPrefs = true
    }

    if (dirtyPrefs) {
      editor.apply()
    }

    _uiState.value = syncDerivedProgress(state, showBanner = true)
  }

  private fun applyPlayerXp(state: GameUiState, gainedXp: Int): Triple<Int, Int, Int> {
    var level = state.playerLevel.coerceAtLeast(1)
    var xp = state.playerXp.coerceAtLeast(0) + gainedXp.coerceAtLeast(0)
    var needed = xpRequiredForLevel(level)
    while (xp >= needed) {
      xp -= needed
      level++
      needed = xpRequiredForLevel(level)
    }
    return Triple(level, xp, needed)
  }

  private fun handleVictory() {
    syncIncrementalGameplayStats()
    val prevState = _uiState.value
    val finalScore = engine.calculateScore(isVictory = true)
    val (blocked, reason) = computeRewardsBlocked(prevState, engine.activeCustomMap != null)

    if (blocked) {
      _uiState.value = prevState.copy(
        currentScore = finalScore,
        lastXpEarned = 0,
        isRewardsBlocked = true,
        rewardsBlockedReason = reason,
        lastVictoryUnlocks = listOf(
          "🚫 Modo Deus ou Modificações ativos ($reason): sem conquistas, XP, nível ou prêmios nesta rodada."
        ),
        showVictoryDialog = true
      )
      return
    }

    val oldUnlockedModeIds = ALL_40_MODES.filter {
      isModeEffectivelyUnlocked(it, prevState)
    }.map { it.id }.toSet()

    val newCount = prevState.championCount + 1
    val curTime = engine.gameTimeSeconds
    val oldBest = prevState.bestTimeSec
    val newBest = if (engine.currentMode.id == "classic_champion" && (oldBest <= 0.01f || curTime < oldBest)) {
      curTime
    } else {
      oldBest
    }
    val newHighScore = max(prevState.bestHighScore, finalScore)
    val catsCredited = if (engine.isPlayerGhost) 0 else engine.catsCollected
    val newTotal = prevState.totalCatsSaved + catsCredited

    val xpGain = 60 + engine.catsCollected * 10 + when (prevState.difficulty) {
      Difficulty.ELITE -> 50
      Difficulty.DESAFIO -> 40
      else -> 0
    }
    val pointsGain = max(120, 120 + finalScore / 6)
    val newPoints = prevState.spendablePoints + pointsGain
    val (newLevel, newXp, newNextXp) = applyPlayerXp(prevState, xpGain)

    val endlessNow = newCount >= 1 || prevState.purchasedModeIds.contains("endless_infinite")
    val godNow = newCount >= 10

    prefs.edit()
      .putInt("champion_count", newCount)
      .putFloat("best_time_sec", newBest)
      .putInt("best_high_score", newHighScore)
      .putInt("spendable_points", newPoints)
      .putInt("player_level", newLevel)
      .putInt("player_xp", newXp)
      .putInt("total_cats_saved", newTotal)
      .apply()

    var updatedState = prevState.copy(
      championCount = newCount,
      bestTimeSec = newBest,
      bestHighScore = newHighScore,
      spendablePoints = newPoints,
      currentScore = finalScore,
      playerLevel = newLevel,
      playerXp = newXp,
      xpToNextLevel = newNextXp,
      lastXpEarned = xpGain,
      totalCatsSaved = newTotal,
      isEndlessUnlocked = endlessNow,
      isGodModeUnlocked = godNow
    )

    val victorySingleEvents = mutableListOf<String>()
    if (curTime <= 45f) victorySingleEvents.add("speedrunner")
    if (engine.damageTakenThisRun == 0 && !engine.isEffectivelyGodMode) victorySingleEvents.add("untouchable")
    if (engine.ghosts.size >= 3) victorySingleEvents.add("triple_ghost_winner")

    val achSet = updatedState.unlockedAchievements.toMutableSet()
    var lastSingleAch: Achievement? = null
    for (id in victorySingleEvents) {
      if (!achSet.contains(id)) {
        achSet.add(id)
        lastSingleAch = ALL_40_ACHIEVEMENTS.find { it.id == id }
      }
    }
    if (achSet.size != updatedState.unlockedAchievements.size) {
      prefs.edit().putStringSet("unlocked_achievements", achSet).apply()
      updatedState = updatedState.copy(unlockedAchievements = achSet)
      lastSingleAch?.let {
        updatedState = updatedState.copy(newlyUnlockedAchievement = it)
        triggerAchievementBanner(it)
      }
    }

    updatedState = syncDerivedProgress(updatedState, showBanner = true)

    val newlyUnlocked = mutableListOf<String>()
    if (newLevel > prevState.playerLevel) {
      newlyUnlocked.add("⭐ Subiu para o Nível $newLevel de Carreira!")
    }
    if (finalScore > prevState.bestHighScore) {
      newlyUnlocked.add("🏅 Novo Recorde no Placar: $finalScore pts!")
    }
    if (!prevState.isEndlessUnlocked && endlessNow) {
      newlyUnlocked.add("🚀 Modo Infinito desbloqueado no Menu!")
    }
    if (!prevState.unlockedAchievements.contains("amor_felino") && updatedState.unlockedAchievements.contains("amor_felino")) {
      newlyUnlocked.add("💖 Conquista 'Amor Felino' desbloqueada (5 vitórias)!")
    }
    if (!prevState.isGodModeUnlocked && godNow) {
      newlyUnlocked.add("👑 Modo Deus desbloqueado nas Configurações (10 vitórias)!")
    }

    val newlyUnlockedModes = ALL_40_MODES.filter {
      !oldUnlockedModeIds.contains(it.id) && isModeEffectivelyUnlocked(it, updatedState)
    }
    for (m in newlyUnlockedModes.take(3)) {
      newlyUnlocked.add("${m.emoji} Novo Modo Liberado: ${m.name}!")
    }
    if (newlyUnlockedModes.size > 3) {
      newlyUnlocked.add("✨ +${newlyUnlockedModes.size - 3} outros modos desbloqueados!")
    }
    newlyUnlocked.add("🪙 +$pointsGain pontos ganhos para gastar na Loja!")

    _uiState.value = updatedState.copy(
      lastVictoryUnlocks = newlyUnlocked,
      showVictoryDialog = true
    )
  }

  private fun handleGameOver() {
    syncIncrementalGameplayStats()
    val prevState = _uiState.value
    val finalScore = engine.calculateScore(isVictory = false)
    val (blocked, reason) = computeRewardsBlocked(prevState, engine.activeCustomMap != null)

    if (blocked) {
      _uiState.value = prevState.copy(
        currentScore = finalScore,
        lastXpEarned = 0,
        isRewardsBlocked = true,
        rewardsBlockedReason = reason,
        showGameOverDialog = true
      )
      return
    }

    val catsCredited = if (engine.isPlayerGhost) 0 else engine.catsCollected
    val newTotal = prevState.totalCatsSaved + catsCredited
    val newBestEndless = if (engine.endlessMode && !engine.isPlayerGhost && engine.catsCollected > prevState.bestEndlessCats) {
      engine.catsCollected
    } else {
      prevState.bestEndlessCats
    }
    val newHighScore = max(prevState.bestHighScore, finalScore)

    val xpGain = engine.catsCollected * 6
    val pointsGain = max(25, finalScore / 6)
    val newPoints = prevState.spendablePoints + pointsGain
    val (newLevel, newXp, newNextXp) = applyPlayerXp(prevState, xpGain)

    prefs.edit()
      .putInt("total_cats_saved", newTotal)
      .putInt("best_endless_cats", newBestEndless)
      .putInt("best_high_score", newHighScore)
      .putInt("spendable_points", newPoints)
      .putInt("player_level", newLevel)
      .putInt("player_xp", newXp)
      .apply()

    val nextState = prevState.copy(
      totalCatsSaved = newTotal,
      bestEndlessCats = newBestEndless,
      bestHighScore = newHighScore,
      spendablePoints = newPoints,
      currentScore = finalScore,
      playerLevel = newLevel,
      playerXp = newXp,
      xpToNextLevel = newNextXp,
      lastXpEarned = xpGain,
      showGameOverDialog = true
    )
    _uiState.value = syncDerivedProgress(nextState, showBanner = true)
  }

  override fun onCleared() {
    super.onCleared()
    gameLoopJob?.cancel()
    bannerDismissJob?.cancel()
    aiJob?.cancel()
    feedbackDismissJob?.cancel()
  }
}
