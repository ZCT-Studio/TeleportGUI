<p align="center">
  <img src="orig_res/banner.jpg" alt="TeleportGUI — Teleportation Mod with GUI for Java and Bedrock players" width="100%">
</p>

<p align="center">
  <b>把家园、传送点与玩家传送请求装进直观的图形界面</b><br>
  服务端模组 · Fabric / NeoForge · Java 版与基岩版
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-26.3+-3C8527?style=flat-square" alt="Minecraft 26.3+">
  <img src="https://img.shields.io/badge/Fabric-supported-DBB69C?style=flat-square" alt="Fabric">
  <img src="https://img.shields.io/badge/NeoForge-supported-EA7B24?style=flat-square" alt="NeoForge">
  <img src="https://img.shields.io/badge/Bedrock-Geyser%20%2B%20Floodgate-4C9EEB?style=flat-square" alt="Bedrock via Geyser and Floodgate">
  <img src="https://img.shields.io/badge/license-MIT-2F81F7?style=flat-square" alt="MIT License">
</p>

<p align="center">
  <a href="README.en-US.md">English</a> ·
  <a href="Modrinth.md">Modrinth描述</a> ·
  <a href="CHANGELOG.MD">更新日志</a>
</p>

## 目录

- [简介](#introduction)
- [功能特性](#features)
- [环境要求](#requirements)
- [安装](#installation)
- [命令](#commands)
- [图形界面](#gui)
- [配置](#configuration)
- [权限](#permissions)
- [数据存储](#storage)
- [从源码构建](#build)
- [鸣谢](#credits)
- [TODO](#todo)
- [许可证](#license)

## <a id="introduction"></a>简介

TeleportGUI 是一个运行在服务端的 Minecraft 传送模组。它把家园、公共传送点、玩家传送请求（TPA）以及死亡点、出生点、返回点等常用传送功能，整合进
Java 版的箱子菜单和基岩版的表单界面，同时保留完整的命令用法。

- **Java 版玩家**：使用原版客户端即可，传送界面以箱子菜单和铁砧命名界面呈现。
- **基岩版玩家**：在服务器安装 Geyser 与 Floodgate 后，会自动获得 Cumulus 表单界面；未安装 Floodgate 时会退回漏斗菜单，功能仍然可用。
- **无需客户端模组**：在专用服务器上，原版 Java 客户端可以直接连接；模组只安装在服务端。

## <a id="features"></a>功能特性

### 家园

- 每位玩家拥有独立的家园列表，可创建、前往、删除、重命名和设置默认家园。
- 首个创建的家园会自动成为默认家园，可通过 `/home default <名字>` 更换。
- 默认最多创建 10 个家园，可在配置中调整或设为无限制。
- Java 版与基岩版均提供图形化家园列表。

### 公共传送点

- 全服共享的传送点，可创建、前往、删除和重命名。
- 默认最多创建 20 个传送点。
- 创建、删除与重命名默认需要权限等级 **2**，可分别在 `config.json` 的 `warp.create.permission.level`、
  `warp.del.permission.level` 与 `warp.rename.permission.level` 中修改；没有对应权限的玩家不会在界面中看到管理按钮。
- 是否允许创建由 `warp.create.permission.level` 控制，可将该值调低以允许普通玩家通过 `/warp create` 创建传送点，创建数量同时受
  `maxCount` 限制。
- Java 版与基岩版均提供图形化传送点列表。

### 玩家传送请求（TPA）

- `/tpa <玩家>`：请求传送到对方身边。
- `/tpahere <玩家>`：请求对方传送到你身边。
- 请求可以通过聊天栏按钮、`/tpaaccept`、`/tpadeny` 或 `/gtpa` 请求列表处理。
- 支持请求过期时间，默认 120 秒；`/tpa` 与 `/tpahere` 可分别配置，设为 `0` 表示永不过期。
- 基岩版玩家收到请求时可自动弹出表单，也可用 `/teleportgui_be notif_gui false` 关闭自动弹出。
- 玩家可用 `/teleportgui allow_request false` 禁止其他玩家向自己发送请求；默认允许。

### 传送点快捷指令

- **死亡点**：玩家死亡时自动记录死亡位置，可用 `/tpdeathpoint` 返回。
- **返回点**：每次传送前记录位置，可用 `/back` 返回上一次传送前的位置。
- **世界出生点**：`/tpworldspawn` 前往主世界出生点。
- **个人出生点**：`/tpspawnpoint` 前往床或重生锚绑定的位置；没有个人出生点时回退到世界出生点。
- 以上命令默认对所有玩家开放（权限等级 `0`），可在 `config.json` 中提高权限等级或调整安全检测开关。

### 安全传送

- 启用安全检测时，传送前会在目标附近搜索可站立的安全位置。
- 找不到安全位置时不会直接传送，而是询问是否强制传送：
    - Java 版：点击聊天栏的「强制传送」按钮，或输入 `/tpforce`。
    - 基岩版：在表单或备用菜单中确认「取消 / 强制传送」。
- 安全检测可按命令单独开关。

### 其他

- 家园、传送点与 TPA 命令支持名称 Tab 补全；Java 版聊天栏中的坐标和维度可点击复制。
- 内置简体中文、繁体中文（香港/台湾）与英文语言文件。
- 使用 JSON 存储后端。
- 支持 Fabric 与 NeoForge，通过 Architectury API 共享同一套逻辑。

## <a id="requirements"></a>环境要求

| 项目        | 要求                                            |
|-----------|-----------------------------------------------|
| Minecraft | **26.3+**                                     |
| Java      | **25+**                                       |
| Fabric    | Fabric Loader + Fabric API                    |
| NeoForge  | NeoForge +                                    |
| 前置模组      | **Architectury API**（Fabric 与 NeoForge 均需安装）  |
| 基岩版支持（可选） | Geyser + Floodgate；未安装 Floodgate 时基岩版使用漏斗菜单回退 |

> TeleportGUI 是服务端模组。在专用服务器上，Java 版玩家不需要安装任何客户端模组。

## <a id="installation"></a>安装

1. 准备一个 Minecraft 26.3+ 的 Fabric 或 NeoForge 服务端。
2. 将 **Architectury API** 放入 `mods/`；Fabric 服务端还需要 **Fabric API**。
3. 将对应加载器的 **TeleportGUI** jar 放入 `mods/`。
4. （可选）安装 **Geyser** 与 **Floodgate**，让基岩版玩家使用表单界面。
5. 启动服务器。首次启动会在 `<世界目录>/teleportgui/` 下生成配置文件与数据文件。

> 请根据加载器下载对应 jar：`TeleportGUI-fabric-<版本>.jar` 或 `TeleportGUI-neoforge-<版本>.jar`。不要把两个加载器的 jar
> 同时放进 `mods/`。

## <a id="commands"></a>命令

命令中的权限等级为原版命令权限等级（0–4），默认值可在 `config.json` 中修改。冷却时间从一次成功操作后开始计算。

### 家园

| 命令                                   | 说明        | 默认权限 | 默认冷却 |
|--------------------------------------|-----------|------|------|
| `/home`、`/home list`                 | 列出自己的家园   | 0    | 无    |
| `/home add <名字>`、`/home create <名字>` | 将当前位置设为家园 | 0    | 无    |
| `/home go <名字>`                      | 前往指定家园    | 0    | 5 秒  |
| `/home del <名字>`、`/home delete <名字>` | 删除家园      | 0    | 无    |
| `/home rename <旧名字> <新名字>`           | 重命名家园     | 0    | 无    |
| `/home default <名字>`                 | 设置默认家园    | 0    | 无    |
| `/home gui`、`/ghome`                 | 打开家园界面    | 0    | 无    |

### 传送点

| 命令                                   | 说明         | 默认权限 | 默认冷却 |
|--------------------------------------|------------|------|------|
| `/warp`、`/warp list`                 | 列出全部传送点    | 0    | 无    |
| `/warp add <名字>`、`/warp create <名字>` | 在当前位置创建传送点 | 2    | 无    |
| `/warp go <名字>`                      | 前往传送点      | 0    | 5 秒  |
| `/warp del <名字>`、`/warp delete <名字>` | 删除传送点      | 2    | 无    |
| `/warp rename <旧名字> <新名字>`           | 重命名传送点     | 2    | 无    |
| `/warp gui`、`/gwarp`                 | 打开传送点界面    | 0    | 无    |

### 玩家传送请求

| 命令                             | 说明         | 默认权限 | 默认冷却 |
|--------------------------------|------------|------|------|
| `/tpa <玩家>`                    | 请求传送到对方身边  | 0    | 30 秒 |
| `/tpahere <玩家>`                | 请求对方传送到你身边 | 0    | 30 秒 |
| `/tpaaccept <玩家>`、`/tpay <玩家>` | 接受来自该玩家的请求 | 0    | 无    |
| `/tpadeny <玩家>`、`/tpan <玩家>`   | 拒绝来自该玩家的请求 | 0    | 无    |
| `/gtpa`                        | 打开传送请求界面   | 0    | 无    |

### 传送点快捷指令

| 命令                      | 说明           | 默认权限 | 默认安全检测 |
|-------------------------|--------------|------|--------|
| `/back`                 | 返回上一次传送前的位置  | 0    | 开启     |
| `/tpdeathpoint`、`/tpdp` | 前往死亡点        | 0    | 开启     |
| `/tpworldspawn`、`/tpws` | 前往世界出生点      | 0    | 开启     |
| `/tpspawnpoint`、`/tpsp` | 前往个人出生点      | 0    | 关闭     |
| `/tpforce`              | 确认执行待处理的强制传送 | 继承   | —      |

> 权限等级可在 `config.json` 的 `teleport.<命令名>` 下通过 `permission.level` 修改，命令名分别为 `back`、`deathPoint`、
`worldSpawn`、`spawnPoint`。`/tpforce` 没有独立权限，仅在所属命令的安全检测未通过、产生待确认传送时才会用到。

### 随机传送

| 命令                 | 说明               | 默认权限 | 默认冷却 |
|--------------------|------------------|------|------|
| `/randomtp`、`/rtp` | 在当前维度随机传送到一个安全位置 | 0    | 60 秒 |

> `/randomtp`（别名 `/rtp`）默认以玩家当前位置为中心，在 **100–1000 格**的范围内随机寻找安全落点；最小距离、最大距离、冷却时间与权限等级可在
`config.json` 的 `teleport.random` 中修改。
>
> 命令开关统一使用 `randomtp`，`/rtp` 跟随它一起启用或禁用。

### 玩家设置

| 命令                                        | 说明                      | 权限   |
|-------------------------------------------|-------------------------|------|
| `/teleportgui allow_request`              | 查看自己是否允许接收 TPA 请求       | 所有玩家 |
| `/teleportgui allow_request <true/false>` | 允许或禁止其他玩家向自己发送 TPA 请求   | 所有玩家 |
| `/teleportgui_be notif_gui`               | 查看自己收到 TPA 时基岩版是否自动弹窗   | 所有玩家 |
| `/teleportgui_be notif_gui <true/false>`  | 设置自己收到 TPA 时基岩版是否自动弹出表单 | 所有玩家 |

### 管理员设置

`/teleportgui_admin` 只有权限等级 **4** 的玩家才能看到和执行。

| 命令                                             | 说明                          | 权限   |
|------------------------------------------------|-----------------------------|------|
| `/teleportgui_admin config <键>`                | 查看 `config.json` 中某个配置项的当前值 | 等级 4 |
| `/teleportgui_admin config <键> <值>`            | 修改并保存 `config.json` 中的配置项   | 等级 4 |
| `/teleportgui_admin command <命令>`              | 查看某组命令当前是否启用                | 等级 4 |
| `/teleportgui_admin command <命令> <true/false>` | 启用或禁用命令                     | 等级 4 |
| `/teleportgui_admin reload command`            | 重新应用命令开关并重建命令树              | 等级 4 |
| `/teleportgui_admin reload config`             | 重新读取 `config.json` 并重建命令树   | 等级 4 |

> `teleportgui`、`teleportgui_admin`、`teleportgui_be` 三个根命令都不能被命令开关禁用。
>
> `notifGuiDefault` 是全局默认值，可执行 `/teleportgui_admin config notifGuiDefault true` 或 `false`；玩家自己的
`/teleportgui_be notif_gui` 设置优先。
>
> 原版 `/reload` 也会重新读取 `config.json` 并更新命令树。

示例：

```text
/teleportgui allow_request
/teleportgui allow_request false
/teleportgui_be notif_gui
/teleportgui_admin config home.create.maxCount
/teleportgui_admin config home.go.cooldownSeconds 10
/teleportgui_admin config notifGuiDefault true
/teleportgui_admin command randomtp
/teleportgui_admin command randomtp false
/teleportgui_admin reload config
```

## <a id="gui"></a>图形界面

### Java 版

Java 版使用服务端箱子菜单，不需要安装客户端模组。

| 界面    | 操作                                                          |
|-------|-------------------------------------------------------------|
| 家园列表  | 左键传送、右键设为默认、`Shift + 左键` 删除、对准条目按 `F` 重命名、下界之星新增家园          |
| 传送点列表 | 左键传送、`Shift + 左键` 删除（管理员）、对准条目按 `F` 重命名（管理员）、下界之星新建传送点（管理员） |
| 请求列表  | 玩家列表左键发起请求、右键请求对方过来；请求列表左键接受、右键拒绝                           |
| 接收开关  | 指南右侧绿宝石块表示允许接收请求，红石块表示拒绝；点击切换                               |
| 通用导航  | `[6]` 关闭、`[7]` 上一页、`[8]` 下一页；`[2]` 在玩家列表与请求列表之间切换           |

创建或重命名地点时，会打开铁砧输入界面。

### 基岩版

安装 Geyser 与 Floodgate 后，基岩版玩家会使用 Cumulus 表单：

- 通过下拉框选择条目与操作，再提交表单。
- 创建、重命名使用自定义表单输入名称。
- 强制传送使用「取消 / 强制传送」模态确认框。
- 未安装 Floodgate 时自动退回基于漏斗的备用操作菜单，功能不受影响。

## <a id="configuration"></a>配置

所有文件位于 `<世界目录>/teleportgui/`。

### `settings.yml`

```yaml
runtimePersistence: true
```

> `runtimePersistence` 默认为 `true`，冷却与待处理 TPA 请求会写入 `teleportgui/runtime.json`；设为 `false`
> 后完全不读取、不写入该文件，冷却与 TPA 请求只保存在内存中，服务器重启后清空。  
> 旧版本中的 `logLevel`、`autoMigrate`、`storage` / `storage.sqlite` 配置已移除并被忽略；TeleportGUI 始终使用
`teleportgui/storage.json`，原 `storage.db` 不会自动迁移。

### `config.json`

`config.json` 保存每个命令的冷却、权限、数量上限和安全检测设置。可以只写需要覆盖的字段，加载时会自动补齐缺失的节点和默认值。

```json
{
  "notifGuiDefault": true,
  "commands": {
    "home": true,
    "warp": true,
    "tpa": true,
    "back": true,
    "tpdeathpoint": true,
    "tpworldspawn": true,
    "tpspawnpoint": true,
    "tpforce": true,
    "randomtp": true
  },
  "home": {
    "create": {
      "maxCount": 10,
      "cooldownSeconds": 0,
      "permission": {
        "level": 0
      }
    },
    "go": {
      "cooldownSeconds": 5,
      "permission": {
        "level": 0
      }
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
    },
    "random": {
      "minDistance": 100,
      "maxDistance": 1000,
      "cooldownSeconds": 60,
      "permission": {
        "level": 0
      }
    }
  }
}
```

`commands` 中的每个键控制一组命令是否启用。除 `teleportgui`、`teleportgui_admin`、`teleportgui_be`
三个根命令外，所有命令都可以关闭；别名跟随主命令一起启用或禁用，例如关闭 `tpa` 后 `/tpahere`、`/tpaaccept`、`/tpay`、
`/tpadeny`、`/tpan`、`/gtpa` 也会被移除。也可以使用 `/teleportgui_admin command <命令>` 查看状态、
`/teleportgui_admin command <命令> <true/false>` 在线修改。

常用字段：

| 字段                 | 说明                                                                        |
|--------------------|---------------------------------------------------------------------------|
| `cooldownSeconds`  | 冷却秒数；`0` 表示无冷却                                                            |
| `permission.level` | 原版命令权限等级 `0`–`4`；`-1` 表示继承 `defaults` 中的值                                 |
| `maxCount`         | 家园/传送点数量上限；`-1` 表示无限制                                                     |
| `expireSeconds`    | 过期秒数；`0` 表示永不过期。`tpa.request` 与 `tpa.requestHere` 分别设置，默认 `120`，其余节点为预留字段 |
| `safetyCheck`      | 是否启用安全位置检测                                                                |
| `minDistance`      | `/randomtp` 的最小水平距离（格）                                                    |
| `maxDistance`      | `/randomtp` 的最大水平距离（格）；`/randomtp` 必须为正数                                  |

> `expireSeconds` 目前仅对 `tpa.request`（`/tpa`）与 `tpa.requestHere`（`/tpahere`）生效，也只有这两项可用
`/teleportgui_admin config` 修改。`maxDistance` 目前仅用于 `/randomtp`。

## <a id="permissions"></a>权限

- 除下方特别说明的命令外，命令默认权限等级为 **0**，即所有玩家可用。
- 由配置控制的权限通过 `permission.level` 设置，取值为原版权限等级 `0`–`4`。
- `/warp del`、`/warp delete`、`/warp rename` 默认要求等级 **2**，由 `warp.del.permission.level` 与
  `warp.rename.permission.level` 控制。
- `/teleportgui_admin config`、`/teleportgui_admin command` 与 `/teleportgui_admin reload` 要求等级 **4**；
  `/teleportgui_admin` 对无权限玩家不可见。
- `/teleportgui` 的 `allow_request` 与 `/teleportgui_be` 的 `notif_gui` 所有玩家可用。
- 权限等级不足时，命令不会执行，并会向玩家发送无权限提示。

## <a id="storage"></a>数据存储

| 路径                         | 内容                                          |
|----------------------------|---------------------------------------------|
| `teleportgui/settings.yml` | 模组设置                                        |
| `teleportgui/config.json`  | 命令冷却、权限、数量上限、安全检测                           |
| `teleportgui/storage.json` | 家园、传送点、玩家偏好等数据                              |
| `teleportgui/runtime.json` | 冷却与待处理的 TPA 请求（`runtimePersistence=true` 时） |

需要注意的运行期数据：

- **冷却**与**待处理的 TPA 请求**会写入 `runtime.json`，服务器重启后保留。
- **死亡点**与**返回点**保存在内存中，服务器重启后会清空。
- 升级或备份前，请先备份整个 `teleportgui/` 目录。

## <a id="build"></a>从源码构建

需要 JDK 25 与 Gradle Wrapper：

```bash
git clone https://github.com/ZCT-Studio/TeleportGUI.git
cd TeleportGUI

# Fabric
./gradlew :<minecraft_version>-fabric:build

# NeoForge
./gradlew :<minecraft_version>.3-neoforge:build
```

Windows 下使用 `gradlew.bat`。构建产物位于：

```text
versions/<minecraft_version>-fabric/build/libs/TeleportGUI-fabric-<版本>.jar
versions/26.<minecraft_version>-neoforge/build/libs/TeleportGUI-neoforge-<版本>.jar
```

项目使用 Stonecutter 管理多加载器源码，并使用 Stonecraft 生成加载器元数据。

## <a id="credits"></a>鸣谢

感谢所有为这个项目提供帮助的人，完整名单见 [ACKNOWLEDGMENTS.MD](ACKNOWLEDGMENTS.MD)。

## <a id="todo"></a>TODO

- [TODO.MD](TODO.MD)

## <a id="license"></a>许可证

本项目使用 [MIT License](LICENSE) 授权。

***TeleportGUI 不是 Mojang Studios、Microsoft、GeyserMC 或 Floodgate 的官方产品，也未获得其认可或关联。***
