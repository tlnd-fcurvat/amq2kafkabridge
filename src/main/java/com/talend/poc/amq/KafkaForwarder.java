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

    private String entrypoint;
    private String topic;
    private KafkaProducer<String, String> producer;

    public KafkaForwarder() {
        this.entrypoint = "localhost:9092";
        if (System.getenv("KAFKA_ENTRYPOINT") != null) {
            this.entrypoint = System.getenv("KAFKA_ENTRYPOINT");
        }
        this.topic = "INBOUND";
        if (System.getenv("KAFKA_TOPIC") != null) {
            this.topic = System.getenv("KAFKA_TOPIC");
        }
        Properties properties = new Properties();
        properties.put("bootstrap.servers", entrypoint);
        properties.put("client.id", "gateway");
        properties.put("acks", "all");
        properties.put("retries", "3");
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("topic", topic);
        this.producer = new KafkaProducer<String, String>(properties);
    }

    @Override
    public void onMessage(Message message) {
        try {
            producer.send(new ProducerRecord<String, String>(topic, ((TextMessage) message).getText()));
            producer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
