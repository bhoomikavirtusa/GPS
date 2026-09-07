@echo off

set OLD_DIR=%CD%

cd %CATALINA_HOME%

if "%1" == "start" (
    del "%CATALINA_HOME%\work\*" /s/q
)

bin\catalina.bat %1

cd %OLD_DIR%