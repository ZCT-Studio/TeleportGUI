# Language resources / 语言资源

These UTF-8 JSON files contain server-side text. The file name matches the player's Minecraft language code: `en_us`, `zh_cn`, `zh_hk`, or `zh_tw`. The server looks up the player's language and falls back to `en_us` for missing keys. If English also lacks a key, the key itself is displayed and an error is logged.

To contribute a translation:

1. Use the existing JSON files as a guide and keep keys identical. Add new user-facing keys to `en_us.json` and translate them in the other files where possible.
2. Preserve placeholders such as `%0%` and `%1%`, including their indices, and retain meaningful formatting and escapes. They may be reordered to suit the language.
3. Keep JSON valid and encoded as UTF-8. Check the wording in game when possible, including menus and forms.
4. Mention incomplete translations in your pull request. See [CONTRIBUTING.md](../../../../../../CONTRIBUTING.md).

这些 UTF-8 JSON 文件包含服务端文本。文件名对应玩家的 Minecraft 语言代码：`en_us`、`zh_cn`、`zh_hk` 和 `zh_tw`。服务端优先使用玩家语言，缺少的键回退到 `en_us`；如果英文也缺少该键，则显示键名并记录错误。

贡献翻译时：

1. 参考现有 JSON 文件并保持键名一致。新增界面文字时，在 `en_us.json` 中添加键，并尽可能同步翻译其他语言。
2. 保留 `%0%`、`%1%` 等占位符及其序号，以及必要的格式和转义；可按语言习惯调整占位符顺序。
3. 确保 JSON 有效且使用 UTF-8 编码；尽可能在游戏内检查菜单和表单中的显示效果。
4. 在 Pull Request 中说明尚未完成的翻译。参见[贡献指南](../../../../../../CONTRIBUTING.md)。
