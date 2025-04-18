package com.reddust9.velocitystatusapi.data;

import java.util.ArrayList;

public class ServerStatusInfo {
    public Boolean isOnline;
    public String serverName;
    public String version;
    public MotdStatusInfo motd;
    public String icon;
    public Integer maxPlayers;
    public Integer onlinePlayers;
    public ArrayList<PlayerStatusInfo> connectedPlayers;
}