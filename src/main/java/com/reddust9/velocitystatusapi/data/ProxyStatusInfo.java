package com.reddust9.velocitystatusapi.data;

import java.util.ArrayList;

public class ProxyStatusInfo {
    public Long serverUptime;
    public String version;
    public MotdStatusInfo motd;
    public String icon;
    public Integer maxPlayers;
    public Integer onlinePlayers;
    public ArrayList<PlayerStatusInfo> connectedPlayers;
    public ArrayList<ServerStatusInfo> connectedServers;
}