# 后端任务：指南针增强 Mod

## 任务列表

- [x] BE-0：Git 分支管理
  - 实现：创建并切换到 `feature/20260820-compass-enhancement`
  - 验证：`git branch --show-current` 符合功能分支规范
  - _需求：tech-spec 开发准备_

- [x] BE-1：创建 Fabric 1.21.4 Java 21 项目与服务端入口
  - 实现：Gradle、Fabric 元数据、Mod 初始化类及资源文件
  - 验证：Gradle 能解析项目并执行资源处理
  - _需求：tech-spec 第1章_

- [x] BE-2：实现指南针配置数据组件
  - 实现：配置 record、Codec/PacketCodec 与 DataComponentType 注册
  - 验证：主源码编译通过，配置可写入 ItemStack
  - _需求：tech-spec 第2章_

- [x] BE-3：实现 C2S Payload 与服务端校验
  - 实现：保存配置、触发扫描、主手指南针校验和 10 tick 冷却
  - 验证：网络 API 编译通过且扫描参数仅来自服务端 ItemStack
  - _需求：tech-spec 第4章_

- [x] BE-4：实现三种合法来源扫描与最近结果合并
  - 实现：BlockScanner、DroppedItemScanner、ContainerScanner、SearchService
  - 验证：源码中不调用玩家 Inventory；全部候选经过球形距离过滤
  - _需求：tech-spec 第2章第2～4条_

- [x] BE-5：实现原版指南针定位与 ActionBar 反馈
  - 实现：写入/清除 LodestoneTrackerComponent，显示来源与距离
  - 验证：服务端源码编译通过
  - _需求：tech-spec 第2章第4条_

- [x] 检查点：服务端编译通过
  - 验证：`./gradlew compileJava`
