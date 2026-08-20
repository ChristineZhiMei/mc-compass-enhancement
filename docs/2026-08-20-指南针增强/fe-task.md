# 客户端任务：指南针增强 Mod

## 任务列表

- [x] FE-0：Git 分支管理
  - 实现：创建并切换到 `feature/20260820-compass-enhancement`
  - 验证：`git branch --show-current` 符合功能分支规范
  - _需求：tech-spec 开发准备_

- [x] FE-1：实现右键打开配置界面
  - 实现：客户端入口注册 UseItemCallback，Shift + 右键不拦截
  - 验证：客户端源码编译通过
  - _需求：tech-spec 第3章第1条_

- [x] FE-2：实现指南针配置界面
  - 实现：目标摘要、范围滑块、来源开关、保存/取消和非 BlockItem 提示
  - 验证：界面状态约束与保存 Payload 参数一致
  - _需求：tech-spec 第3章第2条、tech-spec 第4章_

- [x] FE-3：实现注册物品搜索选择器
  - 实现：基于 Registries.ITEM 的可搜索滚动网格并返回稳定 Identifier
  - 验证：客户端源码编译通过且选择结果不依赖翻译语言
  - _需求：tech-spec 第3章第2条_

- [x] FE-4：实现已配置指南针左键扫描
  - 实现：Mixin 拦截 `MinecraftClient#doAttack()` 并发送空扫描 Payload
  - 验证：仅已配置指南针被拦截，普通指南针保持原版行为
  - _需求：tech-spec 第3章第3条、tech-spec 第4章_

- [x] FE-5：补齐中英文资源与客户端 Mixin 配置
  - 实现：zh_cn/en_us 翻译、Mixin JSON 和 Fabric 客户端入口声明
  - 验证：资源处理和客户端编译通过
  - _需求：tech-spec 第3章_

- [x] 检查点：客户端编译通过
  - 验证：`./gradlew compileClientJava`
