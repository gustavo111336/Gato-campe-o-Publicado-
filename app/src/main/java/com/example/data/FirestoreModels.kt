package com.example.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

data class CloudSavePayload(
  val championCount: Int = 0,
  val bestTimeSec: Float = 0f,
  val bestHighScore: Int = 0,
  val spendablePoints: Int = 0,
  val playerLevel: Int = 1,
  val playerXp: Int = 0,
  val totalCatsSaved: Int = 0,
  val bestEndlessCats: Int = 0,
  val totalGhostsScared: Int = 0,
  val totalItemsUsed: Int = 0,
  val totalDashesUsed: Int = 0,
  val totalSonicsUsed: Int = 0,
  val selectedSkinId: String = "classic_orange",
  val discoveredItemsCsv: String = "",
  val unlockedAchievementsCsv: String = "",
  val purchasedSkinsCsv: String = "",
  val purchasedModesCsv: String = "",
  val purchasedItemsCsv: String = "",
  val tradedAwaySkinsCsv: String = "",
  val reviveCount: Int = 0
)

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val username: String = "",
  val email: String = "",
  val level: Long = 1L,
  val wins: Long = 0L,
  val championCount: Long = 0L,
  val bestTimeSec: Double = 0.0,
  val bestHighScore: Long = 0L,
  val spendablePoints: Long = 0L,
  val playerLevel: Long = 1L,
  val playerXp: Long = 0L,
  val totalCatsSaved: Long = 0L,
  val bestEndlessCats: Long = 0L,
  val totalGhostsScared: Long = 0L,
  val totalItemsUsed: Long = 0L,
  val totalDashesUsed: Long = 0L,
  val totalSonicsUsed: Long = 0L,
  val selectedSkinId: String = "classic_orange",
  val discoveredItemsCsv: String = "",
  val unlockedAchievementsCsv: String = "",
  val purchasedSkinsCsv: String = "",
  val purchasedModesCsv: String = "",
  val purchasedItemsCsv: String = "",
  val tradedAwaySkinsCsv: String = "",
  val reviveCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
  @ServerTimestamp val saveUpdatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AppReleaseInfo(
  @DocumentId val id: String = "",
  val latestVersionCode: Long = 1L,
  val latestVersionName: String = "1.0",
  val releaseNotes: String = "",
  val storeUrl: String = "",
  val forceUpdate: Boolean = false
)

@IgnoreExtraProperties
data class PlayerDirectoryEntry(
  @DocumentId val id: String = "",
  val userId: String = "",
  val username: String = "",
  val usernameLower: String = "",
  val level: Long = 1L,
  val wins: Long = 0L,
  val selectedSkinId: String = "classic_orange",
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FriendLink(
  @DocumentId val id: String = "",
  val friendUserId: String = "",
  val friendUsername: String = "",
  val friendLevel: Long = 1L,
  val friendWins: Long = 0L,
  val friendSkinId: String = "classic_orange",
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SharedSkinPost(
  @DocumentId val id: String = "",
  val shareId: String = "",
  val ownerUserId: String = "",
  val ownerUsername: String = "",
  val skinId: String = "",
  val skinName: String = "",
  val badgeEmoji: String = "🐱",
  val customNote: String = "",
  val recipientUsername: String = "",
  val likes: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MultiplayerRoom(
  @DocumentId val id: String = "",
  val code: String = "",
  val isPublic: Boolean = false,
  val status: String = "waiting",
  val difficulty: String = "NORMAL",
  val maxPlayers: Long = 100L,
  val playerCount: Long = 1L,
  val ghostPlayerUserId: String = "",
  val hostUserId: String = "",
  val hostUsername: String = "",
  val guestUserId: String = "",
  val guestUsername: String = "",
  val hostX: Double = 800.0,
  val hostY: Double = 1400.0,
  val hostScore: Long = 0L,
  val guestX: Double = 800.0,
  val guestY: Double = 1600.0,
  val guestScore: Long = 0L,
  val targetCats: Long = 10L,
  val winnerUserId: String = "",
  val winnerUsername: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class RoomPlayer(
  @DocumentId val id: String = "",
  val userId: String = "",
  val username: String = "",
  val x: Double = 800.0,
  val y: Double = 1400.0,
  val score: Long = 0L,
  val role: String = "cat",
  val skinId: String = "classic_orange",
  val voteGhostForUid: String = "",
  @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class OperationType(val value: String) {
  CREATE("create"),
  UPDATE("update"),
  DELETE("delete"),
  LIST("list"),
  GET("get"),
  WRITE("write")
}

fun handleFirestoreError(exception: Throwable, operationType: OperationType, path: String?): String {
  val causes = generateSequence(exception) { it.cause }
  val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
    ?: causes.firstOrNull { it !is CancellationException }
    ?: exception

  val currentUser = try {
    FirebaseAuth.getInstance().currentUser
  } catch (_: Exception) {
    null
  }

  val providerInfoList = currentUser?.providerData?.map { provider ->
    JSONObject().apply {
      put("providerId", provider.providerId)
      put("email", provider.email)
    }
  } ?: emptyList()

  val authInfoJson = JSONObject().apply {
    put("userId", currentUser?.uid)
    put("email", currentUser?.email)
    put("emailVerified", currentUser?.isEmailVerified)
    put("tenantId", currentUser?.tenantId)
    put("providerInfo", JSONArray(providerInfoList))
  }

  val errorInfoJson = JSONObject().apply {
    put("error", rootError.message ?: rootError.toString())
    put("operationType", operationType.value)
    put("path", path)
    put("authInfo", authInfoJson)
  }

  val jsonString = errorInfoJson.toString()
  Log.e("FirestoreError", "Firestore Error: $jsonString")
  return jsonString
}
