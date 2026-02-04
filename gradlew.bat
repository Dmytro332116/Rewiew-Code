@echo off
setlocal

set APP_HOME=%~dp0
set CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar

if not "%JAVA_HOME%"=="" goto HAVE_JAVA
set JAVA_EXE=java
where java >nul 2>nul
if errorlevel 1 (
  echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH. 1>&2
  exit /b 1
)

goto RUN

:HAVE_JAVA
set JAVA_EXE=%JAVA_HOME%\bin\java
if not exist "%JAVA_EXE%" (
  echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% 1>&2
  exit /b 1
)

:RUN
"%JAVA_EXE%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
endlocal
