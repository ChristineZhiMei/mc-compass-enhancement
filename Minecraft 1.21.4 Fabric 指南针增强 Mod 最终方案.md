# Minecraft 1.21.4 Fabric 指南针增强 Mod 最终方案

## 1. 项目定位

基于：

```text
Minecraft Java Edition：1.21.4
Mod Loader：Fabric
开发语言：Java 21
Mappings：Yarn 1.21.4
```

直接增强原版：

```text
minecraft:compass
```

**不新增新的指南针物品。**

核心能力：

> 玩家手持指南针右键配置目标物品和探测范围，左键执行一次探测，从允许的数据源中寻找距离玩家最近的目标，并让指南针指向该目标。

---

# 2. 最终交互

## 右键指南针

打开配置 GUI：

```text
┌──────────────────────────────────────┐
│            指南针探测配置             │
│                                      │
│  目标物品                            │
│  ┌──────────────────────────────┐    │
│  │ 💎 钻石                   ▼  │    │
│  └──────────────────────────────┘    │
│                                      │
│  探测范围                            │
│                                      │
│  8 ─────────●────────────── 128      │
│             32 格                    │
│                                      │
│  探测来源                            │
│                                      │
│  ☑ 实体方块                          │
│  ☐ 掉落物                            │
│  ☐ 容器内物品                        │
│                                      │
│        [取消]       [保存]            │
└──────────────────────────────────────┘
```

默认：

```text
探测范围：32 格

☑ 实体方块
☐ 掉落物
☐ 容器内物品
```

范围：

```text
最小：8
最大：128
```

---

# 3. 明确排除玩家物品

这是最终方案中的硬性规则：

> **任何情况下都不扫描玩家 Inventory。**

包括当前玩家：

```text
❌ 主手
❌ 副手
❌ 快捷栏
❌ 主背包
❌ 护甲栏
❌ 光标正在拿着的物品
```

同样不扫描其他玩家：

```text
❌ 其他玩家主手
❌ 其他玩家副手
❌ 其他玩家背包
❌ 其他玩家装备
```

因此：

```text
玩家 A 背包有钻石
玩家 B 背包有钻石
```

即使距离只有：

```text
1 格
```

探测器也：

```text
完全忽略
```

整个搜索系统**不会调用玩家 Inventory 作为任何搜索源**。

---

# 4. V1 唯一允许的三个探测来源

```text
                 SearchService
                       │
          ┌────────────┼────────────┐
          │            │            │
          ▼            ▼            ▼
       世界方块       掉落物       方块容器
```

只有：

```text
1. 实体方块
2. ItemEntity 掉落物
3. 方块容器中的物品
```

玩家永远不在搜索源中。

---

# 5. 目标物品选择

点击目标物品：

```text
┌──────────────────────────────────────┐
│  选择探测物品                        │
│                                      │
│  🔍 [diamond____________________]    │
│                                      │
│  💎 钻石          钻石矿石          │
│  ◼ 深层钻石矿石   钻石块            │
│                                      │
│  ...                                 │
└──────────────────────────────────────┘
```

数据来源：

```java
Registries.ITEM
```

保存的不是翻译名称，而是：

```text
minecraft:diamond
minecraft:diamond_ore
minecraft:ancient_debris
```

这样：

```text
中文客户端
英文客户端
其他语言客户端
```

配置都不会变化。

---

# 6. 实体方块搜索规则

“实体方块”准确含义：

> 搜索目标 Item 对应的世界 Block。

例如选择：

```text
minecraft:diamond_ore
```

它本身属于：

```java
BlockItem
```

所以：

```text
diamond_ore Item
       ↓
diamond_ore Block
       ↓
搜索世界中的钻石矿石
```

---

## 非 BlockItem

如果选择：

```text
minecraft:diamond
```

钻石不是一个：

```java
BlockItem
```

因此：

```text
实体方块
→ 无对应搜索目标
```

不会自动转换成：

```text
钻石
 ↓
钻石矿石
 ↓
深层钻石矿石
```

V1 不做这种关联。

GUI 应显示：

```text
⚠ 钻石不是可放置方块，
“实体方块”探测不可用。
```

并禁用：

```text
☐ 实体方块
```

此时玩家可以选择：

```text
☑ 掉落物
☑ 容器内物品
```

---

# 7. 掉落物

世界中扔在地上的物品属于：

```java
ItemEntity
```

例如玩家配置：

```text
目标：钻石
范围：32
掉落物：✓
```

附近：

