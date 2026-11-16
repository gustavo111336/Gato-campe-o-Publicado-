package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CloudSavePayload
import com.example.data.FriendLink
import com.example.data.MultiplayerRoom
import com.example.data.PlayerDirectoryEntry
import com.example.data.RoomPlayer
import com.example.data.SharedSkinPost
import com.example.data.UserProfile
import com.example.model.ALL_HERO_SKINS
import com.google.firebase.auth.FirebaseUser

private enum class OnlineLobbyTab(val label: String) {
  ROOMS("🌐 Salas"),
  CLOUD_SAVE("☁️ Conta"),
  FRIENDS("🤝 Amigos"),
  SHARED_SKINS("🎨 Skins")
}

@Composable
fun OnlineMultiplayerDialog(
  currentUser: FirebaseUser?,
  onlineVm: OnlineMultiplayerViewModel?,
  uiState: GameUiState,
  onExportCloudSave: () -> CloudSavePayload,
  onApplyCloudSave: (UserProfile) -> Unit,
  onClaimSharedSkin: (String) -> Unit,
  onStartMultiplayerMatch: () -> Unit,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.92f)
        .padding(4.dp)
        .shadow(24.dp, RoundedCornerShape(24.dp))
        .border(2.dp, Color(0xFF38BDF8), RoundedCornerShape(24.dp))
        .testTag("online_multiplayer_dialog"),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(14.dp)
          .verticalScroll(rememberScrollState())
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "🌐 Online, Conta Google & Social",
              fontSize = 17.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFF38BDF8)
            )
            Text(
              text = "Salas até 200 players • Elite por votos • Nuvem • Amigos • Skins",
              fontSize = 11.sp,
              color = Color(0xFF94A3B8)
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (currentUser == null || onlineVm == null) {
          AuthGateContent()
        } else {
          AuthenticatedMultiplayerLobby(
            user = currentUser,
            onlineVm = onlineVm,
            uiState = uiState,
            onExportCloudSave = onExportCloudSave,
            onApplyCloudSave = onApplyCloudSave,
            onClaimSharedSkin = onClaimSharedSkin,
            onStartMultiplayerMatch = onStartMultiplayerMatch
          )
        }
      }
    }
  }
}

