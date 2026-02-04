package com.rewiew.autofmt.vcs

import com.rewiew.autofmt.settings.AutoFormatSettings

object PreCommitHookTemplate {
    private const val MARKER = "REWIEW_AUTOFMT"

    fun marker(): String = MARKER

    fun render(settings: AutoFormatSettings): String {
        val formatter = settings.formatterCommand.trim()
        val inspector = settings.inspectorCommand.trim()
        val profile = settings.inspectionProfilePath.trim().ifEmpty { ".idea/inspectionProfiles/Project_Default.xml" }

        return """
#!/usr/bin/env bash
# $MARKER
set -euo pipefail

FORMAT_CMD="$formatter"
INSPECT_CMD="$inspector"
PROFILE_PATH="$profile"

ROOT="${'$'}(git rev-parse --show-toplevel)"
OUT_DIR="${'$'}ROOT/.git/rewiew-inspection-results"

if [[ -z "${'$'}FORMAT_CMD" || -z "${'$'}INSPECT_CMD" ]]; then
  echo "Rewiew Autoformat: formatter or inspector command is not configured." >&2
  exit 1
fi

# Resolve profile path relative to repo root if needed
if [[ "${'$'}PROFILE_PATH" != /* && ! "${'$'}PROFILE_PATH" =~ ^[A-Za-z]: ]]; then
  PROFILE_PATH="${'$'}ROOT/${'$'}PROFILE_PATH"
fi

files=()
while IFS= read -r -d '' file; do
  case "${'$'}file" in
    *.css|*.js|*.mjs|*.cjs|*.jsx|*.twig)
      files+=("${'$'}file")
      ;;
  esac
done < <(git diff --cached --name-only -z --diff-filter=ACM)

if [[ ${'$'}{#files[@]} -eq 0 ]]; then
  exit 0
fi

format_sub="format"
if [[ "${'$'}FORMAT_CMD" == *format.sh || "${'$'}FORMAT_CMD" == *format.bat ]]; then
  format_sub=""
fi

inspect_sub="inspect"
if [[ "${'$'}INSPECT_CMD" == *inspect.sh || "${'$'}INSPECT_CMD" == *inspect.bat ]]; then
  inspect_sub=""
fi

if [[ -n "${'$'}format_sub" ]]; then
  "${'$'}FORMAT_CMD" "${'$'}format_sub" -allowDefaults "${'$'}{files[@]}"
else
  "${'$'}FORMAT_CMD" -allowDefaults "${'$'}{files[@]}"
fi

git add -- "${'$'}{files[@]}"

mkdir -p "${'$'}OUT_DIR"

if [[ -n "${'$'}inspect_sub" ]]; then
  "${'$'}INSPECT_CMD" "${'$'}inspect_sub" "${'$'}ROOT" "${'$'}PROFILE_PATH" "${'$'}OUT_DIR" -d "${'$'}ROOT" -format xml -v0
else
  "${'$'}INSPECT_CMD" "${'$'}ROOT" "${'$'}PROFILE_PATH" "${'$'}OUT_DIR" -d "${'$'}ROOT" -format xml -v0
fi
""".trimIndent() + "\n"
    }
}