```text
玩家
 │
 ├── 8 格 → 地上有钻石
 │
 ├── 14 格 → 地上有钻石
 │
 └── 25 格 → 地上有钻石
```

结果：

```text
8 格钻石
```

Minecraft 1.21.4 的 `EntityView#getEntitiesByClass()` 可以直接按实体类型、Box 和 Predicate 获取范围内的 `ItemEntity`。

底层：

```java
world.getEntitiesByClass(
    ItemEntity.class,
    searchBox,
    entity -> entity.getStack().isOf(targetItem)
);
```

然后再进行球形距离判断。

---

# 8. 容器内物品

UI：

```text
☐ 容器内物品
```

指：

> 世界中方块容器 BlockEntity 内部保存的 ItemStack。

支持：

```text
✅ 箱子
✅ 陷阱箱
✅ 木桶
✅ 潜影盒
✅ 漏斗
✅ 熔炉
✅ 高炉
✅ 烟熏炉
✅ 发射器
✅ 投掷器
✅ 酿造台
✅ 其他兼容 Inventory 的方块容器
```

例如：

```text
目标：钻石

玩家
 │
 ├─ 12 格 → 箱子
 │          └─ 3 钻石
 │
 ├─ 20 格 → 木桶
 │          └─ 64 钻石
 │
 └─ 28 格 → 潜影盒
            └─ 20 钻石
```

结果：

```text
12 格的箱子
```

注意：

> 数量不参与排序。

所以：

```text
5 格箱子：1 个钻石
30 格箱子：64 个钻石
```

仍然选择：

```text
5 格箱子
```

---

# 9. V1 不扫描移动容器实体

第一版“容器”严格定义为：

```text
方块容器 BlockEntity
```

因此：

```text
✅ 普通箱子
✅ 木桶
✅ 潜影盒
```

但暂不支持：

```text
❌ 箱子矿车
❌ 漏斗矿车
❌ 带箱子的船
```

因为这些属于：

```text
Entity
```

而不是：

```text
BlockEntity
```

后续可以作为独立的：

```text
V2：实体容器
```

加入。

---

# 10. 搜索范围

严格按照：

> 玩家当前位置为球心、配置值为半径的三维球体。

例如：

```text
radius = 32
```

候选点：

```text
target = (tx, ty, tz)
player = (px, py, pz)
```

计算：

```java
double dx = tx - px;
double dy = ty - py;
double dz = tz - pz;

double distanceSquared =
    dx * dx +
    dy * dy +
    dz * dz;
```

条件：

```java
distanceSquared <= radius * radius
```

全程不需要：

```java
Math.sqrt()
```

只在最终给玩家显示距离时才开平方。

---

# 11. 三个来源如何选最近

例如：

```text
目标：钻石矿石
范围：48

☑ 实体方块
☑ 掉落物
☑ 容器内物品
```

找到：

```text
实体方块
钻石矿石
距离：17.2

掉落物
钻石矿石 Item
距离：8.4

箱子
里面有钻石矿石 Item
距离：12.7
```

不会按照：

```text
先搜索方块
找到就结束
```

而是三个 Scanner 都产生自己的最近候选：

```text
BlockScanner        → 17.2
DroppedItemScanner  → 8.4
ContainerScanner    → 12.7
```

最后：

```text
SearchService
      ↓
统一比较
      ↓
8.4
```

指南针指向：

```text
掉落物
```

---

# 12. SearchResult

所有来源统一：

```java
public record SearchResult(
    Vec3d target,
    SearchSource source,
    double distanceSquared
) {}
```

来源：

```java
public enum SearchSource {

    BLOCK,

    DROPPED_ITEM,

    CONTAINER
}
```

特别注意：

```text
没有 PLAYER
没有 PLAYER_INVENTORY
没有 ENTITY_INVENTORY
```

因此从代码架构层面就避免误扫描玩家物品。

---

# 13. 配置存储

配置绑定：

> 当前这一只指南针 ItemStack。

使用：

```text
Custom Data Component
```

Minecraft 自 1.20.5 开始已经使用 Data Components 保存这种 ItemStack 级别的结构化持久数据。

定义：

```java
public record CompassConfigComponent(
    Identifier targetItem,
    int radius,
    boolean searchBlocks,
    boolean searchDroppedItems,
    boolean searchContainers
) {}
```

注册：

```text
compass_enhanced:config
```

例如：

```text
指南针 A

目标：diamond_ore
范围：32
方块：true
掉落物：false
容器：false
```

