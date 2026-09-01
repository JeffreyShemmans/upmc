@echo off
setlocal EnableExtensions DisableDelayedExpansion

set "UPMC_JAR="
set "JAR_COUNT=0"

for %%F in ("%~dp0upmc-*.jar") do if exist "%%~fF" (
    set /a JAR_COUNT+=1
    set "UPMC_JAR=%%~fF"
)

where java.exe >nul 2>&1
if errorlevel 1 (
    echo Java is not installed or is not available on PATH. Install Java, then try UPMC again. 1>&2
    pause
    exit /b 127
)

if %JAR_COUNT% equ 0 (
    echo No UPMC application file ^(upmc-*.jar^) was found in %~dp0 1>&2
    pause
    exit /b 1
)

if %JAR_COUNT% gtr 1 (
    echo More than one UPMC application file was found in %~dp0. Keep only the version you want to run. 1>&2
    pause
    exit /b 1
)

java.exe -Xmx384m -Duser.country=US -Duser.language=en -jar "%UPMC_JAR%" %*
exit /b %ERRORLEVEL%
