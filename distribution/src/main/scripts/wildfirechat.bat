@ECHO OFF

set "CURRENT_DIR=%cd%"
set "WILDFIRECHAT_HOME=%CURRENT_DIR%"
if exist "%WILDFIRECHAT_HOME%\bin\wildfirechat.bat" goto okHome
cd ..
set "WILDFIRECHAT_HOME=%cd%"
set "CURRENT_DIR=%cd%"
:gotHome
if exist "%WILDFIRECHAT_HOME%\bin\wildfirechat.bat" goto okHome
    echo The WILDFIRECHAT_HOME environment variable is not defined correctly
    echo This environment variable is needed to run this program
goto end
:okHome

rem Set JavaHome if it exists
if exist { "%JAVA_HOME%\bin\java" } (
    set "JAVA="%JAVA_HOME%\bin\java""
) else {
    set "JAVA="java""
}

echo Using JAVA_HOME:       "%JAVA_HOME%"
echo Using WILDFIRECHAT_HOME:   "%WILDFIRECHAT_HOME%"

rem  set LOG_CONSOLE_LEVEL=info
rem  set LOG_FILE_LEVEL=fine
set JAVA_OPTS=
set JAVA_OPTS_SCRIPT=-XX:+HeapDumpOnOutOfMemoryError -Djava.awt.headless=true
set WILDFIRECHAT_PATH=%WILDFIRECHAT_HOME%
set LOG_FILE=%WILDFIRECHAT_HOME%\config\log4j2.xml
set HZ_CONF_FILE=%WILDFIRECHAT_HOME%\config\hazelcast.xml
set C3P0_CONF_FILE=%WILDFIRECHAT_HOME%\config\c3p0-config.xml

rem Use the Hotspot garbage-first collector.
set JAVA_OPTS=%JAVA_OPTS%  -XX:+UseG1GC

rem Have the JVM do less remembered set work during STW, instead
rem preferring concurrent GC. Reduces p99.9 latency.
set JAVA_OPTS=%JAVA_OPTS%  -XX:G1RSetUpdatingPauseTimePercent=5

rem Main G1GC tunable: lowering the pause target will lower throughput and vise versa.
rem 200ms is the JVM default and lowest viable setting
rem 1000ms increases throughput. Keep it smaller than the timeouts.
set JAVA_OPTS=%JAVA_OPTS%  -XX:MaxGCPauseMillis=500

rem Optional G1 Settings

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

rem rem GC logging options -- uncomment to enable

rem set JAVA_OPTS=%JAVA_OPTS% -XX:+PrintGCDetails
rem set JAVA_OPTS=%JAVA_OPTS% -XX:+PrintGCDateStamps
rem set JAVA_OPTS=%JAVA_OPTS% -XX:+PrintHeapAtGC
rem set JAVA_OPTS=%JAVA_OPTS% -XX:+PrintTenuringDistribution
rem set JAVA_OPTS=%JAVA_OPTS% -XX:+PrintGCApplicationStoppedTime
rem set JAVA_OPTS=%JAVA_OPTS% -XX:+PrintPromotionFailure
rem set JAVA_OPTS=%JAVA_OPTS% -XX:PrintFLSStatistics=1
rem set JAVA_OPTS=%JAVA_OPTS% -XX:+UseGCLogFileRotation
rem set JAVA_OPTS=%JAVA_OPTS% -XX:NumberOfGCLogFiles=10
rem set JAVA_OPTS=%JAVA_OPTS% -XX:GCLogFileSize=10M"

echo
echo 请设置JVM参数Xmx和Xms！！！
echo

rem set JAVA_OPTS=%JAVA_OPTS% -Xmx2G
rem set JAVA_OPTS=%JAVA_OPTS% -Xms2G

%JAVA% -server %JAVA_OPTS% %JAVA_OPTS_SCRIPT% -Dlog4j.configurationFile=%LOG_FILE% -Dcom.mchange.v2.c3p0.cfg.xml=%C3P0_CONF_FILE% -Dhazelcast.configuration=file:%HZ_CONF_FILE% -Dwildfirechat.path=%WILDFIRECHAT_PATH% -cp %WILDFIRECHAT_HOME%\lib\* cn.wildfirechat.server.Server
