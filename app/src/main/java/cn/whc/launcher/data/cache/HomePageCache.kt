package cn.whc.launcher.data.cache

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 首页缓存数据模型。
 */
@Serializable
data class HomePageSnapshot(
    /** 首页应用列表（已排序）。 */
    val homeApps: List<CachedAppInfo>,
    /** 可用字母索引。 */
    val availableLetters: Set<String>,
    /** 应用评分缓存。 */
    val scores: Map<String, Float>,
    /** 时间推荐应用缓存。 */
    val timeRecommendations: List<CachedAppInfo>,
    /** 缓存时间戳。 */
    val timestamp: Long,
    /** 时间推荐更新时间戳（向后兼容时回退到 timestamp）。 */
    val timeRecommendationsTimestamp: Long = timestamp,
    /** 缓存版本号（用于结构升级）。 */
    val version: Int = CACHE_VERSION
) {
    companion object {
        const val CACHE_VERSION = 1
    }
}

/**
 * 缓存使用的精简应用信息。
 */
@Serializable
data class CachedAppInfo(
    val packageName: String,
    val activityName: String,
    val displayName: String,
    val score: Float,
    val firstLetter: String
) {
    val componentKey: String get() = "$packageName/$activityName"
}

/**
 * 首页缓存管理器。
 */
@Singleton
class HomePageCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cacheFile: File
        get() = File(context.cacheDir, CACHE_FILE_NAME)

    private val mutex = Mutex()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    companion object {
        private const val TAG = "HomePageCache"
        private const val CACHE_FILE_NAME = "home_page_cache.json"
        /** 默认缓存有效期：24 小时。 */
        const val DEFAULT_MAX_AGE_MS = 24 * 60 * 60 * 1000L
        /** 评分缓存有效期：5 分钟。 */
        const val SCORE_CACHE_MAX_AGE_MS = 5 * 60 * 1000L
        /** 时间推荐缓存有效期：30 分钟。 */
        const val TIME_RECOMMENDATION_MAX_AGE_MS = 30 * 60 * 1000L
    }

    /**
     * 从缓存读取首页快照。
     */
    suspend fun load(maxAgeMs: Long = DEFAULT_MAX_AGE_MS): HomePageSnapshot? = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                if (!cacheFile.exists()) return@withContext null

                val content = cacheFile.readText()
                val snapshot = json.decodeFromString<HomePageSnapshot>(content)

                if (snapshot.version != HomePageSnapshot.CACHE_VERSION) {
                    Timber.tag(TAG).d("Cache version mismatch, clearing cache")
                    cacheFile.delete()
                    return@withContext null
                }

                val age = System.currentTimeMillis() - snapshot.timestamp
                if (age > maxAgeMs) {
                    Timber.tag(TAG).d("Cache expired (age: %d ms)", age)
                    return@withContext null
                }

                Timber.tag(TAG).d(
                    "Cache loaded successfully (age: %d ms, size: %d bytes)",
                    age,
                    cacheFile.length()
                )
                snapshot
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Failed to load cache, deleting corrupted file")
                cacheFile.delete()
                null
            }
        }
    }

    /**
     * 保存首页快照到缓存。
     */
    suspend fun save(snapshot: HomePageSnapshot) = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val content = json.encodeToString(HomePageSnapshot.serializer(), snapshot)
                cacheFile.writeText(content)
                Timber.tag(TAG).d("Cache saved (size: %d bytes)", cacheFile.length())
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Failed to save cache")
            }
        }
    }

    /**
     * 快速检查缓存是否有效。
     */
    suspend fun isValid(maxAgeMs: Long = DEFAULT_MAX_AGE_MS): Boolean = withContext(Dispatchers.IO) {
        load(maxAgeMs) != null
    }

    /**
     * 清除缓存文件。
     */
    suspend fun clear() = withContext(Dispatchers.IO) {
        mutex.withLock {
            cacheFile.delete()
            Timber.tag(TAG).d("Cache cleared")
        }
    }

    /**
     * 获取缓存文件大小（字节）。
     */
    fun getCacheSize(): Long = cacheFile.length()
}
