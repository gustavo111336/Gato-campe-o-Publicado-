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
import com.example.model.ALL_40_ACHIEVEMENTS
import com.example.model.Achievement
import com.example.model.AchievementProgressType

private enum class AchievementFilter {
  ALL, UNLOCKED, LOCKED
}

@Composable
fun AchievementsDialog(
  uiState: GameUiState,
  onDismiss: () -> Unit
) {
  var filter by remember { mutableStateOf(AchievementFilter.ALL) }
  val unlockedCount = uiState.unlockedAchievements.size
  val totalCount = ALL_40_ACHIEVEMENTS.size

  val displayedList = remember(filter, uiState.unlockedAchievements) {
    ALL_40_ACHIEVEMENTS.filter { ach ->
      val isUnlocked = uiState.unlockedAchievements.contains(ach.id)
      when (filter) {
        AchievementFilter.ALL -> true
        AchievementFilter.UNLOCKED -> isUnlocked
        AchievementFilter.LOCKED -> !isUnlocked
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
        .border(2.dp, Color(0xFFFACC15), RoundedCornerShape(24.dp))
        .testTag("achievements_dialog"),
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
              text = "🏅 40 Conquistas Felinas",
              fontSize = 19.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFFFDE047)
            )
            Text(
              text = "$unlockedCount de $totalCount conquistas desbloqueadas",
              fontSize = 12.sp,
              color = Color(0xFF94A3B8)
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_achievements_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Fechar Conquistas",
              tint = Color.White
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
          progress = { (unlockedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f) },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
          color = Color(0xFFFACC15),
          trackColor = Color(0xFF1E293B)
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          item {
            AchFilterChip(
              label = "🏅 Todas ($totalCount)",
              selected = filter == AchievementFilter.ALL,
              onClick = { filter = AchievementFilter.ALL }
            )
          }
          item {
            AchFilterChip(
              label = "✅ Liberadas ($unlockedCount)",
              selected = filter == AchievementFilter.UNLOCKED,
              onClick = { filter = AchievementFilter.UNLOCKED }
            )
          }
          item {
            AchFilterChip(
              label = "🔒 Faltam (${totalCount - unlockedCount})",
              selected = filter == AchievementFilter.LOCKED,
              onClick = { filter = AchievementFilter.LOCKED }
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f)
        ) {
          items(displayedList, key = { it.id }) { ach ->
            val isUnlocked = uiState.unlockedAchievements.contains(ach.id)
            val rawProgress = when (ach.progressType) {
              AchievementProgressType.WINS -> uiState.championCount
              AchievementProgressType.TOTAL_CATS -> uiState.totalCatsSaved
              AchievementProgressType.ENDLESS_BEST -> uiState.bestEndlessCats
              AchievementProgressType.ITEMS_DISCOVERED -> uiState.discoveredItems.size
              AchievementProgressType.TOTAL_ITEMS_USED -> uiState.totalItemsUsed
              AchievementProgressType.GHOSTS_SCARED -> uiState.totalGhostsScared
              AchievementProgressType.DASHES_USED -> uiState.totalDashesUsed
              AchievementProgressType.SONICS_USED -> uiState.totalSonicsUsed
              AchievementProgressType.MODES_UNLOCKED -> uiState.unlockedModesCount
              AchievementProgressType.SINGLE_EVENT -> if (isUnlocked) 1 else 0
            }
            val currentProgress = rawProgress.coerceAtMost(ach.targetValue)
            AchievementCard(
              achievement = ach,
              isUnlocked = isUnlocked,
              currentProgress = currentProgress
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
          onClick = onDismiss,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
          modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
          Text("Voltar 🐾", color = Color.White, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun AchFilterChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = if (selected) Color(0xFFCA8A04) else Color(0xFF1E293B),
    border = BorderStroke(1.dp, if (selected) Color(0xFFFDE047) else Color(0xFF334155)),
    modifier = Modifier.clickable(onClick = onClick)
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

@Composable
private fun AchievementCard(
  achievement: Achievement,
  isUnlocked: Boolean,
  currentProgress: Int
) {
  val borderColor = when {
    isUnlocked && achievement.isMilestone -> Color(0xFFFACC15)
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
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(48.dp)
          .background(
            if (isUnlocked) Color(0xFF312E81) else Color(0xFF0F172A),
            CircleShape
          )
          .border(2.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = if (isUnlocked) achievement.emoji else "🔒",
          fontSize = 22.sp
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "#${achievement.number} ${achievement.title}",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (isUnlocked) Color(0xFFFDE047) else Color(0xFFCBD5E1),
            modifier = Modifier.weight(1f)
          )
          if (isUnlocked) {
            Text(
              text = "DESBLOQUEADA ✅",
              fontSize = 9.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFF4ADE80)
            )
          }
        }

        Text(
          text = achievement.description,
          fontSize = 11.sp,
          color = Color(0xFF94A3B8),
          modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = "🎁 ${achievement.rewardText}",
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          color = if (isUnlocked) Color(0xFF38BDF8) else Color(0xFF64748B)
        )

        if (!isUnlocked && achievement.targetValue > 1) {
          Spacer(modifier = Modifier.height(6.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
              progress = { (currentProgress.toFloat() / achievement.targetValue.toFloat()).coerceIn(0f, 1f) },
              modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
              color = Color(0xFF38BDF8),
              trackColor = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "$currentProgress/${achievement.targetValue}",
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
