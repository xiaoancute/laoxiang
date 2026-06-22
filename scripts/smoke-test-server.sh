#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
NEOFORGE_VERSION="$(sed -n 's/^neoforge_version=//p' "$ROOT_DIR/gradle.properties")"
MOD_JAR="${1:-}"

if [[ -z "$NEOFORGE_VERSION" ]]; then
  echo "Unable to read neoforge_version from gradle.properties" >&2
  exit 1
fi

if [[ -z "$MOD_JAR" ]]; then
  MOD_JAR="$(find "$ROOT_DIR/build/libs" -maxdepth 1 -name 'iplocationdisplay-*.jar' ! -name '*-sources.jar' ! -name '*-dev.jar' | sort | tail -n 1)"
fi

if [[ ! -f "$MOD_JAR" ]]; then
  echo "Mod jar not found: $MOD_JAR" >&2
  exit 1
fi

SERVER_DIR="${RUNNER_TEMP:-/tmp}/iplocationdisplay-smoke-server"
INSTALLER_URL="https://maven.neoforged.net/releases/net/neoforged/neoforge/${NEOFORGE_VERSION}/neoforge-${NEOFORGE_VERSION}-installer.jar"
LOG_FILE="$SERVER_DIR/server.log"
CONFIG_FILE="$SERVER_DIR/config/iplocationdisplay.toml"

rm -rf "$SERVER_DIR"
mkdir -p "$SERVER_DIR/mods"
cp "$MOD_JAR" "$SERVER_DIR/mods/"

echo "Downloading NeoForge $NEOFORGE_VERSION"
curl -fsSL "$INSTALLER_URL" -o "$SERVER_DIR/neoforge-installer.jar"

echo "Installing temporary NeoForge server"
(
  cd "$SERVER_DIR"
  java -jar neoforge-installer.jar --installServer
  chmod +x run.sh
)

printf 'eula=true\n' > "$SERVER_DIR/eula.txt"
cat > "$SERVER_DIR/server.properties" <<'EOF'
online-mode=false
server-port=25565
enable-query=false
enable-rcon=false
motd=IP Location Display smoke test
EOF

echo "Starting temporary NeoForge server"
(
  cd "$SERVER_DIR"
  timeout 120s ./run.sh nogui > "$LOG_FILE" 2>&1
) &
SERVER_PID=$!

loaded=false
generated=false
for _ in $(seq 1 120); do
  if [[ -f "$LOG_FILE" ]] && grep -q "IP Location Display mod loaded" "$LOG_FILE"; then
    loaded=true
  fi
  if [[ -f "$CONFIG_FILE" ]] && grep -q 'httpPreset = "ip-sb"' "$CONFIG_FILE"; then
    generated=true
  fi
  if [[ "$loaded" == true && "$generated" == true ]]; then
    break
  fi
  if ! kill -0 "$SERVER_PID" 2>/dev/null; then
    break
  fi
  sleep 1
done

if kill -0 "$SERVER_PID" 2>/dev/null; then
  kill "$SERVER_PID" 2>/dev/null || true
  wait "$SERVER_PID" 2>/dev/null || true
else
  wait "$SERVER_PID" 2>/dev/null || true
fi

if [[ "$loaded" != true ]]; then
  echo "Mod did not log successful loading." >&2
  tail -n 200 "$LOG_FILE" >&2 || true
  exit 1
fi

if [[ "$generated" != true ]]; then
  echo "Config file was not generated with expected defaults: $CONFIG_FILE" >&2
  find "$SERVER_DIR" -maxdepth 3 -type f | sort >&2 || true
  tail -n 200 "$LOG_FILE" >&2 || true
  [[ -f "$CONFIG_FILE" ]] && cat "$CONFIG_FILE" >&2
  exit 1
fi

echo "Smoke test passed: mod loaded and generated $CONFIG_FILE"
