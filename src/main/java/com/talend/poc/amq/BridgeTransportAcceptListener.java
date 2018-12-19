package com.talend.poc.amq;

import org.apache.activemq.transport.Transport;
import org.apache.activemq.transport.TransportAcceptListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BridgeTransportAcceptListener implements TransportAcceptListener {

    private final static Logger LOGGER = LoggerFactory.getLogger(BridgeTransportAcceptListener.class);

    public void onAccept(Transport transport) {
        LOGGER.info(transport.toString());
    }

    public void onAcceptError(Exception error) {

    }
}
