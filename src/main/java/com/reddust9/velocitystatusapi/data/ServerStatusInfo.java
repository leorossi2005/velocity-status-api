package com.reddust9.velocitystatusapi.data;

import java.util.ArrayList;

public class ServerStatusInfo {
    public boolean isOnline;
    public String serverName;
    public ArrayList<PlayerStatusInfo> connectedPlayers;
}
