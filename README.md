# MCSS

[![RU](https://img.shields.io/badge/README-Русский-blue)](README.ru.md)

A simple Minecraft mod that connects your world to an external service via WebSocket.
In short: place the "Hub" block, the mod connects to your backend and
sends/receives data for your security system.

## What the mod does

- connects Minecraft to an external service over WebSocket;
- adds blocks: Hub, Hub Extension, and Reader;
- keeps a stable hub identifier (`hubId`) for backend linkage;
- provides simple configuration via `config/hubmod.yml`.

## Compatibility

- Minecraft: 1.21.11
- Fabric Loader: >= 0.18.2
- Fabric API: any recent version

## Installation

1) Install Fabric Loader and Fabric API.
2) Put the mod `.jar` into the `mods` folder.
3) Launch the game/server once to generate configs.

## Quick start

1) Start the world once so configs are created.
2) Open `config/hubmod.yml` and set your WebSocket server address.
3) Restart the game/server.
4) In Creative, open the "Hub" item tab and place the blocks.

## Configuration

File: `config/hubmod.yml`

```yaml
# Enable extra logging
debug: false

# WebSocket server address
wsUrl: "ws://127.0.0.1:8080"

# Connection test intervals (milliseconds)
testPeriodMs: 300000
testFailAfterMs: 300000
```

File: `config/hubmod.json`

- contains `hubId`, created automatically on first launch;
- this identifier links your world to the backend.

## Blocks

- **Hub** — main block that connects the world to the backend.
- **Hub Extension** — extra block to extend the hub.
- **Reader** — reader block for interaction with the system.

## Build from source

```sh
./gradlew build
```

## License

This project uses the license in `LICENSE`.