另一只：

```text
指南针 B

目标：diamond
范围：48
方块：false
掉落物：true
容器：true
```

完全互不影响。

---

# 14. 为什么配置不能存在玩家身上

如果保存到玩家：

```text
玩家配置
 ↓
所有指南针共享
```

就无法实现：

```text
指南针 A → 钻石
指南针 B → 远古残骸
指南针 C → 下界合金锭
```

所以必须：

```text
ItemStack
 ↓
CompassConfigComponent
```

让每一只指南针独立。

---

# 15. 客户端与服务端职责

最终架构：

```text
CLIENT
│
├─ GUI
├─ 物品选择器
├─ 鼠标输入
│
│
├── SaveConfig C2S ──────┐
│                        │
└── Scan C2S ────────────┤
                         ▼
                       SERVER
                         │
                         ├─ 验证指南针
                         ├─ 验证配置
                         ├─ BlockScanner
                         ├─ DroppedItemScanner
                         ├─ ContainerScanner
                         └─ 更新指南针目标
```

**任何世界搜索都在服务端执行。**

Fabric 1.21.4 的 `ServerPlayNetworking` 使用 `CustomPayload` 对象 API，C2S payload 需要先通过 `PayloadTypeRegistry.playC2S()` 注册；接收 handler 在服务器线程执行，可以安全操作世界。

---

# 16. 网络 Payload

只有两个核心 Payload。

## SaveCompassConfigPayload

```java
public record SaveCompassConfigPayload(
    Identifier targetItem,
    int radius,
    boolean blocks,
    boolean droppedItems,
    boolean containers
) implements CustomPayload {}
```

流程：

```text
GUI 保存
 ↓
C2S
 ↓
服务器
 ↓
检查玩家主手指南针
 ↓
检查 Item ID
 ↓
检查 Radius
 ↓
检查来源
 ↓
写 CompassConfigComponent
```

---

## ScanCompassPayload

不需要客户端告诉服务端：

```text
目标
范围
来源
```

只发：

```java
public record ScanCompassPayload()
    implements CustomPayload {}
```

服务端自己：

```text
读取玩家主手
 ↓
读取指南针 Component
 ↓
执行搜索
```

防止客户端直接构造：

```text
radius = 10000
```

Fabric 1.21.4 的客户端同样提供 `ClientPlayNetworking` 发送 C2S payload。

---

# 17. 右键行为

最终定义：

```text
手持配置/未配置指南针

右键
 ↓
打开 CompassConfigScreen
```

而：

```text
Shift + 右键
 ↓
保留原版指南针交互
```

这样不会完全破坏原版指南针行为。

---

# 18. 左键行为

配置过指南针：

```text
左键
 ↓
取消本次普通攻击/挖掘
 ↓
发送 ScanCompassPayload
```

普通指南针：

```text
没有 CompassConfigComponent
 ↓
原版行为
```

实现方式：

```text
Client Mixin
 ↓
MinecraftClient#doAttack()
```

只进行非常小范围的输入拦截。

---

# 19. 搜索入口

```java
public final class CompassSearchService {

    public static Optional<SearchResult> search(
        ServerWorld world,
        ServerPlayerEntity player,
        CompassConfigComponent config
    ) {

        // BlockScanner

        // DroppedItemScanner

        // ContainerScanner

        // compare nearest

    }
}
```

---

# 20. BlockScanner

流程：

```text
targetItem
 ↓
是不是 BlockItem？
 ↓
是
 ↓
获取 Block
 ↓
计算涉及 Chunk
 ↓
只读取已加载 Chunk
 ↓
遍历 ChunkSection
 ↓
快速检查目标 Block
 ↓
遍历候选 Section
 ↓
球体过滤
 ↓
记录最近位置
```

性能要求：

```text
禁止为了搜索加载新的 Chunk。
```

---

# 21. DroppedItemScanner

只查：

```java
ItemEntity.class
```

不会查询：

```java
PlayerEntity.class
```

逻辑：

```java
Box searchBox =
    new Box(
        px - radius,
        py - radius,
        pz - radius,
        px + radius,
        py + radius,
        pz + radius
    );
```

然后：

```java
world.getEntitiesByClass(
    ItemEntity.class,
    searchBox,
    itemEntity ->
        itemEntity.getStack().isOf(targetItem)
);
```

由于直接指定了：

```java
ItemEntity.class
```

所以：

```text
PlayerEntity
ArmorStandEntity
ItemFrameEntity
VillagerEntity
```

