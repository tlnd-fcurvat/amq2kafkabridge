FROM java:8-jre-alpine

COPY target/amq-http-to-kafka-bridge-1.0-SNAPSHOT-shaded.jar /bridge.jar
COPY src/main/resources/jetty.xml /jetty.xml
COPY src/main/resources/entrypoint.sh /usr/local/bin/entrypoint.sh

ENV JETTY_CONFIG /jetty.xml
ENV HOST_MEM_MB 512

RUN chmod a+x /usr/local/bin/entrypoint.sh

EXPOSE 8080

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]