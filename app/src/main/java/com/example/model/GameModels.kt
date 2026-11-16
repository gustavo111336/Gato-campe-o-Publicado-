package com.example.model

import androidx.compose.ui.graphics.Color

data class Vector2D(val x: Float, val y: Float) {
  operator fun plus(other: Vector2D) = Vector2D(x + other.x, y + other.y)
  operator fun minus(other: Vector2D) = Vector2D(x - other.x, y - other.y)
  operator fun times(factor: Float) = Vector2D(x * factor, y * factor)
  fun length(): Float = kotlin.math.sqrt(x * x + y * y)
  fun normalized(): Vector2D {
    val len = length()
    return if (len > 0.0001f) Vector2D(x / len, y / len) else Vector2D(0f, 0f)
  }
}

enum class GameStatus {
  READY,
  PLAYING,
  PAUSED,
  VICTORY,
  GAME_OVER
}

enum class Difficulty(val label: String, val ghostSpeedMult: Float, val ghostRespawnSec: Float) {
  CALMO("Fácil", 0.85f, 6.0f),
  NORMAL("Normal", 1.05f, 4.5f),
  DESAFIO("Difícil", 1.25f, 3.0f),
  ELITE("Elite 👻", 1.30f, 3.0f),
  CUSTOM("1 a 1000", 1.0f, 3.5f)
}

enum class ControlType {
  JOYSTICK,
  DPAD,
  TOUCH_DIRECT
}

enum class ArenaTheme(
  val label: String,
  val emoji: String,
  val bgTop: Color,
  val bgMid: Color,
  val bgBottom: Color,
  val gridColor: Color,
  val wallColor: Color,
  val wallHighlight: Color
) {
  PARQUE_NOTURNO(
    label = "Parque",
    emoji = "🌿",
    bgTop = Color(0xFF064E3B),
    bgMid = Color(0xFF022C22),
    bgBottom = Color(0xFF042F2E),
    gridColor = Color(0xFF065F46),
    wallColor = Color(0xFF047857),
    wallHighlight = Color(0xFF34D399)
  ),
  MANSAO_ASSOMBRADA(
    label = "Mansão",
    emoji = "🏰",
    bgTop = Color(0xFF3B0764),
    bgMid = Color(0xFF1E1B4B),
    bgBottom = Color(0xFF2E1065),
    gridColor = Color(0xFF581C87),
    wallColor = Color(0xFF4C1D95),
    wallHighlight = Color(0xFFC084FC)
  ),
  CIDADE_NEON(
    label = "Neon",
    emoji = "🌆",
    bgTop = Color(0xFF0F172A),
    bgMid = Color(0xFF020617),
    bgBottom = Color(0xFF172554),
    gridColor = Color(0xFF0284C7),
    wallColor = Color(0xFF0369A1),
    wallHighlight = Color(0xFF22D3EE)
  )
}

enum class HeroSpeedOption(val label: String, val multiplier: Float) {
  NORMAL("Padrão", 1.0f),
  RAPIDO("Rápido", 1.25f),
  SUPER("Super", 1.5f)
}

enum class PowerUpFrequency(val label: String, val spawnChance: Float) {
  NORMAL("Normal", 0.50f),
  ABUNDANTE("Chuva de Itens", 0.95f)
}

data class CatBreed(
  val id: Int,
  val name: String,
  val title: String,
  val description: String,
  val furColor: Color,
  val earColor: Color,
  val eyeColor: Color,
  val accessoryEmoji: String
)

