package cn.whc.launcher.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.graphics.drawable.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

val LocalIconCache = staticCompositionLocalOf<IconCache> { error("No IconCache provided") }

/**
 * 应用图标内存缓存
 * 使用 LRU 策略缓存图标，限制总内存 16MB
 */
@Singleton
class IconCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val packageManager: PackageManager = context.packageManager

    companion object {
        private const val TAG = "IconCache"
        private const val MAX_CACHE_SIZE_KB = 16 * 1024  // 16MB
    }

    // LRU 缓存：基于字节数计算，限制总内存 16MB
    private val cache = object : LruCache<String, Bitmap>(MAX_CACHE_SIZE_KB) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            // 返回 KB 数
            return value.byteCount / 1024
        }
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
     * 预加载图标列表 (并行加载，限制 4 并发)
     */
    suspend fun preloadIcons(componentKeys: List<String>) {
        withContext(Dispatchers.IO) {
            val semaphore = Semaphore(4)
            coroutineScope {
                componentKeys.forEach { key ->
                    if (cache.get(key) == null) {
                        launch {
                            semaphore.withPermit {
                                loadIcon(key)?.let { cache.put(key, it) }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 从 PackageManager 加载图标
     */
    private fun loadIcon(componentKey: String): Bitmap? {
        val parts = componentKey.split("/", limit = 2)
        if (parts.size != 2) {
            Timber.tag(TAG).w("Invalid componentKey format: %s", componentKey)
            return null
        }

        val packageName = parts[0]
        val activityName = parts[1]

        return try {
            val activityInfo = packageManager.getActivityInfo(
                ComponentName(packageName, activityName),
                PackageManager.GET_META_DATA
            )
            activityInfo.loadIcon(packageManager).toBitmap()
        } catch (e: Exception) {
            Timber.tag(TAG).d(e, "Activity icon not found, trying app icon: %s", componentKey)
            // 回退到应用图标
            try {
                packageManager.getApplicationIcon(packageName).toBitmap()
            } catch (e2: Exception) {
                Timber.tag(TAG).w(e2, "Failed to load icon for: %s", componentKey)
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
