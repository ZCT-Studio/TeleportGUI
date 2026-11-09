# Agent Instructions

## Project Overview

TeleportGUI is a Minecraft mod for Fabric and NeoForge that provides graphical teleportation interfaces, including TPA, Home, and Warp, with optional Geyser integration for Bedrock Edition players.

- **Language:** Java 21+, Gradle Kotlin DSL
- **Mod Loaders:** Fabric (primary), NeoForge
- **Build Tools:** Gradle, Stonecutter, Architectury, Loom
- **Version Information:** Refer to the project's Gradle properties and version configuration files.

Do not assume that hardcoded version information in documentation is up to date. Always consult the relevant project configuration before making version-dependent changes.

## Project Structure

- `main/`: Shared code and resources.
- `fabric/`: Fabric-specific implementations.
- `neoforge/`: NeoForge-specific implementations.
- `versions/`: Version-specific dependency configurations.
- `libs/`: Local dependency JARs.

### Development Guidelines

- Prefer shared implementations in `main/` whenever possible.
- Keep loader-specific code isolated to the appropriate module.
- Avoid duplicating logic between Fabric and NeoForge.
- Preserve compatibility with both supported loaders.
- Follow existing package structure, naming conventions, and coding style.
- Avoid unrelated refactoring when implementing a focused change.
- Do not introduce new required dependencies without justification.
- Do not modify generated files unless explicitly necessary.

## Build System

The project uses Gradle Kotlin DSL with Stonecutter and Architectury.

- **Gradle:** Build orchestration and dependency management.
- **Stonecutter:** Minecraft version management and conditional source processing.
- **Architectury:** Cross-loader abstractions and shared APIs.
- **Loom:** Minecraft development tooling.

### Key Gradle Files

- `build.gradle.kts`: Root build configuration.
- `settings.gradle.kts`: Project and module configuration.
- `versions/dependencies/`: Version-specific dependency properties.

Consult the actual Gradle configuration before modifying dependencies, modules, or build tasks.

### Optional Dependencies

Geyser integration is optional and must not prevent the mod from loading when Geyser is absent.

Relevant dependency:

```text
org.geysermc.geyser:geyser-api
```

The project may resolve local dependencies from `libs/`.

- Do not assume Geyser or Floodgate is installed.
- Keep optional integration code isolated.
- Avoid introducing unconditional references that could cause class-loading failures.
- Preserve existing runtime compatibility checks.

## Geyser Integration

### Emote Trigger System

The Geyser emote trigger system allows Bedrock Edition players to open TeleportGUI through configured emotes.

Relevant components:

1. `ServerMain.initGeyserBridge()`: Initializes the bridge during server startup.
2. `GeyserApiBridge.tryInit()`: Attempts to initialize the optional Geyser integration.
3. `ClientEmoteEvent`: Receives Bedrock Edition emote events.
4. `GeyserEmoteMenu.openMenu()`: Opens the Cumulus form.

Verify the current implementation before modifying these components.

### Configuration

Configuration file:

```text
config/teleportgui/settings.yml
```

Example:

```yaml
runtimePersistence: true
emoteTriggerUuids:
  - "ce5c0300-7f03-455d-aaf1-352e4927b54d"
```

- The UUID above represents the "Over There!" emote.
- An empty `emoteTriggerUuids` list disables emote-triggered menus.
- Matching emotes trigger the configured menu and cancel the corresponding default behavior.

### Per-Player Settings

- Command: `/teleportgui_be emote_ui <true|false>`
- Available to Bedrock Edition players.
- State: `PlayerData.emoteUiEnabled`
- Default: `true`

Preserve existing persistence and player-specific behavior.

### Implementation Requirements

**Thread Safety**

- Geyser event callbacks may execute outside the Minecraft server thread.
- Schedule Minecraft server state operations using `server.execute(Runnable)`.
- Do not access or mutate non-thread-safe server state directly from asynchronous callbacks.

**Cooldown**

- Preserve the existing per-player cooldown mechanism.
- The current emote menu cooldown is 1.5 seconds.
- Ensure player-specific temporary state is cleaned up appropriately.

**Cumulus Forms**

- Call `validResultHandler()` before `build()` where required by the existing implementation.
- Preserve form callback behavior and error handling.
- Avoid opening duplicate forms.

**Event Cancellation**

- Use `ClientEmoteEvent.setCancelled(true)` when the configured trigger requires cancellation.
- Avoid cancelling unrelated emote events.

