package com.reddust9.velocitystatusapi;

public class YmlConfiguration {
    public String configVersion;
    public String serverBind;
    public Integer serverPort;
    public Player player;
    public Proxy proxy;
    public Server server;

    public static class Player {
        public boolean name;
        public boolean uuid;
        public boolean client;
        public boolean server;
        public boolean ping;
    }

    public static class Proxy {
        public boolean serverUptime;
        public boolean version;
        public boolean motd;
        public boolean icon;
        public boolean maxPlayers;
        public boolean onlinePlayers;
        public boolean connectedPlayers;
        public boolean connectedServers;
    }

    public static class Server {
        public boolean isOnline;
        public boolean serverName;
        public boolean version;
        public boolean motd;
        public boolean icon;
        public boolean maxPlayers;
        public boolean onlinePlayers;
        public boolean connectedPlayers;
    }
}