#!/bin/sh

set -e

if [ -z "$JAVA_OPTS" ]; then
    if [ -z "$HOST_MEM_MB" ]; then
        export JAVA_OPTS="-Xmx512m"
    else
        s=$(($HOST_MEM_MB/10*8))
        x=$(($HOST_MEM_MB/10*8))
        n=$(($HOST_MEM_MB/10*2))
        export JAVA_OPTS="-Xmn${n}m -Xms${s}m -Xmx${x}m"
    fi
fi

echo "START Running gateway on $(date)"
echo "JAVA_OPTS=${JAVA_OPTS}"

exec java $JAVA_OPTS -jar bridge.jar