package com.example.model

import androidx.compose.ui.graphics.Color

data class CustomMapPalette(
  val id: String,
  val name: String,
  val emoji: String,
  val bgTop: Color,
  val bgMid: Color,
  val bgBottom: Color,
  val gridColor: Color,
  val wallColor: Color,
  val wallHighlight: Color
)

val CUSTOM_MAP_PALETTES = listOf(
  CustomMapPalette("forest", "Floresta", "🌿", Color(0xFF064E3B), Color(0xFF022C22), Color(0xFF042F2E), Color(0xFF065F46), Color(0xFF047857), Color(0xFF34D399)),
  CustomMapPalette("magma", "Vulcão", "🌋", Color(0xFF7F1D1D), Color(0xFF450A0A), Color(0xFF2A0808), Color(0xFF991B1B), Color(0xFFDC2626), Color(0xFFFBBF24)),
  CustomMapPalette("galaxy", "Galáxia", "🌌", Color(0xFF3B0764), Color(0xFF1E1B4B), Color(0xFF0F172A), Color(0xFF6B21A8), Color(0xFF7E22CE), Color(0xFFE879F9)),
  CustomMapPalette("ice", "Polo Norte", "❄️", Color(0xFF0C4A6E), Color(0xFF082F49), Color(0xFF164E63), Color(0xFF0284C7), Color(0xFF38BDF8), Color(0xFFE0F2FE)),
  CustomMapPalette("neon", "Cyber Neon", "🌆", Color(0xFF0F172A), Color(0xFF020617), Color(0xFF172554), Color(0xFF0284C7), Color(0xFF06B6D4), Color(0xFF22D3EE)),
  CustomMapPalette("gold", "Templo Real", "👑", Color(0xFF78350F), Color(0xFF451A03), Color(0xFF291204), Color(0xFFB45309), Color(0xFFD97706), Color(0xFFFDE047))
)

data class CustomObstacleSpec(
  val xRatio: Float, // 0.1f .. 0.9f of worldWidth
  val yRatio: Float, // 0.1f .. 0.9f of worldHeight
  val type: String   // "FOUNTAIN", "TREE", "FLOWER_PATCH", "YARN"
)

private fun sanitizeText(input: String): String =
  input.replace("|", " ").replace(";", " ").replace("\n", " ").trim()

data class CustomMapConfig(
  val name: String = "Meu Mapa Secreto",
  val paletteId: String = "galaxy",
  val goalCats: Int = 10,
  val isEndless: Boolean = false,
  val ghostCount: Int = 1,
  val isDarknessMode: Boolean = false,
  val obstacles: List<CustomObstacleSpec> = listOf(
    CustomObstacleSpec(0.5f, 0.5f, "FOUNTAIN"),
    CustomObstacleSpec(0.25f, 0.3f, "TREE"),
    CustomObstacleSpec(0.75f, 0.7f, "YARN")
  )
) {
  val palette: CustomMapPalette
    get() = CUSTOM_MAP_PALETTES.find { it.id == paletteId } ?: CUSTOM_MAP_PALETTES.first()

  fun toJsonString(): String {
    val obsStr = obstacles.joinToString(";") { "${it.xRatio},${it.yRatio},${sanitizeText(it.type)}" }
    return listOf(
      sanitizeText(name).ifEmpty { "Meu Mapa Secreto" },
      sanitizeText(paletteId),
      goalCats.toString(),
      isEndless.toString(),
      ghostCount.toString(),
      isDarknessMode.toString(),
      obsStr
    ).joinToString("|")
  }

  companion object {
    fun fromJsonString(serialized: String?): CustomMapConfig {
      if (serialized.isNullOrBlank()) return CustomMapConfig()
      return try {
        val parts = serialized.split("|")
        if (parts.size < 6) return CustomMapConfig()
        val obsList = if (parts.size >= 7 && parts[6].isNotBlank()) {
          parts[6].split(";").mapNotNull { entry ->
            val seg = entry.split(",")
            if (seg.size == 3) {
              CustomObstacleSpec(
                xRatio = seg[0].toFloatOrNull()?.coerceIn(0.08f, 0.92f) ?: 0.5f,
                yRatio = seg[1].toFloatOrNull()?.coerceIn(0.08f, 0.92f) ?: 0.5f,
                type = seg[2]
              )
            } else null
          }
        } else emptyList()

        CustomMapConfig(
          name = parts[0].ifBlank { "Meu Mapa Secreto" },
          paletteId = parts[1].ifBlank { "galaxy" },
          goalCats = (parts[2].toIntOrNull() ?: 10).coerceIn(5, 50),
          isEndless = parts[3].toBooleanStrictOrNull() ?: false,
          ghostCount = (parts[4].toIntOrNull() ?: 1).coerceIn(1, 3),
          isDarknessMode = parts[5].toBooleanStrictOrNull() ?: false,
          obstacles = obsList
        )
      } catch (_: Exception) {
        CustomMapConfig()
      }
    }
  }
}

