#!/usr/bin/env bash
# Apply Jeeves-local patches to the llama.cpp submodule. Idempotent: safe to
# re-run; detects an already-patched tree and exits 0.
#
# WHY THIS EXISTS (L-020): llama.cpp is a pinned upstream submodule, but the
# Android/AGP cross-build needs local changes that upstream doesn't carry
# (see tools/patches/*.patch). We deliberately do NOT fork upstream; instead
# the patches live in this repo and are applied on top of the pinned commit.
# CI runs this right after checkout; developers run it once after
# `git submodule update --init`. Bumping the submodule = re-validate patches.
set -euo pipefail
cd "$(dirname "$0")/.."

SUB=app/src/main/cpp/llama.cpp

if [ ! -f "$SUB/CMakeLists.txt" ]; then
  echo "ERROR: $SUB is empty. Run: git submodule update --init" >&2
  exit 1
fi

for patch in tools/patches/*.patch; do
  abs_patch="$(pwd)/$patch"
  if git -C "$SUB" apply --reverse --check "$abs_patch" 2>/dev/null; then
    echo "OK (already applied): $patch"
  elif git -C "$SUB" apply --check "$abs_patch" 2>/dev/null; then
    git -C "$SUB" apply "$abs_patch"
    echo "Applied: $patch"
  else
    echo "ERROR: $patch neither applies nor is already applied." >&2
    echo "The submodule pin and the patch have drifted — rebase the patch." >&2
    exit 1
  fi
done
