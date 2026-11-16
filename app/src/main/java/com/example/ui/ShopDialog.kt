package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.ALL_40_ITEMS
import com.example.model.ALL_40_MODES
import com.example.model.ALL_HERO_SKINS
import com.example.model.HeroSkinDef

private enum class ShopTab(val label: String) {
  SKINS("👗 Skins"),
  MODES("🎮 Modos"),
  ITEMS("🎁 Itens"),
  REVIVES("💖 Revive")
}

@Composable
fun ShopDialog(
  uiState: GameUiState,
  tradableSkins: List<HeroSkinDef>,
  isSkinUnlocked: (HeroSkinDef) -> Boolean,
  isModeUnlocked: (com.example.model.GameModeDef) -> Boolean,
  shopCostForSkin: (HeroSkinDef) -> Int,
  shopCostForMode: (com.example.model.GameModeDef) -> Int,
  shopCostForItem: (com.example.model.GameItemDef) -> Int,
  onBuySkin: (String, String?) -> Unit,
  onBuyMode: (String, String?) -> Unit,
  onBuyItem: (String, String?) -> Unit,
  onBuyRevives: (Int, Int, String?) -> Unit,
  onDismiss: () -> Unit
) {
  var selectedTab by remember { mutableStateOf(ShopTab.SKINS) }
  var selectedTradeSkinId by remember(tradableSkins) {
    mutableStateOf(tradableSkins.firstOrNull()?.id)
  }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.92f)
        .padding(4.dp)
        .shadow(24.dp, RoundedCornerShape(24.dp))
        .border(2.dp, Color(0xFFF59E0B), RoundedCornerShape(24.dp))
        .testTag("shop_dialog"),
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
              text = "🛒 Loja do Campeão",
              fontSize = 19.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFFFDE047)
            )
            Text(
              text = "Saldo: 🪙 ${uiState.spendablePoints} pts • 💖 ${uiState.reviveCount} Revive(s)",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF34D399)
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_shop_button")
          ) {
            Icon(Icons.Default.Close, contentDescription = "Fechar Loja", tint = Color.White)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Trade-in skin selector banner (if user owns any non-default skins)
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Color(0xFF1E1B4B),
          border = BorderStroke(1.dp, Color(0xFFC084FC)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text(
              text = if (tradableSkins.isEmpty()) {
                "🔄 Troca por Skin: desbloqueie mais skins jogando para poder trocar!"
              } else {
                "🔄 Troca Rápida: selecione uma skin sua para trocar por algo bloqueado:"
              },
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFE9D5FF)
            )
            if (tradableSkins.isNotEmpty()) {
              Spacer(modifier = Modifier.height(6.dp))
              LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(tradableSkins, key = { it.id }) { s ->
                  val active = selectedTradeSkinId == s.id
                  FilterChip(
                    selected = active,
                    onClick = { selectedTradeSkinId = s.id },
                    label = { Text("${s.badgeEmoji} ${s.name}", fontSize = 11.sp) }
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tabs
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          ShopTab.values().forEach { tab ->
            val selected = selectedTab == tab
            Button(
              onClick = { selectedTab = tab },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (selected) Color(0xFFD97706) else Color(0xFF1E293B)
              ),
              shape = RoundedCornerShape(12.dp),
              contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
              modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
            ) {
              Text(tab.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f)
        ) {
          when (selectedTab) {
            ShopTab.SKINS -> {
              val lockedSkins = ALL_HERO_SKINS.filter { !isSkinUnlocked(it) }
              if (lockedSkins.isEmpty()) {
                item {
                  EmptyShopCard("🎉 Parabéns! Você já possui TODAS as skins do jogo!")
                }
              } else {
                items(lockedSkins, key = { it.id }) { skin ->
                  val cost = shopCostForSkin(skin)
                  val canPayPoints = uiState.spendablePoints >= cost
                  val tradeCandidate = tradableSkins.firstOrNull { it.id == selectedTradeSkinId && it.id != skin.id }
                  ShopOfferCard(
                    emoji = skin.badgeEmoji,
                    accentColor = skin.furColor,
                    title = "#${skin.number} ${skin.name}",
                    subtitle = "${skin.description}\nDesbloqueio grátis: ${skin.unlockDescription}",
                    pointCost = cost,
                    canPayPoints = canPayPoints,
                    tradeLabel = tradeCandidate?.let { "🔄 Trocar ${it.badgeEmoji}" },
                    onBuyWithPoints = { onBuySkin(skin.id, null) },
                    onTradeSkin = {
                      tradeCandidate?.let { onBuySkin(skin.id, it.id) }
                    },
                    buyTag = "buy_skin_${skin.id}"
                  )
                }
              }
            }

            ShopTab.MODES -> {
              val lockedModes = ALL_40_MODES.filter { !isModeUnlocked(it) }
              if (lockedModes.isEmpty()) {
                item {
                  EmptyShopCard("🎉 Todos os 40 modos já estão desbloqueados!")
                }
              } else {
                items(lockedModes, key = { it.id }) { mode ->
                  val cost = shopCostForMode(mode)
                  val canPayPoints = uiState.spendablePoints >= cost
                  val tradeCandidate = tradableSkins.firstOrNull { it.id == selectedTradeSkinId }
                  ShopOfferCard(
                    emoji = mode.emoji,
                    accentColor = Color(0xFF38BDF8),
                    title = "#${mode.number} ${mode.name}",
                    subtitle = "${mode.objectiveText}\nDesbloqueio grátis: ${mode.unlockDescription}",
                    pointCost = cost,
                    canPayPoints = canPayPoints,
                    tradeLabel = tradeCandidate?.let { "🔄 Trocar ${it.badgeEmoji}" },
                    onBuyWithPoints = { onBuyMode(mode.id, null) },
                    onTradeSkin = {
                      tradeCandidate?.let { onBuyMode(mode.id, it.id) }
                    },
                    buyTag = "buy_mode_${mode.id}"
                  )
                }
              }
            }

            ShopTab.ITEMS -> {
              val lockedItems = ALL_40_ITEMS.filter { !uiState.discoveredItems.contains(it.id) }
              if (lockedItems.isEmpty()) {
                item {
                  EmptyShopCard("🎉 Todos os 40 itens já estão na sua coleção!")
                }
              } else {
                items(lockedItems, key = { it.id }) { item ->
                  val cost = shopCostForItem(item)
                  val canPayPoints = uiState.spendablePoints >= cost
                  val tradeCandidate = tradableSkins.firstOrNull { it.id == selectedTradeSkinId }
                  ShopOfferCard(
                    emoji = item.emoji,
                    accentColor = item.color,
                    title = "#${item.number} ${item.name}",
                    subtitle = "${item.description} (${item.effectSummary})",
                    pointCost = cost,
                    canPayPoints = canPayPoints,
                    tradeLabel = tradeCandidate?.let { "🔄 Trocar ${it.badgeEmoji}" },
                    onBuyWithPoints = { onBuyItem(item.id, null) },
                    onTradeSkin = {
                      tradeCandidate?.let { onBuyItem(item.id, it.id) }
                    },
                    buyTag = "buy_item_${item.id}"
                  )
                }
              }
            }

            ShopTab.REVIVES -> {
              item {
                Surface(
                  shape = RoundedCornerShape(14.dp),
                  color = Color(0xFF3B0764),
                  border = BorderStroke(1.dp, Color(0xFFF472B6)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(
                    text = "💖 Seu estoque atual: ${uiState.reviveCount} Revive(s). Use um Revive quando perder todas as vidas para continuar na mesma partida!",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFBCFE8),
                    modifier = Modifier.padding(12.dp)
                  )
                }
              }

              val revivePacks = listOf(
                Triple(1, 150, "💖 1x Coração Revive" to "Revive imediato com vidas cheias + 3,5s invencível!"),
                Triple(3, 400, "💝 Pacote 3x Revives" to "Economize pontos! Tenha 3 segundas chances na arena."),
                Triple(5, 600, "👑 Mega Pacote 5x Revives" to "Para bater recordes em modos difíceis e Bots!")
              )
              items(revivePacks, key = { it.first }) { (qty, cost, copy) ->
                val canPayPoints = uiState.spendablePoints >= cost
                val tradeCandidate = tradableSkins.firstOrNull { it.id == selectedTradeSkinId }
                ShopOfferCard(
                  emoji = if (qty == 1) "💖" else if (qty == 3) "💝" else "👑",
                  accentColor = Color(0xFFF472B6),
                  title = copy.first,
                  subtitle = copy.second,
                  pointCost = cost,
                  canPayPoints = canPayPoints,
                  tradeLabel = tradeCandidate?.let { "🔄 Trocar ${it.badgeEmoji}" },
                  onBuyWithPoints = { onBuyRevives(qty, cost, null) },
                  onTradeSkin = {
                    tradeCandidate?.let { onBuyRevives(qty, cost, it.id) }
                  },
                  buyTag = "buy_revive_$qty"
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
          onClick = onDismiss,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
          modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
          Text("Fechar Loja 🐾", color = Color.White, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun EmptyShopCard(message: String) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = Color(0xFF1E293B),
    border = BorderStroke(1.5.dp, Color(0xFF10B981)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Text(
      text = message,
      fontSize = 13.sp,
      fontWeight = FontWeight.Bold,
      color = Color(0xFF6EE7B7),
      modifier = Modifier.padding(16.dp)
    )
  }
}

@Composable
private fun ShopOfferCard(
  emoji: String,
  accentColor: Color,
  title: String,
  subtitle: String,
  pointCost: Int,
  canPayPoints: Boolean,
  tradeLabel: String?,
  onBuyWithPoints: () -> Unit,
  onTradeSkin: () -> Unit,
  buyTag: String
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = Color(0xFF1E293B),
    border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.7f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(46.dp)
            .background(accentColor.copy(alpha = 0.22f), CircleShape)
            .border(2.dp, accentColor, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Text(emoji, fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(title, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
          Text(subtitle, fontSize = 11.sp, color = Color(0xFF94A3B8), modifier = Modifier.padding(top = 2.dp))
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = onBuyWithPoints,
          enabled = canPayPoints,
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF10B981),
            disabledContainerColor = Color(0xFF334155)
          ),
          shape = RoundedCornerShape(12.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
          modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag(buyTag)
        ) {
          Text(
            text = if (canPayPoints) "🪙 Comprar ($pointCost pts)" else "🪙 Faltam pts ($pointCost)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = if (canPayPoints) Color.White else Color(0xFF94A3B8)
          )
        }

        if (tradeLabel != null) {
          OutlinedButton(
            onClick = onTradeSkin,
            border = BorderStroke(1.5.dp, Color(0xFFE879F9)),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            modifier = Modifier.weight(0.95f).defaultMinSize(minHeight = 48.dp)
          ) {
            Text(tradeLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF0ABFC))
          }
        }
      }
    }
  }
}
