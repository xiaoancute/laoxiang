# IP位置显示平滑跟随优化

## 问题描述

IP属地显示在玩家头顶上时更新不流畅，出现"一卡一卡"的现象，显示实体无法实时跟随玩家移动。

## 根本原因

原设计中 `tickInterval` 默认值为 `5` ticks，意味着每5个游戏刻（约0.25秒）才更新一次TextDisplay实体的位置。这个更新频率对于跟随快速移动的玩家来说太慢了，导致视觉上的卡顿和延迟。

## 解决方案

### 方案1：每tick更新（推荐）

将 `tickInterval` 默认值改为 `1`，即每个游戏刻（约0.05秒/50ms）都更新一次位置。

**优点：**
- 完全流畅的跟随效果
- 实时响应玩家移动
- 视觉效果最佳

**性能影响：**
- 每个玩家每tick执行一次简单的位置更新和实体传送
- 对于现代服务器来说，这个开销可以忽略不计
- 即使有100个在线玩家，每秒也只是2000次简单的Vec3计算和实体传送

### 方案2：使用Minecraft内置的跟随机制

不使用定时更新，而是利用以下机制之一：

**选项A：使用实体乘客系统（Passenger）**
- 将TextDisplay设置为玩家的乘客实体
- 客户端会自动处理位置同步
- 需要手动设置偏移量

**优点：**
- 零服务器端更新成本
- 完美的实时跟随

**缺点：**
- 可能影响玩家的碰撞箱和交互
- 某些客户端mod可能不兼容
- 垂直偏移控制较复杂

**选项B：客户端插值**
- 仅在玩家显著移动时更新（例如距离 > 0.5方块）
- 依赖客户端的实体位置插值

### 方案3：自适应更新频率

根据玩家移动状态动态调整更新频率：

```java
// 伪代码
if (player.isMoving() || player.getVelocity().lengthSquared() > 0.01) {
    updateInterval = 1; // 移动时每tick更新
} else {
    updateInterval = 20; // 静止时每秒更新一次
}
```

**优点：**
- 在保证流畅度的同时优化性能
- 静止玩家减少不必要的更新

**缺点：**
- 实现复杂度增加
- 需要跟踪每个玩家的移动状态

## 推荐实现

**采用方案1**：将 `tickInterval` 默认值改为 `1`

理由：
1. 实现简单，代码改动最小
2. 性能影响可以忽略不计
3. 用户体验最佳
4. 服务器管理员可以根据需要在配置中调整该值

## 配置变更

### 设计文档更新

`docs/superpowers/specs/2026-06-22-minecraft-ip-location-display-design.md`

```markdown
- `tickInterval`: update frequency, default `1` tick for smooth real-time following.
```

### 实现建议

在 `IpLocationConfig.java` 中：

```java
public static final ForgeConfigSpec.IntValue TICK_INTERVAL = BUILDER
    .comment("Number of ticks between display position updates. Lower = smoother following, higher = less server load.",
             "1 tick = ~50ms. Default: 1 (update every tick for smooth real-time following)",
             "Recommended range: 1-5")
    .defineInRange("tickInterval", 1, 1, 100);
```

在 `PlayerDisplayManager.java` 中：

```java
private int tickCounter = 0;
private final int tickInterval;

public void onServerTick(TickEvent.ServerTickEvent event) {
    if (event.phase != TickEvent.Phase.END) return;
    
    tickCounter++;
    if (tickCounter < tickInterval) return;
    tickCounter = 0;
    
    // 更新所有显示实体位置
    for (Map.Entry<UUID, Display.TextDisplay> entry : displays.entrySet()) {
        ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
        if (player != null) {
            Display.TextDisplay display = entry.getValue();
            Vec3 targetPos = player.position().add(0, verticalOffset, 0);
            display.setPos(targetPos);
            // 同步到客户端
            display.level().getChunkSource().broadcastAndSend(display, 
                new ClientboundTeleportEntityPacket(display));
        }
    }
}
```

## 性能基准参考

假设服务器TPS为20（正常情况）：

| 在线玩家数 | tickInterval=5 | tickInterval=1 | 性能差异 |
|-----------|----------------|----------------|----------|
| 10        | 40次/秒        | 200次/秒       | +160次/秒 |
| 50        | 200次/秒       | 1000次/秒      | +800次/秒 |
| 100       | 400次/秒       | 2000次/秒      | +1600次/秒 |

每次更新操作包括：
- 1次Vec3加法（玩家位置 + 偏移）
- 1次实体传送
- 1次数据包广播（仅发送给附近的玩家）

在现代Java和Minecraft服务器上，这些操作的开销极小（< 0.01ms per operation）。

## 测试验证

实现后需要验证：

1. **视觉流畅度测试**
   - 玩家行走时，头顶显示应无明显延迟
   - 玩家快速移动（疾跑、飞行）时，显示应紧密跟随
   - 不应出现"跳跃"或"卡顿"现象

2. **性能测试**
   - 使用多个玩家测试服务器TPS是否稳定
   - 监控服务器tick耗时，确认没有显著增加

3. **配置测试**
   - 验证服务器管理员可以通过配置文件调整 `tickInterval`
   - 测试不同的 `tickInterval` 值（1, 2, 5, 10）的视觉效果差异

## 后续优化（可选）

如果在高玩家数量服务器上发现性能问题，可以考虑：

1. **渲染距离限制**：只对附近玩家渲染显示实体
2. **批量更新**：将多个实体更新合并为一次数据包
3. **自适应频率**：根据服务器TPS动态调整更新频率
4. **客户端插值**：利用客户端的实体位置插值减少服务器更新频率
