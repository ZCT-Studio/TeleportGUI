# TeleportGUI

TeleportGUI is a server-side teleportation mod for Minecraft 26.3 that gives Java and Bedrock players a proper graphical interface for homes, global warps, and player teleport requests (TPA). It also adds death-point, spawn-point, and return-point teleports, with configurable safety checks and permissions.

The mod runs on the server. Java players can join with a vanilla client; no client-side mod is required. Bedrock players get native form menus when Geyser and Floodgate are installed.

## Features

- **Homes** — create, teleport to, list, delete, rename, and set a default home. Each player has an independent home list.
- **Global warps** — server-wide teleport points with operator-controlled deletion and renaming.
- **TPA requests** — `/tpa` and `/tpahere`, with clickable accept/deny actions, request expiry, and a request-list GUI.
- **Teleport shortcuts** — death point, world spawn, personal spawn, and `/back` to the previous position.
- **Safe teleporting** — when enabled, searches for a safe standing position and asks for confirmation before forcing an unsafe teleport.
- **Java GUI** — server-side chest menus with pagination and anvil naming screens.
- **Bedrock forms** — dropdown-based Cumulus forms for homes, warps, and TPA; automatic fallback to hopper menus when Floodgate is unavailable.
- **Configurable** — per-command cooldowns, permission levels, home/warp limits, TPA expiry, and safety checks.
- **Storage** — JSON or SQLite backend.
- **Localization** — English and Simplified Chinese included.

## Requirements

- **Architectury API** on both Fabric and NeoForge.
- **Fabric API** on Fabric servers.
- **Geyser + Floodgate** are optional and only needed for the Bedrock form interface. They are separate projects and are not bundled with TeleportGUI.

## Installation

1. Install a Minecraft 26.3 Fabric or NeoForge server.
2. Add Architectury API to the server's `mods/` folder. On Fabric, add Fabric API as well.
3. Add the TeleportGUI jar that matches your loader to `mods/`.
4. Optionally add Geyser and Floodgate for Bedrock forms.
5. Start the server. Configuration and data files are created under `<world directory>/teleportgui/`.

Do not install the Fabric and NeoForge jars at the same time.

## Commands

Permission levels use the vanilla 0–4 scale. Defaults are shown below. Cooldowns and configurable permission levels can be changed in `config.json`; the level 4 requirements noted below are always enforced.

### Homes

| Command | Description | Permission |
| --- | --- | --- |
| `/home`, `/home list` | List your homes | 0 |
| `/home add <name>`, `/home create <name>` | Set a home at your position | 0 |
| `/home go <name>` | Teleport to a home | 0 |
| `/home del <name>`, `/home delete <name>` | Delete a home | 0 |
| `/home rename <old> <new>` | Rename a home | 0 |
| `/home default <name>` | Set your default home | 0 |
| `/home gui`, `/ghome` | Open the home GUI | 0 |

### Warps

| Command | Description | Permission |
| --- | --- | --- |
| `/warp`, `/warp list` | List all warps | 0 |
| `/warp add <name>`, `/warp create <name>` | Create a warp at your position | 0 |
| `/warp go <name>` | Teleport to a warp | 0 |
| `/warp del <name>`, `/warp delete <name>` | Delete a warp | 4 |
| `/warp rename <old> <new>` | Rename a warp | 4 |
| `/warp gui`, `/gwarp` | Open the warp GUI | 0 |

Deleting and renaming warps always require permission level 4.

### Teleport Requests

| Command | Description | Permission |
| --- | --- | --- |
| `/tpa <player>` | Ask to teleport to another player | 0 |
| `/tpahere <player>` | Ask another player to teleport to you | 0 |
| `/tpaaccept <player>`, `/tpaY <player>` | Accept a request | 0 |
| `/tpadeny <player>`, `/tpaN <player>` | Deny a request | 0 |
| `/gtpa` | Open the request GUI | 0 |

TPA requests are kept in memory and are cleared when the server restarts.

### Teleport Shortcuts

