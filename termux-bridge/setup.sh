#!/data/data/com.termux/files/usr/bin/bash
# AgyDroid Bridge Setup Script
# Run this inside Termux before launching the Android app

set -e

echo "=== AgyDroid Termux Bridge Setup ==="
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "[1/3] Installing Python dependencies..."
pip install -r "$SCRIPT_DIR/requirements.txt" --quiet

echo "[2/3] Verifying agy is available..."
if ! command -v agy &> /dev/null; then
    echo "ERROR: agy not found in PATH. Please install Antigravity CLI first."
    exit 1
fi
AGY_VERSION=$(agy --version 2>/dev/null || echo "unknown")
echo "      agy version: $AGY_VERSION ✓"

echo "[3/3] Starting bridge server on port 7860..."
echo "      Press Ctrl+C to stop."
echo ""
python3 "$SCRIPT_DIR/server.py"
