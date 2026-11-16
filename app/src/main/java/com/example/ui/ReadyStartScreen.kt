package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import com.example.BuildConfig
import com.example.model.ALL_40_ACHIEVEMENTS
import com.example.model.ALL_40_ITEMS
import com.example.model.ALL_40_MODES
import com.example.model.ALL_HERO_SKINS
import com.example.model.Difficulty

@Composable
fun ReadyStartScreen(
  uiState: GameUiState,
  onStartNormal: () -> Unit,
  onStartEndless: () -> Unit,
  onStartBots: () -> Unit,
  onDifficultyChange: (Difficulty) -> Unit,
  onOpenModes: () -> Unit,
  onOpenCreator: () -> Unit,
  onOpenSkins: () -> Unit,
  onOpenShop: () -> Unit,
  onOpenAchievements: () -> Unit,
  onOpenItems: () -> Unit,
  onOpenGallery: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenOnline: () -> Unit,
  onOpenAi: () -> Unit,
  onCheckUpdate: () -> Unit,
  onSendMeowNotification: () -> Unit,
  onRestoreDefaultFair: () -> Unit,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "hero_cat_bob")
  val catScale by infiniteTransition.animateFloat(
    initialValue = 0.96f,
    targetValue = 1.05f,
    animationSpec = infiniteRepeatable(
      animation = tween(1000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "scale"
  )

  val activeSkin = ALL_HERO_SKINS.find { it.id == uiState.selectedSkinId } ?: ALL_HERO_SKINS.first()

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xE60F172A))
      .padding(16.dp),
    contentAlignment = Alignment.Center
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 540.dp)
        .shadow(24.dp, RoundedCornerShape(32.dp))
        .border(2.dp, Color(0xFFFACC15), RoundedCornerShape(32.dp)),
      shape = RoundedCornerShape(32.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B))
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Hero Graphic with Active Skin and Ghost
        Row(
          modifier = Modifier
            .scale(catScale)
            .padding(top = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Box(
            modifier = Modifier
              .size(66.dp)
              .background(
                brush = Brush.radialGradient(
                  listOf(activeSkin.furColor, activeSkin.stripeColor)
                ),
                shape = CircleShape
              )
              .border(3.dp, activeSkin.capeBorderColor, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Text(activeSkin.badgeEmoji, fontSize = 36.sp)
          }

          Spacer(modifier = Modifier.width(12.dp))
          Text("⚡", fontSize = 22.sp)
          Spacer(modifier = Modifier.width(12.dp))

          Box(
            modifier = Modifier
              .size(56.dp)
              .background(
                brush = Brush.radialGradient(
                  listOf(Color(0xFFC084FC), Color(0xFF7E22CE))
                ),
                shape = CircleShape
              )
              .border(2.dp, Color(0xFFE9D5FF), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Text("👻", fontSize = 28.sp)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "GATO CAMPEÃO",
          fontSize = 25.sp,
          fontWeight = FontWeight.Black,
          color = Color(0xFFFDE047),
          letterSpacing = 1.sp
        )

        Text(
          text = "🏆 10 gatos = Campeão • 40 Modos • 40 Itens • 40 Conquistas • Skins & 3D!",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF38BDF8),
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )

        // Status & Milestone badges row
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(bottom = 8.dp)
        ) {
          MenuBadge("⭐ Nível ${uiState.playerLevel}", Color(0xFF38BDF8))
          MenuBadge("🪙 ${uiState.spendablePoints} pts", Color(0xFFFBBF24))
          if (uiState.reviveCount > 0) {
            MenuBadge("💖 ${uiState.reviveCount}", Color(0xFFF472B6))
          }
          if (uiState.bestHighScore > 0) {
            MenuBadge("🏅 Rec ${uiState.bestHighScore}", Color(0xFF34D399))
          }
          if (uiState.championCount > 0) {
            MenuBadge("👑 ${uiState.championCount}x", Color(0xFFFACC15))
          }
          if (uiState.is4dModeEnabled) {
            MenuBadge("🌌 4D", Color(0xFFE879F9))
          } else if (uiState.is3dModeEnabled) {
            MenuBadge("🧊 3D", Color(0xFF22D3EE))
          }
        }

        // Level XP Progress Bar
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "⭐ Progresso de Nível",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF7DD3FC)
            )
            Text(
              text = "${uiState.playerXp}/${uiState.xpToNextLevel} XP",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFE2E8F0)
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          LinearProgressIndicator(
            progress = { (uiState.playerXp.toFloat() / uiState.xpToNextLevel.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
            color = Color(0xFF38BDF8),
            trackColor = Color(0xFF0F172A)
          )
        }

        if (uiState.isRewardsBlocked) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF7F1D1D),
            border = BorderStroke(1.dp, Color(0xFFF87171)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "🚫 Modo Deus/Modificado: sem conquistas ou XP",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFECACA),
                modifier = Modifier.weight(1f)
              )
              TextButton(onClick = onRestoreDefaultFair, contentPadding = PaddingValues(horizontal = 6.dp)) {
                Text("Reativar", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFFFDE047))
              }
            }
          }
        }

        // Quick Difficulty Selector at Start (includes Fácil + Normal + Difícil + Elite Ghost Hunter)
        Column(
          modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            listOf(
              Difficulty.CALMO to "🐢 Fácil",
              Difficulty.NORMAL to "⚖️ Normal"
            ).forEach { (diff, label) ->
              val selected = uiState.difficulty == diff
              OutlinedButton(
                onClick = { onDifficultyChange(diff) },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                  containerColor = if (selected) Color(0xFF7C3AED) else Color(0xFF0F172A)
                ),
                border = BorderStroke(1.dp, if (selected) Color(0xFFC084FC) else Color(0xFF334155)),
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
              ) {
                Text(
                  text = label,
                  fontSize = 11.sp,
                  fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
                  color = if (selected) Color.White else Color(0xFF94A3B8)
                )
              }
            }
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            listOf(
              Difficulty.DESAFIO to "🔥 Difícil",
              Difficulty.ELITE to "👻 Elite (Fantasma)"
            ).forEach { (diff, label) ->
              val selected = uiState.difficulty == diff
              OutlinedButton(
                onClick = { onDifficultyChange(diff) },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                  containerColor = if (selected) {
                    if (diff == Difficulty.ELITE) Color(0xFF9333EA) else Color(0xFF7C3AED)
                  } else Color(0xFF0F172A)
                ),
                border = BorderStroke(
                  1.dp,
                  if (selected) Color(0xFFFDE047) else Color(0xFF334155)
                ),
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("diff_${diff.name.lowercase()}")
              ) {
                Text(
                  text = label,
                  fontSize = 11.sp,
                  fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
                  color = if (selected) Color.White else Color(0xFF94A3B8)
                )
              }
            }
          }
        }

        // Objectives summary
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = Color(0xFF0F172A),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            RuleItem("1️⃣", if (uiState.difficulty == Difficulty.ELITE) {
              "Modo ELITE: Você é o Fantasma! Pegue o gato corredor 3x antes de ele salvar 10!"
            } else {
              "Resgate 10 gatinhos para vencer, subir de nível e ganhar pontos!"
            })
            RuleItem("2️⃣", "Fácil dá conquistas e pontos normal! No Difícil o fantasma prevê seus passos.")
            RuleItem("3️⃣", "Salve tudo na Conta Google, faça amizades, compartilhe e compre skins!")
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Start Normal Game Button
        Button(
          onClick = onStartNormal,
          colors = ButtonDefaults.buttonColors(
            containerColor = if (uiState.difficulty == Difficulty.ELITE) Color(0xFF9333EA) else Color(0xFF10B981)
          ),
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .shadow(8.dp, RoundedCornerShape(18.dp))
            .testTag("start_game_button")
        ) {
          Text(
            text = if (uiState.difficulty == Difficulty.ELITE) "JOGAR ELITE (CAÇAR COMO FANTASMA) 👻" else "MODO CAMPEÃO (10 GATOS) 🐾",
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Endless Mode Button (Unlocked at 1 win!)
        if (uiState.isEndlessUnlocked) {
          Button(
            onClick = onStartEndless,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .border(1.5.dp, Color(0xFFC084FC), RoundedCornerShape(18.dp))
              .testTag("menu_endless_button")
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                "♾️ MODO INFINITO SEM FIM 🚀",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
              )
              if (uiState.bestEndlessCats > 0) {
                Text(
                  "Recorde: ${uiState.bestEndlessCats} gatos infinitos",
                  fontSize = 10.sp,
                  color = Color(0xFFE9D5FF)
                )
              }
            }
          }
        } else {
          OutlinedButton(
            onClick = {},
            enabled = false,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            colors = ButtonDefaults.outlinedButtonColors(
              disabledContainerColor = Color(0x660F172A),
              disabledContentColor = Color(0xFF64748B)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("menu_endless_locked")
          ) {
            Text(
              "🔒 Modo Infinito (Vença 1x para liberar)",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Modo Bots Button (20 Bots, Goal 50 Cats for everyone!)
        Button(
          onClick = onStartBots,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .border(1.5.dp, Color(0xFF2DD4BF), RoundedCornerShape(18.dp))
            .testTag("start_bots_mode_button")
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "🤖 MODO BOTS (20 BOTS • META 50) 🏁",
              fontSize = 13.sp,
              fontWeight = FontWeight.Black,
              color = Color.White
            )
            Text(
              text = "Vença 20 bots na arena — limite de todos: 50 gatos!",
              fontSize = 10.sp,
              color = Color(0xFFCCFBF1)
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 40 Modes Selector Button
        Button(
          onClick = onOpenModes,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(1.5.dp, Color(0xFF7DD3FC), RoundedCornerShape(18.dp))
            .testTag("menu_modes_button")
        ) {
          Text(
            "🎮 40 MODOS DE JOGO (${uiState.unlockedModesCount}/${ALL_40_MODES.size})",
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 1: Estúdio Criativo & Skins
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = onOpenCreator,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFF34D399)),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 48.dp)
              .testTag("menu_creator_button")
          ) {
            Text(
              "🛠️ Criar Mapa\n& Meu Item",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF6EE7B7),
              textAlign = TextAlign.Center,
              lineHeight = 14.sp
            )
          }

          OutlinedButton(
            onClick = onOpenSkins,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFFE879F9)),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 48.dp)
              .testTag("menu_skins_button")
          ) {
            Text(
              "👗 Skins\n(${uiState.unlockedSkinsCount}/${ALL_HERO_SKINS.size})",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFE879F9),
              textAlign = TextAlign.Center,
              lineHeight = 14.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 2: Conquistas & 40 Itens
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = onOpenAchievements,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFFFACC15)),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 48.dp)
              .testTag("menu_achievements_button")
          ) {
            Text(
              "🏅 40 Conquistas\n(${uiState.unlockedAchievements.size}/${ALL_40_ACHIEVEMENTS.size})",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFFDE047),
              textAlign = TextAlign.Center,
              lineHeight = 14.sp
            )
          }

          OutlinedButton(
            onClick = onOpenItems,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFFF472B6)),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 48.dp)
              .testTag("menu_items_button")
          ) {
            Text(
              "🎁 40 Itens\n(${uiState.discoveredItems.size}/${ALL_40_ITEMS.size})",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFF472B6),
              textAlign = TextAlign.Center,
              lineHeight = 14.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 3: Galeria & Ajustes
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = onOpenGallery,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 48.dp)
              .testTag("menu_gallery_button")
          ) {
            Text(
              "📖 Galeria\n(10 Felinos)",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF38BDF8),
              textAlign = TextAlign.Center,
              lineHeight = 14.sp
            )
          }

          OutlinedButton(
            onClick = onOpenSettings,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFF94A3B8)),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 48.dp)
              .testTag("menu_settings_button")
          ) {
            Text(
              "⚙️ Ajustes, 3D/4D\n& Velocidade",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFCBD5E1),
              textAlign = TextAlign.Center,
              lineHeight = 14.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 4: Loja (Pontos/Trocas) & Multiplayer (200+ players + Conta Google + Amizades)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = onOpenShop,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
            modifier = Modifier
              .weight(0.9f)
              .defaultMinSize(minHeight = 48.dp)
              .testTag("menu_shop_button")
          ) {
            Text(
              "🛒 Loja\n(${uiState.spendablePoints} pts)",
              fontSize = 11.sp,
              fontWeight = FontWeight.Black,
              color = Color.White,
              textAlign = TextAlign.Center,
              lineHeight = 13.sp
            )
          }

          Button(
            onClick = onOpenOnline,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0369A1)),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
            modifier = Modifier
              .weight(1.3f)
              .defaultMinSize(minHeight = 48.dp)
              .testTag("menu_online_button")
          ) {
            Text(
              text = if (uiState.activeRoomCode != null) {
                "👥 Sala ${uiState.activeRoomCode} (${uiState.multiplayerPlayerCount}/${uiState.multiplayerMaxPlayers})"
              } else {
                "🌐 Online 200+ • ☁️ Save\n& 🤝 Amizades"
              },
              fontSize = 11.sp,
              fontWeight = FontWeight.Black,
              color = Color.White,
              textAlign = TextAlign.Center,
              lineHeight = 13.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 5: IA Dicas & Notificação Miau
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = onOpenAi,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFF22D3EE)),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 48.dp)
              .testTag("menu_ai_button")
          ) {
            Text(
              "🤖 IA Guia Felino",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF67E8F9),
              textAlign = TextAlign.Center
            )
          }

          OutlinedButton(
            onClick = onSendMeowNotification,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFFF472B6)),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 48.dp)
              .testTag("menu_meow_notify_button")
          ) {
            Text(
              "🔔 Notif. Miau!",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFF9A8D4),
              textAlign = TextAlign.Center
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Auto-Update Quick Button
        OutlinedButton(
          onClick = onCheckUpdate,
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(
            1.2.dp,
            if (uiState.isUpdateAvailable) Color(0xFF34D399) else Color(0xFF475569)
          ),
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (uiState.isUpdateAvailable) Color(0xFF064E3B) else Color(0x660F172A)
          ),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .testTag("menu_check_update_button")
        ) {
          val label = when {
            uiState.isUpdateDownloading -> "⬇️ Baixando atualização... ${uiState.updateProgressPercent}%"
            uiState.isUpdateAvailable -> "🚀 Nova versão disponível! Toque para atualizar"
            uiState.appUpdateStatusText.isNotBlank() -> uiState.appUpdateStatusText
            else -> "🔄 Atualização Auto (v${BuildConfig.VERSION_NAME}) • Verificar Agora"
          }
          Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (uiState.isUpdateAvailable) Color(0xFF6EE7B7) else Color(0xFFCBD5E1),
            textAlign = TextAlign.Center
          )
        }
      }
    }
  }
}

@Composable
private fun MenuBadge(text: String, color: Color) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = color.copy(alpha = 0.16f),
    border = BorderStroke(1.dp, color)
  ) {
    Text(
      text = text,
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold,
      color = color,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
    )
  }
}

@Composable
fun RuleItem(number: String, text: String) {
  Row(
    modifier = Modifier.padding(vertical = 2.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(number, fontSize = 13.sp)
    Spacer(modifier = Modifier.width(8.dp))
    Text(text, fontSize = 11.sp, color = Color(0xFFE2E8F0))
  }
}
