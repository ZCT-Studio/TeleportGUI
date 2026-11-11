# Changelog / 更新日志

This file documents **player-facing** changes and is used directly for GitHub Releases and MOD platform release notes.
<br> 本文件记录**面向玩家**的改动，内容会直接用于 GitHub Release 与模组平台的更新说明。

Internal-only changes (such as refactoring, build configuration, and documentation maintenance) are not listed.
<br> 纯内部的调整（重构、构建配置、文档整理等）不在此列出。

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
<br> 本更新日志格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，
并遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

Older changelogs are archived by major and minor versions in the [Changelog Archives](changelogs/README.md).
<br> 旧版本更新日志按主版本号和次版本号归档，详见[更新日志归档](changelogs/README.md)。

See [AGENTS.md#changelog](AGENTS.md#changelog) for details.
<br> 详见 [AGENTS.md#changelog](AGENTS.md#changelog)。

Sections appear in this order when applicable: **Added / 新增**, **Changed / 变更**, **Deprecated / 弃用**, **Removed / 移除**, **Fixed / 修复**, **Security / 安全**. Performance improvements are listed under **Changed / 变更**.
<br> 各分类按以上顺序排列，空分类省略；性能优化归入 **Changed / 变更**。

## [Unreleased / 未发布]

### Changed / 变更

- Improved English and Chinese in-game text and punctuation.
<br> 优化英文和中文游戏内文本及标点。

### Fixed / 修复

- Fixed translation placeholders being replaced incorrectly or disappearing when arguments are missing.
<br> 修复翻译占位符被错误替换或在参数缺失时消失的问题。

## [1.1.0-beta.3] - 2026-11-8

### Added / 新增

- Added bilingual changelog (will sync to Modrinth)
<br> 新增双语言更新日志（将同步到Modrinth）

### Fixed / 修复

- Fixes the issue where Bedrock edition commands are still usable in Java(Fixes [#3])
<br> 修复基岩版指令在Java依旧可用的问题(Fixes [#3])
- Fixed the issue where all brigadier logs were in Chinese from early development debugging
<br> 修复brigadier日志都为早期开发调试的中文的问题

[#3]:https://github.com/ZCT-Studio/TeleportGUI/issues/3

## [1.1.0-beta.2] - 2026-11-8

### Added / 新增

- Added customizable client key bindings for quickly opening the TPA request list and other interfaces.
<br> 新增客户端自定义按键绑定，支持快捷打开 TPA 请求列表等界面。
- Added the `/gtpa req` command so Bedrock Edition players can open the TPA request list.
<br> 新增 `/gtpa req` 指令，支持基岩版玩家打开 TPA 请求列表。
- Added support for opening the Emote GUI menu through Bedrock Edition emotes to perform various actions (requires Geyser).
<br> 支持通过基岩版表情触发 Emote GUI 菜单以完成各种操作（需 Geyser）。

### Changed / 变更

- Bedrock Edition features no longer require Floodgate; only Geyser is needed.
<br> 基岩版功能不再强制依赖 Floodgate，仅需 Geyser。
- Changed the default permission level for `/randomtp` (`/rtp`) to administrator-only.
<br> `/randomtp`（`/rtp`）默认权限调整为管理员。

### Removed / 移除

- Removed the legacy hopper-menu fallback.
<br> 移除旧版漏斗菜单回退方案。

### Fixed / 修复

- Fixed issues with the TPA request list and incorrect Emote GUI status display.
<br> 修复 TPA 请求列表异常及 Emote GUI 状态显示错误。
- Fixed the incorrect mod ID for the Geyser dependency.
<br> 修复 Geyser 依赖的 Mod ID 错误。
- Improved wording in some localized texts.
<br> 优化部分国际化文本。

## [1.1.0-beta.1] - 2026-10-23

### Changed / 变更

- Moved `settings.yml` to `<game directory>/config/teleportgui/settings.yml`, so **all worlds share one configuration file**. During upgrades, the previous `settings.yml` in the world directory is automatically copied to the new location; the original file is preserved.
<br> `settings.yml` 迁移到 `<游戏目录>/config/teleportgui/settings.yml`，**所有存档共用一份设置**。升级时会自动把旧版本存放在世界目录内的 `settings.yml` 复制到新位置，旧文件保留。

## [1.1.0-alpha.2] - 2026-10-23

### Added / 新增

- Added automatic save-data migration from the mod's predecessor, **GeyserTPC**. If the new storage is empty and `<world directory>/GeyserTPC/storage.json` exists, warps, homes, and default homes are imported automatically. The old file is only read, never deleted, and can be cleaned up manually after verification.
<br> 支持从模组前身 **GeyserTPC** 自动迁移存档：当新存档为空且 `<世界目录>/GeyserTPC/storage.json` 存在时，会自动导入其中的传送点、家园与默认家园。旧文件只读不删，确认无误后可自行清理。

### Fixed / 修复

- Fixed TPA requests being consumed prematurely when the destination was unsafe and the player was waiting to confirm a forced teleport. Requests now remain valid until teleportation actually completes and are preserved if the player cancels.
<br> 修复 TPA 请求在「目标位置不安全、等待玩家确认强制传送」时被提前消耗的问题。现在请求会保留到传送真正完成，玩家取消后请求仍然有效。

## [1.1.0-alpha.1] - 2026-10-22

### Changed / 变更

- `settings.yml` now supports full YAML syntax, including lists and multiline text.
<br> `settings.yml` 现在支持完整 YAML 语法（列表、多行文本等）。

- Saving no longer repeatedly rewrites files as the number of entries grows, significantly reducing lag with many homes and warps.
<br> 保存存档不再随条目数量反复重写文件，家园与传送点较多时卡顿明显减少。
- Cached Bedrock Edition player detection results for faster command and GUI responses.
<br> 基岩版玩家的判断改为缓存结果，命令与界面响应更快。
- Optimized teleport destination searches to reduce waiting time for safe teleports.
<br> 传送落点搜索优化，安全传送时的等待时间缩短。

### Fixed / 修复

- Fixed players without `tpa.accept` or `tpa.deny` permissions being able to accept or deny teleport requests through the GUI or Bedrock Edition forms.
<br> 修复缺少 `tpa.accept` / `tpa.deny` 权限的玩家仍可从 GUI 或基岩版表单接受、拒绝传送请求的问题。
- Fixed duplicate command registration preventing servers from starting.
<br> 修复命令重复注册导致服务端无法启动的问题。
- Fixed `/reload` incorrectly treating other commands beginning with `reload` as reload commands.
<br> 修复 `/reload` 会误把 `reload` 开头的其它命令当成重载的问题。
- Fixed save writes potentially producing truncated files. If the process crashes or power is lost during writing, the original file is preserved instead of leaving a corrupted save.
<br> 修复存档写入可能产生半截文件的问题：中途崩溃或断电时会保留原文件，不会再写出损坏的存档。
- Fixed `/back` locations and death points remaining in memory after players disconnected.
<br> 修复玩家退出后 `/back` 位置与死亡点仍长期占用内存的问题。
- Fixed `/warp create` being available when the permission level was `0`.
<br> 修复 `/warp create` 在权限等级为 `0` 时仍可使用的问题。
