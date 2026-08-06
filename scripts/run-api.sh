#!/usr/bin/env bash
# Load repo-root .env and run the skyWash API jar.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ -f "$ROOT/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT/.env"
  set +a
else
  echo "Missing .env — copy .env.example to .env and fill in secrets." >&2
  exit 1
fi

export PATH="/opt/homebrew/opt/openjdk@21/bin:${PATH:-}"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21}"

JAR="$ROOT/backend/target/skywash-api-0.1.0.jar"
if [[ ! -f "$JAR" ]]; then
  (cd "$ROOT/backend" && mvn -q -DskipTests package)
fi

exec java -jar "$JAR"