data class CustomItemConfig(
  val id: String,
  val name: String,
  val emoji: String,
  val colorArgb: Long,
  val effectType: ItemEffectType,
  val durationSec: Float,
  val powerMagnitude: Float = 1.6f
) {
  fun toGameItemDef(index: Int): GameItemDef {
    val effectLabel = when (effectType) {
      ItemEffectType.SPEED_BOOST -> "Super velocidade ${String.format("%.1fx", powerMagnitude)} por ${durationSec.toInt()}s"
      ItemEffectType.GHOST_FLEE -> "Fantasmas fogem por ${durationSec.toInt()}s"
      ItemEffectType.SHIELD -> "Bloqueia 1 ataque fantasma"
      ItemEffectType.MAGNET -> "Atrai gatos por ${durationSec.toInt()}s"
      ItemEffectType.FREEZE_GHOSTS -> "Congela fantasmas por ${durationSec.toInt()}s"
      ItemEffectType.HEAL_HEART -> "+1 Vida e vigor felino"
      ItemEffectType.SONIC_BLAST -> "Onda gigante afasta fantasmas"
      ItemEffectType.INVISIBILITY -> "Invisibilidade por ${durationSec.toInt()}s"
      ItemEffectType.DOUBLE_CAT_VALUE -> "Gatos valem x2 por ${durationSec.toInt()}s"
      ItemEffectType.SLOW_MOTION_GHOSTS -> "Fantasmas em câmera lenta ${durationSec.toInt()}s"
      ItemEffectType.TURBO_OVERDRIVE -> "Turbo infinito por ${durationSec.toInt()}s"
      ItemEffectType.SONIC_OVERDRIVE -> "Onda Sônica livre por ${durationSec.toInt()}s"
      ItemEffectType.GIANT_MAGNET -> "Super Ímã global por ${durationSec.toInt()}s"
      ItemEffectType.SPAWN_EXTRA_CATS -> "Invoca +3 gatos na arena"
      ItemEffectType.TELEPORT_SAFE -> "Teleporte seguro longe do perigo"
      ItemEffectType.INVINCIBILITY_STAR -> "Invencibilidade por ${durationSec.toInt()}s"
      ItemEffectType.TIME_BONUS_15S -> "+15s em modos com tempo"
      ItemEffectType.GHOST_REPEL_RING -> "Explosão repulsora gigante"
      ItemEffectType.RECHARGE_SKILLS -> "Zera recarga das habilidades"
      ItemEffectType.LUCKY_RAIN -> "Chuva de gatos e itens"
    }
    return GameItemDef(
      id = id,
      number = 41 + index,
      name = name,
      emoji = emoji,
      description = "Item personalizado criado por você!",
      effectSummary = effectLabel,
      color = Color(colorArgb),
      durationSec = durationSec,
      category = ItemCategory.BONUS,
      effectType = effectType,
      powerMagnitude = powerMagnitude
    )
  }

  companion object {
    fun listToJson(items: List<CustomItemConfig>): String {
      return items.joinToString("\n") { item ->
        listOf(
          sanitizeText(item.id),
          sanitizeText(item.name).ifEmpty { "Poção Miau" },
          sanitizeText(item.emoji).ifEmpty { "🧪" },
          item.colorArgb.toString(),
          item.effectType.name,
          item.durationSec.toString(),
          item.powerMagnitude.toString()
        ).joinToString("|")
      }
    }

    fun listFromJson(serialized: String?): List<CustomItemConfig> {
      if (serialized.isNullOrBlank()) return emptyList()
      return try {
        serialized.split("\n").mapIndexedNotNull { idx, line ->
          if (line.isBlank()) return@mapIndexedNotNull null
          val parts = line.split("|")
          if (parts.size < 7) return@mapIndexedNotNull null
          val eff = try { ItemEffectType.valueOf(parts[4]) } catch (_: Exception) { ItemEffectType.SPEED_BOOST }
          CustomItemConfig(
            id = parts[0].ifBlank { "custom_item_$idx" },
            name = parts[1].ifBlank { "Poção Miau" },
            emoji = parts[2].ifBlank { "🧪" },
            colorArgb = parts[3].toLongOrNull() ?: 0xFFF472B6L,
            effectType = eff,
            durationSec = (parts[5].toFloatOrNull() ?: 7f).coerceIn(3f, 20f),
            powerMagnitude = (parts[6].toFloatOrNull() ?: 1.6f).coerceIn(1.2f, 2.5f)
          )
        }
      } catch (_: Exception) {
        emptyList()
      }
    }
  }
}
