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
        int port = 8080;
        if (System.getProperty("http.port") != null) {
            port = Integer.parseInt(System.getProperty("http.port"));
        }
        String queueName = "INCOMING";
        if (System.getenv("JMS_QUEUE") != null) {
            queueName = System.getenv("JMS_QUEUE");
        }

        LOGGER.info("Starting bridge on http://0.0.0.0:{}", port);
        BrokerFacade facade = new BrokerFacade(port);
        facade.start();

        LOGGER.info("Starting kafka forwarder ({})", queueName);
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://facade");
        Connection connection = connectionFactory.createConnection();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue queue = session.createQueue(queueName);
        MessageConsumer messageConsumer = session.createConsumer(queue);
        messageConsumer.setMessageListener(new KafkaForwarder());
        connection.start();
    }

}
