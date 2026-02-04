# Rewiew Code Formatter (PhpStorm Plugin)

A PhpStorm plugin to autoformat and inspect **CSS**, **JavaScript**, and **Twig** **without Node.js**.

## What It Does

- On save: reformats `*.css`, `*.js/*.mjs/*.cjs/*.jsx`, `*.twig` using **PhpStorm Code Style**.
- Before commit (inside IDE):
  - optional reformat of staged/changed CSS/JS/Twig
- Pre-commit hook (Git): installs/updates a `pre-commit` hook that runs **PhpStorm headless formatter + inspections** (no npm).

## Installation (Dev)

This repository is a Gradle IntelliJ Platform plugin project.

- Open the folder in IntelliJ IDEA (or PhpStorm) and import as Gradle project.
- Run the `runIde` Gradle task.

## Build and Plugin Repository

To build and generate a custom plugin repository (for **Manage Plugin Repositories**):

1. Ensure `PHPSTORM_BIN` is set (for pre-commit wrappers).
2. Run:
   - macOS/Linux: `tools/rewiew/build-plugin.sh`
   - Windows: `tools\\rewiew\\build-plugin.bat`
3. This generates:
   - `docs/<plugin-zip>.zip`
   - `docs/updatePlugins.xml`

Use this URL in PhpStorm (GitHub Pages):
`https://dmytro332116.github.io/Rewiew-Code/updatePlugins.xml`

If Pages are not enabled yet, use the raw GitHub URL:
`https://raw.githubusercontent.com/Dmytro332116/Rewiew-Code/main/docs/updatePlugins.xml`

## Configuration

Settings: `Settings/Preferences -> Rewiew Code Formatter`

- `Format on save`
- `Format before commit (IDE)`
- File type toggles: CSS / JavaScript / Twig

## Pre-Commit Hook (No Node.js)

The Git hook runs formatter/inspections via **PhpStorm scripts** shipped with the IDE.

1. Find PhpStorm `bin` directory (examples):
   - macOS Toolbox: `~/Library/Application Support/JetBrains/Toolbox/apps/PhpStorm/ch-*/<version>/PhpStorm.app/Contents/bin`
   - macOS App: `/Applications/PhpStorm.app/Contents/bin`
   - Linux: `<phpstorm>/bin`
   - Windows: `<phpstorm>\\bin`

2. In plugin settings set:
   - Recommended (portable): use repo-local wrappers:
     - `Formatter command`: `tools/rewiew/format.sh` (or `.bat` on Windows)
     - `Inspector command`: `tools/rewiew/inspect.sh` (or `.bat` on Windows)
   - Alternative (direct): `/path/to/PhpStorm/bin/format.sh` and `/path/to/PhpStorm/bin/inspect.sh`
   - `Inspection profile` (optional): `.idea/inspectionProfiles/Project_Default.xml`

3. In PhpStorm run: `Tools -> Install/Update Pre-Commit Hook`

Notes:
- The hook formats only staged files: `*.css`, `*.js/*.mjs/*.cjs/*.jsx`, `*.twig`.
- Headless inspection requires a valid inspection profile XML.
- Repo-local wrappers read `PHPSTORM_BIN` or `PHPSTORM_HOME` env vars.

## Run Manually (Tools Menu)

You can manually trigger formatting from the IDE:

`Tools -> Format CSS/JS/Twig`

This formats the currently selected files in the Project view using IDE code style.

`Tools -> Auto-Fix CSS/JS/Twig`  
Runs IDE cleanup + formatter + safe Twig fixes, and writes a report to `.idea/rewiew-autofix.txt`.

`Tools -> Run Rewiew Checks (Report)`  
Runs inspections and writes a readable report to `.idea/rewiew-report.txt`.

`Tools -> Rewiew Formatter Settings`  
Opens plugin settings.

## Twig Support

This plugin relies on PhpStorm/Twig language support:
- Formatting uses the IDE formatter for Twig.
- Inspection is enforced in pre-commit via PhpStorm headless inspections (not HTMLHint).
- Additional inspection: **Twig print tag spacing** (group: `Rewiew`). Can be disabled in Inspections settings.

## Roadmap

- Better pre-commit experience (portable repo-local wrappers).
- Optional auto-fix for a small set of Twig style rules.
- CI build and plugin ZIP artifacts.
