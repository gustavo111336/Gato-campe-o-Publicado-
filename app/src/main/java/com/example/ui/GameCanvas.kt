package com.example.ui

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.game.GameEngine
import com.example.game.Obstacle
import com.example.game.RemotePartnerState
import com.example.model.*
import kotlin.math.*

@Composable
fun GameCanvas(
  engine: GameEngine,
  uiState: GameUiState,
  modifier: Modifier = Modifier,
  onTouchDirectDirection: (Vector2D) -> Unit = {}
) {
  val textPaint = remember {
    Paint().apply {
      isAntiAlias = true
      textAlign = Paint.Align.CENTER
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      textSize = 32f
      setShadowLayer(6f, 0f, 2f, android.graphics.Color.BLACK)
    }
  }
  val itemEmojiPaint = remember {
    Paint().apply {
      isAntiAlias = true
      textAlign = Paint.Align.CENTER
      textSize = 24f
    }
  }

  val context = LocalContext.current
  val activeSkin = remember(uiState.selectedSkinId) {
    ALL_HERO_SKINS.find { it.id == uiState.selectedSkinId } ?: ALL_HERO_SKINS.first()
  }
  val is4d = uiState.is4dModeEnabled
  val is3d = uiState.is3dModeEnabled || is4d

  var tiltX by remember { mutableFloatStateOf(0f) }
  var tiltY by remember { mutableFloatStateOf(0f) }

  DisposableEffect(context, is4d) {
    if (!is4d) {
      tiltX = 0f
      tiltY = 0f
      return@DisposableEffect onDispose {}
    }
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
      ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    if (sensorManager == null || sensor == null) {
      return@DisposableEffect onDispose {}
    }
    val listener = object : SensorEventListener {
      override fun onSensorChanged(event: SensorEvent) {
        val rawX = (event.values.getOrNull(0) ?: 0f) / 9.81f
        val rawY = (event.values.getOrNull(1) ?: 0f) / 9.81f
        tiltX = (tiltX * 0.82f + rawX.coerceIn(-1f, 1f) * 0.18f)
        tiltY = (tiltY * 0.82f + (rawY - 0.35f).coerceIn(-1f, 1f) * 0.18f)
      }
      override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
    sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
    onDispose {
      sensorManager.unregisterListener(listener)
    }
  }

  val touchModifier = if (uiState.controlType == ControlType.TOUCH_DIRECT) {
    Modifier.pointerInput(Unit) {
      detectDragGestures(
        onDragStart = { offset ->
          val center = Offset(size.width / 2f, size.height / 2f)
          val diff = offset - center
          onTouchDirectDirection(Vector2D(diff.x, diff.y).normalized())
        },
        onDrag = { change, _ ->
          val center = Offset(size.width / 2f, size.height / 2f)
          val diff = change.position - center
          onTouchDirectDirection(Vector2D(diff.x, diff.y).normalized())
        },
        onDragEnd = { onTouchDirectDirection(Vector2D(0f, 0f)) },
        onDragCancel = { onTouchDirectDirection(Vector2D(0f, 0f)) }
      )
    }.pointerInput(Unit) {
      detectTapGestures(
        onTap = { offset ->
          val center = Offset(size.width / 2f, size.height / 2f)
          val diff = offset - center
          onTouchDirectDirection(Vector2D(diff.x, diff.y).normalized())
        }
      )
    }
  } else Modifier

  Box(modifier = modifier.fillMaxSize().then(touchModifier).testTag("game_canvas")) {
    // LAYER 0: DEEP PARALLAX DIMENSION (Only rendered in Modo 4D)
    if (is4d) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        if (size.width <= 1f || size.height <= 1f) return@Canvas
        val t = uiState.gameTimeSeconds
        val driftX = tiltX * 48f + sin(t * 1.1f) * 18f
        val driftY = tiltY * 48f + cos(t * 0.9f) * 18f
        drawRect(
          brush = Brush.verticalGradient(
            listOf(Color(0xFF020617), Color(0xFF1E1B4B), Color(0xFF090D24))
          )
        )
        for (i in 0 until 26) {
          val bx = (((i * 173 + 57) % 1000) / 1000f) * size.width + driftX * (0.4f + (i % 4) * 0.25f)
          val by = (((i * 317 + 89) % 1000) / 1000f) * size.height + driftY * (0.4f + (i % 3) * 0.3f)
          val wrappedX = ((bx % size.width) + size.width) % size.width
          val wrappedY = ((by % size.height) + size.height) % size.height
          val col = if (i % 2 == 0) Color(0xFF38BDF8) else Color(0xFFE879F9)
          drawCircle(
            color = col.copy(alpha = 0.28f),
            radius = 2f + (i % 4) * 1.6f,
            center = Offset(wrappedX, wrappedY)
          )
        }
      }
    }

    // LAYER 1: WORLD SPACE CANVAS (Tilted in 3D / Dynamic 4D Holographic Tilt)
    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
          val t = uiState.gameTimeSeconds
          if (is4d) {
            rotationX = (25f - tiltY * 14f + sin(t * 1.7f) * 2.2f).coerceIn(8f, 40f)
            rotationY = (tiltX * 16f + cos(t * 1.3f) * 2.5f).coerceIn(-18f, 18f)
            translationX = -tiltX * 22f
            translationY = -tiltY * 16f
            scaleX = 1.18f
            scaleY = 1.24f
            cameraDistance = 11f * density
          } else if (is3d) {
            rotationX = 28f
            scaleX = 1.16f
            scaleY = 1.24f
            cameraDistance = 16f * density
          }
        }
    ) {
      if (size.width <= 1f || size.height <= 1f) return@Canvas
      val frameTime = uiState.gameTimeSeconds

      val halfW = size.width / 2f
      val halfH = size.height / 2f
      val heroPos = engine.heroPosition
      val camX = heroPos.x.coerceIn(halfW, max(halfW, engine.worldWidth - halfW))
      val camY = heroPos.y.coerceIn(halfH, max(halfH, engine.worldHeight - halfH))

      val offsetX = halfW - camX
      val offsetY = halfH - camY

      if (!is4d) {
        drawRect(color = Color(0xFF020617))
      }

      val obstaclesSnapshot = engine.snapshotObstacles()
      val shockwavesSnapshot = engine.snapshotShockwaves()
      val powerUpsSnapshot = engine.snapshotPowerUps()
      val collectiblesSnapshot = engine.snapshotCollectibles()
      val followersSnapshot = engine.snapshotFollowers()
      val botsSnapshot = engine.snapshotBots()
      val ghostsSnapshot = engine.snapshotGhosts()
      val particlesSnapshot = engine.snapshotParticles()
      val remotePlayersSnapshot = engine.snapshotRemotePlayers()
      val aiRunnerSnapshot = engine.snapshotAiRunnerCat()

      withTransform({
        translate(left = offsetX, top = offsetY)
      }) {
        drawArenaGround(
          w = engine.worldWidth,
          h = engine.worldHeight,
          theme = engine.effectiveArenaTheme,
          customMap = engine.activeCustomMap,
          is3d = is3d
        )

        for (obs in obstaclesSnapshot) {
          drawObstacle(obs, frameTime, is3d)
        }

        for (sw in shockwavesSnapshot) {
          val r = max(1f, sw.radius)
          drawCircle(
            color = sw.color.copy(alpha = sw.alpha * 0.8f),
            radius = r,
            center = Offset(sw.origin.x, sw.origin.y),
            style = Stroke(width = 8f)
          )
          drawCircle(
            color = Color.White.copy(alpha = sw.alpha * 0.4f),
            radius = max(1f, r - 12f),
            center = Offset(sw.origin.x, sw.origin.y),
            style = Stroke(width = 3f)
          )
        }

        for (p in powerUpsSnapshot) {
          drawPowerUpItem(p, frameTime, itemEmojiPaint, is3d)
        }

        for (cat in collectiblesSnapshot) {
          if (is4d) {
            drawCircle(
              color = Color(0xFF22D3EE).copy(alpha = 0.22f),
              radius = 28f,
              center = Offset(cat.position.x - 5f, cat.position.y - 12f),
              style = Stroke(width = 2.5f)
            )
          }
          drawCollectibleCat(cat, frameTime, is3d)
        }

        for (follower in followersSnapshot) {
          drawFollowerCat(follower, is3d)
        }

        aiRunnerSnapshot?.let { runner ->
          drawAiRunnerCat(runner, frameTime, textPaint, is3d)
        }

        val nearestNamedIds = remotePlayersSnapshot
          .sortedBy { (it.position - heroPos).length() }
          .take(15)
          .map { it.userId.ifBlank { it.username } }
          .toSet()

        for (partner in remotePlayersSnapshot) {
          val key = partner.userId.ifBlank { partner.username }
          val showLabel = partner.role == "ghost" || nearestNamedIds.contains(key)
          drawRemotePartnerCat(partner, frameTime, textPaint, is3d, showLabel)
        }

        val nearestBotIds = botsSnapshot
          .sortedBy { (it.position - heroPos).length() }
          .take(10)
          .map { it.id }
          .toSet()

        for (bot in botsSnapshot) {
          drawBotCat(
            bot = bot,
            gameTime = frameTime,
            textPaint = textPaint,
            is3d = is3d,
            showLabel = nearestBotIds.contains(bot.id)
          )
        }

        if (is4d) {
          // Stereo 4D depth halo behind Hero
          drawCircle(
            color = Color(0xFF38BDF8).copy(alpha = 0.22f),
            radius = 34f,
            center = Offset(heroPos.x - 6f - tiltX * 6f, heroPos.y - 14f)
          )
          drawCircle(
            color = Color(0xFFE879F9).copy(alpha = 0.20f),
            radius = 34f,
            center = Offset(heroPos.x + 6f - tiltX * 6f, heroPos.y - 10f)
          )
        }

        if (uiState.isPlayerGhost || engine.isPlayerGhost) {
          drawPlayerHunterGhost(
            pos = heroPos,
            isDashing = engine.isDashing,
            is3d = is3d,
            gameTime = frameTime
          )
        } else {
          drawHeroCat(
            pos = heroPos,
            angle = engine.heroAngle,
            skin = activeSkin,
            isInvincible = engine.isInvincible,
            isInvisible = engine.isEffectivelyInvisible,
            hasShield = engine.shieldActive,
            hasCatnip = engine.catnipTimer > 0f,
            hasMagnet = engine.magnetTimer > 0f || engine.giantMagnetTimer > 0f,
            hasDoubleCat = engine.doubleCatTimer > 0f,
            isGodMode = engine.isEffectivelyGodMode,
            is3d = is3d,
            gameTime = frameTime
          )
        }

        for (g in ghostsSnapshot) {
          if (is4d) {
            drawCircle(
              color = Color(0xFFE879F9).copy(alpha = 0.22f),
              radius = 38f,
              center = Offset(g.position.x + 6f, g.position.y - 20f)
            )
          }
          drawGhost(ghost = g, gameTime = frameTime, is3d = is3d)
        }

        for (p in particlesSnapshot) {
          drawCircle(
            color = p.color.copy(alpha = p.alpha),
            radius = max(1f, p.size),
            center = Offset(p.position.x, p.position.y)
          )
        }
      }
    }

    // LAYER 2: CRISP UN-TILTED OVERLAYS + FOREGROUND 4D NEAR-EYE HOLOGRAPHIC MOTES
    Canvas(modifier = Modifier.fillMaxSize()) {
      if (size.width <= 1f || size.height <= 1f) return@Canvas
      val frameTime = uiState.gameTimeSeconds

      val halfW = size.width / 2f
      val halfH = size.height / 2f
      val heroPos = engine.heroPosition
      val camX = heroPos.x.coerceIn(halfW, max(halfW, engine.worldWidth - halfW))
      val camY = heroPos.y.coerceIn(halfH, max(halfH, engine.worldHeight - halfH))

      val offsetX = halfW - camX
      val offsetY = halfH - camY

      if (is4d) {
        // Near-eye foreground floating 4D motes & holographic depth frame
        for (i in 0 until 14) {
          val speed = 22f + (i % 5) * 9f
          val rawX = ((i * 211 + frameTime * speed) % (size.width + 80f)) - 40f + tiltX * 65f
          val rawY = ((i * 149 + sin(frameTime * 1.4f + i) * 35f) % (size.height + 80f)) - 40f + tiltY * 65f
          val mx = ((rawX % size.width) + size.width) % size.width
          val my = ((rawY % size.height) + size.height) % size.height
          val moteColor = if (i % 2 == 0) Color(0xFF67E8F9) else Color(0xFFF0ABFC)
          drawCircle(
            color = moteColor.copy(alpha = 0.24f),
            radius = 4f + (i % 3) * 3f,
            center = Offset(mx, my)
          )
          drawCircle(
            color = Color.White.copy(alpha = 0.35f),
            radius = 1.8f,
            center = Offset(mx, my)
          )
        }
        drawRect(
          brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color(0x4422D3EE)),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = max(1f, size.maxDimension * 0.72f)
          )
        )
      }

      if (engine.currentMode.isDarknessMode) {
        val heroScreenX = heroPos.x + offsetX
        val heroScreenY = heroPos.y + offsetY
        drawRect(
          brush = Brush.radialGradient(
            0.0f to Color.Transparent,
            0.52f to Color(0x55020617),
            1.0f to Color(0xEE020617),
            center = Offset(heroScreenX, heroScreenY),
            radius = 360f
          )
        )
      }

      if (uiState.isGhostNear) {
        val intensity = ((320f - uiState.ghostDistance) / 200f).coerceIn(0.15f, 0.65f)
        val pulse = (sin(frameTime * 10f) * 0.15f) + 0.5f
        val vignetteAlpha = (intensity * pulse).coerceIn(0f, 0.75f)
        drawRect(
          brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color(0xFF9333EA).copy(alpha = vignetteAlpha)),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = max(1f, size.minDimension * 0.75f)
          )
        )
      }

      val targetPos: Vector2D? = if (uiState.isPlayerGhost || engine.isPlayerGhost) {
        engine.snapshotAiRunnerCat()?.position
          ?: engine.snapshotRemotePlayers().filter { it.role != "ghost" }.minByOrNull { (it.position - heroPos).length() }?.position
      } else {
        engine.snapshotCollectibles().minByOrNull { (it.position - heroPos).length() }?.position
      }
      targetPos?.let { target ->
        drawCatTrackerCompass(
          heroPos = heroPos,
          catPos = target,
          screenSize = size,
          isGhostHunt = uiState.isPlayerGhost || engine.isPlayerGhost
        )
      }

      val textsSnapshot = engine.snapshotFloatingTexts()
      for (txt in textsSnapshot) {
        val screenX = txt.position.x + offsetX
        val screenY = txt.position.y + offsetY
        if (screenX in -100f..(size.width + 100f) && screenY in -60f..(size.height + 60f)) {
          textPaint.color = txt.color.copy(alpha = txt.alpha).toArgb()
          drawContext.canvas.nativeCanvas.drawText(
            txt.text,
            screenX,
            screenY,
            textPaint
          )
        }
      }
    }
  }
}

