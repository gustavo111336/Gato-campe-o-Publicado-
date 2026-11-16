package com.example.ui

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.CloudSavePayload
import com.example.data.FriendLink
import com.example.data.MultiplayerRepository
import com.example.data.MultiplayerRoom
import com.example.data.PlayerDirectoryEntry
import com.example.data.RoomPlayer
import com.example.data.SharedSkinPost
import com.example.data.UserProfile
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed interface OnlineLoadState<out T> {
  data object Loading : OnlineLoadState<Nothing>
  data class Success<T>(val data: T) : OnlineLoadState<T>
  data class Error(val message: String) : OnlineLoadState<Nothing>
}

class OnlineMultiplayerViewModel(
  private val repository: MultiplayerRepository,
  private val currentUserId: String,
  private val defaultEmail: String,
  private val defaultDisplayName: String
) : ViewModel() {

  val userProfileState: StateFlow<OnlineLoadState<UserProfile?>> =
    repository.observeUserProfile(currentUserId)
      .map<UserProfile?, OnlineLoadState<UserProfile?>> { OnlineLoadState.Success(it) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        emit(OnlineLoadState.Error(rootError.message ?: "Erro ao carregar perfil."))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = OnlineLoadState.Loading
      )

  val publicRoomsState: StateFlow<OnlineLoadState<List<MultiplayerRoom>>> =
    repository.observePublicWaitingRooms()
      .map<List<MultiplayerRoom>, OnlineLoadState<List<MultiplayerRoom>>> { rooms ->
        OnlineLoadState.Success(rooms.filter { it.playerCount < it.maxPlayers })
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        emit(OnlineLoadState.Error(rootError.message ?: "Erro ao buscar salas públicas."))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = OnlineLoadState.Loading
      )

  val friendsState: StateFlow<OnlineLoadState<List<FriendLink>>> =
    repository.observeFriends(currentUserId)
      .map<List<FriendLink>, OnlineLoadState<List<FriendLink>>> { OnlineLoadState.Success(it) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        emit(OnlineLoadState.Error(rootError.message ?: "Erro ao carregar amizades."))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = OnlineLoadState.Loading
      )

  val sharedSkinsState: StateFlow<OnlineLoadState<List<SharedSkinPost>>> =
    repository.observeSharedSkins()
      .map<List<SharedSkinPost>, OnlineLoadState<List<SharedSkinPost>>> { OnlineLoadState.Success(it) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        emit(OnlineLoadState.Error(rootError.message ?: "Erro ao carregar skins compartilhadas."))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = OnlineLoadState.Loading
      )

  val gameplayVideosState: StateFlow<OnlineLoadState<List<com.example.data.GameplayVideoPost>>> =
    repository.observeGameplayVideos()
      .map<List<com.example.data.GameplayVideoPost>, OnlineLoadState<List<com.example.data.GameplayVideoPost>>> { OnlineLoadState.Success(it) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        emit(OnlineLoadState.Error(rootError.message ?: "Erro ao carregar vídeos."))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = OnlineLoadState.Loading
      )

  private val _activeRoom = MutableStateFlow<MultiplayerRoom?>(null)
  val activeRoom: StateFlow<MultiplayerRoom?> = _activeRoom.asStateFlow()

  private val _roomPlayersState = MutableStateFlow<List<RoomPlayer>>(emptyList())
  val roomPlayersState: StateFlow<List<RoomPlayer>> = _roomPlayersState.asStateFlow()

  private val _playerSearchResults = MutableStateFlow<List<PlayerDirectoryEntry>>(emptyList())
  val playerSearchResults: StateFlow<List<PlayerDirectoryEntry>> = _playerSearchResults.asStateFlow()

  private val _isSearchingPlayers = MutableStateFlow(false)
  val isSearchingPlayers: StateFlow<Boolean> = _isSearchingPlayers.asStateFlow()

  private val _cloudSaveStatus = MutableStateFlow<String?>(null)
  val cloudSaveStatus: StateFlow<String?> = _cloudSaveStatus.asStateFlow()

  private val _leftPlayerAlert = MutableStateFlow<String?>(null)
  val leftPlayerAlert: StateFlow<String?> = _leftPlayerAlert.asStateFlow()

  private val _statusMessage = MutableStateFlow<String?>(null)
  val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

  private val _errorMessage = MutableStateFlow<String?>(null)
  val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

  private val _isBusy = MutableStateFlow(false)
  val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

  private var roomListenerJob: Job? = null
  private var roomPlayersListenerJob: Job? = null
  private var previousOtherPlayers: Map<String, String> = emptyMap()

  fun suggestedUsername(): String {
    val profile = (userProfileState.value as? OnlineLoadState.Success)?.data
    if (profile != null && profile.username.isNotBlank()) return profile.username
    val base = defaultDisplayName.ifBlank {
      defaultEmail.substringBefore("@")
    }.replace(Regex("[^A-Za-z0-9_ ]"), "").trim()
    return base.ifBlank { "Gato${currentUserId.take(5)}" }.take(20)
  }

  fun clearMessages() {
    _statusMessage.value = null
    _errorMessage.value = null
  }

  fun consumeLeftPlayerAlert() {
    _leftPlayerAlert.value = null
  }

  fun blockUser(targetUserId: String, currentBlockedCsv: String) {
    viewModelScope.launch {
      repository.blockUser(currentUserId, targetUserId, currentBlockedCsv).fold(
        onSuccess = { _statusMessage.value = "🚫 Usuário bloqueado." },
        onFailure = { _errorMessage.value = "Erro ao bloquear: ${it.message}" }
      )
    }
  }

  fun unblockUser(targetUserId: String, currentBlockedCsv: String) {
    viewModelScope.launch {
      repository.unblockUser(currentUserId, targetUserId, currentBlockedCsv).fold(
        onSuccess = { _statusMessage.value = "✅ Usuário desbloqueado." },
        onFailure = { _errorMessage.value = "Erro ao desbloquear: ${it.message}" }
      )
    }
  }

  fun postGameplayVideo(title: String, score: Long, modeName: String, durationSec: Float, skinId: String, username: String) {
    viewModelScope.launch {
      val post = com.example.data.GameplayVideoPost(
        userId = currentUserId,
        username = username,
        userSkinId = skinId,
        title = title,
        score = score,
        modeName = modeName,
        durationSec = durationSec
      )
      repository.postGameplayVideo(post).fold(
        onSuccess = { _statusMessage.value = "🎬 Gameplay postada no feed com sucesso!" },
        onFailure = { _errorMessage.value = "Erro ao postar vídeo: ${it.message}" }
      )
    }
  }

  fun likeVideo(postId: String, currentLikes: Long, likedList: List<String>) {
    viewModelScope.launch {
      repository.likeGameplayVideo(postId, currentUserId, currentLikes, likedList)
    }
  }

  fun commentVideo(postId: String, text: String, username: String) {
    viewModelScope.launch {
      val comment = com.example.data.VideoComment(
        userId = currentUserId,
        username = username,
        text = text
      )
      repository.commentOnVideo(postId, comment)
    }
  }

  fun observeCommentsForPost(postId: String) = repository.observeVideoComments(postId)

  fun saveUsername(username: String, level: Int, wins: Int, selectedSkinId: String = "classic_orange") {
    val clean = username.trim()
    if (clean.length < 3) {
      _errorMessage.value = "O @usuário precisa ter pelo menos 3 caracteres."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _errorMessage.value = null
      _statusMessage.value = null
      val res = repository.upsertUserProfile(
        userId = currentUserId,
        username = clean,
        email = defaultEmail,
        level = level.toLong(),
        wins = wins.toLong(),
        selectedSkinId = selectedSkinId
      )
      _isBusy.value = false
      res.onSuccess {
        _statusMessage.value = "Perfil @$clean salvo na nuvem!"
      }.onFailure { err ->
        _errorMessage.value = err.localizedMessage ?: "Falha ao salvar perfil."
      }
    }
  }

  fun saveAllProgressToCloud(payload: CloudSavePayload) {
    val myName = suggestedUsername()
    viewModelScope.launch {
      _isBusy.value = true
      _errorMessage.value = null
      _cloudSaveStatus.value = "☁️ Salvando todo seu progresso na conta Google..."
      val res = repository.saveCloudProgress(
        userId = currentUserId,
        username = myName,
        email = defaultEmail,
        payload = payload
      )
      _isBusy.value = false
      res.onSuccess {
        val msg = "✅ Todo seu progresso foi salvo na conta Google (@$myName)!"
        _cloudSaveStatus.value = msg
        _statusMessage.value = msg
      }.onFailure { err ->
        val errMsg = err.localizedMessage ?: "Falha ao salvar progresso na nuvem."
        _cloudSaveStatus.value = "❌ $errMsg"
        _errorMessage.value = errMsg
      }
    }
  }

  fun loadAllProgressFromCloud(onApply: (UserProfile) -> Unit) {
    viewModelScope.launch {
      _isBusy.value = true
      _errorMessage.value = null
      _cloudSaveStatus.value = "☁️ Buscando progresso salvo na nuvem..."
      val res = repository.loadCloudProgress(currentUserId)
      _isBusy.value = false
      res.onSuccess { profile ->
        if (profile == null) {
          val msg = "Nenhum save encontrado ainda nesta conta. Clique em Salvar Progresso primeiro!"
          _cloudSaveStatus.value = msg
          _statusMessage.value = msg
        } else {
          onApply(profile)
          val msg = "✅ Progresso restaurado! Nível ${profile.playerLevel} • ${profile.championCount} vitórias • ${profile.spendablePoints} pts."
          _cloudSaveStatus.value = msg
          _statusMessage.value = msg
        }
      }.onFailure { err ->
        val errMsg = err.localizedMessage ?: "Falha ao carregar progresso da nuvem."
        _cloudSaveStatus.value = "❌ $errMsg"
        _errorMessage.value = errMsg
      }
    }
  }

  fun searchPlayers(query: String) {
    viewModelScope.launch {
      _isSearchingPlayers.value = true
      val res = repository.searchPlayersByUsername(query, excludeUserId = currentUserId)
      _isSearchingPlayers.value = false
      res.onSuccess { list ->
        _playerSearchResults.value = list
      }.onFailure { err ->
        _errorMessage.value = err.localizedMessage ?: "Falha ao pesquisar jogadores."
      }
    }
  }

  fun addFriend(entry: PlayerDirectoryEntry) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.addFriend(currentUserId, entry)
      _isBusy.value = false
      res.onSuccess {
        _statusMessage.value = "🤝 @${entry.username} adicionado aos amigos!"
      }.onFailure { err ->
        _errorMessage.value = err.localizedMessage ?: "Erro ao adicionar amizade."
      }
    }
  }

  fun removeFriend(friendUserId: String, friendUsername: String) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.removeFriend(currentUserId, friendUserId)
      _isBusy.value = false
      res.onSuccess {
        _statusMessage.value = "Amizade com @$friendUsername removida."
      }.onFailure { err ->
        _errorMessage.value = err.localizedMessage ?: "Erro ao remover amizade."
      }
    }
  }

  fun shareSkin(
    skinId: String,
    skinName: String,
    badgeEmoji: String,
    customNote: String,
    recipientUsername: String
  ) {
    val myName = suggestedUsername()
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.shareSkin(
        ownerUserId = currentUserId,
        ownerUsername = myName,
        skinId = skinId,
        skinName = skinName,
        badgeEmoji = badgeEmoji,
        customNote = customNote,
        recipientUsername = recipientUsername
      )
      _isBusy.value = false
      res.onSuccess {
        _statusMessage.value = if (recipientUsername.isBlank()) {
          "$badgeEmoji Skin '$skinName' compartilhada com todos!"
        } else {
          "$badgeEmoji Skin '$skinName' enviada para @${recipientUsername.trim().removePrefix("@")}!"
        }
      }.onFailure { err ->
        _errorMessage.value = err.localizedMessage ?: "Falha ao compartilhar skin."
      }
    }
  }

  fun likeSharedSkin(post: SharedSkinPost) {
    viewModelScope.launch {
      repository.likeSharedSkin(post.shareId.ifBlank { post.id }, post.likes)
    }
  }

  fun createRoom(
    isPublic: Boolean,
    playerLevel: Int,
    wins: Int,
    maxPlayers: Int = 100,
    difficulty: String = "NORMAL",
    skinId: String = "classic_orange",
    targetCats: Int = 10
  ) {
    val myName = suggestedUsername()
    viewModelScope.launch {
      _isBusy.value = true
      _errorMessage.value = null
      _statusMessage.value = null
      repository.upsertUserProfile(
        userId = currentUserId,
        username = myName,
        email = defaultEmail,
        level = playerLevel.toLong(),
        wins = wins.toLong(),
        selectedSkinId = skinId
      )
      val res = repository.createRoom(
        hostUserId = currentUserId,
        hostUsername = myName,
        isPublic = isPublic,
        targetCats = targetCats,
        maxPlayers = maxPlayers,
        difficulty = difficulty,
        skinId = skinId
      )
      _isBusy.value = false
      res.onSuccess { room ->
        attachRoomListener(room.code)
        _statusMessage.value = if (isPublic) {
          "Sala pública ${room.code} criada (${room.maxPlayers} vagas, ${room.difficulty})!"
        } else {
          "Sala privada ${room.code} criada (${room.maxPlayers} vagas)! Compartilhe o código."
        }
      }.onFailure { err ->
        _errorMessage.value = err.localizedMessage ?: "Não foi possível criar a sala."
      }
    }
  }

  fun joinRoom(
    code: String,
    playerLevel: Int,
    wins: Int,
    skinId: String = "classic_orange"
  ) {
    val cleanCode = code.trim().uppercase()
    if (cleanCode.length != 6) {
      _errorMessage.value = "Digite um código válido com 6 letras ou números."
      return
    }
    val myName = suggestedUsername()
    viewModelScope.launch {
      _isBusy.value = true
      _errorMessage.value = null
      _statusMessage.value = null
      repository.upsertUserProfile(
        userId = currentUserId,
        username = myName,
        email = defaultEmail,
        level = playerLevel.toLong(),
        wins = wins.toLong(),
        selectedSkinId = skinId
      )
      val res = repository.joinRoomByCode(
        roomCode = cleanCode,
        guestUserId = currentUserId,
        guestUsername = myName,
        skinId = skinId
      )
      _isBusy.value = false
      res.onSuccess { room ->
        attachRoomListener(room.code)
        _statusMessage.value = "Você entrou na sala ${room.code} (${room.playerCount}/${room.maxPlayers})!"
      }.onFailure { err ->
        _errorMessage.value = err.localizedMessage ?: "Não foi possível entrar na sala."
      }
    }
  }

  fun quickJoinPublicMatch(
    playerLevel: Int,
    wins: Int,
    maxPlayers: Int = 100,
    difficulty: String = "NORMAL",
    skinId: String = "classic_orange"
  ) {
    val myName = suggestedUsername()
    viewModelScope.launch {
      _isBusy.value = true
      _errorMessage.value = null
      _statusMessage.value = "🔍 Pesquisando partida pública disponível..."
      repository.upsertUserProfile(
        userId = currentUserId,
        username = myName,
        email = defaultEmail,
        level = playerLevel.toLong(),
        wins = wins.toLong(),
        selectedSkinId = skinId
      )
      val res = repository.findAndJoinOrCreatePublicRoom(
        userId = currentUserId,
        username = myName,
        skinId = skinId,
        preferredMaxPlayers = maxPlayers,
        preferredDifficulty = difficulty,
        targetCats = 10
      )
      _isBusy.value = false
      res.onSuccess { (room, joinedExisting) ->
        attachRoomListener(room.code)
        _statusMessage.value = if (joinedExisting) {
          "⚡ Entrou na partida pública ${room.code} sem código! (${room.playerCount}/${room.maxPlayers})"
        } else {
          "🌐 Nenhuma sala aberta havia — sala pública ${room.code} (${room.maxPlayers} vagas) criada automaticamente!"
        }
      }.onFailure { err ->
        _errorMessage.value = err.localizedMessage ?: "Falha na busca de partida pública."
      }
    }
  }

  fun castGhostVote(targetUserId: String, skinId: String = "classic_orange") {
    val room = _activeRoom.value ?: return
    val myName = suggestedUsername()
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.castGhostVoteAndResolve(
        roomCode = room.code,
        voterUserId = currentUserId,
        voterUsername = myName,
        voteForUserId = targetUserId,
        skinId = skinId
      )
      _isBusy.value = false
      res.onSuccess { chosenUid ->
        val chosenName = _roomPlayersState.value.find { it.userId == chosenUid }?.username ?: "Eleito"
        _statusMessage.value = "🗳️ Voto registrado! Fantasma atual por votos: @$chosenName"
      }.onFailure { err ->
        _errorMessage.value = err.localizedMessage ?: "Falha ao votar no Fantasma."
      }
    }
  }

  fun syncArenaProgress(
    x: Float,
    y: Float,
    score: Int,
    role: String = "cat",
    skinId: String = "classic_orange",
    voteGhostForUid: String = "",
    winnerUserId: String? = null,
    winnerUsername: String? = null
  ) {
    val room = _activeRoom.value ?: return
    if (room.status == "finished") return
    val isHost = room.hostUserId == currentUserId
    val isGuestSlot = room.guestUserId == currentUserId
    val myName = suggestedUsername()
    viewModelScope.launch {
      repository.syncPlayerProgress(
        roomCode = room.code,
        userId = currentUserId,
        username = myName,
        isHost = isHost,
        isGuestSlot = isGuestSlot,
        x = x,
        y = y,
        score = score,
        role = role,
        skinId = skinId,
        voteGhostForUid = voteGhostForUid,
        winnerUserId = winnerUserId,
        winnerUsername = winnerUsername
      )
    }
  }

  fun leaveRoom() {
    val room = _activeRoom.value
    roomListenerJob?.cancel()
    roomListenerJob = null
    roomPlayersListenerJob?.cancel()
    roomPlayersListenerJob = null
    previousOtherPlayers = emptyMap()
    _activeRoom.value = null
    _roomPlayersState.value = emptyList()
    if (room != null) {
      viewModelScope.launch {
        withContext(NonCancellable) {
          repository.leaveRoomPlayer(room.code, currentUserId)
        }
      }
    }
    _statusMessage.value = "Você saiu da sala."
  }

  private fun attachRoomListener(roomCode: String) {
    roomListenerJob?.cancel()
    roomPlayersListenerJob?.cancel()
    previousOtherPlayers = emptyMap()

    roomListenerJob = viewModelScope.launch {
      repository.observeRoom(roomCode)
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          _errorMessage.value = (firestoreError ?: error).message ?: "Conexão com a sala perdida."
        }
        .collect { room ->
          _activeRoom.value = room
        }
    }

    roomPlayersListenerJob = viewModelScope.launch {
      repository.observeRoomPlayers(roomCode)
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
        }
        .collect { players ->
          val currentOtherMap = players
            .filter { it.userId != currentUserId && it.userId.isNotBlank() }
            .associate { it.userId to it.username.ifBlank { "Jogador" } }

          if (previousOtherPlayers.isNotEmpty()) {
            val departed = previousOtherPlayers.keys - currentOtherMap.keys
            if (departed.isNotEmpty()) {
              val firstLeftName = previousOtherPlayers[departed.first()] ?: "Jogador"
              val msg = if (departed.size == 1) {
                "🚪 @$firstLeftName saiu da partida!"
              } else {
                "🚪 @$firstLeftName e +${departed.size - 1} saíram da partida!"
              }
              _leftPlayerAlert.value = msg
              _statusMessage.value = msg
            }
          }
          previousOtherPlayers = currentOtherMap
          _roomPlayersState.value = players
        }
    }
  }

  override fun onCleared() {
    super.onCleared()
    roomListenerJob?.cancel()
    roomPlayersListenerJob?.cancel()
  }

  companion object {
    fun factory(
      context: Context,
      currentUserId: String,
      email: String,
      displayName: String
    ): ViewModelProvider.Factory {
      val repo = MultiplayerRepository(context.applicationContext)
      return object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          return OnlineMultiplayerViewModel(repo, currentUserId, email, displayName) as T
        }
      }
    }
  }
}

