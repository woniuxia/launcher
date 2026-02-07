package cn.whc.launcher.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cn.whc.launcher.data.entity.GraylistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GraylistDao {
    @Query("SELECT * FROM graylist")
    fun getAllGraylist(): Flow<List<GraylistEntity>>

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