private fun DrawScope.drawRemotePartnerCat(
  partner: RemotePartnerState,
  gameTime: Float,
  textPaint: Paint,
  is3d: Boolean,
  showLabel: Boolean
) {
  val lift = if (is3d) -10f else 0f
  val center = Offset(partner.position.x, partner.position.y + lift)

  if (is3d) {
    drawOval(
      color = Color.Black.copy(alpha = 0.4f),
      topLeft = Offset(partner.position.x - 22f, partner.position.y + 10f),
      size = Size(44f, 20f)
    )
  }

  if (partner.role == "ghost") {
    val haloR = 40f + sin(gameTime * 7f) * 5f
    drawCircle(color = Color(0xFF9333EA).copy(alpha = 0.35f), radius = haloR, center = center)
    val ghostPath = Path().apply {
      moveTo(center.x - 22f, center.y)
      cubicTo(center.x - 22f, center.y - 32f, center.x + 22f, center.y - 32f, center.x + 22f, center.y)
      lineTo(center.x + 22f, center.y + 22f)
      cubicTo(center.x + 10f, center.y + 30f, center.x - 10f, center.y + 16f, center.x - 22f, center.y + 22f)
      close()
    }
    drawPath(ghostPath, color = Color(0xFFC084FC).copy(alpha = 0.9f))
    drawPath(ghostPath, color = Color(0xFFFDE047), style = Stroke(width = 2.5f))
    drawCircle(Color(0xFF581C87), 5f, Offset(center.x - 7f, center.y - 6f))
    drawCircle(Color(0xFF581C87), 5f, Offset(center.x + 7f, center.y - 6f))
    drawCircle(Color(0xFFFDE047), 2.5f, Offset(center.x - 7f, center.y - 6f))
    drawCircle(Color(0xFFFDE047), 2.5f, Offset(center.x + 7f, center.y - 6f))
  } else {
    val skin = ALL_HERO_SKINS.find { it.id == partner.skinId }
    val fur = skin?.furColor ?: Color(0xFF38BDF8)
    val ear = skin?.earColor ?: Color(0xFF0284C7)

    val haloR = 34f + sin(gameTime * 6f) * 4f
    drawCircle(color = fur.copy(alpha = 0.25f), radius = haloR, center = center)
    drawCircle(color = fur, radius = 21f, center = center)
    drawCircle(color = Color.White, radius = 21f, center = center, style = Stroke(width = 2.2f))

    val earPath = Path().apply {
      moveTo(center.x - 13f, center.y - 11f)
      lineTo(center.x - 17f, center.y - 26f)
      lineTo(center.x - 4f, center.y - 16f)
      close()
      moveTo(center.x + 4f, center.y - 16f)
      lineTo(center.x + 17f, center.y - 26f)
      lineTo(center.x + 13f, center.y - 11f)
      close()
    }
    drawPath(earPath, color = ear)
    drawCircle(Color(0xFF0F172A), 3f, Offset(center.x - 6f, center.y - 3f))
    drawCircle(Color(0xFF0F172A), 3f, Offset(center.x + 6f, center.y - 3f))
  }

  if (showLabel) {
    val savedColor = textPaint.color
    val savedSize = textPaint.textSize
    textPaint.color = if (partner.role == "ghost") android.graphics.Color.MAGENTA else android.graphics.Color.CYAN
    textPaint.textSize = 22f
    val prefix = if (partner.role == "ghost") "👻 " else ""
    drawContext.canvas.nativeCanvas.drawText(
      "$prefix@${partner.username} (${partner.score})",
      center.x,
      center.y - 36f,
      textPaint
    )
    textPaint.color = savedColor
    textPaint.textSize = savedSize
  }
}

