@echo off
setlocal

set ROOT=%~dp0\..\..
cd /d %ROOT%

set GRADLE_USER_HOME=%ROOT%\.gradle

call gradlew.bat buildPlugin
if errorlevel 1 exit /b 1

for %%f in (build\distributions\*.zip) do set ZIP_PATH=%%f
if "%ZIP_PATH%"=="" (
  echo No plugin ZIP found in build\distributions 1>&2
  exit /b 1
)

if not exist docs mkdir docs
copy /y "%ZIP_PATH%" docs\

for /f "tokens=2 delims==" %%v in ('findstr /r /c:"^version *= *\"" build.gradle.kts') do set VERSION=%%v
set VERSION=%VERSION:"=%
if "%VERSION%"=="" (
  echo Unable to parse version from build.gradle.kts 1>&2
  exit /b 1
)

for %%f in ("%ZIP_PATH%") do set ZIP_NAME=%%~nxf
set REPO_URL_BASE=https://raw.githubusercontent.com/Dmytro332116/Rewiew-Code/main/docs
set ZIP_URL=%REPO_URL_BASE%/%ZIP_NAME%

(
  echo ^<plugins^>
  echo   ^<plugin id="com.rewiew.autofmt" version="%VERSION%" url="%ZIP_URL%"^>
  echo     ^<name^>Rewiew Code Formatter^</name^>
  echo     ^<description^>Autoformat and inspect CSS/JS/Twig on save and pre-commit without Node.js^</description^>
  echo     ^<vendor^>Rewiew^</vendor^>
  echo     ^<idea-version since-build="243" /^>
  echo   ^</plugin^>
  echo ^</plugins^>
) > docs\updatePlugins.xml

echo Built: %ZIP_NAME%
echo Repository: docs\updatePlugins.xml
endlocal
