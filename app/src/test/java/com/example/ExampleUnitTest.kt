package com.example

import com.example.game.GameEngine
import com.example.game.RemotePartnerState
import com.example.model.*
import com.example.ui.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun `catalogs contain exactly 40 unique achievements, 40 unique items, and 40 unique modes`() {
    assertEquals(40, ALL_40_ACHIEVEMENTS.size)
    assertEquals(40, ALL_40_ACHIEVEMENTS.map { it.id }.toSet().size)
    assertEquals((1..40).toList(), ALL_40_ACHIEVEMENTS.map { it.number })

    assertEquals(40, ALL_40_ITEMS.size)
    assertEquals(40, ALL_40_ITEMS.map { it.id }.toSet().size)
    assertEquals((1..40).toList(), ALL_40_ITEMS.map { it.number })

    assertEquals(40, ALL_40_MODES.size)
    assertEquals(40, ALL_40_MODES.map { it.id }.toSet().size)
    assertEquals((1..40).toList(), ALL_40_MODES.map { it.number })
  }

  @Test
  fun `game engine initializes with correct defaults`() {
    val engine = GameEngine()
    assertEquals(GameStatus.READY, engine.status)
    assertEquals(3, engine.lives)
    assertEquals(0, engine.catsCollected)
    assertEquals(10, engine.goalCats)
  }

  @Test
  fun `collecting 10 cats achieves champion victory status`() {
    val engine = GameEngine()
    engine.startPlaying()
    assertEquals(GameStatus.PLAYING, engine.status)

    for (i in 1..10) {
      val cat = engine.activeCollectible
      assertTrue("Collectible cat should exist", cat != null)
      engine.heroPosition = cat!!.position
      engine.update(0.016f, Vector2D(0f, 0f))
    }

    assertEquals(10, engine.catsCollected)
    assertEquals(GameStatus.VICTORY, engine.status)
  }

  @Test
  fun `endless mode spawns many cats simultaneously and never stops`() {
    val engine = GameEngine()
    engine.resetGame(isEndless = true)
    engine.startPlaying()

    assertTrue("Endless mode should spawn at least 10 cats at once", engine.activeCollectibles.size >= 10)

    for (i in 1..15) {
      val cat = engine.activeCollectibles.first()
      engine.heroPosition = cat.position
      engine.update(0.016f, Vector2D(0f, 0f))
    }

    assertTrue("Should collect at least 15 cats in endless mode", engine.catsCollected >= 15)
    assertEquals(GameStatus.PLAYING, engine.status)
    assertTrue("Endless mode must continue keeping many cats on map", engine.activeCollectibles.size >= 10)
  }

  @Test
  fun `god mode prevents losing lives on ghost collision`() {
    val engine = GameEngine()
    engine.godMode = true
    engine.startPlaying()
    assertEquals(3, engine.lives)

    engine.ghost.position = engine.heroPosition
    engine.update(0.016f, Vector2D(0f, 0f))

    assertEquals(3, engine.lives)
    assertEquals(GameStatus.PLAYING, engine.status)
    assertFalse("Hero should not take damage in God Mode", engine.damageTakenThisRun > 0)
  }

  @Test
  fun `invisibility and double cat items apply real gameplay effects`() {
    val engine = GameEngine()
    engine.startPlaying()

    val cloakItem = ALL_40_ITEMS.first { it.effectType == ItemEffectType.INVISIBILITY }
    val doubleItem = ALL_40_ITEMS.first { it.effectType == ItemEffectType.DOUBLE_CAT_VALUE }

    engine.activePowerUps.clear()
    engine.activePowerUps.add(PowerUpItem(cloakItem, engine.heroPosition))
    engine.activePowerUps.add(PowerUpItem(doubleItem, engine.heroPosition))
    engine.update(0.016f, Vector2D(0f, 0f))

    assertTrue("Invisibility should be active", engine.isEffectivelyInvisible)
    assertTrue("Double cat timer should be active", engine.doubleCatTimer > 0f)

    // Ghost collision while invisible should deal 0 damage
    engine.ghost.position = engine.heroPosition
    engine.update(0.016f, Vector2D(0f, 0f))
    assertEquals(3, engine.lives)

    // Collecting 1 cat with Double Cat Value adds +2
    val before = engine.catsCollected
    engine.heroPosition = engine.activeCollectible!!.position
    engine.update(0.016f, Vector2D(0f, 0f))
    assertTrue("Double cat item should add at least 2 cats", engine.catsCollected - before >= 2)
  }

  @Test
  fun `timed mode ends in game over when time expires and ninja mode has 1 life`() {
    val engine = GameEngine()
    val timedMode = ALL_40_MODES.first { it.id == "time_attack_60" }
    engine.resetGameForMode(timedMode)
    engine.startPlaying()
    assertEquals(60f, engine.timeLimitRemaining, 0.01f)

    // Fast forward past 60s
    engine.timeLimitRemaining = 0.01f
    engine.update(0.05f, Vector2D(0f, 0f))
    assertEquals(GameStatus.GAME_OVER, engine.status)

    // Ninja 1-hit mode
    val ninjaMode = ALL_40_MODES.first { it.id == "ninja_one_hit" }
    engine.resetGameForMode(ninjaMode)
    engine.startPlaying()
    assertEquals(1, engine.lives)
    engine.ghost.position = engine.heroPosition
    engine.update(0.016f, Vector2D(0f, 0f))
    assertEquals(0, engine.lives)
    assertEquals(GameStatus.GAME_OVER, engine.status)
  }

  @Test
  fun `multiple ghosts and sonic wave work properly`() {
    val engine = GameEngine()
    engine.ghostCount = 3
    engine.resetGame(isEndless = false)
    engine.startPlaying()

    assertEquals(3, engine.ghosts.size)

    engine.ghosts[0].position = engine.heroPosition + Vector2D(50f, 0f)
    val triggered = engine.triggerSonicWave()
    assertTrue("Sonic wave should trigger", triggered)
    assertTrue("Ghost should be stunned by sonic wave", engine.ghosts[0].stunTimerSec > 0f)
  }

  @Test
  fun `triggering dash increases speed and sets dash timer`() {
    val engine = GameEngine()
    engine.startPlaying()
    val dashed = engine.triggerDash()
    assertTrue("Dash should succeed when ready", dashed)
    assertTrue("Hero should be dashing", engine.isDashing)
  }

  @Test
  fun `ghost hit reduces lives and gives invulnerability frames`() {
    val engine = GameEngine()
    engine.startPlaying()
    assertEquals(3, engine.lives)

    engine.ghost.position = engine.heroPosition
    engine.update(0.016f, Vector2D(0f, 0f))

    assertEquals(2, engine.lives)
    assertTrue("Hero should be temporarily invincible after hit", engine.isInvincible)
  }

  @Test
  fun `custom difficulty 1 to 1000 scales ghost movement speed accurately`() {
    val slowEngine = GameEngine()
    slowEngine.difficulty = Difficulty.CUSTOM
    slowEngine.customGhostSpeed = 1
    slowEngine.startPlaying()
    val startSlow = slowEngine.ghost.position
    repeat(10) { slowEngine.update(0.05f, Vector2D(0f, 0f)) }
    val slowDist = (slowEngine.ghost.position - startSlow).length()

    val fastEngine = GameEngine()
    fastEngine.difficulty = Difficulty.CUSTOM
    fastEngine.customGhostSpeed = 1000
    fastEngine.startPlaying()
    val startFast = fastEngine.ghost.position
    repeat(10) { fastEngine.update(0.05f, Vector2D(0f, 0f)) }
    val fastDist = (fastEngine.ghost.position - startFast).length()

    assertTrue("Speed 1000 ghost should travel much farther than speed 1 ghost", fastDist > slowDist * 5f)
  }

  @Test
  fun `custom map, custom item, and 16 skins work properly`() {
    assertEquals(16, ALL_HERO_SKINS.size)
    assertEquals(16, ALL_HERO_SKINS.map { it.id }.toSet().size)
    assertTrue(ALL_HERO_SKINS.first().isUnlocked(0, 0, 0, 0, 0, 0, 0, 1, false))
    assertFalse(ALL_HERO_SKINS.last().isUnlocked(0, 0, 0, 0, 0, 0, 0, 1, false))

    val customMap = CustomMapConfig(
      name = "Mapa Teste",
      paletteId = "magma",
      goalCats = 7,
      isEndless = false,
      ghostCount = 2,
      isDarknessMode = true,
      obstacles = listOf(CustomObstacleSpec(0.3f, 0.4f, "FOUNTAIN"))
    )
    val parsedMap = CustomMapConfig.fromJsonString(customMap.toJsonString())
    assertEquals("Mapa Teste", parsedMap.name)
    assertEquals(7, parsedMap.goalCats)
    assertEquals(1, parsedMap.obstacles.size)

    val customItem = CustomItemConfig(
      id = "my_test_item",
      name = "Pizza Turbo",
      emoji = "🍕",
      colorArgb = 0xFF10B981L,
      effectType = ItemEffectType.SPEED_BOOST,
      durationSec = 9f,
      powerMagnitude = 1.8f
    )
    val parsedItems = CustomItemConfig.listFromJson(CustomItemConfig.listToJson(listOf(customItem)))
    assertEquals(1, parsedItems.size)
    assertEquals("Pizza Turbo", parsedItems.first().name)

    val engine = GameEngine()
    engine.customItemsPool = parsedItems.mapIndexed { idx, c -> c.toGameItemDef(idx) }
    engine.resetGameForCustomMap(parsedMap)
    assertEquals(7, engine.goalCats)
    assertEquals(2, engine.ghosts.size)
    assertEquals(1, engine.obstacles.size)
    assertTrue(engine.currentMode.isDarknessMode)
  }

  @Test
  fun `match level increases every 5 cats and scoreboard increases with cats and speed`() {
    val engine = GameEngine()
    engine.resetGame(isEndless = true)
    engine.startPlaying()
    assertEquals(1, engine.matchLevel)
    assertEquals(0, engine.calculateScore(false))

    for (i in 1..5) {
      val cat = engine.activeCollectibles.first()
      engine.heroPosition = cat.position
      engine.update(0.016f, Vector2D(0f, 0f))
    }
    assertEquals(2, engine.matchLevel)
    val scoreAt5 = engine.calculateScore(false)
    assertTrue("Score should be positive after collecting 5 cats", scoreAt5 >= 750)

    for (i in 1..5) {
      val cat = engine.activeCollectibles.first()
      engine.heroPosition = cat.position
      engine.update(0.016f, Vector2D(0f, 0f))
    }
    assertEquals(3, engine.matchLevel)
    assertTrue("Score should increase with more cats", engine.calculateScore(false) > scoreAt5)
  }

  @Test
  fun `rewards and achievements are blocked in god mode and modified gameplay`() {
    val fairState = GameUiState()
    val (fairBlocked, _) = computeRewardsBlocked(fairState, activeCustomMap = false)
    assertFalse("Default fair settings should allow rewards", fairBlocked)

    val (godBlocked, _) = computeRewardsBlocked(fairState.copy(isGodModeEnabled = true))
    assertTrue("God Mode must block rewards", godBlocked)

    val (customSpeedBlocked, _) = computeRewardsBlocked(fairState.copy(difficulty = Difficulty.CUSTOM))
    assertTrue("Custom ghost speed must block rewards", customSpeedBlocked)

    val (heroSpeedBlocked, _) = computeRewardsBlocked(fairState.copy(heroSpeedOption = HeroSpeedOption.SUPER))
    assertTrue("Modified hero speed must block rewards", heroSpeedBlocked)

    val (customMapBlocked, _) = computeRewardsBlocked(fairState, activeCustomMap = true)
    assertTrue("Custom map must block rewards", customMapBlocked)
  }

  @Test
  fun `easy difficulty calmo does not block rewards`() {
    val easyState = GameUiState(difficulty = Difficulty.CALMO)
    val (blocked, reason) = computeRewardsBlocked(easyState, activeCustomMap = false)
    assertFalse("Modo Fácil (CALMO) deve permitir conquistas e recompensas", blocked)
    assertEquals("", reason)
  }

  @Test
  fun `elite difficulty lets player act as ghost hunting ai runner cat`() {
    val engine = GameEngine()
    engine.difficulty = Difficulty.ELITE
    engine.resetGame(isEndless = false)
    engine.startPlaying()

    assertTrue("Jogador deve ser o fantasma na dificuldade Elite", engine.isPlayerGhost)
    assertTrue("Fantasmas de IA devem ficar vazios no modo Elite solo", engine.ghosts.isEmpty())
    val runner = engine.aiRunnerCat
    assertTrue("Corredor Gato IA deve existir no modo Elite solo", runner != null)

    // Capture runner 3 times
    for (hit in 1..3) {
      val currentRunner = engine.aiRunnerCat!!
      engine.heroPosition = currentRunner.position
      engine.update(0.016f, Vector2D(0f, 0f))
      // Advance cooldown (>2.0s) if not yet 3rd hit
      if (hit < 3) {
        repeat(110) { engine.update(0.02f, Vector2D(0f, 0f)) }
      }
    }

    assertEquals(3, engine.catsCollected)
    assertEquals(GameStatus.VICTORY, engine.status)
  }

  @Test
  fun `multiplayer supports over 100 players and removes departed player immediately`() {
    val engine = GameEngine()
    val batch120 = (1..120).map { i ->
      RemotePartnerState(
        userId = "uid_$i",
        username = "Player$i",
        position = Vector2D(200f + (i % 10) * 80f, 200f + (i / 10) * 60f),
        score = i % 10,
        role = if (i == 1) "ghost" else "cat",
        skinId = "classic_orange"
      )
    }

    engine.setMultiplayerParticipants(
      active = true,
      myRole = "ghost",
      players = batch120,
      captureGoal = 3
    )

    assertTrue("Jogador eleito fantasma deve atuar como fantasma", engine.isPlayerGhost)
    assertEquals(120, engine.snapshotRemotePlayers().size)

    // Remove one player who left the room
    val batch119 = batch120.drop(1)
    engine.setMultiplayerParticipants(
      active = true,
      myRole = "ghost",
      players = batch119,
      captureGoal = 3
    )
    assertEquals(119, engine.snapshotRemotePlayers().size)
    assertFalse(engine.snapshotRemotePlayers().any { it.userId == "uid_1" })
  }

  @Test
  fun `bots arena mode spawns 20 bots with 50 cat goal and crowns bot winner at 50`() {
    val engine = GameEngine()
    engine.resetGameForMode(BOTS_ARENA_MODE)
    engine.startPlaying()

    assertEquals(40, ALL_40_MODES.size)
    assertTrue(engine.currentMode.isBotsMode)
    assertEquals(50, engine.goalCats)
    assertEquals(20, engine.bots.size)
    assertTrue("Bots arena should keep ample collectibles on map", engine.activeCollectibles.size >= 16)

    // Move bot 0 onto cats repeatedly until it hits 50
    val firstBot = engine.bots.first()
    firstBot.catsCollected = 49
    val targetCat = engine.activeCollectibles.first()
    firstBot.position = targetCat.position
    engine.update(0.016f, Vector2D(0f, 0f))

    assertEquals(50, firstBot.catsCollected)
    assertEquals(firstBot.name, engine.winnerBotName)
    assertEquals(GameStatus.GAME_OVER, engine.status)
    assertFalse("Cannot revive when a bot already won the 50-cat race", engine.revivePlayer())
  }

  @Test
  fun `revivePlayer restores lives and resumes game after ghost game over`() {
    val engine = GameEngine()
    val ninjaMode = ALL_40_MODES.first { it.id == "ninja_one_hit" }
    engine.resetGameForMode(ninjaMode)
    engine.startPlaying()
    assertEquals(1, engine.lives)

    engine.ghost.position = engine.heroPosition
    engine.update(0.016f, Vector2D(0f, 0f))
    assertEquals(GameStatus.GAME_OVER, engine.status)
    assertEquals(0, engine.lives)

    val revived = engine.revivePlayer()
    assertTrue("Revive should succeed after ghost capture", revived)
    assertEquals(GameStatus.PLAYING, engine.status)
    assertTrue("Revive should restore at least 3 lives", engine.lives >= 3)
    assertTrue("Revive should grant temporary invincibility", engine.isInvincible)
  }

  @Test
  fun `engine snapshots return safe isolated copies`() {
    val engine = GameEngine()
    engine.startPlaying()
    val catsSnap = engine.snapshotCollectibles()
    val ghostsSnap = engine.snapshotGhosts()
    assertTrue(catsSnap.isNotEmpty())
    assertTrue(ghostsSnap.isNotEmpty())
    engine.activeCollectibles.clear()
    assertTrue("Snapshot must remain intact if engine list changes", catsSnap.isNotEmpty())
  }
}
