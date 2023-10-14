package com.reddust9.velocitystatusapi;

import com.reddust9.velocitystatusapi.data.PlayerStatusInfo;
import com.reddust9.velocitystatusapi.data.ProxyStatusInfo;
import com.reddust9.velocitystatusapi.data.ServerStatusInfo;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

public class RequestHandler {
    private final StatusApi plugin;
    private HttpServer server;

    public RequestHandler(StatusApi _plugin, JsonConfiguration _config) {
        plugin = _plugin;
        try {
            server = HttpServer.create(new InetSocketAddress(_config.serverBind, _config.serverPort), 0);
        } catch (IOException e) {
            plugin.logger.error("Could not create HttpServer. Is the port in use?");
            return;
        }

        // setup request contexts
        server.createContext("/", new RootHandler());
        server.createContext("/text", new TextHandler());
        server.createContext("/json", new JsonHandler());

        // use default executor and start
        server.setExecutor(null);
        server.start();
    }

    public void shutdown() {
        server.stop(0);
    }

    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String responseString =
                    "Velocity status API\n" +
                    "Version: " + BuildConstants.VERSION + "\n" +
                    "Supported endpoints:\n" +
                    "- /text\n" +
                    "- /json\n";
            byte[] response = responseString.getBytes(Charset.defaultCharset());
            Headers headers = exchange.getResponseHeaders();
            headers.add("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, response.length);
            OutputStream stream = exchange.getResponseBody();
            stream.write(response);
            stream.close();
        }
    }

    class TextHandler implements HttpHandler {
/*
        Formatting example:

        === SERVER STATUS ===
        Uptime: 111

        Connected servers:

                ==> lobby (online)
        Players:
                - reddust9 on Vanilla [120 ping]
                - someoneelse on Vanilla [250 ping]

                ==> smp (online)
        Players:
                - anotherguy on Vanilla [200 ping]

                ==> bedwars (offline)
        Players:
*/

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            ProxyStatusInfo info = plugin.reportProxyStatus();
            StringBuilder rb = new StringBuilder("=== SERVER STATUS ===\n");

            rb.append("Uptime: ");
            rb.append(info.serverUptime);
            rb.append("\n\n");

            rb.append("Connected servers:\n");
            for(ServerStatusInfo ssi : info.connectedServers) {
                rb.append("\n==> ");
                rb.append(ssi.serverName);
                rb.append(ssi.isOnline ? " (online)" : " (offline)");
                rb.append('\n');
                rb.append("    Players:\n");
                for(PlayerStatusInfo psi : ssi.connectedPlayers) {
                    rb.append("    - ");
                    rb.append(psi.name);
                    rb.append(" on ");
                    rb.append(psi.client);
                    rb.append(" [");
                    rb.append(psi.ping);
                    rb.append(" ping]\n");
                }
            }

            byte[] response = rb.toString().getBytes(Charset.defaultCharset());
            Headers headers = exchange.getResponseHeaders();
            headers.add("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, response.length);
            OutputStream stream = exchange.getResponseBody();
            stream.write(response);
            stream.close();
        }
    }

    class JsonHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            ProxyStatusInfo info = plugin.reportProxyStatus();
            String responseText = plugin.serialiser.toJson(info);
            byte[] response = responseText.getBytes(Charset.defaultCharset());

            Headers headers = exchange.getResponseHeaders();
            headers.add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            OutputStream stream = exchange.getResponseBody();
            stream.write(response);
            stream.close();
        }
    }
}
