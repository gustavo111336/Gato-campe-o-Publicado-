package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ControlType
import com.example.model.GameStatus
import com.example.model.Vector2D
import kotlin.math.*

@Composable
fun ControlsOverlay(
  uiState: GameUiState,
  onDirectionChanged: (Vector2D) -> Unit,
  onDash: () -> Unit,
  onSonicWave: () -> Unit,
  onPauseToggle: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenGallery: () -> Unit,
  onOpenAchievements: () -> Unit,
  onOpenAi: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(modifier = modifier.fillMaxSize().padding(14.dp)) {
    // TOP HUD BAR
    TopHudBar(
      uiState = uiState,
      onPauseToggle = onPauseToggle,
      onOpenSettings = onOpenSettings,
      onOpenGallery = onOpenGallery,
      onOpenAchievements = onOpenAchievements,
      onOpenAi = onOpenAi,
      modifier = Modifier.align(Alignment.TopCenter)
    )

    // ACTIVE BUFFS & COMBO PILLS
    ActiveBuffsRow(
      uiState = uiState,
      modifier = Modifier.align(Alignment.TopCenter).padding(top = 114.dp)
    )

    // BOTTOM CONTROLS (Only visible during gameplay)
    if (uiState.gameStatus == GameStatus.PLAYING || uiState.gameStatus == GameStatus.PAUSED) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.BottomCenter)
          .padding(bottom = 12.dp)
      ) {
        // Left Side: Joystick or D-Pad
        when (uiState.controlType) {
          ControlType.JOYSTICK -> {
            VirtualJoystick(
              onDirectionChanged = onDirectionChanged,
              modifier = Modifier.align(Alignment.BottomStart)
            )
          }
          ControlType.DPAD -> {
            VirtualDPad(
              onDirectionChanged = onDirectionChanged,
              modifier = Modifier.align(Alignment.BottomStart)
            )
          }
          ControlType.TOUCH_DIRECT -> {
            Card(
              colors = CardDefaults.cardColors(containerColor = Color(0x991E1B4B)),
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
            ) {
              Text(
                text = "Arraste na tela para guiar o gatinho 🐾",
                color = Color(0xFFE2E8F0),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
              )
            }
          }
        }

        // Right Side: Action Skills (Sonic Wave + Turbo Dash)
        Row(
          modifier = Modifier.align(Alignment.BottomEnd),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.Bottom
        ) {
          if (uiState.showSonicButton) {
            SonicButton(
              cooldownRatio = uiState.sonicCooldownRatio,
              isOverdrive = uiState.sonicOverdriveActive || uiState.currentMode.infiniteSonic,
              onTrigger = onSonicWave
            )
          }

          DashButton(
            cooldownRatio = uiState.dashCooldownRatio,
            isOverdrive = uiState.turboOverdriveActive || uiState.currentMode.infiniteTurbo,
            onDash = onDash
          )
        }
      }
    }
  }
}

