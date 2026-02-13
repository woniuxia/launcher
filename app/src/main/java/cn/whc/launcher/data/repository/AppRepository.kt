package cn.whc.launcher.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.AlarmClock
import android.provider.CallLog
import cn.whc.launcher.data.cache.CachedAppInfo
import cn.whc.launcher.data.cache.HomePageCache
import cn.whc.launcher.data.cache.HomePageSnapshot
import cn.whc.launcher.data.dao.AppDao
import cn.whc.launcher.data.dao.BlacklistDao
import cn.whc.launcher.data.dao.DailyStatsDao
import cn.whc.launcher.data.dao.GraylistDao
import cn.whc.launcher.data.dao.LaunchTimeDao
import cn.whc.launcher.data.dao.TimeRecommendationResult
import cn.whc.launcher.data.entity.AppEntity
import cn.whc.launcher.data.entity.BlacklistEntity
import cn.whc.launcher.data.entity.GraylistEntity
import cn.whc.launcher.data.entity.LaunchTimeEntity
import cn.whc.launcher.data.model.AppInfo
import cn.whc.launcher.util.PinyinHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDao: AppDao,
    private val dailyStatsDao: DailyStatsDao,
    private val blacklistDao: BlacklistDao,
    private val graylistDao: GraylistDao,
    private val launchTimeDao: LaunchTimeDao,
    private val homePageCache: HomePageCache
) {
    private val packageManager: PackageManager = context.packageManager
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    companion object {
        private const val TAG = "AppRepository"
        private const val TIME_RECOMMENDATION_WINDOW_MINUTES = 30
        private const val TIME_RECOMMENDATION_CONFIDENCE_THRESHOLD = 0.45f
        private const val TIME_RECOMMENDATION_MIN_LAUNCH_COUNT = 2
    }

    // 评分缓存 - 使用 componentKey (packageName/activityName) 作为键
    // 仅在 triggerSortRefresh() 调用时刷新，不使用时间过期机制
    private var cachedScores: Map<String, Float> = emptyMap()
    private var scoreCacheInvalid: Boolean = true  // 标记缓存是否需要刷新
    private val scoreCacheMutex = Mutex()  // 保护缓存读写

    // 排序刷新触发器：只有触发时才重新计算排序
    private val _sortRefreshTrigger = MutableStateFlow(0L)

    /**
     * 触发排序刷新（页面重新激活时调用）
     */
    fun triggerSortRefresh() {
        scoreCacheInvalid = true
        _sortRefreshTrigger.value = System.currentTimeMillis()
    }

    /**
     * 检查数据库是否有历史数据
     */
    suspend fun hasHistoryData(): Boolean = withContext(Dispatchers.IO) {
        appDao.getAppCount() > 0
    }

    /**
     * 检查是否有有效的首页缓存
     */
    suspend fun hasValidCache(): Boolean {
        return homePageCache.load(HomePageCache.DEFAULT_MAX_AGE_MS) != null
    }

    /**
     * 从缓存加载首页数据
     * @return 缓存的首页快照，如果缓存无效返回 null
     */
    suspend fun loadHomePageFromCache(): HomePageSnapshot? {
        return homePageCache.load()
    }

    /**
     * 将 CachedAppInfo 转换为 AppInfo
     */
    fun CachedAppInfo.toAppInfo() = AppInfo(
        packageName = packageName,
        activityName = activityName,
        displayName = displayName,
        score = score,
        firstLetter = firstLetter
    )

    /**
     * 保存首页快照到缓存
     */
    suspend fun saveHomePageCache(
        homeApps: List<AppInfo>,
        availableLetters: Set<String>,
        timeRecommendations: List<AppInfo>,
        timeRecommendationsTimestamp: Long = System.currentTimeMillis()
    ) {
        val snapshot = HomePageSnapshot(
            homeApps = homeApps.map { it.toCachedAppInfo() },
            availableLetters = availableLetters,
            // 冷启动首屏不依赖全量 scores，避免缓存文件过大导致 JSON 解析变慢
            scores = emptyMap(),
            timeRecommendations = timeRecommendations.map { it.toCachedAppInfo() },
            timeRecommendationsTimestamp = timeRecommendationsTimestamp,
            timestamp = System.currentTimeMillis()
        )
        homePageCache.save(snapshot)
    }

    /**
     * 使缓存失效 (应用安装/卸载/更新时调用)
     */
    suspend fun invalidateHomePageCache() {
        homePageCache.clear()
    }

    /**
     * 扫描并同步已安装应用到数据库
     *
     * 设计目标：
     * 1) 通过集合差分做增量同步，避免全表清空重建；
     * 2) 同步过程中不触发排序刷新，只在显式 triggerSortRefresh() 时重算；
     * 3) 首次同步后预热评分缓存，降低后续首屏排序等待。
     */
    suspend fun syncInstalledApps() = withContext(Dispatchers.IO) {
        val installedApps = getInstalledLaunchableApps()
        val existingKeys = appDao.getAllComponentKeys().map { it.toComponentKey() }.toSet()

        // 新安装的应用
        val installedKeys = installedApps.map { it.componentKey }.toSet()
        val newApps = installedApps.filter { it.componentKey !in existingKeys }
        if (newApps.isNotEmpty()) {
            appDao.insertAll(newApps.map { it.toEntity() })
        }

        // 已卸载的应用
        val removedKeys = existingKeys - installedKeys
        removedKeys.forEach { key ->
            val (pkg, activity) = key.split("/", limit = 2)
            appDao.delete(pkg, activity)
        }

        // 预热评分缓存（首次启动时）
        if (cachedScores.isEmpty()) {
            cachedScores = calculateAllScores()
            scoreCacheInvalid = false
        }
    }

    /**
     * 获取系统中所有可启动的应用
     * 包括同一包名下的多个 launcher activity
     */
    private fun getInstalledLaunchableApps(): List<InstalledApp> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .mapNotNull { resolveInfo ->
                val appInfo = resolveInfo.activityInfo.applicationInfo
                val packageName = appInfo.packageName
                val activityName = resolveInfo.activityInfo.name

                // 排除自己
                if (packageName == context.packageName) return@mapNotNull null

                val appName = resolveInfo.loadLabel(packageManager).toString()
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                InstalledApp(
                    packageName = packageName,
                    activityName = activityName,
                    appName = appName,
                    isSystemApp = isSystemApp
                )
            }
    }

    /**
     * 获取指定包名下所有可启动 Activity（增量同步用）
     */
    private fun getLaunchableAppsByPackage(packageName: String): List<InstalledApp> {
        if (packageName == context.packageName) return emptyList()

        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            `package` = packageName
        }

        return packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .mapNotNull { resolveInfo ->
                val appInfo = resolveInfo.activityInfo.applicationInfo
                val activityName = resolveInfo.activityInfo.name
                val appName = resolveInfo.loadLabel(packageManager).toString()
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                InstalledApp(
                    packageName = packageName,
                    activityName = activityName,
                    appName = appName,
                    isSystemApp = isSystemApp
                )
            }
    }

    /**
     * 获取首页应用列表 (按频率排序)
     * 排序只在 triggerSortRefresh 时更新，点击应用不会实时改变排序
     * 过滤黑名单和灰名单应用
     *
     * 说明：
     * - 这里在内存层做最终排序和截断，保证首页与抽屉的排序口径一致；
     * - distinctUntilChangedBy 只比较 componentKey 顺序，避免分数字段细微变化引发无效重组。
     */
    fun observeHomeApps(limit: Int): Flow<List<AppInfo>> {
        return combine(
            appDao.getAllApps(),
            blacklistDao.getAllBlacklist(),
            graylistDao.getAllGraylist(),
            _sortRefreshTrigger
        ) { apps, blacklist, graylist, _ ->
            val blacklistKeys = blacklist.map { it.toComponentKey() }.toSet()
            val graylistKeys = graylist.map { it.toComponentKey() }.toSet()
            val scores = getScores()

            apps.filter {
                it.toComponentKey() !in blacklistKeys &&
                        it.toComponentKey() !in graylistKeys &&
                        !it.isHidden
            }
                .map { it.toAppInfo(scores[it.toComponentKey()] ?: 0f) }
                .sortedByDescending { it.score }
                .take(limit)
        }.distinctUntilChangedBy { list -> list.map { it.componentKey } }
    }

    /**
     * 获取应用抽屉常用区应用 (排除首页应用)
     * 排序只在 triggerSortRefresh 时更新，点击应用不会实时改变排序
     * 过滤黑名单和灰名单应用
     *
     * 关键点：
     * - 先按统一评分计算首页 TopN，再做排除，确保“首页/常用”互斥且稳定；
     * - homeKeys 仅在需要排除时计算，减少不必要的临时集合创建。
     */
    fun observeFrequentApps(excludeHomeApps: Boolean, limit: Int, homeAppLimit: Int = 0): Flow<List<AppInfo>> {
        return combine(
            appDao.getAllApps(),
            blacklistDao.getAllBlacklist(),
            graylistDao.getAllGraylist(),
            _sortRefreshTrigger
        ) { apps, blacklist, graylist, _ ->
            val blacklistKeys = blacklist.map { it.toComponentKey() }.toSet()
            val graylistKeys = graylist.map { it.toComponentKey() }.toSet()
            val scores = getScores()

            // 获取首页应用 componentKey 列表（按 score 排序取前 homeAppLimit 个）
            val homeKeys = if (excludeHomeApps && homeAppLimit > 0) {
                apps.filter {
                    it.toComponentKey() !in blacklistKeys &&
                            it.toComponentKey() !in graylistKeys &&
                            !it.isHidden
                }
                    .map { it to (scores[it.toComponentKey()] ?: 0f) }
                    .sortedByDescending { it.second }
                    .take(homeAppLimit)
                    .map { it.first.toComponentKey() }
                    .toSet()
            } else {
                emptySet()
            }

            apps.filter {
                it.toComponentKey() !in blacklistKeys &&
                        it.toComponentKey() !in graylistKeys &&
                        !it.isHidden &&
                        it.toComponentKey() !in homeKeys
            }
                .map { it.toAppInfo(scores[it.toComponentKey()] ?: 0f) }
                .sortedByDescending { it.score }
                .take(limit)
        }.distinctUntilChangedBy { list -> list.map { it.componentKey } }
    }

    /**
     * 获取所有应用 (按字母分组)
     * 排序只在 triggerSortRefresh 时更新，点击应用不会实时改变排序
     *
     * 说明：
     * - 分组内按 score 降序，分组键按 A-Z 排序，'#' 固定置后；
     * - distinctUntilChangedBy 只比较每组 componentKey 顺序，减少 UI 层无意义刷新。
     */
    fun observeAllAppsGrouped(): Flow<Map<String, List<AppInfo>>> {
        return combine(
            appDao.getAllApps(),
            blacklistDao.getAllBlacklist(),
            _sortRefreshTrigger
        ) { apps, blacklist, _ ->
            val blacklistKeys = blacklist.map { it.toComponentKey() }.toSet()
            val scores = getScores()

            apps.filter { it.toComponentKey() !in blacklistKeys && !it.isHidden }
                .map { it.toAppInfo(scores[it.toComponentKey()] ?: 0f) }
                .groupBy { it.firstLetter }
                .mapValues { (_, group) -> group.sortedByDescending { it.score } }
                .toSortedMap(compareBy { if (it == "#") "\uFFFF" else it })
        }.distinctUntilChangedBy { map ->
            // 比较每个分组内的 componentKey 顺序
            map.mapValues { (_, list) -> list.map { it.componentKey } }
        }
    }

    /**
     * 获取所有应用列表 (用于搜索)
     */
    suspend fun getAllApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val blacklistEntities = blacklistDao.getAllBlacklistList()
        val blacklistKeys = blacklistEntities.map { it.toComponentKey() }.toSet()
        val scores = getScores()

        appDao.getAllAppsList()
            .filter { it.toComponentKey() !in blacklistKeys && !it.isHidden }
            .map { it.toAppInfo(scores[it.toComponentKey()] ?: 0f) }
    }

    /**
     * 记录应用启动
     * 注意：只更新数据库，不触发排序刷新，排序在页面重新激活时更新
     */
    suspend fun recordAppLaunch(packageName: String, activityName: String) = withContext(Dispatchers.IO) {
        val today = LocalDate.now().format(dateFormatter)
        val now = System.currentTimeMillis()

        // 更新应用最后启动时间
        appDao.updateLastLaunchTime(packageName, activityName, now)

        // 增加今日启动次数
        dailyStatsDao.incrementLaunchCount(packageName, activityName, today)

        // 记录精确启动时间
        recordLaunchTime(packageName, activityName, now)
    }

    /**
     * 记录启动时间到 launch_time_records 表
     */
    private suspend fun recordLaunchTime(packageName: String, activityName: String, timestamp: Long) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }
        val timeOfDayMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        launchTimeDao.insert(
            LaunchTimeEntity(
                packageName = packageName,
                activityName = activityName,
                launchTimestamp = timestamp,
                timeOfDayMinutes = timeOfDayMinutes
            )
        )
    }

    /**
     * 获取时间段推荐应用 (Top 5)
     * 算法: 当前时间 +-30分钟窗口内启动次数最多的应用，不足5个用最近使用的应用补充
     * SQL 层直接过滤黑名单、灰名单和隐藏应用
     *
     * 说明：
     * - 先在 SQL 侧完成时间窗口过滤、聚合和 TopN，避免全量记录回传内存；
     * - 再用 launchCount / weightedScore 双阈值兜底，防止低置信度推荐进入结果；
     * - 最后使用最近应用补齐到 5 个，保证入口稳定可用。
     */
    suspend fun getTimeBasedRecommendations(): List<AppInfo> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val isWeekend = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY, Calendar.SUNDAY -> 1
            else -> 0
        }

        // 计算时间窗口: 当前时间 +-30分钟
        val windowSize = TIME_RECOMMENDATION_WINDOW_MINUTES
        val startMinutes = (currentMinutes - windowSize + 1440) % 1440
        val endMinutes = (currentMinutes + windowSize) % 1440

        // 30天前的时间戳
        val cutoffTimestamp = now - 30L * 24 * 60 * 60 * 1000

        // 判断是否跨天
        val isCrossDay = startMinutes > endMinutes

        // 单次 SQL 查询: JOIN apps + 排除黑灰名单 + LIMIT 5
        val recommendationsRaw = if (isCrossDay) {
            launchTimeDao.getTimeRecommendationsCrossDay(
                startMinutes = startMinutes,
                endMinutes = endMinutes,
                cutoffTimestamp = cutoffTimestamp,
                currentMinutes = currentMinutes,
                windowMinutes = windowSize,
                nowTimestamp = now,
                isWeekend = isWeekend
            )
        } else {
            launchTimeDao.getTimeRecommendations(
                startMinutes = startMinutes,
                endMinutes = endMinutes,
                cutoffTimestamp = cutoffTimestamp,
                currentMinutes = currentMinutes,
                windowMinutes = windowSize,
                nowTimestamp = now,
                isWeekend = isWeekend
            )
        }

        val recommendations = recommendationsRaw.filter {
            it.launchCount >= TIME_RECOMMENDATION_MIN_LAUNCH_COUNT ||
                it.weightedScore >= TIME_RECOMMENDATION_CONFIDENCE_THRESHOLD
        }

        val timeBasedApps = recommendations.map { it.toAppInfo() }

        // 如果不足5个，用最近使用的应用补充
        if (timeBasedApps.size < 5) {
            val excludeKeys = timeBasedApps.map { it.componentKey }
            val supplementEntities = appDao.getSupplementApps(
                excludeKeys = excludeKeys,
                limit = 5 - timeBasedApps.size
            )
            val supplementApps = supplementEntities.map { it.toAppInfo(0f) }
            return@withContext timeBasedApps + supplementApps
        }

        timeBasedApps
    }

    /**
     * 启动应用
     * 使用 ComponentName 精确启动指定的 Activity
     */
    fun launchApp(packageName: String, activityName: String): Boolean {
        // 电话应用特殊处理：打开最近通话记录
        if (isDialerApp(packageName)) {
            if (openRecentCalls()) {
                return true
            }
        }

        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName(packageName, activityName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Timber.tag(TAG).d(e, "ComponentName launch failed, trying fallback: %s/%s", packageName, activityName)
            // 回退到 getLaunchIntentForPackage
            try {
                val fallbackIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (fallbackIntent != null) {
                    fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(fallbackIntent)
                    true
                } else {
                    Timber.tag(TAG).w("No launch intent found for package: %s", packageName)
                    false
                }
            } catch (e2: Exception) {
                Timber.tag(TAG).e(e2, "Failed to launch app: %s/%s", packageName, activityName)
                false
            }
        }
    }

    /**
     * 判断是否为电话/拨号应用
     */
    private fun isDialerApp(packageName: String): Boolean {
        val dialerPackages = listOf(
            "com.android.dialer",           // AOSP 拨号器
            "com.google.android.dialer",    // Google 拨号器
            "com.samsung.android.dialer",   // 三星拨号器
            "com.samsung.android.incallui", // 三星通话
            "com.huawei.contacts",          // 华为联系人/拨号
            "com.oppo.dialer",              // OPPO 拨号器
            "com.coloros.phonemanager",     // ColorOS 电话
            "com.vivo.contacts",            // vivo 联系人/拨号
            "com.miui.contacts",            // 小米联系人/拨号
            "com.oneplus.dialer",           // 一加拨号器
            "com.asus.contacts"             // 华硕联系人
        )
        return packageName in dialerPackages
    }

    /**
     * 打开最近通话记录
     */
    private fun openRecentCalls(): Boolean {
        // 方案1: 使用 CallLog 打开通话记录
        val callLogIntent = Intent(Intent.ACTION_VIEW).apply {
            type = CallLog.Calls.CONTENT_TYPE
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (tryStartActivity(callLogIntent)) {
            return true
        }

        // 方案2: 使用 ACTION_DIAL 打开拨号器（通常显示最近通话）
        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return tryStartActivity(dialIntent)
    }

    /**
     * 打开系统时钟应用
     * Android 16 兼容：多重 fallback 方案
     */
    fun openClock(): Boolean {
        // 方案1: 标准 ACTION_SHOW_ALARMS
        if (tryStartActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS))) {
            return true
        }

        // 方案2: 尝试常见时钟应用包名
        val clockPackages = listOf(
            "com.google.android.deskclock",      // Google 时钟
            "com.android.deskclock",             // AOSP 时钟
            "com.sec.android.app.clockpackage",  // 三星时钟
            "com.huawei.deskclock",              // 华为时钟
            "com.oppo.clock",                    // OPPO 时钟
            "com.coloros.alarmclock",            // ColorOS 时钟
            "com.vivo.clock",                    // vivo 时钟
            "com.miui.clock",                    // 小米时钟
            "com.oneplus.deskclock",             // 一加时钟
            "com.asus.deskclock"                 // 华硕时钟
        )

        for (pkg in clockPackages) {
            val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null && tryStartActivity(launchIntent)) {
                return true
            }
        }

        // 方案3: 尝试 ACTION_SET_ALARM（部分时钟应用支持）
        if (tryStartActivity(Intent(AlarmClock.ACTION_SET_ALARM))) {
            return true
        }

        // 方案4: 打开系统设置的日期时间页面
        if (tryStartActivity(Intent(android.provider.Settings.ACTION_DATE_SETTINGS))) {
            return true
        }

        return false
    }

    /**
     * 安全启动 Activity
     */
    private fun tryStartActivity(intent: Intent): Boolean {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // 检查是否有应用能处理此 Intent
            if (intent.resolveActivity(packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to start activity with intent: %s", intent.action)
            false
        }
    }

    /**
     * 添加到黑名单
     */
    suspend fun addToBlacklist(packageName: String, activityName: String) = withContext(Dispatchers.IO) {
        blacklistDao.insert(BlacklistEntity(packageName, activityName))
        appDao.updateHidden(packageName, activityName, true)
    }

    /**
     * 从黑名单移除
     */
    suspend fun removeFromBlacklist(packageName: String, activityName: String) = withContext(Dispatchers.IO) {
        blacklistDao.delete(packageName, activityName)
        appDao.updateHidden(packageName, activityName, false)
    }

    /**
     * 获取黑名单应用列表
     */
    fun observeBlacklist(): Flow<List<AppInfo>> {
        return blacklistDao.getBlacklistedApps().map { apps ->
            apps.map { it.toAppInfo(0f) }
        }
    }

    /**
     * 添加到灰名单
     * 灰名单应用不在首页、常用应用和推荐应用中展示，但在抽屉页面可见
     */
    suspend fun addToGraylist(packageName: String, activityName: String) = withContext(Dispatchers.IO) {
        graylistDao.insert(GraylistEntity(packageName, activityName))
    }

    /**
     * 从灰名单移除
     */
    suspend fun removeFromGraylist(packageName: String, activityName: String) = withContext(Dispatchers.IO) {
        graylistDao.delete(packageName, activityName)
    }

    /**
     * 获取灰名单应用列表
     */
    fun observeGraylist(): Flow<List<AppInfo>> {
        return graylistDao.getGraylistedApps().map { apps ->
            apps.map { it.toAppInfo(0f) }
        }
    }

    /**
     * 处理应用安装 - 重新扫描该包名下所有 launcher activity
     */
    suspend fun onAppInstalled(packageName: String) = withContext(Dispatchers.IO) {
        val newApps = getLaunchableAppsByPackage(packageName)

        if (newApps.isNotEmpty()) {
            appDao.insertAll(newApps.map { it.toEntity() })
        }
        // 使缓存失效
        invalidateHomePageCache()
    }

    /**
     * 处理应用卸载 - 删除该包名下所有 activity
     */
    suspend fun onAppUninstalled(packageName: String) = withContext(Dispatchers.IO) {
        appDao.deleteByPackageName(packageName)
        dailyStatsDao.deleteStatsByPackage(packageName)
        // 使缓存失效
        invalidateHomePageCache()
    }

    /**
     * 处理应用更新 - 重新扫描该包名下所有 launcher activity
     *
     * 增量策略：
     * - 先删掉该包中已不存在的 activity；
     * - 再对仍存在的 activity 执行“更新名称或插入新项”；
     * - 最后统一失效首页缓存，避免旧快照展示过期入口。
     */
    suspend fun onAppUpdated(packageName: String) = withContext(Dispatchers.IO) {
        val updatedApps = getLaunchableAppsByPackage(packageName)

        // 获取现有的 activity
        val existingKeys = appDao.getAllComponentKeys()
            .filter { it.packageName == packageName }
            .map { it.toComponentKey() }
            .toSet()

        val newKeys = updatedApps.map { it.componentKey }.toSet()

        // 删除不再存在的 activity
        val removedKeys = existingKeys - newKeys
        removedKeys.forEach { key ->
            val (pkg, activity) = key.split("/", limit = 2)
            appDao.delete(pkg, activity)
        }

        // 更新或插入 activity
        updatedApps.forEach { app ->
            val existing = appDao.getApp(app.packageName, app.activityName)
            if (existing != null) {
                appDao.updateAppName(app.packageName, app.activityName, app.appName)
            } else {
                appDao.insert(app.toEntity())
            }
        }
        // 使缓存失效
        invalidateHomePageCache()
    }

    /**
     * 清理过期统计数据
     */
    suspend fun cleanupOldStats() = withContext(Dispatchers.IO) {
        val cutoffDate = LocalDate.now().minusDays(30).format(dateFormatter)
        val cutoffTimestamp = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000

        dailyStatsDao.deleteOldStats(cutoffDate)
        launchTimeDao.deleteOldRecords(cutoffTimestamp)
    }

    /**
     * 获取评分缓存 (线程安全)
     * 仅在缓存被显式失效时重新计算，避免排序抖动
     *
     * 注意：
     * - 该函数是排序链路的唯一入口，必须保证并发安全；
     * - 通过 Mutex 串行化重算，避免多个订阅方并发触发重复计算。
     */
    private suspend fun getScores(): Map<String, Float> = scoreCacheMutex.withLock {
        if (scoreCacheInvalid || cachedScores.isEmpty()) {
            cachedScores = calculateAllScores()
            scoreCacheInvalid = false
        }
        cachedScores
    }

    /**
     * 批量计算所有应用评分 (优化版: 减少数据库查询)
     */
    private suspend fun calculateAllScores(): Map<String, Float> {
        val startDate30d = LocalDate.now().minusDays(30).format(dateFormatter)
        val startDate7d = LocalDate.now().minusDays(7).format(dateFormatter)

        // 单次查询获取 30天 和 7天 统计
        val scoreStats = dailyStatsDao.getScoreStats(startDate30d, startDate7d)
            .associate { "${it.packageName}/${it.activityName}" to (it.count30d to it.count7d) }

        // 获取最后启动时间
        val lastLaunchTimes = appDao.getAllLastLaunchTimes()
            .associate { "${it.packageName}/${it.activityName}" to it.lastLaunchTime }

        val now = System.currentTimeMillis()
        val allKeys = (scoreStats.keys + lastLaunchTimes.keys).distinct()

        return allKeys.associateWith { key ->
            val (count30d, count7d) = scoreStats[key] ?: (0 to 0)
            calculateScore(
                count30d = count30d,
                count7d = count7d,
                lastLaunchTime = lastLaunchTimes[key] ?: 0L,
                now = now
            )
        }
    }

    /**
     * 计算单个应用评分
     *
     * 评分公式: 总次数权重(50%) + 频率加权时间衰减(30%) + 近期活跃权重(20%)
     *
     * 时间衰减采用频率加权策略：
     * - freqFactor = min(count30d / 30, 1)，月均1次/天为满分
     * - adjustedDecay = timeDecayScore × freqFactor
     * - 效果：低频应用"刚用过"的加成被大幅削弱，避免偶尔使用的应用排名过高
     */
    private fun calculateScore(
        count30d: Int,
        count7d: Int,
        lastLaunchTime: Long,
        now: Long
    ): Float {
        val daysSinceLastLaunch = if (lastLaunchTime > 0) {
            (now - lastLaunchTime) / 86_400_000.0
        } else {
            30.0
        }

        // 7天半衰期衰减公式: 0.5^(days/7)
        val timeDecayScore = (0.5.pow(daysSinceLastLaunch / 7.0) * 100).toFloat()

        // 频率因子：月均1次/天(30次)为满分1.0，低于此按比例降低
        val freqFactor = (count30d / 30f).coerceIn(0f, 1f)

        // 时间衰减 × 频率因子，低频应用的"刚用过"加成被削弱
        val adjustedDecay = timeDecayScore * freqFactor

        return count30d * 0.5f + adjustedDecay * 0.3f + count7d * 0.2f
    }

    /**
     * 加载应用图标 - 使用 ActivityInfo.loadIcon 获取 Activity 专属图标
     */
    fun loadAppIcon(packageName: String, activityName: String) = try {
        val activityInfo = packageManager.getActivityInfo(
            ComponentName(packageName, activityName),
            PackageManager.GET_META_DATA
        )
        activityInfo.loadIcon(packageManager)
    } catch (e: Exception) {
        Timber.tag(TAG).d(e, "Activity icon not found, trying app icon: %s/%s", packageName, activityName)
        // 回退到应用图标
        try {
            packageManager.getApplicationIcon(packageName)
        } catch (e2: Exception) {
            Timber.tag(TAG).w(e2, "Failed to load icon for: %s", packageName)
            null
        }
    }

    private data class InstalledApp(
        val packageName: String,
        val activityName: String,
        val appName: String,
        val isSystemApp: Boolean
    ) {
        val componentKey: String get() = "$packageName/$activityName"
    }

    private fun InstalledApp.toEntity() = AppEntity(
        packageName = packageName,
        activityName = activityName,
        appName = appName,
        firstLetter = PinyinHelper.getFirstLetter(appName),
        isSystemApp = isSystemApp
    )

    private fun AppEntity.toAppInfo(score: Float) = AppInfo(
        packageName = packageName,
        activityName = activityName,
        displayName = customName ?: appName,
        launchCount30d = 0,
        score = score,
        firstLetter = firstLetter,
        isSystemApp = isSystemApp,
        isHidden = isHidden,
        homePosition = homePosition,
        lastLaunchTime = lastLaunchTime
    )

    private fun AppEntity.toComponentKey() = "$packageName/$activityName"

    private fun TimeRecommendationResult.toAppInfo() = AppInfo(
        packageName = packageName,
        activityName = activityName,
        displayName = customName ?: appName,
        score = weightedScore.toFloat(),
        firstLetter = firstLetter
    )

    private fun BlacklistEntity.toComponentKey() = "$packageName/$activityName"

    private fun GraylistEntity.toComponentKey() = "$packageName/$activityName"

    private fun cn.whc.launcher.data.dao.ComponentKey.toComponentKey() = "$packageName/$activityName"

    private fun AppInfo.toCachedAppInfo() = CachedAppInfo(
        packageName = packageName,
        activityName = activityName,
        displayName = displayName,
        score = score,
        firstLetter = firstLetter
    )
}

// AppInfo 扩展属性
val AppInfo.componentKey: String get() = "$packageName/$activityName"
