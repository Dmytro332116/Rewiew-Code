#!/usr/bin/env bash
set -euo pipefail

BIN="${PHPSTORM_BIN:-}"
if [[ -z "$BIN" && -n "${PHPSTORM_HOME:-}" ]]; then
  BIN="$PHPSTORM_HOME/bin"
fi

if [[ -z "$BIN" ]]; then
  echo "Rewiew format: set PHPSTORM_BIN to PhpStorm bin directory." >&2
  echo "Example (macOS): /Applications/PhpStorm.app/Contents/bin" >&2
  exit 1
fi

if [[ ! -x "$BIN/format.sh" ]]; then
  echo "Rewiew format: $BIN/format.sh not found or not executable." >&2
  exit 1
fi

exec "$BIN/format.sh" "$@"
