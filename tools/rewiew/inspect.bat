@echo off
setlocal

if not "%PHPSTORM_BIN%"=="" goto HAVE_BIN
if not "%PHPSTORM_HOME%"=="" set "PHPSTORM_BIN=%PHPSTORM_HOME%\bin"

:HAVE_BIN
if "%PHPSTORM_BIN%"=="" (
  echo Rewiew inspect: set PHPSTORM_BIN to PhpStorm bin directory. 1>&2
  exit /b 1
)

if not exist "%PHPSTORM_BIN%\inspect.bat" (
  echo Rewiew inspect: %PHPSTORM_BIN%\inspect.bat not found. 1>&2
  exit /b 1
)

call "%PHPSTORM_BIN%\inspect.bat" %*
endlocal
