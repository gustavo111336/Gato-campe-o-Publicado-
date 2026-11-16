const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validRoomPayload(code, hostUid, isPublic = false, maxPlayers = 100, difficulty = "NORMAL") {
  return {
    code,
    isPublic,
    status: "waiting",
    difficulty,
    maxPlayers,
    playerCount: 1,
    ghostPlayerUserId: "",
    hostUserId: hostUid,
    hostUsername: "GatoHost",
    guestUserId: "",
    guestUsername: "",
    hostX: 800,
    hostY: 1400,
    hostScore: 0,
    guestX: 800,
    guestY: 1600,
    guestScore: 0,
    targetCats: 10,
    winnerUserId: "",
    winnerUsername: "",
    createdAt: new Date(Date.now() - 1000),
    updatedAt: new Date(Date.now() - 1000),
  };
}

function validPlayerPayload(uid, username, role = "cat", vote = "") {
  return {
    userId: uid,
    username,
    x: 800,
    y: 1200,
    score: 0,
    role,
    skinId: "classic_orange",
    voteGhostForUid: vote,
    updatedAt: new Date(Date.now() - 500),
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or list users, directory, sharedSkins, or rooms", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("playerDirectory").get());
  await assertFails(unauthDb.collection("sharedSkins").get());
  await assertFails(unauthDb.collection("rooms").where("isPublic", "==", true).get());
});

test("Cloud save on own profile succeeds and blocks cross-user reads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "AliceCat",
      email: "alice@example.com",
      level: 1,
      wins: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      championCount: 5,
      spendablePoints: 1200,
      purchasedSkinsCsv: "cyber_neon,cosmic_galaxy",
      selectedSkinId: "cyber_neon",
      reviveCount: 3,
      saveUpdatedAt: new Date(Date.now() - 300),
      updatedAt: new Date(Date.now() - 300),
    })
  );

  // Authenticated user can read /appConfig/release but cannot write to it; unauthenticated cannot read
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("appConfig").doc("release").get());
  await assertSucceeds(aliceDb.collection("appConfig").doc("release").get());
  await assertFails(
    aliceDb.collection("appConfig").doc("release").set({ latestVersionCode: 99 })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("Friends subcollection, playerDirectory without PII, and sharedSkins work securely", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 500);

  await assertSucceeds(
    bobDb.collection("playerDirectory").doc(BOB_UID).set({
      userId: BOB_UID,
      username: "BobFelino",
      usernameLower: "bobfelino",
      level: 4,
      wins: 7,
      selectedSkinId: "cyber_neon",
      updatedAt: now,
    })
  );

  // Directory cannot contain email PII
  await assertFails(
    aliceDb.collection("playerDirectory").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "AliceCat",
      usernameLower: "alicecat",
      email: "leak@example.com",
      level: 1,
      wins: 0,
      selectedSkinId: "classic_orange",
      updatedAt: now,
    })
  );

  // Alice adds Bob as friend
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("friends").doc(BOB_UID).set({
      friendUserId: BOB_UID,
      friendUsername: "BobFelino",
      friendLevel: 4,
      friendWins: 7,
      friendSkinId: "cyber_neon",
      createdAt: now,
    })
  );

  // Bob cannot read Alice's friends list
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("friends").get()
  );

  // Alice shares a skin; Bob can read and like it
  await assertSucceeds(
    aliceDb.collection("sharedSkins").doc("share_01").set({
      shareId: "share_01",
      ownerUserId: ALICE_UID,
      ownerUsername: "AliceCat",
      skinId: "cyber_neon",
      skinName: "Cyber Gato Neon",
      badgeEmoji: "🤖",
      customNote: "Minha skin favorita!",
      recipientUsername: "",
      likes: 0,
      createdAt: now,
    })
  );

  await assertSucceeds(bobDb.collection("sharedSkins").get());
  await assertSucceeds(bobDb.collection("sharedSkins").doc("share_01").update({ likes: 1 }));
});

test("Public matchmaking auto-search query, 150-player room, Elite Ghost voting, and instant player leave work", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("rooms").doc("ELT150").set(validRoomPayload("ELT150", ALICE_UID, true, 150, "ELITE"))
  );
  await assertSucceeds(
    aliceDb.collection("rooms").doc("ELT150").collection("players").doc(ALICE_UID).set(validPlayerPayload(ALICE_UID, "AliceHost", "cat", BOB_UID))
  );

  // Instant public search query matches waiting or playing public rooms
  await assertSucceeds(
    bobDb.collection("rooms").where("isPublic", "==", true).where("status", "in", ["waiting", "playing"]).get()
  );
  // Blanket rooms.get() without filter must fail
  await assertFails(bobDb.collection("rooms").get());

  // Bob and Charlie join the 150-player room and vote on ghost
  await assertSucceeds(
    bobDb.collection("rooms").doc("ELT150").collection("players").doc(BOB_UID).set(validPlayerPayload(BOB_UID, "BobPlayer", "ghost", BOB_UID))
  );
  await assertSucceeds(
    bobDb.collection("rooms").doc("ELT150").update({
      guestUserId: BOB_UID,
      guestUsername: "BobPlayer",
      playerCount: 2,
      status: "playing",
      updatedAt: new Date(Date.now() - 300),
    })
  );
  await assertSucceeds(
    charlieDb.collection("rooms").doc("ELT150").collection("players").doc(CHARLIE_UID).set(validPlayerPayload(CHARLIE_UID, "Charlie3", "cat", BOB_UID))
  );

  // Bob cannot impersonate Charlie in players subcollection
  await assertFails(
    bobDb.collection("rooms").doc("ELT150").collection("players").doc(CHARLIE_UID).update({ score: 99 })
  );

  // Instant leave: Charlie deletes own player doc
  await assertSucceeds(
    charlieDb.collection("rooms").doc("ELT150").collection("players").doc(CHARLIE_UID).delete()
  );

  // Finish match & verify terminal lock
  await assertSucceeds(
    bobDb.collection("rooms").doc("ELT150").update({
      status: "finished",
      ghostPlayerUserId: BOB_UID,
      winnerUserId: BOB_UID,
      winnerUsername: "BobPlayer",
      updatedAt: new Date(Date.now() - 100),
    })
  );
  await assertFails(
    aliceDb.collection("rooms").doc("ELT150").update({
      hostScore: 5,
      updatedAt: new Date(Date.now() - 50),
    })
  );
});
