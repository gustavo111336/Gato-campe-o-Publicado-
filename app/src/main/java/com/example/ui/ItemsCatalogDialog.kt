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
import com.example.model.ALL_40_ITEMS
import com.example.model.GameItemDef
import com.example.model.ItemCategory

@Composable
fun ItemsCatalogDialog(
  uiState: GameUiState,
  onOpenShop: () -> Unit,
  onDismiss: () -> Unit
) {
  var selectedCategory by remember { mutableStateOf<ItemCategory?>(null) }

  val filteredItems = remember(selectedCategory) {
    if (selectedCategory == null) ALL_40_ITEMS
    else ALL_40_ITEMS.filter { it.category == selectedCategory }
  }
  val discoveredCount = uiState.discoveredItems.size

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.90f)
        .padding(4.dp)
        .shadow(24.dp, RoundedCornerShape(24.dp))
        .border(2.dp, Color(0xFFF472B6), RoundedCornerShape(24.dp))
        .testTag("items_catalog_dialog"),
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
              text = "🎁 Arsenal dos 40 Itens",
              fontSize = 19.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFFF472B6)
            )
            Text(
              text = "$discoveredCount de ${ALL_40_ITEMS.size} itens descobertos na arena • ${uiState.totalItemsUsed} coletados",
              fontSize = 11.sp,
              color = Color(0xFF94A3B8)
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_items_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Fechar Catálogo de Itens",
              tint = Color.White
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
          progress = { (discoveredCount.toFloat() / ALL_40_ITEMS.size.toFloat()).coerceIn(0f, 1f) },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
          color = Color(0xFFF472B6),
          trackColor = Color(0xFF1E293B)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Category Filter Row
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          item {
            ItemFilterChip(
              label = "✨ Todos (${ALL_40_ITEMS.size})",
              selected = selectedCategory == null,
              onClick = { selectedCategory = null }
            )
          }
          items(ItemCategory.values()) { cat ->
            val count = ALL_40_ITEMS.count { it.category == cat }
            ItemFilterChip(
              label = "${cat.emoji} ${cat.label} ($count)",
              selected = selectedCategory == cat,
              onClick = { selectedCategory = cat }
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f)
        ) {
          items(filteredItems, key = { it.id }) { itemDef ->
            val isDiscovered = uiState.discoveredItems.contains(itemDef.id)
            ItemCatalogCard(itemDef = itemDef, isDiscovered = isDiscovered)
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
            Text("🛒 Comprar na Loja", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
private fun ItemFilterChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = if (selected) Color(0xFFDB2777) else Color(0xFF1E293B),
    border = BorderStroke(1.dp, if (selected) Color(0xFFF9A8D4) else Color(0xFF334155)),
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
private fun ItemCatalogCard(
  itemDef: GameItemDef,
  isDiscovered: Boolean
) {
  val borderColor = if (isDiscovered) itemDef.color else Color(0xFF334155)
  val bgColor = if (isDiscovered) Color(0xFF1E293B) else Color(0xFF131C2E)

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
          .size(50.dp)
          .background(itemDef.color.copy(alpha = if (isDiscovered) 0.25f else 0.12f), CircleShape)
          .border(2.dp, itemDef.color.copy(alpha = if (isDiscovered) 1f else 0.45f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Text(text = itemDef.emoji, fontSize = 24.sp)
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "#${itemDef.number} ${itemDef.name}",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (isDiscovered) Color.White else Color(0xFFCBD5E1),
            modifier = Modifier.weight(1f)
          )
          Surface(
            color = if (isDiscovered) Color(0xFF065F46) else Color(0xFF1E293B),
            shape = RoundedCornerShape(8.dp)
          ) {
            Text(
              text = if (isDiscovered) "✅ DESCOBERTO" else "🔍 NA ARENA",
              fontSize = 9.sp,
              fontWeight = FontWeight.Black,
              color = if (isDiscovered) Color(0xFF6EE7B7) else Color(0xFF94A3B8),
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
          }
        }

        Text(
          text = itemDef.description,
          fontSize = 11.sp,
          color = Color(0xFF94A3B8),
          modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(5.dp))

        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            color = itemDef.color.copy(alpha = 0.18f),
            shape = RoundedCornerShape(8.dp)
          ) {
            Text(
              text = "⚡ ${itemDef.effectSummary}",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = itemDef.color,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            )
          }
          Surface(
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(8.dp)
          ) {
            Text(
              text = "${itemDef.category.emoji} ${itemDef.category.label}",
              fontSize = 10.sp,
              color = Color(0xFFCBD5E1),
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            )
          }
        }
      }
    }
  }
}
