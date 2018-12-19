package com.talend.poc.amq;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import java.util.Properties;

public class KafkaForwarder implements MessageListener {

    private final static Logger LOGGER = LoggerFactory.getLogger(KafkaForwarder.class);

    private String topic;
    private KafkaProducer<String, String> producer;

    public KafkaForwarder(String entrypoint, String topic) {
        this.topic = topic;
        Properties properties = new Properties();
        properties.put("bootstrap.servers", entrypoint);
        properties.put("acks", "all");
        properties.put("retries", "3");
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        this.producer = new KafkaProducer<>(properties);
    }

    @Override
    public void onMessage(Message message) {
        try {
            producer.send(new ProducerRecord<>(topic, ((TextMessage) message).getText())).get();
            producer.flush();
            message.acknowledge();
        } catch (Exception e) {
            LOGGER.error("Can't forward message to Kafka", e);
        }
    }

}
