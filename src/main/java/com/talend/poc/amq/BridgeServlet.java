package com.talend.poc.amq;

import org.apache.activemq.Service;
import org.apache.activemq.command.Command;
import org.apache.activemq.command.ConnectionInfo;
import org.apache.activemq.command.WireFormatInfo;
import org.apache.activemq.transport.Transport;
import org.apache.activemq.transport.TransportAcceptListener;
import org.apache.activemq.transport.http.BlockingQueueTransport;
import org.apache.activemq.transport.http.HttpTransportFactory;
import org.apache.activemq.transport.util.TextWireFormat;
import org.apache.activemq.transport.xstream.XStreamWireFormat;
import org.apache.activemq.util.IOExceptionSupport;
import org.apache.activemq.util.ServiceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

public class BridgeServlet extends HttpServlet {

    private final static Logger LOGGER = LoggerFactory.getLogger(BridgeServlet.class);

    private TransportAcceptListener listener;
    private HttpTransportFactory transportFactory;
    private ConcurrentMap<String, BlockingQueueTransport> clients = new ConcurrentHashMap<String, BlockingQueueTransport>();
    private HashMap<String, Object> transportOptions;
    private TextWireFormat wireFormat;

    @Override
    public void init() throws ServletException {
        super.init();
        listener = new BridgeTransportAcceptListener();
        transportFactory = new HttpTransportFactory();
        transportOptions = (HashMap<String, Object>)getServletContext().getAttribute("transportOptions");
        wireFormat = (TextWireFormat)getServletContext().getAttribute("wireFormat");
        if (wireFormat == null) {
            wireFormat = createWireFormat();
        }
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        trace(request);
        response.addHeader("Accepts-Encoding", "gzip");
        super.doOptions(request, response);
    }

    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        createTransportChannel(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // lets return the next response
        Command packet = null;
        int count = 0;
        try {
            BlockingQueueTransport transportChannel = getTransportChannel(request, response);
            if (transportChannel == null) {
                return;
            }

            packet = (Command) transportChannel.getQueue().poll(30L, TimeUnit.MILLISECONDS);

            DataOutputStream stream = new DataOutputStream(response.getOutputStream());
            wireFormat.marshal(packet, stream);
            count++;
        } catch (InterruptedException ignore) {
        }

        if (count == 0) {
            response.setStatus(HttpServletResponse.SC_REQUEST_TIMEOUT);
        }
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
            LOGGER.info("Command {}", command.toString());
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

    protected BlockingQueueTransport createTransportChannel(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String clientID = request.getHeader("clientID");

        if (clientID == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No clientID header specified");
            LOGGER.warn("No clientID header specified");
            return null;
        }

        // Optimistically create the client's transport; this transport may be thrown away if the client has already registered.
        BlockingQueueTransport answer = createTransportChannel();

        // Record the client's transport and ensure that it has not already registered; this is thread-safe and only allows one
        // thread to register the client
        if (clients.putIfAbsent(clientID, answer) != null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "A session for clientID '" + clientID + "' has already been established");
            LOGGER.warn("A session for clientID '" + clientID + "' has already been established");
            return null;
        }

        // Ensure that the client's transport is cleaned up when no longer
        // needed.
        answer.addServiceListener(new ServiceListener() {
            public void started(Service service) {
                // Nothing to do.
            }

            public void stopped(Service service) {
                clients.remove(clientID);
            }
        });

        // Configure the transport with any additional properties or filters.  Although the returned transport is not explicitly
        // persisted, if it is a filter (e.g., InactivityMonitor) it will be linked to the client's transport as a TransportListener
        // and not GC'd until the client's transport is disposed.
        Transport transport = answer;
        try {
            // Preserve the transportOptions for future use by making a copy before applying (they are removed when applied).
            HashMap<String, Object> options = new HashMap<String, Object>(transportOptions);
            transport = transportFactory.serverConfigure(answer, null, options);
        } catch (Exception e) {
            throw IOExceptionSupport.create(e);
        }

        // Wait for the transport to be connected or disposed.
        listener.onAccept(transport);
        while (!transport.isConnected() && !transport.isDisposed()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignore) {
            }
        }

        // Ensure that the transport was not prematurely disposed.
        if (transport.isDisposed()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The session for clientID '" + clientID + "' was prematurely disposed");
            LOGGER.warn("The session for clientID '" + clientID + "' was prematurely disposed");
            return null;
        }

        return answer;
    }

    protected BlockingQueueTransport createTransportChannel() {
        return new BlockingQueueTransport(new LinkedBlockingQueue<Object>());
    }

    protected TextWireFormat createWireFormat() {
        return new XStreamWireFormat();
    }

}
