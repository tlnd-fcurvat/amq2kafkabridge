FROM java:8-jre-alpine

COPY target/amq-http-to-kafka-bridge-1.0-SNAPSHOT-shaded.jar /bridge.jar

EXPOSE 8080
CMD ["java", "-jar", "/bridge.jar"]