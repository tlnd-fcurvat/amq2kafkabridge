FROM java:8-jre-alpine

COPY target/amq-http-to-kafka-bridge-1.0-SNAPSHOT-shaded.jar /bridge.jar
COPY src/main/resources/jetty.xml /jetty.xml

ENV JETTY_CONFIG /jetty.xml

EXPOSE 8080
CMD ["java", "-jar", "/bridge.jar"]