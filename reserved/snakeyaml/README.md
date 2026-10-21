# 预留：SnakeYAML 版 settings.yml 加载器

当前实际使用的是内置的 `SimpleYaml` 解析器，本目录代码**不参与编译**，仅作为以后切回 SnakeYAML 的预留实现。

## 切换步骤

1. 在 `build.gradle.kts` 的 `dependencies` 中加入：

   ```kotlin
   implementation("org.yaml:snakeyaml:2.2")
   include("org.yaml:snakeyaml:2.2")
   ```

   `implementation` 只用于编译和开发环境，`include` 才会把依赖打包进发布 jar。

2. 将 `ModSettingsLoader.java` 复制并覆盖到：

   ```text
   src/main/java/io/github/zct_studio/teleportgui/utils/config/ModSettingsLoader.java
   ```

3. 删除或保留 `SimpleYaml.java`（切换后不再被引用，可以删除）。

4. 重新构建并启动验证：

   ```text
   .\gradlew.bat :26.3-fabric:build :26.3-neoforge:build
   ```

## 注意

- 预留代码基于当前 `ModSettings`（只包含 `logLevel` 和 `autoMigrate`）。
- 旧 `settings.yml` 中的 `storage` / `storage.sqlite` 配置会被忽略。
- 切换到 SnakeYAML 后，`settings.yml` 可以使用完整 YAML 语法；当前内置解析器只支持简单的嵌套 map 和标量。