@Composable
private fun AuthGateContent() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  var isSigningIn by remember { mutableStateOf(false) }
  var authError by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = {},
      onUnauthenticated = {},
      scope = scope
    )
  }

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = Color(0xFF1E293B),
    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Text(
        text = "🔐 Entre com sua Conta Google Real",
        fontSize = 15.sp,
        fontWeight = FontWeight.Black,
        color = Color(0xFFFDE047)
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "Conecte sua conta Google real para salvar todo seu progresso na nuvem, adicionar amigos, compartilhar skins e entrar em partidas públicas ou privadas com mais de 100 jogadores!",
        fontSize = 12.sp,
        color = Color(0xFFE2E8F0),
        lineHeight = 17.sp
      )
      Spacer(modifier = Modifier.height(12.dp))

      Button(
        onClick = {
          isSigningIn = true
          authError = null
          onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = { isSigningIn = false },
            onAuthError = { msg ->
              isSigningIn = false
              authError = msg
            },
            scope = scope,
            onAuthCancelled = { isSigningIn = false }
          )
        },
        enabled = !isSigningIn,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("google_sign_in_button")
      ) {
        if (isSigningIn) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = Color.White,
            strokeWidth = 2.5.dp
          )
        } else {
          Text(
            text = "🔑 Entrar com Conta Google",
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
          )
        }
      }

      if (!authError.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "⚠️ $authError",
          fontSize = 11.sp,
          color = Color(0xFFF87171),
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

@Composable
private fun AuthenticatedMultiplayerLobby(
  user: FirebaseUser,
  onlineVm: OnlineMultiplayerViewModel,
  uiState: GameUiState,
  onExportCloudSave: () -> CloudSavePayload,
  onApplyCloudSave: (UserProfile) -> Unit,
  onClaimSharedSkin: (String) -> Unit,
  onStartMultiplayerMatch: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val profileState by onlineVm.userProfileState.collectAsStateWithLifecycle()
  val publicRoomsState by onlineVm.publicRoomsState.collectAsStateWithLifecycle()
  val activeRoom by onlineVm.activeRoom.collectAsStateWithLifecycle()
  val roomPlayers by onlineVm.roomPlayersState.collectAsStateWithLifecycle()
  val friendsState by onlineVm.friendsState.collectAsStateWithLifecycle()
  val sharedSkinsState by onlineVm.sharedSkinsState.collectAsStateWithLifecycle()
  val playerSearchResults by onlineVm.playerSearchResults.collectAsStateWithLifecycle()
  val isSearchingPlayers by onlineVm.isSearchingPlayers.collectAsStateWithLifecycle()
  val cloudSaveStatus by onlineVm.cloudSaveStatus.collectAsStateWithLifecycle()
  val statusMessage by onlineVm.statusMessage.collectAsStateWithLifecycle()
  val errorMessage by onlineVm.errorMessage.collectAsStateWithLifecycle()
  val isBusy by onlineVm.isBusy.collectAsStateWithLifecycle()

  var selectedTab by remember { mutableStateOf(OnlineLobbyTab.ROOMS) }

  val loadedProfile = (profileState as? OnlineLoadState.Success)?.data
  var usernameInput by remember(loadedProfile?.username, user.uid) {
    mutableStateOf(
      loadedProfile?.username?.takeIf { it.isNotBlank() }
        ?: onlineVm.suggestedUsername()
    )
  }

  // Account & Username Compact Banner
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = Color(0xFF1E293B),
    border = BorderStroke(1.dp, Color(0xFF10B981)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "✅ Conta Google Conectada",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF4ADE80)
          )
          Text(
            text = user.email ?: "Conta Google Verificada",
            fontSize = 11.sp,
            color = Color(0xFFCBD5E1),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        OutlinedButton(
          onClick = {
            onlineVm.leaveRoom()
            signOutGoogleAccount(credentialManager, scope)
          },
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          border = BorderStroke(1.dp, Color(0xFFEF4444)),
          modifier = Modifier.defaultMinSize(minHeight = 48.dp)
        ) {
          Text("Sair Conta", fontSize = 11.sp, color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold)
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedTextField(
          value = usernameInput,
          onValueChange = { usernameInput = it.take(24) },
          label = { Text("Seu @usuário", fontSize = 11.sp) },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFF4ADE80),
            unfocusedBorderColor = Color(0xFF475569)
          ),
          modifier = Modifier.weight(1f)
        )
        Button(
          onClick = {
            onlineVm.saveUsername(
              username = usernameInput,
              level = uiState.playerLevel,
              wins = uiState.championCount,
              selectedSkinId = uiState.selectedSkinId
            )
          },
          enabled = !isBusy,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.height(50.dp)
        ) {
          Text("Salvar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
      }
    }
  }

  Spacer(modifier = Modifier.height(8.dp))

  // 4 Sub-tabs
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    OnlineLobbyTab.entries.forEach { tab ->
      val active = selectedTab == tab
      FilterChip(
        selected = active,
        onClick = { selectedTab = tab },
        label = {
          Text(
            text = tab.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = Color(0xFF0284C7),
          selectedLabelColor = Color.White,
          containerColor = Color(0xFF1E293B),
          labelColor = Color(0xFFCBD5E1)
        ),
        modifier = Modifier
          .defaultMinSize(minHeight = 48.dp)
          .testTag("online_tab_${tab.name.lowercase()}")
      )
    }
  }

  if (!statusMessage.isNullOrBlank()) {
    Spacer(modifier = Modifier.height(8.dp))
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = Color(0xFF064E3B),
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(
        text = "✨ $statusMessage",
        fontSize = 12.sp,
        color = Color(0xFFA7F3D0),
        modifier = Modifier.padding(10.dp)
      )
    }
  }

  if (!errorMessage.isNullOrBlank()) {
    Spacer(modifier = Modifier.height(8.dp))
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = Color(0xFF7F1D1D),
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(
        text = "⚠️ $errorMessage",
        fontSize = 12.sp,
        color = Color(0xFFFCA5A5),
        modifier = Modifier.padding(10.dp)
      )
    }
  }

  Spacer(modifier = Modifier.height(10.dp))

  when (selectedTab) {
    OnlineLobbyTab.ROOMS -> {
      RoomsTabContent(
        user = user,
        onlineVm = onlineVm,
        uiState = uiState,
        activeRoom = activeRoom,
        roomPlayers = roomPlayers,
        publicRoomsState = publicRoomsState,
        isBusy = isBusy,
        onStartMultiplayerMatch = onStartMultiplayerMatch
      )
    }

    OnlineLobbyTab.CLOUD_SAVE -> {
      CloudSaveTabContent(
        uiState = uiState,
        loadedProfile = loadedProfile,
        cloudSaveStatus = cloudSaveStatus,
        isBusy = isBusy,
        onSaveToCloud = { onlineVm.saveAllProgressToCloud(onExportCloudSave()) },
        onLoadFromCloud = { onlineVm.loadAllProgressFromCloud(onApplyCloudSave) }
      )
    }

    OnlineLobbyTab.FRIENDS -> {
      FriendsTabContent(
        onlineVm = onlineVm,
        friendsState = friendsState,
        searchResults = playerSearchResults,
        isSearching = isSearchingPlayers,
        isBusy = isBusy
      )
    }

    OnlineLobbyTab.SHARED_SKINS -> {
      SharedSkinsTabContent(
        onlineVm = onlineVm,
        uiState = uiState,
        sharedSkinsState = sharedSkinsState,
        isBusy = isBusy,
        onClaimSharedSkin = onClaimSharedSkin
      )
    }
  }
}

