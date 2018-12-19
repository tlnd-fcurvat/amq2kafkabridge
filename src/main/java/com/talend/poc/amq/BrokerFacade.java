package com.talend.poc.amq;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.store.memory.MemoryPersistenceAdapter;

public class BrokerFacade {

    private BrokerService brokerService;

    public BrokerFacade(int port) throws Exception {
        brokerService = new BrokerService();
        brokerService.setBrokerName("facade");
        brokerService.setUseJmx(false);
        brokerService.setPersistenceAdapter(new MemoryPersistenceAdapter());
        brokerService.addConnector("http://0.0.0.0:" + port);
        brokerService.addConnector("vm://facade");
        brokerService.setPopulateJMSXUserID(true);
        brokerService.setUseAuthenticatedPrincipalForJMSXUserID(false);
    }

    public void start() throws Exception {
        brokerService.start();
    }

    public void stop() throws Exception {
        brokerService.stop();
    }
}
