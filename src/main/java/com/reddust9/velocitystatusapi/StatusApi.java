package com.reddust9.velocitystatusapi;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.reddust9.velocitystatusapi.data.PlayerStatusInfo;
import com.reddust9.velocitystatusapi.data.ProxyStatusInfo;
import com.reddust9.velocitystatusapi.data.ServerStatusInfo;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Plugin(
        id = "statusapi",
        name = "StatusApi",
        version = BuildConstants.VERSION,
        authors = { "reddust9" },
        url = "https://github.com/sbcomputertech/velocity-status-api"
)
public class StatusApi {
    @Inject public Logger logger;
    @Inject public ProxyServer server;
    @Inject @DataDirectory public Path dataDir;

    private long bootTimestamp;
    private JsonConfiguration config;
    public Gson serialiser;
    private RequestHandler handler;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        bootTimestamp = System.currentTimeMillis();
        serialiser = new Gson();
        loadConfiguration();
        handler = new RequestHandler(this, config);
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        handler.shutdown();
    }

    private void loadConfiguration() {
        Path filePath = dataDir.resolve("config.json");
        File configFile = filePath.toFile();

        if(!configFile.exists()) {
            logger.info("Creating config file with default values");
            config = new JsonConfiguration();
            String configText = serialiser.toJson(config);

            try {
                if(configFile.getParentFile().mkdirs()) {
                    logger.info("Created data directory");
                }
                Files.write(filePath, configText.getBytes());
            } catch (IOException e) {
                logger.error("Unable to write default config to file: {}", e.getMessage());
            }
        } else {
            logger.info("Loading configuration from file");

            String configText;
            try {
                configText = Files.readString(filePath);
            } catch (IOException e) {
                logger.error("Unable to read text from config file");
                return;
            }

            config = serialiser.fromJson(configText, JsonConfiguration.class);
        }
    }

    public ProxyStatusInfo reportProxyStatus() {
        ProxyStatusInfo info = new ProxyStatusInfo();
        info.serverUptime = System.currentTimeMillis() - bootTimestamp;
        info.connectedServers = new ArrayList<>();

        server.getAllServers().forEach(srv -> {
            ServerStatusInfo ssi = new ServerStatusInfo();
            ssi.serverName = srv.getServerInfo().getName();

            CompletableFuture<ServerPing> pingFuture = srv.ping();
            try {
                ServerPing ping = pingFuture.get(2, TimeUnit.SECONDS);
                ssi.isOnline = true;
                ssi.connectedPlayers = new ArrayList<>();

                if(ping.getPlayers().isPresent()) {
                    ssi.connectedPlayers = reportPlayerStatuses(ping.getPlayers().get().getSample());
                }

            } catch (ExecutionException | TimeoutException | InterruptedException e) {
                ssi.isOnline = false;
                logger.warn("Detected that server '{}' is unreachable!", ssi.serverName);
            }

            info.connectedServers.add(ssi);
        });

        return info;
    }

    private ArrayList<PlayerStatusInfo> reportPlayerStatuses(List<ServerPing.SamplePlayer> samplePlayers) {
        ArrayList<PlayerStatusInfo> players = new ArrayList<>();
        for(ServerPing.SamplePlayer sample : samplePlayers) {
            PlayerStatusInfo info = new PlayerStatusInfo();

            // we already know this player exists since
            // the UUID was retrieved from the server
            // noinspection OptionalGetWithoutIsPresent
            Player ply = server.getPlayer(sample.getId()).get();

            info.name = ply.getUsername();
            info.uuid = ply.getUniqueId().toString();
            info.client = ply.getClientBrand();
            info.ping = ply.getPing();

            players.add(info);
        }
        return players;
    }
}
