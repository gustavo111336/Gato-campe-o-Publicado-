package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.window.Dialog
import com.example.model.CHAMPION_CATS

@Composable
fun VictoryDialog(
  uiState: GameUiState,
  onPlayAgain: () -> Unit,
  onPlayEndless: () -> Unit,
  onReturnToMenu: () -> Unit,
  onOpenGallery: () -> Unit,
  onDismiss: () -> Unit
) {
  val infiniteTransition = rememberInfiniteTransition(label = "trophy_pulse")
  val trophyScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.15f,
    animationSpec = infiniteRepeatable(
      animation = tween(800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "scale"
  )

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp)
        .shadow(24.dp, RoundedCornerShape(28.dp))
        .border(3.dp, Color(0xFFFACC15), RoundedCornerShape(28.dp))
        .testTag("victory_dialog"),
      shape = RoundedCornerShape(28.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B))
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(
          modifier = Modifier
            .size(84.dp)
            .scale(trophyScale)
            .background(
              brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFDE047), Color(0xFFD97706))
              ),
              shape = CircleShape
            )
            .border(3.dp, Color.White, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Text("🏆", fontSize = 44.sp)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = if (uiState.isPlayerGhost) "👻 FANTASMA CAMPEÃO!" else "VOCÊ É O CAMPEÃO!",
          fontSize = 23.sp,
          fontWeight = FontWeight.Black,
          color = Color(0xFFFDE047),
          textAlign = TextAlign.Center
        )

        Text(
          text = "${uiState.currentMode.emoji} Modo: ${uiState.currentMode.name}",
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF38BDF8),
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(top = 4.dp)
        )

        Text(
          text = if (uiState.isPlayerGhost) {
            "Você capturou o alvo ${uiState.catsCollected} vezes antes que os gatos escapassem!"
          } else {
            "Parabéns! Você resgatou todos os ${uiState.goalCats} gatos e escapou com bravura!"
          },
          fontSize = 13.sp,
          color = Color(0xFFE2E8F0),
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(vertical = 6.dp)
        )

        // Milestone unlocks celebration box!
        if (uiState.lastVictoryUnlocks.isNotEmpty()) {
          Surface(
            color = Color(0xFF312E81),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFACC15)),
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(
                text = "🎉 NOVOS DESBLOQUEIOS:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFDE047)
              )
              for (unlock in uiState.lastVictoryUnlocks) {
                Text(
                  text = unlock,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                  modifier = Modifier.padding(top = 3.dp)
                )
              }
            }
          }
        }

        Text(
          text = "🐱 Matilha dos 10 Felinos Campeões:",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF38BDF8),
          modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
        )

        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.padding(vertical = 4.dp)
        ) {
          items(CHAMPION_CATS) { cat ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0xFF0F172A),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
              Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text(cat.accessoryEmoji, fontSize = 20.sp)
                Text(
                  text = cat.name,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = cat.furColor
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
          color = Color(0xFF0F172A),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceAround
            ) {
              val minutes = (uiState.gameTimeSeconds / 60).toInt()
              val seconds = (uiState.gameTimeSeconds % 60).toInt()
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🏆 Pontuação", fontSize = 11.sp, color = Color(0xFF94A3B8))
                Text(
                  "${uiState.currentScore} pts",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Black,
                  color = Color(0xFF34D399)
                )
              }
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🏅 Recorde", fontSize = 11.sp, color = Color(0xFF94A3B8))
                Text(
                  "${uiState.bestHighScore} pts",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Black,
                  color = Color(0xFFFDE047)
                )
              }
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⏱️ Tempo", fontSize = 11.sp, color = Color(0xFF94A3B8))
                Text(
                  String.format("%02d:%02d", minutes, seconds),
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Black,
                  color = Color.White
                )
              }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceAround
            ) {
              Text(
                text = "⭐ Nível ${uiState.playerLevel} (+${uiState.lastXpEarned} XP)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF38BDF8)
              )
              Text(
                text = "👑 ${uiState.championCount} Vitórias",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFDE047)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = onPlayAgain,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("play_again_button")
        ) {
          Text("Jogar Novamente 🐾", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
          onClick = onPlayEndless,
          shape = RoundedCornerShape(16.dp),
          border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF59E0B)),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("endless_button")
        ) {
          Text("Continuar no Modo Infinito (∞ Gatos) 🚀", fontSize = 12.sp, color = Color(0xFFFDE047), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          TextButton(
            onClick = onOpenGallery,
            modifier = Modifier.testTag("gallery_from_victory_button")
          ) {
            Text("Livro dos Gatos 📖", color = Color(0xFF38BDF8), fontSize = 12.sp)
          }
          TextButton(
            onClick = onReturnToMenu,
            modifier = Modifier.testTag("menu_from_victory_button")
          ) {
            Text("Menu Principal 🏠", color = Color(0xFFCBD5E1), fontSize = 12.sp)
          }
        }
      }
    }
  }
}

