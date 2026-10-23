# TODO

- [ ] 添加 TeleportGUI Client 支持（快捷键）
  - 客户端按键绑定注册（Fabric: `KeyBindingHelper` / NeoForge: `RegisterKeyMappingsEvent`）
  - 需要新增 `src/main/resources/{fabric.mod.json,neoforge.mods.toml}` 里对应平台的客户端入口点
  - 当前的 GUI 全部是 `MenuType.CHEST` / `HOPPER` 的容器菜单，客户端按键应发送命令或
    `ServerboundCustomPayloadPacket` 让服务端打开菜单，而不是在客户端本地绘制
