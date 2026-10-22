# TeleportGUI

TeleportGUI is a server-side teleportation mod for Minecraft 26.3+ that gives Java and Bedrock players a proper
graphical interface for homes, global warps, and player teleport requests (TPA). It also adds death-point, spawn-point,
and return-point teleports, with configurable safety checks and permissions.

The mod runs on the server. Java players can join with a vanilla client; no client-side mod is required. Bedrock players
get native form menus when Geyser and Floodgate are installed.

## Features

- **Homes** — create, teleport to, list, delete, rename, and set a default home. Each player has an independent home
  list.
- **Global warps** — server-wide teleport points with permission-controlled creation, deletion, and renaming.
- **TPA requests** — `/tpa` and `/tpahere`, with clickable accept/deny actions, request expiry, and a request-list GUI.
- **Teleport shortcuts** — death point, world spawn, personal spawn, and `/back` to the previous position.
- **Safe teleporting** — when enabled, searches for a safe standing position and asks for confirmation before forcing an
  unsafe teleport.
- **Java GUI** — server-side chest menus with pagination and anvil naming screens.
- **Bedrock forms** — dropdown-based Cumulus forms for homes, warps, and TPA; automatic fallback to hopper menus when
  Floodgate is unavailable.
- **Configurable** — per-command cooldowns, permission levels, home/warp limits, TPA expiry, and safety checks.
- **Storage** — JSON backend.
- **Localization** — English, Simplified Chinese, and Traditional Chinese (Hong Kong/Taiwan) included.

## Requirements

- **Architectury API** on both Fabric and NeoForge.
- **Fabric API** on Fabric servers.
- **Geyser + Floodgate** are optional and only needed for the Bedrock form interface. They are separate projects and are
  not bundled with TeleportGUI.

## Installation

1. Install a Minecraft 26.3+ Fabric or NeoForge server.
2. Add Architectury API to the server's `mods/` folder. On Fabric, add Fabric API as well.
3. Add the TeleportGUI jar that matches your loader to `mods/`.
4. Optionally add Geyser and Floodgate for Bedrock forms.
5. Start the server. Configuration and data files are created under `<world directory>/teleportgui/`.

Do not install the Fabric and NeoForge jars at the same time.

## Commands

Permission levels use the vanilla 0–4 scale. Defaults are shown below. Cooldowns and configurable permission levels can
be changed in `config.json`; the level 4 requirements noted below are always enforced.

### Homes

| Command                                   | Description                 | Permission |
|-------------------------------------------|-----------------------------|------------|
| `/home`, `/home list`                     | List your homes             | 0          |
| `/home add <name>`, `/home create <name>` | Set a home at your position | 0          |
| `/home go <name>`                         | Teleport to a home          | 0          |
| `/home del <name>`, `/home delete <name>` | Delete a home               | 0          |
| `/home rename <old> <new>`                | Rename a home               | 0          |
| `/home default <name>`                    | Set your default home       | 0          |
| `/home gui`, `/ghome`                     | Open the home GUI           | 0          |

### Warps

| Command                                   | Description                    | Permission |
|-------------------------------------------|--------------------------------|------------|
| `/warp`, `/warp list`                     | List all warps                 | 0          |
| `/warp add <name>`, `/warp create <name>` | Create a warp at your position | 2          |
| `/warp go <name>`                         | Teleport to a warp             | 0          |
| `/warp del <name>`, `/warp delete <name>` | Delete a warp                  | 2          |
| `/warp rename <old> <new>`                | Rename a warp                  | 2          |
| `/warp gui`, `/gwarp`                     | Open the warp GUI              | 0          |

### Teleport Requests

| Command                                 | Description                           | Permission |
|-----------------------------------------|---------------------------------------|------------|
| `/tpa <player>`                         | Ask to teleport to another player     | 0          |
| `/tpahere <player>`                     | Ask another player to teleport to you | 0          |
| `/tpaaccept <player>`, `/tpay <player>` | Accept a request                      | 0          |
| `/tpadeny <player>`, `/tpan <player>`   | Deny a request                        | 0          |
| `/gtpa`                                 | Open the request GUI                  | 0          |

> TPA requests expire after 120 seconds by default and are persisted across restarts while `runtimePersistence` is
> enabled.

### Teleport Shortcuts

| Command                  | Description                                                 | Permission |
|--------------------------|-------------------------------------------------------------|------------|
| `/back`                  | Return to the position before your last teleport            | 0          |
| `/tpdeathpoint`, `/tpdp` | Teleport to your death point                                | 0          |
| `/tpworldspawn`, `/tpws` | Teleport to the world spawn                                 | 0          |
| `/tpspawnpoint`, `/tpsp` | Teleport to your personal spawn                             | 0          |
| `/tpforce`               | Confirm a pending unsafe teleport                           | Inherited  |
| `/randomtp`, `/rtp`      | Teleport to a random safe location in the current dimension | 0          |

> Shortcut permission levels can be changed under `teleport.back`, `teleport.deathPoint`, `teleport.worldSpawn`, and
`teleport.spawnPoint` in `config.json`. `/tpforce` has no separate permission and is only used when a command's safety
> check produces a pending confirmation.

> Death points and back points are kept in memory and are cleared when the server restarts.

### Player Settings

| Command                                   | Description                                                     | Permission |
|-------------------------------------------|-----------------------------------------------------------------|------------|
| `/teleportgui allow_request`              | Show whether you accept incoming teleport requests              | Everyone   |
| `/teleportgui allow_request <true/false>` | Allow or block other players from sending you teleport requests | Everyone   |
| `/teleportgui_be notif_gui`               | Show whether automatic Bedrock forms are enabled for you        | Everyone   |
| `/teleportgui_be notif_gui <true/false>`  | Toggle automatic Bedrock forms for your incoming TPA requests   | Everyone   |

