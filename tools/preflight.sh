#!/usr/bin/env bash
# Preflight — MANDATORY before any agent commit (see AGENTS.md step 3).
# Compiles every module, runs the unit suite, and greps the diff surface for
# anti-patterns that have already shipped as bugs (docs/agents/LESSONS.md).
# Exit 0 = you may commit. Anything else = fix first.
set -uo pipefail
cd "$(dirname "$0")/.."

fail=0
note() { printf '%s\n' "$*"; }

note "== Ledger greps (docs/agents/LESSONS.md) =="

# L-002: coroutines launched in onCleared() die before running.
# (comment lines excluded — the fix's own explanation mentions the pattern)
hits=$(grep -rn --include='*.kt' -A 12 "fun onCleared" app feature core 2>/dev/null \
        | grep "viewModelScope.launch" | grep -vE '^\S+[-:][0-9]+[-:]\s*(//|\*)' || true)
if [ -n "$hits" ]; then
  note "FAIL L-002: viewModelScope.launch inside onCleared() — dead code:"
  note "$hits"
  fail=1
fi

# L-004: personal identifiers in product code (docs/tests excluded).
hits=$(grep -rnEi --include='*.kt' --include='*.xml' "l3ad3r1|reachjacobs|renja" \
        app/src/main core/*/src/main 2>/dev/null \
        | grep -v "octojotter" || true)   # com.l3ad3r1.octojotter is a frozen package name
if [ -n "$hits" ]; then
  note "FAIL L-004: personal identifier in product code:"
  note "$hits"
  fail=1
fi

# L-009 (warning): new prompt/index flows should show a locked/encrypted filter.
if git diff --cached --name-only 2>/dev/null | grep -qE "rag/|Indexer|Orchestrator"; then
  if ! git diff --cached 2>/dev/null | grep -qE "locked|encrypted"; then
    note "WARN L-009: RAG/indexing/orchestrator change with no visible locked/encrypted handling — justify in the commit message."
  fi
fi

# L-021: native GGUF/Jinja owns prompt formatting; Kotlin must pass raw roles.
hits=$(grep -rnE --include='*.kt' "<\|begin_of_text\|>|\[INST\]" \
        app/src/main feature core 2>/dev/null || true)
if [ -n "$hits" ]; then
  note "FAIL L-021: manual model prompt tags found in Kotlin â€” native Jinja must format prompts:"
  note "$hits"
  fail=1
fi

# L-022: screen teardown must not destroy the application-scoped LLM engine.
hits=$(grep -rnE --include='*ViewModel.kt' \
        "localLlmManager\.(close|cleanUp)|inferenceEngine\.(close|cleanUp)|engine\.cleanUp" \
        app/src/main feature core 2>/dev/null || true)
if [ -n "$hits" ]; then
  note "FAIL L-022: a ViewModel owns shared inference-engine cleanup:"
  note "$hits"
  fail=1
fi

# L-023: current Android releases reject data-sync FGS starts from boot.
hits=$(
  grep -nE "AgentServiceController\.start|startForeground(Service)?" \
       app/src/main/kotlin/com/hermes/agent/service/BootReceiver.kt 2>/dev/null
  grep -n "android.intent.action.LOCKED_BOOT_COMPLETED" \
       app/src/main/AndroidManifest.xml 2>/dev/null
  true
)
if [ -n "$hits" ]; then
  note "FAIL L-023: boot path starts a restricted service or enters locked storage:"
  note "$hits"
  fail=1
fi

if [ "$fail" -ne 0 ]; then
  note ""
  note "Preflight FAILED on ledger greps. Fix before compiling."
  exit 1
fi

note "== Compile all modules =="
./gradlew :app:compileDebugKotlin :feature:jotter:compileDebugKotlin \
          :feature:butler:compileDebugKotlin --console=plain || exit 1

note "== Build debug APK (native/CMake/resource/manifest gate) =="
./gradlew :app:assembleDebug --console=plain || exit 1

note "== Unit tests =="
./gradlew :app:testDebugUnitTest --console=plain || exit 1

note ""
note "Preflight PASSED. Reminders before you commit:"
note " - Self-review the diff against docs/agents/LESSONS.md; cite lesson IDs in the commit message."
note " - Mark anything you did not actually run as UNVERIFIED (commit message + PROGRESS.md)."
note " - Update PROGRESS.md (newest-first entry)."
exit 0
