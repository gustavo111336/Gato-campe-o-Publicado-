package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.model.ALL_HERO_SKINS
import com.example.model.HeroSkinDef

@Composable
fun SkinsDialog(
  uiState: GameUiState,
  isSkinUnlocked: (HeroSkinDef) -> Boolean,
  onSelectSkin: (String) -> Unit,
  onOpenShop: () -> Unit,
  onDismiss: () -> Unit
) {
  val totalSkins = ALL_HERO_SKINS.size
  val unlockedCount = uiState.unlockedSkinsCount

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.90f)
        .padding(4.dp)
        .shadow(24.dp, RoundedCornerShape(24.dp))
        .border(2.dp, Color(0xFFE879F9), RoundedCornerShape(24.dp))
        .testTag("skins_dialog"),
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
              text = "👗 Skins do Gato Herói",
              fontSize = 19.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFFE879F9)
            )
            Text(
              text = "$unlockedCount de $totalSkins skins desbloqueadas",
              fontSize = 11.sp,
              color = Color(0xFF94A3B8)
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_skins_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Fechar Skins",
              tint = Color.White
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
          progress = { (unlockedCount.toFloat() / totalSkins.toFloat()).coerceIn(0f, 1f) },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
          color = Color(0xFFE879F9),
          trackColor = Color(0xFF1E293B)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f)
        ) {
          items(ALL_HERO_SKINS, key = { it.id }) { skin ->
            val unlocked = isSkinUnlocked(skin)
            val progress = skin.currentProgress(
              wins = uiState.championCount,
              totalCats = uiState.totalCatsSaved,
              bestEndless = uiState.bestEndlessCats,
              achievementsCount = uiState.unlockedAchievements.size,
              discoveredItemsCount = uiState.discoveredItems.size,
              ghostsScared = uiState.totalGhostsScared,
              dashesUsed = uiState.totalDashesUsed,
              modesUnlocked = uiState.unlockedModesCount,
              hasCreatedCustom = uiState.hasCreatedCustomContent
            )
            val isSelected = uiState.selectedSkinId == skin.id

            SkinCard(
              skin = skin,
              isUnlocked = unlocked,
              isSelected = isSelected,
              currentProgress = progress,
              onSelect = { onSelectSkin(skin.id) },
              onOpenShop = onOpenShop
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
private fun SkinCard(
  skin: HeroSkinDef,
  isUnlocked: Boolean,
  isSelected: Boolean,
  currentProgress: Int,
  onSelect: () -> Unit,
  onOpenShop: () -> Unit
) {
  val borderColor = when {
    isSelected -> Color(0xFFFDE047)
    isUnlocked -> skin.furColor
    else -> Color(0xFF334155)
  }
  val bgColor = when {
    isSelected -> Color(0xFF312E81)
    isUnlocked -> Color(0xFF1E293B)
    else -> Color(0xFF131C2E)
  }

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = bgColor,
    border = BorderStroke(if (isSelected) 2.dp else 1.5.dp, borderColor),
    modifier = Modifier
      .fillMaxWidth()
      .then(if (isUnlocked) Modifier.clickable(onClick = onSelect) else Modifier)
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        // Skin Preview Swatch
        Box(
          modifier = Modifier
            .size(54.dp)
            .background(skin.furColor.copy(alpha = if (isUnlocked) 1f else 0.35f), CircleShape)
            .border(3.dp, skin.capeColor.copy(alpha = if (isUnlocked) 1f else 0.4f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = if (isUnlocked) skin.badgeEmoji else "🔒",
            fontSize = 24.sp
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "#${skin.number} ${skin.name}",
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            color = if (isSelected) Color(0xFFFDE047) else if (isUnlocked) Color.White else Color(0xFFCBD5E1)
          )
          Text(
            text = skin.description,
            fontSize = 11.sp,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(top = 2.dp)
          )
          Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp)
          ) {
            ColorDot("Pelo", skin.furColor)
            ColorDot("Capa", skin.capeColor)
            ColorDot("Olhos", skin.eyeColor)
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (isUnlocked) {
          Button(
            onClick = onSelect,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isSelected) Color(0xFF10B981) else Color(0xFF0284C7)
            ),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier = Modifier
              .defaultMinSize(minHeight = 48.dp)
              .testTag("equip_skin_${skin.id}")
          ) {
            Text(
              text = if (isSelected) "USANDO ✅" else "EQUIPAR",
              fontSize = 10.sp,
              fontWeight = FontWeight.Black,
              color = Color.White
            )
          }
        }
      }

      if (!isUnlocked) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "🔓 ${skin.unlockDescription}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFBBF24),
            modifier = Modifier.weight(1f)
          )
          TextButton(onClick = onOpenShop) {
            Text("🛒 Comprar/Trocar", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF34D399))
          }
        }
        if (skin.unlockTarget > 1) {
          Spacer(modifier = Modifier.height(4.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
              progress = { (currentProgress.toFloat() / skin.unlockTarget.toFloat()).coerceIn(0f, 1f) },
              modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
              color = Color(0xFFE879F9),
              trackColor = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "$currentProgress/${skin.unlockTarget}",
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
private fun ColorDot(label: String, color: Color) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .size(10.dp)
        .background(color, CircleShape)
        .border(0.8.dp, Color.White.copy(alpha = 0.7f), CircleShape)
    )
    Spacer(modifier = Modifier.width(3.dp))
    Text(label, fontSize = 9.sp, color = Color(0xFF94A3B8))
  }
}