### Admin Settings

`/teleportgui_admin` is only visible and executable to permission level **4**.

| Command                                             | Description                                           | Permission |
|-----------------------------------------------------|-------------------------------------------------------|------------|
| `/teleportgui_admin config <key>`                   | Show the current value of a key in `config.json`      | 4          |
| `/teleportgui_admin config <key> <value>`           | Change and save a value in `config.json`              | 4          |
| `/teleportgui_admin command <command>`              | Show whether a command group is enabled               | 4          |
| `/teleportgui_admin command <command> <true/false>` | Enable or disable a command group                     | 4          |
| `/teleportgui_admin reload command`                 | Re-apply command toggles and rebuild the command tree | 4          |
| `/teleportgui_admin reload config`                  | Reload `config.json` and rebuild the command tree     | 4          |

> `teleportgui`, `teleportgui_admin`, and `teleportgui_be` cannot be disabled by command toggles.
>
> `notifGuiDefault` is the global default; a player's own `/teleportgui_be notif_gui` setting takes priority.

Example:

```text
/teleportgui allow_request
/teleportgui allow_request false
/teleportgui_be notif_gui
/teleportgui_admin config home.create.maxCount
/teleportgui_admin config notifGuiDefault true
/teleportgui_admin config home.go.cooldownSeconds 10
/teleportgui_admin command back
/teleportgui_admin command back false
/teleportgui_admin reload config
```

## GUI

### Java Edition

- Home list: left click to teleport, right click to set default, `Shift + left click` to delete, `F` to rename, nether
  star to add a home.
- Warp list: left click to teleport, `Shift + left click` to delete, `F` to rename, nether star to create a warp.
  Creation, deletion, and renaming are permission-gated (level 2 by default).
- TPA: in the player list, left click to send a request and right click to ask the other player to come. In the request
  list, left click to accept and right click to deny.
- `[6]` back, `[7]` previous page, `[8]` next page, `[1]` switch between player and request lists.

### Bedrock Edition

With Floodgate installed, homes, warps, and TPA use Cumulus forms. Choose an entry and an action from dropdowns, then
submit. Force-teleport confirmation uses a modal form. Without Floodgate, the mod falls back to hopper menus.

## Configuration

Files are created under `<world directory>/teleportgui/`.

`settings.yml` contains general mod settings:

```yaml
runtimePersistence: true
```

`runtimePersistence` defaults to `true`; set it to `false` to keep cooldowns and pending TPA requests in memory only and
never read or write `runtime.json`.

TeleportGUI always uses `storage.json`. The old `logLevel`, `autoMigrate`, and `storage` / `storage.sqlite` settings
have been removed and are ignored; an existing `storage.db` is not migrated automatically.

`config.json` controls per-command behavior. Missing values are filled with defaults on load. A minimal example:

```json
{
  "notifGuiDefault": true,
  "home": {
    "create": {
      "maxCount": 10
    },
    "go": {
      "cooldownSeconds": 5
    }
  },
  "warp": {
    "create": {
      "maxCount": 20
    },
    "go": {
      "cooldownSeconds": 5
    }
  },
  "tpa": {
    "request": {
      "cooldownSeconds": 30,
      "expireSeconds": 120
    },
    "requestHere": {
      "cooldownSeconds": 30,
      "expireSeconds": 120
    }
  },
  "teleport": {
    "deathPoint": {
      "safetyCheck": true
    },
    "worldSpawn": {
      "safetyCheck": true
    },
    "back": {
      "safetyCheck": true
    },
    "spawnPoint": {
      "safetyCheck": false
    }
  }
}
```

Common fields:

- `cooldownSeconds` — cooldown in seconds; `0` disables it.
- `permission.level` — vanilla permission level `0`–`4`; `-1` inherits from `defaults`.
- `maxCount` — home or warp limit; `-1` means unlimited.
- `expireSeconds` — expiry in seconds; `0` never expires. Set under `tpa.request` (`/tpa`) and `tpa.requestHere` (
  `/tpahere`) separately; default `120`. Reserved on other nodes.
- `safetyCheck` — enable or disable safe-position checks.

`maxDistance` is used by `/randomtp`; `minDistance` and `maxDistance` are reserved elsewhere.

## Permissions

- Most commands default to permission level 0.
- Config-driven permissions are set with `permission.level`.
- Warp creation, deletion, and renaming default to level 2 and are controlled by `warp.create.permission.level`,
  `warp.del.permission.level`, and `warp.rename.permission.level`; `/teleportgui_admin` is only visible and executable
  to level 4.
- Players without permission receive a message and the command is not executed.

## Storage

- `teleportgui/settings.yml` — mod settings.
- `teleportgui/config.json` — command cooldowns, permissions, limits, and safety checks.
- `teleportgui/storage.json` — homes, warps, and player preferences.
- `teleportgui/runtime.json` — cooldowns and pending TPA requests (when `runtimePersistence` is enabled).

Homes, warps, Bedrock notification preferences, cooldowns, and pending TPA requests persist across restarts. Death
points and back points do not.

## TODO

- [TODO in GitHub](https://github.com/ZCT-Studio/TeleportGUI/blob/master/todo.md)

## License

TeleportGUI is licensed under the [MIT License](https://github.com/ZCT-Studio/TeleportGUI/blob/master/LICENSE).

***TeleportGUI is not an official product of, endorsed by, or affiliated with Mojang Studios, Microsoft, GeyserMC, or
Floodgate.***

## Links

- [ChangeLog in GitHub](https://github.com/ZCT-Studio/TeleportGUI/blob/master/changelog.md)
- [Acknowledgments in GitHub](https://github.com/ZCT-Studio/TeleportGUI/blob/master/acknowledgments.md)
