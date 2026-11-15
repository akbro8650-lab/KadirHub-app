# KadirHub

A community-driven Android app and resource vault created by **[@kadirofficial007](https://www.tiktok.com/@kadirofficial007)**, built with **Kotlin**, **Jetpack Compose (Material 3)**, and **Firebase Firestore**.

Anyone can open **KadirHub** as a guest to browse approved releases, announcements, and reviews, while **Google Sign-In** unlocks interactive features—downloading APKs, copying decrypted links, bookmarking favorites, submitting or editing community uploads, rating/reviewing, and reporting. All community submissions and edits go through an admin approval workflow and are secured with **AES-256-GCM** encryption.

---

## Key Features

### Explore & Discovery
- **Guest Browsing + Sign-In Gating:** Browse approved apps, *What's New* release notes, featured spotlights, and active announcements immediately without signing in. Interactive actions cleanly prompt for Google Sign-In via Jetpack Credential Manager.
- **Featured Spotlight & Live Announcements:** Admin-curated **Featured Spotlight** carousel and active announcement banners pinned at the top of the **Explore** feed.
- **Advanced Filtering & Sorting:** Filter by category (`All`, `App`, `Link`, `Tool`, `Game`, `Resource`) and release type (`All`, `Vault APK`, `Links`, `Saved`), search in real time, and sort by `Newest`, `Top Rated`, `Downloads`, or `A–Z`.
- **Bookmarks & Native Sharing:** Save favorite releases to your personal cloud bookmarks (`/users/{uid}/bookmarks`) and share release details using Android's native share sheet.

### Community Uploads & Editing
- **Submit APKs or Links:** Authenticated users can upload an encrypted APK/file or curated external link via the **Upload** tab and track approval status under **My Uploads**.
- **Custom App Icons:** Attach, preview, and update custom app icons using Android's zero-permission **Photo Picker** (automatically compressed into WebP).
- **What's New & Admin Feedback:** Add structured changelog notes (`whatsNew`) to releases. If an admin flags or requests changes with a `moderationNote`, the feedback banner appears inside **My Uploads** and item details.
- **Uploader Edit & Delete Controls:** Uploaders can update their own releases (title, description, what's new, category, version, file size label, link, icon, or replacement APK) or delete them anytime. Community edits automatically return to `pending` status for admin re-review.

### Encrypted Vault & Integrity Verification
- **50 MB Chunked AES-256-GCM Vault:** Supports encrypted file uploads up to **50 MB** (`52,428,800` bytes) split into ~450 KB chunks (`AES/GCM/NoPadding` with unique 12-byte random IVs per chunk/payload) and verified end-to-end with streaming **SHA-256** checksums.
- **Link Vault Protection:** External download URLs are also AES-256-GCM encrypted in Firestore and decrypted on demand only for signed-in users.
- **Cache & Theme Controls:** Switch between **System**, **Dark**, and **Light** Material 3 themes (persisted locally), track your upload/bookmark counts, and inspect or clear the local decrypted temp cache from **Vault Info**.

### Ratings, Reviews & Moderation
- **Star Ratings & Reviews:** Signed-in users can rate approved items (1–5 stars), post or edit reviews, and flag inappropriate reviews or items.
- **Zero-Trust Security Rules:** Public reads are strictly scoped to approved catalog items, visible reviews, and active announcements; all writes, encrypted payloads/chunks, download counts, bookmarks, and moderation actions are enforced by `firestore.rules`.

### Passwordless Admin Command Center
- **Email & Role-Gated Access:** No client-side password gate—the **Admin** tab and moderation tools appear exclusively for verified **`kadir25.bd@gmail.com`** and server-authorized `/admins/{uid}` accounts.
- **5-Tab Moderation Workspace:**
  - **Overview:** Live catalog metrics, status totals, category distribution bars, and top-downloaded releases.
  - **Approvals:** Search/filter queue, **Bulk Approve Pending**, **Flag with Moderator Feedback** (`moderationNote`), **Feature / Pin** to spotlight, Archive, Restore, and Delete.
  - **Publish:** Publish pre-approved releases directly with optional Featured Spotlight pinning.
  - **Notices:** Broadcast, toggle active state, and delete Explore announcement banners.
  - **Moderation:** Filter open/all reports, moderate/delete reviews, and use one-tap **Hide Target & Resolve**.

---

## Tech Stack

- **Language:** Kotlin (JVM 11)
- **UI:** Jetpack Compose, Material Design 3 (Edge-to-Edge, Window Size Class adaptive layouts, System/Dark/Light theming, Android Photo Picker, Storage Access Framework, `FileProvider`)
- **Architecture:** MVVM with `ViewModel`, Kotlin Coroutines & `StateFlow`
- **Authentication:** Firebase Auth + Jetpack Credential Manager (Google Sign-In)
- **Database & Security:** Cloud Firestore with zero-trust `firestore.rules` (guest-readable approved catalog, owner-isolated bookmarks, chunked encrypted subcollections, and verified admin moderation)
- **Cryptography:** `javax.crypto` (`AES/GCM/NoPadding`, 256-bit key, 128-bit auth tag) & streaming `SHA-256`
- **Testing:** JUnit 4, Robolectric, and Firebase Local Emulator Suite (`firestore.test.js` + `CatalogRepositoryRuleTest`)

---

## Creator

- **TikTok:** [@kadirofficial007](https://www.tiktok.com/@kadirofficial007)