@Composable
fun TopHudBar(
  uiState: GameUiState,
  onPauseToggle: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenGallery: () -> Unit,
  onOpenAchievements: () -> Unit,
  onOpenAi: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Card(
      colors = CardDefaults.cardColors(containerColor = Color(0xCC0F172A)),
      shape = RoundedCornerShape(20.dp),
      modifier = Modifier
        .fillMaxWidth()
        .shadow(8.dp, RoundedCornerShape(20.dp))
        .border(
          1.dp,
          if (uiState.isGodModeEnabled) Color(0xFFFACC15) else Color(0xFF334155),
          RoundedCornerShape(20.dp)
        )
    ) {
      Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
        // Row 1: Lives (or God Mode) + Target Count + Top Action Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Lives, Elite Hunter Badge, or God Mode Indicator
          if (uiState.isPlayerGhost) {
            Surface(
              color = Color(0xFF581C87),
              shape = RoundedCornerShape(10.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE047))
            ) {
              Text(
                text = "👻 FANTASMA",
                color = Color(0xFFFDE047),
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          } else if (uiState.isEffectiveGodMode) {
            Surface(
              color = Color(0xFF78350F),
              shape = RoundedCornerShape(10.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE047))
            ) {
              Text(
                text = if (uiState.starInvincibleActive && !uiState.isGodModeEnabled) "🌟 INVENCÍVEL" else "👑 DEUS",
                color = Color(0xFFFDE047),
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
              for (i in 1..uiState.maxLives) {
                val isFilled = i <= uiState.lives
                Text(
                  text = if (isFilled) "❤️" else "🖤",
                  fontSize = 16.sp,
                  modifier = Modifier.padding(end = 2.dp)
                )
              }
            }
          }

          // Goal Counter Badge
          Surface(
            color = when {
              uiState.isPlayerGhost -> Color(0xFF7E22CE)
              uiState.isEndlessMode -> Color(0xFF6D28D9)
              uiState.catsCollected >= uiState.goalCats -> Color(0xFFF59E0B)
              else -> Color(0xFF1E293B)
            },
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569))
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              if (uiState.isPlayerGhost) {
                Text(
                  text = "🎯 ${uiState.catsCollected} / ${uiState.goalCats}",
                  color = Color.White,
                  fontWeight = FontWeight.Black,
                  fontSize = 14.sp
                )
              } else if (uiState.isEndlessMode) {
                Text(
                  text = "🐱 ${uiState.catsCollected} ♾️",
                  color = Color.White,
                  fontWeight = FontWeight.Black,
                  fontSize = 14.sp
                )
              } else {
                Text(
                  text = "🐱 ${uiState.catsCollected} / ${uiState.goalCats}",
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp
                )
              }
            }
          }

          // Action Buttons: AI Coach, Achievements, Gallery, Settings, Pause
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = onOpenAi,
              modifier = Modifier.size(32.dp).testTag("hud_ai_button")
            ) {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "IA Guia Felina",
                tint = Color(0xFF22D3EE),
                modifier = Modifier.size(18.dp)
              )
            }
            IconButton(
              onClick = onOpenAchievements,
              modifier = Modifier.size(32.dp).testTag("hud_achievements_button")
            ) {
              Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "Conquistas",
                tint = Color(0xFFFDE047),
                modifier = Modifier.size(18.dp)
              )
            }
            IconButton(
              onClick = onOpenGallery,
              modifier = Modifier.size(32.dp).testTag("gallery_button")
            ) {
              Icon(
                imageVector = Icons.Default.CollectionsBookmark,
                contentDescription = "Galeria dos Felinos",
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(18.dp)
              )
            }
            IconButton(
              onClick = onOpenSettings,
              modifier = Modifier.size(32.dp).testTag("settings_button")
            ) {
              Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Configurações",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp)
              )
            }
            IconButton(
              onClick = onPauseToggle,
              modifier = Modifier.size(32.dp).testTag("pause_button")
            ) {
              Icon(
                imageVector = if (uiState.gameStatus == GameStatus.PAUSED) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = "Pausar ou Continuar",
                tint = Color(0xFF4ADE80),
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Row 2: Progress bar
        if (uiState.isEndlessMode) {
          LinearProgressIndicator(
            progress = { 1f },
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp)),
            color = Color(0xFFA855F7),
            trackColor = Color(0xFF1E293B)
          )
        } else {
          val progress = (uiState.catsCollected.toFloat() / uiState.goalCats.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
          LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp)),
            color = Color(0xFF10B981),
            trackColor = Color(0xFF1E293B)
          )
        }

        // Row 3: Mode name + Ghost Radar + Timer / Time Limit
        Spacer(modifier = Modifier.height(5.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          val ghostText = when {
            uiState.isPlayerGhost && uiState.activeRoomCode != null -> "👻 Caçando players! Acertos: ${uiState.catsCollected}/${uiState.goalCats}"
            uiState.isPlayerGhost -> "🏃 Corredor: ${uiState.aiRunnerLives}❤️ • ${uiState.aiRunnerCats}/10 gatos"
            uiState.invisibilityActive -> "🫥 Invisível! Fantasmas perdidos"
            uiState.isEffectiveGodMode -> "👑 Imortalidade Ativa!"
            uiState.freezeActive -> "❄️ Fantasmas Congelados!"
            uiState.slowMoActive -> "⏳ Fantasmas em Câmera Lenta!"
            uiState.isGhostFleeing -> "👻 Fantasmas FUGINDO de você!"
            uiState.isGhostNear -> "⚠️ PERIGO! Fantasma muito perto!"
            else -> "${uiState.currentMode.emoji} ${uiState.currentMode.name} • 👻 ${uiState.activeGhostsCount}"
          }
          val ghostColor = when {
            uiState.isPlayerGhost -> Color(0xFFF0ABFC)
            uiState.invisibilityActive -> Color(0xFFCBD5E1)
            uiState.isEffectiveGodMode -> Color(0xFFFDE047)
            uiState.freezeActive -> Color(0xFF67E8F9)
            uiState.slowMoActive -> Color(0xFFA78BFA)
            uiState.isGhostFleeing -> Color(0xFF38BDF8)
            uiState.isGhostNear -> Color(0xFFEF4444)
            else -> Color(0xFFC084FC)
          }

          Text(
            text = ghostText,
            color = ghostColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1
          )

          if (uiState.currentMode.timeLimitSec > 0f) {
            val secLeft = ceil(uiState.timeLimitRemaining).toInt().coerceAtLeast(0)
            Surface(
              color = if (secLeft <= 10) Color(0xFF991B1B) else Color(0xFF1E293B),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text(
                text = "⏳ ${secLeft}s",
                color = if (secLeft <= 10) Color(0xFFFCA5A5) else Color(0xFFFDE047),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          } else {
            val minutes = (uiState.gameTimeSeconds / 60).toInt()
            val seconds = (uiState.gameTimeSeconds % 60).toInt()
            Text(
              text = String.format("⏱️ %02d:%02d", minutes, seconds),
              color = Color(0xFF94A3B8),
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }

        // Row 4: Placar (Scoreboard), Match Level, Rewards Blocked indicator, Multiplayer Partner
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "⭐ Nv.${uiState.matchLevel} • 🏆 ${uiState.currentScore} pts (Rec: ${uiState.bestHighScore})",
            color = Color(0xFFFDE047),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
          )
          if (uiState.isBotsMode) {
            Text(
              text = "🤖 #${uiState.playerBotRank}/${uiState.botCount + 1} • Líder: ${uiState.leadingBotName} ${uiState.leadingBotCats}/50",
              color = Color(0xFF67E8F9),
              fontSize = 10.sp,
              fontWeight = FontWeight.Black,
              maxLines = 1
            )
          } else if (uiState.isRewardsBlocked) {
            Text(
              text = "🚫 Sem Prêmios",
              color = Color(0xFFFCA5A5),
              fontSize = 10.sp,
              fontWeight = FontWeight.Black
            )
          } else if (uiState.reviveCount > 0) {
            Text(
              text = "💖 ${uiState.reviveCount} Revive(s)",
              color = Color(0xFFF9A8D4),
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        if (uiState.activeRoomCode != null) {
          Spacer(modifier = Modifier.height(4.dp))
          Surface(
            color = Color(0xFF0C4A6E),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            val roleText = if (uiState.multiplayerRole == "ghost") "👻 Fantasma" else "🐱 Gato"
            val partnerLabel = uiState.partnerUsername?.let { "Líder @$it: ${uiState.partnerScore}" } ?: "Aguardando..."
            Text(
              text = "👥 Sala ${uiState.activeRoomCode} (${uiState.multiplayerPlayerCount}/${uiState.multiplayerMaxPlayers}) • $roleText • $partnerLabel",
              color = Color(0xFFE0F2FE),
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
fun ActiveBuffsRow(uiState: GameUiState, modifier: Modifier = Modifier) {
  LazyRow(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
  ) {
    if (uiState.comboCount >= 2) {
      item { BuffBadge(label = "🔥 Combo x${uiState.comboCount}!", color = Color(0xFFEA580C)) }
    }
    if (uiState.starInvincibleActive) {
      item { BuffBadge(label = "🌟 Invencível!", color = Color(0xFFEAB308)) }
    }
    if (uiState.doubleCatActive) {
      item { BuffBadge(label = "🏅 Gatos x2!", color = Color(0xFFD97706)) }
    }
    if (uiState.invisibilityActive) {
      item { BuffBadge(label = "🫥 Invisível!", color = Color(0xFF475569)) }
    }
    if (uiState.catnipActive) {
      item { BuffBadge(label = "🌿 Velocidade!", color = Color(0xFF10B981)) }
    }
    if (uiState.goldenFishActive) {
      item { BuffBadge(label = "🐟 Pânico!", color = Color(0xFFF59E0B)) }
    }
    if (uiState.giantMagnetActive) {
      item { BuffBadge(label = "🪐 Super Ímã!", color = Color(0xFFC026D3)) }
    } else if (uiState.magnetActive) {
      item { BuffBadge(label = "🧲 Ímã!", color = Color(0xFFEC4899)) }
    }
    if (uiState.freezeActive) {
      item { BuffBadge(label = "❄️ Gelo!", color = Color(0xFF06B6D4)) }
    }
    if (uiState.slowMoActive) {
      item { BuffBadge(label = "⏳ Lento!", color = Color(0xFF6366F1)) }
    }
    if (uiState.turboOverdriveActive) {
      item { BuffBadge(label = "🔋 Turbo∞!", color = Color(0xFFCA8A04)) }
    }
    if (uiState.sonicOverdriveActive) {
      item { BuffBadge(label = "🛎️ Onda∞!", color = Color(0xFF9333EA)) }
    }
    if (uiState.shieldActive) {
      item { BuffBadge(label = "🥛 Escudo!", color = Color(0xFF38BDF8)) }
    }
  }
}

@Composable
fun BuffBadge(label: String, color: Color) {
  Surface(
    color = color.copy(alpha = 0.92f),
    shape = RoundedCornerShape(12.dp),
    shadowElevation = 4.dp
  ) {
    Text(
      text = label,
      color = Color.White,
      fontWeight = FontWeight.Bold,
      fontSize = 10.sp,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
  }
}

@Composable
fun VirtualJoystick(
  onDirectionChanged: (Vector2D) -> Unit,
  modifier: Modifier = Modifier
) {
  val baseRadius = 70.dp
  val knobRadius = 28.dp
  val density = LocalDensity.current
  val maxDistancePx = with(density) { (baseRadius - knobRadius).toPx() }

  var knobOffset by remember { mutableStateOf(Offset.Zero) }

  Box(
    modifier = modifier
      .size(baseRadius * 2)
      .shadow(8.dp, CircleShape)
      .background(Color(0x770F172A), CircleShape)
      .border(2.dp, Color(0x6638BDF8), CircleShape)
      .pointerInput(Unit) {
        detectDragGestures(
          onDragStart = { offset ->
            val center = Offset(size.width / 2f, size.height / 2f)
            val diff = offset - center
            val dist = diff.getDistance()
            val clampedDist = min(dist, maxDistancePx)
            val dir = if (dist > 0.001f) diff / dist else Offset.Zero
            knobOffset = dir * clampedDist
            onDirectionChanged(Vector2D(dir.x, dir.y) * (clampedDist / maxDistancePx))
          },
          onDrag = { change, _ ->
            val center = Offset(size.width / 2f, size.height / 2f)
            val diff = change.position - center
            val dist = diff.getDistance()
            val clampedDist = min(dist, maxDistancePx)
            val dir = if (dist > 0.001f) diff / dist else Offset.Zero
            knobOffset = dir * clampedDist
            onDirectionChanged(Vector2D(dir.x, dir.y) * (clampedDist / maxDistancePx))
          },
          onDragEnd = {
            knobOffset = Offset.Zero
            onDirectionChanged(Vector2D(0f, 0f))
          },
          onDragCancel = {
            knobOffset = Offset.Zero
            onDirectionChanged(Vector2D(0f, 0f))
          }
        )
      }
      .testTag("virtual_joystick"),
    contentAlignment = Alignment.Center
  ) {
    Box(
      modifier = Modifier
        .size(baseRadius * 1.2f)
        .border(1.dp, Color(0x22FFFFFF), CircleShape)
    )

    Box(
      modifier = Modifier
        .offset { IntOffset(knobOffset.x.roundToInt(), knobOffset.y.roundToInt()) }
        .size(knobRadius * 2)
        .shadow(6.dp, CircleShape)
        .background(
          brush = Brush.radialGradient(
            colors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
          ),
          shape = CircleShape
        )
        .border(2.dp, Color.White, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Text("🐾", fontSize = 16.sp)
    }
  }
}

@Composable
fun VirtualDPad(
  onDirectionChanged: (Vector2D) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .size(144.dp)
      .background(Color(0x660F172A), RoundedCornerShape(16.dp))
      .padding(4.dp)
      .testTag("virtual_dpad"),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    DPadButton(
      icon = Icons.Default.KeyboardArrowUp,
      onPress = { onDirectionChanged(Vector2D(0f, -1f)) },
      onRelease = { onDirectionChanged(Vector2D(0f, 0f)) }
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      DPadButton(
        icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
        onPress = { onDirectionChanged(Vector2D(-1f, 0f)) },
        onRelease = { onDirectionChanged(Vector2D(0f, 0f)) }
      )
      Text("🐾", fontSize = 16.sp, modifier = Modifier.align(Alignment.CenterVertically))
      DPadButton(
        icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        onPress = { onDirectionChanged(Vector2D(1f, 0f)) },
        onRelease = { onDirectionChanged(Vector2D(0f, 0f)) }
      )
    }

    DPadButton(
      icon = Icons.Default.KeyboardArrowDown,
      onPress = { onDirectionChanged(Vector2D(0f, 1f)) },
      onRelease = { onDirectionChanged(Vector2D(0f, 0f)) }
    )
  }
}

@Composable
fun DPadButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onPress: () -> Unit,
  onRelease: () -> Unit
) {
  Box(
    modifier = Modifier
      .size(44.dp)
      .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
      .border(1.dp, Color(0xFF475569), RoundedCornerShape(8.dp))
      .pointerInput(Unit) {
        detectTapGestures(
          onPress = {
            onPress()
            tryAwaitRelease()
            onRelease()
          }
        )
      },
    contentAlignment = Alignment.Center
  ) {
    Icon(imageVector = icon, contentDescription = null, tint = Color.White)
  }
}

@Composable
fun DashButton(
  cooldownRatio: Float,
  isOverdrive: Boolean = false,
  onDash: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isReady = cooldownRatio <= 0.01f || isOverdrive
  val buttonColor = when {
    isOverdrive -> Color(0xFFEAB308)
    isReady -> Color(0xFFF59E0B)
    else -> Color(0xFF475569)
  }

  Box(
    modifier = modifier
      .size(68.dp)
      .shadow(8.dp, CircleShape)
      .background(buttonColor, CircleShape)
      .border(2.dp, if (isReady) Color(0xFFFDE047) else Color(0xFF64748B), CircleShape)
      .pointerInput(isReady) {
        detectTapGestures(
          onTap = {
            if (isReady) onDash()
          }
        )
      }
      .testTag("dash_button"),
    contentAlignment = Alignment.Center
  ) {
    if (!isReady) {
      CircularProgressIndicator(
        progress = { cooldownRatio },
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFFDE047),
        strokeWidth = 3.dp,
        trackColor = Color.Transparent
      )
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text("⚡", fontSize = 22.sp)
      Text(
        text = when {
          isOverdrive -> "LIVRE!"
          isReady -> "TURBO"
          else -> String.format("%.1fs", cooldownRatio * 3.5f)
        },
        fontSize = 9.sp,
        fontWeight = FontWeight.Black,
        color = Color.White
      )
    }
  }
}

@Composable
fun SonicButton(
  cooldownRatio: Float,
  isOverdrive: Boolean = false,
  onTrigger: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isReady = cooldownRatio <= 0.01f || isOverdrive
  val buttonColor = when {
    isOverdrive -> Color(0xFFA855F7)
    isReady -> Color(0xFF9333EA)
    else -> Color(0xFF475569)
  }

  Box(
    modifier = modifier
      .size(58.dp)
      .shadow(8.dp, CircleShape)
      .background(buttonColor, CircleShape)
      .border(2.dp, if (isReady) Color(0xFFE879F9) else Color(0xFF64748B), CircleShape)
      .pointerInput(isReady) {
        detectTapGestures(
          onTap = {
            if (isReady) onTrigger()
          }
        )
      }
      .testTag("sonic_button"),
    contentAlignment = Alignment.Center
  ) {
    if (!isReady) {
      CircularProgressIndicator(
        progress = { cooldownRatio },
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFE879F9),
        strokeWidth = 3.dp,
        trackColor = Color.Transparent
      )
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text("🔔", fontSize = 18.sp)
      Text(
        text = when {
          isOverdrive -> "LIVRE!"
          isReady -> "ONDA"
          else -> String.format("%.1fs", cooldownRatio * 7.5f)
        },
        fontSize = 8.sp,
        fontWeight = FontWeight.Black,
        color = Color.White
      )
    }
  }
}
