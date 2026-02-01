# 安卓快速启动软件设计文档

## 1. 产品概述

### 1.1 产品名称
**快启桌面 / QuickLaunch**

### 1.2 产品定位
一款轻量级、高效、可自定义的安卓快速启动器，帮助用户快速访问常用应用，提升手机使用效率。

### 1.3 核心功能
- 首页时间日期农历展示
- 网格布局常用应用快捷启动（按使用频率自动排序）
- **上滑进入应用抽屉**：顶部常用区 + 字母分组列表
- 字母索引快速定位
- 悬浮搜索按钮（支持拼音/首字母搜索）
- 使用频率智能排序（30天统计）
- 应用黑名单管理
- 丰富的自定义设置

### 1.4 页面结构与导航
```
┌─────────────────┐        上滑         ┌───────────────────────────────┐
│      首页       │ ──────────────────→ │         应用抽屉              │
│     (Home)      │                     │       (App Drawer)            │
│                 │ ←────────────────── │                               │
│  时间日期农历   │      下滑返回        │ ┌─────────────────────────┐   │
│  应用网格       │                     │ │    常用区（多行列表）    │   │
│  (频率自动排序) │                     │ │  [图标] 微信            │   │
│                 │                     │ │  [图标] 支付宝          │   │
└─────────────────┘                     │ │  [图标] 抖音            │   │
                                        │ └─────────────────────────┘   │
                                        │ ─────────────────────────────  │
                                        │ ┌─────────────────────────┐   │
                                        │ │ A                   ┌─┐ │   │
                                        │ │ [图标] 爱奇艺       │A│ │   │
                                        │ │ B                   │B│ │   │
                                        │ │ [图标] 百度         │.│ │   │
                                        │ │ ...                 │#│ │   │
                                        │ └─────────────────────┴─┘ │   │
                                        │        ┌────┐             │   │
                                        │        │ 🔍 │ 悬浮搜索    │   │
                                        │        └────┘             │   │
                                        └───────────────────────────────┘
```

### 1.5 目标用户
- 追求效率的安卓用户
- 应用数量较多的用户
- 喜欢简洁桌面的用户

---

## 2. 界面设计

### 2.1 首页 (Home)
```
┌──────────────────────────────────────────┐
│  状态栏 (系统)                             │
├──────────────────────────────────────────┤
│                                          │
│  ┌────────────────────────────────────┐  │
│  │      时间日期区域                   │  │
│  │    10:10                           │  │
│  │    周日 01 2月                      │  │
│  │    农历腊月廿三                     │  │
│  └────────────────────────────────────┘  │
│                                          │
│  ┌────────────────────────────────────┐  │
│  │    常用应用网格区域                  │  │
│  │    (按使用频率自动排序)              │  │
│  │  [图标] [图标] [图标] [图标]        │  │
│  │  [图标] [图标] [图标] [图标]        │  │
│  │  [图标] [图标] [图标] [图标]        │  │
│  └────────────────────────────────────┘  │
│                                          │
│           ↑ 上滑打开应用抽屉              │
└──────────────────────────────────────────┘
```

#### 时间日期区域
| 元素 | 样式 |
|------|------|
| 时间 | 72px, 字重 300, #FFFFFF |
| 日期 | 18px, 字重 400, #FFFFFF/80% |
| 农历 | 14px, 字重 400, #FFFFFF/60% |

#### 应用网格区域
| 属性 | 默认值 | 可配置范围 |
|------|--------|-----------|
| 列数 | 4 | 3-6 |
| 行数 | 4 | 2-6 |
| 图标大小 | 56px | 48-72px |
| 图标间距 | 16px | 8-32px |
| 文字大小 | 12px | 10-14px |
| 垂直偏移 | 0px | -200~200px |

**排序规则**: 按30天使用频率自动排序，无需手动调整
**图标样式**: 圆角 16px, 阴影 0 2px 8px rgba(0,0,0,0.15), 点击缩放 0.95

### 2.2 应用抽屉 (App Drawer)
```
┌─────────────────────────────────────────┐
│  状态栏 (系统)                            │
├─────────────────────────────────────────┤
│                                         │
│  ┌─────────────────────────────────┐    │
│  │ 常用                            │    │
│  │ [图标] 微信                     │    │
│  │ [图标] 支付宝                   │    │
│  │ [图标] 抖音                     │    │
│  │ [图标] 淘宝                     │    │
│  │ [图标] 哔哩哔哩                 │    │
│  └─────────────────────────────────┘    │
│  ─────────────────────────────────────  │
│  A                                 ┌─┐  │
│  [图标] 爱奇艺                     │A│  │
│  [图标] 安全中心                   │B│  │
│  B                                 │C│  │
│  [图标] 百度地图                   │D│  │
│  [图标] 哔哩哔哩                   │.│  │
│  ...                               │.│  │
│  #                                 │#│  │
│  [图标] 12306                      └─┘  │
│                                         │
│  ⚙️ 设置                                │
│                      ┌─────┐            │
│                      │  🔍 │ 悬浮搜索   │
│                      └─────┘            │
└─────────────────────────────────────────┘
```

