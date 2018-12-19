package com.talend.poc.amq;

public class Main {

    public static void main(String[] args) throws Exception {
        int port = 8080;
        if (System.getProperty("http.port") != null) {
            port = Integer.parseInt(System.getProperty("http.port"));
        }

        JettyServer server = new JettyServer(port);

        server.addServlet(BridgeServlet.class, "/bridge");
        server.addServlet(HealthCheckServlet.class, "/check");

        server.start();
    }

}
