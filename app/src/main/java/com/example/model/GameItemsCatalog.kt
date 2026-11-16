package com.example.model

import androidx.compose.ui.graphics.Color

enum class ItemCategory(val label: String, val emoji: String) {
  VELOCIDADE("Velocidade", "⚡"),
  CONTROLE("Fantasmas", "👻"),
  DEFESA("Defesa", "🛡️"),
  MAGIA("Magia & Ímã", "✨"),
  BONUS("Bônus Felino", "🐱")
}

enum class ItemEffectType {
  SPEED_BOOST,
  GHOST_FLEE,
  SHIELD,
  MAGNET,
  FREEZE_GHOSTS,
  HEAL_HEART,
  SONIC_BLAST,
  INVISIBILITY,
  DOUBLE_CAT_VALUE,
  SLOW_MOTION_GHOSTS,
  TURBO_OVERDRIVE,
  SONIC_OVERDRIVE,
  GIANT_MAGNET,
  SPAWN_EXTRA_CATS,
  TELEPORT_SAFE,
  INVINCIBILITY_STAR,
  TIME_BONUS_15S,
  GHOST_REPEL_RING,
  RECHARGE_SKILLS,
  LUCKY_RAIN
}

data class GameItemDef(
  val id: String,
  val number: Int,
  val name: String,
  val emoji: String,
  val description: String,
  val effectSummary: String,
  val color: Color,
  val durationSec: Float,
  val category: ItemCategory,
  val effectType: ItemEffectType,
  val powerMagnitude: Float = 1.0f
)

