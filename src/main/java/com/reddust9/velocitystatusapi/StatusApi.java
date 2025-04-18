package com.reddust9.velocitystatusapi;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.reddust9.velocitystatusapi.data.MotdStatusInfo;
import com.reddust9.velocitystatusapi.data.PlayerStatusInfo;
import com.reddust9.velocitystatusapi.data.ProxyStatusInfo;
import com.reddust9.velocitystatusapi.data.ServerStatusInfo;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Plugin(
        id = "statusapi",
        name = "StatusApi",
        version = BuildConstants.VERSION,
        authors = { "reddust9", "leorossi05" },
        url = "https://github.com/leorossi2005/velocity-status-api"
)

public class StatusApi {
    @Inject public Logger logger;
    @Inject public ProxyServer server;
    @Inject @DataDirectory public Path dataDir;

    private long bootTimestamp;
    private YmlConfiguration config;
    public Gson serialiser;
    private RequestHandler handler;
    private final Map<String, Boolean> serverLastOnlineStatus = new HashMap<>();

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        bootTimestamp = System.currentTimeMillis();
        serialiser = new Gson();
        loadConfiguration();
        handler = new RequestHandler(this, config);

        // Register commands
        CommandMeta commandMeta = server.getCommandManager().metaBuilder("statusapi").build();
        server.getCommandManager().register(commandMeta, new SimpleCommand() {
            @Override
            public void execute(Invocation invocation) {
                String[] args = invocation.arguments();

                if (args.length > 0 && args[0].equalsIgnoreCase("reloadConfig")) {
                    new StatusCommand().execute(invocation);
                } else if (args.length == 0) {
                    invocation.source().sendMessage(Component.text("Running VelocityStatusApi v" + BuildConstants.VERSION, NamedTextColor.GREEN));
                } else {
                    invocation.source().sendMessage(Component.text("Subcommand not found", NamedTextColor.RED));
                }
            }

            @Override
            public CompletableFuture<List<String>> suggestAsync(final Invocation invocation) {
                String[] args = invocation.arguments();

                List<String> suggestions = new ArrayList<>();

                if (args.length == 0 || args.length == 1) {
                    suggestions.add("reloadConfig");
                }

                return CompletableFuture.completedFuture(suggestions.stream()
                        .filter(suggestion -> args.length == 0 || suggestion.startsWith(args[args.length - 1].toLowerCase()))
                        .collect(Collectors.toList()));
            }
        });
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        handler.shutdown();
    }

    public class StatusCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            loadConfiguration();
            handler.shutdown();
            handler = new RequestHandler(StatusApi.this, config);
            source.sendMessage(Component.text("Configuration Reloaded", NamedTextColor.GREEN));
        }
    }

    private void loadConfiguration() {
        Yaml yaml = new Yaml(new Constructor(YmlConfiguration.class));
        Path filePath = dataDir.resolve("config.yml");
        File configFile = filePath.toFile();

        if (!configFile.exists()) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                if (configFile.getParentFile().mkdirs()) {
                    logger.info("Created data directory");
                }
                assert is != null;
                Files.copy(is, filePath);
            } catch (IOException e) {
                logger.error("Unable to write default config to file: {}", e.getMessage());
            }
        }

        try (InputStream inputStream = new FileInputStream(configFile)) {
            config = yaml.loadAs(inputStream, YmlConfiguration.class);
        } catch (IOException e) {
            logger.error("Unable to read text from config: {}", e.getMessage());
        }
    }

    public ProxyStatusInfo reportProxyStatus() {
        ProxyStatusInfo info = new ProxyStatusInfo();
        info.serverUptime = System.currentTimeMillis() - bootTimestamp;
        info.version = server.getVersion().getVersion();
        info.maxPlayers = server.getConfiguration().getShowMaxPlayers();
        info.onlinePlayers = server.getPlayerCount();
        info.connectedPlayers = new ArrayList<>();
        info.connectedServers = new ArrayList<>();

        Component motd = server.getConfiguration().getMotd();
        info.motd = new MotdStatusInfo();
        info.motd.raw = LegacyComponentSerializer.legacySection().serialize(motd);
        info.motd.clean = PlainTextComponentSerializer.plainText().serialize(motd);

        File iconFile = new File("server-icon.png");

        if (iconFile.exists()) {
            try {
                BufferedImage bufferedImage = ImageIO.read(iconFile);
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "jpg", byteArrayOutputStream);
                byte[] bytes = byteArrayOutputStream.toByteArray();
                info.icon = "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
            } catch (Exception e) {
                logger.warn("There was a problem creating the icon string");
            }
        }

        server.getAllPlayers().forEach(ply -> {
            PlayerStatusInfo pInfo = new PlayerStatusInfo();
            pInfo.name = ply.getUsername();
            pInfo.uuid = ply.getUniqueId().toString();
            pInfo.client = ply.getClientBrand();
            pInfo.ping = ply.getPing();

            if (ply.getCurrentServer().isPresent()) {
                pInfo.server = ply.getCurrentServer().get().getServerInfo().getName();
            }

            info.connectedPlayers.add(pInfo);
        });

        server.getAllServers().forEach(srv -> {
            ServerStatusInfo ssi = new ServerStatusInfo();
            ssi.serverName = srv.getServerInfo().getName();

            CompletableFuture<ServerPing> pingFuture = srv.ping();
            try {
                ServerPing ping = pingFuture.get(2, TimeUnit.SECONDS);
                ssi.isOnline = true;
                serverLastOnlineStatus.put(ssi.serverName, true);
                ssi.version = ping.getVersion().getName();
                ssi.maxPlayers = ping.asBuilder().getMaximumPlayers();
                ssi.onlinePlayers = ping.asBuilder().getOnlinePlayers();
                ssi.connectedPlayers = new ArrayList<>();

                Component serverMotd = ping.getDescriptionComponent();
                ssi.motd = new MotdStatusInfo();
                ssi.motd.raw = LegacyComponentSerializer.legacySection().serialize(serverMotd);
                ssi.motd.clean = PlainTextComponentSerializer.plainText().serialize(serverMotd);

                if (ping.getFavicon().isPresent()) {
                    ssi.icon = ping.getFavicon().get().getBase64Url();
                }

                ssi.connectedPlayers = info.connectedPlayers
                        .stream()
                        .filter(player -> player.server.equals(ssi.serverName))
                        .collect(Collectors.toCollection(ArrayList::new));
            } catch (ExecutionException | TimeoutException | InterruptedException e) {
                ssi.isOnline = false;

                Boolean wasOnline = serverLastOnlineStatus.get(ssi.serverName);
                if (wasOnline == null || wasOnline) {
                    logger.warn("Detected that server '{}' is unreachable!", ssi.serverName);
                    serverLastOnlineStatus.put(ssi.serverName, false);
                }
            }
            info.connectedServers.add(ssi);
        });
        return info;
    }
}
