package cn.whc.launcher.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.AlarmClock
import android.provider.CallLog
import cn.whc.launcher.data.dao.AppDao
import cn.whc.launcher.data.dao.BlacklistDao
import cn.whc.launcher.data.dao.DailyStatsDao
import cn.whc.launcher.data.entity.AppEntity
import cn.whc.launcher.data.entity.BlacklistEntity
import cn.whc.launcher.data.model.AppInfo
import cn.whc.launcher.util.PinyinHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDao: AppDao,
    private val dailyStatsDao: DailyStatsDao,
    private val blacklistDao: BlacklistDao
) {
    private val packageManager: PackageManager = context.packageManager
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // 评分缓存 - 使用 componentKey (packageName/activityName) 作为键
    private var cachedScores: Map<String, Float> = emptyMap()
    private var lastScoreUpdateTime: Long = 0
    private val scoreExpirationMs: Long = 5 * 60 * 1000 // 5分钟

    // 排序刷新触发器：只有触发时才重新计算排序
    private val _sortRefreshTrigger = MutableStateFlow(0L)

    /**
     * 触发排序刷新（页面重新激活时调用）
     */
    fun triggerSortRefresh() {
        invalidateScoreCache()
        _sortRefreshTrigger.value = System.currentTimeMillis()
    }

    /**
     * 扫描并同步已安装应用到数据库
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
     * 获取首页应用列表 (按频率排序)
     * 排序只在 triggerSortRefresh 时更新，点击应用不会实时改变排序
     */
    fun observeHomeApps(limit: Int): Flow<List<AppInfo>> {
        return combine(
            appDao.getAllApps(),
            blacklistDao.getAllBlacklist(),
            _sortRefreshTrigger
        ) { apps, blacklist, _ ->
            val blacklistKeys = blacklist.map { it.toComponentKey() }.toSet()
            val scores = getScores()

            apps.filter { it.toComponentKey() !in blacklistKeys && !it.isHidden }
                .map { it.toAppInfo(scores[it.toComponentKey()] ?: 0f) }
                .sortedByDescending { it.score }
                .take(limit)
        }.distinctUntilChangedBy { list -> list.map { it.componentKey } }
    }

    /**
     * 获取应用抽屉常用区应用 (排除首页应用)
     * 排序只在 triggerSortRefresh 时更新，点击应用不会实时改变排序
     */
    fun observeFrequentApps(excludeHomeApps: Boolean, limit: Int, homeAppLimit: Int = 0): Flow<List<AppInfo>> {
        return combine(
            appDao.getAllApps(),
            blacklistDao.getAllBlacklist(),
            _sortRefreshTrigger
        ) { apps, blacklist, _ ->
            val blacklistKeys = blacklist.map { it.toComponentKey() }.toSet()
            val scores = getScores()

            // 获取首页应用 componentKey 列表（按 score 排序取前 homeAppLimit 个）
            val homeKeys = if (excludeHomeApps && homeAppLimit > 0) {
                apps.filter { it.toComponentKey() !in blacklistKeys && !it.isHidden }
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

        // 不再调用 invalidateScoreCache()，排序在页面重新激活时更新
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
            // 回退到 getLaunchIntentForPackage
            try {
                val fallbackIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (fallbackIntent != null) {
                    fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(fallbackIntent)
                    true
                } else {
                    false
                }
            } catch (e2: Exception) {
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
        return blacklistDao.getAllBlacklist().map { blacklist ->
            blacklist.mapNotNull { entity ->
                appDao.getApp(entity.packageName, entity.activityName)?.toAppInfo(0f)
            }
        }
    }

    /**
     * 处理应用安装 - 重新扫描该包名下所有 launcher activity
     */
    suspend fun onAppInstalled(packageName: String) = withContext(Dispatchers.IO) {
        val installedApps = getInstalledLaunchableApps()
        val newApps = installedApps.filter { it.packageName == packageName }

        if (newApps.isNotEmpty()) {
            appDao.insertAll(newApps.map { it.toEntity() })
        }
    }

    /**
     * 处理应用卸载 - 删除该包名下所有 activity
     */
    suspend fun onAppUninstalled(packageName: String) = withContext(Dispatchers.IO) {
        appDao.deleteByPackageName(packageName)
        dailyStatsDao.deleteStatsByPackage(packageName)
    }

    /**
     * 处理应用更新 - 重新扫描该包名下所有 launcher activity
     */
    suspend fun onAppUpdated(packageName: String) = withContext(Dispatchers.IO) {
        val installedApps = getInstalledLaunchableApps()
        val updatedApps = installedApps.filter { it.packageName == packageName }

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
    }

    /**
     * 清理过期统计数据
     */
    suspend fun cleanupOldStats() = withContext(Dispatchers.IO) {
        val cutoffDate = LocalDate.now().minusDays(30).format(dateFormatter)
        dailyStatsDao.deleteOldStats(cutoffDate)
    }

    /**
     * 获取评分缓存
     */
    private suspend fun getScores(): Map<String, Float> {
        val now = System.currentTimeMillis()
        if (now - lastScoreUpdateTime > scoreExpirationMs || cachedScores.isEmpty()) {
            cachedScores = calculateAllScores()
            lastScoreUpdateTime = now
        }
        return cachedScores
    }

    /**
     * 使评分缓存失效
     */
    private fun invalidateScoreCache() {
        lastScoreUpdateTime = 0
    }

    /**
     * 批量计算所有应用评分
     */
    private suspend fun calculateAllScores(): Map<String, Float> {
        val startDate30d = LocalDate.now().minusDays(30).format(dateFormatter)
        val startDate7d = LocalDate.now().minusDays(7).format(dateFormatter)

        val stats30d = dailyStatsDao.getStatsSince(startDate30d)
            .associate { "${it.packageName}/${it.activityName}" to it.totalCount }
        val stats7d = dailyStatsDao.getStatsSince(startDate7d)
            .associate { "${it.packageName}/${it.activityName}" to it.totalCount }
        val lastLaunchTimes = appDao.getAllLastLaunchTimes()
            .associate { "${it.packageName}/${it.activityName}" to it.lastLaunchTime }

        val now = System.currentTimeMillis()
        val allKeys = (stats30d.keys + stats7d.keys + lastLaunchTimes.keys).distinct()

        return allKeys.associateWith { key ->
            calculateScore(
                count30d = stats30d[key] ?: 0,
                count7d = stats7d[key] ?: 0,
                lastLaunchTime = lastLaunchTimes[key] ?: 0L,
                now = now
            )
        }
    }

    /**
     * 计算单个应用评分
     *
     * 评分公式: 总次数权重(50%) + 时间衰减权重(30%) + 近期活跃权重(20%)
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

        return count30d * 0.5f + timeDecayScore * 0.3f + count7d * 0.2f
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
        // 回退到应用图标
        try {
            packageManager.getApplicationIcon(packageName)
        } catch (e2: Exception) {
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

    private fun BlacklistEntity.toComponentKey() = "$packageName/$activityName"

    private fun cn.whc.launcher.data.dao.ComponentKey.toComponentKey() = "$packageName/$activityName"
}

// AppInfo 扩展属性
val AppInfo.componentKey: String get() = "$packageName/$activityName"
