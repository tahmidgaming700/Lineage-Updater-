# Lineage Updater & Downloader v2.0.0 Artifacts

This directory contains the official built packages and cryptographic checksums for **Lineage Updater & Downloader v2.0.0**.

---

## 📦 Available Artifacts

### 1. Production Release APK (Signed & R8-Optimized)
- **Filename:** `lineage-updater-downloader-2.0.0-release.apk` (also staged as `app-updater.apk`)
- **Size:** ~16 MB
- **Package ID:** `com.tahmidgaming.lineageupdater`
- **Version:** `2.0.0` (Version Code: `2000`)
- **Min SDK:** Android 7.0 (API 24)
- **Target SDK:** Android 16 (API 36)
- **SHA-256 Checksum:**
  ```
  169aebc0cbff041bd491ba2083caf04b0a823ced785a08ca42286c482d91fc57
  ```

---

### 2. Development / Debug APK
- **Filename:** `lineage-updater-downloader-2.0.0-debug.apk`
- **Size:** ~31 MB
- **Signed with:** `debug.keystore`
- **SHA-256 Checksum:**
  ```
  6fea4839d84d3c46dfa08f1f75db156402f60b5524a589465837d212eb9a2551
  ```

---

## 🛡️ Verification Instructions

To verify the integrity of the downloaded APK before installing on device or emulator, execute:

```bash
# On Linux / macOS:
sha256sum -c lineage-updater-downloader-2.0.0-release.apk.sha256

# On Windows (PowerShell):
Get-FileHash lineage-updater-downloader-2.0.0-release.apk -Algorithm SHA256
```

---

## 🚀 Key Features in Version 2.0.0

- **Official LineageOS & TimSchumi Archive**: Real-time official device and build catalog from Hudson API v2, with historical ROM archive browsing.
- **Safety Simulation Mode (Virtual Phone Root Simulator)**: Unlocks full testing of TWRP and OrangeFox OpenRecoveryScript preparation, recovery reboot simulation, and DD block-level partition writing without hardware risk.
- **Resumable HTTP Downloads**: Background foreground worker via `WorkManager` with HTTP Range requests and SHA-256 integrity validation.
- **Add-on Center**: Curated community GSI releases, official MindTheGapps packages, and topjohnwu/Magisk APK releases.
- **Hardware Architecture & GSI Filters**: Architecture detection (`arm64`, `arm`, `x86_64`) with device compatibility guidance.
- **Harmony Recovery Build Script Planner**: Generates validated host Linux shell recipes for building recovery images from source.
- **M3 Fluid Theming & Edge-to-Edge**: Glassmorphic UI with dynamic system/light/dark themes, adaptive insets, and predictive back navigation.
