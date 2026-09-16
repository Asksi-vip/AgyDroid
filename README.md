# AgyDroid — Antigravity Android Companion

AgyDroid is a native Android AI-powered application builder that transforms Google's **Antigravity CLI (agy)** into a mobile pair-programming experience similar to ChatGPT.

## 🚀 Architecture Overview

```
┌────────────────────────────────────────────────────────┐
│                      Android App                       │
│    (Jetpack Compose + Material 3 + Room + Hilt)        │
└───────────────────────────┬────────────────────────────┘
                            │ HTTP (SSE) via Localhost:7860
┌───────────────────────────▼────────────────────────────┐
│                  Termux Bridge Service                 │
│         (Python aiohttp on port 7860 in Termux)        │
└───────────────────────────┬────────────────────────────┘
                            │ Subprocess
┌───────────────────────────▼────────────────────────────┐
│                  Antigravity CLI (agy)                 │
│    `agy --print --mode accept-edits --add-dir <path>`  │
└───────────────────────────┬────────────────────────────┘
                            │ Git Push
┌───────────────────────────▼────────────────────────────┐
│                  GitHub Actions CI                     │
│       Ubuntu runner -> JDK 17 -> assembleDebug         │
│                 -> APK Download Link                   │
└────────────────────────────────────────────────────────┘
```

## 🛠️ Termux Setup

1. Open Termux.
2. Navigate to the bridge directory:
   ```bash
   cd ~/AgyDroid/termux-bridge
   ```
3. Run the setup script:
   ```bash
   bash setup.sh
   ```
   This installs `aiohttp` and launches the bridge on `127.0.0.1:7860`.

## 📦 Building the APK via GitHub Actions

1. Create a GitHub repository (e.g. `AgyDroid`).
2. Push this directory to your repository:
   ```bash
   cd ~/AgyDroid
   git init
   git add .
   git commit -m "feat: initial AgyDroid architecture"
   git branch -M main
   git remote add origin https://github.com/<your-username>/AgyDroid.git
   git push -u origin main
   ```
3. GitHub Actions will trigger automatically and build `AgyDroid-debug-apk`.
4. Download the `.apk` from the **Actions** tab on GitHub and install it on your device.

## 🔒 Security
- Secrets (like your GitHub Token) are stored encrypted via **Android Keystore (EncryptedSharedPreferences)**.
- Secrets are never logged or hardcoded into the source code or git repository.
