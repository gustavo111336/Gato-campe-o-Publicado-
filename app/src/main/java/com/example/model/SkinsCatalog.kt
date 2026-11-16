package com.example.model

import androidx.compose.ui.graphics.Color

enum class SkinUnlockRule {
  ALWAYS_UNLOCKED,
  WINS_REQUIRED,
  TOTAL_CATS_REQUIRED,
  ENDLESS_BEST_REQUIRED,
  ACHIEVEMENTS_REQUIRED,
  ITEMS_FOUND_REQUIRED,
  GHOSTS_SCARED_REQUIRED,
  DASHES_REQUIRED,
  MODES_UNLOCKED_REQUIRED,
  CREATOR_USED
}

data class HeroSkinDef(
  val id: String,
  val number: Int,
  val name: String,
  val badgeEmoji: String,
  val description: String,
  val unlockDescription: String,
  val unlockRule: SkinUnlockRule,
  val unlockTarget: Int,
  val furColor: Color,
  val stripeColor: Color,
  val earColor: Color,
  val innerEarColor: Color,
  val capeColor: Color,
  val capeBorderColor: Color,
  val eyeColor: Color,
  val trailColor: Color,
  val hasAura: Boolean = false
) {
  fun isUnlocked(
    wins: Int,
    totalCats: Int,
    bestEndless: Int,
    achievementsCount: Int,
    discoveredItemsCount: Int,
    ghostsScared: Int,
    dashesUsed: Int,
    modesUnlocked: Int,
    hasCreatedCustom: Boolean
  ): Boolean {
    return when (unlockRule) {
      SkinUnlockRule.ALWAYS_UNLOCKED -> true
      SkinUnlockRule.WINS_REQUIRED -> wins >= unlockTarget
      SkinUnlockRule.TOTAL_CATS_REQUIRED -> totalCats >= unlockTarget
      SkinUnlockRule.ENDLESS_BEST_REQUIRED -> bestEndless >= unlockTarget
      SkinUnlockRule.ACHIEVEMENTS_REQUIRED -> achievementsCount >= unlockTarget
      SkinUnlockRule.ITEMS_FOUND_REQUIRED -> discoveredItemsCount >= unlockTarget
      SkinUnlockRule.GHOSTS_SCARED_REQUIRED -> ghostsScared >= unlockTarget
      SkinUnlockRule.DASHES_REQUIRED -> dashesUsed >= unlockTarget
      SkinUnlockRule.MODES_UNLOCKED_REQUIRED -> modesUnlocked >= unlockTarget
      SkinUnlockRule.CREATOR_USED -> hasCreatedCustom
    }
  }

  fun currentProgress(
    wins: Int,
    totalCats: Int,
    bestEndless: Int,
    achievementsCount: Int,
    discoveredItemsCount: Int,
    ghostsScared: Int,
    dashesUsed: Int,
    modesUnlocked: Int,
    hasCreatedCustom: Boolean
  ): Int {
    return when (unlockRule) {
      SkinUnlockRule.ALWAYS_UNLOCKED -> 1
      SkinUnlockRule.WINS_REQUIRED -> wins
      SkinUnlockRule.TOTAL_CATS_REQUIRED -> totalCats
      SkinUnlockRule.ENDLESS_BEST_REQUIRED -> bestEndless
      SkinUnlockRule.ACHIEVEMENTS_REQUIRED -> achievementsCount
      SkinUnlockRule.ITEMS_FOUND_REQUIRED -> discoveredItemsCount
      SkinUnlockRule.GHOSTS_SCARED_REQUIRED -> ghostsScared
      SkinUnlockRule.DASHES_REQUIRED -> dashesUsed
      SkinUnlockRule.MODES_UNLOCKED_REQUIRED -> modesUnlocked
      SkinUnlockRule.CREATOR_USED -> if (hasCreatedCustom) 1 else 0
    }.coerceAtMost(unlockTarget.coerceAtLeast(1))
  }
}