#### 常用区
| 属性 | 说明 |
|------|------|
| 布局 | 垂直列表，每行一个应用（图标+名称） |
| 排序方式 | 按30天使用频率倒序 |
| 排除项 | 首页已展示的应用 |
| 显示数量 | 可自定义 (默认 5，范围 3-15) |
| 列表项 | 图标 48dp + 名称 16px，高度 56dp |
| 分隔线 | 常用区底部显示分隔线 |

#### 全部应用列表
| 属性 | 说明 |
|------|------|
| 布局 | 字母分组，每组内按频率排序 |
| 列表项 | 图标 48dp + 名称 16px，高度 64dp |
| 字母分组头 | 16dp 高度，灰色背景 |

#### 右侧字母索引栏
| 属性 | 值 |
|------|-----|
| 宽度 | 28px |
| 字符 | A-Z, # |
| 字体大小 | 10px |
| 点击/滑动 | 滚动到对应字母分组 |
| 反馈 | 震动 + 字母放大提示弹窗 |

#### 悬浮搜索按钮
| 属性 | 值 |
|------|-----|
| 大小 | 56dp x 56dp (圆形) |
| 位置 | 右下角，边距 16dp/80dp |
| 背景 | 主色调 #007AFF |
| 阴影 | 0 4px 12px rgba(0,0,0,0.3) |
| 动画 | 点击展开为搜索框，300ms ease-out |

---

## 3. 功能模块设计

### 3.1 应用数据结构

**设计原则**: 数据库实体 (Entity) 与 UI 模型 (AppInfo) 分离，图标不持久化。

```kotlin
/**
 * 数据库实体 - 仅存储需要持久化的字段
 * 注意: 图标从 PackageManager 实时加载，不存储在数据库
 */
@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val lastLaunchTime: Long = 0,
    val firstLetter: String = "#",
    val isSystemApp: Boolean = false,
    val isHidden: Boolean = false,
    val customName: String? = null,
    val customIconUri: String? = null,  // 仅自定义图标存 URI
    val homePosition: Int = -1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * UI 模型 - 包含运行时计算的字段
 */
data class AppInfo(
    val packageName: String,
    val displayName: String,           // customName ?: appName
    val icon: ImageBitmap,             // 运行时从 PackageManager 加载
    val launchCount30d: Int,           // 计算值，来自 daily_stats
    val score: Float,                  // 综合评分，用于排序
    val firstLetter: String,
    val isSystemApp: Boolean,
    val isHidden: Boolean,
    val homePosition: Int
)

/**
 * 应用分类 - 使用系统分类 API
 */
object AppCategoryResolver {
    fun getCategory(pm: PackageManager, packageName: String): Int {
        return try {
            pm.getApplicationInfo(packageName, 0).category
        } catch (e: Exception) {
            ApplicationInfo.CATEGORY_UNDEFINED
        }
    }

    fun getCategoryLabel(category: Int): String = when (category) {
        ApplicationInfo.CATEGORY_GAME -> "游戏"
        ApplicationInfo.CATEGORY_AUDIO -> "音频"
        ApplicationInfo.CATEGORY_VIDEO -> "视频"
        ApplicationInfo.CATEGORY_IMAGE -> "图像"
        ApplicationInfo.CATEGORY_SOCIAL -> "社交"
        ApplicationInfo.CATEGORY_NEWS -> "新闻"
        ApplicationInfo.CATEGORY_MAPS -> "地图"
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> "效率"
        else -> "其他"
    }
}
```

### 3.2 30天使用频率排序算法

**设计原则**: 批量计算避免 N*2 次数据库查询，评分结果缓存。

