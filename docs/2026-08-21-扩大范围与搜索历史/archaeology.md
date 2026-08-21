# 数据考古文档

## 1. 端到端入口

| 需求 | 客户端入口 | 服务端入口 | 数据源 |
|---|---|---|---|
| 256 格上限 | `CompassConfigScreen.RadiusSlider` | `ModNetworking.handleSave/handleScan` | `CompassConfigComponent.MAX_RADIUS` |
| 搜索历史 | `MinecraftClientMixin#doAttack` | 无 | 主手指南针的 `CompassConfigComponent.targetItem()` |
| 上/中/下 | 翻译文本展示 | `ModNetworking.handleScan` | `SearchResult.target().y - player.getY()` |

## 2. 字段与校验链

| 字段 | 类型 | 来源 | 用途 |
|---|---|---|---|
| `radius` | `int` | 配置页滑块 / 指南针 Data Component | Codec 持久化、保存校验、扫描校验 |
| `targetItem` | `Identifier` | 指南针 Data Component | 客户端记录最近发起的搜索，服务端解析目标物品 |
| `SearchResult.target` | `Vec3d` | 三种 Scanner 的最近命中 | 计算距离、Lodestone 坐标与垂直方向 |
| 历史列表 | `List<Identifier>` | 客户端实例级 JSON | 配置页最近 8 个搜索目标 |

`MAX_RADIUS` 是现有单一真实源，修改后会同时覆盖 `Codec.intRange`、滑块换算、保存包校验和扫描前再校验。

## 3. 前端考古结论

- “搜索过”的稳定入口是 `MinecraftClientMixin#doAttack`，应在客户端发送扫描 Payload 后记录；仅选择或保存配置不记录。
- 历史按 `Identifier` 持久化到 Fabric `config` 目录，最近使用置顶、去重、最多 8 项。加载时过滤无效 ID、未注册物品和空气。
- 配置页在目标物品与半径之间新增独立横向图标行。点击只回填当前目标，不自动保存或关闭页面。
- 历史点击后需立即刷新搜索来源按钮；当目标不是 `BlockItem` 时关闭实体方块来源。
- 配置目录是 Minecraft 实例级，因此历史默认跨服务器、跨存档共享。

## 4. 后端考古结论

- 垂直方向用未取整的最终命中坐标计算：`deltaY > 2` 为“上”，`deltaY < -2` 为“下”，其余（含 ±2）为“中”。
- 仅成功提示新增方向参数，不改变现有 3D 欧氏距离的四舍五入逻辑。
- 三种扫描均只读已加载 FULL 区块，256 格是几何上限，不会主动加载或生成远区块。
- 方块扫描已用 section palette `hasAny` 快速否决；但对石头等常见方块，256 格最坏仍可涉及 1089 个区块列和约 1.07 亿次方块状态读取，同步执行有 TPS 风险。

## 5. 初步改动方案

### 本次交付

- [ ] 将 `MAX_RADIUS` 改为 256，保持已加载区块约束。
- [ ] 新增客户端 JSON 历史存储与配置页横向历史行。
- [ ] 在实际发送搜索请求后记录最近目标。
- [ ] 在搜索成功文案增加上/中/下方位。
- [ ] 产出搜索性能调研文档，明确风险和分阶段路线，本轮不重写扫描架构。

### 后续优化路线

1. 按区块/section 到玩家的最小距离近到远扫描，在已有候选结果后剪枝，并在各 Scanner 间共享当前最佳距离上界。
2. 将大范围方块扫描改为按 tick 固定 section/时间预算的可取消任务，完成后再回传结果。
3. 只在更大规模下考虑按区块生命周期与方块/容器变更维护增量索引，同时承担内存与失效一致性成本。

## 6. 需验证的风险

- 256 格同步常见方块扫描可能占用服务器主线程较长时间，发布文案必须说明“仅已加载区块”。
- 现有协议只能在发包时记录“已尝试搜索”，无法区分服务端因冷却/校验拒绝或真正执行的结果；如要求仅记录搜索成功的物品，需新增 S2C 确认协议。
