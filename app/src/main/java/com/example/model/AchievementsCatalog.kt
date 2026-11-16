package com.example.model

enum class AchievementProgressType {
  WINS,
  TOTAL_CATS,
  ENDLESS_BEST,
  ITEMS_DISCOVERED,
  TOTAL_ITEMS_USED,
  GHOSTS_SCARED,
  DASHES_USED,
  SONICS_USED,
  MODES_UNLOCKED,
  SINGLE_EVENT
}

data class Achievement(
  val id: String,
  val number: Int,
  val title: String,
  val description: String,
  val rewardText: String,
  val emoji: String,
  val targetValue: Int = 1,
  val isMilestone: Boolean = false,
  val progressType: AchievementProgressType = AchievementProgressType.SINGLE_EVENT
)

val ALL_40_ACHIEVEMENTS = listOf(
  // 1-10: Vitórias & Marcos Principais
  Achievement("first_champion", 1, "Primeiro Campeão", "Vença sua primeira partida resgatando 10 gatos.", "Desbloqueia o Modo Infinito no Menu!", "🚀", 1, true, AchievementProgressType.WINS),
  Achievement("wins_2", 2, "Bicampeão Felino", "Vença 2 partidas completas no campeonato.", "Libera Corrida Relâmpago (60s)", "🥈", 2, false, AchievementProgressType.WINS),
  Achievement("wins_3", 3, "Tricampeão Valente", "Alcance 3 vitórias fugindo dos fantasmas.", "Libera Modo Gato Ninja (1 Vida)", "🥉", 3, false, AchievementProgressType.WINS),
  Achievement("amor_felino", 4, "Amor Felino", "Torne-se Campeão vencendo 5 partidas completas!", "Insígnia Lendária Amor Felino 💖", "💖", 5, true, AchievementProgressType.WINS),
  Achievement("wins_7", 5, "Sete Vidas de Sorte", "Complete 7 vitórias heroicas no jogo.", "Libera o Modo Pesadelo", "🍀", 7, false, AchievementProgressType.WINS),
  Achievement("god_mode_unlock", 6, "Divindade Felina", "Alcance 10 vitórias no campeonato dos gatos!", "Desbloqueia o Modo Deus nas Configurações!", "👑", 10, true, AchievementProgressType.WINS),
  Achievement("wins_15", 7, "Lenda de Quinze Coroas", "Vença 15 partidas no total.", "Libera Elite Espectral", "🏆", 15, false, AchievementProgressType.WINS),
  Achievement("wins_20", 8, "Imperador dos Gatos", "Vença 20 partidas e domine o universo felino.", "Libera Universo Supremo dos Gatos", "🪐", 20, true, AchievementProgressType.WINS),
  Achievement("wins_30", 9, "Mito Imortal", "Complete 30 vitórias em qualquer modo com objetivo.", "Título Honorário de Mito", "🌟", 30, false, AchievementProgressType.WINS),
  Achievement("wins_50", 10, "Supremo Campeão Eterno", "Alcance a marca lendária de 50 vitórias!", "Coroa Máxima dos Felinos", "🌞", 50, true, AchievementProgressType.WINS),

  // 11-18: Total de Gatos Resgatados
  Achievement("cats_10", 11, "Primeira Matilha", "Resgate um total de 10 gatinhos acumulados.", "Amigo dos Felinos", "🐱", 10, false, AchievementProgressType.TOTAL_CATS),
  Achievement("cats_25", 12, "Protetor do Parque", "Resgate 25 gatos no total em suas aventuras.", "Coração Bondoso", "🐾", 25, false, AchievementProgressType.TOTAL_CATS),
  Achievement("cat_collector_50", 13, "Protetor dos Gatos", "Resgate um total de 50 gatos acumulados.", "Libera a Mansão Assombrada", "🏡", 50, false, AchievementProgressType.TOTAL_CATS),
  Achievement("cats_100", 14, "Centenário Felino", "Salve 100 gatinhos acumulados!", "Medalha de Cem Patas", "💯", 100, false, AchievementProgressType.TOTAL_CATS),
  Achievement("cats_200", 15, "Guardião da Cidade", "Resgate 200 gatos no total.", "Herói da Metrópole", "🌆", 200, false, AchievementProgressType.TOTAL_CATS),
  Achievement("cats_350", 16, "Rei dos Abrigos", "Salve 350 gatos acumulados.", "Abençoado pelos Gatos", "🎀", 350, false, AchievementProgressType.TOTAL_CATS),
  Achievement("cats_500", 17, "Salvador Lendário", "Resgate 500 gatinhos ao todo!", "Troféu de Platina", "💎", 500, false, AchievementProgressType.TOTAL_CATS),
  Achievement("cats_1000", 18, "Milagre dos Mil Gatos", "Atinja a marca épica de 1.000 gatos resgatados!", "Estátua Dourada", "🗿", 1000, true, AchievementProgressType.TOTAL_CATS),

  // 19-23: Recordes no Modo Infinito
  Achievement("endless_15", 19, "Explorador Infinito", "Pegue 15 gatos em uma partida do Modo Infinito.", "Libera Infinito Duplo", "♾️", 15, false, AchievementProgressType.ENDLESS_BEST),
  Achievement("endless_master_25", 20, "Lenda do Infinito", "Colete 25 ou mais gatos em uma partida Infinita.", "Libera Infinito na Escuridão", "🌌", 25, false, AchievementProgressType.ENDLESS_BEST),
  Achievement("endless_40", 21, "Maratonista Sem Fim", "Colete 40 gatos em uma única sessão Infinita.", "Fôlego Inesgotável", "🏃", 40, false, AchievementProgressType.ENDLESS_BEST),
  Achievement("endless_60", 22, "Furacão Infinito", "Alcance 60 gatos no Modo Infinito!", "Velocidade Infinita", "🌪️", 60, false, AchievementProgressType.ENDLESS_BEST),
  Achievement("endless_100", 23, "Deus do Infinito", "Pegue 100 gatos em uma única partida Infinita!", "Lenda Absoluta", "🌠", 100, true, AchievementProgressType.ENDLESS_BEST),

  // 24-29: Enciclopédia de 40 Itens
  Achievement("items_found_5", 24, "Curioso por Itens", "Descubra 5 itens mágicos diferentes na arena.", "Bolsa de Curioso", "🎒", 5, false, AchievementProgressType.ITEMS_DISCOVERED),
  Achievement("items_found_15", 25, "Colecionador de Poderes", "Descubra 15 dos 40 itens do catálogo.", "Libera Gatos Hiperativos", "🔮", 15, false, AchievementProgressType.ITEMS_DISCOVERED),
  Achievement("items_found_25", 26, "Mestre Alquimista", "Descubra 25 dos 40 itens diferentes!", "Libera Mania de Colecionador", "🧪", 25, false, AchievementProgressType.ITEMS_DISCOVERED),
  Achievement("items_found_40", 27, "Enciclopédia Completa!", "Descubra TODOS os 40 itens mágicos do jogo!", "Mestre Absoluto dos Itens", "📚", 40, true, AchievementProgressType.ITEMS_DISCOVERED),
  Achievement("items_used_30", 28, "Viciado em Magia", "Colete 30 itens mágicos acumulados nas partidas.", "Energia Pura", "✨", 30, false, AchievementProgressType.TOTAL_ITEMS_USED),
  Achievement("items_used_100", 29, "Senhor dos Artefatos", "Colete 100 itens mágicos no total!", "Arsenal Felino", "🪄", 100, false, AchievementProgressType.TOTAL_ITEMS_USED),

  // 30-35: Habilidades & Fantasmas
  Achievement("ghost_scarer", 30, "Caça-Fantasmas", "Assuste ou atordoe fantasmas 5 vezes.", "Mestre da Coragem", "👻", 5, false, AchievementProgressType.GHOSTS_SCARED),
  Achievement("ghost_scarer_25", 31, "Terror das Assombrações", "Assuste ou atordoe fantasmas 25 vezes!", "Libera Infinito Sônico", "😱", 25, false, AchievementProgressType.GHOSTS_SCARED),
  Achievement("ghost_scarer_50", 32, "Exorcista Felino", "Assuste ou atordoe fantasmas 50 vezes!", "Guardião da Luz", "🕯️", 50, false, AchievementProgressType.GHOSTS_SCARED),
  Achievement("dashes_25", 33, "Patinhas Turbinadas", "Use o botão TURBO ⚡ 25 vezes no total.", "Piloto Felino", "⚡", 25, false, AchievementProgressType.DASHES_USED),
  Achievement("sonics_20", 34, "Maestro do Miau", "Dispare a Onda Sônica 🔔 20 vezes no total.", "Voz Poderosa", "🔔", 20, false, AchievementProgressType.SONICS_USED),
  Achievement("powerup_fanatic", 35, "Chuva de Sorte", "Pegue 5 itens mágicos em uma única partida.", "Imã de Tesouros", "🧲", 5, false, AchievementProgressType.SINGLE_EVENT),

  // 36-40: Proezas Especiais & Desbloqueio de Modos
  Achievement("speedrunner", 36, "Patas Relâmpago", "Vença uma partida em menos de 45 segundos!", "Troféu de Velocidade", "⏱️", 1, false, AchievementProgressType.SINGLE_EVENT),
  Achievement("untouchable", 37, "Intocável", "Vença uma partida sem perder nenhuma vida (sem Modo Deus).", "Reflexos Ninja", "🛡️", 1, false, AchievementProgressType.SINGLE_EVENT),
  Achievement("combo_5", 38, "Ritmo Felino", "Faça um Combo x5 pegando gatos em sequência rápida!", "Dançarino do Parque", "🔥", 1, false, AchievementProgressType.SINGLE_EVENT),
  Achievement("triple_ghost_winner", 39, "Domador do Caos", "Vença uma partida enfrentando 3 fantasmas ao mesmo tempo!", "Coragem Suprema", "🔱", 1, false, AchievementProgressType.SINGLE_EVENT),
  Achievement("modes_unlocked_20", 40, "Explorador de Mundos", "Desbloqueie pelo menos 20 dos 40 Modos de Jogo!", "Desbravador Supremo", "🗺️", 20, true, AchievementProgressType.MODES_UNLOCKED)
)
