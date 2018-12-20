#!/bin/sh

set -e

freeMem=$(awk '/MemFree/ { print int($2/1024) }' /proc/meminfo)
s=$(($freeMem/10*8))
x=$(($freeMem/10*8))
n=$(($freeMem/10*2))
export JVM_ARGS="-Xmn${n}m -Xms${s}m -Xmx${x}m"

echo "START Running gateway on $(date)"
echo "JVM_ARGS=${JVM_ARGS}"

exec java $JVM_ARGS -jar bridge.jar