```kotlin
/**
 * 频率评分计算器
 *
 * 评分公式: 总次数权重(50%) + 时间衰减权重(30%) + 近期活跃权重(20%)
 * - 总次数权重: 近30天启动总次数
 * - 时间衰减权重: 7天半衰期指数衰减 (正确公式: 0.5^(days/7))
 * - 近期活跃权重: 近7天启动次数，反映近期使用趋势
 */
class FrequencyScoreCalculator(
    private val statsDao: DailyStatsDao,
    private val appDao: AppDao
) {
    companion object {
        private const val HALF_LIFE_DAYS = 7.0
        private const val WEIGHT_COUNT_30D = 0.5f
        private const val WEIGHT_TIME_DECAY = 0.3f
        private const val WEIGHT_COUNT_7D = 0.2f
    }

    /**
     * 批量计算所有应用评分 (单次查询，避免 N 个 app 触发 2N 次查询)
     */
    suspend fun calculateAllScores(): Map<String, Float> {
        val now = System.currentTimeMillis()
        val stats30d = statsDao.getAllRecentStats(30)  // 一次查询
        val stats7d = statsDao.getAllRecentStats(7)    // 一次查询
        val lastLaunchTimes = appDao.getAllLastLaunchTimes()

        return stats30d.keys.associateWith { pkg ->
            calculateScore(
                count30d = stats30d[pkg] ?: 0,
                count7d = stats7d[pkg] ?: 0,
                lastLaunchTime = lastLaunchTimes[pkg] ?: 0L,
                now = now
            )
        }
    }

    private fun calculateScore(
        count30d: Int,
        count7d: Int,
        lastLaunchTime: Long,
        now: Long
    ): Float {
        val daysSinceLastLaunch = (now - lastLaunchTime) / 86_400_000.0

        // 正确的7天半衰期衰减公式: 0.5^(days/7)
        // 注意: exp(-days/7) 的半衰期约为 4.85 天，不是 7 天
        val timeDecayScore = (0.5.pow(daysSinceLastLaunch / HALF_LIFE_DAYS) * 100).toFloat()

        return count30d * WEIGHT_COUNT_30D +
               timeDecayScore * WEIGHT_TIME_DECAY +
               count7d * WEIGHT_COUNT_7D
    }
}

// DAO 批量查询接口
@Dao
interface DailyStatsDao {
    @Query("""
        SELECT package_name, SUM(launch_count) as total
        FROM daily_stats
        WHERE date >= date('now', '-' || :days || ' days')
        GROUP BY package_name
    """)
    suspend fun getAllRecentStats(days: Int): Map<String, Int>
}

/**
 * 评分缓存 (5分钟过期)
 */
class ScoreCache(
    private val calculator: FrequencyScoreCalculator,
    private val expirationMs: Long = 5 * 60 * 1000
) {
    private var cachedScores: Map<String, Float> = emptyMap()
    private var lastUpdateTime: Long = 0

    suspend fun getScores(): Map<String, Float> {
        val now = System.currentTimeMillis()
        if (now - lastUpdateTime > expirationMs || cachedScores.isEmpty()) {
            cachedScores = calculator.calculateAllScores()
            lastUpdateTime = now
        }
        return cachedScores
    }

    fun invalidate() {
        lastUpdateTime = 0
    }
}

/**
 * 获取应用抽屉常用区数据（排除首页应用）
 */
suspend fun getDrawerFrequentApps(
    scoreCache: ScoreCache,
    appDao: AppDao,
    excludeHomeApps: Boolean = true,
    limit: Int = 5
): List<AppInfo> {
    val scores = scoreCache.getScores()
    var apps = appDao.getAllApps().filter { !it.isHidden }

    if (excludeHomeApps) {
        val homePackages = appDao.getHomeAppPackages()
        apps = apps.filter { it.packageName !in homePackages }
    }

    return apps
        .sortedByDescending { scores[it.packageName] ?: 0f }
        .take(limit)
}
```

### 3.3 应用启动追踪
```kotlin
/**
 * 记录应用启动并更新统计数据
 */
suspend fun recordAppLaunch(packageName: String, scoreCache: ScoreCache) {
    val today = LocalDate.now().toString()

    // 更新或插入今日统计
    statsDao.upsertDailyStat(
        DailyStat(packageName = packageName, date = today, launchCount = 1)
    )

    // 更新应用最后启动时间
    appDao.updateLastLaunchTime(packageName, System.currentTimeMillis())

    // 使评分缓存失效，下次访问时重新计算
    scoreCache.invalidate()
}

/**
 * 数据清理 - 使用 WorkManager 定期执行
 */
class DataCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @Inject lateinit var statsDao: DailyStatsDao

    override suspend fun doWork(): Result {
        val cutoffDate = LocalDate.now().minusDays(30).toString()
        statsDao.deleteOldStats(cutoffDate)
        return Result.success()
    }
}

// Application 中注册定期任务
fun scheduleDataCleanup(context: Context) {
    val request = PeriodicWorkRequestBuilder<DataCleanupWorker>(
        1, TimeUnit.DAYS
    ).build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "data_cleanup",
        ExistingPeriodicWorkPolicy.KEEP,
        request
    )
}
```

### 3.4 黑名单模块
```kotlin
data class BlacklistConfig(
    val packageNames: Set<String>,    // 黑名单包名列表
    val hideSystemApps: Boolean,      // 是否隐藏所有系统应用
    val hiddenCategories: Set<AppCategory> // 隐藏的应用分类
)

class BlacklistManager(private val appDao: AppDao) {
    suspend fun addToBlacklist(packageName: String)
    suspend fun removeFromBlacklist(packageName: String)
    fun isBlacklisted(packageName: String): Boolean
    suspend fun getFilteredApps(): List<AppInfo>
}
```

### 3.5 搜索模块

**搜索匹配**: 支持名称、拼音全拼、拼音首字母、包名匹配，结果按综合评分排序。

