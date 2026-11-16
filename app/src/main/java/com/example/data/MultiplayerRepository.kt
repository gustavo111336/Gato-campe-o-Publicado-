package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.random.Random

class MultiplayerRepository(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  fun observeUserProfile(userId: String): Flow<UserProfile?> {
    val path = "users/$userId"
    return db.collection("users").document(userId)
      .snapshots()
      .map { snap ->
        if (snap.exists()) {
          snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else {
          null
        }
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        handleFirestoreError(firestoreError ?: error, OperationType.GET, path)
        throw error
      }
  }

  suspend fun upsertUserProfile(
    userId: String,
    username: String,
    email: String,
    level: Long,
    wins: Long,
    selectedSkinId: String = "classic_orange"
  ): Result<UserProfile> {
    val cleanUsername = username.trim().take(24)
    require(cleanUsername.length >= 3) { "O nome de usuário deve ter pelo menos 3 caracteres." }
    val cleanEmail = email.trim().take(120)
    val cleanSkin = selectedSkinId.ifBlank { "classic_orange" }.take(64)
    val docRef = db.collection("users").document(userId)
    return try {
      val existing = docRef.get().await()
      if (existing.exists()) {
        docRef.update(
          mapOf(
            "username" to cleanUsername,
            "email" to cleanEmail,
            "level" to level.coerceIn(1L, 9999L),
            "wins" to wins.coerceIn(0L, 999999L),
            "selectedSkinId" to cleanSkin,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        docRef.set(
          mapOf(
            "userId" to userId,
            "username" to cleanUsername,
            "email" to cleanEmail,
            "level" to level.coerceIn(1L, 9999L),
            "wins" to wins.coerceIn(0L, 999999L),
            "selectedSkinId" to cleanSkin,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
      syncPublicDirectoryEntry(userId, cleanUsername, level, wins, cleanSkin)
      val fresh = docRef.get().await()
        .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        ?: UserProfile(id = userId, userId = userId, username = cleanUsername, email = cleanEmail, level = level, wins = wins)
      Result.success(fresh)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  private suspend fun syncPublicDirectoryEntry(
    userId: String,
    username: String,
    level: Long,
    wins: Long,
    selectedSkinId: String
  ) {
    val dirRef = db.collection("playerDirectory").document(userId)
    dirRef.set(
      mapOf(
        "userId" to userId,
        "username" to username.take(24),
        "usernameLower" to username.lowercase().take(24),
        "level" to level.coerceIn(1L, 9999L),
        "wins" to wins.coerceIn(0L, 999999L),
        "selectedSkinId" to selectedSkinId.ifBlank { "classic_orange" }.take(64),
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()
  }

  suspend fun saveCloudProgress(
    userId: String,
    username: String,
    email: String,
    payload: CloudSavePayload
  ): Result<UserProfile> {
    val cleanUsername = username.trim().ifBlank { "Gato${userId.take(5)}" }.take(24)
    val safeName = if (cleanUsername.length < 3) cleanUsername.padEnd(3, '_') else cleanUsername
    val cleanEmail = email.trim().take(120)
    val docRef = db.collection("users").document(userId)

    val saveFields = mapOf(
      "username" to safeName,
      "email" to cleanEmail,
      "level" to payload.playerLevel.toLong().coerceIn(1L, 9999L),
      "wins" to payload.championCount.toLong().coerceIn(0L, 999999L),
      "championCount" to payload.championCount.toLong().coerceIn(0L, 999999L),
      "bestTimeSec" to payload.bestTimeSec.toDouble().coerceIn(0.0, 999999.0),
      "bestHighScore" to payload.bestHighScore.toLong().coerceIn(0L, 99999999L),
      "spendablePoints" to payload.spendablePoints.toLong().coerceIn(0L, 99999999L),
      "playerLevel" to payload.playerLevel.toLong().coerceIn(1L, 9999L),
      "playerXp" to payload.playerXp.toLong().coerceIn(0L, 9999999L),
      "totalCatsSaved" to payload.totalCatsSaved.toLong().coerceIn(0L, 9999999L),
      "bestEndlessCats" to payload.bestEndlessCats.toLong().coerceIn(0L, 999999L),
      "totalGhostsScared" to payload.totalGhostsScared.toLong().coerceIn(0L, 999999L),
      "totalItemsUsed" to payload.totalItemsUsed.toLong().coerceIn(0L, 999999L),
      "totalDashesUsed" to payload.totalDashesUsed.toLong().coerceIn(0L, 999999L),
      "totalSonicsUsed" to payload.totalSonicsUsed.toLong().coerceIn(0L, 999999L),
      "selectedSkinId" to payload.selectedSkinId.ifBlank { "classic_orange" }.take(64),
      "discoveredItemsCsv" to payload.discoveredItemsCsv.take(4096),
      "unlockedAchievementsCsv" to payload.unlockedAchievementsCsv.take(4096),
      "purchasedSkinsCsv" to payload.purchasedSkinsCsv.take(4096),
      "purchasedModesCsv" to payload.purchasedModesCsv.take(4096),
      "purchasedItemsCsv" to payload.purchasedItemsCsv.take(4096),
      "tradedAwaySkinsCsv" to payload.tradedAwaySkinsCsv.take(4096),
      "reviveCount" to payload.reviveCount.toLong().coerceIn(0L, 9999L),
      "updatedAt" to FieldValue.serverTimestamp(),
      "saveUpdatedAt" to FieldValue.serverTimestamp()
    )

    return try {
      val existing = docRef.get().await()
      if (existing.exists()) {
        docRef.update(saveFields).await()
      } else {
        docRef.set(
          saveFields + mapOf(
            "userId" to userId,
            "createdAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
      syncPublicDirectoryEntry(
        userId = userId,
        username = safeName,
        level = payload.playerLevel.toLong(),
        wins = payload.championCount.toLong(),
        selectedSkinId = payload.selectedSkinId
      )
      val fresh = docRef.get().await()
        .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        ?: UserProfile(id = userId, userId = userId, username = safeName, email = cleanEmail)
      Result.success(fresh)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun loadCloudProgress(userId: String): Result<UserProfile?> {
    val docRef = db.collection("users").document(userId)
    return try {
      val snap = docRef.get().await()
      val profile = if (snap.exists()) {
        snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      } else {
        null
      }
      Result.success(profile)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun fetchAppReleaseConfig(): Result<AppReleaseInfo?> {
    val docRef = db.collection("appConfig").document("release")
    return try {
      val snap = docRef.get().await()
      val info = if (snap.exists()) {
        snap.toObject(AppReleaseInfo::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      } else {
        null
      }
      Result.success(info)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  // --- FRIENDS & DIRECTORY ---

  suspend fun searchPlayersByUsername(query: String, excludeUserId: String): Result<List<PlayerDirectoryEntry>> {
    val cleanQuery = query.trim().removePrefix("@").lowercase()
    return try {
      val snap = if (cleanQuery.isEmpty()) {
        db.collection("playerDirectory")
          .orderBy("updatedAt", Query.Direction.DESCENDING)
          .limit(20)
          .get()
          .await()
      } else {
        db.collection("playerDirectory")
          .orderBy("usernameLower")
          .startAt(cleanQuery)
          .endAt("$cleanQuery\uf8ff")
          .limit(20)
          .get()
          .await()
      }
      val list = snap.toObjects(PlayerDirectoryEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .filter { it.userId != excludeUserId && it.userId.isNotBlank() }
      Result.success(list)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, "playerDirectory")
      Result.failure(e)
    }
  }

  fun observeFriends(userId: String): Flow<List<FriendLink>> {
    val path = "users/$userId/friends"
    return db.collection("users").document(userId).collection("friends")
      .snapshots()
      .map { snap ->
        snap.toObjects(FriendLink::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedBy { it.friendUsername.lowercase() }
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
        throw error
      }
  }

  suspend fun addFriend(userId: String, entry: PlayerDirectoryEntry): Result<Unit> {
    if (entry.userId.isBlank() || entry.userId == userId) {
      return Result.failure(IllegalArgumentException("Jogador inválido para amizade."))
    }
    val docRef = db.collection("users").document(userId).collection("friends").document(entry.userId)
    return try {
      docRef.set(
        mapOf(
          "friendUserId" to entry.userId,
          "friendUsername" to entry.username.ifBlank { "AmigoGato" }.take(24),
          "friendLevel" to entry.level.coerceIn(1L, 9999L),
          "friendWins" to entry.wins.coerceIn(0L, 999999L),
          "friendSkinId" to entry.selectedSkinId.ifBlank { "classic_orange" }.take(64),
          "createdAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun removeFriend(userId: String, friendUserId: String): Result<Unit> {
    val docRef = db.collection("users").document(userId).collection("friends").document(friendUserId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  // --- SHARED SKINS ---

  fun observeSharedSkins(): Flow<List<SharedSkinPost>> {
    val path = "sharedSkins"
    return db.collection("sharedSkins")
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .limit(40)
      .snapshots()
      .map { snap ->
        snap.toObjects(SharedSkinPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
        throw error
      }
  }

  suspend fun shareSkin(
    ownerUserId: String,
    ownerUsername: String,
    skinId: String,
    skinName: String,
    badgeEmoji: String,
    customNote: String,
    recipientUsername: String = ""
  ): Result<SharedSkinPost> {
    val shareId = "sk_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    val docRef = db.collection("sharedSkins").document(shareId)
    val cleanOwner = ownerUsername.trim().ifBlank { "GatoCriador" }.take(24)
    val cleanNote = customNote.trim().take(120)
    val cleanRecipient = recipientUsername.trim().removePrefix("@").take(24)
    val payload = mapOf(
      "shareId" to shareId,
      "ownerUserId" to ownerUserId,
      "ownerUsername" to cleanOwner,
      "skinId" to skinId.take(64),
      "skinName" to skinName.take(64),
      "badgeEmoji" to badgeEmoji.ifBlank { "🐱" }.take(16),
      "customNote" to cleanNote,
      "recipientUsername" to cleanRecipient,
      "likes" to 0L,
      "createdAt" to FieldValue.serverTimestamp()
    )
    return try {
      docRef.set(payload).await()
      Result.success(
        SharedSkinPost(
          id = shareId,
          shareId = shareId,
          ownerUserId = ownerUserId,
          ownerUsername = cleanOwner,
          skinId = skinId,
          skinName = skinName,
          badgeEmoji = badgeEmoji,
          customNote = cleanNote,
          recipientUsername = cleanRecipient,
          likes = 0L
        )
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun likeSharedSkin(shareId: String, currentLikes: Long): Result<Unit> {
    val docRef = db.collection("sharedSkins").document(shareId)
    return try {
      docRef.update("likes", (currentLikes + 1L).coerceAtLeast(1L)).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  // --- MULTIPLAYER ROOMS (2 TO 200 PLAYERS, AUTO PUBLIC SEARCH, ELITE VOTING, INSTANT LEAVE) ---

  fun observePublicWaitingRooms(): Flow<List<MultiplayerRoom>> {
    val path = "rooms"
    return db.collection("rooms")
      .whereEqualTo("isPublic", true)
      .whereIn("status", listOf("waiting", "playing"))
      .snapshots()
      .map { snap ->
        snap.toObjects(MultiplayerRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .filter { it.playerCount < it.maxPlayers }
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeRoom(roomCode: String): Flow<MultiplayerRoom?> {
    val cleanCode = roomCode.trim().uppercase()
    val path = "rooms/$cleanCode"
    return db.collection("rooms").document(cleanCode)
      .snapshots()
      .map { snap ->
        if (snap.exists()) {
          snap.toObject(MultiplayerRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else {
          null
        }
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        handleFirestoreError(firestoreError ?: error, OperationType.GET, path)
        throw error
      }
  }

  fun observeRoomPlayers(roomCode: String): Flow<List<RoomPlayer>> {
    val cleanCode = roomCode.trim().uppercase()
    val path = "rooms/$cleanCode/players"
    return db.collection("rooms").document(cleanCode).collection("players")
      .snapshots()
      .map { snap ->
        snap.toObjects(RoomPlayer::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
        throw error
      }
  }

  suspend fun createRoom(
    hostUserId: String,
    hostUsername: String,
    isPublic: Boolean,
    targetCats: Int = 10,
    maxPlayers: Int = 100,
    difficulty: String = "NORMAL",
    skinId: String = "classic_orange",
    explicitCode: String? = null
  ): Result<MultiplayerRoom> {
    val code = (explicitCode ?: generateRoomCode()).trim().uppercase()
    val safeName = hostUsername.trim().ifBlank { "GatoHost" }.take(24)
    val safeMax = maxPlayers.toLong().coerceIn(2L, 200L)
    val safeDiff = if (difficulty in listOf("NORMAL", "CALMO", "DESAFIO", "ELITE")) difficulty else "NORMAL"
    val safeSkin = skinId.ifBlank { "classic_orange" }.take(64)
    val docRef = db.collection("rooms").document(code)

    val payload = mapOf(
      "code" to code,
      "isPublic" to isPublic,
      "status" to "waiting",
      "difficulty" to safeDiff,
      "maxPlayers" to safeMax,
      "playerCount" to 1L,
      "ghostPlayerUserId" to "",
      "hostUserId" to hostUserId,
      "hostUsername" to safeName,
      "guestUserId" to "",
      "guestUsername" to "",
      "hostX" to 800.0,
      "hostY" to 1400.0,
      "hostScore" to 0L,
      "guestX" to 800.0,
      "guestY" to 1600.0,
      "guestScore" to 0L,
      "targetCats" to targetCats.toLong().coerceIn(1L, 100L),
      "winnerUserId" to "",
      "winnerUsername" to "",
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    return try {
      docRef.set(payload).await()
      docRef.collection("players").document(hostUserId).set(
        mapOf(
          "userId" to hostUserId,
          "username" to safeName,
          "x" to 800.0,
          "y" to 1400.0,
          "score" to 0L,
          "role" to "cat",
          "skinId" to safeSkin,
          "voteGhostForUid" to "",
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      val room = docRef.get().await()
        .toObject(MultiplayerRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        ?: MultiplayerRoom(
          id = code,
          code = code,
          isPublic = isPublic,
          status = "waiting",
          difficulty = safeDiff,
          maxPlayers = safeMax,
          playerCount = 1L,
          hostUserId = hostUserId,
          hostUsername = safeName,
          targetCats = targetCats.toLong()
        )
      Result.success(room)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun joinRoomByCode(
    roomCode: String,
    guestUserId: String,
    guestUsername: String,
    skinId: String = "classic_orange"
  ): Result<MultiplayerRoom> {
    val cleanCode = roomCode.trim().uppercase()
    if (!cleanCode.matches(Regex("^[A-Z0-9]{6}$"))) {
      return Result.failure(IllegalArgumentException("O código da sala precisa ter 6 letras/números."))
    }
    val docRef = db.collection("rooms").document(cleanCode)
    val safeName = guestUsername.trim().ifBlank { "GatoVisitante" }.take(24)
    val safeSkin = skinId.ifBlank { "classic_orange" }.take(64)
    return try {
      val snap = docRef.get().await()
      if (!snap.exists()) {
        return Result.failure(IllegalStateException("Sala '$cleanCode' não encontrada."))
      }
      val existing = snap.toObject(MultiplayerRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        ?: return Result.failure(IllegalStateException("Erro ao ler dados da sala."))

      if (existing.status == "finished") {
        return Result.failure(IllegalStateException("Essa partida já foi encerrada!"))
      }

      val playerRef = docRef.collection("players").document(guestUserId)
      val alreadyInRoom = playerRef.get().await().exists() || existing.hostUserId == guestUserId
      if (!alreadyInRoom && existing.playerCount >= existing.maxPlayers) {
        return Result.failure(IllegalStateException("Essa sala atingiu o limite de ${existing.maxPlayers} jogadores!"))
      }

      playerRef.set(
        mapOf(
          "userId" to guestUserId,
          "username" to safeName,
          "x" to 800.0,
          "y" to 1600.0,
          "score" to 0L,
          "role" to if (existing.ghostPlayerUserId == guestUserId) "ghost" else "cat",
          "skinId" to safeSkin,
          "voteGhostForUid" to "",
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()

      val newCount = if (alreadyInRoom) existing.playerCount.coerceAtLeast(1L) else (existing.playerCount + 1L).coerceAtMost(existing.maxPlayers)
      val roomUpdates = mutableMapOf<String, Any>(
        "playerCount" to newCount,
        "status" to if (newCount >= 2L || existing.status == "playing") "playing" else "waiting",
        "updatedAt" to FieldValue.serverTimestamp()
      )
      if (existing.guestUserId.isBlank() && existing.hostUserId != guestUserId) {
        roomUpdates["guestUserId"] = guestUserId
        roomUpdates["guestUsername"] = safeName
      }
      docRef.update(roomUpdates).await()

      val updated = docRef.get().await()
        .toObject(MultiplayerRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        ?: existing.copy(playerCount = newCount, status = "playing")
      Result.success(updated)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  /**
   * Auto-joins the first available open public room with NO code required,
   * or creates a new public room automatically if none is currently waiting.
   */
  suspend fun findAndJoinOrCreatePublicRoom(
    userId: String,
    username: String,
    skinId: String = "classic_orange",
    preferredMaxPlayers: Int = 100,
    preferredDifficulty: String = "NORMAL",
    targetCats: Int = 10
  ): Result<Pair<MultiplayerRoom, Boolean>> {
    return try {
      val snap = db.collection("rooms")
        .whereEqualTo("isPublic", true)
        .whereIn("status", listOf("waiting", "playing"))
        .limit(20)
        .get()
        .await()
      val candidate = snap.toObjects(MultiplayerRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .firstOrNull { it.playerCount < it.maxPlayers && it.hostUserId != userId }
        ?: snap.toObjects(MultiplayerRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .firstOrNull { it.playerCount < it.maxPlayers }

      if (candidate != null) {
        val joined = joinRoomByCode(candidate.code, userId, username, skinId)
        joined.map { it to true }
      } else {
        val created = createRoom(
          hostUserId = userId,
          hostUsername = username,
          isPublic = true,
          targetCats = targetCats,
          maxPlayers = preferredMaxPlayers,
          difficulty = preferredDifficulty,
          skinId = skinId
        )
        created.map { it to false }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, "rooms")
      Result.failure(e)
    }
  }

  suspend fun castGhostVoteAndResolve(
    roomCode: String,
    voterUserId: String,
    voterUsername: String,
    voteForUserId: String,
    skinId: String = "classic_orange"
  ): Result<String> {
    val cleanCode = roomCode.trim().uppercase()
    val roomRef = db.collection("rooms").document(cleanCode)
    val playerRef = roomRef.collection("players").document(voterUserId)
    return try {
      val existingPlayer = playerRef.get().await()
      if (existingPlayer.exists()) {
        playerRef.update(
          mapOf(
            "voteGhostForUid" to voteForUserId,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        playerRef.set(
          mapOf(
            "userId" to voterUserId,
            "username" to voterUsername.ifBlank { "GatoVoto" }.take(24),
            "x" to 800.0,
            "y" to 1400.0,
            "score" to 0L,
            "role" to "cat",
            "skinId" to skinId.ifBlank { "classic_orange" }.take(64),
            "voteGhostForUid" to voteForUserId,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }

      val allPlayers = roomRef.collection("players").get().await()
        .toObjects(RoomPlayer::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      val activeIds = allPlayers.map { it.userId }.toSet()
      val tally = mutableMapOf<String, Int>()
      for (p in allPlayers) {
        val target = p.voteGhostForUid
        if (target.isNotBlank() && activeIds.contains(target)) {
          tally[target] = (tally[target] ?: 0) + 1
        }
      }
      val chosenGhostUid = tally.entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .firstOrNull()?.key ?: ""

      roomRef.update(
        mapOf(
          "ghostPlayerUserId" to chosenGhostUid,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(chosenGhostUid)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
      Result.failure(e)
    }
  }

  suspend fun syncPlayerProgress(
    roomCode: String,
    userId: String,
    username: String,
    isHost: Boolean,
    isGuestSlot: Boolean,
    x: Float,
    y: Float,
    score: Int,
    role: String = "cat",
    skinId: String = "classic_orange",
    voteGhostForUid: String = "",
    winnerUserId: String? = null,
    winnerUsername: String? = null
  ): Result<Unit> {
    val cleanCode = roomCode.trim().uppercase()
    val docRef = db.collection("rooms").document(cleanCode)
    val playerRef = docRef.collection("players").document(userId)
    val safeX = x.toDouble().coerceIn(0.0, 2000.0)
    val safeY = y.toDouble().coerceIn(0.0, 3000.0)
    val safeScore = score.toLong().coerceIn(0L, 9999L)
    val safeRole = if (role == "ghost") "ghost" else "cat"
    val safeSkin = skinId.ifBlank { "classic_orange" }.take(64)

    return try {
      playerRef.set(
        mapOf(
          "userId" to userId,
          "username" to username.ifBlank { "GatoPlayer" }.take(24),
          "x" to safeX,
          "y" to safeY,
          "score" to safeScore,
          "role" to safeRole,
          "skinId" to safeSkin,
          "voteGhostForUid" to voteGhostForUid.take(128),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()

      val updates = mutableMapOf<String, Any>(
        "updatedAt" to FieldValue.serverTimestamp()
      )
      if (isHost) {
        updates["hostX"] = safeX
        updates["hostY"] = safeY
        updates["hostScore"] = safeScore
      } else if (isGuestSlot) {
        updates["guestX"] = safeX
        updates["guestY"] = safeY
        updates["guestScore"] = safeScore
      }
      if (!winnerUserId.isNullOrBlank()) {
        updates["status"] = "finished"
        updates["winnerUserId"] = winnerUserId
        updates["winnerUsername"] = (winnerUsername ?: "Campeão").take(24)
      }
      if (updates.size > 1 || !winnerUserId.isNullOrBlank()) {
        docRef.update(updates).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  /**
   * Overload preserved for clean compatibility in rule tests.
   */
  suspend fun syncPlayerProgress(
    roomCode: String,
    isHost: Boolean,
    x: Float,
    y: Float,
    score: Int,
    winnerUserId: String? = null,
    winnerUsername: String? = null
  ): Result<Unit> {
    val cleanCode = roomCode.trim().uppercase()
    val docRef = db.collection("rooms").document(cleanCode)
    val safeX = x.toDouble().coerceIn(0.0, 2000.0)
    val safeY = y.toDouble().coerceIn(0.0, 3000.0)
    val safeScore = score.toLong().coerceIn(0L, 9999L)

    val updates = mutableMapOf<String, Any>(
      (if (isHost) "hostX" else "guestX") to safeX,
      (if (isHost) "hostY" else "guestY") to safeY,
      (if (isHost) "hostScore" else "guestScore") to safeScore,
      "updatedAt" to FieldValue.serverTimestamp()
    )

    if (!winnerUserId.isNullOrBlank()) {
      updates["status"] = "finished"
      updates["winnerUserId"] = winnerUserId
      updates["winnerUsername"] = (winnerUsername ?: "Campeão").take(24)
    }

    return try {
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun leaveRoomPlayer(roomCode: String, userId: String): Result<Unit> {
    val cleanCode = roomCode.trim().uppercase()
    val roomRef = db.collection("rooms").document(cleanCode)
    val playerRef = roomRef.collection("players").document(userId)
    return try {
      playerRef.delete().await()
      val roomSnap = roomRef.get().await()
      if (roomSnap.exists()) {
        val room = roomSnap.toObject(MultiplayerRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        if (room != null && room.status != "finished") {
          val remainingPlayers = roomRef.collection("players").get().await().size().toLong()
          if (room.hostUserId == userId && remainingPlayers == 0L) {
            roomRef.delete().await()
          } else {
            val updates = mutableMapOf<String, Any>(
              "playerCount" to remainingPlayers.coerceIn(0L, room.maxPlayers),
              "updatedAt" to FieldValue.serverTimestamp()
            )
            if (room.guestUserId == userId) {
              updates["guestUserId"] = ""
              updates["guestUsername"] = ""
            }
            if (room.ghostPlayerUserId == userId) {
              updates["ghostPlayerUserId"] = ""
            }
            if (remainingPlayers <= 1L && room.status == "playing") {
              updates["status"] = "waiting"
            }
            roomRef.update(updates).await()
          }
        }
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, playerRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteRoom(roomCode: String): Result<Unit> {
    val docRef = db.collection("rooms").document(roomCode.trim().uppercase())
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun blockUser(currentUserId: String, targetUserId: String, currentBlockedCsv: String): Result<String> {
    val set = currentBlockedCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableSet()
    set.add(targetUserId)
    val nextCsv = set.joinToString(",")
    val docRef = db.collection("users").document(currentUserId)
    return try {
      docRef.update(mapOf("blockedUserIdsCsv" to nextCsv, "updatedAt" to FieldValue.serverTimestamp())).await()
      Result.success(nextCsv)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun unblockUser(currentUserId: String, targetUserId: String, currentBlockedCsv: String): Result<String> {
    val set = currentBlockedCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableSet()
    set.remove(targetUserId)
    val nextCsv = set.joinToString(",")
    val docRef = db.collection("users").document(currentUserId)
    return try {
      docRef.update(mapOf("blockedUserIdsCsv" to nextCsv, "updatedAt" to FieldValue.serverTimestamp())).await()
      Result.success(nextCsv)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  fun observeGameplayVideos(): Flow<List<GameplayVideoPost>> {
    val path = "gameplayVideos"
    return db.collection("gameplayVideos")
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .limit(50)
      .snapshots()
      .map { snap ->
        snap.toObjects(GameplayVideoPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
        throw error
      }
  }

  suspend fun postGameplayVideo(post: GameplayVideoPost): Result<Unit> {
    val docId = post.postId.ifBlank { UUID.randomUUID().toString() }
    val docRef = db.collection("gameplayVideos").document(docId)
    val payload = mapOf(
      "postId" to docId,
      "userId" to post.userId,
      "username" to post.username.take(24),
      "userSkinId" to post.userSkinId.take(64),
      "title" to post.title.take(120),
      "score" to post.score,
      "modeName" to post.modeName.take(64),
      "durationSec" to post.durationSec,
      "likes" to 0L,
      "likedByUserIds" to emptyList<String>(),
      "commentsCount" to 0L,
      "createdAt" to FieldValue.serverTimestamp()
    )
    return try {
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun likeGameplayVideo(postId: String, currentUserId: String, currentLikes: Long, likedList: List<String>): Result<Unit> {
    val docRef = db.collection("gameplayVideos").document(postId)
    val isAlreadyLiked = likedList.contains(currentUserId)
    val nextLiked = if (isAlreadyLiked) likedList - currentUserId else likedList + currentUserId
    val nextLikes = if (isAlreadyLiked) (currentLikes - 1).coerceAtLeast(0L) else currentLikes + 1L

    return try {
      docRef.update(
        mapOf(
          "likes" to nextLikes,
          "likedByUserIds" to nextLiked
        )
      ).await()
      Result.success(Unit)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun commentOnVideo(postId: String, comment: VideoComment): Result<Unit> {
    val commentId = comment.commentId.ifBlank { UUID.randomUUID().toString() }
    val postRef = db.collection("gameplayVideos").document(postId)
    val commentRef = postRef.collection("comments").document(commentId)
    val payload = mapOf(
      "commentId" to commentId,
      "userId" to comment.userId,
      "username" to comment.username.take(24),
      "text" to comment.text.take(280),
      "createdAt" to FieldValue.serverTimestamp()
    )
    return try {
      db.runBatch { batch ->
        batch.set(commentRef, payload)
        batch.update(postRef, "commentsCount", FieldValue.increment(1))
      }.await()
      Result.success(Unit)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, commentRef.path)
      Result.failure(e)
    }
  }

  fun observeVideoComments(postId: String): Flow<List<VideoComment>> {
    val path = "gameplayVideos/$postId/comments"
    return db.collection("gameplayVideos").document(postId).collection("comments")
      .orderBy("createdAt", Query.Direction.ASCENDING)
      .snapshots()
      .map { snap ->
        snap.toObjects(VideoComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
        throw error
      }
  }

  private fun generateRoomCode(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
  }
}
