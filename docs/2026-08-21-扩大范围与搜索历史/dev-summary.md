# 开发改动总结

## 1. 改动概览

| 项目 | 内容 |
|---|---|
| 需求名称 | 指南针 256 格范围、搜索历史、垂直方位与短期搜索优化 |
| 开发时间 | 2026-08-21 |
| 涉及应用 | Fabric 模组服务端 / 客户端 |
| 分支名称 | `feature/20260820-compass-enhancement` |
| 模组版本 | `1.2.1` |

## 2. 服务端与共享改动

| 文件 | 类型 | 说明 |
|---|---|---|
| `gradle.properties` | 修改 | 模组版本提升为 1.2.1 |
| `CompassConfigComponent.java` | 修改 | 共享合法半径上限提升为 256 |
| `ModNetworking.java` | 修改 | 用原始命中 Y 坐标按 ±2 阈值追加上/中/下提示 |
| `zh_cn.json` / `en_us.json` | 修改 | 新增方向和历史界面文案 |
| `LoadedChunkAccess.java` | 修改 | 在取区块前做水平距离下界剪枝，已加载区块近到远排序并复用 |
| `CompassSearchService.java` | 修改 | 按掉落物→容器→方块执行，在来源间传递 best 距离上界 |
| `ContainerScanner.java` | 修改 | 容器中心距离校验前置，不可能更近时不读取 Inventory 槽位 |
| `BlockScanner.java` | 修改 | 按 Section 实际搜索交集的 3D AABB 下界升序处理并精确剪枝 |

网络 Payload 结构、指南针 Data Component 结构和存档数据格式均未变更。

## 3. 客户端改动

| 文件 | 类型 | 说明 |
|---|---|---|
| `SearchHistoryStore.java` | 新增 | 客户端 config JSON 持久化，最近 8 项、MRU 去重、无效项过滤和异常容错 |
| `SearchHistoryWidget.java` | 新增 | 横向物品图标行、选中态、悬浮提示、旁白与点击回填 |
| `MinecraftClientMixin.java` | 修改 | 在扫描 C2S Payload 发送后记录已尝试的目标 |
| `CompassConfigScreen.java` | 修改 | 插入历史行，重排 214px 紧凑面板，历史点击后刷新来源状态 |

## 4. 已知限制

| 限制 | 影响 | 后续路线 |
|---|---|---|
| 256 格仅搜索已加载 FULL 区块 | 范围内未加载目标不会命中 | 保持不主动加载的安全边界 |
| 目标不存在时，256 格同步方块扫描仍接近全扫描 | 单次搜索仍可增加服务器 tick 延迟 | 短期近到远与 best 剪枝已实施；若压测仍超预算，进入跨 tick 预算任务 |
| 历史在客户端发包时记录 | 冷却或校验拒绝的请求仍可进入历史 | 若未来要求“仅成功”，增加 S2C 执行确认 |

## 5. 验证结果与注意事项

- WebStorm JDK 21 执行 `./gradlew clean build --offline --no-daemon`：`BUILD SUCCESSFUL`。
- 产物：`build/libs/compass-enhanced-1.2.1.jar`，fabric.mod.json 内版本为 1.2.1，Loader 下限仍为 0.16.12。
- 静态检查确认：仅读已加载区块、未读取玩家 Inventory、Scanner 数据源与精确最近语义不变、未新增测试文件。
- 人工游戏内验证时重点覆盖：256 滑块上限、8/9 条历史截断、历史点击回填、损坏 JSON 恢复、±2 格方位边界及三种搜索来源。