```kotlin
/**
 * 拼音工具类 - 使用 TinyPinyin 库
 */
object PinyinHelper {
    fun getFirstLetter(text: String): String {
        if (text.isEmpty()) return "#"
        val first = text.first()
        return when {
            first.isLetter() && first.code < 128 -> first.uppercaseChar().toString()
            Pinyin.isChinese(first) -> Pinyin.toPinyin(first).first().uppercaseChar().toString()
            else -> "#"
        }
    }

    fun toPinyin(text: String): String {
        return text.map { char ->
            if (Pinyin.isChinese(char)) Pinyin.toPinyin(char).lowercase()
            else char.lowercase().toString()
        }.joinToString("")
    }

    fun getInitials(text: String): String {
        return text.map { getFirstLetter(it.toString()) }.joinToString("").lowercase()
    }
}

/**
 * 搜索应用
 * 匹配优先级: 名称 > 拼音全拼 > 拼音首字母 > 包名
 * 结果按综合评分排序 (非 launchCount30d)
 */
fun searchApps(
    query: String,
    allApps: List<AppInfo>,
    scores: Map<String, Float>
): List<AppInfo> {
    val lowerQuery = query.lowercase()

    return allApps
        .filter { app ->
            val name = app.displayName.lowercase()
            val pinyin = PinyinHelper.toPinyin(app.displayName)
            val initials = PinyinHelper.getInitials(app.displayName)

            name.contains(lowerQuery) ||
            pinyin.contains(lowerQuery) ||
            initials.contains(lowerQuery) ||
            app.packageName.lowercase().contains(lowerQuery)
        }
        .sortedByDescending { scores[it.packageName] ?: 0f }
}
```

### 3.6 字母索引模块
```kotlin
class AlphabetIndex(private val appDao: AppDao) {
    /**
     * 获取按字母分组的应用列表
     * 每组内按30天频率排序
     */
    fun getGroupedApps(apps: List<AppInfo>): Map<String, List<AppInfo>> {
        return apps
            .groupBy { it.firstLetter }
            .mapValues { (_, group) ->
                group.sortedByDescending { calculate30DayScore(it, statsDao) }
            }
            .toSortedMap()
    }

    fun scrollToLetter(letter: String)
}
```

---

## 4. 设置模块设计

### 4.1 设置入口
位于全部应用列表最底部，齿轮图标 + "设置"

### 4.2 设置项

#### 布局设置
| 设置项 | 类型 | 默认值 | 范围 |
|--------|------|--------|------|
| 网格列数 | 滑块 | 4 | 3-6 |
| 网格行数 | 滑块 | 4 | 2-6 |
| 图标大小 | 滑块 | 56px | 48-72px |
| 图标间距 | 滑块 | 16px | 8-32px |
| 垂直偏移 | 滑块 | 0px | -200~200px |
| 首页显示数量 | 滑块 | 16 | 6-36 |
| 抽屉常用区数量 | 滑块 | 5 | 3-15 |

#### 外观设置
| 设置项 | 类型 | 默认值 |
|--------|------|--------|
| 主题模式 | 选择 | 跟随系统 |
| 背景样式 | 选择 | 模糊 |
| 模糊强度 | 滑块 | 20 |
| 图标圆角 | 滑块 | 16px |
| 图标阴影 | 开关 | 开 |

#### 时间日期设置
| 设置项 | 类型 | 默认值 |
|--------|------|--------|
| 显示时间/秒数/日期/农历 | 开关 | 开/关/开/开 |
| 时间格式 | 选择 | 24小时制 |

#### 应用管理设置
| 设置项 | 类型 | 默认值 |
|--------|------|--------|
| 首页排序方式 | 选择 | 使用频率 |
| 常用页排序方式 | 选择 | 使用频率 |
| 隐藏系统应用 | 开关 | 关 |
| 应用黑名单 | 页面跳转 | - |

#### 搜索设置
| 设置项 | 类型 | 默认值 |
|--------|------|--------|
| 启用搜索/拼音搜索/T9搜索 | 开关 | 开/开/关 |
| 显示搜索历史 | 开关 | 开 |

#### 手势设置
| 设置项 | 类型 | 默认值 |
|--------|------|--------|
| 上滑灵敏度 | 滑块 | 中等 |
| 震动反馈 | 开关 | 开 |

#### 高级设置
设为默认桌面 | 备份/恢复设置 | 清除缓存 | 版本信息

### 4.3 设置数据结构
```kotlin
data class AppSettings(
    val layout: LayoutSettings,
    val appearance: AppearanceSettings,
    val clock: ClockSettings,
    val appManagement: AppManagementSettings,
    val search: SearchSettings,
    val gesture: GestureSettings
)

data class LayoutSettings(
    val columns: Int = 4,
    val rows: Int = 4,
    val iconSize: Int = 56,
    val iconSpacing: Int = 16,
    val verticalOffset: Int = 0,
    val homeDisplayCount: Int = 16,
    val drawerFrequentCount: Int = 5
)

data class AppearanceSettings(
    val theme: Theme = Theme.SYSTEM,
    val backgroundType: BackgroundType = BackgroundType.BLUR,
    val backgroundColor: String = "#FFFFFF",
    val backgroundImage: String? = null,
    val blurStrength: Int = 20,
    val iconRadius: Int = 16,
    val showShadow: Boolean = true,
    val textColor: String = "#FFFFFF"
)

enum class Theme { LIGHT, DARK, SYSTEM }
enum class BackgroundType { SOLID, BLUR, IMAGE }
```

---

## 5. 数据存储设计

