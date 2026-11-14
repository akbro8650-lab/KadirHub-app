const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_kadir_uid";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_EMAIL = "kadir25.bd@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function serverNow() {
  return new Date(Date.now() - 1000);
}

function validCatalogItem(uploaderUid, overrides = {}) {
  const now = serverNow();
  return {
    title: "Kadir Studio Tools",
    description: "Essential creator utilities and presets by @kadirofficial007",
    category: "App",
    itemType: "link",
    version: "2.1.0",
    fileSizeLabel: "14.2 MB",
    creatorHandle: "@kadirofficial007",
    encryptedTargetUrl: "ZW5jcnlwdGVkX3VybF9wYXlsb2Fk",
    urlIv: "MTIzNDU2Nzg5MDEy",
    encryptedFileId: "",
    sha256Checksum: "a".repeat(64),
    encryptionAlgorithm: "AES-256-GCM",
    status: "approved",
    downloadCount: 0,
    averageRating: 0,
    reviewCount: 0,
    uploaderUid,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validEncryptedFile(uploaderUid, catalogItemId, overrides = {}) {
  const now = serverNow();
  return {
    catalogItemId,
    fileName: "creator_pack.zip",
    mimeType: "application/zip",
    byteSize: 2048,
    ciphertextBase64: "ZW5jcnlwdGVkX2ZpbGVfYnl0ZXM=",
    ivBase64: "MTIzNDU2Nzg5MDEy",
    sha256Checksum: "b".repeat(64),
    encryptionAlgorithm: "AES-256-GCM",
    status: "approved",
    uploaderUid,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validReview(userId, itemId, overrides = {}) {
  const now = serverNow();
  return {
    itemId,
    itemTitle: "Kadir Studio Tools",
    userId,
    authorName: "Alice",
    rating: 5,
    comment: "Fast and safe download!",
    moderationStatus: "visible",
    flagReason: "",
    createdAt: now,
    updatedAt: now,
    ...overrides,
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

test("Unauthenticated user cannot list or read catalog_items", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("catalog_items").where("status", "==", "approved").get()
  );
  await assertFails(unauthDb.collection("catalog_items").doc("item_1").get());
});

test("Email spoofing with email_verified=false cannot upload catalog items", async () => {
  const spoofDb = testEnv
    .authenticatedContext("spoof_uid", {
      email: ADMIN_EMAIL,
      email_verified: false,
    })
    .firestore();
  await assertFails(
    spoofDb
      .collection("catalog_items")
      .doc("item_spoof")
      .set(validCatalogItem("spoof_uid"))
  );
});

test("Regular user cannot upload catalog_items or encrypted_files or write admins", async () => {
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true })
    .firestore();
  await assertFails(
    aliceDb
      .collection("catalog_items")
      .doc("item_alice")
      .set(validCatalogItem(ALICE_UID))
  );
  await assertFails(
    aliceDb
      .collection("encrypted_files")
      .doc("file_alice")
      .set(validEncryptedFile(ALICE_UID, "item_alice"))
  );
  await assertFails(
    aliceDb.collection("admins").doc(ALICE_UID).set({
      email: "alice@example.com",
      createdAt: serverNow(),
    })
  );
});

test("Verified admin can create catalog_item and encrypted_file, and shadow ghost fields are rejected", async () => {
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();

  await assertSucceeds(
    adminDb
      .collection("catalog_items")
      .doc("item_1")
      .set(validCatalogItem(ADMIN_UID, { itemType: "encrypted_file", encryptedFileId: "file_1" }))
  );

  await assertSucceeds(
    adminDb
      .collection("encrypted_files")
      .doc("file_1")
      .set(validEncryptedFile(ADMIN_UID, "item_1"))
  );

  // Shadow update / ghost field rejection
  await assertFails(
    adminDb
      .collection("catalog_items")
      .doc("item_ghost")
      .set({ ...validCatalogItem(ADMIN_UID), ghostField: true })
  );
});

test("Regular user can query approved catalog_items, fails unfiltered list, and can increment downloadCount", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("catalog_items").doc("item_approved").set(validCatalogItem(ADMIN_UID));
    await db
      .collection("catalog_items")
      .doc("item_archived")
      .set(validCatalogItem(ADMIN_UID, { status: "archived" }));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  // Unfiltered query must fail (rules are not filters)
  await assertFails(aliceDb.collection("catalog_items").get());
  // Filtered query succeeds
  await assertSucceeds(
    aliceDb.collection("catalog_items").where("status", "==", "approved").get()
  );
  // Cannot read archived item directly
  await assertFails(aliceDb.collection("catalog_items").doc("item_archived").get());

  // Can increment downloadCount by exactly 1
  await assertSucceeds(
    aliceDb.collection("catalog_items").doc("item_approved").update({
      downloadCount: 1,
      updatedAt: serverNow(),
    })
  );
  // Cannot tamper with title or jump downloadCount by +50
  await assertFails(
    aliceDb.collection("catalog_items").doc("item_approved").update({
      downloadCount: 51,
      updatedAt: serverNow(),
    })
  );
  await assertFails(
    aliceDb.collection("catalog_items").doc("item_approved").update({
      title: "Hacked",
      updatedAt: serverNow(),
    })
  );
});

test("User review lifecycle: create visible review, flag by another user, hide by admin locks author edit", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("catalog_items").doc("item_1").set(validCatalogItem(ADMIN_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true })
    .firestore();

  const reviewId = `item_1_${ALICE_UID}`;
  // Alice creates visible review
  await assertSucceeds(
    aliceDb.collection("reviews").doc(reviewId).set(validReview(ALICE_UID, "item_1"))
  );
  // Cannot create review already hidden or impersonate Bob
  await assertFails(
    aliceDb
      .collection("reviews")
      .doc("fake_rev")
      .set(validReview(BOB_UID, "item_1"))
  );

  // Bob can flag Alice's visible review
  await assertSucceeds(
    bobDb.collection("reviews").doc(reviewId).update({
      moderationStatus: "flagged",
      flagReason: "Spam link",
      updatedAt: serverNow(),
    })
  );

  // Admin hides the flagged review
  await assertSucceeds(
    adminDb.collection("reviews").doc(reviewId).update({
      moderationStatus: "hidden",
      flagReason: "Violates policy",
      updatedAt: serverNow(),
    })
  );

  // Alice cannot edit or unhide a hidden review
  await assertFails(
    aliceDb.collection("reviews").doc(reviewId).update({
      comment: "Trying to bypass moderation",
      updatedAt: serverNow(),
    })
  );
});

test("Moderation reports are private to reporter and admin", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true })
    .firestore();

  const now = serverNow();
  await assertSucceeds(
    aliceDb.collection("moderation_reports").doc("rep_1").set({
      targetType: "catalog_item",
      targetId: "item_1",
      targetTitle: "Kadir Studio Tools",
      reporterUid: ALICE_UID,
      reporterName: "Alice",
      reason: "Broken link mirror",
      status: "open",
      adminNotes: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob cannot read Alice's report
  await assertFails(bobDb.collection("moderation_reports").doc("rep_1").get());
  // Admin can resolve it
  await assertSucceeds(
    adminDb.collection("moderation_reports").doc("rep_1").update({
      status: "resolved",
      adminNotes: "Updated mirror link",
      updatedAt: serverNow(),
    })
  );
});
