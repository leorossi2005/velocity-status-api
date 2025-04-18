# Status API for Velocity Minecraft servers

### Usage
After installing the plugin and starting the server, a config file will be generated under `/plugins/statusapi/config.yml`. You can configure the bind address and port here (defaults to 0.0.0.0:8081)
You can access the API from this port, and see server information in either readable text or JSON formats at `/text` or `/json` respectively.

### Motivations
reddust9: This was created as a way to provide a minimal information webpage about a Minecraft server, and also to practice writing server plugins for me.

leorossi05: I wanted to upgrade the plugin to be more expansive and complete, now is easier to obtain any info from the proxy and the connected servers, I hope it'll be helpful.

### Response example - Text

```
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
```

### Response example - JSON

```json
{
  "serverUptime": 13406,
  "version": "3.4.0-SNAPSHOT",
  "motd": {
    "raw": "§3I'm §3a §3velocity §3server",
    "clean": "I'm a velocity server"
  },
  "icon": "data:image/png;base64,/9j/4AAQSkZJRgABAgAAAQABAAD/2wBDAAg...",
  "maxPlayers": 100,
  "onlinePlayers": 3,
  "connectedPlayers": [
    {
      "name": "reddust9",
      "uuid": "6640ecc2-69cb-4c46-9485-29874db29c83",
      "client": "vanilla",
      "server": "lobby",
      "ping": 130
    },
    {
      "name": "leorossi05",
      "uuid": "2d1064ef-b998-4c89-aacb-e366f30c5016",
      "client": "vanilla",
      "server": "lobby",
      "ping": 280
    },
    {
      "name": "anotherguy",
      "uuid": "94c4c50a-0790-4b52-b6fa-859907c5f125",
      "client": "vanilla",
      "server": "smp",
      "ping": 200
    }
  ],
  "connectedServers": [
    {
      "isOnline": true,
      "serverName": "lobby",
      "version": "1.21.4",
      "motd": {
        "raw": "Lobby server",
        "clean": "Lobby server"
      },
      "maxPlayers": 34,
      "onlinePlayers": 2,
      "connectedPlayers": [
        {
          "name": "reddust9",
          "uuid": "6640ecc2-69cb-4c46-9485-29874db29c83",
          "client": "vanilla",
          "server": "lobby",
          "ping": 130
        },
        {
          "name": "leorossi05",
          "uuid": "2d1064ef-b998-4c89-aacb-e366f30c5016",
          "client": "vanilla",
          "server": "lobby",
          "ping": 280
        }
      ]
    },
    {
      "isOnline": true,
      "serverName": "smp",
      "version": "1.21.4",
      "motd": {
        "raw": "I'm an smp server",
        "clean": "I'm an smp server"
      },
      "maxPlayers": 33,
      "onlinePlayers": 1,
      "connectedPlayers": [
        {
          "name": "anotherguy",
          "uuid": "94c4c50a-0790-4b52-b6fa-859907c5f125",
          "client": "vanilla",
          "server": "smp",
          "ping": 200
        }
      ]
    },
    {
      "isOnline": false,
      "serverName": "bedwars"
    }
  ]
}
```