val CHAMPION_CATS = listOf(
  CatBreed(
    id = 1,
    name = "Mingau",
    title = "O Primeiro Resgatado",
    description = "Um gatinho branquinho carinhoso com lacinho rosa. Adora leite morno!",
    furColor = Color(0xFFF8FAFC),
    earColor = Color(0xFFF472B6),
    eyeColor = Color(0xFF38BDF8),
    accessoryEmoji = "🎀"
  ),
  CatBreed(
    id = 2,
    name = "Solzinho",
    title = "O Laranjinha Sapeca",
    description = "Um felino laranja elétrico com listras douradas que nunca para quieto.",
    furColor = Color(0xFFFB923C),
    earColor = Color(0xFFEA580C),
    eyeColor = Color(0xFF10B981),
    accessoryEmoji = "☀️"
  ),
  CatBreed(
    id = 3,
    name = "Frajola",
    title = "O Trapalhão Elegante",
    description = "Preto e branco, com meias brancas nas patas. Especialista em fugir de sustos!",
    furColor = Color(0xFF1E293B),
    earColor = Color(0xFFE2E8F0),
    eyeColor = Color(0xFFFACC15),
    accessoryEmoji = "🎩"
  ),
  CatBreed(
    id = 4,
    name = "Pipoca",
    title = "O Saltador de Sofá",
    description = "Pequenino de pelo caramelo claro que pula mais alto que qualquer arbusto.",
    furColor = Color(0xFFFDE68A),
    earColor = Color(0xFFD97706),
    eyeColor = Color(0xFF0284C7),
    accessoryEmoji = "🍿"
  ),
  CatBreed(
    id = 5,
    name = "Sombra",
    title = "O Guardião da Sorte",
    description = "Panterinha preta com olhos brilhantes cor de esmeralda. Afasta a má sorte!",
    furColor = Color(0xFF0F172A),
    earColor = Color(0xFF64748B),
    eyeColor = Color(0xFF22C55E),
    accessoryEmoji = "🍀"
  ),
  CatBreed(
    id = 6,
    name = "Nuvem",
    title = "O Rei das Sonecas",
    description = "Cinza aveludado e macio como um algodão doce. Adora um cafuné no queixo.",
    furColor = Color(0xFF94A3B8),
    earColor = Color(0xFFCBD5E1),
    eyeColor = Color(0xFFA855F7),
    accessoryEmoji = "☁️"
  ),
  CatBreed(
    id = 7,
    name = "Canela",
    title = "O Curioso Siamês",
    description = "Corpo creme com orelhas e focinho castanhos. Olhar penetrante e charmoso.",
    furColor = Color(0xFFF5E6D3),
    earColor = Color(0xFF78350F),
    eyeColor = Color(0xFF38BDF8),
    accessoryEmoji = "🍂"
  ),
  CatBreed(
    id = 8,
    name = "Paçoca",
    title = "A Tricolor da Alegria",
    description = "Malhada com manchas laranja, marrom e branca. Traz sorte para a matilha!",
    furColor = Color(0xFFD97706),
    earColor = Color(0xFFFFFBEB),
    eyeColor = Color(0xFF84CC16),
    accessoryEmoji = "🥜"
  ),
  CatBreed(
    id = 9,
    name = "Panqueca",
    title = "O Doce Aventureiro",
    description = "Amarelo listrado bem gordinho que ronrona alto quando você chega perto.",
    furColor = Color(0xFFFBBF24),
    earColor = Color(0xFFB45309),
    eyeColor = Color(0xFF059669),
    accessoryEmoji = "🥞"
  ),
  CatBreed(
    id = 10,
    name = "Estrela Dourada",
    title = "O Grande Campeão Mágico",
    description = "O lendário felino dourado! Ao resgatá-lo, o título sagrado de Campeão é concedido!",
    furColor = Color(0xFFFDE047),
    earColor = Color(0xFFEAB308),
    eyeColor = Color(0xFFEC4899),
    accessoryEmoji = "👑"
  )
)

val ENDLESS_EXTRA_BREEDS = listOf(
  CatBreed(11, "Nebulosa", "O Gato Cósmico", "Brilha com poeira estelar infinita.", Color(0xFFA855F7), Color(0xFFE879F9), Color(0xFFFDE047), "🌌"),
  CatBreed(12, "Esmeralda", "O Guardião Verde", "Olhos brilhantes da floresta infinita.", Color(0xFF10B981), Color(0xFF6EE7B7), Color(0xFFFFFFFF), "💎"),
  CatBreed(13, "Safira", "O Felino Azul", "Veloz como as ondas do mar noturno.", Color(0xFF38BDF8), Color(0xFFBAE6FD), Color(0xFFFACC15), "🌊"),
  CatBreed(14, "Morango", "O Doce Rosado", "Cheirinho de frutas e muita energia.", Color(0xFFF472B6), Color(0xFFFBCFE8), Color(0xFF22C55E), "🍓"),
  CatBreed(15, "Rubi", "O Gato Flamejante", "Aquece todos os amigos da fila conga.", Color(0xFFEF4444), Color(0xFFFCA5A5), Color(0xFFFDE047), "🔥")
)

data class CollectibleCat(
  val id: Int,
  val breed: CatBreed,
  var position: Vector2D,
  val spawnTime: Long = System.currentTimeMillis(),
  var bobOffset: Float = 0f,
  var wanderDir: Vector2D = Vector2D(0f, 0f)
)

data class FollowerCat(
  val breed: CatBreed,
  var position: Vector2D,
  var angle: Float = 0f,
  var hopOffset: Float = 0f
)

data class Ghost(
  val id: Int = 0,
  var position: Vector2D,
  var velocity: Vector2D = Vector2D(0f, 0f),
  var isFleeing: Boolean = false,
  var fleeTimerSec: Float = 0f,
  var floatOffset: Float = 0f,
  var freezeTimerSec: Float = 0f,
  var stunTimerSec: Float = 0f
)

data class Particle(
  var position: Vector2D,
  var velocity: Vector2D,
  val color: Color,
  var alpha: Float = 1f,
  val size: Float,
  var life: Float = 1f
)

data class FloatingText(
  val text: String,
  var position: Vector2D,
  val color: Color,
  var alpha: Float = 1f,
  var lifetime: Float = 1.2f
)

data class ShockwaveEffect(
  val origin: Vector2D,
  var radius: Float = 20f,
  val maxRadius: Float = 360f,
  val color: Color = Color(0xFFFDE047),
  var alpha: Float = 1f
)

data class AiRunnerCat(
  var position: Vector2D,
  var velocity: Vector2D = Vector2D(0f, 0f),
  var angle: Float = 0f,
  var lives: Int = 3,
  var maxLives: Int = 3,
  var catsCollected: Int = 0,
  var goalCats: Int = 10,
  var invincibleTimer: Float = 0f,
  var stunTimer: Float = 0f
)

data class BotCatState(
  val id: Int,
  val name: String,
  val furColor: Color,
  val accentEmoji: String,
  var position: Vector2D,
  var velocity: Vector2D = Vector2D(0f, 0f),
  var angle: Float = 0f,
  var catsCollected: Int = 0,
  var invincibleTimer: Float = 0f
)
