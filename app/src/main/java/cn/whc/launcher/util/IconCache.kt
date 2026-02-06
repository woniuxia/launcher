package cn.whc.launcher.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用图标内存缓存
 * 使用 LRU 策略缓存最近使用的 100 个图标
 */
@Singleton
class IconCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val packageManager: PackageManager = context.packageManager

    // LRU 缓存：最多 100 个图标，每个图标约 100KB (64x64 ARGB_8888)
    private val cache = object : LruCache<String, Bitmap>(100) {
        override fun sizeOf(key: String, value: Bitmap): Int = 1
    }

    /**
     * 获取应用图标 (带缓存)
     * @param componentKey 格式: "packageName/activityName"
     */
    suspend fun getIcon(componentKey: String): Bitmap? {
        // 先查缓存
        cache.get(componentKey)?.let { return it }

        // 缓存未命中，从 PackageManager 加载
        return withContext(Dispatchers.IO) {
            loadIcon(componentKey)?.also { bitmap ->
                cache.put(componentKey, bitmap)
            }
        }
    }

    /**
     * 预加载图标列表 (后台批量加载)
     */
    suspend fun preloadIcons(componentKeys: List<String>) {
        withContext(Dispatchers.IO) {
            componentKeys.forEach { key ->
                if (cache.get(key) == null) {
                    loadIcon(key)?.let { cache.put(key, it) }
                }
            }
        }
    }

    /**
     * 从 PackageManager 加载图标
     */
    private fun loadIcon(componentKey: String): Bitmap? {
        val parts = componentKey.split("/", limit = 2)
        if (parts.size != 2) return null

        val packageName = parts[0]
        val activityName = parts[1]

        return try {
            val activityInfo = packageManager.getActivityInfo(
                ComponentName(packageName, activityName),
                PackageManager.GET_META_DATA
            )
            activityInfo.loadIcon(packageManager).toBitmap()
        } catch (e: Exception) {
            // 回退到应用图标
            try {
                packageManager.getApplicationIcon(packageName).toBitmap()
            } catch (e2: Exception) {
                null
            }
        }
    }

    /**
     * 清除缓存
     */
    fun clear() {
        cache.evictAll()
    }

    /**
     * 移除单个缓存项 (应用卸载时调用)
     */
    fun remove(componentKey: String) {
        cache.remove(componentKey)
    }
}
