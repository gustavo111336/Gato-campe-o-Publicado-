package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.*
import kotlin.math.hypot
import kotlin.random.Random

private enum class CreatorTab(val label: String) {
  MAP("🗺️ Criar Mapa"),
  ITEM("🧪 Criar Item")
}

private val ITEM_EMOJIS = listOf(
  "🧪", "🍕", "🍪", "💎", "🛸", "⚡", "🔥", "❄️", "🌈", "👑",
  "🧲", "👻", "🐟", "🚀", "🎯", "🧀", "🍓", "⭐", "🪄", "🛡️"
)

private val ITEM_COLORS = listOf(
  0xFF10B981L, 0xFFF472B6L, 0xFF38BDF8L, 0xFFFACC15L,
  0xFFA855F7L, 0xFFEF4444L, 0xFFFB923CL, 0xFF22D3EEL
)

private val CREATOR_EFFECTS = listOf(
  ItemEffectType.SPEED_BOOST to "⚡ Super Velocidade",
  ItemEffectType.INVISIBILITY to "🫥 Invisibilidade",
  ItemEffectType.DOUBLE_CAT_VALUE to "🏅 Gatos Valem x2",
  ItemEffectType.FREEZE_GHOSTS to "❄️ Congela Fantasmas",
  ItemEffectType.GHOST_FLEE to "🐟 Fantasmas Fogem",
  ItemEffectType.SHIELD to "🥛 Escudo Protetor",
  ItemEffectType.GIANT_MAGNET to "🪐 Super Ímã Global",
  ItemEffectType.INVINCIBILITY_STAR to "🌟 Invencibilidade",
  ItemEffectType.TURBO_OVERDRIVE to "🔋 Turbo Infinito",
  ItemEffectType.SONIC_OVERDRIVE to "🔔 Onda Sônica Livre",
  ItemEffectType.LUCKY_RAIN to "🌈 Chuva de Gatos & Itens",
  ItemEffectType.TELEPORT_SAFE to "🪶 Teleporte Seguro"
)