fun attemptAutoSignIn(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onUnauthenticated: () -> Unit,
  scope: CoroutineScope
) {
  if (Firebase.auth.currentUser != null) {
    onAuthSuccess()
    return
  }
  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (_: Exception) {
    onUnauthenticated()
    return
  }

  val googleIdOption = GetGoogleIdOption.Builder()
    .setFilterByAuthorizedAccounts(true)
    .setServerClientId(clientId)
    .setAutoSelectEnabled(true)
    .build()

  val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

  scope.launch {
    try {
      val result = credentialManager.getCredential(context, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onUnauthenticated()
      }
    } catch (_: Exception) {
      onUnauthenticated()
    }
  }
}

fun onGoogleSignInClicked(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  scope: CoroutineScope,
  onAuthCancelled: () -> Unit = {}
) {
  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (_: Exception) {
    onAuthError("Configuração Google Sign-In ausente (default_web_client_id).")
    return
  }

  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

  scope.launch {
    try {
      val activityContext = context as? Activity ?: context
      val result = credentialManager.getCredential(activityContext, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onAuthError("Tipo de credencial inesperado.")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
      onAuthCancelled()
    } catch (e: Exception) {
      Log.e("Auth", "Google Sign-In failed", e)
      onAuthError(e.localizedMessage ?: "Falha ao entrar com Google.")
    }
  }
}

fun signOutGoogleAccount(
  credentialManager: CredentialManager,
  scope: CoroutineScope,
  onSignOutComplete: () -> Unit = {}
) {
  scope.launch {
    withContext(NonCancellable) {
      try {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
      } catch (e: Exception) {
        Log.w("Auth", "Failed to clear credential state: ${e.message}")
      } finally {
        Firebase.auth.signOut()
        onSignOutComplete()
      }
    }
  }
}
