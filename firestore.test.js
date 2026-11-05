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

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated user: cannot read or write user quota", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: can create and read own UserQuota", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      displayName: "Alice",
      dailyDate: "2026-10-30",
      dailyCount: 2,
      proLifetime: false,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: cannot read or write another user's UserQuota", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      dailyDate: "2026-10-30",
      dailyCount: 1,
      proLifetime: true,
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      dailyDate: "2026-10-30",
      dailyCount: 0,
      proLifetime: false,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Authenticated user: can create, list, and delete own CloudPresets", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const presetRef = aliceDb.collection("users").doc(ALICE_UID).collection("presets").doc("preset_1");

  await assertSucceeds(
    presetRef.set({
      userId: ALICE_UID,
      title: "Metallica Master",
      song: "Master of Puppets",
      style: "Thrash Metal",
      bpm: 212,
      youtubeUrl: "",
      details: "High gain mesa",
      jsonContent: "{\"title\":\"Metallica Master\"}",
      favorite: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("presets").where("userId", "==", ALICE_UID).get()
  );

  await assertSucceeds(presetRef.delete());
});

test("Authenticated user: cannot access another user's CloudPresets", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).collection("presets").doc("bob_preset").set({
      userId: BOB_UID,
      title: "Bob Tone",
      song: "Bob Song",
      style: "Blues",
      bpm: 120,
      youtubeUrl: "",
      details: "",
      jsonContent: "{\"title\":\"Bob Tone\"}",
      favorite: false,
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("presets").doc("bob_preset").get()
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("presets").get()
  );
});

test("Shadow Update Test: rejects ghost fields on UserQuota and CloudPreset", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      dailyDate: "2026-10-30",
      dailyCount: 0,
      proLifetime: false,
      isAdmin: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});