private fun DrawScope.drawAiRunnerCat(
  runner: AiRunnerCat,
  gameTime: Float,
  textPaint: Paint,
  is3d: Boolean
) {
  val lift = if (is3d) -10f else 0f
  val center = Offset(runner.position.x, runner.position.y + lift)

  if (runner.invincibleTimer > 0f && (gameTime * 15f).toInt() % 2 == 0) {
    // Flicker when hit
  } else {
    if (is3d) {
      drawOval(
        color = Color.Black.copy(alpha = 0.4f),
        topLeft = Offset(runner.position.x - 22f, runner.position.y + 10f),
        size = Size(44f, 20f)
      )
    }
    val haloR = 38f + sin(gameTime * 8f) * 5f
    drawCircle(color = Color(0xFF10B981).copy(alpha = 0.28f), radius = haloR, center = center)
    drawCircle(color = Color(0xFFFB923C), radius = 23f, center = center)
    drawCircle(color = Color.White, radius = 23f, center = center, style = Stroke(width = 2.5f))

    val earPath = Path().apply {
      moveTo(center.x - 14f, center.y - 12f)
      lineTo(center.x - 18f, center.y - 29f)
      lineTo(center.x - 4f, center.y - 18f)
      close()
      moveTo(center.x + 4f, center.y - 18f)
      lineTo(center.x + 18f, center.y - 29f)
      lineTo(center.x + 14f, center.y - 12f)
      close()
    }
    drawPath(earPath, color = Color(0xFFEA580C))
    drawCircle(Color(0xFF0F172A), 3.5f, Offset(center.x - 6f, center.y - 3f))
    drawCircle(Color(0xFF0F172A), 3.5f, Offset(center.x + 6f, center.y - 3f))
  }

  val savedColor = textPaint.color
  val savedSize = textPaint.textSize
  textPaint.color = android.graphics.Color.GREEN
  textPaint.textSize = 23f
  drawContext.canvas.nativeCanvas.drawText(
    "🏃 Corredor (${runner.lives}❤️ • ${runner.catsCollected}/10)",
    center.x,
    center.y - 38f,
    textPaint
  )
  textPaint.color = savedColor
  textPaint.textSize = savedSize
}

