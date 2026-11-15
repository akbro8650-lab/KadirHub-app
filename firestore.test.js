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
    status: "pending",
    downloadCount: 0,
    averageRating: 0,
    reviewCount: 0,
    uploaderUid,
    uploaderName: "Community Uploader",
    iconBase64: "iVBORw0KGgoAAAANSUhEUgAAAAEAAAAB",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validEncryptedFile(uploaderUid, catalogItemId, overrides = {}) {
  const now = serverNow();
  return {
    catalogItemId,
    fileName: "creator_pack.apk",
    mimeType: "application/vnd.android.package-archive",
    byteSize: 2048,
    chunkCount: 0,
    ciphertextBase64: "ZW5jcnlwdGVkX2ZpbGVfYnl0ZXM=",
    ivBase64: "MTIzNDU2Nzg5MDEy",
    sha256Checksum: "b".repeat(64),
    encryptionAlgorithm: "AES-256-GCM",
    status: "pending",
    uploaderUid,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validEncryptedChunk(uploaderUid, fileId, catalogItemId, chunkIndex = 0, overrides = {}) {
  const now = serverNow();
  return {
    fileId,
    catalogItemId,
    chunkIndex,
    byteSize: 450000,
    ciphertextBase64: "ZW5jcnlwdGVkX2NodW5r",
    ivBase64: "MTIzNDU2Nzg5MDEy",
    status: "pending",
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

test("Guests can view approved catalog_items & visible reviews, but fail on unfiltered list, pending items, files, download increments, or writes", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db
      .collection("catalog_items")
      .doc("item_approved")
      .set(validCatalogItem(ADMIN_UID, { status: "approved", whatsNew: "Initial release", isFeatured: true }));
    await db
      .collection("catalog_items")
      .doc("item_pending")
      .set(validCatalogItem(ALICE_UID, { status: "pending" }));
    await db
      .collection("encrypted_files")
      .doc("file_approved")
      .set(validEncryptedFile(ADMIN_UID, "item_approved", { status: "approved" }));
    await db
      .collection("reviews")
      .doc("item_approved_alice")
      .set(validReview(ALICE_UID, "item_approved", { moderationStatus: "visible" }));
    await db
      .collection("announcements")
      .doc("ann_1")
      .set({
        title: "Welcome to KadirHub",
        message: "New 50 MB vault live!",
        actionUrl: "",
        active: true,
        authorEmail: ADMIN_EMAIL,
        createdAt: serverNow(),
        updatedAt: serverNow(),
      });
  });

  const unauthDb = testEnv.unauthenticatedContext().firestore();
  // Allowed: filtered approved catalog, visible reviews, active announcements
  await assertSucceeds(
    unauthDb.collection("catalog_items").where("status", "==", "approved").get()
  );
  await assertSucceeds(unauthDb.collection("catalog_items").doc("item_approved").get());
  await assertSucceeds(
    unauthDb.collection("reviews").where("itemId", "==", "item_approved").where("moderationStatus", "==", "visible").get()
  );
  await assertSucceeds(
    unauthDb.collection("announcements").where("active", "==", true).get()
  );

  // Blocked for guests: unfiltered list, pending item, encrypted_files, download increment, create/write, bookmarks
  await assertFails(unauthDb.collection("catalog_items").get());
  await assertFails(unauthDb.collection("catalog_items").doc("item_pending").get());
  await assertFails(unauthDb.collection("encrypted_files").doc("file_approved").get());
  await assertFails(
    unauthDb.collection("catalog_items").doc("item_approved").update({
      downloadCount: 1,
      updatedAt: serverNow(),
    })
  );
  await assertFails(
    unauthDb.collection("catalog_items").doc("item_guest_write").set(validCatalogItem("guest"))
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("item_approved").get()
  );
});

test("Email spoofing with email_verified=false cannot self-approve catalog items", async () => {
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
      .set(validCatalogItem("spoof_uid", { status: "approved" }))
  );
});

test("Regular user can upload pending APK/item & chunks (up to 50 MB) but cannot self-approve or write admins", async () => {
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true })
    .firestore();
  const bobDb = testEnv
    .authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true })
    .firestore();

  // Cannot self-approve
  await assertFails(
    aliceDb
      .collection("catalog_items")
      .doc("item_alice_self_approve")
      .set(validCatalogItem(ALICE_UID, { status: "approved" }))
  );
  await assertFails(
    aliceDb
      .collection("encrypted_files")
      .doc("file_alice_self_approve")
      .set(validEncryptedFile(ALICE_UID, "item_alice_self_approve", { status: "approved" }))
  );
  // Cannot write /admins
  await assertFails(
    aliceDb.collection("admins").doc(ALICE_UID).set({
      email: "alice@example.com",
      createdAt: serverNow(),
    })
  );

  // Can upload pending chunked 25 MB APK
  await assertSucceeds(
    aliceDb
      .collection("encrypted_files")
      .doc("file_alice_apk")
      .set(
        validEncryptedFile(ALICE_UID, "item_alice_apk", {
          byteSize: 25 * 1024 * 1024,
          chunkCount: 59,
          ciphertextBase64: "",
          ivBase64: "",
          status: "pending",
        })
      )
  );
  await assertSucceeds(
    aliceDb
      .collection("encrypted_files")
      .doc("file_alice_apk")
      .collection("chunks")
      .doc("chunk_0")
      .set(validEncryptedChunk(ALICE_UID, "file_alice_apk", "item_alice_apk", 0))
  );
  await assertSucceeds(
    aliceDb
      .collection("catalog_items")
      .doc("item_alice_apk")
      .set(
        validCatalogItem(ALICE_UID, {
          itemType: "encrypted_file",
          encryptedFileId: "file_alice_apk",
          fileSizeLabel: "25.0 MB",
          status: "pending",
        })
      )
  );

  // Exceeding 50 MB limit is rejected
  await assertFails(
    aliceDb
      .collection("encrypted_files")
      .doc("file_too_big")
      .set(
        validEncryptedFile(ALICE_UID, "item_alice_apk", {
          byteSize: 60 * 1024 * 1024,
          chunkCount: 135,
          ciphertextBase64: "",
          ivBase64: "",
          status: "pending",
        })
      )
  );

  // Alice can query her own uploads (including pending)
  await assertSucceeds(
    aliceDb.collection("catalog_items").where("uploaderUid", "==", ALICE_UID).get()
  );

  // Bob cannot read Alice's pending APK or catalog item directly
  await assertFails(bobDb.collection("catalog_items").doc("item_alice_apk").get());
  await assertFails(bobDb.collection("encrypted_files").doc("file_alice_apk").get());
  await assertFails(
    bobDb
      .collection("encrypted_files")
      .doc("file_alice_apk")
      .collection("chunks")
      .doc("chunk_0")
      .get()
  );
});

