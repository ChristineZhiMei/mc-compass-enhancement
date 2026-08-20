# 开发改动总结

## 1. 改动概览

| 项目 | 内容 |
|------|------|
| 需求名称 | Minecraft 1.21.4 Fabric 指南针增强 Mod |
| 开发时间 | 2026-08-20 |
| 涉及应用 | Fabric 服务端逻辑 / Minecraft 客户端界面 |
| 分支名称 | `feature/20260820-compass-enhancement` |

## 2. 服务端与共享改动

| 模块 | 改动说明 |
|------|---------|
| Gradle/Fabric 工程 | 新建 Java 21、Loom 1.9.2、Yarn 1.21.4+build.8、Fabric API 0.119.4 工程及 Wrapper |
| 数据组件 | 为每个原版指南针 ItemStack 注册独立的 `compass_enhanced:config` 配置组件 |
| 网络协议 | 新增保存配置和触发扫描两个 C2S CustomPayload，并在服务端执行可信校验与 10 tick 冷却 |
| 搜索服务 | 新增实体方块、ItemEntity 掉落物、BlockEntity Inventory 三类扫描器并统一比较最近结果 |
| 指南针定位 | 使用原版 LodestoneTrackerComponent 锁定目标；失败时清除旧目标并通过 ActionBar 反馈 |

## 3. 客户端改动

| 组件 | 改动说明 |
|------|---------|
| CompassEnhancedClient | 普通右键打开配置界面，Shift + 右键保留原版交互 |
| CompassConfigScreen | 提供目标、8～64 格范围、三个来源开关、保存/取消及非 BlockItem 警告 |
| ItemSelectorScreen / ItemGridWidget | 基于 Registries.ITEM 提供按翻译名或稳定 Identifier 搜索的滚动物品网格 |
| MinecraftClientMixin | 仅拦截已配置指南针的左键攻击，发送空扫描 Payload |
| 客户端资源 | 新增 Mixin 配置与中英文界面、结果反馈文本 |

## 4. 构建与验收

- `./gradlew clean build`：通过。
- 主源码与客户端源码：编译通过。
- Fabric `remapJar` 与 `remapSourcesJar`：通过。
- 产物：`build/libs/compass-enhanced-1.0.0.jar`、`build/libs/compass-enhanced-1.0.0-sources.jar`。
- 静态约束检查：未出现 `player.getInventory()`、`world.getPlayers()` 或 `PlayerInventory` 搜索调用。
- 所有世界方块和容器扫描只通过 `getChunk(..., ChunkStatus.FULL, false)` 访问已加载区块。
- 未创建测试代码或测试文件。

## 5. 已知边界

- 尚未在实际 Minecraft 客户端中进行人工交互冒烟测试；当前验证范围是完整编译、资源处理、Mixin 打包与 remap 打包。
- V1 按设计不支持移动容器实体、玩家 Inventory、跨维度或持续自动扫描。
