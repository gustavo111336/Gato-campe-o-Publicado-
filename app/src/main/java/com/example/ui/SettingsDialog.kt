package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.*
import com.example.update.AppUpdateUiState
import com.example.update.AppUpdateUiStatus

@Composable
fun SettingsDialog(
  uiState: GameUiState,
  updateState: AppUpdateUiState,
  onControlTypeChange: (ControlType) -> Unit,
  onToggleSound: () -> Unit,
  onToggleHaptic: () -> Unit,
  onToggle3dMode: () -> Unit,
  onToggle4dMode: () -> Unit,
  onDifficultyChange: (Difficulty) -> Unit,
  onCustomGhostSpeedChange: (Int) -> Unit,
  onArenaThemeChange: (ArenaTheme) -> Unit,
  onGhostCountChange: (Int) -> Unit,
  onHeroSpeedChange: (HeroSpeedOption) -> Unit,
  onPowerUpFreqChange: (PowerUpFrequency) -> Unit,
  onToggleSonicButton: () -> Unit,
  onToggleGodMode: () -> Unit,
  onRestoreDefaultFair: () -> Unit,
  onToggleAutoUpdate: (Boolean) -> Unit,
  onCheckUpdateNow: () -> Unit,
  onOpenUpdateStore: () -> Unit,
  onSendMeowNotification: () -> Unit,
  onDismiss: () -> Unit
) {
  var speedText by remember(uiState.customGhostSpeed) { mutableStateOf(uiState.customGhostSpeed.toString()) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.90f)
        .padding(4.dp)
        .shadow(24.dp, RoundedCornerShape(24.dp))
        .border(2.dp, Color(0xFF64748B), RoundedCornerShape(24.dp))
        .testTag("settings_dialog"),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(18.dp)
          .verticalScroll(rememberScrollState())
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "⚙️ Configurações & Gameplay",
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
          )
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_settings_button")
          ) {
            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (uiState.isRewardsBlocked) {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF7F1D1D),
            border = BorderStroke(1.5.dp, Color(0xFFF87171)),
            modifier = Modifier.fillMaxWidth().testTag("rewards_blocked_warning")
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = "🚫 Conquistas, Nível e Prêmios Pausados",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFDE047)
              )
              Text(
                text = "Motivo: ${uiState.rewardsBlockedReason}. No Modo Deus ou com modificações no jogo, nenhuma conquista, XP, nível ou prêmio é ganho.",
                fontSize = 11.sp,
                color = Color(0xFFFECACA),
                modifier = Modifier.padding(top = 3.dp)
              )
              Spacer(modifier = Modifier.height(8.dp))
              Button(
                onClick = onRestoreDefaultFair,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
              ) {
                Text("🔄 Restaurar Padrão (Reativar Prêmios)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
              }
            }
          }
          Spacer(modifier = Modifier.height(12.dp))
        }

        // 3D & 4D MODE TOGGLE CARDS
        Text("🧊 Câmera 3D & Holograma 4D", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = if (uiState.is3dModeEnabled) Color(0xFF0C4A6E) else Color(0xFF131C2E),
          border = BorderStroke(
            1.5.dp,
            if (uiState.is3dModeEnabled) Color(0xFF22D3EE) else Color(0xFF334155)
          ),
          modifier = Modifier.fillMaxWidth().testTag("mode_3d_card")
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
              Text(
                text = if (uiState.is3dModeEnabled) "🧊 Modo 3D Ativado!" else "🧊 Modo 3D (Visão em Perspectiva)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = if (uiState.is3dModeEnabled) Color(0xFF67E8F9) else Color.White
              )
              Text(
                text = "Inclina a câmera em 3D com sombras reais, paredes elevadas e obstáculos em relevo.",
                fontSize = 11.sp,
                color = Color(0xFFCBD5E1),
                modifier = Modifier.padding(top = 2.dp)
              )
            }
            Switch(
              checked = uiState.is3dModeEnabled,
              onCheckedChange = { onToggle3dMode() },
              modifier = Modifier.testTag("mode_3d_switch")
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
          shape = RoundedCornerShape(16.dp),
          color = if (uiState.is4dModeEnabled) Color(0xFF3B0764) else Color(0xFF131C2E),
          border = BorderStroke(
            1.5.dp,
            if (uiState.is4dModeEnabled) Color(0xFFE879F9) else Color(0xFF334155)
          ),
          modifier = Modifier.fillMaxWidth().testTag("mode_4d_card")
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
              Text(
                text = if (uiState.is4dModeEnabled) "🌌 Modo 4D Holográfico ON!" else "🌌 Modo 4D (Diante dos Seus Olhos)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = if (uiState.is4dModeEnabled) Color(0xFFF0ABFC) else Color.White
              )
              Text(
                text = "Usa sensor de inclinação, fundo espacial profundo e partículas perto dos olhos!",
                fontSize = 11.sp,
                color = Color(0xFFCBD5E1),
                modifier = Modifier.padding(top = 2.dp)
              )
            }
            Switch(
              checked = uiState.is4dModeEnabled,
              onCheckedChange = { onToggle4dMode() },
              modifier = Modifier.testTag("mode_4d_switch")
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // GOD MODE SECTION (Unlocked at 10 wins!)
        Text("👑 Modo Secreto (10 Vitórias)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFDE047))
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = if (uiState.isGodModeUnlocked) Color(0xFF312E81) else Color(0xFF131C2E),
          border = BorderStroke(
            1.5.dp,
            if (uiState.isGodModeEnabled) Color(0xFFFACC15) else if (uiState.isGodModeUnlocked) Color(0xFF818CF8) else Color(0xFF334155)
          ),
          modifier = Modifier.fillMaxWidth().testTag("god_mode_card")
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
              Text(
                text = if (uiState.isGodModeUnlocked) "👑 Modo Deus (Imortal)" else "🔒 Modo Deus (Bloqueado)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = if (uiState.isGodModeUnlocked) Color(0xFFFDE047) else Color(0xFF94A3B8)
              )
              Text(
                text = if (uiState.isGodModeUnlocked) {
                  "Você nunca morre! Fantasmas são repelidos com luz dourada."
                } else {
                  val remaining = (10 - uiState.championCount).coerceAtLeast(0)
                  "Vença 10 vezes para desbloquear! Faltam $remaining vitória(s)."
                },
                fontSize = 11.sp,
                color = Color(0xFFCBD5E1),
                modifier = Modifier.padding(top = 2.dp)
              )
            }
            Switch(
              checked = uiState.isGodModeEnabled,
              onCheckedChange = { onToggleGodMode() },
              enabled = uiState.isGodModeUnlocked,
              modifier = Modifier.testTag("god_mode_switch")
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // DIFFICULTY & CUSTOM GHOST SPEED 1..1000
        Text("⚡ Dificuldade & Velocidade do Fantasma", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC084FC))
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(Difficulty.CALMO, Difficulty.NORMAL, Difficulty.DESAFIO).forEach { diff ->
            OptionChipButton(
              label = diff.label,
              isSelected = uiState.difficulty == diff,
              selectedColor = Color(0xFF7E22CE),
              borderColor = Color(0xFFC084FC),
              onClick = { onDifficultyChange(diff) },
              modifier = Modifier.weight(1f)
            )
          }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          OptionChipButton(
            label = "👻 Elite (Você é o Fantasma)",
            isSelected = uiState.difficulty == Difficulty.ELITE,
            selectedColor = Color(0xFF9333EA),
            borderColor = Color(0xFFFDE047),
            onClick = { onDifficultyChange(Difficulty.ELITE) },
            modifier = Modifier.weight(1.2f)
          )
          OptionChipButton(
            label = "🎛️ Veloc. 1 a 1000",
            isSelected = uiState.difficulty == Difficulty.CUSTOM,
            selectedColor = Color(0xFFDB2777),
            borderColor = Color(0xFFF472B6),
            onClick = { onDifficultyChange(Difficulty.CUSTOM) },
            modifier = Modifier.weight(1f)
          )
        }

        if (uiState.difficulty == Difficulty.CUSTOM) {
          Spacer(modifier = Modifier.height(8.dp))
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1E1B4B),
            border = BorderStroke(1.5.dp, Color(0xFFF472B6)),
            modifier = Modifier.fillMaxWidth().testTag("custom_speed_panel")
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "👻 Velocidade: ${uiState.customGhostSpeed} / 1000",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Black,
                  color = Color(0xFFFDE047)
                )
                Text(
                  text = when {
                    uiState.customGhostSpeed <= 80 -> "Tartaruga 🐢"
                    uiState.customGhostSpeed <= 280 -> "Equilibrada 👻"
                    uiState.customGhostSpeed <= 550 -> "Rápida ⚡"
                    else -> "Insana 🔥"
                  },
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFFF9A8D4)
                )
              }

              Slider(
                value = uiState.customGhostSpeed.toFloat(),
                onValueChange = { onCustomGhostSpeedChange(it.toInt()) },
                valueRange = 1f..1000f,
                modifier = Modifier.testTag("custom_speed_slider")
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                OutlinedButton(
                  onClick = { onCustomGhostSpeedChange(uiState.customGhostSpeed - 50) },
                  contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Text("-50", fontSize = 11.sp, color = Color.White)
                }
                OutlinedButton(
                  onClick = { onCustomGhostSpeedChange(uiState.customGhostSpeed - 1) },
                  contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Text("-1", fontSize = 11.sp, color = Color.White)
                }
                OutlinedTextField(
                  value = speedText,
                  onValueChange = { raw ->
                    val digits = raw.filter { it.isDigit() }.take(4)
                    speedText = digits
                    digits.toIntOrNull()?.let { parsed ->
                      onCustomGhostSpeedChange(parsed.coerceIn(1, 1000))
                    }
                  },
                  singleLine = true,
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFF472B6),
                    unfocusedBorderColor = Color(0xFF475569)
                  ),
                  modifier = Modifier.width(84.dp)
                )
                OutlinedButton(
                  onClick = { onCustomGhostSpeedChange(uiState.customGhostSpeed + 1) },
                  contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Text("+1", fontSize = 11.sp, color = Color.White)
                }
                OutlinedButton(
                  onClick = { onCustomGhostSpeedChange(uiState.customGhostSpeed + 50) },
                  contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Text("+50", fontSize = 11.sp, color = Color.White)
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // CAREER STATS CARD
        Text("🏆 Seu Histórico de Campeão", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFDE047))
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Color(0xFF1E293B),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            StatRow("Nível de Carreira:", "⭐ Nível ${uiState.playerLevel} (${uiState.playerXp}/${uiState.xpToNextLevel} XP)")
            StatRow("Pontos na Loja:", "${uiState.spendablePoints} 🪙")
            StatRow("Recorde no Placar:", "${uiState.bestHighScore} pts 🏅")
            StatRow("Títulos de Campeão:", "${uiState.championCount} 🏆")
            val minutes = (uiState.bestTimeSec / 60).toInt()
            val seconds = (uiState.bestTimeSec % 60).toInt()
            val timeStr = if (uiState.bestTimeSec > 0.01f) String.format("%02d:%02d", minutes, seconds) else "--:--"
            StatRow("Melhor Tempo (10 Gatos):", timeStr)
            StatRow("Recorde Modo Infinito:", "${uiState.bestEndlessCats} ♾️")
            StatRow("Total de Felinos Salvos:", "${uiState.totalCatsSaved} 🐱")
            StatRow("Conquistas Desbloqueadas:", "${uiState.unlockedAchievements.size} / 40 🏅")
            StatRow("Itens Descobertos:", "${uiState.discoveredItems.size} / 40 🎁")
            StatRow("Modos Desbloqueados:", "${uiState.unlockedModesCount} / 40 🎮")
            StatRow("Skins Desbloqueadas:", "${uiState.unlockedSkinsCount} / ${ALL_HERO_SKINS.size} 👗")
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ARENA THEME SELECTION
        Text("🗺️ Cenário da Arena", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          for (theme in ArenaTheme.values()) {
            OptionChipButton(
              label = "${theme.emoji} ${theme.label}",
              isSelected = uiState.arenaTheme == theme,
              selectedColor = Color(0xFF059669),
              borderColor = Color(0xFF34D399),
              onClick = { onArenaThemeChange(theme) },
              modifier = Modifier.weight(1f)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // NUMBER OF GHOSTS
        Text("👻 Fantasmas na Perseguição", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC084FC))
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf(1, 2, 3).forEach { count ->
            OptionChipButton(
              label = "$count Fantasma${if (count > 1) "s" else ""}",
              isSelected = uiState.ghostCount == count,
              selectedColor = Color(0xFF7E22CE),
              borderColor = Color(0xFFC084FC),
              onClick = { onGhostCountChange(count) },
              modifier = Modifier.weight(1f)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // HERO SPEED
        Text("🐾 Velocidade do Gato Herói", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          for (speedOpt in HeroSpeedOption.values()) {
            OptionChipButton(
              label = speedOpt.label,
              isSelected = uiState.heroSpeedOption == speedOpt,
              selectedColor = Color(0xFFD97706),
              borderColor = Color(0xFFFBBF24),
              onClick = { onHeroSpeedChange(speedOpt) },
              modifier = Modifier.weight(1f)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // POWER-UP FREQUENCY
        Text("🎁 Itens Mágicos na Arena", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF472B6))
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          for (freq in PowerUpFrequency.values()) {
            OptionChipButton(
              label = freq.label,
              isSelected = uiState.powerUpFrequency == freq,
              selectedColor = Color(0xFFDB2777),
              borderColor = Color(0xFFF472B6),
              onClick = { onPowerUpFreqChange(freq) },
              modifier = Modifier.weight(1f)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // CONTROLS SELECTION
        Text("🎮 Modo de Controle", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OptionChipButton(
            label = "Joystick",
            isSelected = uiState.controlType == ControlType.JOYSTICK,
            selectedColor = Color(0xFF0284C7),
            borderColor = Color(0xFF38BDF8),
            onClick = { onControlTypeChange(ControlType.JOYSTICK) },
            modifier = Modifier.weight(1f)
          )
          OptionChipButton(
            label = "Botões",
            isSelected = uiState.controlType == ControlType.DPAD,
            selectedColor = Color(0xFF0284C7),
            borderColor = Color(0xFF38BDF8),
            onClick = { onControlTypeChange(ControlType.DPAD) },
            modifier = Modifier.weight(1f)
          )
          OptionChipButton(
            label = "Toque",
            isSelected = uiState.controlType == ControlType.TOUCH_DIRECT,
            selectedColor = Color(0xFF0284C7),
            borderColor = Color(0xFF38BDF8),
            onClick = { onControlTypeChange(ControlType.TOUCH_DIRECT) },
            modifier = Modifier.weight(1f)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AUDIO, HAPTIC & HUD TOGGLES
        Text("🔊 Efeitos & Habilidades", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
        Spacer(modifier = Modifier.height(6.dp))

        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Color(0xFF1E293B),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Botão Onda Sônica na Tela 🔔", fontSize = 12.sp, color = Color.White)
              Switch(
                checked = uiState.showSonicButton,
                onCheckedChange = { onToggleSonicButton() }
              )
            }
            HorizontalDivider(color = Color(0xFF334155), thickness = 0.5.dp)
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Efeitos Sonoros (Miau & Fanfarra)", fontSize = 12.sp, color = Color.White)
              Switch(
                checked = uiState.isSoundEnabled,
                onCheckedChange = { onToggleSound() }
              )
            }
            HorizontalDivider(color = Color(0xFF334155), thickness = 0.5.dp)
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Vibração Tátil (Haptic)", fontSize = 12.sp, color = Color.White)
              Switch(
                checked = uiState.isHapticEnabled,
                onCheckedChange = { onToggleHaptic() }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // AUTO-UPDATE SECTION (Google Play In-App Updates + Firestore release check)
        Text("🔄 Atualização Automática do App", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = Color(0xFF131C2E),
          border = BorderStroke(1.5.dp, if (uiState.autoUpdateEnabled) Color(0xFF38BDF8) else Color(0xFF334155)),
          modifier = Modifier.fillMaxWidth().testTag("auto_update_settings_card")
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                  text = "Atualizar Sozinho ao Abrir",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Black,
                  color = Color.White
                )
                Text(
                  text = "Versão instalada: v${updateState.currentVersionName} (build ${updateState.currentVersionCode})",
                  fontSize = 11.sp,
                  color = Color(0xFF94A3B8)
                )
              }
              Switch(
                checked = uiState.autoUpdateEnabled,
                onCheckedChange = onToggleAutoUpdate,
                modifier = Modifier.testTag("auto_update_switch")
              )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = updateState.message,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = when (updateState.status) {
                AppUpdateUiStatus.UPDATE_AVAILABLE, AppUpdateUiStatus.DOWNLOADED -> Color(0xFF34D399)
                AppUpdateUiStatus.ERROR -> Color(0xFFFCA5A5)
                else -> Color(0xFFCBD5E1)
              }
            )

            if (updateState.status == AppUpdateUiStatus.DOWNLOADING) {
              Spacer(modifier = Modifier.height(6.dp))
              LinearProgressIndicator(
                progress = { (updateState.downloadProgressPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = Color(0xFF34D399),
                trackColor = Color(0xFF0F172A)
              )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Button(
                onClick = onCheckUpdateNow,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("check_update_now_button")
              ) {
                Text("🔄 Verificar e Atualizar", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
              }

              OutlinedButton(
                onClick = onOpenUpdateStore,
                border = BorderStroke(1.dp, Color(0xFF34D399)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(0.85f).defaultMinSize(minHeight = 48.dp).testTag("open_store_update_button")
              ) {
                Text("📲 Google Play", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6EE7B7))
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
          onClick = onSendMeowNotification,
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.5.dp, Color(0xFFF472B6)),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("settings_meow_notification_button")
        ) {
          Text("🔔 Notificação do Gato: \"meow meow meow\"", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF9A8D4))
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
          onClick = onDismiss,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
          modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
          Text("Salvar e Fechar 👍", fontWeight = FontWeight.Bold, color = Color.White)
        }
      }
    }
  }
}

@Composable
fun StatRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(label, fontSize = 12.sp, color = Color(0xFF94A3B8))
    Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
  }
}

@Composable
fun OptionChipButton(
  label: String,
  isSelected: Boolean,
  selectedColor: Color,
  borderColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  OutlinedButton(
    onClick = onClick,
    shape = RoundedCornerShape(12.dp),
    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
    colors = ButtonDefaults.outlinedButtonColors(
      containerColor = if (isSelected) selectedColor else Color.Transparent
    ),
    border = BorderStroke(
      1.5.dp,
      if (isSelected) borderColor else Color(0xFF334155)
    ),
    modifier = modifier
  ) {
    Text(
      label,
      fontSize = 11.sp,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
      color = if (isSelected) Color.White else Color(0xFF94A3B8),
      maxLines = 1
    )
  }
}