@Composable
private fun RoomsTabContent(
  user: FirebaseUser,
  onlineVm: OnlineMultiplayerViewModel,
  uiState: GameUiState,
  activeRoom: MultiplayerRoom?,
  roomPlayers: List<RoomPlayer>,
  publicRoomsState: OnlineLoadState<List<MultiplayerRoom>>,
  isBusy: Boolean,
  onStartMultiplayerMatch: () -> Unit
) {
  var joinCodeInput by remember { mutableStateOf("") }
  var selectedMaxPlayers by remember { mutableIntStateOf(100) }
  var selectedRoomDifficulty by remember { mutableStateOf("NORMAL") }

  if (activeRoom != null) {
    ActiveRoomCard(
      room = activeRoom,
      roomPlayers = roomPlayers,
      currentUserId = user.uid,
      isBusy = isBusy,
      onVoteGhost = { targetUid ->
        onlineVm.castGhostVote(targetUid, uiState.selectedSkinId)
      },
      onStartMatch = onStartMultiplayerMatch,
      onLeaveRoom = { onlineVm.leaveRoom() }
    )
    return
  }

  // Instant Public Matchmaking (no code needed)
  Button(
    onClick = {
      onlineVm.quickJoinPublicMatch(
        playerLevel = uiState.playerLevel,
        wins = uiState.championCount,
        maxPlayers = selectedMaxPlayers,
        difficulty = selectedRoomDifficulty,
        skinId = uiState.selectedSkinId
      )
    },
    enabled = !isBusy,
    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier
      .fillMaxWidth()
      .height(54.dp)
      .testTag("quick_public_search_button")
  ) {
    Text(
      text = "🔍 Pesquisar Públicas (Entrar Sem Código)",
      fontSize = 13.sp,
      fontWeight = FontWeight.Black,
      color = Color.White
    )
  }

  Spacer(modifier = Modifier.height(10.dp))

  // Room settings (Max players up to 200 + Difficulty including Elite voting)
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = Color(0xFF131C2E),
    border = BorderStroke(1.dp, Color(0xFF334155)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Text(
        text = "👥 Capacidade Máxima da Partida (suporta +100 players!)",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF38BDF8)
      )
      Spacer(modifier = Modifier.height(4.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        listOf(2, 10, 50, 100, 150, 200).forEach { cap ->
          FilterChip(
            selected = selectedMaxPlayers == cap,
            onClick = { selectedMaxPlayers = cap },
            label = {
              Text(
                text = if (cap > 100) "$cap+ 🔥" else "$cap players",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            },
            modifier = Modifier
              .defaultMinSize(minHeight = 48.dp)
              .testTag("max_players_chip_$cap")
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "⚡ Dificuldade da Sala (Elite = 1 Fantasma por votos!)",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFFFDE047)
      )
      Spacer(modifier = Modifier.height(4.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        listOf(
          "NORMAL" to "⚖️ Normal",
          "CALMO" to "🐢 Fácil",
          "DESAFIO" to "🔥 Difícil",
          "ELITE" to "👻 Elite (Votos)"
        ).forEach { (key, label) ->
          FilterChip(
            selected = selectedRoomDifficulty == key,
            onClick = { selectedRoomDifficulty = key },
            label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = if (key == "ELITE") Color(0xFF7E22CE) else Color(0xFF0284C7),
              selectedLabelColor = Color.White
            ),
            modifier = Modifier
              .defaultMinSize(minHeight = 48.dp)
              .testTag("room_diff_chip_${key.lowercase()}")
          )
        }
      }
    }
  }

  Spacer(modifier = Modifier.height(10.dp))

  // Create Private or Public room
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Button(
      onClick = {
        onlineVm.createRoom(
          isPublic = false,
          playerLevel = uiState.playerLevel,
          wins = uiState.championCount,
          maxPlayers = selectedMaxPlayers,
          difficulty = selectedRoomDifficulty,
          skinId = uiState.selectedSkinId
        )
      },
      enabled = !isBusy,
      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .weight(1f)
        .height(50.dp)
        .testTag("create_private_room_button")
    ) {
      Text("🔒 Privada\n(Com Código)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }

    Button(
      onClick = {
        onlineVm.createRoom(
          isPublic = true,
          playerLevel = uiState.playerLevel,
          wins = uiState.championCount,
          maxPlayers = selectedMaxPlayers,
          difficulty = selectedRoomDifficulty,
          skinId = uiState.selectedSkinId
        )
      },
      enabled = !isBusy,
      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .weight(1f)
        .height(50.dp)
        .testTag("create_public_room_button")
    ) {
      Text("🌐 Pública\n(Até $selectedMaxPlayers)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
  }

  Spacer(modifier = Modifier.height(12.dp))

  // Join by 6-char Code
  Text("🔑 Entrar com Código Privado", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
  Spacer(modifier = Modifier.height(4.dp))
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    OutlinedTextField(
      value = joinCodeInput,
      onValueChange = { joinCodeInput = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(6) },
      placeholder = { Text("Ex: GATO24", fontSize = 12.sp, color = Color(0xFF64748B)) },
      singleLine = true,
      colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = Color(0xFF38BDF8),
        unfocusedBorderColor = Color(0xFF475569)
      ),
      modifier = Modifier
        .weight(1f)
        .testTag("join_room_code_input")
    )
    Button(
      onClick = {
        onlineVm.joinRoom(
          code = joinCodeInput,
          playerLevel = uiState.playerLevel,
          wins = uiState.championCount,
          skinId = uiState.selectedSkinId
        )
      },
      enabled = !isBusy && joinCodeInput.length == 6,
      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier
        .height(50.dp)
        .testTag("join_room_code_button")
    ) {
      Text("Entrar", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
    }
  }

  Spacer(modifier = Modifier.height(12.dp))

  // Public Waiting Rooms List
  Text("🌐 Salas Públicas Abertas Agora", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
  Spacer(modifier = Modifier.height(6.dp))
  when (val listState = publicRoomsState) {
    is OnlineLoadState.Loading -> {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF34D399), strokeWidth = 2.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Buscando salas públicas...", fontSize = 12.sp, color = Color(0xFF94A3B8))
      }
    }
    is OnlineLoadState.Error -> {
      Text("⚠️ ${listState.message}", fontSize = 12.sp, color = Color(0xFFF87171))
    }
    is OnlineLoadState.Success -> {
      if (listState.data.isEmpty()) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color(0xFF131C2E),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "Nenhuma sala pública aberta agora. Toque em 'Pesquisar Públicas' acima para entrar ou criar automaticamente!",
            fontSize = 11.sp,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(12.dp)
          )
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          for (publicRoom in listState.data) {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = Color(0xFF1E293B),
              border = BorderStroke(1.dp, Color(0xFF334155)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "🐱 @${publicRoom.hostUsername} • ${publicRoom.difficulty}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                  )
                  Text(
                    text = "${publicRoom.playerCount}/${publicRoom.maxPlayers} players • Cód: ${publicRoom.code}",
                    fontSize = 11.sp,
                    color = Color(0xFF38BDF8)
                  )
                }
                Button(
                  onClick = {
                    onlineVm.joinRoom(
                      code = publicRoom.code,
                      playerLevel = uiState.playerLevel,
                      wins = uiState.championCount,
                      skinId = uiState.selectedSkinId
                    )
                  },
                  enabled = !isBusy,
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                  Text("Entrar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ActiveRoomCard(
  room: MultiplayerRoom,
  roomPlayers: List<RoomPlayer>,
  currentUserId: String,
  isBusy: Boolean,
  onVoteGhost: (String) -> Unit,
  onStartMatch: () -> Unit,
  onLeaveRoom: () -> Unit
) {
  val isElite = room.difficulty == "ELITE"
  val voteCounts = remember(roomPlayers) {
    roomPlayers.mapNotNull { it.voteGhostForUid.takeIf { uid -> uid.isNotBlank() } }
      .groupingBy { it }
      .eachCount()
  }
  val electedGhostName = roomPlayers.find { it.userId == room.ghostPlayerUserId }?.username
    ?: if (isElite) "Vote abaixo!" else ""

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = Color(0xFF1E1B4B),
    border = BorderStroke(2.dp, if (isElite) Color(0xFFC084FC) else Color(0xFFFACC15)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("active_room_card")
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (room.isPublic) "🌐 SALA PÚBLICA" else "🔒 SALA PRIVADA",
          fontSize = 11.sp,
          fontWeight = FontWeight.Black,
          color = Color(0xFFFDE047)
        )
        Text(
          text = "👥 ${roomPlayers.size.coerceAtLeast(room.playerCount.toInt())}/${room.maxPlayers} players",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF38BDF8)
        )
      }

      Text(
        text = "Código: ${room.code} • ${room.difficulty}",
        fontSize = 20.sp,
        fontWeight = FontWeight.Black,
        color = Color.White,
        letterSpacing = 1.sp
      )

      if (isElite) {
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = Color(0xFF3B0764),
          border = BorderStroke(1.dp, Color(0xFFA855F7)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "👻 MODO ELITE POR VOTOS: Vote em quem será o Fantasma! Fantasma atual: @$electedGhostName",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE9D5FF),
            modifier = Modifier.padding(8.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      if (roomPlayers.isEmpty()) {
        Text(
          text = "Conectando lista de jogadores...",
          fontSize = 12.sp,
          color = Color(0xFFCBD5E1)
        )
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          roomPlayers.forEach { p ->
            val isGhostRole = isElite && (p.userId == room.ghostPlayerUserId || p.role == "ghost")
            val isMe = p.userId == currentUserId
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (isGhostRole) Color(0xFF4C1D95) else Color(0xFF1E293B),
              border = BorderStroke(1.dp, if (isMe) Color(0xFF38BDF8) else Color(0xFF334155)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${if (isGhostRole) "👻 Fantasma" else "🐱 Gato"} • @${p.username}${if (isMe) " (Você)" else ""}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  val votesForPlayer = voteCounts[p.userId] ?: 0
                  Text(
                    text = "🐾 ${p.score} pts${if (isElite) " • 🗳️ $votesForPlayer voto(s)" else ""}",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                  )
                }

                if (isElite) {
                  Button(
                    onClick = { onVoteGhost(p.userId) },
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                      .defaultMinSize(minHeight = 48.dp)
                      .testTag("vote_ghost_${p.userId}")
                  ) {
                    Text("🗳️ Votar", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                  }
                }
              }
            }
          }
        }
      }

      if (room.status == "finished" && room.winnerUsername.isNotBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "🏆 Vencedor da Sala: @${room.winnerUsername}!",
          fontSize = 13.sp,
          fontWeight = FontWeight.Black,
          color = Color(0xFFFDE047)
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Button(
        onClick = onStartMatch,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("enter_multiplayer_arena_button")
      ) {
        val label = if (isElite && room.ghostPlayerUserId == currentUserId) {
          "👻 Entrar na Arena como FANTASMA!"
        } else {
          "▶️ Entrar na Arena Agora! (${roomPlayers.size}/${room.maxPlayers})"
        }
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
      }

      Spacer(modifier = Modifier.height(6.dp))

      OutlinedButton(
        onClick = onLeaveRoom,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFEF4444)),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("leave_room_button")
      ) {
        Text("🚪 Sair da Sala", fontSize = 12.sp, color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun CloudSaveTabContent(
  uiState: GameUiState,
  loadedProfile: UserProfile?,
  cloudSaveStatus: String?,
  isBusy: Boolean,
  onSaveToCloud: () -> Unit,
  onLoadFromCloud: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = Color(0xFF1E293B),
    border = BorderStroke(1.5.dp, Color(0xFF38BDF8)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(
        text = "☁️ Salvar Todo Progresso na Conta Google",
        fontSize = 14.sp,
        fontWeight = FontWeight.Black,
        color = Color(0xFF38BDF8)
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Guarde vitórias, nível, recorde, pontos da loja, conquistas, skins, modos e itens na nuvem para nunca perder ao trocar de aparelho.",
        fontSize = 11.sp,
        color = Color(0xFFCBD5E1),
        lineHeight = 15.sp
      )

      Spacer(modifier = Modifier.height(10.dp))

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0F172A),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Text("📊 Seu Progresso Local Atual:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFDE047))
          Text("• Vitórias: ${uiState.championCount} | Nível: ${uiState.playerLevel}", fontSize = 11.sp, color = Color.White)
          Text("• Recorde Placar: ${uiState.bestHighScore} pts | Pontos Loja: ${uiState.spendablePoints}", fontSize = 11.sp, color = Color.White)
          Text("• Conquistas: ${uiState.unlockedAchievements.size}/40 | Itens: ${uiState.discoveredItems.size}/40", fontSize = 11.sp, color = Color.White)
          Text("• Skins compradas: ${uiState.purchasedSkinIds.size} | Modos comprados: ${uiState.purchasedModeIds.size}", fontSize = 11.sp, color = Color.White)
        }
      }

      if (loadedProfile != null && (loadedProfile.saveUpdatedAt != null || loadedProfile.championCount > 0L || loadedProfile.spendablePoints > 0L)) {
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color(0xFF064E3B),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text("☁️ Salvo na Nuvem Agora:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6EE7B7))
            Text(
              "Vitórias: ${loadedProfile.championCount} • Nível ${loadedProfile.playerLevel} • ${loadedProfile.spendablePoints} pts na Loja",
              fontSize = 11.sp,
              color = Color.White
            )
          }
        }
      }

      if (!cloudSaveStatus.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = cloudSaveStatus,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFFFDE047)
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Button(
        onClick = onSaveToCloud,
        enabled = !isBusy,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("cloud_save_button")
      ) {
        Text("💾 Salvar Todo Progresso no Google", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
      }

      Spacer(modifier = Modifier.height(8.dp))

      OutlinedButton(
        onClick = onLoadFromCloud,
        enabled = !isBusy,
        border = BorderStroke(1.5.dp, Color(0xFF38BDF8)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("cloud_load_button")
      ) {
        Text("⬇️ Restaurar Progresso da Nuvem", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
      }
    }
  }
}

@Composable
private fun FriendsTabContent(
  onlineVm: OnlineMultiplayerViewModel,
  friendsState: OnlineLoadState<List<FriendLink>>,
  searchResults: List<PlayerDirectoryEntry>,
  isSearching: Boolean,
  isBusy: Boolean
) {
  var searchQuery by remember { mutableStateOf("") }

  LaunchedEffect(Unit) {
    onlineVm.searchPlayers("")
  }

  Text("🔍 Pesquisar Jogadores para Adicionar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
  Spacer(modifier = Modifier.height(6.dp))
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it.take(24) },
      placeholder = { Text("Buscar @usuário...", fontSize = 12.sp, color = Color(0xFF64748B)) },
      singleLine = true,
      colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = Color(0xFF38BDF8),
        unfocusedBorderColor = Color(0xFF475569)
      ),
      modifier = Modifier
        .weight(1f)
        .testTag("friend_search_input")
    )
    Button(
      onClick = { onlineVm.searchPlayers(searchQuery) },
      enabled = !isSearching,
      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier
        .height(50.dp)
        .testTag("friend_search_button")
    ) {
      Text("Buscar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
  }

  Spacer(modifier = Modifier.height(8.dp))

  val currentFriendIds = ((friendsState as? OnlineLoadState.Success)?.data ?: emptyList())
    .map { it.friendUserId }
    .toSet()

  if (isSearching) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(6.dp)) {
      CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF38BDF8), strokeWidth = 2.dp)
      Spacer(modifier = Modifier.width(8.dp))
      Text("Buscando jogadores...", fontSize = 11.sp, color = Color(0xFF94A3B8))
    }
  } else if (searchResults.isNotEmpty()) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      searchResults.forEach { entry ->
        val alreadyFriend = currentFriendIds.contains(entry.userId)
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color(0xFF1E293B),
          border = BorderStroke(1.dp, Color(0xFF334155)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("@${entry.username}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
              Text("Nível ${entry.level} • ${entry.wins}🏆", fontSize = 11.sp, color = Color(0xFF94A3B8))
            }
            if (alreadyFriend) {
              Text("✅ Amigo", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4ADE80))
            } else {
              Button(
                onClick = { onlineVm.addFriend(entry) },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
              ) {
                Text("Adicionar 🤝", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
              }
            }
          }
        }
      }
    }
  }

  Spacer(modifier = Modifier.height(12.dp))
  Text("🤝 Suas Amizades Salvas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4ADE80))
  Spacer(modifier = Modifier.height(6.dp))

  when (friendsState) {
    is OnlineLoadState.Loading -> {
      Text("Carregando amizades...", fontSize = 11.sp, color = Color(0xFF94A3B8))
    }
    is OnlineLoadState.Error -> {
      Text("⚠️ ${friendsState.message}", fontSize = 11.sp, color = Color(0xFFF87171))
    }
    is OnlineLoadState.Success -> {
      if (friendsState.data.isEmpty()) {
        Text(
          text = "Você ainda não adicionou amigos. Pesquise jogadores acima e toque em Adicionar!",
          fontSize = 11.sp,
          color = Color(0xFF94A3B8)
        )
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          friendsState.data.forEach { friend ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0xFF064E3B),
              border = BorderStroke(1.dp, Color(0xFF10B981)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text("🐱 @${friend.friendUsername}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                  Text("Nível ${friend.friendLevel} • ${friend.friendWins}🏆", fontSize = 11.sp, color = Color(0xFFA7F3D0))
                }
                OutlinedButton(
                  onClick = { onlineVm.removeFriend(friend.friendUserId, friend.friendUsername) },
                  enabled = !isBusy,
                  border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                  modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                  Text("Remover", fontSize = 11.sp, color = Color(0xFFFCA5A5))
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SharedSkinsTabContent(
  onlineVm: OnlineMultiplayerViewModel,
  uiState: GameUiState,
  sharedSkinsState: OnlineLoadState<List<SharedSkinPost>>,
  isBusy: Boolean,
  onClaimSharedSkin: (String) -> Unit
) {
  val unlockedSkins = remember(
    uiState.championCount,
    uiState.totalCatsSaved,
    uiState.bestEndlessCats,
    uiState.discoveredItems,
    uiState.unlockedAchievements,
    uiState.bestTimeSec,
    uiState.playerLevel,
    uiState.hasCreatedCustomContent,
    uiState.purchasedSkinIds,
    uiState.tradedAwaySkinIds
  ) {
    ALL_HERO_SKINS.filter { skin ->
      skin.id == "classic_orange" ||
        (!uiState.tradedAwaySkinIds.contains(skin.id) &&
          (uiState.purchasedSkinIds.contains(skin.id) ||
            skin.isUnlocked(
              wins = uiState.championCount,
              totalCats = uiState.totalCatsSaved,
              bestEndless = uiState.bestEndlessCats,
              achievementsCount = uiState.unlockedAchievements.size,
              discoveredItemsCount = uiState.discoveredItems.size,
              ghostsScared = uiState.totalGhostsScared,
              dashesUsed = uiState.totalDashesUsed,
              modesUnlocked = uiState.unlockedModesCount,
              hasCreatedCustom = uiState.hasCreatedCustomContent
            )))
    }
  }

  var chosenSkinId by remember(uiState.selectedSkinId) { mutableStateOf(uiState.selectedSkinId) }
  val chosenSkin = ALL_HERO_SKINS.find { it.id == chosenSkinId } ?: unlockedSkins.first()
  var noteInput by remember { mutableStateOf("") }
  var recipientInput by remember { mutableStateOf("") }

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = Color(0xFF1E293B),
    border = BorderStroke(1.dp, Color(0xFFF472B6)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Text("🎨 Compartilhar uma Skin com Amigos", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFFF9A8D4))
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        unlockedSkins.forEach { skin ->
          FilterChip(
            selected = chosenSkin.id == skin.id,
            onClick = { chosenSkinId = skin.id },
            label = { Text("${skin.badgeEmoji} ${skin.name}", fontSize = 11.sp) },
            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))
      OutlinedTextField(
        value = recipientInput,
        onValueChange = { recipientInput = it.take(24) },
        label = { Text("Para @amigo (vazio = público para todos)", fontSize = 11.sp) },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = Color.White,
          unfocusedTextColor = Color.White,
          focusedBorderColor = Color(0xFFF472B6),
          unfocusedBorderColor = Color(0xFF475569)
        ),
        modifier = Modifier.fillMaxWidth()
      )
      Spacer(modifier = Modifier.height(6.dp))
      OutlinedTextField(
        value = noteInput,
        onValueChange = { noteInput = it.take(60) },
        label = { Text("Mensagem opcional", fontSize = 11.sp) },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = Color.White,
          unfocusedTextColor = Color.White,
          focusedBorderColor = Color(0xFFF472B6),
          unfocusedBorderColor = Color(0xFF475569)
        ),
        modifier = Modifier.fillMaxWidth()
      )
      Spacer(modifier = Modifier.height(8.dp))
      Button(
        onClick = {
          onlineVm.shareSkin(
            skinId = chosenSkin.id,
            skinName = chosenSkin.name,
            badgeEmoji = chosenSkin.badgeEmoji,
            customNote = noteInput,
            recipientUsername = recipientInput
          )
          noteInput = ""
        },
        enabled = !isBusy,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDB2777)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("share_skin_button")
      ) {
        Text("📤 Compartilhar ${chosenSkin.badgeEmoji} ${chosenSkin.name}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
      }
    }
  }

  Spacer(modifier = Modifier.height(10.dp))
  Text("✨ Mural de Skins Compartilhadas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFDE047))
  Spacer(modifier = Modifier.height(6.dp))

  when (sharedSkinsState) {
    is OnlineLoadState.Loading -> {
      Text("Carregando skins compartilhadas...", fontSize = 11.sp, color = Color(0xFF94A3B8))
    }
    is OnlineLoadState.Error -> {
      Text("⚠️ ${sharedSkinsState.message}", fontSize = 11.sp, color = Color(0xFFF87171))
    }
    is OnlineLoadState.Success -> {
      if (sharedSkinsState.data.isEmpty()) {
        Text("Nenhuma skin compartilhada ainda. Compartilhe a sua acima!", fontSize = 11.sp, color = Color(0xFF94A3B8))
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          sharedSkinsState.data.forEach { post ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0xFF1E293B),
              border = BorderStroke(1.dp, Color(0xFF475569)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "${post.badgeEmoji} ${post.skinName}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                  )
                  Text(
                    text = "por @${post.ownerUsername}",
                    fontSize = 11.sp,
                    color = Color(0xFF38BDF8)
                  )
                }
                if (post.recipientUsername.isNotBlank()) {
                  Text(
                    text = "🎁 Presente para @${post.recipientUsername}",
                    fontSize = 11.sp,
                    color = Color(0xFFFDE047)
                  )
                }
                if (post.customNote.isNotBlank()) {
                  Text(
                    text = "\"${post.customNote}\"",
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1)
                  )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  OutlinedButton(
                    onClick = { onlineVm.likeSharedSkin(post) },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                  ) {
                    Text("❤️ ${post.likes}", fontSize = 11.sp, color = Color(0xFFF9A8D4))
                  }
                  Button(
                    onClick = { onClaimSharedSkin(post.skinId) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    modifier = Modifier
                      .weight(1f)
                      .defaultMinSize(minHeight = 48.dp)
                  ) {
                    Text("🎨 Pegar & Usar Skin", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
