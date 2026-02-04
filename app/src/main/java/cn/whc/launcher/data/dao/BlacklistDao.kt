package cn.whc.launcher.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cn.whc.launcher.data.entity.BlacklistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlacklistDao {
    @Query("SELECT * FROM blacklist")
    fun getAllBlacklist(): Flow<List<BlacklistEntity>>

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
