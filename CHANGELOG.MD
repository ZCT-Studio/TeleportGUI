# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.1-rc1] - 2026-10-20

### Fixed

- Teleporting to a home, warp, death point, or player standing on a non-full block (such as a path, farmland, or slab) no longer places the player below that block. Saved positions now round the Y coordinate, and teleport targets are lifted out of any occupied block, including positions saved by older versions.

## [1.0.0-rc.2] - 2026-10-20

### Added

- Add authors support

### Removed

- Removed the extra information expressed in the language file
- Remove Fabric Banner

## [1.0.0-rc.1] - 2026-10-20

### Added

- Added full docs
- Added `/tpaY` and `/tpaN` as aliases for accepting and denying teleport requests.
- Added a mod icon and a mod banner.
- Added the active loader name to the startup log.
- The mod version is now read from loader metadata at runtime instead of being hard-coded in a Java constant.

### Changed

- Consolidated loader-specific sources into `src/fabric` and `src/neoforge`, removing the per-version source copies.
- Reworked Bedrock form interactions, including a cancel/force modal for forced teleports and adjusted button order.
- Unified button styling and moved shared styles into constants.
- Swapped the Java Edition home/warp list mouse actions: right-click now sets the default home, and `Shift + left-click` deletes.
- Normalized mod metadata, dependency declarations, and version configuration; Fabric now declares Architectury as a dependency.
- Updated the NeoForge version and fixed the NeoForge build.
- Updated the mod description.

### Fixed

- Fixed misaligned placeholders in chat messages.
- Fixed Bedrock form sending failures.
- Fixed incorrect dropdown and input indices when reading Bedrock home/warp forms.
- Fixed deprecated click commands in home/warp lists and added support for names containing quotes.
- Removed duplicate command hints.
- Removed an extra blank line when returning the feather item in the anvil rename/create screens.
- Fixed the Fabric dependency list missing Architectury.
- Removed Fabric/NeoForge entries from `.gitignore` so loader configuration files are tracked.

### Removed

- Removed unused parameters, methods, and the unused migration class.
- Removed the rename action from interactive chat list entries.

## [1.0.0-alpha.1] - 2026-10-20

### Added

- Initial release with Fabric and NeoForge support for Minecraft 26.3 through Architectury.
- Homes: create, go, list, delete, rename, default home, and the `/ghome` GUI.
- Global warps: create, go, list, delete, rename, and the `/gwarp` GUI.
- TPA: `/tpa`, `/tpahere`, `/tpaaccept`, `/tpadeny`, and the `/gtpa` request GUI.
- Teleport shortcuts: `/back`, `/tpdeathpoint` (`/tpdp`), `/tpworldspawn` (`/tpws`), and `/tpspawnpoint` (`/tpsp`).
- Automatic death-point recording.
- Java Edition chest-menu GUIs with pagination and anvil naming screens.
- Bedrock Edition forms through Geyser/Floodgate, with a hopper-menu fallback.
- Safe-position checks and forced teleport confirmation through `/tpforce`.
- Per-command cooldowns, permission levels, limits, TPA expiry, and safety checks through `config.json`.
- JSON and SQLite storage backends with `settings.yml`.
- English and Simplified Chinese localization.

### Changed

- Hid the warp add button from players without permission.
- Distinguished default homes (red bed) from normal homes (white bed), and changed the add icon to a nether star.

### Fixed

- Fixed preview items in the Java GUI being movable to the offhand and usable in creative mode.
- Fixed TPA accept/deny result messages being sent to the wrong player.