private fun DrawScope.drawBotCat(
  bot: BotCatState,
  gameTime: Float,
  textPaint: Paint,
  is3d: Boolean,
  showLabel: Boolean
) {
  val lift = if (is3d) -9f else 0f
  val center = Offset(bot.position.x, bot.position.y + lift)

  if (bot.invincibleTimer > 0f && (gameTime * 14f).toInt() % 2 == 0) {
    return
  }

  if (is3d) {
    drawOval(
      color = Color.Black.copy(alpha = 0.36f),
      topLeft = Offset(bot.position.x - 19f, bot.position.y + 8f),
      size = Size(38f, 16f)
    )
  }

  val haloR = 30f + sin(gameTime * 6f + bot.id) * 3f
  drawCircle(color = bot.furColor.copy(alpha = 0.24f), radius = haloR, center = center)
  drawCircle(color = bot.furColor, radius = 19f, center = center)
  drawCircle(color = Color.White, radius = 19f, center = center, style = Stroke(width = 2f))

  val earPath = Path().apply {
    moveTo(center.x - 11f, center.y - 10f)
    lineTo(center.x - 15f, center.y - 24f)
    lineTo(center.x - 3f, center.y - 14f)
    close()
    moveTo(center.x + 3f, center.y - 14f)
    lineTo(center.x + 15f, center.y - 24f)
    lineTo(center.x + 11f, center.y - 10f)
    close()
  }
  drawPath(earPath, color = bot.furColor)
  drawCircle(Color(0xFF0F172A), 2.8f, Offset(center.x - 5.5f, center.y - 2.5f))
  drawCircle(Color(0xFF0F172A), 2.8f, Offset(center.x + 5.5f, center.y - 2.5f))

  if (showLabel) {
    val savedColor = textPaint.color
    val savedSize = textPaint.textSize
    textPaint.color = android.graphics.Color.YELLOW
    textPaint.textSize = 20f
    drawContext.canvas.nativeCanvas.drawText(
      "${bot.accentEmoji} ${bot.name} (${bot.catsCollected}/50)",
      center.x,
      center.y - 32f,
      textPaint
    )
    textPaint.color = savedColor
    textPaint.textSize = savedSize
  }
}

private fun DrawScope.drawPlayerHunterGhost(
  pos: Vector2D,
  isDashing: Boolean,
  is3d: Boolean,
  gameTime: Float
) {
  val lift = if (is3d) -16f else 0f
  val floatOffset = sin(gameTime * 6f) * 5f
  val center = Offset(pos.x, pos.y + floatOffset + lift)

  if (is3d) {
    drawOval(
      color = Color(0xFF3B0764).copy(alpha = 0.5f),
      topLeft = Offset(pos.x - 24f, pos.y + 16f),
      size = Size(48f, 20f)
    )
  }

  val glowR = if (isDashing) 56f else 48f + sin(gameTime * 7f) * 5f
  drawCircle(color = Color(0xFFA855F7).copy(alpha = 0.36f), radius = glowR, center = center)
  drawCircle(color = Color(0xFFFDE047), radius = glowR, center = center, style = Stroke(width = 3f))

  val ghostPath = Path().apply {
    moveTo(center.x - 24f, center.y)
    cubicTo(center.x - 24f, center.y - 36f, center.x + 24f, center.y - 36f, center.x + 24f, center.y)
    lineTo(center.x + 24f, center.y + 26f)
    val wave = sin(gameTime * 9f) * 5f
    cubicTo(center.x + 16f, center.y + 34f + wave, center.x + 8f, center.y + 18f, center.x, center.y + 30f - wave)
    cubicTo(center.x - 8f, center.y + 18f, center.x - 16f, center.y + 34f + wave, center.x - 24f, center.y + 26f)
    close()
  }
  drawPath(ghostPath, color = Color(0xFF7E22CE).copy(alpha = 0.92f))
  drawPath(ghostPath, color = Color(0xFFFDE047), style = Stroke(width = 2.8f))

  drawCircle(Color(0xFF1E1B4B), 6f, Offset(center.x - 8f, center.y - 8f))
  drawCircle(Color(0xFF1E1B4B), 6f, Offset(center.x + 8f, center.y - 8f))
  drawCircle(Color(0xFFEF4444), 3f, Offset(center.x - 8f, center.y - 8f))
  drawCircle(Color(0xFFEF4444), 3f, Offset(center.x + 8f, center.y - 8f))
}

