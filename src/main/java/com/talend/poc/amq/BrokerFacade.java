package com.talend.poc.amq;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.store.memory.MemoryPersistenceAdapter;
import org.apache.activemq.usage.MemoryUsage;
import org.apache.activemq.usage.StoreUsage;
import org.apache.activemq.usage.SystemUsage;

public class BrokerFacade {

    private BrokerService brokerService;

    public BrokerFacade(int port, String jettyConfig) throws Exception {
        brokerService = new BrokerService();
        brokerService.setBrokerName("facade");
        brokerService.setUseJmx(false);
        brokerService.setPersistenceAdapter(new MemoryPersistenceAdapter());
        if (jettyConfig != null) {
            brokerService.addConnector("http://0.0.0.0:" + port + "?jetty.config=" + jettyConfig);
        } else {
            brokerService.addConnector("http://0.0.0.0:" + port);
        }
        brokerService.addConnector("vm://facade");
        brokerService.setPopulateJMSXUserID(true);
        brokerService.setUseAuthenticatedPrincipalForJMSXUserID(false);
        SystemUsage systemUsage = new SystemUsage();
        MemoryUsage memoryUsage = new MemoryUsage();
        memoryUsage.setPercentOfJvmHeap(80);
        systemUsage.setMemoryUsage(memoryUsage);
        StoreUsage storeUsage = new StoreUsage();
        storeUsage.setLimit(419430400);
        systemUsage.setStoreUsage(storeUsage);
        brokerService.setSystemUsage(systemUsage);
    }

    public void start() throws Exception {
        brokerService.start();
    }

    public void stop() throws Exception {
        brokerService.stop();
    }
}
