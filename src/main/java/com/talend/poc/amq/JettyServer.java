package com.talend.poc.amq;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;

public class JettyServer {

    private Server server;
    private ServletHandler handler;

    public JettyServer(int port) {
        server = new Server(port);
        server.setStopTimeout(500L);
        handler = new ServletHandler();
        server.setHandler(handler);
    }

    public void start() throws Exception {
        server.start();
        server.join();
    }

    public void addServlet(Class servlet, String mapping) {
        handler.addServletWithMapping(servlet, mapping);
    }

    public void stop() throws Exception {
        server.stop();
    }

}
