# 256 格搜索性能调研

## 版本与本轮边界

- Minecraft：`1.21.4`
- Yarn：`1.21.4+build.8`
- Fabric API：`0.119.4+1.21.4`
- 本轮已在提高共享半径上限和补充搜索结果方向的基础上，实施近到远遍历与共享 `bestDistanceSquared` 剪枝。扫描仍在服务端同步执行，且只检查已加载区块、已加载实体；不会为了搜索加载或生成区块。

## 当前 API 语义

当前方块和容器搜索都通过 `ServerChunkManager#getChunk(x, z, ChunkStatus.FULL, false)` 取区块。最后一个参数 `create=false` 是 loaded-only 约束：调用返回现有完整区块或 `null`，不会因搜索创建/加载缺失区块。相关官方 Yarn API：

- [`ServerChunkManager#getChunk(int, int, ChunkStatus, boolean)`](https://maven.fabricmc.net/docs/yarn-1.21.4+build.8/net/minecraft/server/world/ServerChunkManager.html#getChunk(int,int,net.minecraft.world.chunk.ChunkStatus,boolean))
- [`AbstractChunk#getSectionArray()`（`WorldChunk` 继承）](https://maven.fabricmc.net/docs/yarn-1.21.4+build.8/net/minecraft/world/chunk/AbstractChunk.html#getSectionArray())
- [`WorldChunk#getBlockEntities()`](https://maven.fabricmc.net/docs/yarn-1.21.4+build.8/net/minecraft/world/chunk/WorldChunk.html#getBlockEntities())

方块搜索先用 `ChunkSection#isEmpty` 和 `ChunkSection#hasAny` 过滤 section，再对可能命中的 section 逐方块读取状态。`hasAny` 是调色板级候选判断，能跳过不含目标方块的 section，但不能替代命中 section 内的精确遍历：

- [`ChunkSection#hasAny(Predicate)`](https://maven.fabricmc.net/docs/yarn-1.21.4+build.8/net/minecraft/world/chunk/ChunkSection.html#hasAny(java.util.function.Predicate))
- [`ChunkSection#getBlockState(int, int, int)`](https://maven.fabricmc.net/docs/yarn-1.21.4+build.8/net/minecraft/world/chunk/ChunkSection.html#getBlockState(int,int,int))

掉落物搜索使用 `ServerWorld#getEntitiesByClass` 对包围盒内已加载的 `ItemEntity` 做过滤；它查询服务端当前实体集合，不承担区块加载职责：

- [`ServerWorld#getEntitiesByClass(Class, Box, Predicate)`](https://maven.fabricmc.net/docs/yarn-1.21.4+build.8/net/minecraft/server/world/ServerWorld.html#getEntitiesByClass(java.lang.Class,net.minecraft.util.math.Box,java.util.function.Predicate))

扫描由 Fabric 服务端 C2S payload handler 触发。Fabric 的 handler API 与上下文见：

- [`ServerPlayNetworking#registerGlobalReceiver`](https://maven.fabricmc.net/docs/fabric-api-0.119.4+1.21.4/net/fabricmc/fabric/api/networking/v1/ServerPlayNetworking.html#registerGlobalReceiver(net.minecraft.network.packet.CustomPayload.Id,net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.PlayPayloadHandler))
- [`ServerPlayNetworking.PlayPayloadHandler`](https://maven.fabricmc.net/docs/fabric-api-0.119.4+1.21.4/net/fabricmc/fabric/api/networking/v1/ServerPlayNetworking.PlayPayloadHandler.html)

因此当前同步遍历会占用服务器 tick 的执行时间；loaded-only 避免区块生成或磁盘加载成本，但不等于扫描无主线程成本。

## 256 格的最坏数量级

以下是保守上界，不是每次扫描的实际耗时。假设玩家位置使搜索正方形跨越最多数量的区块边界，所有候选区块都已加载，并且 `hasAny` 无法排除 section：

| 半径 | 水平跨度 | 最多区块列 | 相对 128 | 最坏方块状态读取量级 |
|---|---:|---:|---:|---:|
| 128 | 257 × 257 | 17 × 17 = 289 | 1× | 约 2,840 万 |
| 256 | 513 × 513 | 33 × 33 = 1,089 | 3.77× | 约 1.07 亿 |

方块读取上界按主世界 384 格高度（24 个 section）计算：`区块列 × 24 × 16³`。因此 256 格为 `1,089 × 24 × 4,096 = 107,053,056`，约 1.07 亿次状态读取；128 格对应 `289 × 24 × 4,096 = 28,409,856`。实际搜索还受玩家 Y、搜索球体距离判断、已加载区块比例、空 section 与 `hasAny` 命中率影响，通常低于该上界。容器扫描成本更接近已加载区块实体数量，掉落物扫描成本更接近包围盒中的已加载实体数量，不能直接套用方块读取数。

## 优化路线

### 短期（已实施）：近到远遍历与共享 best 剪枝

一次请求只收集一次半径内已加载的 FULL 区块，按玩家到区块方块中心包围盒的最小水平距离排序，并由容器与方块扫描复用。来源按掉落物、容器、方块执行，后一个来源接收前一个来源的最近距离上界；掉落物已有近距离命中时，会先用 chunk 坐标计算水平下界，不可能严格更近的区块不会调用 `getChunk(..., false)`。容器在读取 Inventory 槽位前先验证容器中心的三维距离；方块继续使用 `isEmpty` 和 palette `hasAny`，再把候选 section 按实际搜索交集的三维包围盒下界排序。区块或 section 的下界无法严格优于当前 best 时停止后续扫描。

该实现不改变精确最近结果和半径边界：没有既有 best 时仍接受 `distanceSquared <= radiusSquared`，已有 best 时只接受严格更近的结果。同距离时保留先发现的掉落物、容器、方块来源顺序。附近存在目标时剪枝收益明显；目标不存在或只在范围边缘时仍会接近全扫描，且当前排序会产生少量请求期临时对象。

### 中期：跨 tick 固定预算

把一次搜索拆为可恢复任务，每 tick 只处理固定区块列、section 或方块状态预算，并保存游标与 best。玩家离线、切换世界、配置变化、手持物变化或新搜索到来时取消旧任务。它能限制单 tick 峰值，但会增加结果延迟、任务状态和一致性处理成本；Lodestone 与 ActionBar 只能在任务完成且上下文仍有效时更新。

### 长期：增量索引

按区块维护可搜索方块位置、容器内容摘要和目标物品倒排索引，在区块加载/卸载、方块变化、容器变化时增量更新，查询时只读取候选。重复搜索可接近索引查询成本，但实现和内存成本最高，还必须覆盖所有修改入口、处理区块生命周期与索引失效，并验证与模组方块实体的兼容性。

## 结论

本轮保留同步 loaded-only 扫描，并已完成短期近到远与共享 best 剪枝。256 格扩大的是“已加载数据中的搜索半径”，不是区块加载半径；发布说明不得宣称覆盖未加载区块，也不得宣称所有目标都没有服务器 tick 成本。上线前应以真实存档分别测量目标近、目标远、目标不存在和高方块种类/高实体密度场景；若目标不存在场景仍超过单 tick 预算，应进入跨 tick 固定预算方案，而不是把世界读取直接移到普通异步线程。
