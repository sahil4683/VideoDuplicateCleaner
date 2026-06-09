# 🎬 Video Duplicate Cleaner

A production-ready Android application for detecting and safely removing duplicate and similar videos from device storage.

[![Android CI](https://github.com/your-org/video-duplicate-cleaner/actions/workflows/android-ci.yml/badge.svg)](https://github.com/your-org/video-duplicate-cleaner/actions/workflows/android-ci.yml)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://android-arsenal.com/api?level=26)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

---

## 📱 Screenshots

| Dashboard | Exact Duplicates | Similar Videos | Scan Progress |
|-----------|-----------------|----------------|---------------|
| Overview of storage stats | Groups of identical files | Visually similar videos | Real-time scan progress |

---

## ✨ Features

### Core Detection
- **Exact Duplicate Detection** — Multi-stage approach: file size grouping → partial hash (first/middle/last 1 MB) → full SHA-256 confirmation
- **Similar Video Detection** — Perceptual hashing (pHash) on frames extracted at 0%, 25%, 50%, 75%, 100% of duration
- **Configurable Similarity Threshold** — Default 90%, adjustable from 70%–99%

### Smart Selection
- Keep largest file (highest quality original)
- Keep newest file (most recent recording)
- Keep highest resolution file (best resolution)
- Manual selection per video

### Safety First
- ⚠️ **Never auto-deletes anything**
- Confirmation dialog before every deletion
- Shows exact storage to be recovered
- Uses Android MediaStore APIs for safe deletion

### Background Processing
- WorkManager periodic scans (Daily / Weekly / Monthly)
- Foreground service for active scan sessions
- Handles 10,000+ videos without memory issues
- Paginated lazy lists throughout

### UI/UX
- Material 3 with dynamic color (Android 12+)
- Full dark/light theme support
- Animated splash screen (Android 12 SplashScreen API)
- Smooth onboarding flow

---

## 🏗️ Architecture

```
com.videocleaner/
├── data/
│   ├── local/
│   │   ├── dao/           # Room DAOs (VideoDao, DuplicateGroupDao)
│   │   ├── database/      # AppDatabase (Room)
│   │   └── entity/        # VideoEntity, DuplicateGroupEntity, GroupVideoEntity
│   └── repository/        # VideoRepository, SettingsRepository
├── di/                    # Hilt modules (DatabaseModule, AppModule)
├── domain/
│   ├── model/             # VideoFile, DuplicateGroup, ScanProgress, ScanSettings
│   └── usecase/           # (future use cases)
├── presentation/
│   ├── about/             # About screen
│   ├── components/        # VideoCard, ConfirmDeleteDialog, StorageBadge
│   ├── dashboard/         # Dashboard screen + ViewModel
│   ├── duplicates/        # Exact Duplicates screen + ViewModel
│   ├── onboarding/        # Onboarding flow + ViewModel
│   ├── player/            # Video player (ExoPlayer/Media3)
│   ├── scan/              # Scan progress screen + ViewModel
│   ├── settings/          # Settings screen + ViewModel
│   ├── similar/           # Similar Videos screen + ViewModel
│   ├── splash/            # Splash (handled via SplashScreen API)
│   ├── theme/             # Material 3 theme + typography
│   ├── AppNavigation.kt   # NavHost with all routes
│   └── MainActivity.kt    # Single activity entry point
├── service/               # VideoScanService (foreground service)
├── util/                  # HashUtils, PerceptualHash, VideoFrameExtractor, MediaStoreUtils, Mappers
└── worker/                # VideoScanWorker (WorkManager)
```

### Architecture Diagram

```
┌─────────────────────────────────────────────┐
│               Presentation Layer             │
│   Compose UI ←→ ViewModel ←→ StateFlow      │
└────────────────────┬────────────────────────┘
                     │
┌────────────────────▼────────────────────────┐
│               Domain Layer                   │
│     Models (VideoFile, DuplicateGroup)       │
│     Pure Kotlin — no Android dependencies   │
└────────────────────┬────────────────────────┘
                     │
┌────────────────────▼────────────────────────┐
│               Data Layer                     │
│  VideoRepository ←→ Room DB                  │
│  SettingsRepository ←→ DataStore             │
│  MediaStoreUtils ←→ ContentResolver          │
└─────────────────────────────────────────────┘
```

---

## 🛠️ Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt |
| Database | Room (SQLite) |
| Async | Kotlin Coroutines + Flow |
| Video Player | Media3 / ExoPlayer |
| Image Loading | Coil |
| Background Jobs | WorkManager |
| Preferences | DataStore |
| Serialization | kotlinx.serialization |
| Testing | JUnit4 + MockK + Turbine + Truth |
| Static Analysis | detekt |
| Build | Gradle Kotlin DSL + Version Catalogs |

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (2024.2.x) or later
- JDK 21
- Android SDK 35
- Physical device or emulator with API 26+

### Build & Run

```bash
# Clone the repository
git clone https://github.com/your-org/video-duplicate-cleaner.git
cd video-duplicate-cleaner

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run unit tests
./gradlew testDebugUnitTest

# Run lint
./gradlew lint

# Run static analysis
./gradlew detekt
```

### Setting Up Release Signing

1. Generate a keystore:
   ```bash
   keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias app
   ```

2. Add to your `local.properties` (never commit this):
   ```properties
   KEYSTORE_PATH=../release.jks
   KEYSTORE_PASSWORD=your_password
   KEY_ALIAS=app
   KEY_PASSWORD=your_key_password
   ```

3. For GitHub Actions, add these repository secrets:
   - `KEYSTORE_BASE64` — base64-encoded keystore file
   - `KEYSTORE_PASSWORD`
   - `KEY_ALIAS`
   - `KEY_PASSWORD`

---

## 🔐 Permissions

| Permission | Purpose | When Required |
|-----------|---------|--------------|
| `READ_MEDIA_VIDEO` | Read video files for scanning | Android 13+ |
| `READ_EXTERNAL_STORAGE` | Read video files (legacy) | Android ≤ 12 |
| `WRITE_EXTERNAL_STORAGE` | Delete files (legacy) | Android ≤ 8 |
| `POST_NOTIFICATIONS` | Scan completion notifications | Android 13+ |
| `FOREGROUND_SERVICE` | Keep scan alive in background | Always |
| `RECEIVE_BOOT_COMPLETED` | Reschedule periodic scans | Always |

---

## 🧪 Testing

```bash
# Unit tests (fast, no device needed)
./gradlew testDebugUnitTest

# Generate coverage report
./gradlew jacocoTestReport

# Instrumented tests (requires device/emulator)
./gradlew connectedDebugAndroidTest
```

### Test Coverage Targets
- **Unit tests**: Hash utilities, domain models, ViewModels, Repository
- **Repository tests**: DAO interactions, data mapping
- **ViewModel tests**: State management, user actions

---

## 📦 CI/CD

### GitHub Actions Workflows

| Workflow | Trigger | Steps |
|---------|---------|-------|
| `android-ci.yml` | Push/PR to main/develop | detekt → lint → unit tests → debug APK |
| `release.yml` | Tag `v*` | Release APK + AAB → sign → GitHub Release |
| `code-quality.yml` | PR to main | detekt checks |

---

## 🔍 Duplicate Detection Algorithm

### Stage 1 — File Size Grouping
Groups all videos by exact file size. Only groups with 2+ videos proceed.
- Time complexity: O(n)
- Purpose: Fast pre-filter eliminating most non-duplicates

### Stage 2 — Partial Hash
For each size group, reads 3 × 1 MB chunks (start, middle, end) and hashes them.
- ~3x faster than full SHA-256 for large files
- Eliminates most false positives from stage 1

### Stage 3 — Full SHA-256
Only for files that match on partial hash. Reads entire file.
- Guarantees exact byte-for-byte duplicate confirmation

### Similar Video Detection
1. Extract frames at 0%, 25%, 50%, 75%, 100% using `MediaMetadataRetriever`
2. Resize each frame to 32×32 grayscale
3. Apply 2D DCT, take top-left 8×8 coefficients (64-bit hash)
4. Compare all video pairs using Hamming distance
5. Flag as similar if average similarity > threshold (default 90%)

---

## 📄 License

```
Copyright 2024 Video Duplicate Cleaner Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```
