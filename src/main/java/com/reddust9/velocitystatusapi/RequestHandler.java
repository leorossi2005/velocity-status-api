package com.reddust9.velocitystatusapi;

import com.reddust9.velocitystatusapi.data.MotdStatusInfo;
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
import java.util.ArrayList;

public class RequestHandler {
    private final StatusApi plugin;
    private HttpServer server;

    public RequestHandler(StatusApi _plugin, YmlConfiguration _config) {
        plugin = _plugin;
        try {
            server = HttpServer.create(new InetSocketAddress(_config.serverBind, _config.serverPort), 0);
        } catch (IOException e) {
            plugin.logger.error("Could not create HttpServer. Is the port in use?");
            return;
        }

        // setup request contexts
        server.createContext("/", new RootHandler());
        server.createContext("/text", new TextHandler(_config));
        server.createContext("/json", new JsonHandler(_config));

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
                Uptime: 13406
                Version: 3.4.0-SNAPSHOT
                MOTD: I'm a velocity server
                Max players: 100
                Online players: 3
                Players:
                        - reddust9 on vanilla client connected to lobby [130 ping]
                        - leorossi05 on vanilla client connected to lobby [280 ping]
                        - anotherguy on vanilla client connected to smp [200 ping]

                Connected servers:
                        ==> lobby (online)
                        Version: 1.21.4
                        MOTD: Lobby server
                        Max players: 34
                        Online players: 2
                        Players:
                                - reddust9 on vanilla client connected to lobby [130 ping]
                                - leorossi05 on vanilla client connected to lobby [280 ping]

                        ==> smp (online)
                        Version: 1.21.4
                        MOTD: I'm an smp server
                        Max players: 33
                        Online players: 1
                        Players:
                                - anotherguy on vanilla client connected to smp [200 ping]

                        ==> bedwars (offline)
        */
        private final YmlConfiguration _config;

        public TextHandler(YmlConfiguration config) {
            _config = config;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            ProxyStatusInfo info = plugin.reportProxyStatus();
            StringBuilder rb = new StringBuilder();

            rb.append("=== SERVER STATUS ===\n");

            if (!_config.proxy.connectedPlayers && !_config.proxy.connectedServers && !_config.proxy.maxPlayers && !_config.proxy.onlinePlayers && !_config.proxy.motd && !_config.proxy.serverUptime && !_config.proxy.version) {
                rb.append("Emmm... I don't think this makes any sense.\n");
            }

            // Proxy info
            appendIfEnabled(_config.proxy.serverUptime, rb, "Uptime: %d\n", info.serverUptime);
            appendIfEnabled(_config.proxy.version, rb, "Version: %s\n", info.version);
            appendIfEnabled(_config.proxy.motd, rb, "MOTD: %s\n", info.motd.clean);
            appendIfEnabled(_config.proxy.maxPlayers, rb, "Max players: %d\n", info.maxPlayers);
            appendIfEnabled(_config.proxy.onlinePlayers, rb, "Online players: %d\n", info.onlinePlayers);

            // Players info
            if (_config.proxy.connectedPlayers && (_config.player.name || _config.player.uuid || _config.player.client || _config.player.server || _config.player.ping)) {
                rb.append("Players:\n");
                info.connectedPlayers.forEach(psi -> {
                    rb.append("\t- ");
                    appendIfEnabled(_config.player.name, rb, "%s ", psi.name);
                    appendIfEnabled(_config.player.client, rb, "on %s client ", psi.client);
                    appendIfEnabled(_config.player.server, rb, "connected to %s ", psi.server);
                    appendIfEnabled(_config.player.ping, rb, "[%d]", psi.ping);
                    rb.append("\n");
                });
            }
            rb.append("\n");

            // Servers info
            if (_config.proxy.connectedServers && !info.connectedServers.isEmpty()) {
                rb.append("Connected servers:\n");
                info.connectedServers.forEach(ssi -> {
                    rb.append(String.format("\t==> %s ", _config.server.serverName ? ssi.serverName : "a server"));
                    appendIfEnabled(_config.server.isOnline, rb, "%s", ssi.isOnline ? "(online)" : "(offline)");
                    rb.append("\n");

                    if (ssi.isOnline) {
                        appendIfEnabled(_config.server.version, rb, "\tVersion: %s\n", ssi.version);
                        appendIfEnabled(_config.server.motd, rb, "\tMOTD: %s\n", ssi.motd.clean);
                        appendIfEnabled(_config.server.maxPlayers, rb, "\tMax players: %d\n", ssi.maxPlayers);
                        appendIfEnabled(_config.server.onlinePlayers, rb, "\tOnline players: %d\n", ssi.onlinePlayers);

                        if (_config.server.connectedPlayers && (_config.player.name || _config.player.uuid || _config.player.client || _config.player.server || _config.player.ping)) {
                            rb.append("\tPlayers:\n");
                            ssi.connectedPlayers.forEach(psi -> {
                                rb.append("\t\t- ");
                                appendIfEnabled(_config.player.name, rb, "%s ", psi.name);
                                appendIfEnabled(_config.player.client, rb, "on %s client ", psi.client);
                                appendIfEnabled(_config.player.server, rb, "connected to %s ", psi.server);
                                appendIfEnabled(_config.player.ping, rb, "[%d]", psi.ping);
                                rb.append("\n");
                            });
                        }
                        rb.append("\n");
                    }
                });
            }

            byte[] response = rb.toString().getBytes(Charset.defaultCharset());
            Headers headers = exchange.getResponseHeaders();
            headers.add("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, response.length);

            try (OutputStream stream = exchange.getResponseBody()) {
                stream.write(response);
            }
        }

        private void appendIfEnabled(boolean enabled, StringBuilder sb, String format, Object... args) {
            if (enabled) {
                sb.append(String.format(format, args));
            }
        }

    }

    class JsonHandler implements HttpHandler {
        private final YmlConfiguration _config;

        public JsonHandler(YmlConfiguration config) {
            _config = config;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            ProxyStatusInfo info = plugin.reportProxyStatus();
            ProxyStatusInfo filteredInfo = filterInfoBasedOnConfig(info, _config);
            String responseText = plugin.serialiser.toJson(filteredInfo);
            byte[] response = responseText.getBytes(Charset.defaultCharset());
            Headers headers = exchange.getResponseHeaders();

            // add CORS headers
            // https://stackoverflow.com/a/43044139
            headers.add("Access-Control-Allow-Origin", "*");
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                headers.add("Access-Control-Allow-Methods", "GET, OPTIONS");
                headers.add("Access-Control-Allow-Headers", "Content-Type,Authorization");
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            // add response headers
            headers.add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);

            // send body
            try (OutputStream stream = exchange.getResponseBody()) {
                stream.write(response);
            }
        }

        private ProxyStatusInfo filterInfoBasedOnConfig(ProxyStatusInfo info, YmlConfiguration config) {
            ProxyStatusInfo filtered = new ProxyStatusInfo();

            // Proxy info
            filtered.serverUptime = config.proxy.serverUptime ? info.serverUptime : null;
            if (config.proxy.version) filtered.version = info.version;
            if (config.proxy.icon) filtered.icon = info.icon;
            filtered.maxPlayers = config.proxy.maxPlayers ? info.maxPlayers : null;
            filtered.onlinePlayers = config.proxy.onlinePlayers ? info.onlinePlayers : null;

            // Motd
            if (config.proxy.motd) {
                filtered.motd = new MotdStatusInfo();
                filtered.motd.raw = info.motd.raw;
                filtered.motd.clean = info.motd.clean;
            }

            // Connected players
            if (config.proxy.connectedPlayers) {
                filtered.connectedPlayers = new ArrayList<>();
                info.connectedPlayers.forEach(player -> {
                    PlayerStatusInfo filteredPlayer = new PlayerStatusInfo();
                    if (config.player.name) filteredPlayer.name = player.name;
                    if (config.player.uuid) filteredPlayer.uuid = player.uuid;
                    if (config.player.client) filteredPlayer.client = player.client;
                    if (config.player.server) filteredPlayer.server = player.server;
                    filteredPlayer.ping = config.player.ping ? player.ping : null;

                    // Add player only if it has at least one property
                    if (config.player.name || config.player.uuid || config.player.client || config.player.server || config.player.ping) {
                        filtered.connectedPlayers.add(filteredPlayer);
                    }
                });
                if (filtered.connectedPlayers.isEmpty()) {
                    filtered.connectedPlayers = null;
                }
            }

            // Connected servers
            if (config.proxy.connectedServers) {
                filtered.connectedServers = new ArrayList<>();
                info.connectedServers.forEach(server -> {
                    ServerStatusInfo filteredServer = new ServerStatusInfo();
                    filteredServer.serverName = config.server.serverName ? server.serverName : "a server";
                    filteredServer.isOnline = config.server.isOnline ? server.isOnline : null;

                    if (server.isOnline) {
                        if (config.server.version) filteredServer.version = server.version;
                        if (config.server.icon) filteredServer.icon = server.icon;
                        filteredServer.maxPlayers = config.server.maxPlayers ? server.maxPlayers : null;
                        filteredServer.onlinePlayers = config.server.onlinePlayers ? server.onlinePlayers : null;

                        // Motd
                        if (config.server.motd) {
                            filteredServer.motd = new MotdStatusInfo();
                            filteredServer.motd.raw = server.motd.raw;
                            filteredServer.motd.clean = server.motd.clean;
                        }

                        // Connected players in server
                        if (config.server.connectedPlayers) {
                            filteredServer.connectedPlayers = new ArrayList<>();
                            server.connectedPlayers.forEach(player -> {
                                PlayerStatusInfo filteredPlayer = new PlayerStatusInfo();
                                if (config.player.name) filteredPlayer.name = player.name;
                                if (config.player.uuid) filteredPlayer.uuid = player.uuid;
                                if (config.player.client) filteredPlayer.client = player.client;
                                if (config.player.server) filteredPlayer.server = player.server;
                                filteredPlayer.ping = config.player.ping ? player.ping : null;

                                // Add player only if it has at least one property
                                if (config.player.name || config.player.uuid || config.player.client || config.player.server || config.player.ping) {
                                    filteredServer.connectedPlayers.add(filteredPlayer);
                                }
                            });
                            if (filteredServer.connectedPlayers.isEmpty()) {
                                filteredServer.connectedPlayers = null;
                            }
                        }
                    }

                    filtered.connectedServers.add(filteredServer);
                });
                if (filtered.connectedServers.isEmpty()) {
                    filtered.connectedServers = null;
                }
            }

            return filtered;
        }
    }
}