private fun DrawScope.drawArenaGround(
  w: Float,
  h: Float,
  theme: ArenaTheme,
  customMap: CustomMapConfig?,
  is3d: Boolean
) {
  val palette = customMap?.palette
  val bgTop = palette?.bgTop ?: theme.bgTop
  val bgMid = palette?.bgMid ?: theme.bgMid
  val bgBottom = palette?.bgBottom ?: theme.bgBottom
  val gridCol = palette?.gridColor ?: theme.gridColor
  val wallCol = palette?.wallColor ?: theme.wallColor
  val wallHi = palette?.wallHighlight ?: theme.wallHighlight

  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(bgTop, bgMid, bgBottom),
      startY = 0f,
      endY = h
    ),
    topLeft = Offset(0f, 0f),
    size = Size(w, h)
  )

  val tileSize = 80f
  val cols = (w / tileSize).toInt()
  val rows = (h / tileSize).toInt()
  val gridColor = gridCol.copy(alpha = if (is3d) 0.36f else 0.28f)

  for (i in 0..cols) {
    drawLine(
      color = gridColor,
      start = Offset(i * tileSize, 0f),
      end = Offset(i * tileSize, h),
      strokeWidth = if (is3d) 1.5f else 1f
    )
  }
  for (j in 0..rows) {
    drawLine(
      color = gridColor,
      start = Offset(0f, j * tileSize),
      end = Offset(w, j * tileSize),
      strokeWidth = if (is3d) 1.5f else 1f
    )
  }

  drawCircle(
    color = Color(0xFF1E293B).copy(alpha = 0.35f),
    radius = 320f,
    center = Offset(w / 2f, h / 2f),
    style = Stroke(width = 40f)
  )

  if (is3d) {
    // 3D Extruded Outer Wall Base Shadow & Elevated Rim
    drawRect(
      color = Color.Black.copy(alpha = 0.45f),
      topLeft = Offset(10f, 16f),
      size = Size(w - 20f, h - 16f),
      style = Stroke(width = 34f)
    )
    drawRect(
      color = wallCol,
      topLeft = Offset(0f, 0f),
      size = Size(w, h),
      style = Stroke(width = 30f)
    )
    drawRect(
      color = wallHi.copy(alpha = 0.75f),
      topLeft = Offset(14f, 4f),
      size = Size(w - 28f, h - 18f),
      style = Stroke(width = 6f)
    )
  } else {
    drawRect(
      color = wallCol,
      topLeft = Offset(0f, 0f),
      size = Size(w, h),
      style = Stroke(width = 24f)
    )
    drawRect(
      color = wallHi.copy(alpha = 0.45f),
      topLeft = Offset(12f, 12f),
      size = Size(w - 24f, h - 24f),
      style = Stroke(width = 4f)
    )
  }
}

private fun DrawScope.drawObstacle(obs: Obstacle, gameTime: Float, is3d: Boolean) {
  val baseCenter = Offset(obs.x, obs.y)
  val topCenter = if (is3d) Offset(obs.x, obs.y - 18f) else baseCenter

  if (is3d) {
    drawOval(
      color = Color.Black.copy(alpha = 0.42f),
      topLeft = Offset(obs.x - obs.radius * 1.05f, obs.y - obs.radius * 0.72f + 12f),
      size = Size(obs.radius * 2.1f, obs.radius * 1.6f)
    )
  }

  when (obs.type) {
    "FOUNTAIN" -> {
      if (is3d) {
        drawCircle(color = Color(0xFF075985), radius = obs.radius, center = baseCenter)
      }
      drawCircle(color = Color(0xFF0284C7), radius = obs.radius, center = topCenter)
      drawCircle(color = Color(0xFF38BDF8), radius = obs.radius - 8f, center = topCenter)
      drawCircle(color = Color(0xFF0369A1), radius = obs.radius - 18f, center = topCenter)

      val rippleR = (obs.radius - 22f) * ((gameTime * 0.8f) % 1f)
      drawCircle(
        color = Color.White.copy(alpha = (1f - ((gameTime * 0.8f) % 1f)) * 0.5f),
        radius = rippleR,
        center = topCenter,
        style = Stroke(width = 3f)
      )

      val spoutCenter = if (is3d) Offset(topCenter.x, topCenter.y - 12f) else topCenter
      drawCircle(color = Color(0xFFFDE047), radius = 11f, center = spoutCenter)
      drawCircle(color = Color.White, radius = 5.5f, center = spoutCenter)
    }
    "TREE" -> {
      if (is3d) {
        drawCircle(color = Color(0xFF451A03), radius = obs.radius * 0.92f, center = baseCenter)
        drawCircle(color = Color(0xFF78350F), radius = obs.radius, center = Offset(obs.x, obs.y - 9f))
      } else {
        drawCircle(color = Color(0xFF78350F), radius = obs.radius, center = baseCenter)
      }
      val canopy = if (is3d) Offset(obs.x, obs.y - 24f) else topCenter
      drawCircle(color = Color(0xFFD97706), radius = obs.radius - 8f, center = canopy)
      drawCircle(color = Color(0xFFB45309), radius = obs.radius - 18f, center = canopy)
      drawCircle(color = Color(0xFFFDE68A), radius = 18f, center = if (is3d) Offset(canopy.x, canopy.y - 6f) else canopy)
      drawCircle(color = Color(0xFFD97706), radius = 10f, center = if (is3d) Offset(canopy.x, canopy.y - 6f) else canopy)
    }
    "FLOWER_PATCH" -> {
      if (is3d) {
        drawCircle(color = Color(0xFF14532D), radius = obs.radius, center = baseCenter)
      }
      drawCircle(color = Color(0xFF15803D), radius = obs.radius, center = topCenter)
      drawCircle(color = Color(0xFF22C55E).copy(alpha = 0.6f), radius = obs.radius - 8f, center = topCenter)
      val petalColors = listOf(Color(0xFFF472B6), Color(0xFFFBBF24), Color(0xFF38BDF8), Color(0xFFA855F7))
      for (i in 0 until 5) {
        val angle = (i * 72f + gameTime * 10f) * PI.toFloat() / 180f
        val px = topCenter.x + cos(angle) * (obs.radius * 0.55f)
        val py = topCenter.y + sin(angle) * (obs.radius * 0.55f)
        drawCircle(color = petalColors[i % petalColors.size], radius = 8f, center = Offset(px, py))
      }
    }
    "YARN" -> {
      if (is3d) {
        drawCircle(color = Color(0xFF581C87), radius = obs.radius, center = baseCenter)
      }
      drawCircle(color = Color(0xFF9333EA), radius = obs.radius, center = topCenter)
      drawCircle(color = Color(0xFFA855F7), radius = obs.radius - 6f, center = topCenter)
      drawLine(
        color = Color(0xFFE9D5FF),
        start = Offset(topCenter.x - obs.radius * 0.6f, topCenter.y),
        end = Offset(topCenter.x + obs.radius * 0.6f, topCenter.y),
        strokeWidth = 3f
      )
      drawLine(
        color = Color(0xFFE9D5FF),
        start = Offset(topCenter.x, topCenter.y - obs.radius * 0.6f),
        end = Offset(topCenter.x, topCenter.y + obs.radius * 0.6f),
        strokeWidth = 3f
      )
    }
  }
}

