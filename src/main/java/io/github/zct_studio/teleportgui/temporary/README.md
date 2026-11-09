# Temporary code / 临时代码

This package contains transitional code that may be moved or removed after it is no longer needed. It is active code, not a general-purpose API.

`geyser_tpc` migrates homes, default homes, and warps from the predecessor GeyserTPC. During server initialization it reads `<world>/GeyserTPC/storage.json` when the new TeleportGUI storage has no existing player or warp data, then saves the converted data. It leaves the old file in place. Check the implementation and migration behavior before changing or removing this package; existing servers may still rely on it.

此包包含过渡性代码，在不再需要时可能被移动或删除。它仍会运行，并非通用 API。

`geyser_tpc` 将前身 GeyserTPC 的家园、默认家园和传送点迁移到 TeleportGUI。服务器初始化时，如果新存储中没有已有玩家或传送点数据，它会读取 `<世界>/GeyserTPC/storage.json` 并保存转换后的数据；旧文件会保留。修改或移除此包前，请检查实现和迁移行为，因为现有服务器可能仍依赖它。
