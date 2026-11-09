# TeleportGUI

## About

TeleportGUI is a server-side teleportation mod for Fabric and NeoForge. It brings graphical menus to homes, shared warps, and player teleport requests (TPA). Java Edition players can join with an unmodified client; Bedrock Edition players using Geyser and Floodgate can use forms.

🌈️ This project is LGBTQ+ friendly.

## Features

- Create, visit, rename, and delete personal homes and server-wide warps.
- Send, accept, and deny teleport requests, with expiry and per-player request preferences.
- Travel to a death point, world spawn, personal spawn, or your previous position, or use random teleportation.
- Configure safety checks, cooldowns, permissions, and limits. Players can confirm an unsafe teleport.
- Use server-side menus on Java Edition and forms on Bedrock Edition. English, Simplified Chinese, and Traditional Chinese (Hong Kong and Taiwan) are included.

## Installation

1. Choose a release for your Minecraft version and Fabric or NeoForge loader. The current source configuration targets Minecraft 26.3; check release files for other supported versions.
2. Install Architectury API on the server; Fabric also requires Fabric API.
3. Place the TeleportGUI jar for your loader in the server's mods folder.
4. Optionally install Geyser and Floodgate for Bedrock connections and forms.

## Common commands

| Command | Purpose |
| --- | --- |
| /home, /ghome | List homes or open the menu |
| /warp, /gwarp | List warps or open the menu |
| /tpa `<player>`, /tpahere `<player>`, /gtpa | Send a request or open the request menu |
| /tpaaccept `<player>`, /tpadeny `<player>` | Accept or deny a request |
| /back, /tpdeathpoint, /tpworldspawn, /tpspawnpoint | Return or travel to a death point, world spawn, or personal spawn |
| /randomtp | Teleport to a random location |
| /teleportgui allow_request | View or change whether you accept requests |
| /teleportgui_be notif_gui, /teleportgui_be emote_gui | View or change Bedrock request popups and the emote menu |
| /teleportgui_admin | Manage configuration and command toggles |

Some commands require permission. Use in-game command completion to see available arguments.

## Configuration and data

- config/teleportgui/settings.yml: settings shared across worlds. runtimePersistence controls whether cooldowns and pending requests persist; emoteTriggerUuids lists Bedrock quick-menu trigger emotes. An empty list disables the trigger.
- `<world directory>/teleportgui/config.json`: command, permission, cooldown, and safety settings.
- `<world directory>/teleportgui/storage.json`: homes, warps, and player preferences.
- `<world directory>/teleportgui/runtime.json`: cooldowns and pending requests when runtime persistence is enabled.

Back up both the global configuration and each world's teleportgui directory. Death and back points are held in memory and cleared on restart.

---

## 简体中文

### 简介

TeleportGUI 是支持 Fabric 和 NeoForge 的服务端传送模组，提供家园、公共传送点和玩家传送请求（TPA）的图形界面。Java 版玩家无需安装客户端模组；使用 Geyser 和 Floodgate 的基岩版玩家可以使用表单界面。

🌈️ 本项目对 LGBTQ+ 社群友好。

## 功能

- 创建、传送、重命名和删除个人家园与全服共享的传送点。
- 发送、接受或拒绝玩家传送请求；支持请求过期和玩家接收偏好。
- 传送到死亡点、世界出生点、个人出生点或上一次传送前的位置，也支持随机传送。
- 可配置传送安全检查、冷却、权限和数量上限；不安全的传送可由玩家确认。
- Java 版使用服务端菜单；基岩版可使用表单。支持英文、简体中文和繁体中文（香港、台湾）。

## 安装

1. 根据下载页面选择与你的 Minecraft 版本及 Fabric 或 NeoForge 加载器匹配的版本。当前源码配置的 Minecraft 版本为 26.3；其他版本的支持情况以发布文件为准。
2. 在服务器安装 Architectury API；Fabric 还需要 Fabric API。
3. 将对应加载器的 TeleportGUI 文件放入服务器的 mods 文件夹。
4. 如需基岩版玩家连接和表单支持，可安装 Geyser 与 Floodgate。它们是可选依赖。

## 常用命令

| 命令 | 用途 |
| --- | --- |
| /home、/ghome | 家园列表和界面 |
| /warp、/gwarp | 传送点列表和界面 |
| /tpa `<玩家>`、/tpahere `<玩家>`、/gtpa | 发起传送请求或打开请求界面 |
| /tpaaccept `<玩家>`、/tpadeny `<玩家>` | 接受或拒绝请求 |
| /back、/tpdeathpoint、/tpworldspawn、/tpspawnpoint | 返回、死亡点、世界出生点和个人出生点 |
| /randomtp | 随机传送 |
| /teleportgui allow_request | 查看或设置是否接收请求 |
| /teleportgui_be notif_gui、/teleportgui_be emote_gui | 查看或设置基岩版请求弹窗和表情菜单 |
| /teleportgui_admin | 管理配置和命令开关 |

部分命令受权限限制；使用游戏内命令补全查看可用参数。

## 配置与数据

- config/teleportgui/settings.yml：跨存档的模组设置。runtimePersistence 控制冷却及待处理请求是否持久化；emoteTriggerUuids 配置基岩版快捷菜单触发表情，空列表可禁用。
- `<世界目录>/teleportgui/config.json`：命令、权限、冷却及安全检查配置。
- `<世界目录>/teleportgui/storage.json`：家园、传送点和玩家偏好。
- `<世界目录>/teleportgui/runtime.json`：启用运行期持久化时保存冷却和待处理请求。

备份时请同时保存全局配置和各世界的 teleportgui 目录。死亡点和返回点保存在内存中，重启后清空。

## Links

[Report an issue](https://github.com/ZCT-Studio/TeleportGUI/issues) · [Changelog](https://github.com/ZCT-Studio/TeleportGUI/blob/main/CHANGELOG.md) · [MIT License](https://github.com/ZCT-Studio/TeleportGUI/blob/main/LICENSE)
