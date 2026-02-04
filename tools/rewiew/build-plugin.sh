#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

export GRADLE_USER_HOME="$ROOT/.gradle"

if [[ -z "${JAVA_HOME:-}" ]]; then
  if [[ -d "/Applications/PhpStorm.app/Contents/jbr/Contents/Home" ]]; then
    export JAVA_HOME="/Applications/PhpStorm.app/Contents/jbr/Contents/Home"
  fi
fi

if [[ -n "${JAVA_HOME:-}" ]]; then
  export GRADLE_OPTS="${GRADLE_OPTS:-} -Dorg.gradle.java.home=${JAVA_HOME}"
fi

./gradlew buildPlugin

ZIP_PATH=$(ls -1 build/distributions/*.zip | tail -n 1)
if [[ -z "$ZIP_PATH" ]]; then
  echo "No plugin ZIP found in build/distributions" >&2
  exit 1
fi

mkdir -p docs
cp -f "$ZIP_PATH" docs/

VERSION=$(grep -E '^version\s*=\s*"' build.gradle.kts | head -n 1 | sed -E 's/.*"([^"]+)".*/\1/')
if [[ -z "$VERSION" ]]; then
  echo "Unable to parse version from build.gradle.kts" >&2
  exit 1
fi

ZIP_NAME=$(basename "$ZIP_PATH")
REPO_URL_BASE=${REPO_URL_BASE:-"https://dmytro332116.github.io/Plugin-PhpStorm-2-"}
ZIP_URL="$REPO_URL_BASE/$ZIP_NAME"

cat > docs/plugins.xml <<XML
<plugins>
  <plugin id="com.rewiew.autofmt" version="$VERSION" url="$ZIP_URL">
    <name>Rewiew Code Formatter</name>
    <description>Autoformat and inspect CSS/JS/Twig on save and pre-commit without Node.js</description>
    <vendor>Rewiew</vendor>
    <idea-version since-build="243"/>
  </plugin>
</plugins>
XML

echo "Built: $ZIP_NAME"
echo "Repository: docs/plugins.xml"