val ALL_HERO_SKINS = listOf(
  HeroSkinDef(
    id = "classic_orange",
    number = 1,
    name = "Campeão Laranja",
    badgeEmoji = "🐱",
    description = "O clássico herói laranja com capa roxa.",
    unlockDescription = "Desbloqueada desde o início",
    unlockRule = SkinUnlockRule.ALWAYS_UNLOCKED,
    unlockTarget = 0,
    furColor = Color(0xFFFB923C),
    stripeColor = Color(0xFFC2410C),
    earColor = Color(0xFFC2410C),
    innerEarColor = Color(0xFFFDA4AF),
    capeColor = Color(0xFF7C3AED),
    capeBorderColor = Color(0xFFFDE047),
    eyeColor = Color(0xFF22C55E),
    trailColor = Color(0xFFFBBF24)
  ),
  HeroSkinDef(
    id = "snow_mingau",
    number = 2,
    name = "Príncipe Mingau",
    badgeEmoji = "🤍",
    description = "Pelagem branca nevada com olhos azul-celeste.",
    unlockDescription = "Salve 10 gatos no total",
    unlockRule = SkinUnlockRule.TOTAL_CATS_REQUIRED,
    unlockTarget = 10,
    furColor = Color(0xFFF8FAFC),
    stripeColor = Color(0xFFCBD5E1),
    earColor = Color(0xFFE2E8F0),
    innerEarColor = Color(0xFFF472B6),
    capeColor = Color(0xFF0284C7),
    capeBorderColor = Color(0xFFBAE6FD),
    eyeColor = Color(0xFF38BDF8),
    trailColor = Color(0xFFE0F2FE)
  ),
  HeroSkinDef(
    id = "midnight_ninja",
    number = 3,
    name = "Gato Ninja Sombrio",
    badgeEmoji = "🥷",
    description = "Furtivo na noite com máscara escura e faixa vermelha.",
    unlockDescription = "Use o TURBO ⚡ 10 vezes",
    unlockRule = SkinUnlockRule.DASHES_REQUIRED,
    unlockTarget = 10,
    furColor = Color(0xFF1E293B),
    stripeColor = Color(0xFF0F172A),
    earColor = Color(0xFF0F172A),
    innerEarColor = Color(0xFFEF4444),
    capeColor = Color(0xFFDC2626),
    capeBorderColor = Color(0xFFFCA5A5),
    eyeColor = Color(0xFFFACC15),
    trailColor = Color(0xFFEF4444)
  ),
  HeroSkinDef(
    id = "cyber_neon",
    number = 4,
    name = "Cyber Gato Neon",
    badgeEmoji = "🤖",
    description = "Armadura ciano futurista com brilho elétrico.",
    unlockDescription = "Vença 1 partida completa",
    unlockRule = SkinUnlockRule.WINS_REQUIRED,
    unlockTarget = 1,
    furColor = Color(0xFF06B6D4),
    stripeColor = Color(0xFF0284C7),
    earColor = Color(0xFF0F172A),
    innerEarColor = Color(0xFF22D3EE),
    capeColor = Color(0xFFEC4899),
    capeBorderColor = Color(0xFF67E8F9),
    eyeColor = Color(0xFFFDE047),
    trailColor = Color(0xFF22D3EE),
    hasAura = true
  ),
  HeroSkinDef(
    id = "amor_felino_pink",
    number = 5,
    name = "Amor Felino Rosa",
    badgeEmoji = "💖",
    description = "Pelagem rosa encantada com corações brilhantes.",
    unlockDescription = "Vença 5 partidas (Amor Felino)",
    unlockRule = SkinUnlockRule.WINS_REQUIRED,
    unlockTarget = 5,
    furColor = Color(0xFFF472B6),
    stripeColor = Color(0xFFDB2777),
    earColor = Color(0xFFBE185D),
    innerEarColor = Color(0xFFFCE7F3),
    capeColor = Color(0xFFFDE047),
    capeBorderColor = Color(0xFFFFFFFF),
    eyeColor = Color(0xFF38BDF8),
    trailColor = Color(0xFFF9A8D4),
    hasAura = true
  ),
  HeroSkinDef(
    id = "golden_king",
    number = 6,
    name = "Rei Dourado",
    badgeEmoji = "👑",
    description = "Brilho puro de ouro digno da realeza felina.",
    unlockDescription = "Vença 10 partidas (Modo Deus)",
    unlockRule = SkinUnlockRule.WINS_REQUIRED,
    unlockTarget = 10,
    furColor = Color(0xFFFACC15),
    stripeColor = Color(0xFFD97706),
    earColor = Color(0xFFB45309),
    innerEarColor = Color(0xFFFEF08A),
    capeColor = Color(0xFF991B1B),
    capeBorderColor = Color(0xFFFDE047),
    eyeColor = Color(0xFFEC4899),
    trailColor = Color(0xFFFDE047),
    hasAura = true
  ),
  HeroSkinDef(
    id = "ghost_cat",
    number = 7,
    name = "Gato Espectral",
    badgeEmoji = "👻",
    description = "Aparência fantasmagórica lilás para assustar fantasmas!",
    unlockDescription = "Assuste fantasmas 10 vezes",
    unlockRule = SkinUnlockRule.GHOSTS_SCARED_REQUIRED,
    unlockTarget = 10,
    furColor = Color(0xFFC084FC),
    stripeColor = Color(0xFF9333EA),
    earColor = Color(0xFF7E22CE),
    innerEarColor = Color(0xFFE9D5FF),
    capeColor = Color(0xFF38BDF8),
    capeBorderColor = Color(0xFFE0F2FE),
    eyeColor = Color(0xFFFDE047),
    trailColor = Color(0xFFD8B4FE),
    hasAura = true
  ),
  HeroSkinDef(
    id = "emerald_guardian",
    number = 8,
    name = "Guardião Esmeralda",
    badgeEmoji = "🌿",
    description = "Gato místico das florestas iluminadas.",
    unlockDescription = "Descubra 10 itens diferentes",
    unlockRule = SkinUnlockRule.ITEMS_FOUND_REQUIRED,
    unlockTarget = 10,
    furColor = Color(0xFF10B981),
    stripeColor = Color(0xFF047857),
    earColor = Color(0xFF065F46),
    innerEarColor = Color(0xFFA7F3D0),
    capeColor = Color(0xFFF59E0B),
    capeBorderColor = Color(0xFFFEF3C7),
    eyeColor = Color(0xFFFDE047),
    trailColor = Color(0xFF34D399)
  ),
  HeroSkinDef(
    id = "cosmic_galaxy",
    number = 9,
    name = "Gato Galáctico",
    badgeEmoji = "🌌",
    description = "Feito de nebulosas e poeira estelar infinita.",
    unlockDescription = "Pegue 20 gatos no Modo Infinito",
    unlockRule = SkinUnlockRule.ENDLESS_BEST_REQUIRED,
    unlockTarget = 20,
    furColor = Color(0xFF6366F1),
    stripeColor = Color(0xFF312E81),
    earColor = Color(0xFF4338CA),
    innerEarColor = Color(0xFFE879F9),
    capeColor = Color(0xFF0F172A),
    capeBorderColor = Color(0xFFA855F7),
    eyeColor = Color(0xFF22D3EE),
    trailColor = Color(0xFFE879F9),
    hasAura = true
  ),
  HeroSkinDef(
    id = "ice_crystal",
    number = 10,
    name = "Felino Glacial",
    badgeEmoji = "❄️",
    description = "Cristal de gelo brilhante que congela a pista.",
    unlockDescription = "Descubra 20 itens diferentes",
    unlockRule = SkinUnlockRule.ITEMS_FOUND_REQUIRED,
    unlockTarget = 20,
    furColor = Color(0xFF67E8F9),
    stripeColor = Color(0xFF0891B2),
    earColor = Color(0xFF0E7490),
    innerEarColor = Color(0xFFFFFFFF),
    capeColor = Color(0xFF1D4ED8),
    capeBorderColor = Color(0xFFA5F3FC),
    eyeColor = Color(0xFFFFFFFF),
    trailColor = Color(0xFFA5F3FC)
  ),
  HeroSkinDef(
    id = "fire_comet",
    number = 11,
    name = "Cometa Flamejante",
    badgeEmoji = "🔥",
    description = "Rastro incandescente de velocidade pura!",
    unlockDescription = "Desbloqueie 10 conquistas",
    unlockRule = SkinUnlockRule.ACHIEVEMENTS_REQUIRED,
    unlockTarget = 10,
    furColor = Color(0xFFEF4444),
    stripeColor = Color(0xFF991B1B),
    earColor = Color(0xFFB91C1C),
    innerEarColor = Color(0xFFFDE047),
    capeColor = Color(0xFFF97316),
    capeBorderColor = Color(0xFFFEF08A),
    eyeColor = Color(0xFFFDE047),
    trailColor = Color(0xFFFB923C),
    hasAura = true
  ),
  HeroSkinDef(
    id = "creator_artist",
    number = 12,
    name = "Gato Criador",
    badgeEmoji = "🎨",
    description = "Exclusiva para quem criou seu próprio mapa ou item!",
    unlockDescription = "Crie um Mapa ou Item no Estúdio Criativo",
    unlockRule = SkinUnlockRule.CREATOR_USED,
    unlockTarget = 1,
    furColor = Color(0xFFF59E0B),
    stripeColor = Color(0xFFEC4899),
    earColor = Color(0xFF8B5CF6),
    innerEarColor = Color(0xFF38BDF8),
    capeColor = Color(0xFF10B981),
    capeBorderColor = Color(0xFFFDE047),
    eyeColor = Color(0xFF38BDF8),
    trailColor = Color(0xFFF472B6),
    hasAura = true
  ),
  HeroSkinDef(
    id = "rainbow_unicorn",
    number = 13,
    name = "Gato Arco-Íris",
    badgeEmoji = "🌈",
    description = "Vibrante com todas as cores mágicas da arena.",
    unlockDescription = "Desbloqueie 15 modos de jogo",
    unlockRule = SkinUnlockRule.MODES_UNLOCKED_REQUIRED,
    unlockTarget = 15,
    furColor = Color(0xFFE879F9),
    stripeColor = Color(0xFF38BDF8),
    earColor = Color(0xFFFACC15),
    innerEarColor = Color(0xFF4ADE80),
    capeColor = Color(0xFFEC4899),
    capeBorderColor = Color(0xFF67E8F9),
    eyeColor = Color(0xFFFDE047),
    trailColor = Color(0xFF34D399),
    hasAura = true
  ),
  HeroSkinDef(
    id = "vampire_count",
    number = 14,
    name = "Conde Miau",
    badgeEmoji = "🧛",
    description = "Elegante capa escarlate da mansão assombrada.",
    unlockDescription = "Salve 100 gatos no total",
    unlockRule = SkinUnlockRule.TOTAL_CATS_REQUIRED,
    unlockTarget = 100,
    furColor = Color(0xFF334155),
    stripeColor = Color(0xFF0F172A),
    earColor = Color(0xFF1E293B),
    innerEarColor = Color(0xFFDC2626),
    capeColor = Color(0xFF7F1D1D),
    capeBorderColor = Color(0xFFE2E8F0),
    eyeColor = Color(0xFFEF4444),
    trailColor = Color(0xFFDC2626)
  ),
  HeroSkinDef(
    id = "diamond_legend",
    number = 15,
    name = "Gato Diamante",
    badgeEmoji = "💎",
    description = "Facetas cristalinas inquebráveis e brilho azul real.",
    unlockDescription = "Desbloqueie 25 conquistas",
    unlockRule = SkinUnlockRule.ACHIEVEMENTS_REQUIRED,
    unlockTarget = 25,
    furColor = Color(0xFFBAE6FD),
    stripeColor = Color(0xFF38BDF8),
    earColor = Color(0xFF0284C7),
    innerEarColor = Color(0xFFFFFFFF),
    capeColor = Color(0xFF1E40AF),
    capeBorderColor = Color(0xFFFFFFFF),
    eyeColor = Color(0xFF0EA5E9),
    trailColor = Color(0xFF7DD3FC),
    hasAura = true
  ),
  HeroSkinDef(
    id = "supreme_universe",
    number = 16,
    name = "Entidade Suprema",
    badgeEmoji = "🪐",
    description = "A skin definitiva do campeão absoluto do universo!",
    unlockDescription = "Vença 20 partidas",
    unlockRule = SkinUnlockRule.WINS_REQUIRED,
    unlockTarget = 20,
    furColor = Color(0xFFFDE047),
    stripeColor = Color(0xFFA855F7),
    earColor = Color(0xFF7C3AED),
    innerEarColor = Color(0xFF22D3EE),
    capeColor = Color(0xFF312E81),
    capeBorderColor = Color(0xFFFDE047),
    eyeColor = Color(0xFF22D3EE),
    trailColor = Color(0xFFFDE047),
    hasAura = true
  )
)
