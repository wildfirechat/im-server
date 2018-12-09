@ECHO OFF
rem #
rem # Copyright (c) 2012-2015 Andrea Selva
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
echo "                                               version: 0.12.1-SNAPSHOT  "

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
    set "JAVA="%JAVA_HOME%\bin\java"
)

echo Using JAVA_HOME:       "%JAVA_HOME%"
echo Using MOQUETTE_HOME:   "%MOQUETTE_HOME%"

rem  set LOG_CONSOLE_LEVEL=info
rem  set LOG_FILE_LEVEL=fine
set JAVA_OPTS=
set JAVA_OPTS_SCRIPT=-XX:+HeapDumpOnOutOfMemoryError -Djava.awt.headless=true
set MOQUETTE_PATH=%MOQUETTE_HOME%
set LOG_FILE=%MOQUETTE_HOME%\config\moquette-log.properties

rem # Use the Hotspot garbage-first collector.
set JAVA_OPTS=%JAVA_OPTS%  -XX:+UseG1GC

rem # Have the JVM do less remembered set work during STW, instead
rem # preferring concurrent GC. Reduces p99.9 latency.
set JAVA_OPTS=%JAVA_OPTS%  -XX:G1RSetUpdatingPauseTimePercent=5

rem # Main G1GC tunable: lowering the pause target will lower throughput and vise versa.
rem # 200ms is the JVM default and lowest viable setting
rem # 1000ms increases throughput. Keep it smaller than the timeouts.
set JAVA_OPTS=%JAVA_OPTS%  -XX:MaxGCPauseMillis=500

rem # Optional G1 Settings

rem  Save CPU time on large (>= 16GB) heaps by delaying region scanning
rem  until the heap is 70% full. The default in Hotspot 8u40 is 40%.
rem set JAVA_OPTS=%JAVA_OPTS%  -XX:InitiatingHeapOccupancyPercent=70

rem  For systems with > 8 cores, the default ParallelGCThreads is 5/8 the number of logical cores.
rem  Otherwise equal to the number of cores when 8 or less.
rem  Machines with > 10 cores should try setting these to <= full cores.
rem set JAVA_OPTS=%JAVA_OPTS%  -XX:ParallelGCThreads=16

rem  By default, ConcGCThreads is 1/4 of ParallelGCThreads.
rem  Setting both to the same value can reduce STW durations.
rem set JAVA_OPTS=%JAVA_OPTS%  -XX:ConcGCThreads=16

rem ## GC logging options -- uncomment to enable

set JAVA_OPTS=%JAVA_OPTS% -XX:+PrintGCDetails
set JAVA_OPTS=%JAVA_OPTS% -XX:+PrintGCDateStamps
set JAVA_OPTS=%JAVA_OPTS% -XX:+PrintHeapAtGC
set JAVA_OPTS=%JAVA_OPTS% -XX:+PrintTenuringDistribution
set JAVA_OPTS=%JAVA_OPTS% -XX:+PrintGCApplicationStoppedTime
set JAVA_OPTS=%JAVA_OPTS% -XX:+PrintPromotionFailure
rem set JAVA_OPTS=%JAVA_OPTS% -XX:PrintFLSStatistics=1
rem set JAVA_OPTS=%JAVA_OPTS% -Xloggc:/var/log/moquette/gc.log
set JAVA_OPTS=%JAVA_OPTS% -XX:+UseGCLogFileRotation
set JAVA_OPTS=%JAVA_OPTS% -XX:NumberOfGCLogFiles=10
set JAVA_OPTS=%JAVA_OPTS% -XX:GCLogFileSize=10M"

%JAVA% -server %JAVA_OPTS% %JAVA_OPTS_SCRIPT% -Dlog4j.configuration=file:%LOG_FILE% -Dmoquette.path=%MOQUETTE_PATH% -cp %MOQUETTE_HOME%\lib\* io.moquette.broker.Server