test("Uploader can edit own upload (resets to pending), while other users cannot edit it, and admin can approve", async () => {
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true })
    .firestore();
  const bobDb = testEnv
    .authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true })
    .firestore();
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true })
    .firestore();

  await assertSucceeds(
    aliceDb
      .collection("catalog_items")
      .doc("item_alice_edit")
      .set(validCatalogItem(ALICE_UID, { status: "pending" }))
  );

  // Admin approves Alice's upload
  await assertSucceeds(
    adminDb.collection("catalog_items").doc("item_alice_edit").update({
      status: "approved",
      updatedAt: serverNow(),
    })
  );

  // Bob CANNOT edit Alice's upload
  await assertFails(
    bobDb.collection("catalog_items").doc("item_alice_edit").update({
      title: "Hijacked App",
      status: "pending",
      updatedAt: serverNow(),
    })
  );

  // Alice CAN edit her own upload, but CANNOT keep it approved — must reset to pending
  await assertFails(
    aliceDb.collection("catalog_items").doc("item_alice_edit").update({
      title: "Alice App v2",
      status: "approved",
      updatedAt: serverNow(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("catalog_items").doc("item_alice_edit").update({
      title: "Alice App v2",
      version: "2.0.0",
      iconBase64: "bmV3X2ljb25fYmFzZTY0",
      status: "pending",
      updatedAt: serverNow(),
    })
  );
});

test("Verified admin can create approved catalog_item and encrypted_file, and shadow ghost fields are rejected", async () => {
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
      .set(
        validCatalogItem(ADMIN_UID, {
          itemType: "encrypted_file",
          encryptedFileId: "file_1",
          status: "approved",
        })
      )
  );

  await assertSucceeds(
    adminDb
      .collection("encrypted_files")
      .doc("file_1")
      .set(validEncryptedFile(ADMIN_UID, "item_1", { status: "approved" }))
  );

  // Shadow update / ghost field rejection
  await assertFails(
    adminDb
      .collection("catalog_items")
      .doc("item_ghost")
      .set({ ...validCatalogItem(ADMIN_UID, { status: "approved" }), ghostField: true })
  );
});

test("Regular user can query approved catalog_items, fails unfiltered list, and can increment downloadCount", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db
      .collection("catalog_items")
      .doc("item_approved")
      .set(validCatalogItem(ADMIN_UID, { status: "approved" }));
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
    await ctx
      .firestore()
      .collection("catalog_items")
      .doc("item_1")
      .set(validCatalogItem(ADMIN_UID, { status: "approved" }));
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
  // Cannot impersonate Bob
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

test("User bookmarks are strictly isolated per user and blocked for guests or other users", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = serverNow();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("item_1").set({
      userId: ALICE_UID,
      itemId: "item_1",
      createdAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").get()
  );
  // Bob cannot read or write Alice's bookmarks
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("bookmarks").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("item_2").set({
      userId: BOB_UID,
      itemId: "item_2",
      createdAt: now,
    })
  );
  // Alice can delete her own bookmark
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("item_1").delete()
  );
});

test("Admin can manage announcements, featured flags, and moderation notes while regular users cannot", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();
  const now = serverNow();

  // Regular user cannot create an announcement or self-feature an item
  await assertFails(
    aliceDb.collection("announcements").doc("ann_alice").set({
      title: "Fake Notice",
      message: "Unauthorized",
      actionUrl: "",
      active: true,
      authorEmail: "alice@example.com",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    aliceDb.collection("catalog_items").doc("item_featured_attempt").set(
      validCatalogItem(ALICE_UID, { isFeatured: true, status: "pending" })
    )
  );

  // Admin can create announcement & update isFeatured + moderationNote
  await assertSucceeds(
    adminDb.collection("announcements").doc("ann_kadir").set({
      title: "KadirHub v2.0 Launch",
      message: "Guest browsing, bookmarks, and analytics are live!",
      actionUrl: "https://www.tiktok.com/@kadirofficial007",
      active: true,
      authorEmail: ADMIN_EMAIL,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    adminDb.collection("catalog_items").doc("item_admin").set(
      validCatalogItem(ADMIN_UID, {
        status: "approved",
        whatsNew: "Added dark/light theme & spotlight",
        isFeatured: true,
        moderationNote: "Verified clean APK",
      })
    )
  );
  await assertSucceeds(
    adminDb.collection("announcements").doc("ann_kadir").update({
      active: false,
      updatedAt: serverNow(),
    })
  );
  // Regular user cannot read inactive announcement
  await assertFails(
    aliceDb.collection("announcements").doc("ann_kadir").get()
  );
});