private fun DrawScope.drawPowerUpItem(p: PowerUpItem, gameTime: Float, emojiPaint: Paint, is3d: Boolean) {
  val bob = sin(gameTime * 4f) * 6f
  val lift = if (is3d) -10f else 0f
  val center = Offset(p.position.x, p.position.y + bob + lift)
  val itemColor = p.itemDef.color

  if (is3d) {
    drawOval(
      color = Color.Black.copy(alpha = 0.4f),
      topLeft = Offset(p.position.x - 18f, p.position.y + 8f),
      size = Size(36f, 16f)
    )
  }

  val auraR = 32f + sin(gameTime * 6f) * 4f
  drawCircle(
    color = itemColor.copy(alpha = 0.35f),
    radius = auraR,
    center = center
  )
  drawCircle(
    color = itemColor,
    radius = 22f,
    center = center
  )
  drawCircle(
    color = Color(0xFF0F172A).copy(alpha = 0.75f),
    radius = 17f,
    center = center
  )
  drawContext.canvas.nativeCanvas.drawText(
    p.itemDef.emoji,
    center.x,
    center.y + 8.5f,
    emojiPaint
  )
}

private fun DrawScope.drawCollectibleCat(cat: CollectibleCat, gameTime: Float, is3d: Boolean) {
  val bob = cat.bobOffset
  val lift = if (is3d) -8f else 0f
  val center = Offset(cat.position.x, cat.position.y + bob + lift)

  if (is3d) {
    drawOval(
      color = Color.Black.copy(alpha = 0.38f),
      topLeft = Offset(cat.position.x - 20f, cat.position.y + 10f),
      size = Size(40f, 18f)
    )
  }

  val pulseRadius = 42f + sin(gameTime * 5f + cat.id) * 7f
  drawCircle(
    color = cat.breed.furColor.copy(alpha = 0.32f),
    radius = pulseRadius,
    center = center
  )
  drawCircle(
    color = cat.breed.furColor.copy(alpha = 0.18f),
    radius = pulseRadius + 14f,
    center = center,
    style = Stroke(width = 3f)
  )

  drawCircle(color = cat.breed.furColor, radius = 22f, center = center)

  if (is3d) {
    drawCircle(
      color = Color.White.copy(alpha = 0.25f),
      radius = 8f,
      center = Offset(center.x - 6f, center.y - 9f)
    )
  }

  val earPath = Path().apply {
    moveTo(center.x - 14f, center.y - 12f)
    lineTo(center.x - 18f, center.y - 30f)
    lineTo(center.x - 4f, center.y - 20f)
    close()
    moveTo(center.x + 4f, center.y - 20f)
    lineTo(center.x + 18f, center.y - 30f)
    lineTo(center.x + 14f, center.y - 12f)
    close()
  }
  drawPath(earPath, color = cat.breed.earColor)

  val blink = sin(gameTime * 2f + cat.id) > 0.95f
  if (blink) {
    drawLine(Color(0xFF1E293B), Offset(center.x - 9f, center.y - 3f), Offset(center.x - 3f, center.y - 3f), 2.5f)
    drawLine(Color(0xFF1E293B), Offset(center.x + 3f, center.y - 3f), Offset(center.x + 9f, center.y - 3f), 2.5f)
  } else {
    drawCircle(color = cat.breed.eyeColor, radius = 4f, center = Offset(center.x - 6f, center.y - 3f))
    drawCircle(color = cat.breed.eyeColor, radius = 4f, center = Offset(center.x + 6f, center.y - 3f))
    drawCircle(color = Color(0xFF0F172A), radius = 2f, center = Offset(center.x - 6f, center.y - 3f))
    drawCircle(color = Color(0xFF0F172A), radius = 2f, center = Offset(center.x + 6f, center.y - 3f))
  }

  drawCircle(color = Color(0xFFFB7185), radius = 2.5f, center = Offset(center.x, center.y + 4f))

  drawLine(Color(0xFF334155), Offset(center.x - 6f, center.y + 4f), Offset(center.x - 18f, center.y + 2f), 1.5f)
  drawLine(Color(0xFF334155), Offset(center.x - 6f, center.y + 6f), Offset(center.x - 18f, center.y + 8f), 1.5f)
  drawLine(Color(0xFF334155), Offset(center.x + 6f, center.y + 4f), Offset(center.x + 18f, center.y + 2f), 1.5f)
  drawLine(Color(0xFF334155), Offset(center.x + 6f, center.y + 6f), Offset(center.x + 18f, center.y + 8f), 1.5f)

  val heartY = center.y - 36f + sin(gameTime * 6f + cat.id) * 4f
  drawCircle(color = Color(0xFFEC4899), radius = 5f, center = Offset(center.x, heartY))
}

private fun DrawScope.drawFollowerCat(follower: FollowerCat, is3d: Boolean) {
  val lift = if (is3d) -6f else 0f
  val center = Offset(follower.position.x, follower.position.y + follower.hopOffset + lift)

  if (is3d) {
    drawOval(
      color = Color.Black.copy(alpha = 0.35f),
      topLeft = Offset(follower.position.x - 15f, follower.position.y + 8f),
      size = Size(30f, 14f)
    )
  }

  withTransform({
    rotate(degrees = follower.angle, pivot = center)
  }) {
    val tailPath = Path().apply {
      moveTo(center.x - 14f, center.y)
      quadraticTo(center.x - 24f, center.y - 12f, center.x - 22f, center.y - 20f)
    }
    drawPath(tailPath, color = follower.breed.furColor, style = Stroke(width = 4f, cap = StrokeCap.Round))

    drawCircle(color = follower.breed.furColor, radius = 17f, center = center)

    val earPath = Path().apply {
      moveTo(center.x + 2f, center.y - 10f)
      lineTo(center.x + 10f, center.y - 22f)
      lineTo(center.x + 14f, center.y - 8f)
      close()
      moveTo(center.x + 2f, center.y + 10f)
      lineTo(center.x + 10f, center.y + 22f)
      lineTo(center.x + 14f, center.y + 8f)
      close()
    }
    drawPath(earPath, color = follower.breed.earColor)

    drawLine(Color(0xFF1E293B), Offset(center.x + 8f, center.y - 5f), Offset(center.x + 12f, center.y - 5f), 2f)
    drawLine(Color(0xFF1E293B), Offset(center.x + 8f, center.y + 5f), Offset(center.x + 12f, center.y + 5f), 2f)
  }
}

