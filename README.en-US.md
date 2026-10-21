<p align="center">
  <img src="orig_res/banner.jpg" alt="TeleportGUI — Teleportation Mod with GUI for Java and Bedrock players" width="100%">
</p>

<p align="center">
  <b>Homes, warps, and teleport requests — in a proper GUI</b><br>
  Server-side mod · Fabric / NeoForge · Java &amp; Bedrock players
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-26.3+-3C8527?style=flat-square" alt="Minecraft 26.3+">
  <img src="https://img.shields.io/badge/Fabric-supported-DBB69C?style=flat-square" alt="Fabric">
  <img src="https://img.shields.io/badge/NeoForge-supported-EA7B24?style=flat-square" alt="NeoForge">
  <img src="https://img.shields.io/badge/Bedrock-Geyser%20%2B%20Floodgate-4C9EEB?style=flat-square" alt="Bedrock via Geyser and Floodgate">
  <img src="https://img.shields.io/badge/license-MIT-2F81F7?style=flat-square" alt="MIT License">
</p>

<p align="center">
  <a href="README.md">简体中文</a> ·
  <a href="Modrinth.md">Modrinth Description</a> ·
  <a href="CHANGELOG.MD">Change Log</a>
</p>

## Table of Contents

- [Introduction](#introduction)
- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Commands](#commands)
- [GUI](#gui)
- [Configuration](#configuration)
- [Permissions](#permissions)
- [Storage](#storage)
- [Building from Source](#build)
- [Credits](#credits)
- [TODO](#todo)
- [License](#license)

## <a id="introduction"></a>Introduction

TeleportGUI is a server-side Minecraft teleportation mod. It brings homes, global warps, player teleport requests (TPA), death points, spawn points, and return points into Java Edition chest menus and Bedrock Edition forms — while keeping the full command interface available.

- **Java Edition players** can use a vanilla client. Teleport menus are rendered as server-side chest menus and anvil naming screens.
- **Bedrock Edition players** get Cumulus forms when Geyser and Floodgate are installed. Without Floodgate, the mod falls back to hopper menus, so the features remain usable.
- **No client mod required** on dedicated servers. Java players connect with a vanilla client; the mod only needs to be installed on the server.

## <a id="features"></a>Features

### Homes

- Every player has an independent home list with create, go, delete, rename, and set-default actions.
- The first home a player creates automatically becomes their default home; it can be changed with `/home default <name>`.
- Players can create up to 10 homes by default. The limit is configurable and can be disabled.
- Home lists are available as a GUI on both Java and Bedrock.

### Global Warps

- Server-wide warp points with create, go, delete, and rename actions.
- Up to 20 warps by default.
- Deleting and renaming default to permission level **2** and can be changed with `warp.del.permission.level` and `warp.rename.permission.level` in `config.json`; players without the required permission do not see management buttons in the GUI.
- Regular players can still create warps with `/warp create`, subject to the configured permission level and warp limit.
- Warp lists are available as a GUI on both Java and Bedrock.

### Player Teleport Requests (TPA)

- `/tpa <player>`: ask to teleport to another player.
- `/tpahere <player>`: ask another player to teleport to you.
- Requests can be handled through chat buttons, `/tpaaccept`, `/tpadeny`, or the `/gtpa` request list.
- Request expiry is configurable; requests never expire by default.
- Bedrock players can receive an automatic form when a request arrives, or disable it with `/teleportgui be notification_gui false`.

### Teleport Shortcuts

- **Death point**: recorded automatically when a player dies and reachable with `/tpdeathpoint`.
- **Back point**: the position before each teleport is recorded and can be restored with `/back`.
- **World spawn**: `/tpworldspawn` teleports to the Overworld spawn.
- **Personal spawn**: `/tpspawnpoint` teleports to the player's bed or respawn anchor, falling back to world spawn when none is set.

### Safe Teleporting

- When safety checks are enabled, the mod searches for a safe standing position near the target before teleporting.
- If no safe position is found, the teleport is not performed immediately. Instead, the player is asked to confirm:
  - **Java Edition**: click the “Force Teleport” chat button or run `/tpforce`.
  - **Bedrock Edition**: confirm “Cancel / Force Teleport” in a form or the fallback menu.
- Safety checks can be enabled or disabled per command.

### More

- Name tab-completion for home, warp, and TPA commands.
- Clickable coordinates and dimension IDs in Java Edition chat.
- Built-in English, Simplified Chinese, and Traditional Chinese (Hong Kong/Taiwan) language files.
- JSON storage backend.
- Fabric and NeoForge support through a shared Architectury API codebase.

## <a id="requirements"></a>Requirements

| Item                       | Requirement                                                             |
|----------------------------|-------------------------------------------------------------------------|
| Minecraft                  | **26.3+**                                                               |
| Java                       | **25+**                                                                 |
| Fabric                     | Fabric Loader + Fabric API                                              |
| NeoForge                   | NeoForge                                                                |
| Dependency                 | **Architectury API** (required on both loaders)                         |
| Bedrock support (optional) | Geyser + Floodgate; without Floodgate, Bedrock players use hopper menus |

> TeleportGUI is a server-side mod. On a dedicated server, Java players do not need to install anything on their client.

## <a id="installation"></a>Installation

1. Set up a Minecraft 26.3+ Fabric or NeoForge server.
2. Put **Architectury API** in `mods/`. Fabric servers also need **Fabric API**.
3. Put the **TeleportGUI** jar for your loader in `mods/`.
4. (Optional) Install **Geyser** and **Floodgate** to enable Bedrock forms.
5. Start the server. On first start, configuration and data files are generated under `<world directory>/teleportgui/`.

> Download the jar that matches your loader: `TeleportGUI-fabric-<version>.jar` or `TeleportGUI-neoforge-<version>.jar`. Do not install both loader variants at the same time.

## <a id="commands"></a>Commands

Permission levels use the vanilla command permission scale (0–4) and can be changed in `config.json`. Cooldowns start after a successful action.

### Homes

| Command                                   | Description                         | Default permission | Default cooldown |
|-------------------------------------------|-------------------------------------|--------------------|------------------|
| `/home`, `/home list`                     | List your homes                     | 0                  | None             |
| `/home add <name>`, `/home create <name>` | Set a home at your current position | 0                  | None             |
| `/home go <name>`                         | Teleport to a home                  | 0                  | 5 s              |
| `/home del <name>`, `/home delete <name>` | Delete a home                       | 0                  | None             |
| `/home rename <old> <new>`                | Rename a home                       | 0                  | None             |
| `/home default <name>`                    | Set your default home               | 0                  | None             |
| `/home gui`, `/ghome`                     | Open the home GUI                   | 0                  | None             |

### Warps

| Command                                   | Description                            | Default permission | Default cooldown |
|-------------------------------------------|----------------------------------------|--------------------|------------------|
| `/warp`, `/warp list`                     | List all warps                         | 0                  | None             |
| `/warp add <name>`, `/warp create <name>` | Create a warp at your current position | 0                  | None             |
| `/warp go <name>`                         | Teleport to a warp                     | 0                  | 5 s              |
| `/warp del <name>`, `/warp delete <name>` | Delete a warp                          | 2                  | None             |
| `/warp rename <old> <new>`                | Rename a warp                          | 2                  | None             |
| `/warp gui`, `/gwarp`                     | Open the warp GUI                      | 0                  | None             |

### Teleport Requests

| Command                                 | Description                           | Default permission | Default cooldown |
|-----------------------------------------|---------------------------------------|--------------------|------------------|
| `/tpa <player>`                         | Ask to teleport to another player     | 0                  | 30 s             |
| `/tpahere <player>`                     | Ask another player to teleport to you | 0                  | 30 s             |
| `/tpaaccept <player>`, `/tpaY <player>` | Accept a request from that player     | 0                  | None             |
| `/tpadeny <player>`, `/tpaN <player>`   | Deny a request from that player       | 0                  | None             |
| `/gtpa`                                 | Open the teleport request GUI         | 0                  | None             |

### Teleport Shortcuts

| Command                  | Description                                      | Safety check |
|--------------------------|--------------------------------------------------|--------------|
| `/back`                  | Return to the position before your last teleport | Enabled      |
| `/tpdeathpoint`, `/tpdp` | Teleport to your death point                     | Enabled      |
| `/tpworldspawn`, `/tpws` | Teleport to the world spawn                      | Enabled      |
| `/tpspawnpoint`, `/tpsp` | Teleport to your personal spawn                  | Disabled     |
| `/tpforce`               | Confirm a pending unsafe teleport                | —            |

### Random Teleport

| Command              | Description                                                 | Default permission | Default cooldown |
|----------------------|-------------------------------------------------------------|--------------------|------------------|
| `/randomtp`, `/rtp`) | Teleport to a random safe location in the current dimension | 0                  | 60 s             |

> `/randomtp` (alias `/rtp`) searches around the player's current position within a default radius of **100–1000 blocks**. The minimum/maximum distance, cooldown, and permission level can be changed under `teleport.random` in `config.json`.

### Mod Settings

| Command                                         | Description                                                   | Permission |
|-------------------------------------------------|---------------------------------------------------------------|------------|
| `/teleportgui be notification_gui`              | Show whether automatic Bedrock forms are enabled for you      | Everyone   |
| `/teleportgui be notification_gui <true/false>` | Toggle automatic Bedrock forms for your incoming TPA requests | Everyone   |
| `/teleportgui config <key>`                     | Show the current value of a key in `config.json`              | Level 4    |
| `/teleportgui config <key> <integer>`           | Change and save a value in `config.json`                      | Level 4    |
| `/teleportgui command <command>`                | Show whether a command group is enabled                       | Level 4    |
| `/teleportgui command <command> <true/false>`   | Enable or disable a command group                             | Level 4    |
| `/teleportgui reload command`                   | Re-apply command toggles and rebuild the command tree         | Level 4    |
| `/teleportgui reload config`                    | Reload `config.json` and rebuild the command tree             | Level 4    |

> Vanilla `/reload` also reloads `config.json` and updates the command tree.

Examples:

```text
/teleportgui be notification_gui
/teleportgui config home.create.maxCount
/teleportgui config home.go.cooldownSeconds 10
/teleportgui config tpa.request.requestExpireSeconds 120
/teleportgui config teleport.random.minDistance 200
/teleportgui command randomtp
/teleportgui command randomtp false
/teleportgui reload config
```

## <a id="gui"></a>GUI

### Java Edition

Java Edition uses server-side chest menus, so no client mod is needed.

| Screen         | Controls                                                                                                                                                  |
|----------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| Home list      | Left click to teleport, right click to set default, `Shift + left click` to delete, press `F` on an entry to rename, nether star to add a home            |
| Warp list      | Left click to teleport, `Shift + left click` to delete (operators), press `F` on an entry to rename (operators), nether star to create a warp (operators) |
| Request screen | In the player list: left click to send a request, right click to ask them to come. In the request list: left click to accept, right click to deny         |
| Navigation     | `[6]` back, `[7]` previous page, `[8]` next page; `[1]` switches between the player list and request list                                                 |

Creating or renaming a location opens an anvil input screen.

### Bedrock Edition

With Geyser and Floodgate installed, Bedrock players use Cumulus forms:

- Pick an entry and an action from dropdowns, then submit the form.
- Creating and renaming use custom forms with a text input.
- Force teleporting uses a “Cancel / Force Teleport” modal.
- Without Floodgate, the mod falls back to hopper-based action menus; functionality is unchanged.

## <a id="configuration"></a>Configuration

All files are stored under `<world directory>/teleportgui/`.

### `settings.yml`

```yaml
runtimePersistence: true
```

> `runtimePersistence` defaults to `true`: cooldowns and pending TPA requests are written to `teleportgui/runtime.json`. When set to `false`, the file is never read or written, and cooldowns/TPA requests exist only in memory and are cleared on restart.  
> The old `logLevel`, `autoMigrate`, and `storage` / `storage.sqlite` settings have been removed and are ignored. TeleportGUI always uses `teleportgui/storage.json`, and an existing `storage.db` is not migrated automatically.

### `config.json`

`config.json` stores per-command cooldowns, permissions, limits, and safety settings. You only need to include the fields you want to override; missing nodes and values are filled with defaults on load.

```json
{
  "commands": {
    "home": true,
    "warp": true,
    "tpa": true,
    "back": true,
    "tpdeathpoint": true,
    "tpworldspawn": true,
    "tpspawnpoint": true,
    "tpforce": true,
    "rtp": true
  },
  "home": {
    "create": { "maxCount": 10, "cooldownSeconds": 0, "permission": { "level": 0 } },
    "go": { "cooldownSeconds": 5, "permission": { "level": 0 } }
  },
  "warp": {
    "create": { "maxCount": 20 },
    "go": { "cooldownSeconds": 5 }
  },
  "tpa": {
    "request": { "cooldownSeconds": 30, "requestExpireSeconds": 0 }
  },
  "teleport": {
    "deathPoint": { "safetyCheck": true },
    "worldSpawn": { "safetyCheck": true },
    "back": { "safetyCheck": true },
    "spawnPoint": { "safetyCheck": false },
    "random": {
      "minDistance": 100,
      "maxDistance": 1000,
      "cooldownSeconds": 60,
      "permission": { "level": 0 }
    }
  }
}
```

Each key in `commands` enables or disables a command group. Every command except `/teleportgui` can be disabled. Aliases follow their main command: disabling `tpa`, for example, also removes `/tpahere`, `/tpaaccept`, `/tpaY`, `/tpadeny`, `/tpaN`, and `/gtpa`. You can also check a group with `/teleportgui command <command>` and change it at runtime with `/teleportgui command <command> <true/false>`.

Common fields:

| Field                  | Description                                                                                               |
|------------------------|-----------------------------------------------------------------------------------------------------------|
| `cooldownSeconds`      | Cooldown in seconds; `0` disables the cooldown                                                            |
| `permission.level`     | Vanilla permission level `0`–`4`; `-1` inherits from `defaults`                                           |
| `maxCount`             | Home/warp limit; `-1` means unlimited                                                                     |
| `requestExpireSeconds` | TPA request expiry in seconds; `0` never expires (set under `tpa.request`; applies to both request types) |
| `safetyCheck`          | Whether safe-position checks are enabled                                                                  |
| `minDistance`          | `/randomtp` minimum horizontal distance in blocks                                                         |
| `maxDistance`          | `/randomtp` maximum horizontal distance in blocks; must be positive for `/randomtp`                       |

> `expireSeconds` is reserved. `maxDistance` is currently used by `/randomtp` only.

## <a id="permissions"></a>Permissions

- Except for the commands noted below, commands default to permission level **0**, available to all players.
- Config-driven permissions use `permission.level` with vanilla levels `0`–`4`.
- `/warp del`, `/warp delete`, and `/warp rename` default to level **2**, controlled by `warp.del.permission.level` and `warp.rename.permission.level`.
- `/teleportgui config`, `/teleportgui command`, and `/teleportgui reload` require level **4**.
- When a player lacks permission, the command is not executed and a message is sent to them.

## <a id="storage"></a>Storage

| Path                       | Contents                                                                  |
|----------------------------|---------------------------------------------------------------------------|
| `teleportgui/settings.yml` | Mod settings                                                              |
| `teleportgui/config.json`  | Command cooldowns, permissions, limits, and safety checks                 |
| `teleportgui/storage.json` | Homes, warps, player preferences, and other data                          |
| `teleportgui/runtime.json` | Cooldowns and pending TPA requests (when `runtimePersistence` is enabled) |

Runtime data:

- **Cooldowns** and **pending TPA requests** are saved to `runtime.json` and survive restarts.
- **Death points** and **back points** are kept in memory and are cleared when the server restarts.
- Back up the whole `teleportgui/` directory before upgrading.

## <a id="build"></a>Building from Source

You need JDK 25 and the Gradle wrapper:

```bash
git clone https://github.com/ZCT-Studio/TeleportGUI.git
cd TeleportGUI

# Fabric
./gradlew :26.3-fabric:build

# NeoForge
./gradlew :26.3-neoforge:build
```

On Windows, use `gradlew.bat`. Build outputs:

```text
versions/26.3-fabric/build/libs/TeleportGUI-fabric-<version>.jar
versions/26.3-neoforge/build/libs/TeleportGUI-neoforge-<version>.jar
```

The project uses Stonecutter for multi-loader sources and Stonecraft for loader metadata.

## <a id="credits"></a>Credits

Thanks to everyone who helped with this project. See [ACKNOWLEDGMENTS.MD](ACKNOWLEDGMENTS.MD) for the full list.

## <a id="todo"></a>TODO

- [TODO.MD](TODO.MD)

## <a id="license"></a>License

Licensed under the [MIT License](LICENSE).

***TeleportGUI is not an official product of, endorsed by, or affiliated with Mojang Studios, Microsoft, GeyserMC, or Floodgate.***