### 5.1 存储方案
| 数据类型 | 存储方式 |
|----------|----------|
| 应用信息 | SQLite (Room) |
| 设置项 | DataStore Preferences |
| 图标缓存 | 内存 LRU + Coil 磁盘缓存 |

**注意**: 使用 DataStore 替代 SharedPreferences，避免同步 IO 阻塞主线程。

```kotlin
// 依赖
implementation("androidx.datastore:datastore-preferences:1.1.0")

// 设置存储
val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val layoutSettings: Flow<LayoutSettings> = dataStore.data.map { prefs ->
        LayoutSettings(
            columns = prefs[COLUMNS_KEY] ?: 4,
            rows = prefs[ROWS_KEY] ?: 4,
            iconSize = prefs[ICON_SIZE_KEY] ?: 56,
            iconSpacing = prefs[ICON_SPACING_KEY] ?: 16
        )
    }

    suspend fun updateColumns(columns: Int) {
        dataStore.edit { it[COLUMNS_KEY] = columns }
    }

    companion object {
        val COLUMNS_KEY = intPreferencesKey("columns")
        val ROWS_KEY = intPreferencesKey("rows")
        val ICON_SIZE_KEY = intPreferencesKey("icon_size")
        val ICON_SPACING_KEY = intPreferencesKey("icon_spacing")
    }
}
```

### 5.2 数据库设计
```sql
-- 应用信息表 (注意: 不存储 app_icon)
CREATE TABLE apps (
    package_name TEXT PRIMARY KEY,
    app_name TEXT NOT NULL,
    last_launch_time INTEGER DEFAULT 0,
    first_letter TEXT DEFAULT '#',
    is_system_app INTEGER DEFAULT 0,
    is_hidden INTEGER DEFAULT 0,
    custom_icon_uri TEXT,             -- 仅自定义图标存 URI
    custom_name TEXT,
    home_position INTEGER DEFAULT -1,
    created_at INTEGER DEFAULT 0,
    updated_at INTEGER DEFAULT 0
);

-- 每日使用统计表 (保留30天)
CREATE TABLE daily_stats (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    package_name TEXT NOT NULL,
    date TEXT NOT NULL,              -- 格式: YYYY-MM-DD
    launch_count INTEGER DEFAULT 0,
    created_at INTEGER DEFAULT (strftime('%s', 'now')),
    FOREIGN KEY (package_name) REFERENCES apps(package_name) ON DELETE CASCADE,
    UNIQUE(package_name, date)
);

-- 黑名单表
CREATE TABLE blacklist (
    package_name TEXT PRIMARY KEY,
    added_at INTEGER DEFAULT (strftime('%s', 'now')),
    FOREIGN KEY (package_name) REFERENCES apps(package_name) ON DELETE CASCADE
);

-- 索引
CREATE INDEX idx_apps_last_launch ON apps(last_launch_time DESC);
CREATE INDEX idx_apps_home_position ON apps(home_position);
CREATE INDEX idx_apps_first_letter ON apps(first_letter);
CREATE INDEX idx_daily_stats_date ON daily_stats(date);
CREATE INDEX idx_daily_stats_package ON daily_stats(package_name);
```

### 5.3 数据清理策略

使用 WorkManager 定期清理过期数据，避免在应用启动时执行耗时操作。

```kotlin
// 见 3.3 节 DataCleanupWorker 实现
// 推荐: 每日执行一次，清理30天前的统计数据
```

---

## 6. 系统集成

### 6.1 Launcher Activity 配置

**AndroidManifest.xml**:
```xml
<activity
    android:name=".MainActivity"
    android:launchMode="singleTask"
    android:stateNotNeeded="true"
    android:resumeWhilePausing="true"
    android:taskAffinity=""
    android:windowSoftInputMode="adjustPan"
    android:screenOrientation="nosensor"
    android:excludeFromRecents="true"
    android:exported="true">

    <!-- 作为桌面启动器 -->
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.HOME" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>

    <!-- 壁纸设置入口 -->
    <intent-filter>
        <action android:name="android.intent.action.SET_WALLPAPER" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>
```

**关键属性说明**:
| 属性 | 作用 |
|------|------|
| `launchMode="singleTask"` | 避免 Activity 重复创建 |
| `stateNotNeeded="true"` | 不保存实例状态，减少内存占用 |
| `excludeFromRecents="true"` | 不出现在最近任务列表 |
| `taskAffinity=""` | 独立任务栈 |

### 6.2 应用安装/卸载监听

```kotlin
class AppChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return

        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                    // 新应用安装 - 刷新应用列表
                    AppRepository.instance.onAppInstalled(packageName)
                }
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                    // 应用卸载 - 从列表和首页移除
                    AppRepository.instance.onAppUninstalled(packageName)
                }
            }
            Intent.ACTION_PACKAGE_CHANGED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                // 应用更新 - 刷新图标和名称
                AppRepository.instance.onAppUpdated(packageName)
            }
        }
    }
}

// AndroidManifest.xml 注册
<receiver android:name=".AppChangeReceiver" android:exported="false">
    <intent-filter>
        <action android:name="android.intent.action.PACKAGE_ADDED" />
        <action android:name="android.intent.action.PACKAGE_REMOVED" />
        <action android:name="android.intent.action.PACKAGE_CHANGED" />
        <action android:name="android.intent.action.PACKAGE_REPLACED" />
        <data android:scheme="package" />
    </intent-filter>
</receiver>
```