| Command | Description |
| --- | --- |
| `/back` | Return to the position before your last teleport |
| `/tpdeathpoint`, `/tpdp` | Teleport to your death point |
| `/tpworldspawn`, `/tpws` | Teleport to the world spawn |
| `/tpspawnpoint`, `/tpsp` | Teleport to your personal spawn |
| `/tpforce` | Confirm a pending unsafe teleport |

Death points and back points are kept in memory and are cleared when the server restarts.

### Mod Settings

| Command | Description | Permission |
| --- | --- | --- |
| `/teleportgui be notification_gui <true/false>` | Toggle automatic Bedrock forms for your incoming TPA requests | Everyone |
| `/teleportgui config <key> <integer>` | Change and save a value in `config.json` | 4 |

Example:

```text
/teleportgui config home.go.cooldownSeconds 10
```

## GUI

### Java Edition

- Home list: left click to teleport, right click to set default, `Shift + left click` to delete, `F` to rename, nether star to add a home.
- Warp list: left click to teleport, `Shift + left click` to delete, `F` to rename, nether star to create a warp. Management actions are operator-only.
- TPA: in the player list, left click to send a request and right click to ask the other player to come. In the request list, left click to accept and right click to deny.
- `[6]` back, `[7]` previous page, `[8]` next page, `[1]` switch between player and request lists.

### Bedrock Edition

With Floodgate installed, homes, warps, and TPA use Cumulus forms. Choose an entry and an action from dropdowns, then submit. Force-teleport confirmation uses a modal form. Without Floodgate, the mod falls back to hopper menus.

## Configuration

Files are created under `<world directory>/teleportgui/`.

`settings.yml` selects the storage backend:

```yaml
storage:
  type: json
```

Use `json` for `storage.json` or `sqlite` for `storage.db`. Changing the backend does not migrate existing data. Back up the `teleportgui/` directory before switching.

`config.json` controls per-command behavior. Missing values are filled with defaults on load. A minimal example:

```json
{
  "home": {
    "create": { "maxCount": 10 },
    "go": { "cooldownSeconds": 5 }
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
    "spawnPoint": { "safetyCheck": false }
  }
}
```

Common fields:

- `cooldownSeconds` — cooldown in seconds; `0` disables it.
- `permission.level` — vanilla permission level `0`–`4`; `-1` inherits from `defaults`.
- `maxCount` — home or warp limit; `-1` means unlimited.
- `requestExpireSeconds` — TPA request expiry; `0` never expires. Set under `tpa.request`; applies to both request types.
- `safetyCheck` — enable or disable safe-position checks.

`maxDistance` and `expireSeconds` are reserved fields and are not currently used by teleport checks.

## Permissions

- Most commands default to permission level 0.
- Config-driven permissions are set with `permission.level`.
- Warp deletion and renaming, and `/teleportgui config`, always require level 4.
- Players without permission receive a message and the command is not executed.

## Storage

- `teleportgui/settings.yml` — mod settings and storage backend.
- `teleportgui/config.json` — command cooldowns, permissions, limits, and safety checks.
- `teleportgui/storage.json` — JSON backend data.
- `teleportgui/storage.db` — SQLite backend data.

Homes, warps, and Bedrock notification preferences persist across restarts. Death points, back points, and pending TPA requests do not.

## TODO

- [TODO.MD](https://github.com/ZCT-Studio/TeleportGUI/blob/master/TODO.MD)

## License

TeleportGUI is licensed under the [MIT License](https://github.com/ZCT-Studio/TeleportGUI/blob/master/LICENSE).

***TeleportGUI is not an official product of, endorsed by, or affiliated with Mojang Studios, Microsoft, GeyserMC, or Floodgate.***

## Links

- Changelog: [CHANGELOG.MD](https://github.com/ZCT-Studio/TeleportGUI/blob/master/CHANGELOG.MD)
- Acknowledgments: [ACKNOWLEDGMENTS.MD](https://github.com/ZCT-Studio/TeleportGUI/blob/master/ACKNOWLEDGMENTS.MD)
