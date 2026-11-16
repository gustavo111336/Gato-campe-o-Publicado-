package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class MultiplayerRepositoryRuleTest : FirestoreEmulatorTestBase() {

  private fun randomCode(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    val raw = UUID.randomUUID().toString().replace("-", "").uppercase()
    return raw.take(6).map { if (it in 'A'..'Z' || it in '0'..'9') it else chars[0] }.joinToString("")
  }

  @Test
  fun upsertUserProfile_and_observeOwnProfile_succeeds() = runBlocking {
    val uid = signInTestUser("alice_cat@test.com")
    val repo = MultiplayerRepository(firestore)

    val saved = withTimeout(5000L) {
      repo.upsertUserProfile(uid, "AliceGato", "alice_cat@test.com", level = 3L, wins = 2L).getOrThrow()
    }
    assertEquals("AliceGato", saved.username)
    assertEquals(3L, saved.level)

    val observed = withTimeout(3000L) {
      repo.observeUserProfile(uid).first { it != null }
    }
    assertNotNull(observed)
    assertEquals("AliceGato", observed?.username)
  }

  @Test
  fun saveAndLoadCloudProgress_roundTripsAllFields() = runBlocking {
    val uid = signInTestUser("cloud_cat@test.com")
    val repo = MultiplayerRepository(firestore)

    val payload = CloudSavePayload(
      championCount = 12,
      bestTimeSec = 28.5f,
      bestHighScore = 4500,
      spendablePoints = 820,
      playerLevel = 7,
      playerXp = 40,
      totalCatsSaved = 150,
      bestEndlessCats = 42,
      totalGhostsScared = 19,
      totalItemsUsed = 33,
      totalDashesUsed = 24,
      totalSonicsUsed = 11,
      selectedSkinId = "cyber_neon",
      discoveredItemsCsv = "catnip,milk_shield",
      unlockedAchievementsCsv = "first_win,amor_felino",
      purchasedSkinsCsv = "cyber_neon,cosmic_galaxy",
      purchasedModesCsv = "turbo_frenzy",
      purchasedItemsCsv = "golden_collar",
      tradedAwaySkinsCsv = "snow_mingau",
      reviveCount = 4
    )

    val saved = withTimeout(5000L) {
      repo.saveCloudProgress(uid, "CloudMiau", "cloud_cat@test.com", payload).getOrThrow()
    }
    assertEquals(4500L, saved.bestHighScore)
    assertEquals(820L, saved.spendablePoints)
    assertEquals("cyber_neon", saved.selectedSkinId)
    assertEquals(4L, saved.reviveCount)

    val loaded = withTimeout(5000L) {
      repo.loadCloudProgress(uid).getOrThrow()
    }
    assertNotNull(loaded)
    assertEquals(12L, loaded?.championCount)
    assertEquals(4L, loaded?.reviveCount)
    assertEquals("cyber_neon,cosmic_galaxy", loaded?.purchasedSkinsCsv)
    assertEquals("snow_mingau", loaded?.tradedAwaySkinsCsv)
  }

  @Test
  fun directorySearch_friends_and_sharedSkins_succeeds() = runBlocking {
    val aliceUid = signInTestUser("alice_friend@test.com")
    val aliceRepo = MultiplayerRepository(firestore)
    withTimeout(5000L) {
      aliceRepo.upsertUserProfile(aliceUid, "AliceAmiga", "alice_friend@test.com", 5L, 4L, "cyber_neon").getOrThrow()
    }

    val bobUid = signInTestUser("bob_friend@test.com")
    val bobRepo = MultiplayerRepository(firestore)
    withTimeout(5000L) {
      bobRepo.upsertUserProfile(bobUid, "BobFelino", "bob_friend@test.com", 2L, 1L, "classic_orange").getOrThrow()
    }

    val results = withTimeout(5000L) {
      bobRepo.searchPlayersByUsername("alice", bobUid).getOrThrow()
    }
    assertTrue(results.any { it.userId == aliceUid && it.username == "AliceAmiga" })

    val aliceEntry = results.first { it.userId == aliceUid }
    withTimeout(5000L) {
      bobRepo.addFriend(bobUid, aliceEntry).getOrThrow()
    }
    val friends = withTimeout(3000L) {
      bobRepo.observeFriends(bobUid).first { list -> list.any { it.friendUserId == aliceUid } }
    }
    assertEquals("AliceAmiga", friends.first().friendUsername)

    val post = withTimeout(5000L) {
      bobRepo.shareSkin(
        ownerUserId = bobUid,
        ownerUsername = "BobFelino",
        skinId = "cyber_neon",
        skinName = "Cyber Gato Neon",
        badgeEmoji = "🤖",
        customNote = "Presente de amigo!",
        recipientUsername = "AliceAmiga"
      ).getOrThrow()
    }
    val shared = withTimeout(3000L) {
      bobRepo.observeSharedSkins().first { list -> list.any { it.shareId == post.shareId } }
    }
    assertTrue(shared.any { it.shareId == post.shareId && it.recipientUsername == "AliceAmiga" })

    withTimeout(5000L) {
      bobRepo.likeSharedSkin(post.shareId, post.likes).getOrThrow()
    }

    withTimeout(5000L) {
      bobRepo.removeFriend(bobUid, aliceUid).getOrThrow()
    }
    val friendsAfter = withTimeout(3000L) {
      bobRepo.observeFriends(bobUid).first { list -> list.none { it.friendUserId == aliceUid } }
    }
    assertTrue(friendsAfter.none { it.friendUserId == aliceUid })
  }

  @Test
  fun createAndJoinRoom_150Players_eliteVote_and_leaveRemovesImmediately() = runBlocking {
    val hostUid = signInTestUser("host_elite@test.com")
    val hostRepo = MultiplayerRepository(firestore)
    val code = randomCode()

    val created = withTimeout(5000L) {
      hostRepo.createRoom(
        hostUserId = hostUid,
        hostUsername = "HostElite",
        isPublic = true,
        targetCats = 10,
        maxPlayers = 150,
        difficulty = "ELITE",
        skinId = "classic_orange",
        explicitCode = code
      ).getOrThrow()
    }
    assertEquals(code, created.code)
    assertEquals(150L, created.maxPlayers)
    assertEquals("ELITE", created.difficulty)

    val guestUid = signInTestUser("guest_elite@test.com")
    val guestRepo = MultiplayerRepository(firestore)

    val (joinedRoom, joinedExisting) = withTimeout(5000L) {
      guestRepo.findAndJoinOrCreatePublicRoom(
        userId = guestUid,
        username = "GuestElite",
        skinId = "cyber_neon"
      ).getOrThrow()
    }
    assertTrue(joinedExisting)
    assertEquals("playing", joinedRoom.status)

    val chosenGhost = withTimeout(5000L) {
      guestRepo.castGhostVoteAndResolve(
        roomCode = joinedRoom.code,
        voterUserId = guestUid,
        voterUsername = "GuestElite",
        voteForUserId = hostUid,
        skinId = "cyber_neon"
      ).getOrThrow()
    }
    assertEquals(hostUid, chosenGhost)

    withTimeout(5000L) {
      guestRepo.syncPlayerProgress(
        roomCode = joinedRoom.code,
        userId = guestUid,
        username = "GuestElite",
        isHost = false,
        isGuestSlot = true,
        x = 640f,
        y = 1200f,
        score = 5,
        role = "cat",
        skinId = "cyber_neon",
        voteGhostForUid = hostUid
      ).getOrThrow()
    }

    val playersBeforeLeave = withTimeout(3000L) {
      guestRepo.observeRoomPlayers(joinedRoom.code).first { list -> list.any { it.userId == guestUid && it.score == 5L } }
    }
    assertTrue(playersBeforeLeave.any { it.userId == guestUid })

    withTimeout(5000L) {
      guestRepo.leaveRoomPlayer(joinedRoom.code, guestUid).getOrThrow()
    }

    val playersAfterLeave = withTimeout(3000L) {
      guestRepo.observeRoomPlayers(joinedRoom.code).first { list -> list.none { it.userId == guestUid } }
    }
    assertFalse(playersAfterLeave.any { it.userId == guestUid })
  }

  @Test
  fun unauthenticatedCaller_rejectedWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = MultiplayerRepository(firestore)

    var thrown: Exception? = null
    try {
      withTimeout(3000L) {
        repo.observePublicWaitingRooms().first()
      }
    } catch (e: Exception) {
      thrown = e
    }
    val fsEx = checkNotNull(thrown?.unwrapFirestoreException()) {
      "Expected FirebaseFirestoreException, got $thrown"
    }
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
  }

  @Test
  fun crossUserProfileRead_rejectedWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser("alice_pii@test.com")
    val aliceRepo = MultiplayerRepository(firestore)
    withTimeout(5000L) {
      aliceRepo.upsertUserProfile(aliceUid, "AlicePriv", "alice_pii@test.com", 1L, 0L).getOrThrow()
    }

    signInTestUser("bob_pii@test.com")
    val bobRepo = MultiplayerRepository(firestore)
    var thrown: Exception? = null
    try {
      withTimeout(3000L) {
        bobRepo.observeUserProfile(aliceUid).first()
      }
    } catch (e: Exception) {
      thrown = e
    }
    val fsEx = checkNotNull(thrown?.unwrapFirestoreException()) {
      "Expected FirebaseFirestoreException, got $thrown"
    }
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
  }
}