private fun DrawScope.drawHeroCat(
  pos: Vector2D,
  angle: Float,
  skin: HeroSkinDef,
  isInvincible: Boolean,
  isInvisible: Boolean,
  hasShield: Boolean,
  hasCatnip: Boolean,
  hasMagnet: Boolean,
  hasDoubleCat: Boolean,
  isGodMode: Boolean,
  is3d: Boolean,
  gameTime: Float
) {
  val lift = if (is3d) -10f else 0f
  val center = Offset(pos.x, pos.y + lift)

  if (is3d) {
    drawOval(
      color = Color.Black.copy(alpha = 0.45f),
      topLeft = Offset(pos.x - 24f, pos.y + 10f),
      size = Size(48f, 22f)
    )
  }

  if (isInvincible && !isGodMode && !isInvisible && (gameTime * 15f).toInt() % 2 == 0) {
    return
  }

  val bodyAlpha = if (isInvisible) 0.38f else 1f

  // Skin special aura
  if (skin.hasAura && !isGodMode) {
    val skinAuraR = 44f + sin(gameTime * 7f) * 5f
    drawCircle(
      color = skin.trailColor.copy(alpha = 0.22f),
      radius = skinAuraR,
      center = center
    )
  }

  // Divine God Mode golden radiant aura
  if (isGodMode) {
    val haloR = 48f + sin(gameTime * 8f) * 6f
    drawCircle(color = Color(0xFFFDE047).copy(alpha = 0.30f), radius = haloR, center = center)
    drawCircle(color = Color(0xFFFACC15), radius = haloR, center = center, style = Stroke(width = 3.5f))
  }

  if (hasDoubleCat) {
    val dblR = 54f + cos(gameTime * 7f) * 5f
    drawCircle(color = Color(0xFFFDE047).copy(alpha = 0.35f), radius = dblR, center = center, style = Stroke(width = 2.5f))
  }

  if (hasMagnet) {
    val magR = 60f + sin(gameTime * 10f) * 8f
    drawCircle(color = Color(0xFFEC4899).copy(alpha = 0.25f), radius = magR, center = center, style = Stroke(width = 3f))
  }

  if (hasShield) {
    drawCircle(color = Color(0xFF38BDF8).copy(alpha = 0.35f), radius = 42f, center = center)
    drawCircle(color = Color(0xFFE0F2FE), radius = 42f, center = center, style = Stroke(width = 3f))
  }

  if (hasCatnip) {
    drawCircle(color = Color(0xFF10B981).copy(alpha = 0.25f), radius = 38f, center = center)
  }

  withTransform({
    rotate(degrees = angle, pivot = center)
  }) {
    val capePath = Path().apply {
      moveTo(center.x - 8f, center.y - 12f)
      lineTo(center.x - 30f + sin(gameTime * 12f) * 4f, center.y - 18f)
      lineTo(center.x - 30f + sin(gameTime * 12f) * 4f, center.y + 18f)
      lineTo(center.x - 8f, center.y + 12f)
      close()
    }
    val effectiveCape = if (isGodMode) Color(0xFFFACC15) else skin.capeColor
    drawPath(capePath, color = effectiveCape.copy(alpha = bodyAlpha))
    drawPath(capePath, color = skin.capeBorderColor.copy(alpha = bodyAlpha), style = Stroke(width = 2.5f))

    val tailPath = Path().apply {
      moveTo(center.x - 16f, center.y)
      quadraticTo(center.x - 32f, center.y - 14f + sin(gameTime * 8f) * 6f, center.x - 28f, center.y - 24f)
    }
    drawPath(tailPath, color = skin.stripeColor.copy(alpha = bodyAlpha), style = Stroke(width = 5f, cap = StrokeCap.Round))

    drawCircle(color = skin.furColor.copy(alpha = bodyAlpha), radius = 24f, center = center)

    if (is3d) {
      drawCircle(color = Color.White.copy(alpha = 0.22f * bodyAlpha), radius = 10f, center = Offset(center.x - 4f, center.y - 7f))
    }

    drawLine(skin.stripeColor.copy(alpha = bodyAlpha), Offset(center.x - 6f, center.y - 12f), Offset(center.x - 2f, center.y - 6f), 3f)
    drawLine(skin.stripeColor.copy(alpha = bodyAlpha), Offset(center.x - 6f, center.y + 12f), Offset(center.x - 2f, center.y + 6f), 3f)

    val earPath = Path().apply {
      moveTo(center.x + 4f, center.y - 14f)
      lineTo(center.x + 18f, center.y - 30f)
      lineTo(center.x + 22f, center.y - 12f)
      close()
      moveTo(center.x + 4f, center.y + 14f)
      lineTo(center.x + 18f, center.y + 30f)
      lineTo(center.x + 22f, center.y + 12f)
      close()
    }
    drawPath(earPath, color = skin.earColor.copy(alpha = bodyAlpha))

    val innerEarPath = Path().apply {
      moveTo(center.x + 8f, center.y - 15f)
      lineTo(center.x + 16f, center.y - 26f)
      lineTo(center.x + 19f, center.y - 13f)
      close()
      moveTo(center.x + 8f, center.y + 15f)
      lineTo(center.x + 16f, center.y + 26f)
      lineTo(center.x + 19f, center.y + 13f)
      close()
    }
    drawPath(innerEarPath, color = skin.innerEarColor.copy(alpha = bodyAlpha))

    drawCircle(color = skin.eyeColor.copy(alpha = bodyAlpha), radius = 5f, center = Offset(center.x + 12f, center.y - 7f))
    drawCircle(color = skin.eyeColor.copy(alpha = bodyAlpha), radius = 5f, center = Offset(center.x + 12f, center.y + 7f))
    drawCircle(color = Color(0xFF0F172A).copy(alpha = bodyAlpha), radius = 2.5f, center = Offset(center.x + 14f, center.y - 7f))
    drawCircle(color = Color(0xFF0F172A).copy(alpha = bodyAlpha), radius = 2.5f, center = Offset(center.x + 14f, center.y + 7f))
    drawCircle(color = Color.White.copy(alpha = bodyAlpha), radius = 1.2f, center = Offset(center.x + 13f, center.y - 8f))
    drawCircle(color = Color.White.copy(alpha = bodyAlpha), radius = 1.2f, center = Offset(center.x + 13f, center.y + 6f))

    drawCircle(color = Color(0xFFFB7185).copy(alpha = bodyAlpha), radius = 3f, center = Offset(center.x + 20f, center.y))

    drawLine(skin.stripeColor.copy(alpha = bodyAlpha), Offset(center.x + 18f, center.y - 5f), Offset(center.x + 30f, center.y - 16f), 2f)
    drawLine(skin.stripeColor.copy(alpha = bodyAlpha), Offset(center.x + 18f, center.y + 5f), Offset(center.x + 30f, center.y + 16f), 2f)

    val crownPath = Path().apply {
      moveTo(center.x - 2f, center.y - 10f)
      lineTo(center.x + 4f, center.y - 16f)
      lineTo(center.x, center.y - 7f)
      lineTo(center.x + 6f, center.y)
      lineTo(center.x, center.y + 7f)
      lineTo(center.x + 4f, center.y + 16f)
      lineTo(center.x - 2f, center.y + 10f)
      close()
    }
    drawPath(crownPath, color = Color(0xFFFDE047).copy(alpha = bodyAlpha))
  }
}

