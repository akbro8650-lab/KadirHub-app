# KadirHub

A curated Android app and resource hub created by **[@kadirofficial007](https://www.tiktok.com/@kadirofficial007)**, built with **Kotlin**, **Jetpack Compose (Material 3)**, and **Firebase Firestore**.

Users can browse, search, review, and download apps or open curated links directly, while uploads and content moderation are secured through an admin-only dashboard and **AES-256-GCM** encrypted storage.

---

## Key Features

- **Browse & Direct Download**
  - Explore categorized releases (`App`, `Link`, `Tool`, `Game`, `Resource`) with real-time search and filtering.
  - Open encrypted external links or decrypt and save uploaded files directly via Android's Storage Access Framework.
- **Encrypted Vault & Integrity Verification**
  - Uploaded files and target URLs are encrypted using **AES-256-GCM** with unique 12-byte initialization vectors.
  - Every payload includes a **SHA-256** checksum verified prior to download/delivery.
- **Ratings, Reviews & Reporting**
  - Authenticated users can rate items (1–5 stars), write/edit reviews, and flag or report inappropriate content.
  - Aggregate ratings and download counts update dynamically.
- **Admin Dashboard & Content Moderation**
  - Role-verified admin access protected by Firebase Authentication (`Google Sign-In`) and a SHA-256 verified admin gate.
  - Publish new apps/links, upload encrypted files, and review, approve, hide, or dismiss flagged reviews and moderation reports.
- **Creator Attribution**
  - Integrated creator branding and direct TikTok link for **@kadirofficial007**.

---

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose, Material Design 3 (Edge-to-Edge & Adaptive Layouts)
- **Architecture:** MVVM with `ViewModel`, Kotlin Coroutines & `StateFlow`
- **Authentication:** Firebase Auth + Jetpack Credential Manager (Google Sign-In)
- **Database & Security:** Cloud Firestore with zero-trust `firestore.rules`
- **Cryptography:** `javax.crypto` (`AES/GCM/NoPadding`, 256-bit key, 128-bit tag) & `SHA-256`

---

## Creator

- **TikTok:** [@kadirofficial007](https://www.tiktok.com/@kadirofficial007)
