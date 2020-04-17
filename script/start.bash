#!/bin/bash
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 \
-Djava.rmi.server.hostname=eca1 \
-Dcom.sun.mannagement.jmxremote.port=18888 \
-Dcom.sun.management.jmxremote=true \
-Dcom.sun.management.jmxremote.ssl=false \
-Dcom.sun.managementote.ssl=false \
-Dcom.sun.management.jmxremote.authenticate=false \
$1 -jar /project/netcrazy/target/netcrazy-1.0-SNAPSHOT.jar