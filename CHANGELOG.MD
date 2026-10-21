# 更新日志

本项目所有重要更改都会记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)，
本项目遵循 [语义化版本](https://semver.org/spec/v2.0.0.html)。

## [未发布]

## [1.0.1-rc2] - 2026-10-21

### 新增

- 新增 `/randomtp` 随机传送命令（`/rtp` 为别名）：在当前维度内以玩家当前位置为中心，在配置的最小距离与最大距离之间随机寻找安全落点；支持冷却、权限等级、距离范围配置。
- `config.json` 新增 `commands` 命令开关，除 `/teleportgui` 外所有命令（含别名）均可禁用；禁用后命令会从命令树中移除，启用后重新加入。
- 新增 `/teleportgui command disable <命令>` 与 `/teleportgui command enable <命令>`，修改后会自动保存配置、重建命令树并同步在线玩家的命令列表。
- 新增 `/teleportgui reload command` 与 `/teleportgui reload config`：前者重建当前命令树，后者重新读取 `config.json` 后再重建。
- `/reload` 现在会同时重新加载 `config.json` 并更新命令树。
- `config.json` 新增 `teleport.random` 节点，可配置 `minDistance`、`maxDistance`、`cooldownSeconds` 与 `permission.level`。
- 新增 `zh_hk`（繁体中文·香港）与 `zh_tw`（繁体中文·台湾）语言文件。
- `settings.yml` 新增 `runtimePersistence` 选项，默认 `true`；设为 `false` 后冷却与 TPA 请求仅保存在内存中，完全不读取、不写入 `runtime.json`。

### 变更

- `config.json` 配置版本升级到 2；旧配置在加载时会自动补齐 `commands`、`teleport.random` 等新字段并保存。
- 命令开关按功能分组，别名跟随主命令一起启用或禁用（例如禁用 `tpa` 会同时移除 `/tpahere`、`/tpaaccept`、`/gtpa` 等）。
- 优化随机传送：只同步检查已加载区块，未加载区块改为异步加载且最多尝试 4 个区块，下界扫描深度限制为 48 格，避免服务器线程连续生成区块造成卡顿。
- 命令冷却与 TPA 请求改为基于服务器刻的惰性计时，使用玩家 UUID + 数值命令 ID 作为键，不再拼接字符串；每 1200 刻执行一次粗粒度过期清理。
- 当 `runtimePersistence=true` 时，冷却与待处理 TPA 请求写入 `teleportgui/runtime.json`，服务器重启后保留。
- 语言文件按语言缓存完整翻译表，翻译调用不再重复读取和解析 JSON。
- `/teleportgui` 永远不会被命令开关禁用，确保管理员始终可以恢复其他命令。
- 版本号变更为 `1.0.0-rc.2`。

### 移除

- 移除 SQLite 存储后端，TeleportGUI 现在始终使用 JSON 存储（`teleportgui/storage.json`）。
- 移除 `settings.yml` 中的 `logLevel`、`autoMigrate` 与 `storage` / `storage.type` 预留配置；这些旧键会被忽略，`settings.yml` 现在只保留 `runtimePersistence`。
- 旧 `settings.yml` 中的 `storage` / `storage.sqlite` 配置会被忽略；已有 `storage.db` 不会自动迁移。

### 修复

- 修复传送点删除与重命名始终硬性要求权限等级 4 的问题。现在由 `warp.del.permission.level` 与 `warp.rename.permission.level` 控制，默认等级 2；GUI 管理按钮也按配置权限显示。
- 修复发布版未打包 SnakeYAML，导致服务器启动时因 `NoClassDefFoundError: org/yaml/snakeyaml/Yaml` 崩溃的问题。`settings.yml` 现在使用内置的轻量解析器，不再依赖第三方 YAML 库。

## [1.0.1-rc1] - 2026-10-20

### 变更

- 将命令格式工具中的重复代码提取到 `CommandUtils` 类。

### 修复

- 传送到位于非完整方块（如土径、耕地、台阶）上的家园、传送点、死亡点或玩家时，不再把玩家放到方块下方。保存位置时会对 Y 坐标四舍五入，传送前还会将目标从被占用的方块中抬升到可站立位置，旧版本保存的位置同样适用。

## [1.0.0-rc.2] - 2026-10-20

### 新增

- 新增作者信息支持。

### 移除

- 移除了语言文件中多余的说明信息。
- 移除 Fabric 横幅。

## [1.0.0-rc.1] - 2026-10-20

### 新增

- 新增完整文档。
- 新增 `/tpaY` 和 `/tpaN` 作为接受与拒绝传送请求的别名。
- 新增模组图标与横幅。
- 启动日志中新增当前加载器名称。
- 模组版本改为运行时从加载器元数据读取，不再硬编码在 Java 常量中。

### 变更

- 将加载器专属源码整合到 `src/fabric` 与 `src/neoforge`，移除按版本复制的源码。
- 重做基岩版表单交互，包括强制传送的取消/确认弹窗，并调整按钮顺序。
- 统一按钮样式，并将共享样式移入常量。
- 交换 Java 版家园/传送点列表的鼠标操作：右键设为默认家园，`Shift + 左键` 删除。
- 规范化模组元数据、依赖声明与版本配置；Fabric 现在声明 Architectury 依赖。
- 更新 NeoForge 版本并修复 NeoForge 构建。
- 更新模组描述。

### 修复

- 修复聊天消息中占位符错位的问题。
- 修复基岩版表单发送失败的问题。
- 修复读取基岩版家园/传送点表单时下拉框和输入框索引错误的问题。
- 修复家园/传送点列表中已弃用的点击命令，并支持包含引号的名称。
- 移除重复的命令提示。
- 移除铁砧重命名/创建界面返还羽毛物品时多余的空行。
- 修复 Fabric 依赖列表缺少 Architectury 的问题。
- 从 `.gitignore` 中移除 Fabric/NeoForge 条目，使加载器配置文件纳入版本管理。

### 移除

- 移除未使用的参数、方法和未使用的迁移类。
- 移除交互式聊天列表条目中的重命名操作。

## [1.0.0-alpha.1] - 2026-10-20

### 新增

- 首个版本，通过 Architectury 支持 Minecraft 26.3 的 Fabric 与 NeoForge。
- 家园：创建、前往、列表、删除、重命名、默认家园，以及 `/ghome` 界面。
- 全服传送点：创建、前往、列表、删除、重命名，以及 `/gwarp` 界面。
- TPA：`/tpa`、`/tpahere`、`/tpaaccept`、`/tpadeny` 以及 `/gtpa` 请求界面。
- 传送快捷指令：`/back`、`/tpdeathpoint`（`/tpdp`）、`/tpworldspawn`（`/tpws`）、`/tpspawnpoint`（`/tpsp`）。
- 自动记录死亡点。
- Java 版箱子菜单界面，支持分页与铁砧命名。
- 基岩版通过 Geyser/Floodgate 使用表单，并提供漏斗菜单回退。
- 安全位置检测，以及通过 `/tpforce` 确认强制传送。
- 通过 `config.json` 配置每个命令的冷却、权限等级、数量上限、TPA 过期时间与安全检测。
- JSON 与 SQLite 存储后端，以及 `settings.yml`。
- 简体中文与英文语言文件。

### 变更

- 对无权限玩家隐藏传送点添加按钮。
- 区分默认家园（红色床）与普通家园（白色床），并将添加图标改为下界之星。

### 修复

- 修复 Java 界面中的预览物品可移动到副手并在创造模式下使用的问题。
- 修复 TPA 接受/拒绝结果消息发送给错误玩家的问题。
