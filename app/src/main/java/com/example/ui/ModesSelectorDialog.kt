package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.ALL_40_MODES
import com.example.model.GameModeDef

private enum class ModeFilter(val label: String) {
  ALL("Todos (40)"),
  UNLOCKED("Liberados"),
  LOCKED("Bloqueados")
}

@Composable
fun ModesSelectorDialog(
  uiState: GameUiState,
  isModeUnlocked: (GameModeDef) -> Boolean,
  onSelectMode: (GameModeDef) -> Unit,
  onOpenShop: () -> Unit,
  onDismiss: () -> Unit
) {
  var selectedFilter by remember { mutableStateOf(ModeFilter.ALL) }

  val unlockedCount = uiState.unlockedModesCount
  val filteredModes = remember(
    selectedFilter,
    uiState.championCount,
    uiState.totalCatsSaved,
    uiState.bestEndlessCats,
    uiState.unlockedAchievements.size,
    uiState.discoveredItems.size,
    uiState.totalGhostsScared,
    uiState.purchasedModeIds
  ) {
    ALL_40_MODES.filter { mode ->
      val isUnlocked = isModeUnlocked(mode)
      when (selectedFilter) {
        ModeFilter.ALL -> true
        ModeFilter.UNLOCKED -> isUnlocked
        ModeFilter.LOCKED -> !isUnlocked
      }
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.90f)
        .padding(4.dp)
        .shadow(24.dp, RoundedCornerShape(24.dp))
        .border(2.dp, Color(0xFF38BDF8), RoundedCornerShape(24.dp))
        .testTag("modes_selector_dialog"),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "🎮 40 Modos de Jogo",
              fontSize = 19.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFF38BDF8)
            )
            Text(
              text = "$unlockedCount de ${ALL_40_MODES.size} modos desbloqueados • Cada um com seu desafio!",
              fontSize = 11.sp,
              color = Color(0xFF94A3B8)
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_modes_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Fechar 40 Modos",
              tint = Color.White
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
          progress = { (unlockedCount.toFloat() / ALL_40_MODES.size.toFloat()).coerceIn(0f, 1f) },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
          color = Color(0xFF38BDF8),
          trackColor = Color(0xFF1E293B)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(ModeFilter.values()) { filter ->
            val label = when (filter) {
              ModeFilter.ALL -> "🎮 Todos (40)"
              ModeFilter.UNLOCKED -> "✅ Liberados ($unlockedCount)"
              ModeFilter.LOCKED -> "🔒 Faltam (${ALL_40_MODES.size - unlockedCount})"
            }
            val selected = selectedFilter == filter
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = if (selected) Color(0xFF0284C7) else Color(0xFF1E293B),
              border = BorderStroke(1.dp, if (selected) Color(0xFF7DD3FC) else Color(0xFF334155)),
              modifier = Modifier.clickable { selectedFilter = filter }
            ) {
              Text(
                text = label,
                color = if (selected) Color.White else Color(0xFFCBD5E1),
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f)
        ) {
          items(filteredModes, key = { it.id }) { mode ->
            val isUnlocked = isModeUnlocked(mode)
            val progress = mode.currentProgress(
              wins = uiState.championCount,
              totalCats = uiState.totalCatsSaved,
              bestEndless = uiState.bestEndlessCats,
              achievementsCount = uiState.unlockedAchievements.size,
              discoveredItemsCount = uiState.discoveredItems.size,
              ghostsScared = uiState.totalGhostsScared
            )
            GameModeCard(
              mode = mode,
              isUnlocked = isUnlocked,
              currentProgress = progress,
              onPlay = { onSelectMode(mode) }
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = {
              onDismiss()
              onOpenShop()
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
            modifier = Modifier.weight(1f).height(48.dp)
          ) {
            Text("🛒 Loja (${uiState.spendablePoints} pts)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
          Button(
            onClick = onDismiss,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier.weight(0.8f).height(48.dp)
          ) {
            Text("Voltar 🐾", color = Color.White, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
private fun GameModeCard(
  mode: GameModeDef,
  isUnlocked: Boolean,
  currentProgress: Int,
  onPlay: () -> Unit
) {
  val borderColor = when {
    isUnlocked && mode.isEndless -> Color(0xFFA855F7)
    isUnlocked && mode.isGodTrialMode -> Color(0xFFFACC15)
    isUnlocked -> Color(0xFF10B981)
    else -> Color(0xFF334155)
  }
  val bgColor = if (isUnlocked) Color(0xFF1E293B) else Color(0xFF131C2E)

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = bgColor,
    border = BorderStroke(1.5.dp, borderColor),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Box(
          modifier = Modifier
            .size(46.dp)
            .background(
              if (isUnlocked) Color(0xFF312E81) else Color(0xFF0F172A),
              CircleShape
            )
            .border(2.dp, borderColor, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = if (isUnlocked) mode.emoji else "🔒",
            fontSize = 22.sp
          )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "#${mode.number} ${mode.name}",
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            color = if (isUnlocked) Color(0xFFFDE047) else Color(0xFFCBD5E1)
          )
          Text(
            text = mode.description,
            fontSize = 11.sp,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(top = 2.dp)
          )
        }

        if (isUnlocked) {
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = onPlay,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = if (mode.isEndless) Color(0xFF7C3AED) else Color(0xFF10B981)
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier
              .defaultMinSize(minHeight = 48.dp)
              .testTag("play_mode_${mode.id}")
          ) {
            Text("JOGAR ▶", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Objective text
      Text(
        text = "🎯 Objetivo: ${mode.objectiveText}",
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (isUnlocked) Color(0xFF38BDF8) else Color(0xFF64748B)
      )

      Spacer(modifier = Modifier.height(6.dp))

      // Spec tags
      LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        item {
          ModeTag(
            text = if (mode.isEndless) "♾️ Infinitos gatos" else "🐱 ${mode.goalCats} gatos",
            color = Color(0xFF38BDF8)
          )
        }
        item {
          ModeTag(
            text = "👻 ${mode.ghostCount} fantasma${if (mode.ghostCount > 1) "s" else ""}",
            color = Color(0xFFC084FC)
          )
        }
        item {
          ModeTag(
            text = if (mode.isGodTrialMode) "👑 Imortal" else "❤️ ${mode.heroLives} vida${if (mode.heroLives > 1) "s" else ""}",
            color = if (mode.isGodTrialMode) Color(0xFFFACC15) else Color(0xFFF43F5E)
          )
        }
        if (mode.timeLimitSec > 0f) {
          item { ModeTag("⏱️ ${mode.timeLimitSec.toInt()}s", Color(0xFFFB923C)) }
        }
        if (mode.infiniteTurbo) {
          item { ModeTag("⚡ Turbo Livre", Color(0xFFFDE047)) }
        }
        if (mode.infiniteSonic) {
          item { ModeTag("🔔 Onda Livre", Color(0xFFE879F9)) }
        }
        if (mode.isDarknessMode) {
          item { ModeTag("🔦 Escuridão", Color(0xFF94A3B8)) }
        }
      }

      if (!isUnlocked) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "🔓 Como liberar: ${mode.unlockDescription}",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFFFBBF24)
        )
        if (mode.unlockTarget > 1) {
          Spacer(modifier = Modifier.height(4.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
              progress = { (currentProgress.toFloat() / mode.unlockTarget.toFloat()).coerceIn(0f, 1f) },
              modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
              color = Color(0xFFFBBF24),
              trackColor = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "$currentProgress/${mode.unlockTarget}",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF94A3B8)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ModeTag(text: String, color: Color) {
  Surface(
    color = color.copy(alpha = 0.14f),
    shape = RoundedCornerShape(8.dp),
    border = BorderStroke(0.8.dp, color.copy(alpha = 0.5f))
  ) {
    Text(
      text = text,
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold,
      color = color,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
    )
  }
}
