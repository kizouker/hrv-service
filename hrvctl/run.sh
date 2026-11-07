#!/bin/bash
# Builds (mvn verify: compile + 12 tests + shaded jar) and starts hrvctl.
# Runs on: Mac (M4). Modes: run (default) | check (verify only, changes nothing).
# Usage: ./run.sh [check] [args passed to hrvctl, e.g. watchdog --dry-run]
set -u
HERE="$(cd "$(dirname "$0")" && pwd)"
. "$HERE/scripts/ui.sh"
MODE="${1:-run}"
if [ "$MODE" = "check" ]; then shift || true; fi

JAVA_HOME="${JAVA_HOME:-/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home}"
JAR="$HERE/target/hrvctl.jar"

title "hrvctl build + start ($MODE)"

step "Preflight"
what "Check Java 21, Maven, project file and target dir before changing anything."
[ -x "$JAVA_HOME/bin/java" ] || fail "no java at $JAVA_HOME — install Temurin 21"
ok "java 21 found"
command -v mvn >/dev/null 2>&1 || fail "mvn not on PATH — install Maven (brew install maven)"
ok "maven found"
[ -f "$HERE/pom.xml" ] || fail "pom.xml missing in $HERE"
ok "pom.xml found"
if [ "$MODE" = "check" ]; then
  if [ -f "$JAR" ]; then ok "jar already built: $JAR"; else warn "no jar yet — run mode would build it"; fi
  title "Check only: nothing changed"
  exit 0
fi
[ -d "$HERE/target" ] || mkdir -p "$HERE/target"
[ -w "$HERE/target" ] || fail "target dir not writable: $HERE/target"
ok "target dir writable"

step "Build + test"
what "mvn verify compiles Kotlin, runs the 12 unit tests and builds the shaded jar."
run "mvn -B -f $HERE/pom.xml verify"
mvn -B -f "$HERE/pom.xml" verify || fail "mvn verify failed — fix the errors above and retry"
[ -f "$JAR" ] || fail "build finished but $JAR is missing"
ok "jar built: $JAR"

title "Starting hrvctl (args: ${*:-<menu>}) — summary: built, tested, launching now"
exec "$JAVA_HOME/bin/java" -jar "$JAR" "$@"
