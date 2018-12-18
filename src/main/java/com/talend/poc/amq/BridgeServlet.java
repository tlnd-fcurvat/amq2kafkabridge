package com.talend.poc.amq;

import org.apache.activemq.command.Command;
import org.apache.activemq.command.ConnectionInfo;
import org.apache.activemq.command.WireFormatInfo;
import org.apache.activemq.transport.http.BlockingQueueTransport;
import org.apache.activemq.transport.util.TextWireFormat;
import org.apache.activemq.transport.xstream.XStreamWireFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.zip.GZIPInputStream;

public class BridgeServlet extends HttpServlet {

    private final static Logger LOGGER = LoggerFactory.getLogger(BridgeServlet.class);

    private ConcurrentMap<String, BlockingQueueTransport> clients = new ConcurrentHashMap<String, BlockingQueueTransport>();
    private TextWireFormat wireFormat;

    @Override
    public void init() throws ServletException {
        LOGGER.info("Initializing BridgeServlet");
        super.init();
        wireFormat = new XStreamWireFormat();
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        trace(request);
        response.addHeader("Accepts-Encoding", "gzip");
        super.doOptions(request, response);
    }

    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        trace(request);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        trace(request);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // trace(request);
        InputStream stream = request.getInputStream();
        String contentType = request.getContentType();
        if (contentType != null && contentType.equals("application/x-gzip")) {
            stream = new GZIPInputStream(stream);
        }

        // Read the command directly from the reader, assuming UTF8 encoding
        Command command = (Command) wireFormat.unmarshalText(new InputStreamReader(stream, "UTF-8"));

        if (command instanceof WireFormatInfo) {
            WireFormatInfo info = (WireFormatInfo) command;
            if (!canProcessWireFormatVersion(info.getVersion())) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Cannot process wire format of version: "
                        + info.getVersion());
            }

        } else {

            BlockingQueueTransport transport = getTransportChannel(request, response);
            if (transport == null) {
                return;
            }

            if (command instanceof ConnectionInfo) {
                ((ConnectionInfo) command).setTransportContext(request.getAttribute("javax.servlet.request.X509Certificate"));
            }
            transport.doConsume(command);
        }
    }

    private boolean canProcessWireFormatVersion(int version) {
        return true;
    }

    protected BlockingQueueTransport getTransportChannel(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String clientID = request.getHeader("clientID");
        if (clientID == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No clientID header specified");
            LOGGER.warn("No clientID header specified");
            return null;
        }
        BlockingQueueTransport answer = clients.get(clientID);
        if (answer == null) {
            LOGGER.warn("The clientID header specified is invalid. Client sesion has not yet been established for it: " + clientID);
            return null;
        }
        return answer;
    }

    private void trace(HttpServletRequest request) throws IOException {
        LOGGER.info("== Method {}", request.getMethod());
        LOGGER.info("== Context Path {}", request.getContextPath());
        LOGGER.info("== Path Info {}", request.getPathInfo());
        LOGGER.info("== Headers");
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            LOGGER.info("    {} = {}", headerName, request.getHeader(headerName));
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
        String line;
        StringBuilder builder = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            builder.append(line).append("\n");
        }
        LOGGER.info("== Payload");
        LOGGER.info("----");
        LOGGER.info(builder.toString());
        LOGGER.info("----");
    }

}
