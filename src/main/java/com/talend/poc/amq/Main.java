package com.talend.poc.amq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;

public class Main {

    public final static Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        LOGGER.info("Max memory: {}", Runtime.getRuntime().maxMemory());

        int port = 8080;
        if (System.getenv("HTTP_PORT") != null) {
            port = Integer.parseInt(System.getenv("HTTP_PORT"));
        }
        String queueName = "INCOMING";
        if (System.getenv("JMS_QUEUE") != null) {
            queueName = System.getenv("JMS_QUEUE");
        }
        String entrypoint = "localhost:9092";
        if (System.getenv("KAFKA_ENTRYPOINT") != null) {
            entrypoint = System.getenv("KAFKA_ENTRYPOINT");
        }
        String topic = "INBOUND";
        if (System.getenv("KAFKA_TOPIC") != null) {
            topic = System.getenv("KAFKA_TOPIC");
        }
        String jettyConfig = null;
        if (System.getenv("JETTY_CONFIG") != null) {
            jettyConfig = System.getenv("JETTY_CONFIG");
        }
        String syncopeAccess = System.getenv("SYNCOPE_ACCESS");
        if (syncopeAccess == null) {
            throw new RuntimeException("SYNCOPE_ACCESS is not set");
        }
        String activemqUsername = System.getenv("ACTIVEMQ_USERNAME");
        if (activemqUsername == null) {
            throw new RuntimeException("ACTIVEMQ_USERNAME is not set");
        }
        String activemqPassword = System.getenv("ACTIVEMQ_PASSWORD");
        if (activemqPassword == null) {
            throw new RuntimeException("ACTIVEMQ_PASSWORD is not set");
        }

        LOGGER.info("Starting bridge on http://0.0.0.0:{}", port);
        BrokerFacade facade = new BrokerFacade(port, jettyConfig, syncopeAccess);
        facade.start();

        LOGGER.info("Starting kafka forwarder (http://0.0.0.0:{}/{} -> {}/{})", port, queueName, entrypoint, topic);
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://facade");
        Connection connection = connectionFactory.createConnection(activemqUsername, activemqPassword);
        Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
        Queue queue = session.createQueue(queueName);
        MessageConsumer messageConsumer = session.createConsumer(queue);
        messageConsumer.setMessageListener(new KafkaForwarder(entrypoint, topic));
        connection.start();
    }

}
