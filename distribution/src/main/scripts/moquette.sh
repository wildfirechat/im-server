#!/bin/sh
#
# Copyright (c) 2012-2014 Andrea Selva
#

echo "##########################################################"
echo "#                                                        #"
echo "#   Launching Moquette-MQTT broker                       #"
echo "#                                                        #"
echo "##########################################################"

cd "$(dirname "$0")"

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

# Get standard environment variables
PRGDIR=`dirname "$PRG"`

# Only set MOQUETTE_HOME if not already set
[ -f "$MOQUETTE_HOME"/bin/moquette.sh ] || MOQUETTE_HOME=`cd "$PRGDIR/.." ; pwd`
export MOQUETTE_HOME

# Set JavaHome if it exists
if [ -f "${JAVA_HOME}/bin/java" ]; then 
   JAVA=${JAVA_HOME}/bin/java
else
   JAVA=java
fi
export JAVA

#LOG_FILE=$ORIENTDB_HOME/config/moquette-server-log.properties
#LOG_CONSOLE_LEVEL=info
#LOG_FILE_LEVEL=fine
#set MOQUETTE_SETTINGS="-Dprofiler.enabled=true -Dcache.level1.enabled=false -Dcache.level2.enabled=false"
JAVA_OPTS_SCRIPT="-XX:+HeapDumpOnOutOfMemoryError -Djava.awt.headless=true"

#$JAVA -server $JAVA_OPTS $JAVA_OPTS_SCRIPT $ORIENTDB_SETTINGS -Dfile.encoding=UTF8 -Djava.util.logging.config.file="$LOG_FILE" -Dmoquette.config.file="$CONFIG_FILE" -Dmoquette.www.path="$WWW_PATH" -Dmoquette.build.number="@BUILD@" -cp "$ORIENTDB_HOME/lib/moquette-server-@VERSION@.jar:$ORIENTDB_HOME/lib/*" com.orientechnologies.orient.server.OServerMain
$JAVA -server $JAVA_OPTS $JAVA_OPTS_SCRIPT -cp "$MOQUETTE_HOME/lib/moquette-broker-0.5-SNAPSHOT.jar:$MOQUETTE_HOME/lib/*" org.dna.mqtt.moquette.server.Server

