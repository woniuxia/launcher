package cn.whc.launcher.data.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cn.whc.launcher.data.entity.AppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM apps WHERE is_hidden = 0 ORDER BY last_launch_time DESC")
    fun getAllApps(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE is_hidden = 0 ORDER BY last_launch_time DESC")
    suspend fun getAllAppsList(): List<AppEntity>

    @Query("SELECT * FROM apps WHERE package_name = :packageName AND activity_name = :activityName")
    suspend fun getApp(packageName: String, activityName: String): AppEntity?

    @Query("SELECT * FROM apps WHERE home_position >= 0 ORDER BY home_position ASC")
    fun getHomeApps(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE home_position >= 0 ORDER BY home_position ASC")
    suspend fun getHomeAppsList(): List<AppEntity>

    @Query("SELECT * FROM apps WHERE is_hidden = 0 AND first_letter = :letter ORDER BY last_launch_time DESC")
    suspend fun getAppsByFirstLetter(letter: String): List<AppEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: AppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<AppEntity>)

    @Update
    suspend fun update(app: AppEntity)

    @Query("UPDATE apps SET last_launch_time = :timestamp, updated_at = :timestamp WHERE package_name = :packageName AND activity_name = :activityName")
    suspend fun updateLastLaunchTime(packageName: String, activityName: String, timestamp: Long)

    @Query("UPDATE apps SET is_hidden = :isHidden, updated_at = :timestamp WHERE package_name = :packageName AND activity_name = :activityName")
    suspend fun updateHidden(packageName: String, activityName: String, isHidden: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE apps SET home_position = :position, updated_at = :timestamp WHERE package_name = :packageName AND activity_name = :activityName")
    suspend fun updateHomePosition(packageName: String, activityName: String, position: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE apps SET app_name = :appName, updated_at = :timestamp WHERE package_name = :packageName AND activity_name = :activityName")
    suspend fun updateAppName(packageName: String, activityName: String, appName: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM apps WHERE package_name = :packageName AND activity_name = :activityName")
    suspend fun delete(packageName: String, activityName: String)

    @Query("DELETE FROM apps WHERE package_name = :packageName")
    suspend fun deleteByPackageName(packageName: String)

    @Query("SELECT COUNT(*) FROM apps")
    suspend fun getAppCount(): Int

    @Query("SELECT package_name, activity_name FROM apps")
    suspend fun getAllComponentKeys(): List<ComponentKey>

    @Query("SELECT package_name, activity_name, last_launch_time FROM apps")
    suspend fun getAllLastLaunchTimes(): List<ComponentLaunchTime>

    @Query("SELECT custom_name FROM apps WHERE package_name = :packageName AND activity_name = :activityName")
    suspend fun getCustomName(packageName: String, activityName: String): String?

    @Query("SELECT package_name, activity_name, custom_name FROM apps WHERE custom_name IS NOT NULL")
    suspend fun getAllCustomNames(): List<ComponentCustomName>

    @Query("UPDATE apps SET home_position = -1 WHERE home_position >= 0")
    suspend fun clearAllHomePositions()

    /**
     * 获取补足推荐的应用：排除黑灰名单、隐藏应用和已有推荐，按最近使用时间排序
     */
    @Query("""
        SELECT * FROM apps
        WHERE is_hidden = 0
          AND (package_name || '/' || activity_name) NOT IN (:excludeKeys)
          AND (package_name || '/' || activity_name) NOT IN (
              SELECT package_name || '/' || activity_name FROM blacklist
          )
          AND (package_name || '/' || activity_name) NOT IN (
              SELECT package_name || '/' || activity_name FROM graylist
          )
        ORDER BY last_launch_time DESC
        LIMIT :limit
    """)
    suspend fun getSupplementApps(excludeKeys: List<String>, limit: Int): List<AppEntity>
}

data class ComponentKey(
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "activity_name") val activityName: String
)

data class ComponentLaunchTime(
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "activity_name") val activityName: String,
    @ColumnInfo(name = "last_launch_time") val lastLaunchTime: Long
)

data class ComponentCustomName(
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "activity_name") val activityName: String,
    @ColumnInfo(name = "custom_name") val customName: String
)