### 6.3 壁纸模糊处理

```kotlin
@Composable
fun WallpaperBackground(blurRadius: Int = 20) {
    val context = LocalContext.current
    val wallpaperManager = remember { WallpaperManager.getInstance(context) }

    var wallpaperBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val drawable = wallpaperManager.drawable
            val bitmap = (drawable as? BitmapDrawable)?.bitmap
            wallpaperBitmap = bitmap?.asImageBitmap()
        }
    }

    wallpaperBitmap?.let { bitmap ->
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Android 12+ 使用 RenderEffect 模糊
                    renderEffect = RenderEffect
                        .createBlurEffect(
                            blurRadius.toFloat(),
                            blurRadius.toFloat(),
                            Shader.TileMode.CLAMP
                        )
                        .asComposeRenderEffect()
                },
            contentScale = ContentScale.Crop
        )
    }
}
```

### 6.4 返回键处理

```kotlin
@Composable
fun LauncherContent(pagerState: PagerState) {
    val coroutineScope = rememberCoroutineScope()
    var isSearchActive by remember { mutableStateOf(false) }

    // Launcher 不应响应返回键退出
    BackHandler(enabled = true) {
        when {
            // 1. 如果在搜索模式，关闭搜索
            isSearchActive -> isSearchActive = false
            // 2. 如果在应用抽屉，返回首页
            pagerState.currentPage == 1 -> {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(0)
                }
            }
            // 3. 如果在首页，什么都不做 (不退出)
            else -> { /* 忽略 */ }
        }
    }
}
```

### 6.5 图标加载 (AdaptiveIcon 兼容)

```kotlin
object IconLoader {
    fun loadAppIcon(
        context: Context,
        packageName: String,
        iconSize: Int = 48
    ): ImageBitmap? {
        val pm = context.packageManager
        return try {
            val drawable = pm.getApplicationIcon(packageName)
            drawableToBitmap(drawable, iconSize)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable, size: Int): ImageBitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 统一处理普通图标和 AdaptiveIconDrawable
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)

        return bitmap.asImageBitmap()
    }
}
```

---

## 7. 交互设计

### 7.1 页面切换 (VerticalPager)

**使用 Compose Foundation VerticalPager 实现两页导航**:

```kotlin
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherPager(viewModel: LauncherViewModel) {
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 2 }  // 首页 + 应用抽屉
    )

    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 1  // 预加载应用抽屉
    ) { page ->
        when (page) {
            0 -> HomePage(viewModel)
            1 -> AppDrawerPage(viewModel)
        }
    }
}
```

### 7.2 手势操作
| 手势 | 功能 | 触发区域 |
|------|------|----------|
| 上滑 | 打开应用抽屉 | 首页任意区域 |
| 下滑 | 返回首页 | 应用抽屉 |
| 点击应用图标 | 启动应用 | 任意页面 |
| 滑动字母索引 | 滚动到对应分组 | 应用抽屉右侧 |

### 7.3 字母索引栏 (滑动选择)

```kotlin
@Composable
fun AlphabetIndexBar(
    letters: List<String> = ('A'..'Z').map { it.toString() } + "#",
    onLetterSelected: (String) -> Unit
) {
    var selectedLetter by remember { mutableStateOf<String?>(null) }
    var showPopup by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .width(28.dp)
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        showPopup = true
                        val index = (offset.y / (size.height / letters.size)).toInt()
                            .coerceIn(0, letters.lastIndex)
                        selectedLetter = letters[index]
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    onDrag = { change, _ ->
                        val index = (change.position.y / (size.height / letters.size)).toInt()
                            .coerceIn(0, letters.lastIndex)
                        if (letters[index] != selectedLetter) {
                            selectedLetter = letters[index]
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    },
                    onDragEnd = {
                        showPopup = false
                        selectedLetter?.let { onLetterSelected(it) }
                    }
                )
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        letters.forEach { letter ->
            Text(
                text = letter,
                fontSize = 10.sp,
                color = if (letter == selectedLetter) Color.Blue else Color.White.copy(alpha = 0.8f),
                modifier = Modifier.clickable { onLetterSelected(letter) }
            )
        }
    }

    // 放大提示弹窗
    if (showPopup && selectedLetter != null) {
        LetterPopup(letter = selectedLetter!!)
    }
}
```

### 7.4 搜索按钮展开动画

```kotlin
@Composable
fun ExpandableSearchButton() {
    var isExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val width by animateDpAsState(
        targetValue = if (isExpanded) 280.dp else 56.dp,
        animationSpec = spring(dampingRatio = 0.8f)
    )

    Box(
        modifier = Modifier
            .padding(16.dp)
            .height(56.dp)
            .width(width)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable { if (!isExpanded) isExpanded = true }
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.Search, contentDescription = "搜索", tint = Color.White)
            AnimatedVisibility(visible = isExpanded) {
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                    textStyle = TextStyle(color = Color.White),
                    singleLine = true
                )
            }
            if (isExpanded) {
                IconButton(onClick = { isExpanded = false; searchQuery = "" }) {
                    Icon(Icons.Default.Close, "关闭", tint = Color.White)
                }
            }
        }
    }

    BackHandler(enabled = isExpanded) { isExpanded = false; searchQuery = "" }
}
```

