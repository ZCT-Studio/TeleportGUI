# Contributing

<details>
<summary>简体中文</summary>

# 贡献指南

感谢帮助改进 TeleportGUI。欢迎提交问题报告、翻译、文档和代码。

## 报告问题

请先搜索 [Issue 页面](https://github.com/ZCT-Studio/TeleportGUI/issues)。报告 Bug 时，请提供模组和 Minecraft 版本、加载器及版本、相关依赖、复现步骤、预期与实际行为以及相关日志；提交前删除日志中的隐私信息。提出功能建议时，请说明要解决的问题和期望的行为。

## 提交修改

1. 较大的改动请先创建 Issue 讨论方案。Fork 仓库并为修改创建分支。
2. 保持改动聚焦并遵循现有风格。共享逻辑放在 `src/main`，加载器专用逻辑放在 `src/fabric` 或 `src/neoforge`；保持可选集成的可选性。
3. 查看 `settings.gradle.kts` 和版本配置，使用兼容的 JDK。运行 `./gradlew compileJava --no-daemon` 及相关测试；修改共享代码时尽可能验证两个加载器，并说明无法运行的测试。
4. 行为或用户界面文字变化时更新文档和翻译。翻译方法见[语言资源说明](src/main/resources/assets/teleportgui/lang/README.md)。适当时按照现有格式添加面向用户的双语更新记录。
5. 提交 Pull Request，说明问题、修改和测试方式，关联相关 Issue，并回应审查意见。

在讨论和审查中请保持尊重与建设性。提交贡献即表示同意按照项目的 [MIT 许可证](LICENSE)纳入项目。

---

</details>

Thank you for helping improve TeleportGUI. Bug reports, translations, documentation, and code changes are welcome.

## Report an issue

Search the [issue tracker](https://github.com/ZCT-Studio/TeleportGUI/issues) first. For bugs, include the mod and Minecraft versions, loader and version, relevant dependencies, steps to reproduce, expected and actual behavior, and relevant logs. Remove private information from logs. For feature requests, describe the problem and proposed behavior.

## Make a change

1. For substantial changes, open an issue first to discuss the approach. Fork the repository and create a branch for your work.
2. Keep changes focused and follow the existing style. Shared behavior belongs in `src/main`; loader-specific behavior belongs in `src/fabric` or `src/neoforge`. Keep optional integrations optional.
3. Check `settings.gradle.kts` and version configuration, and use a compatible JDK. Run `./gradlew compileJava --no-daemon` and any relevant tests; validate both loaders for shared changes when feasible. Document tests you could not run.
4. Update documentation and translations when behavior or user-facing text changes. For translation guidance see [the language README](src/main/resources/assets/teleportgui/lang/README.md). Add a bilingual user-facing changelog entry when appropriate, following the existing format.
5. Submit a pull request describing the problem, the change, and how it was tested. Link related issues, and respond to review feedback.

Be respectful and constructive in issues and reviews. By submitting a contribution, you agree to its inclusion under the project's [MIT License](LICENSE).
