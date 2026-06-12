@echo off
setlocal enabledelayedexpansion

set "GRADLE_VERSION=8.10.2"
set "PROJECT_DIR=%~dp0"
set "GRADLE_USER_HOME=%PROJECT_DIR%.gradle"
set "GRADLE_DIST_DIR=%GRADLE_USER_HOME%\wrapper\dists\gradle-%GRADLE_VERSION%-bin"
set "GRADLE_HOME=%GRADLE_DIST_DIR%\gradle-%GRADLE_VERSION%"
set "GRADLE_ZIP=%GRADLE_DIST_DIR%\gradle-%GRADLE_VERSION%-bin.zip"

if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  if not exist "%GRADLE_DIST_DIR%" mkdir "%GRADLE_DIST_DIR%"
  if not exist "%GRADLE_ZIP%" (
    powershell -NoProfile -ExecutionPolicy Bypass -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%GRADLE_ZIP%'"
    if errorlevel 1 exit /b 1
  )
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '%GRADLE_ZIP%' -DestinationPath '%GRADLE_DIST_DIR%' -Force"
  if errorlevel 1 exit /b 1
)

call "%GRADLE_HOME%\bin\gradle.bat" %*
