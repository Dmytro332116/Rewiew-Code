@echo off
setlocal

if not "%PHPSTORM_BIN%"=="" goto HAVE_BIN
if not "%PHPSTORM_HOME%"=="" set "PHPSTORM_BIN=%PHPSTORM_HOME%\bin"

:HAVE_BIN
if "%PHPSTORM_BIN%"=="" (
  echo Rewiew format: set PHPSTORM_BIN to PhpStorm bin directory. 1>&2
  exit /b 1
)

if not exist "%PHPSTORM_BIN%\format.bat" (
  echo Rewiew format: %PHPSTORM_BIN%\format.bat not found. 1>&2
  exit /b 1
)

call "%PHPSTORM_BIN%\format.bat" %*
endlocal
