package com.talend.poc.amq;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws Exception {
        int port = 8080;
        if (System.getProperty("http.port") != null) {
            port = Integer.parseInt(System.getProperty("http.port"));
        }

        JettyServer server = new JettyServer(port);

        server.addServlet(BridgeServlet.class, "/bridge");
        server.addServlet(HealthCheck.class, "/health");

        server.start();
    }

    public static class HealthCheck extends HttpServlet {

        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println("<h1>OK</h1>");
        }

    }

}
