@ECHO OFF
rem #
rem # Copyright (c) 2012-2015 Igor Yova
rem #

echo "                                                                         "
echo "  ___  ___                       _   _        ___  ________ _____ _____  "
echo "  |  \/  |                      | | | |       |  \/  |  _  |_   _|_   _| "
echo "  | .  . | ___   __ _ _   _  ___| |_| |_ ___  | .  . | | | | | |   | |   "
echo "  | |\/| |/ _ \ / _\ | | | |/ _ \ __| __/ _ \ | |\/| | | | | | |   | |   "
echo "  | |  | | (_) | (_| | |_| |  __/ |_| ||  __/ | |  | \ \/' / | |   | |   "
echo "  \_|  |_/\___/ \__, |\__,_|\___|\__|\__\___| \_|  |_/\_/\_\ \_/   \_/   "
echo "                   | |                                                   "
echo "                   |_|                                                   "
echo "                                                                         "

set "CURRENT_DIR=%cd%"
if not "%MOQUETTE_HOME%" == "" goto gotHome
set "MOQUETTE_HOME=%CURRENT_DIR%"
if exist "%MOQUETTE_HOME%\bin\moquette.bat" goto okHome
cd ..
set "MOQUETTE_HOME=%cd%"
cd "%CURRENT_DIR%"
:gotHome
if exist "%MOQUETTE_HOME%\bin\moquette.bat" goto okHome
    echo The MOQUETTE_HOME environment variable is not defined correctly
    echo This environment variable is needed to run this program
goto end
:okHome

rem Set JavaHome if it exists
if exist { "%JAVA_HOME%\bin\java" } (
    set "JAVA="%JAVA_HOME%\bin\java""
)

echo Using JAVA_HOME:       "%JAVA_HOME%"
echo Using MOQUETTE_HOME:   "%MOQUETTE_HOME%"

rem  set LOG_CONSOLE_LEVEL=info
rem  set LOG_FILE_LEVEL=fine
set JAVA_OPTS=
set JAVA_OPTS_SCRIPT=-XX:+HeapDumpOnOutOfMemoryError -Djava.awt.headless=true
set MOQUETTE_PATH=%MOQUETTE_HOME%
set LOG_FILE=%MOQUETTE_HOME%\config\moquette-log.properties
%JAVA% -server %JAVA_OPTS% %JAVA_OPTS_SCRIPT% -Dlog4j.configuration=file:%LOG_FILE% -Dmoquette.path=%MOQUETTE_PATH% -cp %MOQUETTE_HOME%\lib\* org.eclipse.moquette.server.Server