### 7.5 页面切换动画
| 场景 | 动画 | 时长 |
|------|------|------|
| 上滑进入应用抽屉 | 上滑覆盖 | 300ms ease-out |
| 下滑返回首页 | 下滑退出 | 250ms ease-in |
| 字母索引滚动 | 平滑滚动 | 200ms |
| 搜索按钮展开 | 弹簧动画 | 300ms spring |

### 7.6 震动反馈
| 场景 | 时长 |
|------|------|
| 滑动字母索引 | 10ms |
| 应用启动 | 5ms |
| 搜索框展开 | 15ms |

---

## 8. 技术架构

### 7.1 架构图
```
┌─────────────────────────────────────────────────────────┐
│                      表现层 (UI Layer)                    │
│  ┌──────────────────┐  ┌───────────────────────────┐    │
│  │    首页组件       │  │    应用抽屉组件            │    │
│  │                  │  │  常用区 + 字母分组列表     │    │
│  │                  │  │  字母索引 + 悬浮搜索       │    │
│  └──────────────────┘  └───────────────────────────┘    │
├─────────────────────────────────────────────────────────┤
│                      业务逻辑层 (ViewModel)               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │
│  │ 应用管理器  │  │ 搜索管理器   │  │   设置管理器     │  │
│  └─────────────┘  └─────────────┘  └─────────────────┘  │
├─────────────────────────────────────────────────────────┤
│                      数据访问层 (Repository + DAO)        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │
│  │   AppDao    │  │ DailyStatsDao│  │  BlacklistDao   │  │
│  └─────────────┘  └─────────────┘  └─────────────────┘  │
├─────────────────────────────────────────────────────────┤
│                      系统服务层                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │
│  │ PackageManager│ │ LauncherService│ │  StorageService │  │
│  └─────────────┘  └─────────────┘  └─────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### 7.2 核心类设计
```kotlin
// 主 Activity
class QuickLaunchActivity : AppCompatActivity() {
    private val viewModel: LauncherViewModel by viewModels()
}

// ViewModel
class LauncherViewModel(
    private val appRepository: AppRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {
    val homeApps: StateFlow<List<AppInfo>>           // 首页网格应用
    val drawerFrequentApps: StateFlow<List<AppInfo>> // 抽屉常用区
    val allApps: StateFlow<Map<String, List<AppInfo>>> // 字母分组列表
    val settings: StateFlow<AppSettings>

    fun launchApp(packageName: String)
    fun searchApps(query: String): List<AppInfo>
    fun updateSettings(settings: AppSettings)
}

// Repository
class AppRepository(
    private val appDao: AppDao,
    private val statsDao: DailyStatsDao
) {
    suspend fun loadHomeApps(): List<AppInfo>
    suspend fun loadDrawerFrequentApps(excludeHome: Boolean, limit: Int): List<AppInfo>
    suspend fun loadAllAppsGrouped(): Map<String, List<AppInfo>>
    suspend fun recordLaunch(packageName: String)
}
```

### 7.3 依赖库
| 功能 | 库 | 版本 |
|------|-----|------|
| UI 框架 | Jetpack Compose | 1.5.x |
| 导航 | Jetpack Navigation | 2.7.x |
| 数据库 | Room | 2.6.x |
| 协程 | Kotlin Coroutines | 1.7.x |
| 图片加载 | Coil | 2.5.x |
| 依赖注入 | Hilt | 2.48.x |
| 农历计算 | LunarCalendar | 最新 |
| 拼音转换 | TinyPinyin | 最新 |

---

## 9. 权限需求

| 权限 | 用途 | 必要性 |
|------|------|--------|
| `QUERY_ALL_PACKAGES` | 获取所有应用列表 | 必需 |
| `VIBRATE` | 震动反馈 | 可选 |
| `READ_EXTERNAL_STORAGE` | 读取自定义背景 | 可选 |
| `RECEIVE_BOOT_COMPLETED` | 开机自启 | 可选 |

### 9.1 权限请求流程

```kotlin
@Composable
fun PermissionHandler(onGranted: () -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            onGranted()
        }
    }

    LaunchedEffect(Unit) {
        val requiredPermissions = listOf(
            Manifest.permission.QUERY_ALL_PACKAGES
        )
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            launcher.launch(missingPermissions.toTypedArray())
        } else {
            onGranted()
        }
    }
}
```

### 9.2 备份恢复方案

```kotlin
@Serializable
data class BackupData(
    val version: Int = 1,
    val timestamp: Long,
    val homeApps: List<String>,           // 首页应用包名列表
    val blacklist: List<String>,          // 黑名单包名列表
    val settings: Map<String, String>,    // 设置项
    val customNames: Map<String, String>  // 自定义名称
)

