#!/usr/bin/env bash

set -euo pipefail

if [[ ! -d ".git/hooks" ]]; then
  echo "[setup] .git/hooks not found. Run this script from the repository root." >&2
  exit 1
fi

SOURCE_DIR="ops-engine-room/git-hooks"
TARGET_DIR=".git/hooks"

if [[ ! -f "${SOURCE_DIR}/pre-commit.py" ]]; then
  echo "[setup] Missing source hook: ${SOURCE_DIR}/pre-commit.py" >&2
  exit 1
fi

if [[ ! -f "${SOURCE_DIR}/commit-msg.py" ]]; then
  echo "[setup] Missing source hook: ${SOURCE_DIR}/commit-msg.py" >&2
  exit 1
fi

cp "${SOURCE_DIR}/pre-commit.py" "${TARGET_DIR}/pre-commit"
chmod +x "${TARGET_DIR}/pre-commit"
cp "${SOURCE_DIR}/commit-msg.py" "${TARGET_DIR}/commit-msg"
chmod +x "${TARGET_DIR}/commit-msg"

echo "[setup] Installed ${TARGET_DIR}/pre-commit from ${SOURCE_DIR}/pre-commit.py"
echo "[setup] Installed ${TARGET_DIR}/commit-msg from ${SOURCE_DIR}/commit-msg.py"