package cn.whc.launcher.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.AlarmClock
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
import kotlinx.coroutines.flow.combine
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

    // 评分缓存
    private var cachedScores: Map<String, Float> = emptyMap()
    private var lastScoreUpdateTime: Long = 0
    private val scoreExpirationMs: Long = 5 * 60 * 1000 // 5分钟

    /**
     * 扫描并同步已安装应用到数据库
     */
    suspend fun syncInstalledApps() = withContext(Dispatchers.IO) {
        val installedApps = getInstalledLaunchableApps()
        val existingPackages = appDao.getAllPackageNames().toSet()

        // 新安装的应用
        val newApps = installedApps.filter { it.packageName !in existingPackages }
        if (newApps.isNotEmpty()) {
            appDao.insertAll(newApps.map { it.toEntity() })
        }

        // 已卸载的应用
        val installedPackages = installedApps.map { it.packageName }.toSet()
        val removedPackages = existingPackages - installedPackages
        removedPackages.forEach { pkg ->
            appDao.deleteByPackageName(pkg)
        }
    }

    /**
     * 获取系统中所有可启动的应用
     */
    private fun getInstalledLaunchableApps(): List<InstalledApp> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .mapNotNull { resolveInfo ->
                val appInfo = resolveInfo.activityInfo.applicationInfo
                val packageName = appInfo.packageName

                // 排除自己
                if (packageName == context.packageName) return@mapNotNull null

                val appName = resolveInfo.loadLabel(packageManager).toString()
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                InstalledApp(
                    packageName = packageName,
                    appName = appName,
                    isSystemApp = isSystemApp
                )
            }
    }

    /**
     * 获取首页应用列表 (按频率排序)
     */
    fun observeHomeApps(limit: Int): Flow<List<AppInfo>> {
        return appDao.getAllApps().combine(blacklistDao.getAllBlacklist()) { apps, blacklist ->
            val blacklistPackages = blacklist.map { it.packageName }.toSet()
            val scores = getScores()

            apps.filter { it.packageName !in blacklistPackages && !it.isHidden }
                .map { it.toAppInfo(scores[it.packageName] ?: 0f) }
                .sortedByDescending { it.score }
                .take(limit)
        }
    }

    /**
     * 获取应用抽屉常用区应用 (排除首页应用)
     */
    fun observeFrequentApps(excludeHomeApps: Boolean, limit: Int): Flow<List<AppInfo>> {
        return appDao.getAllApps().combine(blacklistDao.getAllBlacklist()) { apps, blacklist ->
            val blacklistPackages = blacklist.map { it.packageName }.toSet()
            val homePackages = if (excludeHomeApps) {
                runCatching { appDao.getHomeAppPackages() }.getOrDefault(emptyList()).toSet()
            } else {
                emptySet()
            }
            val scores = getScores()

            apps.filter {
                it.packageName !in blacklistPackages &&
                        !it.isHidden &&
                        (if (excludeHomeApps) it.packageName !in homePackages else true)
            }
                .map { it.toAppInfo(scores[it.packageName] ?: 0f) }
                .sortedByDescending { it.score }
                .take(limit)
        }
    }

    /**
     * 获取所有应用 (按字母分组)
     */
    fun observeAllAppsGrouped(): Flow<Map<String, List<AppInfo>>> {
        return appDao.getAllApps().combine(blacklistDao.getAllBlacklist()) { apps, blacklist ->
            val blacklistPackages = blacklist.map { it.packageName }.toSet()
            val scores = getScores()

            apps.filter { it.packageName !in blacklistPackages && !it.isHidden }
                .map { it.toAppInfo(scores[it.packageName] ?: 0f) }
                .groupBy { it.firstLetter }
                .mapValues { (_, group) -> group.sortedByDescending { it.score } }
                .toSortedMap()
        }
    }

    /**
     * 获取所有应用列表 (用于搜索)
     */
    suspend fun getAllApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val blacklistPackages = blacklistDao.getBlacklistPackages().toSet()
        val scores = getScores()

        appDao.getAllAppsList()
            .filter { it.packageName !in blacklistPackages && !it.isHidden }
            .map { it.toAppInfo(scores[it.packageName] ?: 0f) }
    }

    /**
     * 记录应用启动
     */
    suspend fun recordAppLaunch(packageName: String) = withContext(Dispatchers.IO) {
        val today = LocalDate.now().format(dateFormatter)
        val now = System.currentTimeMillis()

        // 更新应用最后启动时间
        appDao.updateLastLaunchTime(packageName, now)

        // 增加今日启动次数
        dailyStatsDao.incrementLaunchCount(packageName, today)

        // 使评分缓存失效
        invalidateScoreCache()
    }

    /**
     * 启动应用
     */
    fun launchApp(packageName: String): Boolean {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
     * 打开系统时钟应用
     */
    fun openClock(): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 添加到黑名单
     */
    suspend fun addToBlacklist(packageName: String) = withContext(Dispatchers.IO) {
        blacklistDao.insert(BlacklistEntity(packageName))
        appDao.updateHidden(packageName, true)
    }

    /**
     * 从黑名单移除
     */
    suspend fun removeFromBlacklist(packageName: String) = withContext(Dispatchers.IO) {
        blacklistDao.delete(packageName)
        appDao.updateHidden(packageName, false)
    }

    /**
     * 获取黑名单应用列表
     */
    fun observeBlacklist(): Flow<List<AppInfo>> {
        return blacklistDao.getAllBlacklist().map { blacklist ->
            blacklist.mapNotNull { entity ->
                appDao.getAppByPackageName(entity.packageName)?.toAppInfo(0f)
            }
        }
    }

    /**
     * 处理应用安装
     */
    suspend fun onAppInstalled(packageName: String) = withContext(Dispatchers.IO) {
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val appName = packageManager.getApplicationLabel(appInfo).toString()
            val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            val entity = AppEntity(
                packageName = packageName,
                appName = appName,
                firstLetter = PinyinHelper.getFirstLetter(appName),
                isSystemApp = isSystemApp
            )
            appDao.insert(entity)
        } catch (e: PackageManager.NameNotFoundException) {
            // 忽略
        }
    }

    /**
     * 处理应用卸载
     */
    suspend fun onAppUninstalled(packageName: String) = withContext(Dispatchers.IO) {
        appDao.deleteByPackageName(packageName)
        dailyStatsDao.deleteStatsByPackage(packageName)
        blacklistDao.delete(packageName)
    }

    /**
     * 处理应用更新
     */
    suspend fun onAppUpdated(packageName: String) = withContext(Dispatchers.IO) {
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val appName = packageManager.getApplicationLabel(appInfo).toString()

            val existingApp = appDao.getAppByPackageName(packageName)
            if (existingApp != null) {
                appDao.update(
                    existingApp.copy(
                        appName = appName,
                        firstLetter = PinyinHelper.getFirstLetter(appName),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        } catch (e: PackageManager.NameNotFoundException) {
            // 忽略
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

        val stats30d = dailyStatsDao.getStatsSince(startDate30d).associate { it.packageName to it.totalCount }
        val stats7d = dailyStatsDao.getStatsSince(startDate7d).associate { it.packageName to it.totalCount }
        val lastLaunchTimes = appDao.getAllLastLaunchTimes().associate { it.packageName to it.lastLaunchTime }

        val now = System.currentTimeMillis()
        val allPackages = (stats30d.keys + stats7d.keys + lastLaunchTimes.keys).distinct()

        return allPackages.associateWith { pkg ->
            calculateScore(
                count30d = stats30d[pkg] ?: 0,
                count7d = stats7d[pkg] ?: 0,
                lastLaunchTime = lastLaunchTimes[pkg] ?: 0L,
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
     * 加载应用图标
     */
    fun loadAppIcon(packageName: String) = try {
        packageManager.getApplicationIcon(packageName)
    } catch (e: Exception) {
        null
    }

    private data class InstalledApp(
        val packageName: String,
        val appName: String,
        val isSystemApp: Boolean
    )

    private fun InstalledApp.toEntity() = AppEntity(
        packageName = packageName,
        appName = appName,
        firstLetter = PinyinHelper.getFirstLetter(appName),
        isSystemApp = isSystemApp
    )

    private fun AppEntity.toAppInfo(score: Float) = AppInfo(
        packageName = packageName,
        displayName = customName ?: appName,
        icon = loadAppIcon(packageName),
        launchCount30d = 0,
        score = score,
        firstLetter = firstLetter,
        isSystemApp = isSystemApp,
        isHidden = isHidden,
        homePosition = homePosition,
        lastLaunchTime = lastLaunchTime
    )
}
