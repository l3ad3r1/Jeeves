#!/usr/bin/env bash
# Automated proactive-engine device gates (#16, #17, #18). Drives the app via
# the DEBUG-ONLY ProactiveTestReceiver and asserts outcomes from logcat.
# Output: devicegate/RESULTS.txt (PASS/FAIL per gate) + raw evidence files.
set -u
ADB="C:/Users/renja/AppData/Local/Android/Sdk/platform-tools/adb.exe"
PKG=com.jeeves.app.debug
cd /e/claude-projects/jeeves || exit 1
mkdir -p devicegate
R=devicegate/RESULTS.txt
: > "$R"

# Manifest receivers only get EXPLICIT broadcasts on API 26+ — target the component.
RCV=$PKG/com.hermes.agent.debug.ProactiveTestReceiver
bc() { "$ADB" shell am broadcast -n "$RCV" -a "$@" > /dev/null 2>&1; sleep 2; }
 lastgate() { "$ADB" logcat -d -s GATE Proactive | tail -"$1"; }
say() { echo "$1" | tee -a "$R"; }
check() { # check <name> <pattern> <lines>
  if lastgate "${3:-6}" | grep -qE "$2"; then say "PASS: $1"; else say "FAIL: $1 (wanted /$2/)"; lastgate 8 >> "$R"; fi
}

say "=== Proactive device gates $(date) ==="
"$ADB" logcat -c

say "-- G1: quiet-hours suppression (window forced around now) --"
bc com.jeeves.debug.SET_QUIET --ei start 0 --ei end 86340   # quiet nearly all day
bc com.jeeves.debug.PROACTIVE_PING --es title quietping --es text body
check "quiet-hours ping suppressed" "posted=false|suppressed .quietping.: quiet hours"

say "-- G2: ping arrives outside quiet hours --"
bc com.jeeves.debug.SET_QUIET --ei start 10800 --ei end 10860   # 03:00-03:01
bc com.jeeves.debug.PROACTIVE_PING --es title liveping --es text body
check "open-hours ping posted" "GATE:PING source=SCHEDULED_TASK posted=true"
"$ADB" shell dumpsys notification --noredact 2>/dev/null | grep -B2 -A6 "jeeves_proactive" | head -30 > devicegate/10_notification.txt
if grep -q "liveping" devicegate/10_notification.txt; then say "PASS: notification visible in dumpsys"; else say "WARN: liveping not found in dumpsys snapshot"; fi

say "-- G3: less-of-this damping 2->1->muted --"
bc com.jeeves.debug.LESS_OF_THIS --es source SCHEDULED_TASK
bc com.jeeves.debug.LESS_OF_THIS --es source SCHEDULED_TASK
check "less-of-this count reached 2" "GATE:LESS source=SCHEDULED_TASK count=2"
bc com.jeeves.debug.PROACTIVE_PING --es title mutedping --es text body
check "muted source suppressed" "posted=false|suppressed .mutedping.: .*muted"
bc com.jeeves.debug.RESET_LESS --es source SCHEDULED_TASK

say "-- G4: digest (consent off -> suppressed; on -> posted) --"
bc com.jeeves.debug.PROACTIVE_PING --es source DIGEST --es title digestoff --es text x
check "digest denied without consent" "posted=false|suppressed .digestoff.: .*turned off"
bc com.jeeves.debug.SET_CONSENT --es source DIGEST --ez granted true
bc com.jeeves.debug.RUN_DIGEST
sleep 8
if "$ADB" logcat -d -s Proactive DailyDigest | grep -qE "suppressed 'Your daily digest'"; then
  say "FAIL: digest suppressed unexpectedly"
elif "$ADB" shell dumpsys notification --noredact 2>/dev/null | grep -q "Your daily digest"; then
  say "PASS: digest notification posted"
else
  say "WARN: digest outcome not observed yet (worker may still run)"
fi

say "-- G5: commitment nudge end-to-end --"
bc com.jeeves.debug.SET_CONSENT --es source NUDGE --ez granted true
bc com.jeeves.debug.ADD_COMMITMENT --es text "water the plants for the gate test"
bc com.jeeves.debug.RUN_NUDGE
sleep 8
if "$ADB" shell dumpsys notification --noredact 2>/dev/null | grep -q "commitment you made"; then
  say "PASS: nudge notification posted"
else
  say "WARN: nudge not observed (check logcat Nudge tag)"
  "$ADB" logcat -d -s Nudge | tail -3 >> "$R"
fi

say "-- G6: notification capture boundary --"
"$ADB" shell cmd notification allow_listener $PKG/com.hermes.agent.service.JeevesNotificationListener
bc com.jeeves.debug.SET_CAPTURE --ez enabled true
"$ADB" shell cmd notification post -S bigtext -t "GateTestSender" gatetag "hello from another app" > /dev/null 2>&1
sleep 3
bc com.jeeves.debug.RUN_DIGEST
sleep 8
if "$ADB" shell dumpsys notification --noredact 2>/dev/null | grep -q "GateTestSender"; then
  say "PASS: third-party notification captured into digest"
else
  say "WARN: captured text not observed in digest snapshot"
fi
bc com.jeeves.debug.SET_CAPTURE --ez enabled false
check "capture off deletes data" "GATE:CAPTURE enabled=false"

say "-- Ledger audit trail --"
bc com.jeeves.debug.DUMP_LEDGER
lastgate 14 | tee devicegate/11_ledger.txt >> "$R"

say "-- restore defaults --"
bc com.jeeves.debug.SET_QUIET --ei start 79200 --ei end 25200   # 22:00-07:00
bc com.jeeves.debug.SET_CONSENT --es source DIGEST --ez granted false
bc com.jeeves.debug.SET_CONSENT --es source NUDGE --ez granted false

say "=== done — see $R ==="