@Composable
fun GameOverDialog(
  uiState: GameUiState,
  onPlayAgain: () -> Unit,
  onUseRevive: () -> Unit,
  onOpenShopForRevives: () -> Unit,
  onReturnToMenu: () -> Unit,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
        .shadow(24.dp, RoundedCornerShape(28.dp))
        .border(2.dp, Color(0xFFEF4444), RoundedCornerShape(28.dp))
        .testTag("game_over_dialog"),
      shape = RoundedCornerShape(28.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B))
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        val timedOut = uiState.currentMode.timeLimitSec > 0f && uiState.timeLimitRemaining <= 0f
        val botWon = !uiState.winnerBotName.isNullOrBlank()
        Text(
          text = when {
            botWon -> "🤖"
            timedOut -> "⏱️"
            else -> "👻"
          },
          fontSize = 54.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = when {
            botWon -> "UM BOT ALCANÇOU 50 GATOS!"
            timedOut -> "O TEMPO ACABOU!"
            uiState.isPlayerGhost -> "O GATO CORREDOR ESCAPOU!"
            else -> "O FANTASMA TE PEGOU!"
          },
          fontSize = 20.sp,
          fontWeight = FontWeight.Black,
          color = Color(0xFFF87171),
          textAlign = TextAlign.Center
        )

        val summaryText = when {
          botWon -> "🏆 ${uiState.winnerBotName} venceu a corrida dos 20 bots (50/50)! Você resgatou ${uiState.catsCollected}/50 gatos."
          uiState.isPlayerGhost -> "Você acertou ${uiState.catsCollected}/${uiState.goalCats} capturas antes do corredor resgatar 10 gatos!"
          uiState.isEndlessMode -> "${uiState.currentMode.emoji} ${uiState.currentMode.name}: você resgatou ${uiState.catsCollected} gatinhos! (Recorde: ${uiState.bestEndlessCats} ♾️)"
          else -> "${uiState.currentMode.emoji} ${uiState.currentMode.name}: ${uiState.catsCollected} de ${uiState.goalCats} felinos nesta rodada."
        }

        Text(
          text = summaryText,
          fontSize = 14.sp,
          color = Color(0xFFE2E8F0),
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(vertical = 8.dp)
        )

        Surface(
          color = Color(0xFF0F172A),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.SpaceAround
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("🏆 Pontuação", fontSize = 10.sp, color = Color(0xFF94A3B8))
              Text("${uiState.currentScore} pts", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF34D399))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("🏅 Recorde", fontSize = 10.sp, color = Color(0xFF94A3B8))
              Text("${uiState.bestHighScore} pts", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFFFDE047))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("⭐ Nível ${uiState.playerLevel}", fontSize = 10.sp, color = Color(0xFF94A3B8))
              Text("+${uiState.lastXpEarned} XP", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF38BDF8))
            }
          }
        }

        if (uiState.isRewardsBlocked) {
          Surface(
            color = Color(0xFF7F1D1D),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
          ) {
            Text(
              text = "🚫 Modo Deus ou modificações ativos (${uiState.rewardsBlockedReason}): sem conquistas, XP ou prêmios.",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFFECACA),
              modifier = Modifier.padding(10.dp)
            )
          }
        }

        if (!botWon) {
          if (uiState.reviveCount > 0) {
            Button(
              onClick = onUseRevive,
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("use_revive_button")
            ) {
              Text(
                text = "💖 USAR REVIVE (${uiState.reviveCount} restante(s)) ✨",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
              )
            }
            Spacer(modifier = Modifier.height(10.dp))
          } else {
            OutlinedButton(
              onClick = onOpenShopForRevives,
              border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF472B6)),
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("buy_revive_from_gameover_button")
            ) {
              Text(
                text = "🛒 Sem Revives — Comprar na Loja (150 pts)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF9A8D4)
              )
            }
            Spacer(modifier = Modifier.height(10.dp))
          }
        }

        Surface(
          color = Color(0xFF0F172A),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = "💡 Dica para se tornar Campeão:",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFFDE047)
            )
            Text(
              text = "Use o TURBO ⚡ e a ONDA SÔNICA 🔔 para afastar os fantasmas, ou pegue o Relógio de Gelo ❄️ e o Ímã 🧲!",
              fontSize = 11.sp,
              color = Color(0xFF94A3B8),
              modifier = Modifier.padding(top = 4.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = onPlayAgain,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("retry_button")
        ) {
          Text("Tentar Novamente 🐾", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onReturnToMenu) {
          Text("Voltar ao Menu Principal 🏠", color = Color(0xFFCBD5E1), fontSize = 13.sp)
        }
      }
    }
  }
}