**Internationalization**

- Reuse existing translation keys whenever possible.
- Emote GUI translation keys use the prefix `gui.teleportgui.be.emoteGUI.*`.
- Do not hardcode user-facing text when a translation key is appropriate.
- Preserve existing translations when modifying interface behavior.

## Building and Testing

Use the Gradle wrapper provided by the repository.

```bash
# Compile supported modules
./gradlew compileJava --no-daemon

# Run Fabric server
./gradlew :26.3-fabric:runServer --no-daemon

# Run NeoForge server
./gradlew :26.3-neoforge:runServer --no-daemon
```

The commands above reflect the current project layout. Verify available Gradle tasks before execution.

### Validation Guidelines

- Run relevant compilation or tests after modifying code.
- Validate both Fabric and NeoForge when changing shared code.
- Test optional Geyser behavior when modifying its integration.
- Verify that the mod remains functional without Geyser.
- Check for missing translations when changing user-facing interfaces.
- Report any tests that could not be executed.
- Do not claim successful validation without actual execution.

### Expected Startup Log

When the Geyser emote bridge initializes successfully, a log similar to the following may appear:

```text
[Server thread/INFO] (TeleportGUI) Geyser emote bridge initialized (trigger: 1)
```

The trigger count depends on the configuration.

### Development Environment

If a test world is locked, identify the process holding the lock before taking action.

- Prefer stopping the relevant Gradle task or server process normally.
- Do not terminate unrelated Java processes.
- Do not use broad process termination commands without explicit authorization.

## Changelog

The [`CHANGELOG.md`](CHANGELOG.md) file follows a bilingual format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

### Format Requirements

- Use bilingual section headings, such as `Added / 新增` and `Fixed / 修复`.
- Preserve the existing version heading structure.
- Each changelog entry must contain exactly two language lines: English first, Simplified Chinese second.
- Separate the languages using `<br>` at the beginning of the second line.
- Use `[TODO]` as the exact placeholder for a missing translation.
- Regular development commits may contain `[TODO]` placeholders.
- Both languages must convey the same meaning when completed.
- Do not use `[TODO]` for both languages in the same entry.
- Keep changelog entries focused on user-visible changes rather than internal implementation details.

### Examples

**Both languages available:**

```markdown
- Added...
<br> 新增...
```

**English only:**

```markdown
- Added...
<br> [TODO]
```

**Simplified Chinese only:**

```markdown
- [TODO]
<br> 新增...
```

**Invalid:**

```markdown
- [TODO]
<br> [TODO]
```

### Version Bump Requirements

When updating the project version:

- Complete all missing translations in the changelog entries included in the upcoming release.
- Do not leave `[TODO]` placeholders in the release's changelog entries.
- Ensure the release version matches the project's version configuration.
- Preserve the changelog structure expected by the release CI.
- Do not modify previously released changelog entries unless correcting an error.
- Do not create or move Git tags unless explicitly requested.

### Release Integration

The release CI extracts version sections from `CHANGELOG.md`.

- Keep version headings at Markdown level 2 (`##`).
- Do not wrap version headings in blockquotes.
- Preserve the existing release extraction format.
- Keep Issue and PR references compatible with the release workflow.
- Do not introduce formatting that breaks automated changelog extraction.

### Changelog Archives

Older changelogs are archived in [`changelogs/`](changelogs), organized by major and minor versions.

- Use `changelogs/v<major>/<major>.<minor>.md` for archived changelogs.
    - Example: `changelogs/v1/1.0.md`.
- Keep the current development series in `CHANGELOG.md`.
- Archive a version series only after its maintenance has ended.
- Preserve the original version numbers, release dates, and changelog entries when archiving.
- Maintain the same bilingual format in archived changelogs.
- Do not duplicate archived entries in `CHANGELOG.md`.
- Do not modify archived changelogs unless correcting errors.
- Update archive references when moving changelog entries.
- Ensure changelog archiving does not break CI release note extraction.

## General Agent Guidelines

- Read relevant code and configuration before making changes.
- Prefer minimal, targeted modifications.
- Preserve existing behavior unless a change is explicitly required.
- Avoid introducing unnecessary dependencies.
- Maintain compatibility across supported Minecraft versions and mod loaders.
- Do not modify unrelated files without a clear reason.
- Run appropriate validation whenever feasible.
- Clearly report changes made, tests performed, and any remaining limitations.