class BackupManager(
    private val appDao: AppDao,
    private val settingsRepository: SettingsRepository
) {
    suspend fun exportBackup(): String {
        val data = BackupData(
            timestamp = System.currentTimeMillis(),
            homeApps = appDao.getHomeAppPackages(),
            blacklist = appDao.getBlacklistPackages(),
            settings = settingsRepository.exportAll(),
            customNames = appDao.getAllCustomNames()
        )
        return Json.encodeToString(data)
    }

    suspend fun importBackup(json: String): Result<Unit> = runCatching {
        val data = Json.decodeFromString<BackupData>(json)
        appDao.restoreHomeApps(data.homeApps)
        appDao.restoreBlacklist(data.blacklist)
        appDao.restoreCustomNames(data.customNames)
        settingsRepository.importAll(data.settings)
    }
}
```

---

## 10. 性能优化

### 10.1 图标内存缓存

```kotlin
object IconCache {
    private val lruCache = object : LruCache<String, ImageBitmap>(
        maxSize = 100  // 最多缓存 100 个图标
    ) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = 1
    }

    fun get(packageName: String): ImageBitmap? = lruCache.get(packageName)
    fun put(packageName: String, icon: ImageBitmap) { lruCache.put(packageName, icon) }
    fun remove(packageName: String) { lruCache.remove(packageName) }
    fun clear() { lruCache.evictAll() }
}

// 使用 Coil 配置图标加载
val imageLoader = ImageLoader.Builder(context)
    .memoryCache {
        MemoryCache.Builder(context)
            .maxSizePercent(0.25)  // 25% 可用内存
            .build()
    }
    .diskCache {
        DiskCache.Builder()
            .directory(context.cacheDir.resolve("icon_cache"))
            .maxSizeBytes(50 * 1024 * 1024)  // 50MB
            .build()
    }
    .build()
```

### 10.2 LazyColumn/Grid 优化

```kotlin
@Composable
fun OptimizedAppList(apps: List<AppInfo>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            items = apps,
            key = { it.packageName },      // 稳定 key
            contentType = { "app_item" }   // 类型提示
        ) { app ->
            AppListItem(
                app = app,
                modifier = Modifier.fillMaxWidth().height(64.dp)
            )
        }
    }
}
```

### 10.3 数据加载优化

```kotlin
class LauncherViewModel(private val appRepository: AppRepository) : ViewModel() {

    // 使用 StateFlow + stateIn 缓存
    val homeApps: StateFlow<List<AppInfo>> = appRepository
        .observeHomeApps()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val frequentApps: StateFlow<List<AppInfo>> = appRepository
        .observeFrequentApps()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
```

### 10.4 优化清单

- **加载优化**: 应用列表异步加载、图标懒加载 + LRU 缓存、数据库索引优化
- **内存优化**: 及时释放资源、避免内存泄漏、页面退出时清理
- **响应优化**: 异步启动应用、DiffUtil 更新列表、频率排序缓存 (见 3.2 节)

---

## 11. 国际化

支持语言: 简体中文 (默认) | 繁体中文 | English

```
res/
├── values/           # 默认 (英文)
├── values-zh/        # 简体中文
├── values-zh-rTW/    # 繁体中文
```

---

## 12. 版本规划

### V1.0 (MVP)
- [x] 首页时间日期农历显示
- [x] 网格布局应用展示（使用频率自动排序）
- [x] 应用抽屉（常用区 + 字母分组列表）
- [x] 上滑打开应用抽屉
- [x] 字母索引快速定位
- [x] 悬浮搜索按钮（支持拼音/首字母）
- [x] 黑名单功能
- [x] 基础设置

### V1.1
- [ ] 搜索功能增强
- [ ] 自定义主题
- [ ] 图标包支持

### V1.2
- [ ] 文件夹功能
- [ ] 小部件支持
- [ ] 备份恢复 / 云端同步

---

## 13. 附录

### 颜色规范
| 模式 | 主背景 | 卡片背景 | 主文字 | 次要文字 | 强调色 |
|------|--------|----------|--------|----------|--------|
| 浅色 | #F5F5F7 | #FFFFFF | #1C1C1E | #8E8E93 | #007AFF |
| 深色 | #000000 | #1C1C1E | #FFFFFF | #8E8E93 | #0A84FF |

### 尺寸规范
- 设计基准: 360dp x 640dp
- 图标: 48dp-72dp
- 文字: 12sp-72sp
- 间距: 4dp, 8dp, 16dp, 24dp, 32dp

---

**文档版本**: 4.0
**更新日期**: 2026-02-01
**作者**: AI Assistant

### 更新记录
- **V4.0**: 交互优化 - 三页合并为两页（首页+应用抽屉）、常用区改为多行列表展示、移除首页字母索引、移除编辑模式（改为频率自动排序）、移除双击锁定手势
- **V3.0**: 实现细节优化 - 数据模型分离、评分算法修正、系统集成、UI 交互实现、性能优化
- **V2.0**: 初始设计文档
