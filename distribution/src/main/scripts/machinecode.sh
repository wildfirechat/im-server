#!/bin/sh

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

PRGDIR=`dirname "$PRG"`
cd ..
WILDFIRECHAT_HOME=`pwd`


export WILDFIRECHAT_HOME

if [ -f "${JAVA_HOME}/bin/java" ]; then
   JAVA=${JAVA_HOME}/bin/java
else
   JAVA=java
fi
export JAVA


WILDFIRECHAT_PATH=$WILDFIRECHAT_HOME/


$JAVA  -cp "$WILDFIRECHAT_HOME/lib/*" cn.wildfirechat.server.MechineCode $1 $2
