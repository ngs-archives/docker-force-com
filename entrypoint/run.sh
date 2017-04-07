#!/bin/sh

if [ $DEBUG ]; then
  set -eux
else
  set -eu
fi

BUILD_CLASSPATH=${JAVA_HOME}/lib/tools.jar:${JAVA_HOME}/lib/force-wsc-${API_VERSION}-uber.jar
RUN_CLASSPATH="${BUILD_CLASSPATH}:/entrypoint"
for F in /wsdl/*.wsdl; do
  FILENAME=$(basename $F)
  NAME="${FILENAME%%.*}"
  JAR="${JAVA_HOME}/lib/force-com-wsdl/${NAME}.jar"
  java -classpath $BUILD_CLASSPATH com.sforce.ws.tools.wsdlc $F $JAR
  RUN_CLASSPATH="${RUN_CLASSPATH}:${JAR}"
done

echo $@
javac -classpath $RUN_CLASSPATH /entrypoint/TestRunner.java && java -classpath $RUN_CLASSPATH TestRunner $@

