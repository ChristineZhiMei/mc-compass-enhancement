# 后端任务：扩大范围、方向反馈与性能调研

> 本清单不包含测试代码或测试文件；不改变 Scanner 的数据源与精确最近语义。

## 任务列表

- [x] BE-OPT-1：实现已加载区块有序复用与共享 best 上界
  - 文件：`LoadedChunkAccess.java`、`CompassSearchService.java`
  - 验证：容器与方块复用同一有序列表，扫描来源依次传递更小的距离上界
  - _需求：R-06 / AC-015～AC-016_

- [x] BE-OPT-2：实现容器与方块精确包围盒剪枝
  - 文件：`ContainerScanner.java`、`BlockScanner.java`
  - 验证：不可能更近的区块/Section/容器不扫描，仍返回精确最近目标
  - _需求：R-06 / AC-015_

- [x] BE-OPT-3：更新调研状态并完成后端编译
  - 文件：`search-performance-research.md`
  - 验证：`compileJava` 通过，diff 无测试文件或非相关重构
  - 结果：WebStorm JDK 21 下 `compileJava processResources --offline --no-daemon` 通过
  - _需求：R-06 / AC-017_

- [x] Git 分支门禁：复用 `feature/20260820-compass-enhancement`

- [x] BE-1：将共享探测半径上限提升到 256
  - 文件：`src/main/java/dev/christine/compassenhanced/component/CompassConfigComponent.java`
  - 实现：将 `MAX_RADIUS` 从 128 改为 256，保持 Codec、保存校验和扫描校验共同引用该常量
  - 验证：滑块可保存 256；服务端拒绝 257；既有配置仍可读取
  - _需求：tech-spec R-01 / AC-001～AC-002_

- [x] BE-2：在搜索成功反馈中追加垂直方向
  - 文件：`src/main/java/dev/christine/compassenhanced/network/ModNetworking.java`
  - 实现：在 `handleScan` 中用原始 `nearest.target().y - player.getY()` 分类；`> 2` 上、`< -2` 下、其余中；作为成功文案第 4 个参数
  - 验证：分别验证 2.01、2.00、-2.00、-2.01；原距离、来源和 Lodestone 行为不变
  - _需求：tech-spec R-04 / AC-009～AC-012_

- [x] BE-3：完成官方 API 性能调研文档
  - 文件：`docs/2026-08-21-扩大范围与搜索历史/search-performance-research.md`
  - 实现：基于 Minecraft 1.21.4 Yarn 与 Fabric API 官方资料，记录 loaded-only/API 语义、128/256 数量级和近到远剪枝、跨 tick 分片、增量索引路线
  - 验证：所有 API 结论附官方直链与版本；明确本轮保留同步扫描且不改 Scanner
  - _需求：tech-spec R-05 / AC-013_

- [x] BE-4：完成后端与共享资源回归验证
  - 文件：现有主源码与语言资源，不新增测试文件
  - 实现：执行 `./gradlew compileJava processResources`，手工验证 256、越界拒绝、三种来源与 loaded-only 行为
  - 验证：构建通过；不读取玩家 Inventory；未加载区块不被加载或生成
  - 结果：WebStorm JDK 21 下完整 `clean build --offline` 通过；静态约束检查通过
  - _需求：tech-spec AC-014_
