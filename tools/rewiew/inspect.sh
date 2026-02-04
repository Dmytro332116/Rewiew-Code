#!/usr/bin/env bash
set -euo pipefail

BIN="${PHPSTORM_BIN:-}"
if [[ -z "$BIN" && -n "${PHPSTORM_HOME:-}" ]]; then
  BIN="$PHPSTORM_HOME/bin"
fi

if [[ -z "$BIN" ]]; then
  echo "Rewiew inspect: set PHPSTORM_BIN to PhpStorm bin directory." >&2
  echo "Example (macOS): /Applications/PhpStorm.app/Contents/bin" >&2
  exit 1
fi

if [[ ! -x "$BIN/inspect.sh" ]]; then
  echo "Rewiew inspect: $BIN/inspect.sh not found or not executable." >&2
  exit 1
fi

exec "$BIN/inspect.sh" "$@"
