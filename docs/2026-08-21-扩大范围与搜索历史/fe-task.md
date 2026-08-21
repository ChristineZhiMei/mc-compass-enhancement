# 客户端任务：最近搜索历史与配置页交互

> 本清单不包含测试代码或测试文件；历史表示客户端已尝试发送扫描请求。

## 任务列表

- [x] Git 分支门禁：复用 `feature/20260820-compass-enhancement`

- [x] UI 规范检查：本项目使用 Minecraft 原生 GUI，无 CSS/Zent；复用现有 Widget、原版颜色和紧凑间距

- [x] FE-1：实现最近 8 项搜索历史持久化
  - 文件：新增 `src/client/java/dev/christine/compassenhanced/client/history/SearchHistoryStore.java`
  - 实现：在 Fabric config 目录读写版本化 JSON；Identifier 校验、MRU 去重、最多 8 项、非法项过滤和读写失败容错
  - 验证：9 项截断、重复置顶、跨客户端重启保留；坏 JSON 和失效物品不导致崩溃
  - _需求：tech-spec R-02 / AC-005～AC-006_

- [x] FE-2：仅在扫描 Payload 发送后记录历史
  - 文件：`src/client/java/dev/christine/compassenhanced/client/mixin/MinecraftClientMixin.java`
  - 实现：读取主手 `CompassConfigComponent`，发送 `ScanCompassPayload` 后调用 `recordAttempt(config.targetItem())`
  - 验证：只选择或保存不记录；已配置指南针左键后记录；普通指南针不记录
  - _需求：tech-spec R-02 / AC-003～AC-004_

- [x] FE-3：新增横向历史控件并重排配置页
  - 文件：新增 `src/client/java/dev/christine/compassenhanced/client/screen/SearchHistoryWidget.java`；修改 `CompassConfigScreen.java`
  - 实现：横向显示最多 8 个图标、悬浮提示与旁白；点击复用 `selectTarget` 回填；来源按钮改为两列加整行的 214px 紧凑布局
  - 验证：240px 高窗口无越界；点击非方块物品后关闭实体方块来源；不自动保存或关闭页面
  - _需求：tech-spec R-03 / AC-007～AC-008_

- [x] FE-4：补齐中英文历史与方向资源
  - 文件：`src/main/resources/assets/compass_enhanced/lang/zh_cn.json`、`en_us.json`
  - 实现：成功文案增加第 4 个方向参数；新增上/中/下、历史标签、空状态和旁白翻译键
  - 验证：两种语言 JSON 可被资源处理；界面和 ActionBar 不显示原始翻译键
  - _需求：tech-spec R-03～R-04 / AC-007、AC-009～AC-012_

- [x] FE-5：完成客户端回归验证
  - 文件：现有客户端源码，不新增测试文件
  - 实现：执行 `./gradlew compileClientJava processResources`，手工覆盖历史容量、持久化、点击回填、256 滑块与普通指南针行为
  - 验证：构建通过；普通指南针左键/右键行为不变；历史不进入网络 Payload
  - 结果：WebStorm JDK 21 下 `compileClientJava processResources --offline` 通过；静态回验确认普通指南针早退、历史仅保存本地 JSON
  - _需求：tech-spec AC-014_
