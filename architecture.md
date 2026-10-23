# 架构速览

给接手维护的人（以及 AI 助手）看的项目地图。用户向的用法见 [README.md](README.md)，
更新记录见 [changelog.md](changelog.md)。

## 一句话概括

TeleportGUI 是一个**纯服务端**传送模组：把家园、公共传送点、TPA、死亡点等传送功能
统一成「命令 + 容器菜单 + 基岩版表单」三套入口，Java 版和基岩版玩家都能用。

## 目录结构

| 路径 | 作用 |
| --- | --- |
| `src/main/java/.../teleportgui/` | 与加载器无关的核心逻辑 |
| `src/fabric/java/...` | Fabric 入口（`FabricEntry`）与版本查询 |
| `src/neoforge/java/...` | NeoForge 入口（`NeoForgeEntry`）与版本查询 |
| `src/main/resources/` | 语言文件、`fabric.mod.json`、`neoforge.mods.toml` 模板 |
| `versions/<mc>-<loader>/` | Stonecutter 生成的各版本子项目 |
| `.github/workflows/` | CI：`build.yml` 常规编译，`release.yml` 打标签发布 |

`settings.gradle.kts` 里的 `mc("26.3", "fabric", "neoforge")` 决定当前支持哪些组合。
新增 Minecraft 版本只需在这里加一行，源码里的 Stonecutter 预处理指令（`//? if ...`）
会自动按版本裁剪。

## 核心分层

```
java/io/github/zct_studio/teleportgui/
├── ServerMain.java         入口：注册事件、串起所有子系统
├── Constants.java          MOD_ID / LOGGER / 可选依赖的类名
├── Styles.java             ChatFormatting 样式常量
├── Tr_i18n.java            服务端多语言查找 + %0% 占位符替换
├── commands/               命令实现
├── gui/                    Java 版容器菜单 + 基岩版表单
├── storage/                持久化
├── types/                  数据模型
├── utils/                  传送、权限、冷却等工具
└── compat/                 与外部库内部结构强绑定的适配层
```

### 命令层 `commands/`

- `Registry` —— 命令注册总入口，维护「功能名 → 全部别名」的分组，负责运行期增删。
- `Home` / `Warp` / `Tpa` / `TpPoints` / `RandomTp` —— 各功能模块。
- 每个命令都走 `CommandGuard.checkPermission` 与 `checkAndWarnCooldown`，
  权限与冷却配置来自 `config.json`。

> **重要约定**：`Tpa.accept` / `Tpa.deny` 只做业务，**不自带权限校验**。
> 所有入口（命令、GUI、表单）必须走 `acceptChecked` / `denyChecked`，
> 否则会绕过 `tpa.accept` 权限节点。

### GUI 层 `gui/`

- `GUI` —— Java 版箱子菜单。`PagedMenu` 是分页基类，下标计算见类注释。
- `Form` —— 基岩版 Floodgate 表单，**全部通过反射调用** Cumulus API，
  因为 Geyser / Floodgate 是可选依赖，直接引用类型会导致没装插件的服务器加载失败。
- `menu/anvil/` —— 借用铁砧输入框实现「文本输入」（原版 Java 版没有原生文本框）。
- `menu/hopper/` —— 5 格漏斗菜单，用于单个条目的操作面板。

### 存储层 `storage/`

```
Manager（内存态 + 写盘时机）
   ├─ StorageSnapshot   唯一的落盘模型，Gson 直接序列化它
   ├─ Json              Provider 的 JSON 实现，只负责读写快照
   ├─ AtomicFiles       临时文件 + 原子替换
   ├─ RuntimeStore      冷却与 TPA 请求的独立小文件（runtime.json）
   └─ DeathPoint        死亡点（仅内存，玩家退出即清理）
```

- 字段全部是**包装类型**，`null` 表示「未设置」，用于 Gson 区分「缺省」与「显式 0」。
- 批量保存必须走 `Manager.save()`（内部调用 `Provider.saveAll`），
  不要逐条调用 `saveWarp` / `savePlayer`，那样每条都会重写整个文件。

### 跨版本适配 `compat/`

放「换个 MC / 库版本就可能失效」的代码，业务层只依赖接口：

| 包 | 适配对象 | 说明 |
| --- | --- | --- |
| `compat.brigadier` | Brigadier 命令树 | `BrigadierAccess` 接口 + 反射实现，用于运行期移除命令 |

新增跨版本代码时请复用这个约定：**接口放在 `compat/`，具体实现在子包里**，
业务代码不要出现 `java.lang.reflect`。

## 关键设计取舍

### 为什么 GUI 用容器菜单而不是自定义界面

模组声明为纯服务端，客户端没有安装任何东西。自定义 GUI 需要客户端资源包与网络协议，
而原版容器菜单可以零客户端改动地渲染出来。

### 为什么基岩版走反射

Geyser / Floodgate / Cumulus 都是可选插件。若在字段或方法签名里直接引用这些类型，
没装插件的服务器会在类加载阶段抛 `NoClassDefFoundError`，整个模组起不来。
`Form` / `TeleportUtils` 里所有相关调用都用「字符串类名 + 反射」，
并把成功的反射句柄缓存起来（失败不缓存，便于插件热重载后恢复）。

### 并发模型

所有运行期状态（冷却表、TPA 请求、待确认传送）**只在服务端主线程访问**，
因此使用普通 `HashMap` / `ArrayList`。异步回调（如区块加载）必须通过
`server.execute(...)` 切回主线程再操作这些结构。

## 配置与数据文件

运行期文件位于 `<世界目录>/teleportgui/`：

| 文件 | 内容 | 解析方式 |
| --- | --- | --- |
| `settings.yml` | 模组开关（如 `runtimePersistence`） | SnakeYAML |
| `config.json` | 命令冷却、权限等级、数量上限 | Gson |
| `storage.json` | 家园、传送点、玩家偏好 | Gson（`StorageSnapshot`） |
| `runtime.json` | 冷却与待处理 TPA 请求 | Gson（`RuntimeStore`） |

所有写入都经过 `AtomicFiles`，要么完整成功，要么保留旧文件。

## 本地开发

```bash
# 编译
./gradlew :26.3-fabric:build :26.3-neoforge:build

# 开发环境运行服务端
./gradlew :26.3-neoforge:runServer
```

首次运行会在 `run/` 下生成测试服务器，需要手动同意 [Minecraft EULA](https://www.minecraft.net/eula)。

## 发布

推 `v*` 标签触发 `.github/workflows/release.yml`：

- tag 去掉 `v` 后必须等于 `gradle.properties` 的 `mod.version`，否则直接失败。
- 含 `snapshot` / `alpha` → 以 ALPHA 发布；含 `beta` → BETA；其余 STABLE。
- GitHub Release 由 fabric 子项目统一创建，同时附带两个加载器的 jar；
  Modrinth 由两个加载器各自发布一条版本。

需要的仓库密钥：`MODRINTH_TOKEN`（Secret）、`MODRINTH_ID`（Variable，缺省为 `teleportgui`）。