private fun DrawScope.drawGhost(ghost: Ghost, gameTime: Float, is3d: Boolean) {
  val lift = if (is3d) -16f else 0f
  val center = Offset(ghost.position.x, ghost.position.y + ghost.floatOffset + lift)
  val isFrozen = ghost.freezeTimerSec > 0f
  val isStunned = ghost.stunTimerSec > 0f

  if (is3d) {
    drawOval(
      color = Color(0xFF3B0764).copy(alpha = 0.48f),
      topLeft = Offset(ghost.position.x - 22f, ghost.position.y + 18f),
      size = Size(44f, 18f)
    )
  }

  val normalColor = when (ghost.id) {
    1 -> Color(0xFFF472B6)
    2 -> Color(0xFFFB7185)
    else -> Color(0xFFC084FC)
  }
  val normalGlow = when (ghost.id) {
    1 -> Color(0xFFDB2777)
    2 -> Color(0xFFE11D48)
    else -> Color(0xFF7E22CE)
  }

  val baseColor = when {
    isFrozen -> Color(0xFF67E8F9)
    ghost.isFleeing -> Color(0xFF38BDF8)
    else -> normalColor
  }
  val glowColor = when {
    isFrozen -> Color(0xFF06B6D4)
    ghost.isFleeing -> Color(0xFF0284C7)
    else -> normalGlow
  }

  val glowR = 45f + sin(gameTime * 6f) * 6f
  drawCircle(color = glowColor.copy(alpha = 0.35f), radius = glowR, center = center)

  val ghostPath = Path().apply {
    moveTo(center.x - 22f, center.y)
    cubicTo(center.x - 22f, center.y - 32f, center.x + 22f, center.y - 32f, center.x + 22f, center.y)
    lineTo(center.x + 22f, center.y + 24f)
    val wave = if (isFrozen) 0f else sin(gameTime * 8f) * 4f
    cubicTo(center.x + 15f, center.y + 32f + wave, center.x + 8f, center.y + 18f, center.x, center.y + 28f - wave)
    cubicTo(center.x - 8f, center.y + 18f, center.x - 15f, center.y + 32f + wave, center.x - 22f, center.y + 24f)
    close()
  }

  drawPath(ghostPath, color = baseColor.copy(alpha = 0.88f))
  drawPath(ghostPath, color = Color.White.copy(alpha = 0.55f), style = Stroke(width = 2f))

  if (isFrozen) {
    drawRect(color = Color(0xFFA5F3FC).copy(alpha = 0.38f), topLeft = Offset(center.x - 26f, center.y - 30f), size = Size(52f, 62f))
    drawRect(color = Color.White.copy(alpha = 0.7f), topLeft = Offset(center.x - 26f, center.y - 30f), size = Size(52f, 62f), style = Stroke(width = 2.5f))
  }

  if (ghost.isFleeing || isStunned) {
    drawCircle(Color(0xFF0F172A), 4f, Offset(center.x - 8f, center.y - 8f))
    drawCircle(Color(0xFF0F172A), 4f, Offset(center.x + 8f, center.y - 8f))
    drawCircle(Color.White, 2f, Offset(center.x - 8f, center.y - 8f))
    drawCircle(Color.White, 2f, Offset(center.x + 8f, center.y - 8f))
  } else {
    drawCircle(Color(0xFF581C87), 5.5f, Offset(center.x - 8f, center.y - 8f))
    drawCircle(Color(0xFF581C87), 5.5f, Offset(center.x + 8f, center.y - 8f))
    drawCircle(Color(0xFFFDE047), 2.5f, Offset(center.x - 8f, center.y - 8f))
    drawCircle(Color(0xFFFDE047), 2.5f, Offset(center.x + 8f, center.y - 8f))
  }

  val mouthY = center.y + 4f
  if (ghost.isFleeing || isStunned) {
    drawCircle(Color(0xFF0F172A), 4f, Offset(center.x, mouthY))
  } else {
    val smilePath = Path().apply {
      moveTo(center.x - 6f, mouthY)
      quadraticTo(center.x, mouthY + 7f, center.x + 6f, mouthY)
    }
    drawPath(smilePath, color = Color(0xFF3B0764), style = Stroke(width = 2.5f, cap = StrokeCap.Round))
  }
}

private fun DrawScope.drawCatTrackerCompass(
  heroPos: Vector2D,
  catPos: Vector2D,
  screenSize: Size,
  isGhostHunt: Boolean = false
) {
  val diff = catPos - heroPos
  val dist = diff.length()
  if (dist < 200f) return

  val angle = atan2(diff.y, diff.x)
  val margin = 55f
  val screenCenter = Offset(screenSize.width / 2f, screenSize.height / 2f)

  val edgeX = (screenCenter.x + cos(angle) * (screenSize.width / 2f - margin)).coerceIn(margin, screenSize.width - margin)
  val edgeY = (screenCenter.y + sin(angle) * (screenSize.height / 2f - margin * 1.5f)).coerceIn(margin, screenSize.height - margin * 1.5f)
  val pointerCenter = Offset(edgeX, edgeY)

  val badgeColor = if (isGhostHunt) Color(0xFF9333EA) else Color(0xFF10B981)
  drawCircle(color = badgeColor.copy(alpha = 0.85f), radius = 22f, center = pointerCenter)
  drawCircle(color = if (isGhostHunt) Color(0xFFFDE047) else Color.White, radius = 22f, center = pointerCenter, style = Stroke(width = 3f))

  withTransform({
    rotate(degrees = angle * 180f / PI.toFloat(), pivot = pointerCenter)
  }) {
    val arrowPath = Path().apply {
      moveTo(pointerCenter.x + 12f, pointerCenter.y)
      lineTo(pointerCenter.x - 4f, pointerCenter.y - 8f)
      lineTo(pointerCenter.x - 4f, pointerCenter.y + 8f)
      close()
    }
    drawPath(arrowPath, color = Color.White)
  }
}