val ALL_40_ITEMS = listOf(
  GameItemDef("catnip", 1, "Erva-de-Gato", "🌿", "A erva favorita dos felinos ágeis.", "Super velocidade por 6s", Color(0xFF10B981), 6f, ItemCategory.VELOCIDADE, ItemEffectType.SPEED_BOOST, 1.45f),
  GameItemDef("golden_fish", 2, "Peixe Dourado", "🐟", "Um peixe brilhante que assusta assombrações.", "Fantasmas fogem por 5s", Color(0xFFF59E0B), 5f, ItemCategory.CONTROLE, ItemEffectType.GHOST_FLEE),
  GameItemDef("milk_shield", 3, "Escudo de Leite", "🥛", "Bolha protetora de leite morno.", "Bloqueia 1 ataque fantasma", Color(0xFF38BDF8), 0f, ItemCategory.DEFESA, ItemEffectType.SHIELD),
  GameItemDef("cat_magnet", 4, "Ímã de Felinos", "🧲", "Atrai gatinhos próximos automaticamente.", "Atrai gatos por 8s", Color(0xFFEC4899), 8f, ItemCategory.MAGIA, ItemEffectType.MAGNET),
  GameItemDef("freeze_clock", 5, "Relógio de Gelo", "❄️", "Para o tempo para os fantasmas.", "Congela fantasmas por 5s", Color(0xFF67E8F9), 5f, ItemCategory.CONTROLE, ItemEffectType.FREEZE_GHOSTS),
  GameItemDef("extra_heart", 6, "Coração de Gato", "💖", "Restaura energia vital felina.", "+1 Vida (ou Escudo se cheia)", Color(0xFFF43F5E), 0f, ItemCategory.DEFESA, ItemEffectType.HEAL_HEART),
  GameItemDef("sonic_bell", 7, "Sino Sônico", "🔔", "Emite um badalar mágico atordoante.", "Onda gigante afasta fantasmas", Color(0xFFA855F7), 3f, ItemCategory.CONTROLE, ItemEffectType.SONIC_BLAST),
  GameItemDef("ghost_cloak", 8, "Capa Fantasma", "🫥", "Deixa o herói invisível na névoa.", "Fantasmas não te atacam por 6s", Color(0xFF94A3B8), 6f, ItemCategory.DEFESA, ItemEffectType.INVISIBILITY),
  GameItemDef("golden_collar", 9, "Coleira Dourada", "🏅", "Dobra o valor de cada resgate.", "Cada gato vale +2 por 10s", Color(0xFFFDE047), 10f, ItemCategory.BONUS, ItemEffectType.DOUBLE_CAT_VALUE),
  GameItemDef("hourglass_slow", 10, "Ampulheta Lenta", "⏳", "Deixa o ar pesado para assombrações.", "Fantasmas em câmera lenta por 7s", Color(0xFF818CF8), 7f, ItemCategory.CONTROLE, ItemEffectType.SLOW_MOTION_GHOSTS),
  GameItemDef("rocket_yarn", 11, "Novelo Foguete", "🚀", "Propulsão a jato nas patinhas.", "Velocidade extrema 1.7x por 5s", Color(0xFFFB7185), 5f, ItemCategory.VELOCIDADE, ItemEffectType.SPEED_BOOST, 1.70f),
  GameItemDef("turbo_battery", 12, "Pilha Turbo", "🔋", "Energia infinita para arrancadas.", "TURBO sem recarga por 8s", Color(0xFFFACC15), 8f, ItemCategory.VELOCIDADE, ItemEffectType.TURBO_OVERDRIVE),
  GameItemDef("mega_bell", 13, "Mega Gongo", "🛎️", "Som ensurdecedor para ondas felinas.", "ONDA SÔNICA livre por 8s", Color(0xFFC084FC), 8f, ItemCategory.CONTROLE, ItemEffectType.SONIC_OVERDRIVE),
  GameItemDef("galaxy_magnet", 14, "Ímã Galáctico", "🪐", "Puxa gatos de toda a arena!", "Super Ímã global por 6s", Color(0xFFE879F9), 6f, ItemCategory.MAGIA, ItemEffectType.GIANT_MAGNET),
  GameItemDef("cat_whistle", 15, "Apito de Gatinhos", "🪈", "Chama mais felinos para perto.", "Faz nascer +3 gatos na arena", Color(0xFF34D399), 0f, ItemCategory.BONUS, ItemEffectType.SPAWN_EXTRA_CATS),
  GameItemDef("star_invincible", 16, "Estrela Campeã", "🌟", "Brilho invencível das estrelas.", "Imortalidade brilhante por 7s", Color(0xFFFBBF24), 7f, ItemCategory.DEFESA, ItemEffectType.INVINCIBILITY_STAR),
  GameItemDef("warp_feather", 17, "Pena Teleporte", "🪶", "Teletransporta para local seguro.", "Salto instantâneo longe do perigo", Color(0xFF2DD4BF), 0f, ItemCategory.MAGIA, ItemEffectType.TELEPORT_SAFE),
  GameItemDef("clock_plus", 18, "Cronômetro Extra", "⏱️", "Adiciona segundos preciosos.", "+15s em modos com tempo limite", Color(0xFF60A5FA), 0f, ItemCategory.BONUS, ItemEffectType.TIME_BONUS_15S),
  GameItemDef("lightning_paw", 19, "Pata Relâmpago", "⚡", "Recarrega todas suas habilidades.", "Zera espera de Turbo e Onda", Color(0xFFFDE047), 0f, ItemCategory.VELOCIDADE, ItemEffectType.RECHARGE_SKILLS),
  GameItemDef("rainbow_treat", 20, "Petisco Arco-Íris", "🌈", "Chuva mágica de itens na arena!", "Cria +3 novos itens e +2 gatos", Color(0xFFF472B6), 0f, ItemCategory.BONUS, ItemEffectType.LUCKY_RAIN),
  GameItemDef("tuna_can", 21, "Atum Especial", "🥫", "Sabor irresistível que dá vigor.", "+1 Vida e 4s de velocidade", Color(0xFFFB923C), 4f, ItemCategory.DEFESA, ItemEffectType.HEAL_HEART),
  GameItemDef("crystal_ice", 22, "Cristal Glacial", "🧊", "Neve mágica que paralisa inimigos.", "Congela fantasmas por 7s", Color(0xFF22D3EE), 7f, ItemCategory.CONTROLE, ItemEffectType.FREEZE_GHOSTS),
  GameItemDef("garlic_charm", 23, "Amuleto Espanta-Tudo", "🧄", "Cheirinho que fantasmas odeiam.", "Fantasmas fogem por 7s", Color(0xFFE2E8F0), 7f, ItemCategory.CONTROLE, ItemEffectType.GHOST_FLEE),
  GameItemDef("magic_wand", 24, "Varinha Estelar", "🪄", "Explosão de luz repulsora.", "Empurra todos fantasmas longe", Color(0xFFD946EF), 4f, ItemCategory.MAGIA, ItemEffectType.GHOST_REPEL_RING),
  GameItemDef("neon_boots", 25, "Botinhas Neon", "🥾", "Sapatos velozes da cidade neon.", "Velocidade 1.55x por 7s", Color(0xFF06B6D4), 7f, ItemCategory.VELOCIDADE, ItemEffectType.SPEED_BOOST, 1.55f),
  GameItemDef("royal_crown", 26, "Coroa Real", "👑", "Digna de um verdadeiro campeão.", "Gatos valem x2 por 12s", Color(0xFFEAB308), 12f, ItemCategory.BONUS, ItemEffectType.DOUBLE_CAT_VALUE),
  GameItemDef("shadow_veil", 27, "Véu das Sombras", "🥷", "Furtividade total no escuro.", "Invisível aos fantasmas por 8s", Color(0xFF475569), 8f, ItemCategory.DEFESA, ItemEffectType.INVISIBILITY),
  GameItemDef("sushi_roll", 28, "Sushi Fresquinho", "🍣", "Refeição rápida e energizante.", "Velocidade + recarga de Turbo", Color(0xFFF97316), 5f, ItemCategory.VELOCIDADE, ItemEffectType.SPEED_BOOST, 1.4f),
  GameItemDef("bubble_potion", 29, "Poção Bolha", "🫧", "Proteção líquida cintilante.", "Escudo protetor + Onda leve", Color(0xFF7DD3FC), 0f, ItemCategory.DEFESA, ItemEffectType.SHIELD),
  GameItemDef("moon_stone", 30, "Pedra da Lua", "🌙", "Gravidade lunar nos fantasmas.", "Fantasmas lentos por 9s", Color(0xFFA78BFA), 9f, ItemCategory.CONTROLE, ItemEffectType.SLOW_MOTION_GHOSTS),
  GameItemDef("pepper_candy", 31, "Bala Apimentada", "🌶️", "Fogo nas patas para correr!", "Turbo infinito por 6s", Color(0xFFEF4444), 6f, ItemCategory.VELOCIDADE, ItemEffectType.TURBO_OVERDRIVE),
  GameItemDef("diamond_paw", 32, "Pata de Diamante", "💎", "Dureza e brilho inquebráveis.", "Invencibilidade por 6s", Color(0xFF38BDF8), 6f, ItemCategory.DEFESA, ItemEffectType.INVINCIBILITY_STAR),
  GameItemDef("yarn_storm", 33, "Tempestade de Lã", "🧶", "Atrai felinos brincalhões.", "Ímã poderoso por 10s", Color(0xFFF43F5E), 10f, ItemCategory.MAGIA, ItemEffectType.MAGNET),
  GameItemDef("sun_lantern", 34, "Lanterna Solar", "🏮", "Ilumina e espanta assombrações.", "Fantasmas fogem por 6s", Color(0xFFFBBF24), 6f, ItemCategory.CONTROLE, ItemEffectType.GHOST_FLEE),
  GameItemDef("clover_four", 35, "Trevo de 4 Folhas", "🍀", "Sorte pura na arena.", "Faz nascer +2 gatos e +2 itens", Color(0xFF22C55E), 0f, ItemCategory.BONUS, ItemEffectType.LUCKY_RAIN),
  GameItemDef("emerald_heart", 36, "Coração Esmeralda", "💚", "Cura mística da floresta.", "+1 Vida instantânea", Color(0xFF10B981), 0f, ItemCategory.DEFESA, ItemEffectType.HEAL_HEART),
  GameItemDef("laser_pointer", 37, "Laser Divertido", "🔴", "Nenhum gato resiste ao pontinho!", "Super Ímã global por 5s", Color(0xFFDC2626), 5f, ItemCategory.MAGIA, ItemEffectType.GIANT_MAGNET),
  GameItemDef("wind_chime", 38, "Sino dos Ventos", "🎐", "Brisa sonora protetora.", "Onda sônica afasta fantasmas", Color(0xFF93C5FD), 4f, ItemCategory.CONTROLE, ItemEffectType.SONIC_BLAST),
  GameItemDef("comet_tail", 39, "Cauda de Cometa", "☄️", "Velocidade cósmica instantânea.", "Velocidade 1.8x por 4.5s", Color(0xFFF97316), 4.5f, ItemCategory.VELOCIDADE, ItemEffectType.SPEED_BOOST, 1.80f),
  GameItemDef("champion_elixir", 40, "Elixir do Campeão", "🏆", "O item supremo de todos os gatos!", "Invencível + Velocidade + Ímã 8s", Color(0xFFFDE047), 8f, ItemCategory.BONUS, ItemEffectType.INVINCIBILITY_STAR)
)

data class PowerUpItem(
  val itemDef: GameItemDef,
  val position: Vector2D,
  val spawnTime: Long = System.currentTimeMillis()
)