@Composable
fun CreatorStudioDialog(
  uiState: GameUiState,
  onSaveCustomMap: (CustomMapConfig, Boolean) -> Unit,
  onSaveCustomItem: (CustomItemConfig) -> Unit,
  onDeleteCustomItem: (String) -> Unit,
  onToggleSpawnCustomItems: () -> Unit,
  onDismiss: () -> Unit
) {
  var activeTab by remember { mutableStateOf(CreatorTab.MAP) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.92f)
        .padding(4.dp)
        .shadow(24.dp, RoundedCornerShape(24.dp))
        .border(2.dp, Color(0xFF34D399), RoundedCornerShape(24.dp))
        .testTag("creator_studio_dialog"),
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
              text = "🛠️ Estúdio Criativo",
              fontSize = 19.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFF34D399)
            )
            Text(
              text = "Crie seu próprio mapa e seus próprios itens mágicos!",
              fontSize = 11.sp,
              color = Color(0xFF94A3B8)
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_creator_button")
          ) {
            Icon(Icons.Default.Close, contentDescription = "Fechar Estúdio", tint = Color.White)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tab Switcher
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          CreatorTab.values().forEach { tab ->
            val selected = activeTab == tab
            Button(
              onClick = { activeTab = tab },
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = if (selected) Color(0xFF059669) else Color(0xFF1E293B)
              ),
              border = BorderStroke(1.5.dp, if (selected) Color(0xFF6EE7B7) else Color(0xFF334155)),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
            ) {
              Text(
                text = tab.label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.weight(1f)) {
          when (activeTab) {
            CreatorTab.MAP -> {
              MapCreatorTab(
                initialMap = uiState.customMap,
                onSaveAndPlay = onSaveCustomMap
              )
            }
            CreatorTab.ITEM -> {
              ItemCreatorTab(
                savedItems = uiState.customItems,
                spawnInAllModes = uiState.spawnCustomItemsInAllModes,
                onSaveItem = onSaveCustomItem,
                onDeleteItem = onDeleteCustomItem,
                onToggleSpawnInAllModes = onToggleSpawnCustomItems
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MapCreatorTab(
  initialMap: CustomMapConfig,
  onSaveAndPlay: (CustomMapConfig, Boolean) -> Unit
) {
  var mapName by remember { mutableStateOf(initialMap.name) }
  var paletteId by remember { mutableStateOf(initialMap.paletteId) }
  var goalCats by remember { mutableStateOf(initialMap.goalCats) }
  var isEndless by remember { mutableStateOf(initialMap.isEndless) }
  var ghostCount by remember { mutableStateOf(initialMap.ghostCount) }
  var isDarkness by remember { mutableStateOf(initialMap.isDarknessMode) }
  var obstacles by remember { mutableStateOf(initialMap.obstacles) }
  var selectedBrush by remember { mutableStateOf("TREE") }

  val palette = CUSTOM_MAP_PALETTES.find { it.id == paletteId } ?: CUSTOM_MAP_PALETTES.first()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
  ) {
    OutlinedTextField(
      value = mapName,
      onValueChange = { if (it.length <= 26) mapName = it },
      label = { Text("Nome do Seu Mapa", color = Color(0xFF94A3B8), fontSize = 12.sp) },
      singleLine = true,
      colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = Color(0xFF34D399),
        unfocusedBorderColor = Color(0xFF334155)
      ),
      modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(10.dp))

    Text("🎨 Cores & Tema do Mapa", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
    Spacer(modifier = Modifier.height(6.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(CUSTOM_MAP_PALETTES) { pal ->
        val selected = pal.id == paletteId
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (selected) pal.wallColor else Color(0xFF1E293B),
          border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) pal.wallHighlight else Color(0xFF334155)),
          modifier = Modifier.clickable { paletteId = pal.id }
        ) {
          Text(
            text = "${pal.emoji} ${pal.name}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Text(
      text = "🧱 Toque no mapa abaixo para colocar ou apagar obstáculos (${obstacles.size}/16)",
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      color = Color(0xFFFDE047)
    )
    Spacer(modifier = Modifier.height(6.dp))

    // Brush Selector
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      val brushes = listOf(
        "FOUNTAIN" to "⛲ Fonte",
        "TREE" to "🌳 Árvore",
        "FLOWER_PATCH" to "🌸 Flores",
        "YARN" to "🧶 Novelo",
        "ERASE" to "🗑️ Apagar"
      )
      items(brushes) { (id, label) ->
        val sel = selectedBrush == id
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (sel) (if (id == "ERASE") Color(0xFFDC2626) else Color(0xFF0284C7)) else Color(0xFF1E293B),
          border = BorderStroke(1.dp, if (sel) Color.White else Color(0xFF334155)),
          modifier = Modifier.clickable { selectedBrush = id }
        ) {
          Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Interactive Mini-Arena Canvas
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(210.dp)
        .clip(RoundedCornerShape(16.dp))
        .border(2.dp, palette.wallHighlight, RoundedCornerShape(16.dp))
    ) {
      Canvas(
        modifier = Modifier
          .fillMaxSize()
          .pointerInput(selectedBrush, obstacles) {
            detectTapGestures { offset ->
              val rx = (offset.x / size.width).coerceIn(0.10f, 0.90f)
              val ry = (offset.y / size.height).coerceIn(0.10f, 0.90f)
              if (selectedBrush == "ERASE") {
                val nearest = obstacles.minByOrNull { hypot(it.xRatio - rx, it.yRatio - ry) }
                if (nearest != null && hypot(nearest.xRatio - rx, nearest.yRatio - ry) < 0.14f) {
                  obstacles = obstacles.filterNot { it == nearest }
                }
              } else if (obstacles.size < 16) {
                obstacles = obstacles + CustomObstacleSpec(rx, ry, selectedBrush)
              }
            }
          }
      ) {
        drawRect(
          brush = Brush.verticalGradient(listOf(palette.bgTop, palette.bgMid, palette.bgBottom))
        )
        val stepX = size.width / 8f
        val stepY = size.height / 6f
        for (i in 1..7) {
          drawLine(palette.gridColor.copy(alpha = 0.4f), Offset(i * stepX, 0f), Offset(i * stepX, size.height), 1f)
        }
        for (j in 1..5) {
          drawLine(palette.gridColor.copy(alpha = 0.4f), Offset(0f, j * stepY), Offset(size.width, j * stepY), 1f)
        }
        drawRect(palette.wallColor, style = Stroke(width = 8f))

        for (obs in obstacles) {
          val cx = obs.xRatio * size.width
          val cy = obs.yRatio * size.height
          val color = when (obs.type) {
            "FOUNTAIN" -> Color(0xFF38BDF8)
            "TREE" -> Color(0xFFD97706)
            "FLOWER_PATCH" -> Color(0xFF22C55E)
            else -> Color(0xFFA855F7)
          }
          val r = if (obs.type == "FOUNTAIN") 15f else 11f
          drawCircle(color, radius = r, center = Offset(cx, cy))
          drawCircle(Color.White.copy(alpha = 0.6f), radius = r, center = Offset(cx, cy), style = Stroke(2f))
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      OutlinedButton(
        onClick = {
          val types = listOf("FOUNTAIN", "TREE", "FLOWER_PATCH", "YARN")
          obstacles = (1..7).map {
            CustomObstacleSpec(
              xRatio = Random.nextFloat() * 0.72f + 0.14f,
              yRatio = Random.nextFloat() * 0.72f + 0.14f,
              type = types[Random.nextInt(types.size)]
            )
          }
        },
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.weight(1f)
      ) {
        Text("🎲 Aleatório", fontSize = 11.sp, color = Color(0xFF38BDF8))
      }
      OutlinedButton(
        onClick = { obstacles = emptyList() },
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.weight(1f)
      ) {
        Text("🧹 Limpar Tudo", fontSize = 11.sp, color = Color(0xFFF87171))
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Map Rules
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = Color(0xFF1E293B),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("♾️ Modo Infinito no Meu Mapa", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
          Switch(checked = isEndless, onCheckedChange = { isEndless = it })
        }
        if (!isEndless) {
          Text("🐱 Meta de Gatos: $goalCats", fontSize = 12.sp, color = Color(0xFFFDE047), fontWeight = FontWeight.Bold)
          Slider(
            value = goalCats.toFloat(),
            onValueChange = { goalCats = it.toInt() },
            valueRange = 5f..50f
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("👻 Fantasmas: $ghostCount", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(1, 2, 3).forEach { cnt ->
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (ghostCount == cnt) Color(0xFF7E22CE) else Color(0xFF0F172A),
                border = BorderStroke(1.dp, if (ghostCount == cnt) Color(0xFFC084FC) else Color(0xFF334155)),
                modifier = Modifier.clickable { ghostCount = cnt }
              ) {
                Text("$cnt", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("🔦 Escuridão com Lanterna", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
          Switch(checked = isDarkness, onCheckedChange = { isDarkness = it })
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Button(
      onClick = {
        val cfg = CustomMapConfig(
          name = mapName.ifBlank { "Meu Mapa Secreto" },
          paletteId = paletteId,
          goalCats = goalCats,
          isEndless = isEndless,
          ghostCount = ghostCount,
          isDarknessMode = isDarkness,
          obstacles = obstacles
        )
        onSaveAndPlay(cfg, true)
      },
      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
        .testTag("play_custom_map_button")
    ) {
      Text("💾 SALVAR & JOGAR MEU MAPA ▶", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
    }
  }
}

@Composable
private fun ItemCreatorTab(
  savedItems: List<CustomItemConfig>,
  spawnInAllModes: Boolean,
  onSaveItem: (CustomItemConfig) -> Unit,
  onDeleteItem: (String) -> Unit,
  onToggleSpawnInAllModes: () -> Unit
) {
  var itemName by remember { mutableStateOf("Super Poção Miau") }
  var selectedEmoji by remember { mutableStateOf(ITEM_EMOJIS.first()) }
  var selectedColor by remember { mutableStateOf(ITEM_COLORS[1]) }
  var selectedEffect by remember { mutableStateOf(ItemEffectType.SPEED_BOOST) }
  var durationSec by remember { mutableStateOf(8f) }
  var powerMagnitude by remember { mutableStateOf(1.7f) }
  var justSavedMsg by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
  ) {
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = Color(0xFF1E293B),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("✨ Ativar meus itens nas arenas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
          Text("Faz seus itens criados nascerem durante as partidas", fontSize = 10.sp, color = Color(0xFF94A3B8))
        }
        Switch(checked = spawnInAllModes, onCheckedChange = { onToggleSpawnInAllModes() })
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    OutlinedTextField(
      value = itemName,
      onValueChange = { if (it.length <= 24) { itemName = it; justSavedMsg = false } },
      label = { Text("Nome do Novo Item", color = Color(0xFF94A3B8), fontSize = 12.sp) },
      singleLine = true,
      colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = Color(0xFFF472B6),
        unfocusedBorderColor = Color(0xFF334155)
      ),
      modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(10.dp))

    Text("😀 Ícone do Item", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFDE047))
    Spacer(modifier = Modifier.height(6.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(ITEM_EMOJIS) { emoji ->
        val sel = selectedEmoji == emoji
        Box(
          modifier = Modifier
            .size(44.dp)
            .background(if (sel) Color(selectedColor).copy(alpha = 0.35f) else Color(0xFF1E293B), CircleShape)
            .border(2.dp, if (sel) Color(selectedColor) else Color(0xFF334155), CircleShape)
            .clickable { selectedEmoji = emoji; justSavedMsg = false },
          contentAlignment = Alignment.Center
        ) {
          Text(emoji, fontSize = 20.sp)
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Text("🎨 Cor do Brilho", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
    Spacer(modifier = Modifier.height(6.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      items(ITEM_COLORS) { col ->
        val sel = selectedColor == col
        Box(
          modifier = Modifier
            .size(34.dp)
            .background(Color(col), CircleShape)
            .border(if (sel) 3.dp else 1.dp, if (sel) Color.White else Color(0xFF334155), CircleShape)
            .clickable { selectedColor = col; justSavedMsg = false }
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Text("⚡ Poder do Item", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC084FC))
    Spacer(modifier = Modifier.height(6.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items(CREATOR_EFFECTS) { (eff, label) ->
        val sel = selectedEffect == eff
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (sel) Color(0xFF7C3AED) else Color(0xFF1E293B),
          border = BorderStroke(1.dp, if (sel) Color(0xFFE879F9) else Color(0xFF334155)),
          modifier = Modifier.clickable { selectedEffect = eff; justSavedMsg = false }
        ) {
          Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Surface(
      shape = RoundedCornerShape(14.dp),
      color = Color(0xFF1E293B),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Text("⏱️ Duração do Efeito: ${durationSec.toInt()}s", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
        Slider(
          value = durationSec,
          onValueChange = { durationSec = it },
          valueRange = 3f..20f
        )
        Text("💥 Força (Velocidade): ${String.format("%.1fx", powerMagnitude)}", fontSize = 12.sp, color = Color(0xFFFDE047), fontWeight = FontWeight.Bold)
        Slider(
          value = powerMagnitude,
          onValueChange = { powerMagnitude = it },
          valueRange = 1.2f..2.5f
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Button(
      onClick = {
        val newItem = CustomItemConfig(
          id = "custom_item_${System.currentTimeMillis()}",
          name = itemName.ifBlank { "Item Mágico" },
          emoji = selectedEmoji,
          colorArgb = selectedColor,
          effectType = selectedEffect,
          durationSec = durationSec,
          powerMagnitude = powerMagnitude
        )
        onSaveItem(newItem)
        justSavedMsg = true
      },
      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDB2777)),
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("save_custom_item_button")
    ) {
      Text("➕ CRIAR & SALVAR ITEM NO JOGO", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
    }

    if (justSavedMsg) {
      Text(
        text = "✅ Item salvo! Ele já pode aparecer na arena!",
        color = Color(0xFF4ADE80),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 6.dp)
      )
    }

    if (savedItems.isNotEmpty()) {
      Spacer(modifier = Modifier.height(14.dp))
      Text("🎒 Seus Itens Criados (${savedItems.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
      Spacer(modifier = Modifier.height(6.dp))
      for ((idx, item) in savedItems.withIndex()) {
        val def = item.toGameItemDef(idx)
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Color(0xFF1E293B),
          border = BorderStroke(1.dp, def.color),
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .background(def.color.copy(alpha = 0.25f), CircleShape)
                .border(1.5.dp, def.color, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Text(def.emoji, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(def.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
              Text(def.effectSummary, color = def.color, fontSize = 11.sp)
            }
            IconButton(onClick = { onDeleteItem(item.id) }) {
              Icon(Icons.Default.Delete, contentDescription = "Remover Item", tint = Color(0xFFF87171))
            }
          }
        }
      }
    }
  }
}
