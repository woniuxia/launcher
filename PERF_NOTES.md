# 启动性能基线与调优说明

本文档用于固化当前启动性能优化结果、关键参数与回归方法，避免后续功能迭代时出现性能回退。

## 1. 当前基线（2026-02-11）

基于近期多轮冷启动日志（`StartupTrace`）的稳定区间：

- `S4`（缓存加载）：`11ms ~ 21ms`
- `S5`（Splash 放行）：`13ms ~ 22ms`
- `S8`（冷启动主流程结束）：`12ms ~ 22ms`
- `S2`（schema 迁移）：多数为 `schema migration skipped`
- `S7`（启动全量同步）：多数为 `app sync skipped`（缓存窗口内跳过）

说明：当前首屏链路已从早期 `100ms+` 量级优化到 `20ms` 量级。

---

## 2. 已固化的启动优化策略

### 2.1 冷启动链路异步化
- 冷启动主流程在 `Dispatchers.Default` 执行。
- 设置迁移在 `Dispatchers.IO` 后台执行，不阻塞 Splash 放行。

### 2.2 缓存瘦身
- 首页缓存不再写入全量 `scores`，仅保留首屏必要数据。
- 缓存体积当前稳定在 `~3.3KB`。

### 2.3 推荐刷新去重
- 启动期去除无意义重复刷新。
- 黑/灰名单监听首发事件跳过（`drop(1)`），避免初始化触发多次刷新。

### 2.4 图标预加载两阶段
- `S6A`：优先预加载前 `6` 个关键图标。
- `S6B`：延迟 `300ms` 预加载剩余图标。

### 2.5 启动同步节流
- 缓存命中场景下，若缓存年龄小于 `30分钟`，跳过 `syncInstalledApps()`。
- 通过安装/卸载广播机制维持数据新鲜度。

---

## 3. 关键参数（可调）

定义位置：`app/src/main/java/cn/whc/launcher/ui/viewmodel/LauncherViewModel.kt`

- `POST_START_SYNC_DELAY_MS = 250L`
- `ICON_PRELOAD_PRIMARY_COUNT = 6`
- `ICON_PRELOAD_SECONDARY_DELAY_MS = 300L`
- `STARTUP_SYNC_MIN_INTERVAL_MS = 30 * 60 * 1000L`

调参建议：

- 若首屏后仍有轻微卡顿：先降低 `ICON_PRELOAD_PRIMARY_COUNT`（如 6 -> 4）。
- 若数据更新不及时：降低 `STARTUP_SYNC_MIN_INTERVAL_MS`（如 30min -> 15min）。
- 若后台任务干扰明显：适当提高 `POST_START_SYNC_DELAY_MS`（如 250 -> 400）。

---

## 4. 日志标签与含义

标签：`StartupTrace`、`HomePageCache`

- `S1`：ViewModel 初始化开始
- `S2`：schema 迁移（done/skipped）
- `S3`：冷启动主流程开始
- `S4`：缓存加载耗时
- `S5`：Splash 放行时刻
- `S6A/S6B`：图标预加载两阶段
- `S7`：启动同步（start/done/skipped）
- `S8`：冷启动主流程结束

推荐采集命令：

```bash
adb logcat -v time *:D | findstr "StartupTrace HomePageCache"
```

---

## 5. 回归验收标准

功能改动后建议至少做 5 次冷启动回归：

1. `S5` P95 不高于 `35ms`
2. `S4` P95 不高于 `30ms`
3. 不出现重复推荐刷新风暴
4. 缓存大小保持小文件量级（通常 `< 6KB`）
5. 缓存命中时 `S7` 应多数为 `skipped`

若连续两轮采样超出阈值，视为性能回退，需排查并回滚最近高风险改动。

