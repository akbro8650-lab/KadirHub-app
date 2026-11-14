# KadirHub

A community-driven Android app and resource hub created by **[@kadirofficial007](https://www.tiktok.com/@kadirofficial007)**, built with **Kotlin**, **Jetpack Compose (Material 3)**, and **Firebase Firestore**.

Signed-in users can browse, search, review, download, and submit APKs or links with custom app icons, while all community submissions and edits are reviewed through an admin approval workflow and secured with **AES-256-GCM** encryption.

---

## Key Features

- **Browse & Direct Download**
  - Explore categorized releases (`App`, `Link`, `Tool`, `Game`, `Resource`) with real-time search, category filters, and custom app icons.
  - Decrypt and save/open uploaded APKs and files or launch curated external links with live progress tracking.
- **Community APK & Link Uploads**
  - Any authenticated user can submit an APK/file or link via the **Upload** tab and track submission status under **My Uploads**.
  - Attach, preview, and update custom app icons using Android's zero-permission **Photo Picker** (compressed 160×160 WebP).
  - New community uploads start in `pending` status and appear in **Explore** only after admin approval.
- **Uploader Edit Controls**
  - Uploaders can edit their own releases (title, description, category, version, file size label, link, icon, or replacement APK) or delete them anytime.
  - Non-admin edits automatically reset the item back to `pending` for admin re-approval.
- **50 MB Encrypted Vault & Integrity Verification**
  - Supports encrypted file uploads up to **50 MB** using streamed chunked **AES-256-GCM** encryption (12-byte IVs per payload/chunk) and streaming **SHA-256** checksum verification.
- **Ratings, Reviews & Reporting**
  - Authenticated users can rate approved items (1–5 stars), write or edit reviews, and flag or report content.
  - Aggregate ratings and verified download counts update dynamically.
- **Admin Dashboard & Content Moderation**
  - Role-verified admin access protected by **Google Sign-In** and a SHA-256 verified admin panel gate.
  - Review and one-tap approve pending community uploads, edit or archive releases, and moderate flagged reviews or reports.
- **Creator Attribution**
  - Integrated creator branding and direct TikTok link for **@kadirofficial007**.

---

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose, Material Design 3 (Edge-to-Edge, Window Size Class Adaptive Layouts, Android Photo Picker, Storage Access Framework)
- **Architecture:** MVVM with `ViewModel`, Kotlin Coroutines & `StateFlow`
- **Authentication:** Firebase Auth + Jetpack Credential Manager (Google Sign-In)
- **Database & Security:** Cloud Firestore with zero-trust `firestore.rules` (owner-scoped pending uploads, chunked encrypted subcollections, and admin-only moderation)
- **Cryptography:** `javax.crypto` (`AES/GCM/NoPadding`, 256-bit key, 128-bit auth tag) & streaming `SHA-256`

---

## Creator

- **TikTok:** [@kadirofficial007](https://www.tiktok.com/@kadirofficial007)