全部天然排除。Minecraft 1.21.4 的这一实体查询 API 正好支持这种按类型过滤。

---

# 22. ContainerScanner

只遍历：

```text
WorldChunk
 ↓
BlockEntity
```

然后：

```java
if (blockEntity instanceof Inventory inventory)
```

再检查：

```text
inventory
 ↓
ItemStack
 ↓
targetItem
```

关键规则：

```text
ContainerScanner
只能接收 BlockEntity
```

绝对不：

```java
player.getInventory()
```

也不：

```java
world.getPlayers()
```

因此不会因为 `PlayerInventory` 也属于 Inventory 体系，就误把玩家纳入搜索。

---

# 23. 玩家物品排除的代码约束

建议直接把这一条写进代码设计规范：

```text
Compass Search Rule #1

SearchService 及所有 Scanner
不得调用 PlayerInventory 查询接口。
```

禁止出现：

```java
player.getInventory()
```

禁止：

```java
world.getPlayers()
```

禁止：

```java
PlayerEntity instanceof Inventory
```

允许的唯一 ItemStack 来源：

```text
BlockScanner
→ BlockState 对应 BlockItem

DroppedItemScanner
→ ItemEntity#getStack()

ContainerScanner
→ BlockEntity + Inventory#getStack()
```

这样排除规则不是靠：

```text
if 玩家 then skip
```

而是：

> 从架构上根本不把玩家加入候选集合。

这是更安全的实现。

---

# 24. 指南针指向

搜索成功后：

```text
SearchResult
 ↓
目标坐标
 ↓
LODESTONE_TRACKER
```

Minecraft 1.21.4 的：

```java
DataComponentTypes.LODESTONE_TRACKER
```

类型就是：

```java
LodestoneTrackerComponent
```

可以直接利用原版指南针定位机制。

设置类似：

```java
stack.set(
    DataComponentTypes.LODESTONE_TRACKER,
    new LodestoneTrackerComponent(
        Optional.of(
            GlobalPos.create(
                world.getRegistryKey(),
                targetPos
            )
        ),
        false
    )
);
```

因此不需要自己实现：

```text
❌ 指南针旋转算法
❌ 自定义 Shader
❌ 自定义针动画
❌ 玩家朝向转换
```

全部交给原版。

---

# 25. 一次扫描、一次锁定

不持续扫描。

例如：

```text
左键
 ↓
最近目标 A
 ↓
锁定 A
 ↓
指南针持续指向 A
```

即使玩家移动以后：

```text
目标 B 变得更近
```

指南针依然指：

```text
A
```

只有再次：

```text
左键
```

才重新寻找最近目标。

---

# 26. 搜索失败

如果：

```text
32 格范围没有目标
```

ActionBar：

```text
🔍 32 格范围内未找到：钻石
```

同时清除当前增强探测目标：

```java
stack.remove(
    DataComponentTypes.LODESTONE_TRACKER
);
```

避免指南针继续指向旧目标。

---

# 27. 搜索成功反馈

例如：

```text
🔍 已找到 钻石 · 13 格 · 容器
```

或者：

```text
🔍 已找到 钻石矿石 · 21 格 · 方块
```

使用：

```text
ActionBar
```

不刷聊天栏。

---

# 28. 性能限制

固定：

```text
最小范围：8
默认范围：32
最大范围：128
```

并且：

```text
只扫描已加载 Chunk
```

禁止：

```text
指南针扫描
 ↓
强制加载新区块
```

同时增加服务端冷却：

```text
10 Tick
=
0.5 秒
```

即：

```text
疯狂左键
```

不会让服务器连续执行大量世界扫描。

---

# 29. 搜索优先优化

顺序：

```text
① 限制 Radius <= 128

② 不主动加载 Chunk

③ BlockScanner 先筛 ChunkSection

④ 掉落物通过 Entity 索引查询

⑤ ContainerScanner 只遍历 BlockEntity

⑥ 使用 squaredDistance

⑦ 不扫描 Player

⑧ 10 tick 服务端 cooldown
```

第一版不进行异步 World 读取。

---

# 30. 当前维度限制

只搜索：

```text
玩家当前 Dimension
```

例如：

```text
玩家在 Overworld
```

则：

```text
✅ Overworld
❌ Nether
❌ End
```

进入另一个维度以后：

```text
重新左键
```

执行新的探测。

---

# 31. 最终项目结构

