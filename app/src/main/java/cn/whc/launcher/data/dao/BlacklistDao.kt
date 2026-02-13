package cn.whc.launcher.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cn.whc.launcher.data.entity.AppEntity
import cn.whc.launcher.data.entity.BlacklistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlacklistDao {
    @Query("SELECT * FROM blacklist")
    fun getAllBlacklist(): Flow<List<BlacklistEntity>>

    /**
     * 获取黑名单对应的应用详情。
     * 排序策略：最近加入优先，其次按应用名稳定排序，避免列表顺序抖动。
     */
    @Query("""
        SELECT a.* FROM apps a
        INNER JOIN blacklist b
            ON a.package_name = b.package_name
           AND a.activity_name = b.activity_name
        ORDER BY b.added_at DESC, a.app_name ASC
    """)
    fun getBlacklistedApps(): Flow<List<AppEntity>>

    @Query("SELECT * FROM blacklist")
    suspend fun getAllBlacklistList(): List<BlacklistEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM blacklist WHERE package_name = :packageName AND activity_name = :activityName)")
    suspend fun isBlacklisted(packageName: String, activityName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(blacklistEntity: BlacklistEntity)

    @Query("DELETE FROM blacklist WHERE package_name = :packageName AND activity_name = :activityName")
    suspend fun delete(packageName: String, activityName: String)

    @Query("DELETE FROM blacklist")
    suspend fun clearAll()
}
