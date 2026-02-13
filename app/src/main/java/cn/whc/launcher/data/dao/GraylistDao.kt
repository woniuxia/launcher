package cn.whc.launcher.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cn.whc.launcher.data.entity.AppEntity
import cn.whc.launcher.data.entity.GraylistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GraylistDao {
    @Query("SELECT * FROM graylist")
    fun getAllGraylist(): Flow<List<GraylistEntity>>

    /**
     * 获取灰名单对应的应用详情。
     * 排序策略：最近加入优先，其次按应用名稳定排序，避免列表顺序抖动。
     */
    @Query("""
        SELECT a.* FROM apps a
        INNER JOIN graylist g
            ON a.package_name = g.package_name
           AND a.activity_name = g.activity_name
        ORDER BY g.added_at DESC, a.app_name ASC
    """)
    fun getGraylistedApps(): Flow<List<AppEntity>>

    @Query("SELECT * FROM graylist")
    suspend fun getAllGraylistList(): List<GraylistEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM graylist WHERE package_name = :packageName AND activity_name = :activityName)")
    suspend fun isGraylisted(packageName: String, activityName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(graylistEntity: GraylistEntity)

    @Query("DELETE FROM graylist WHERE package_name = :packageName AND activity_name = :activityName")
    suspend fun delete(packageName: String, activityName: String)

    @Query("DELETE FROM graylist")
    suspend fun clearAll()
}
