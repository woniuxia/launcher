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

    @Query("SELECT package_name FROM blacklist")
    suspend fun getBlacklistPackages(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM blacklist WHERE package_name = :packageName)")
    suspend fun isBlacklisted(packageName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(blacklistEntity: BlacklistEntity)

    @Query("DELETE FROM blacklist WHERE package_name = :packageName")
    suspend fun delete(packageName: String)

    @Query("DELETE FROM blacklist")
    suspend fun clearAll()
}