```text
src/
│
├── main/
│   ├── java/
│   │   └── com/xxx/compassenhanced/
│   │
│   │       ├── CompassEnhanced.java
│   │
│   │       ├── component/
│   │       │   ├── CompassConfigComponent.java
│   │       │   └── ModComponents.java
│   │
│   │       ├── network/
│   │       │   ├── ModNetworking.java
│   │       │   ├── SaveCompassConfigPayload.java
│   │       │   └── ScanCompassPayload.java
│   │
│   │       └── search/
│   │           ├── CompassSearchService.java
│   │           ├── BlockScanner.java
│   │           ├── DroppedItemScanner.java
│   │           ├── ContainerScanner.java
│   │           ├── SearchResult.java
│   │           └── SearchSource.java
│   │
│   └── resources/
│       ├── fabric.mod.json
│       └── assets/
│           └── compassenhanced/
│               └── lang/
│                   ├── zh_cn.json
│                   └── en_us.json
│
└── client/
    ├── java/
    │   └── com/xxx/compassenhanced/client/
    │
    │       ├── CompassEnhancedClient.java
    │
    │       ├── screen/
    │       │   ├── CompassConfigScreen.java
    │       │   ├── ItemSelectorScreen.java
    │       │   └── ItemGridWidget.java
    │       │
    │       └── mixin/
    │           └── MinecraftClientMixin.java
    │
    └── resources/
        └── compassenhanced.client.mixins.json
```

---

# 32. 最终搜索流程

```text
                          玩家
                           │
                    手持原版指南针
                           │
             ┌─────────────┴─────────────┐
             │                           │
            右键                        左键
             │                           │
             ▼                           ▼
    CompassConfigScreen           Scan Payload
             │                           │
             │                           ▼
      ┌──────┼──────┐                Server
      │      │      │                    │
     目标   范围   来源                   ▼
      │      │      │            CompassSearchService
      └──────┼──────┘                    │
             │              ┌────────────┼────────────┐
             ▼              │            │            │
       Save Payload         ▼            ▼            ▼
             │          BlockScanner  ItemEntity   Container
             ▼                                      Scanner
           Server            │            │            │
             │               └────────────┼────────────┘
             ▼                            │
      Compass Component                   ▼
                                   最近 SearchResult
                                          │
                                          ▼
                                  LodestoneTracker
                                          │
                                          ▼
                                      原版指南针
                                          │
                                          ▼
                                       指向目标
```

整个流程中：

```text
Player Inventory
```

**完全不参与。**

---

# 33. V1 最终功能清单

| 功能 | V1 |
|---|---|
| Minecraft 1.21.4 | ✅ |
| Fabric | ✅ |
| Java 21 | ✅ |
| 增强原版指南针 | ✅ |
| 右键配置 GUI | ✅ |
| 搜索选择物品 | ✅ |
| 8～128 格范围 | ✅ |
| 球形搜索范围 | ✅ |
| 实体方块 | ✅ |
| 地面掉落物 | ✅ |
| 方块容器内部物品 | ✅ |
| 多来源同时启用 | ✅ |
| 找所有来源中最近目标 | ✅ |
| 指南针指向目标 | ✅ |
| 每只指南针独立配置 | ✅ |
| 配置持久保存 | ✅ |
| 左键重新探测 | ✅ |
| 不持续扫描 | ✅ |
| ActionBar 结果提示 | ✅ |
| 不加载新区块 | ✅ |
| 服务端冷却 | ✅ |
| 排除自己背包 | ✅ |
| 排除自己手持物品 | ✅ |
| 排除自己装备 | ✅ |
| 排除其他玩家背包 | ✅ |
| 排除其他玩家手持物品 | ✅ |
| 排除其他玩家装备 | ✅ |
| 箱子矿车 | ❌ |
| 带箱子的船 | ❌ |
| 玩家 Inventory | ❌ |
| 跨维度扫描 | ❌ |
| 自动持续扫描 | ❌ |

---

# 34. 最终定义

这个 Mod 最终可以归纳成：

```text
增强原版指南针

右键
→ 配置目标

左键
→ 一次主动探测

探测范围
→ 玩家为球心的 8~128 格三维球体

合法搜索源
→ 世界实体方块
→ 地面 ItemEntity
→ 方块容器 Inventory

明确禁止
→ 当前玩家物品
→ 其他玩家物品

结果
→ 所有启用来源中距离最近的目标

导航
→ 复用原版 Lodestone Compass 指向机制
```

**最终核心原则：指南针寻找的是“存在于世界环境中的目标”，而不是玩家持有的物品。**
