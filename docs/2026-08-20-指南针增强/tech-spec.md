# 指南针增强 Mod 技术规格

本文件的唯一规范来源是仓库根目录的《Minecraft 1.21.4 Fabric 指南针增强 Mod 最终方案.md》。实现与验收必须逐条遵循该最终方案，不在此重复维护副本。

## 1. 平台与构建

- Minecraft Java Edition 1.21.4
- Fabric Loader、Fabric API、Yarn mappings
- Java 21、Gradle 构建

## 2. 服务端能力

- 为每个原版指南针 ItemStack 保存独立配置。
- 仅扫描实体方块、ItemEntity 掉落物、实现 Inventory 的 BlockEntity。
- 球形范围限制为 8～128 格，只访问已加载区块，10 tick 冷却。
- 三种来源统一比较距离，使用原版 LodestoneTrackerComponent 锁定最近目标。
- 架构上禁止读取任何玩家 Inventory。

## 3. 客户端能力

- 普通右键打开配置界面，Shift + 右键保留原版行为。
- 提供注册物品搜索选择、范围滑块和来源开关。
- 已配置指南针左键取消普通攻击/挖掘并请求一次扫描。

## 4. interface-contract

- `SaveCompassConfigPayload`：目标物品 ID、半径、方块/掉落物/容器三个开关。
- `ScanCompassPayload`：空载荷，服务端从主手 ItemStack 读取可信配置。
- 服务端校验物品、半径和来源后才写入或执行扫描。

## 5. 需求完整度审计

以最终方案第 33 章 V1 功能清单为验收基线；所有标记为 ✅ 的能力均需实现，所有标记为 ❌ 的来源均不得进入扫描集合。
