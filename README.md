# Status API for Velocity Minecraft servers

### Usage
After installing the plugin and starting the server, a config file will be generated under `/plugins/statusapi/config.json`. You can configure the bind address and port here (defaults to 0.0.0.0:8081)
You can access the API from this port, and see server information in either readable text or JSON formats at `/text` or `/json` respectively.

### Motivations
This was created as a way to provide a minimal information webpage about a Minecraft server, and also to practice writing server plugins for me.

### Response example - Text

```
=== SERVER STATUS ===
Uptime: 111

Connected servers:

        ==> lobby (online)
Players:
        - reddust9 on vanilla [120 ping]
        - someoneelse on vanilla [250 ping]

        ==> smp (online)
Players:
        - anotherguy on vanilla [200 ping]

        ==> bedwars (offline)
Players:
```

### Response example - JSON

```json
{
  "serverUptime": 753457,
  "connectedServers": [
    {
      "isOnline": true,
      "serverName": "lobby",
      "connectedPlayers": [
        {
          "name": "reddust9",
          "uuid": "70c1712d-f203-4062-a9bb-9f437d018bab",
          "client": "vanilla",
          "ping": 120
        }
      ]
    },
    {
      "isOnline": true,
      "serverName": "smp",
      "connectedPlayers": []
    },
    {
      "isOnline": false,
      "serverName": "bedwars",
      "connectedPlayers": []
    }
  ]
}
```
