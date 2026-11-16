package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.audio.GameAudio
import com.example.model.*
import kotlin.math.*
import kotlin.random.Random

data class Obstacle(
  val x: Float,
  val y: Float,
  val radius: Float,
  val type: String, // "FOUNTAIN", "TREE", "FLOWER_PATCH", "YARN"
  val label: String
)

data class RemotePartnerState(
  val userId: String = "",
  val username: String,
  val position: Vector2D,
  val score: Int,
  val role: String = "cat",
  val skinId: String = "classic_orange"
)

class GameEngine(
  val worldWidth: Float = 1600f,
  val worldHeight: Float = 2400f,
  private val audio: GameAudio? = null
) {
  private val stateLock = Any()

  var heroPosition = Vector2D(worldWidth / 2f, worldHeight / 2f)
  var heroVelocity = Vector2D(0f, 0f)
  var heroAngle = 0f
  var heroSpeed = 340f
  var lives = 3
  var maxLives = 3

  var isInvincible = false
  var invincibilityTimer = 0f

  var isDashing = false
  var dashTimer = 0f
  var dashCooldownTimer = 0f
  val dashCooldown = 3.5f

  var sonicCooldownTimer = 0f
  val sonicCooldown = 7.5f

  // Item effect timers
  var catnipTimer = 0f
  var activeSpeedMult = 1.45f
  var goldenFishTimer = 0f
  var magnetTimer = 0f
  var giantMagnetTimer = 0f
  var freezeTimer = 0f
  var invisibilityTimer = 0f
  var doubleCatTimer = 0f
  var slowMotionTimer = 0f
  var turboOverdriveTimer = 0f
  var sonicOverdriveTimer = 0f
  var starInvincibleTimer = 0f
  var shieldActive = false

  // Combo & Match Level System
  var currentCombo = 0
  var maxComboThisRun = 0
  var comboTimer = 0f
  var matchLevel = 1

  // User Settings & Active Mode
  var godMode = false
  var ghostCount = 1
  var heroSpeedMultiplier = 1.0f
  var powerUpFrequency = PowerUpFrequency.NORMAL
  var arenaTheme = ArenaTheme.PARQUE_NOTURNO
  var difficulty = Difficulty.NORMAL
  var customGhostSpeed = 250 // 1..1000 used when difficulty == Difficulty.CUSTOM

  var customItemsPool: List<GameItemDef> = emptyList()
  var spawnCustomItemsEnabled: Boolean = true
  var activeCustomMap: CustomMapConfig? = null
  var remotePartner: RemotePartnerState? = null
  var remotePlayers: List<RemotePartnerState> = emptyList()

  // Elite / Multiplayer Ghost Role State
  var isMultiplayerActive = false
  var multiplayerRole = "cat" // "cat" or "ghost"
  var multiplayerCaptureGoal = 3
  private var ghostCaptureCooldownTimer = 0f
  var aiRunnerCat: AiRunnerCat? = null

  var currentMode: GameModeDef = ALL_40_MODES.first()
  var timeLimitRemaining = 0f

  var status = GameStatus.READY
  var catsCollected = 0
  var goalCats = 10
  var endlessMode = false

  var gameTimeSeconds = 0f
  var damageTakenThisRun = 0
  var powerUpsCollectedThisRun = 0
  var ghostsScaredThisRun = 0
  var dashesUsedThisRun = 0
  var sonicsUsedThisRun = 0
  val collectedItemIdsThisRun = mutableListOf<String>()

  private val heroTrail = mutableListOf<Vector2D>()
  val followers = mutableListOf<FollowerCat>()
  val bots = mutableListOf<BotCatState>()
  var winnerBotName: String? = null

  val ghosts = mutableListOf(
    Ghost(
      id = 0,
      position = Vector2D(150f, 150f),
      velocity = Vector2D(0f, 0f)
    )
  )

  val ghost: Ghost
    get() = ghosts.first()

  val activeCollectibles = mutableListOf<CollectibleCat>()

  val activeCollectible: CollectibleCat?
    get() = activeCollectibles.firstOrNull()

  val activePowerUps = mutableListOf<PowerUpItem>()
  val activePowerUp: PowerUpItem?
    get() = activePowerUps.firstOrNull()

  val particles = mutableListOf<Particle>()
  val floatingTexts = mutableListOf<FloatingText>()
  val shockwaves = mutableListOf<ShockwaveEffect>()

  private var endlessSpawnTimer = 0f
  private var itemRainTimer = 0f
  private var nextCatSerial = 1

  private val defaultObstacles = listOf(
    Obstacle(worldWidth * 0.5f, worldHeight * 0.5f, 75f, "FOUNTAIN", "Fonte dos Peixinhos"),
    Obstacle(worldWidth * 0.25f, worldHeight * 0.3f, 55f, "TREE", "Arranhador Alto"),
    Obstacle(worldWidth * 0.75f, worldHeight * 0.3f, 55f, "TREE", "Arranhador Flor"),
    Obstacle(worldWidth * 0.25f, worldHeight * 0.7f, 50f, "FLOWER_PATCH", "Jardim de Catnip"),
    Obstacle(worldWidth * 0.75f, worldHeight * 0.7f, 50f, "FLOWER_PATCH", "Canteiro dos Sonhos"),
    Obstacle(worldWidth * 0.5f, worldHeight * 0.2f, 45f, "YARN", "Novelo de Lã Gigante"),
    Obstacle(worldWidth * 0.5f, worldHeight * 0.8f, 45f, "YARN", "Novelo Lilás")
  )

  var obstacles: List<Obstacle> = defaultObstacles

  init {
    resetGame(isEndless = false)
  }

  val isPlayerGhost: Boolean
    get() = if (currentMode.isBotsMode) {
      false
    } else if (isMultiplayerActive) {
      multiplayerRole == "ghost"
    } else {
      difficulty == Difficulty.ELITE
    }

  val isEffectivelyGodMode: Boolean
    get() = !isPlayerGhost && (godMode || currentMode.isGodTrialMode || starInvincibleTimer > 0f)

  val isEffectivelyInvisible: Boolean
    get() = invisibilityTimer > 0f

  val effectiveArenaTheme: ArenaTheme
    get() = currentMode.arenaThemeOverride ?: arenaTheme

  // Safe snapshots for Compose DrawScope so Canvas never hits ConcurrentModificationException
  fun snapshotObstacles(): List<Obstacle> = synchronized(stateLock) { obstacles.toList() }
  fun snapshotShockwaves(): List<ShockwaveEffect> = synchronized(stateLock) { shockwaves.map { it.copy() } }
  fun snapshotPowerUps(): List<PowerUpItem> = synchronized(stateLock) { activePowerUps.map { it.copy() } }
  fun snapshotCollectibles(): List<CollectibleCat> = synchronized(stateLock) { activeCollectibles.map { it.copy() } }
  fun snapshotFollowers(): List<FollowerCat> = synchronized(stateLock) { followers.take(28).map { it.copy() } }
  fun snapshotBots(): List<BotCatState> = synchronized(stateLock) { bots.map { it.copy() } }
  fun snapshotGhosts(): List<Ghost> = synchronized(stateLock) { ghosts.map { it.copy() } }
  fun snapshotParticles(): List<Particle> = synchronized(stateLock) { particles.map { it.copy() } }
  fun snapshotFloatingTexts(): List<FloatingText> = synchronized(stateLock) { floatingTexts.map { it.copy() } }
  fun snapshotAiRunnerCat(): AiRunnerCat? = synchronized(stateLock) { aiRunnerCat?.copy() }
  fun snapshotRemotePlayers(): List<RemotePartnerState> = synchronized(stateLock) {
    if (remotePlayers.isNotEmpty()) {
      remotePlayers.map { it.copy() }
    } else {
      remotePartner?.let { listOf(it.copy()) } ?: emptyList()
    }
  }

  fun getBotRaceSummary(): Triple<Int, String, Int> = synchronized(stateLock) {
    if (bots.isEmpty()) return Triple(1, "", 0)
    val higherOrEqualBots = bots.count { it.catsCollected > catsCollected }
    val leaderBot = bots.maxByOrNull { it.catsCollected }
    Triple(higherOrEqualBots + 1, leaderBot?.name.orEmpty(), leaderBot?.catsCollected ?: 0)
  }

  fun setMultiplayerParticipants(
    active: Boolean,
    myRole: String,
    players: List<RemotePartnerState>,
    captureGoal: Int = 3
  ) = synchronized(stateLock) {
    val wasGhost = isPlayerGhost
    isMultiplayerActive = active
    multiplayerRole = if (myRole == "ghost") "ghost" else "cat"
    multiplayerCaptureGoal = captureGoal.coerceIn(1, 20)
    remotePlayers = players
    remotePartner = players.firstOrNull()
    if (active && multiplayerRole == "ghost" && !wasGhost) {
      ghosts.clear()
      goalCats = multiplayerCaptureGoal
    }
  }

  fun resetGame(isEndless: Boolean = false) {
    val fallbackMode = if (isEndless) ALL_40_MODES[1] else ALL_40_MODES[0]
    resetGameForMode(fallbackMode, overrideGhostCount = ghostCount)
  }

  fun resetGameForCustomMap(mapConfig: CustomMapConfig) {
    val customModeDef = GameModeDef(
      id = "custom_map",
      number = 0,
      name = mapConfig.name.ifBlank { "Meu Mapa" },
      emoji = mapConfig.palette.emoji,
      description = "Mapa criado por você!",
      objectiveText = if (mapConfig.isEndless) "Resgate infinitos gatos no seu mapa" else "Resgate ${mapConfig.goalCats} gatos no seu mapa",
      unlockDescription = "Mapa Personalizado",
      unlockRule = ModeUnlockRule.ALWAYS_UNLOCKED,
      unlockTarget = 0,
      goalCats = if (mapConfig.isEndless) -1 else mapConfig.goalCats,
      ghostCount = mapConfig.ghostCount,
      startingCatsOnMap = if (mapConfig.isEndless) 12 else 3,
      isEndless = mapConfig.isEndless,
      isDarknessMode = mapConfig.isDarknessMode
    )
    resetGameForMode(customModeDef, overrideGhostCount = mapConfig.ghostCount, customMap = mapConfig)
  }

  fun resetGameForMode(
    mode: GameModeDef,
    overrideGhostCount: Int? = null,
    customMap: CustomMapConfig? = null
  ) = synchronized(stateLock) {
    currentMode = mode
    activeCustomMap = customMap
    obstacles = if (customMap != null) {
      customMap.obstacles.map { spec ->
        val radius = when (spec.type) {
          "FOUNTAIN" -> 75f
          "TREE" -> 55f
          "FLOWER_PATCH" -> 50f
          else -> 45f
        }
        Obstacle(
          x = worldWidth * spec.xRatio.coerceIn(0.08f, 0.92f),
          y = worldHeight * spec.yRatio.coerceIn(0.08f, 0.92f),
          radius = radius,
          type = spec.type,
          label = spec.type
        )
      }
    } else {
      defaultObstacles
    }
    val ghostPlayerMode = isPlayerGhost
    endlessMode = !ghostPlayerMode && mode.isEndless
    goalCats = if (ghostPlayerMode) {
      if (isMultiplayerActive) multiplayerCaptureGoal else 3
    } else if (mode.goalCats > 0) {
      mode.goalCats
    } else {
      999999
    }
    timeLimitRemaining = mode.timeLimitSec

    heroPosition = if (ghostPlayerMode) {
      Vector2D(worldWidth * 0.2f, worldHeight * 0.2f)
    } else {
      Vector2D(worldWidth / 2f, worldHeight * 0.65f)
    }
    heroVelocity = Vector2D(0f, 0f)
    heroAngle = if (ghostPlayerMode) 45f else -90f
    maxLives = if (ghostPlayerMode) 3 else mode.heroLives
    lives = maxLives
    ghostCaptureCooldownTimer = 0f

    isInvincible = false
    invincibilityTimer = 0f
    isDashing = false
    dashTimer = 0f
    dashCooldownTimer = 0f
    sonicCooldownTimer = 0f

    catnipTimer = 0f
    activeSpeedMult = 1.45f
    goldenFishTimer = 0f
    magnetTimer = 0f
    giantMagnetTimer = 0f
    freezeTimer = 0f
    invisibilityTimer = 0f
    doubleCatTimer = 0f
    slowMotionTimer = 0f
    turboOverdriveTimer = 0f
    sonicOverdriveTimer = 0f
    starInvincibleTimer = 0f
    shieldActive = false

    currentCombo = 0
    maxComboThisRun = 0
    comboTimer = 0f
    matchLevel = 1
    catsCollected = 0
    gameTimeSeconds = 0f
    damageTakenThisRun = 0
    powerUpsCollectedThisRun = 0
    ghostsScaredThisRun = 0
    dashesUsedThisRun = 0
    sonicsUsedThisRun = 0
    collectedItemIdsThisRun.clear()
    endlessSpawnTimer = 0f
    itemRainTimer = 0f
    nextCatSerial = 1

    heroTrail.clear()
    for (i in 0 until 220) {
      heroTrail.add(heroPosition)
    }
    followers.clear()
    bots.clear()
    winnerBotName = null

    ghosts.clear()
    aiRunnerCat = null

    if (ghostPlayerMode) {
      if (!isMultiplayerActive) {
        aiRunnerCat = AiRunnerCat(
          position = Vector2D(worldWidth * 0.52f, worldHeight * 0.68f),
          lives = 3,
          maxLives = 3,
          catsCollected = 0,
          goalCats = 10
        )
      }
    } else {
      val spawnCorners = listOf(
        Vector2D(worldWidth * 0.15f, worldHeight * 0.15f),
        Vector2D(worldWidth * 0.85f, worldHeight * 0.15f),
        Vector2D(worldWidth * 0.15f, worldHeight * 0.85f)
      )
      val effectiveGhosts = if (mode.id == "classic_champion" || mode.id == "endless_infinite") {
        (overrideGhostCount ?: mode.ghostCount).coerceIn(1, 3)
      } else {
        mode.ghostCount.coerceIn(1, 3)
      }
      for (i in 0 until effectiveGhosts) {
        ghosts.add(
          Ghost(
            id = i,
            position = spawnCorners[i % spawnCorners.size],
            velocity = Vector2D(0f, 0f)
          )
        )
      }

      if (mode.isBotsMode && mode.botCount > 0) {
        val botNames = listOf(
          "Bot Alfa", "Bot Turbo", "Bot Ninja", "Bot Pixel", "Bot Miau",
          "Bot Raio", "Bot Estrela", "Bot Cometa", "Bot Sombra", "Bot Lua",
          "Bot Cristal", "Bot Foguete", "Bot Tigre", "Bot Safira", "Bot Rubi",
          "Bot Vento", "Bot Flash", "Bot Cyber", "Bot Ouro", "Bot Lenda"
        )
        val botColors = listOf(
          Color(0xFF38BDF8), Color(0xFFFB7185), Color(0xFF34D399), Color(0xFFFBBF24),
          Color(0xFFA78BFA), Color(0xFF2DD4BF), Color(0xFFF472B6), Color(0xFF60A5FA),
          Color(0xFFFB923C), Color(0xFFC084FC)
        )
        val botEmojis = listOf("🤖", "⚡", "🥷", "👾", "🐱", "🚀", "🌟", "☄️", "🕶️", "🌙")
        val totalBots = mode.botCount.coerceIn(1, 40)
        for (i in 0 until totalBots) {
          val angle = (i.toFloat() / totalBots.toFloat()) * 2f * PI.toFloat()
          val ringRadius = 320f + (i % 3) * 180f
          val rawPos = Vector2D(
            worldWidth * 0.5f + cos(angle) * ringRadius,
            worldHeight * 0.5f + sin(angle) * ringRadius
          )
          bots.add(
            BotCatState(
              id = i + 1,
              name = botNames[i % botNames.size],
              furColor = botColors[i % botColors.size],
              accentEmoji = botEmojis[i % botEmojis.size],
              position = clampAndResolveObstacles(rawPos, 24f),
              catsCollected = 0
            )
          )
        }
      }
    }

    particles.clear()
    floatingTexts.clear()
    shockwaves.clear()
    activeCollectibles.clear()
    activePowerUps.clear()

    val catsToSpawn = when {
      ghostPlayerMode && isMultiplayerActive -> 0
      ghostPlayerMode -> 4
      mode.isBotsMode -> max(18, mode.startingCatsOnMap)
      mode.isEndless -> max(12, mode.startingCatsOnMap)
      else -> max(1, mode.startingCatsOnMap)
    }
    repeat(catsToSpawn) {
      spawnNextCat()
    }

    val startingItems = if (mode.isEndless || mode.isBotsMode || mode.powerUpSpawnMult >= 2.0f) 3 else 1
    repeat(startingItems) {
      spawnPowerUp()
    }

    status = GameStatus.READY
  }

  fun revivePlayer(): Boolean = synchronized(stateLock) {
    if (status != GameStatus.GAME_OVER || winnerBotName != null) return false
    lives = max(3, maxLives)
    isInvincible = true
    invincibilityTimer = 3.5f
    if (currentMode.timeLimitSec > 0f && timeLimitRemaining <= 0f) {
      timeLimitRemaining = 15f
    }
    for (g in ghosts) {
      val diff = g.position - heroPosition
      val dist = diff.length()
      if (dist < 260f) {
        val pushDir = if (dist > 0.1f) diff.normalized() else Vector2D(1f, 0f)
        g.position = Vector2D(
          (heroPosition.x + pushDir.x * 280f).coerceIn(50f, worldWidth - 50f),
          (heroPosition.y + pushDir.y * 280f).coerceIn(50f, worldHeight - 50f)
        )
      }
      g.stunTimerSec = max(g.stunTimerSec, 1.8f)
    }
    shockwaves.add(
      ShockwaveEffect(
        origin = heroPosition,
        radius = 24f,
        maxRadius = 380f,
        color = Color(0xFFF472B6),
        alpha = 1f
      )
    )
    addFloatingTextSafe(FloatingText("💖 REVIVEU! (+3.5s invencível)", heroPosition + Vector2D(0f, -55f), Color(0xFFF472B6)))
    audio?.playPowerUpSound()
    status = GameStatus.PLAYING
    return true
  }

  fun startPlaying() = synchronized(stateLock) {
    if (status == GameStatus.READY || status == GameStatus.PAUSED) {
      status = GameStatus.PLAYING
    }
  }

  fun togglePause() = synchronized(stateLock) {
    if (status == GameStatus.PLAYING) {
      status = GameStatus.PAUSED
    } else if (status == GameStatus.PAUSED) {
      status = GameStatus.PLAYING
    }
  }

  fun triggerDash(): Boolean = synchronized(stateLock) {
    if (status != GameStatus.PLAYING) return false
    val freeDash = currentMode.infiniteTurbo || turboOverdriveTimer > 0f
    if ((dashCooldownTimer <= 0f || freeDash) && !isDashing) {
      isDashing = true
      dashTimer = 0.35f
      dashCooldownTimer = if (freeDash) 0.4f else dashCooldown
      dashesUsedThisRun++
      audio?.playDashSound()
      audio?.vibrateDash()

      for (i in 0 until 12) {
        val angle = Random.nextFloat() * 2f * PI.toFloat()
        val spd = Random.nextFloat() * 120f + 40f
        addParticleSafe(
          Particle(
            position = heroPosition,
            velocity = Vector2D(cos(angle) * spd, sin(angle) * spd),
            color = Color(0xFFFACC15),
            size = Random.nextFloat() * 8f + 4f,
            life = 0.5f
          )
        )
      }
      return true
    }
    return false
  }

  fun triggerSonicWave(): Boolean = synchronized(stateLock) {
    if (status != GameStatus.PLAYING) return false
    val freeSonic = currentMode.infiniteSonic || sonicOverdriveTimer > 0f
    if (sonicCooldownTimer <= 0f || freeSonic) {
      sonicCooldownTimer = if (freeSonic) 0.7f else sonicCooldown
      sonicsUsedThisRun++
      emitSonicShockwave(heroPosition, maxRadius = 380f, stunSec = 2.5f)
      audio?.playSonicMeowSound()
      audio?.vibrateDash()
      return true
    }
    return false
  }

  private fun emitSonicShockwave(origin: Vector2D, maxRadius: Float, stunSec: Float) {
    if (shockwaves.size < 12) {
      shockwaves.add(
        ShockwaveEffect(
          origin = origin,
          radius = 24f,
          maxRadius = maxRadius,
          color = Color(0xFFC084FC),
          alpha = 1f
        )
      )
    }
    var hitAny = false
    aiRunnerCat?.let { ai ->
      val dist = (ai.position - origin).length()
      if (dist < maxRadius) {
        ai.stunTimer = max(ai.stunTimer, 1.8f)
        hitAny = true
      }
    }
    for (g in ghosts) {
      val diff = g.position - origin
      val dist = diff.length()
      if (dist < maxRadius) {
        val push = if (dist > 0.1f) diff.normalized() else Vector2D(1f, 0f)
        g.position = Vector2D(
          (g.position.x + push.x * 260f).coerceIn(40f, worldWidth - 40f),
          (g.position.y + push.y * 260f).coerceIn(40f, worldHeight - 40f)
        )
        g.stunTimerSec = max(g.stunTimerSec, stunSec)
        hitAny = true
        ghostsScaredThisRun++
      }
    }
    if (hitAny) {
      val msg = if (isPlayerGhost) "👻 Grito Sônico atordoou o Runner!" else "🔔 Onda Felina afastou o fantasma!"
      addFloatingTextSafe(FloatingText(msg, origin + Vector2D(0f, -50f), Color(0xFFE879F9)))
    } else {
      val msg = if (isPlayerGhost) "👻 Buuu Sônico!" else "🔔 Miau Sônico!"
      addFloatingTextSafe(FloatingText(msg, origin + Vector2D(0f, -45f), Color(0xFFC084FC)))
    }
  }

  fun update(deltaTime: Float, inputDirection: Vector2D) = synchronized(stateLock) {
    if (status != GameStatus.PLAYING) return

    val safeDelta = deltaTime.coerceIn(0.001f, 0.05f)
    gameTimeSeconds += safeDelta

    if (currentMode.timeLimitSec > 0f) {
      timeLimitRemaining = max(0f, timeLimitRemaining - safeDelta)
      if (timeLimitRemaining <= 0f) {
        status = GameStatus.GAME_OVER
        addFloatingTextSafe(FloatingText("⏱️ Tempo esgotado!", heroPosition, Color(0xFFEF4444)))
        return
      }
    }

    if (dashTimer > 0f) {
      dashTimer -= safeDelta
      if (dashTimer <= 0f) isDashing = false
    }
    if (dashCooldownTimer > 0f) dashCooldownTimer = max(0f, dashCooldownTimer - safeDelta)
    if (sonicCooldownTimer > 0f) sonicCooldownTimer = max(0f, sonicCooldownTimer - safeDelta)
    if (invincibilityTimer > 0f) {
      invincibilityTimer -= safeDelta
      if (invincibilityTimer <= 0f) isInvincible = false
    }
    if (catnipTimer > 0f) catnipTimer = max(0f, catnipTimer - safeDelta)
    if (magnetTimer > 0f) magnetTimer = max(0f, magnetTimer - safeDelta)
    if (giantMagnetTimer > 0f) giantMagnetTimer = max(0f, giantMagnetTimer - safeDelta)
    if (freezeTimer > 0f) freezeTimer = max(0f, freezeTimer - safeDelta)
    if (invisibilityTimer > 0f) invisibilityTimer = max(0f, invisibilityTimer - safeDelta)
    if (doubleCatTimer > 0f) doubleCatTimer = max(0f, doubleCatTimer - safeDelta)
    if (slowMotionTimer > 0f) slowMotionTimer = max(0f, slowMotionTimer - safeDelta)
    if (turboOverdriveTimer > 0f) turboOverdriveTimer = max(0f, turboOverdriveTimer - safeDelta)
    if (sonicOverdriveTimer > 0f) sonicOverdriveTimer = max(0f, sonicOverdriveTimer - safeDelta)
    if (starInvincibleTimer > 0f) starInvincibleTimer = max(0f, starInvincibleTimer - safeDelta)

    if (ghostCaptureCooldownTimer > 0f) ghostCaptureCooldownTimer = max(0f, ghostCaptureCooldownTimer - safeDelta)
    if (comboTimer > 0f) {
      comboTimer -= safeDelta
      if (comboTimer <= 0f) currentCombo = 0
    }
    if (goldenFishTimer > 0f) {
      goldenFishTimer = max(0f, goldenFishTimer - safeDelta)
      ghosts.forEach { it.isFleeing = (goldenFishTimer > 0f) }
    } else {
      ghosts.forEach { it.isFleeing = false }
    }

    var currentSpeed = heroSpeed * heroSpeedMultiplier * currentMode.heroSpeedMult
    if (isPlayerGhost) currentSpeed *= 1.15f
    if (catnipTimer > 0f) currentSpeed *= activeSpeedMult
    if (isDashing) currentSpeed *= 1.9f

    if (inputDirection.length() > 0.05f) {
      val norm = inputDirection.normalized()
      heroVelocity = norm * currentSpeed
      heroAngle = (atan2(norm.y, norm.x) * 180f / PI.toFloat())
    } else {
      heroVelocity = heroVelocity * 0.85f
    }

    val newPos = heroPosition + heroVelocity * safeDelta
    heroPosition = clampAndResolveObstacles(newPos, 32f)

    heroTrail.add(0, heroPosition)
    if (heroTrail.size > 400) {
      heroTrail.removeAt(heroTrail.size - 1)
    }

    val visibleFollowers = followers.take(28)
    visibleFollowers.forEachIndexed { index, follower ->
      val trailIndex = min((index + 1) * 13, heroTrail.size - 1)
      val targetPos = heroTrail[trailIndex]
      val diff = targetPos - follower.position
      follower.position = follower.position + diff * (10f * safeDelta).coerceAtMost(1f)
      if (diff.length() > 2f) {
        follower.angle = atan2(diff.y, diff.x) * 180f / PI.toFloat()
      }
      follower.hopOffset = sin((gameTimeSeconds * 8f) + index * 0.7f) * 6f
    }

    if (isPlayerGhost) {
      updateGhostPlayerRole(safeDelta)
      val pickedPowerUps = mutableListOf<PowerUpItem>()
      val powerUpIter = activePowerUps.iterator()
      while (powerUpIter.hasNext()) {
        val item = powerUpIter.next()
        if ((item.position - heroPosition).length() < 56f) {
          powerUpIter.remove()
          pickedPowerUps.add(item)
        }
      }
      for (item in pickedPowerUps) {
        collectPowerUp(item)
      }
    } else {
      for (g in ghosts) {
        updateGhost(g, safeDelta)
      }

      if (endlessMode || currentMode.isBotsMode) {
        val targetCount = max(16, currentMode.startingCatsOnMap)
        endlessSpawnTimer += safeDelta
        if (activeCollectibles.size < targetCount || (endlessMode && endlessSpawnTimer >= 2.0f && activeCollectibles.size < targetCount + 6)) {
          spawnNextCat()
          endlessSpawnTimer = 0f
        }
      } else {
        val targetNormalCount = max(1, currentMode.startingCatsOnMap)
        val remainingNeeded = goalCats - catsCollected
        while (activeCollectibles.size < min(targetNormalCount, remainingNeeded)) {
          spawnNextCat()
        }
      }

      itemRainTimer += safeDelta
      val maxItemsOnMap = if (currentMode.powerUpSpawnMult >= 2.5f) 7 else 3
      val rainPeriod = (4.5f / currentMode.powerUpSpawnMult).coerceAtLeast(0.8f)
      if (itemRainTimer >= rainPeriod) {
        itemRainTimer = 0f
        if (activePowerUps.size < maxItemsOnMap && Random.nextFloat() < powerUpFrequency.spawnChance) {
          spawnPowerUp()
        }
      }

      val collectedList = mutableListOf<CollectibleCat>()
      for (cat in activeCollectibles) {
        cat.bobOffset = sin(gameTimeSeconds * 5f + cat.id) * 8f

        if ((endlessMode || currentMode.startingCatsOnMap >= 5) && cat.wanderDir.length() > 0.01f) {
          val nextCatPos = cat.position + cat.wanderDir * (38f * safeDelta)
          if (nextCatPos.x < 80f || nextCatPos.x > worldWidth - 80f) {
            cat.wanderDir = Vector2D(-cat.wanderDir.x, cat.wanderDir.y)
          }
          if (nextCatPos.y < 80f || nextCatPos.y > worldHeight - 80f) {
            cat.wanderDir = Vector2D(cat.wanderDir.x, -cat.wanderDir.y)
          }
          cat.position = clampAndResolveObstacles(nextCatPos, 24f)
        }

        val toHero = heroPosition - cat.position
        val dist = toHero.length()
        if (giantMagnetTimer > 0f && dist > 1f) {
          cat.position = cat.position + toHero.normalized() * (560f * safeDelta)
        } else if (magnetTimer > 0f && dist < 540f && dist > 1f) {
          cat.position = cat.position + toHero.normalized() * (460f * safeDelta)
        }

        if ((cat.position - heroPosition).length() < 55f) {
          collectedList.add(cat)
        }
      }

      for (cat in collectedList) {
        if (status == GameStatus.PLAYING) {
          collectCat(cat)
        }
      }

      val pickedPowerUps = mutableListOf<PowerUpItem>()
      val powerUpIter = activePowerUps.iterator()
      while (powerUpIter.hasNext()) {
        val item = powerUpIter.next()
        val dist = (item.position - heroPosition).length()
        if (dist < 56f) {
          powerUpIter.remove()
          pickedPowerUps.add(item)
        }
      }
      for (item in pickedPowerUps) {
        collectPowerUp(item)
      }

      for (g in ghosts) {
        val ghostDist = (g.position - heroPosition).length()
        if (ghostDist < 58f) {
          if (g.isFleeing || g.freezeTimerSec > 0f) {
            g.position = g.position + (g.position - heroPosition).normalized() * 280f
            addFloatingTextSafe(FloatingText("Fantasma espantado! 👻💨", g.position, Color(0xFF38BDF8)))
            audio?.playPowerUpSound()
            ghostsScaredThisRun++
          } else if (isEffectivelyInvisible) {
            // Ghosts pass harmlessly when invisible
          } else if (isEffectivelyGodMode) {
            val pushDir = (g.position - heroPosition).normalized()
            g.position = Vector2D(
              (g.position.x + pushDir.x * 220f).coerceIn(40f, worldWidth - 40f),
              (g.position.y + pushDir.y * 220f).coerceIn(40f, worldHeight - 40f)
            )
            g.stunTimerSec = 1.0f
            val msg = if (starInvincibleTimer > 0f) "🌟 INVENCÍVEL!" else "👑 MODO DEUS: IMORTAL!"
            addFloatingTextSafe(FloatingText(msg, heroPosition + Vector2D(0f, -45f), Color(0xFFFDE047)))
            for (i in 0 until 8) {
              val angle = Random.nextFloat() * 2f * PI.toFloat()
              addParticleSafe(
                Particle(
                  position = heroPosition,
                  velocity = Vector2D(cos(angle) * 160f, sin(angle) * 160f),
                  color = Color(0xFFFDE047),
                  size = 7f,
                  life = 0.5f
                )
              )
            }
          } else if (!isInvincible) {
            onHeroHitByGhost(g)
          }
        }
      }

      // Remote player who was voted as Ghost in Multiplayer Elite can catch local Cat player
      if (isMultiplayerActive && !isEffectivelyGodMode && !isEffectivelyInvisible && !isInvincible) {
        val remoteGhost = remotePlayers.firstOrNull { it.role == "ghost" && (it.position - heroPosition).length() < 58f }
        if (remoteGhost != null) {
          onHeroHitByGhost(Ghost(id = 99, position = remoteGhost.position))
        }
      }

      if (currentMode.isBotsMode && bots.isNotEmpty() && status == GameStatus.PLAYING) {
        updateBots(safeDelta)
      }
    }

    val swIter = shockwaves.iterator()
    while (swIter.hasNext()) {
      val sw = swIter.next()
      sw.radius += 850f * safeDelta
      sw.alpha = (1f - (sw.radius / max(1f, sw.maxRadius))).coerceIn(0f, 1f)
      if (sw.radius >= sw.maxRadius) swIter.remove()
    }

    val pIter = particles.iterator()
    while (pIter.hasNext()) {
      val p = pIter.next()
      p.position = p.position + p.velocity * safeDelta
      p.life -= safeDelta * 1.5f
      p.alpha = p.life.coerceIn(0f, 1f)
      if (p.life <= 0f) pIter.remove()
    }

    val tIter = floatingTexts.iterator()
    while (tIter.hasNext()) {
      val txt = tIter.next()
      txt.position = txt.position + Vector2D(0f, -65f * safeDelta)
      txt.lifetime -= safeDelta
      txt.alpha = (txt.lifetime / 1.2f).coerceIn(0f, 1f)
      if (txt.lifetime <= 0f) tIter.remove()
    }

    if (isDashing || catnipTimer > 0f || isEffectivelyGodMode) {
      if (Random.nextFloat() < (if (isEffectivelyGodMode && !isDashing) 0.35f else 0.9f)) {
        addParticleSafe(
          Particle(
            position = heroPosition + Vector2D(Random.nextFloat() * 24f - 12f, Random.nextFloat() * 24f - 12f),
            velocity = Vector2D(-heroVelocity.x * 0.2f, -heroVelocity.y * 0.2f),
            color = when {
              isEffectivelyGodMode -> Color(0xFFFDE047)
              isDashing -> Color(0xFFFBBF24)
              else -> Color(0xFF34D399)
            },
            size = Random.nextFloat() * 6f + 3f,
            life = 0.45f
          )
        )
      }
    }
  }

  private fun addParticleSafe(p: Particle) {
    if (particles.size >= 160) {
      particles.removeAt(0)
    }
    particles.add(p)
  }

  private fun addFloatingTextSafe(txt: FloatingText) {
    if (floatingTexts.size >= 20) {
      floatingTexts.removeAt(0)
    }
    floatingTexts.add(txt)
  }

  private fun updateBots(deltaTime: Float) {
    val botPickedCats = mutableSetOf<CollectibleCat>()
    val speedDiffFactor = when (difficulty) {
      Difficulty.CALMO -> 0.85f
      Difficulty.DESAFIO -> 1.15f
      else -> 1.0f
    }

    for (bot in bots) {
      if (bot.invincibleTimer > 0f) {
        bot.invincibleTimer = max(0f, bot.invincibleTimer - deltaTime)
      }

      val nearestCat = activeCollectibles
        .filter { !botPickedCats.contains(it) }
        .minByOrNull { (it.position - bot.position).length() }

      val nearestGhost = ghosts.minByOrNull { (it.position - bot.position).length() }
      val ghostDist = nearestGhost?.let { (it.position - bot.position).length() } ?: 9999f

      val moveDir = when {
        ghostDist < 190f && nearestGhost != null && !nearestGhost.isFleeing && nearestGhost.freezeTimerSec <= 0f -> {
          val flee = (bot.position - nearestGhost.position).normalized()
          val toCat = nearestCat?.let { (it.position - bot.position).normalized() } ?: Vector2D(0f, 0f)
          (flee * 0.72f + toCat * 0.28f).normalized()
        }
        nearestCat != null -> (nearestCat.position - bot.position).normalized()
        else -> {
          val orbit = gameTimeSeconds * 1.1f + bot.id * 0.7f
          Vector2D(cos(orbit), sin(orbit))
        }
      }

      val botBaseSpeed = (210f + (bot.id % 6) * 14f) * speedDiffFactor
      val targetVel = moveDir * botBaseSpeed
      bot.velocity = bot.velocity + (targetVel - bot.velocity) * (6f * deltaTime).coerceAtMost(1f)
      bot.position = clampAndResolveObstacles(bot.position + bot.velocity * deltaTime, 22f)
      if (bot.velocity.length() > 8f) {
        bot.angle = atan2(bot.velocity.y, bot.velocity.x) * 180f / PI.toFloat()
      }

      if (nearestCat != null && (nearestCat.position - bot.position).length() < 48f) {
        botPickedCats.add(nearestCat)
        bot.catsCollected++
        if (bot.catsCollected >= goalCats && status == GameStatus.PLAYING) {
          winnerBotName = bot.name
          status = GameStatus.GAME_OVER
          addFloatingTextSafe(
            FloatingText(
              text = "🤖 ${bot.name} atingiu $goalCats/$goalCats gatos!",
              position = heroPosition + Vector2D(0f, -60f),
              color = Color(0xFFEF4444)
            )
          )
          audio?.playGhostHitSound()
          break
        }
      }

      if (nearestGhost != null && ghostDist < 52f && bot.invincibleTimer <= 0f &&
        !nearestGhost.isFleeing && nearestGhost.freezeTimerSec <= 0f && nearestGhost.stunTimerSec <= 0f
      ) {
        bot.invincibleTimer = 2.0f
        val push = (bot.position - nearestGhost.position).normalized()
        bot.position = clampAndResolveObstacles(bot.position + push * 130f, 22f)
      }
    }

    if (botPickedCats.isNotEmpty()) {
      activeCollectibles.removeAll(botPickedCats)
      if (status == GameStatus.PLAYING) {
        val targetCount = max(16, currentMode.startingCatsOnMap)
        while (activeCollectibles.size < targetCount) {
          spawnNextCat()
        }
      }
    }
  }

  private fun updateGhostPlayerRole(deltaTime: Float) {
    if (isMultiplayerActive) {
      for (p in remotePlayers) {
        if (p.role == "cat" && (p.position - heroPosition).length() < 62f && ghostCaptureCooldownTimer <= 0f) {
          ghostCaptureCooldownTimer = 1.6f
          catsCollected++
          ghostsScaredThisRun++
          matchLevel = (catsCollected / 2) + 1
          val isWin = catsCollected >= goalCats
          audio?.playCatCollectSound(isWin)
          audio?.vibrateCollect()
          addFloatingTextSafe(
            FloatingText(
              text = "👻 Pegou @${p.username}! ($catsCollected/$goalCats)",
              position = heroPosition + Vector2D(0f, -45f),
              color = if (isWin) Color(0xFFFDE047) else Color(0xFFE879F9)
            )
          )
          for (i in 0 until 16) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val spd = Random.nextFloat() * 180f + 50f
            addParticleSafe(
              Particle(
                position = p.position,
                velocity = Vector2D(cos(angle) * spd, sin(angle) * spd),
                color = Color(0xFFE879F9),
                size = 7f,
                life = 0.65f
              )
            )
          }
          if (isWin) {
            status = GameStatus.VICTORY
            audio?.vibrateVictory()
          }
          break
        }
      }
      return
    }

    // Solo Elite Mode: player is the Ghost chasing an AI Runner Cat
    while (activeCollectibles.size < 4) {
      spawnNextCat()
    }
    val ai = aiRunnerCat ?: return
    if (ai.invincibleTimer > 0f) ai.invincibleTimer = max(0f, ai.invincibleTimer - deltaTime)
    if (ai.stunTimer > 0f) ai.stunTimer = max(0f, ai.stunTimer - deltaTime)

    val distToGhost = (heroPosition - ai.position).length()
    val nearestCat = activeCollectibles.minByOrNull { (it.position - ai.position).length() }
    val desiredDir = when {
      ai.stunTimer > 0f -> Vector2D(0f, 0f)
      distToGhost < 360f -> {
        val flee = (ai.position - heroPosition).normalized()
        val toCat = nearestCat?.let { (it.position - ai.position).normalized() } ?: Vector2D(0f, 0f)
        (flee * 0.78f + toCat * 0.22f).normalized()
      }
      nearestCat != null -> (nearestCat.position - ai.position).normalized()
      else -> Vector2D(cos(gameTimeSeconds), sin(gameTimeSeconds))
    }

    val runnerSpeed = if (ai.stunTimer > 0f) 0f else 310f + (ai.catsCollected * 6f)
    val targetVel = desiredDir * runnerSpeed
    ai.velocity = ai.velocity + (targetVel - ai.velocity) * (6f * deltaTime).coerceAtMost(1f)
    ai.position = clampAndResolveObstacles(ai.position + ai.velocity * deltaTime, 24f)
    if (ai.velocity.length() > 10f) {
      ai.angle = atan2(ai.velocity.y, ai.velocity.x) * 180f / PI.toFloat()
    }

    // Check if AI Runner Cat rescued a collectible cat
    val rescuedCat = activeCollectibles.firstOrNull { (it.position - ai.position).length() < 52f }
    if (rescuedCat != null) {
      activeCollectibles.remove(rescuedCat)
      ai.catsCollected++
      addFloatingTextSafe(
        FloatingText(
          text = "🐱 Runner resgatou ${ai.catsCollected}/${ai.goalCats}!",
          position = ai.position + Vector2D(0f, -35f),
          color = Color(0xFFFB923C)
        )
      )
      if (ai.catsCollected >= ai.goalCats) {
        status = GameStatus.GAME_OVER
        audio?.playGhostHitSound()
        return
      } else {
        spawnNextCat()
      }
    }

    // Check if Player Ghost caught AI Runner Cat
    if ((heroPosition - ai.position).length() < 58f && ai.invincibleTimer <= 0f) {
      ai.lives--
      ai.invincibleTimer = 2.0f
      catsCollected++
      ghostsScaredThisRun++
      matchLevel = (catsCollected / 2) + 1
      val pushDir = (ai.position - heroPosition).normalized()
      ai.position = clampAndResolveObstacles(ai.position + pushDir * 140f, 24f)

      val isWin = ai.lives <= 0
      audio?.playCatCollectSound(isWin)
      audio?.vibrateCollect()
      addFloatingTextSafe(
        FloatingText(
          text = if (isWin) "🏆 ELITE VENCIDO! Você pegou o Runner!" else "👻 Pegou o Runner! ($catsCollected/$goalCats)",
          position = ai.position + Vector2D(0f, -45f),
          color = if (isWin) Color(0xFFFDE047) else Color(0xFFE879F9)
        )
      )
      for (i in 0 until 18) {
        val angle = Random.nextFloat() * 2f * PI.toFloat()
        val spd = Random.nextFloat() * 200f + 60f
        addParticleSafe(
          Particle(
            position = ai.position,
            velocity = Vector2D(cos(angle) * spd, sin(angle) * spd),
            color = Color(0xFFE879F9),
            size = 7f,
            life = 0.7f
          )
        )
      }
      if (isWin) {
        status = GameStatus.VICTORY
        audio?.vibrateVictory()
      }
    }
  }

  private fun updateGhost(g: Ghost, deltaTime: Float) {
    g.floatOffset = sin(gameTimeSeconds * 3.5f + g.id * 1.3f) * 12f

    if (g.freezeTimerSec > 0f) {
      g.freezeTimerSec = max(0f, g.freezeTimerSec - deltaTime)
      g.velocity = Vector2D(0f, 0f)
      return
    }
    if (g.stunTimerSec > 0f) {
      g.stunTimerSec = max(0f, g.stunTimerSec - deltaTime)
      g.velocity = g.velocity * 0.85f
      return
    }

    val scaledCats = min(catsCollected, 25)
    val levelBoost = (matchLevel - 1) * 10f
    var baseGhostSpeed = if (difficulty == Difficulty.CUSTOM) {
      val clamped = customGhostSpeed.coerceIn(1, 1000)
      (10f + clamped * 0.72f) * currentMode.ghostSpeedMult * (1f + g.id * 0.05f)
    } else {
      (175f + (scaledCats * 8.0f) + levelBoost) * difficulty.ghostSpeedMult * currentMode.ghostSpeedMult * (1f + g.id * 0.06f)
    }
    if (slowMotionTimer > 0f) baseGhostSpeed *= 0.35f

    val actualDist = (heroPosition - g.position).length()

    if (actualDist > 850f && Random.nextFloat() < 0.0025f) {
      g.position = Vector2D(
        (heroPosition.x + if (Random.nextBoolean()) 400f else -400f).coerceIn(60f, worldWidth - 60f),
        (heroPosition.y + if (Random.nextBoolean()) 400f else -400f).coerceIn(60f, worldHeight - 60f)
      )
      addFloatingTextSafe(FloatingText("Buuu! O fantasma surgiu! 👻", g.position, Color(0xFFA855F7)))
      audio?.playGhostAlertSound()
    }

    val targetPos = if (difficulty == Difficulty.DESAFIO && !isEffectivelyInvisible && !g.isFleeing) {
      val predicted = heroPosition + heroVelocity * 0.42f
      Vector2D(
        predicted.x.coerceIn(60f, worldWidth - 60f),
        predicted.y.coerceIn(60f, worldHeight - 60f)
      )
    } else {
      heroPosition
    }
    val toHero = targetPos - g.position

    val dir: Vector2D = when {
      g.isFleeing -> (g.position - heroPosition).normalized()
      isEffectivelyInvisible -> {
        val angle = gameTimeSeconds * 1.2f + g.id * 2f
        Vector2D(cos(angle), sin(angle))
      }
      else -> toHero.normalized()
    }

    val targetVel = dir * (if (g.isFleeing) baseGhostSpeed * 0.78f else baseGhostSpeed)
    g.velocity = g.velocity + (targetVel - g.velocity) * (4f * deltaTime)
    val nextGhostPos = g.position + g.velocity * deltaTime

    g.position = Vector2D(
      nextGhostPos.x.coerceIn(40f, worldWidth - 40f),
      nextGhostPos.y.coerceIn(40f, worldHeight - 40f)
    )

    if (Random.nextFloat() < 0.20f) {
      addParticleSafe(
        Particle(
          position = g.position + Vector2D(Random.nextFloat() * 20f - 10f, Random.nextFloat() * 20f - 10f),
          velocity = Vector2D(Random.nextFloat() * 20f - 10f, Random.nextFloat() * 20f - 10f),
          color = if (g.isFleeing) Color(0xFF38BDF8) else Color(0xFFC084FC),
          size = Random.nextFloat() * 8f + 4f,
          life = 0.65f
        )
      )
    }
  }

  private fun collectCat(cat: CollectibleCat) {
    activeCollectibles.remove(cat)
    val value = if (doubleCatTimer > 0f) 2 else 1
    catsCollected += value
    currentCombo++
    if (currentCombo > maxComboThisRun) maxComboThisRun = currentCombo
    comboTimer = 4.0f

    val isChampion = !endlessMode && catsCollected >= goalCats

    // Match Level-Up every 5 cats collected!
    val computedLevel = (catsCollected / 5) + 1
    if (computedLevel > matchLevel) {
      matchLevel = computedLevel
      addFloatingTextSafe(
        FloatingText(
          text = "⬆️ SUBIU PARA O NÍVEL $matchLevel! ⭐",
          position = heroPosition + Vector2D(0f, -80f),
          color = Color(0xFF38BDF8)
        )
      )
      if (!isChampion) {
        audio?.playAchievementSound()
        if (activePowerUps.size < 6) {
          spawnPowerUp()
        }
      }
    }

    followers.add(
      FollowerCat(
        breed = cat.breed,
        position = cat.position
      )
    )

    audio?.playCatCollectSound(isChampion)
    audio?.vibrateCollect()

    val comboSuffix = if (currentCombo >= 2) " (Combo x$currentCombo! 🔥)" else ""
    val doubleTag = if (value > 1) " x2!" else ""
    val celebratoryMsg = when {
      isChampion -> "🏆 VITÓRIA: VOCÊ É O CAMPEÃO! 🏆"
      endlessMode -> "+$value ${cat.breed.name}$doubleTag ($catsCollected ♾️)$comboSuffix"
      else -> "+$value ${cat.breed.name}$doubleTag ($catsCollected/$goalCats)$comboSuffix"
    }

    addFloatingTextSafe(
      FloatingText(
        text = celebratoryMsg,
        position = heroPosition + Vector2D(0f, -45f),
        color = if (isChampion) Color(0xFFFDE047) else Color(0xFF4ADE80)
      )
    )

    for (i in 0 until 20) {
      val angle = Random.nextFloat() * 2f * PI.toFloat()
      val spd = Random.nextFloat() * 220f + 50f
      addParticleSafe(
        Particle(
          position = cat.position,
          velocity = Vector2D(cos(angle) * spd, sin(angle) * spd),
          color = if (isChampion) Color(0xFFFACC15) else cat.breed.furColor,
          size = Random.nextFloat() * 8f + 5f,
          life = 0.8f
        )
      )
    }

    if (isChampion) {
      status = GameStatus.VICTORY
      audio?.vibrateVictory()
      activeCollectibles.clear()
    } else {
      if (endlessMode || currentMode.isBotsMode) {
        spawnNextCat()
        if (activeCollectibles.size < max(16, currentMode.startingCatsOnMap)) {
          spawnNextCat()
        }
      } else {
        val remaining = goalCats - catsCollected
        if (activeCollectibles.size < remaining) {
          spawnNextCat()
        }
      }

      if (Random.nextFloat() < powerUpFrequency.spawnChance && activePowerUps.size < 5) {
        spawnPowerUp()
      }
    }
  }

  private fun collectPowerUp(powerUp: PowerUpItem) {
    val def = powerUp.itemDef
    powerUpsCollectedThisRun++
    collectedItemIdsThisRun.add(def.id)
    audio?.vibrateCollect()

    when (def.effectType) {
      ItemEffectType.SPEED_BOOST -> {
        audio?.playPowerUpSound()
        catnipTimer = max(catnipTimer, def.durationSec)
        activeSpeedMult = max(activeSpeedMult, def.powerMagnitude)
      }
      ItemEffectType.GHOST_FLEE -> {
        audio?.playPowerUpSound()
        goldenFishTimer = max(goldenFishTimer, def.durationSec)
        ghosts.forEach { it.isFleeing = true }
        ghostsScaredThisRun++
      }
      ItemEffectType.SHIELD -> {
        audio?.playPowerUpSound()
        shieldActive = true
      }
      ItemEffectType.MAGNET -> {
        audio?.playPowerUpSound()
        magnetTimer = max(magnetTimer, def.durationSec)
      }
      ItemEffectType.FREEZE_GHOSTS -> {
        audio?.playFreezeSound()
        freezeTimer = max(freezeTimer, def.durationSec)
        ghosts.forEach { it.freezeTimerSec = max(it.freezeTimerSec, def.durationSec) }
      }
      ItemEffectType.HEAL_HEART -> {
        audio?.playPowerUpSound()
        if (lives < maxLives) {
          lives++
        } else {
          shieldActive = true
        }
        if (def.durationSec > 0f) {
          catnipTimer = max(catnipTimer, def.durationSec)
        }
      }
      ItemEffectType.SONIC_BLAST -> {
        audio?.playSonicMeowSound()
        emitSonicShockwave(heroPosition, maxRadius = 520f, stunSec = 3.0f)
      }
      ItemEffectType.INVISIBILITY -> {
        audio?.playPowerUpSound()
        invisibilityTimer = max(invisibilityTimer, def.durationSec)
      }
      ItemEffectType.DOUBLE_CAT_VALUE -> {
        audio?.playPowerUpSound()
        doubleCatTimer = max(doubleCatTimer, def.durationSec)
      }
      ItemEffectType.SLOW_MOTION_GHOSTS -> {
        audio?.playFreezeSound()
        slowMotionTimer = max(slowMotionTimer, def.durationSec)
      }
      ItemEffectType.TURBO_OVERDRIVE -> {
        audio?.playDashSound()
        turboOverdriveTimer = max(turboOverdriveTimer, def.durationSec)
        dashCooldownTimer = 0f
      }
      ItemEffectType.SONIC_OVERDRIVE -> {
        audio?.playSonicMeowSound()
        sonicOverdriveTimer = max(sonicOverdriveTimer, def.durationSec)
        sonicCooldownTimer = 0f
      }
      ItemEffectType.GIANT_MAGNET -> {
        audio?.playPowerUpSound()
        giantMagnetTimer = max(giantMagnetTimer, def.durationSec)
      }
      ItemEffectType.SPAWN_EXTRA_CATS -> {
        audio?.playCatCollectSound(false)
        repeat(3) { spawnNextCat() }
      }
      ItemEffectType.TELEPORT_SAFE -> {
        audio?.playDashSound()
        val corners = listOf(
          Vector2D(250f, 250f),
          Vector2D(worldWidth - 250f, 250f),
          Vector2D(250f, worldHeight - 250f),
          Vector2D(worldWidth - 250f, worldHeight - 250f)
        )
        val bestSpot = corners.maxByOrNull { spot ->
          ghosts.minOfOrNull { (it.position - spot).length() } ?: 0f
        } ?: heroPosition
        heroPosition = bestSpot
        isInvincible = true
        invincibilityTimer = max(invincibilityTimer, 1.5f)
      }
      ItemEffectType.INVINCIBILITY_STAR -> {
        audio?.playPowerUpSound()
        starInvincibleTimer = max(starInvincibleTimer, def.durationSec)
        if (def.id == "champion_elixir") {
          catnipTimer = max(catnipTimer, def.durationSec)
          magnetTimer = max(magnetTimer, def.durationSec)
        }
      }
      ItemEffectType.TIME_BONUS_15S -> {
        audio?.playPowerUpSound()
        if (currentMode.timeLimitSec > 0f) {
          timeLimitRemaining += 15f
        } else {
          catnipTimer = max(catnipTimer, 4f)
        }
      }
      ItemEffectType.GHOST_REPEL_RING -> {
        audio?.playSonicMeowSound()
        emitSonicShockwave(heroPosition, maxRadius = 700f, stunSec = 3.5f)
      }
      ItemEffectType.RECHARGE_SKILLS -> {
        audio?.playDashSound()
        dashCooldownTimer = 0f
        sonicCooldownTimer = 0f
      }
      ItemEffectType.LUCKY_RAIN -> {
        audio?.playPowerUpSound()
        repeat(2) { spawnNextCat() }
        repeat(3) { spawnPowerUp() }
      }
    }

    addFloatingTextSafe(
      FloatingText(
        text = "${def.emoji} ${def.name}: ${def.effectSummary}!",
        position = heroPosition + Vector2D(0f, -35f),
        color = def.color
      )
    )

    for (i in 0 until 18) {
      val angle = Random.nextFloat() * 2f * PI.toFloat()
      val spd = Random.nextFloat() * 150f + 40f
      addParticleSafe(
        Particle(
          position = powerUp.position,
          velocity = Vector2D(cos(angle) * spd, sin(angle) * spd),
          color = def.color,
          size = 7f,
          life = 0.6f
        )
      )
    }
  }

  private fun onHeroHitByGhost(hitGhost: Ghost) {
    if (shieldActive) {
      shieldActive = false
      isInvincible = true
      invincibilityTimer = 1.5f
      addFloatingTextSafe(FloatingText("Escudo de Leite protegeu você! 🥛✨", heroPosition, Color(0xFF38BDF8)))
      audio?.playPowerUpSound()
      return
    }

    lives--
    damageTakenThisRun++
    currentCombo = 0
    isInvincible = true
    invincibilityTimer = 2.0f
    audio?.playGhostHitSound()
    audio?.vibrateGhostHit()

    val pushDir = (heroPosition - hitGhost.position).normalized()
    heroPosition = clampAndResolveObstacles(heroPosition + pushDir * 80f, 32f)
    hitGhost.position = Vector2D(
      (hitGhost.position.x - pushDir.x * 120f).coerceIn(40f, worldWidth - 40f),
      (hitGhost.position.y - pushDir.y * 120f).coerceIn(40f, worldHeight - 40f)
    )

    addFloatingTextSafe(FloatingText("Ai! O fantasma te pegou! 💔", heroPosition, Color(0xFFEF4444)))

    for (i in 0 until 20) {
      val angle = Random.nextFloat() * 2f * PI.toFloat()
      val spd = Random.nextFloat() * 180f + 60f
      addParticleSafe(
        Particle(
          position = heroPosition,
          velocity = Vector2D(cos(angle) * spd, sin(angle) * spd),
          color = Color(0xFFEF4444),
          size = 8f,
          life = 0.6f
        )
      )
    }

    if (lives <= 0) {
      status = GameStatus.GAME_OVER
    }
  }

  fun spawnNextCat() {
    val allBreeds = if (endlessMode) CHAMPION_CATS + ENDLESS_EXTRA_BREEDS else CHAMPION_CATS
    val breed = if (endlessMode || currentMode.goalCats > 10) {
      allBreeds[Random.nextInt(allBreeds.size)]
    } else {
      CHAMPION_CATS[catsCollected % CHAMPION_CATS.size]
    }

    var spawnPos = Vector2D(worldWidth * 0.5f, worldHeight * 0.35f)
    for (attempt in 0 until 25) {
      val margin = 110f
      val rx = Random.nextFloat() * (worldWidth - margin * 2) + margin
      val ry = Random.nextFloat() * (worldHeight - margin * 2) + margin
      val candidate = Vector2D(rx, ry)
      val distToHero = (candidate - heroPosition).length()
      val minGhostDist = ghosts.minOfOrNull { (candidate - it.position).length() } ?: 999f
      val minCatDist = activeCollectibles.minOfOrNull { (candidate - it.position).length() } ?: 999f

      if (distToHero > 240f && minGhostDist > 180f && minCatDist > 120f) {
        var clearOfObstacles = true
        for (obs in obstacles) {
          if ((candidate - Vector2D(obs.x, obs.y)).length() < obs.radius + 40f) {
            clearOfObstacles = false
            break
          }
        }
        if (clearOfObstacles) {
          spawnPos = candidate
          break
        }
      }
    }

    val wanderAngle = Random.nextFloat() * 2f * PI.toFloat()
    val wander = if (endlessMode || currentMode.startingCatsOnMap >= 5) {
      Vector2D(cos(wanderAngle), sin(wanderAngle))
    } else {
      Vector2D(0f, 0f)
    }

    activeCollectibles.add(
      CollectibleCat(
        id = nextCatSerial++,
        breed = breed,
        position = spawnPos,
        wanderDir = wander
      )
    )
  }

  fun spawnPowerUp() {
    val useCustom = spawnCustomItemsEnabled && customItemsPool.isNotEmpty() &&
      Random.nextFloat() < (if (activeCustomMap != null) 0.50f else 0.25f)
    val chosenItem = if (useCustom) {
      customItemsPool[Random.nextInt(customItemsPool.size)]
    } else {
      ALL_40_ITEMS[Random.nextInt(ALL_40_ITEMS.size)]
    }
    val margin = 120f
    val rx = Random.nextFloat() * (worldWidth - margin * 2) + margin
    val ry = Random.nextFloat() * (worldHeight - margin * 2) + margin
    activePowerUps.add(
      PowerUpItem(
        itemDef = chosenItem,
        position = Vector2D(rx, ry)
      )
    )
  }

  private fun clampAndResolveObstacles(pos: Vector2D, radius: Float): Vector2D {
    var cur = Vector2D(
      pos.x.coerceIn(radius + 20f, worldWidth - radius - 20f),
      pos.y.coerceIn(radius + 20f, worldHeight - radius - 20f)
    )

    for (obs in obstacles) {
      val obsPos = Vector2D(obs.x, obs.y)
      val diff = cur - obsPos
      val dist = diff.length()
      val minAllowed = obs.radius + radius
      if (dist < minAllowed && dist > 0.001f) {
        val push = diff.normalized() * (minAllowed - dist)
        cur = cur + push
      }
    }
    return cur
  }

  fun calculateScore(isVictory: Boolean = (status == GameStatus.VICTORY)): Int {
    if (catsCollected <= 0 && !isVictory) return 0
    val catPoints = catsCollected * 150
    val levelPoints = (matchLevel - 1) * 100
    val comboPoints = maxComboThisRun * 35
    val timeBudget = if (isVictory) 600 else 240
    val timeBonus = max(0, timeBudget - (gameTimeSeconds * 3f).toInt())
    val livesBonus = if (isVictory) lives * 100 else 0
    return (catPoints + levelPoints + comboPoints + timeBonus + livesBonus).coerceAtLeast(0)
  }
}
