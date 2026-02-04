package cn.whc.launcher.data.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cn.whc.launcher.data.entity.DailyStatEntity

@Dao
interface DailyStatsDao {
    @Query("""
        SELECT package_name, activity_name, SUM(launch_count) as totalCount
        FROM daily_stats
        WHERE date >= date('now', '-' || :days || ' days')
        GROUP BY package_name, activity_name
    """)
    suspend fun getRecentStats(days: Int): List<ComponentCount>

    @Query("""
        SELECT package_name, activity_name, SUM(launch_count) as totalCount
        FROM daily_stats
        WHERE date >= :startDate
        GROUP BY package_name, activity_name
    """)
    suspend fun getStatsSince(startDate: String): List<ComponentCount>

    @Query("SELECT * FROM daily_stats WHERE package_name = :packageName AND activity_name = :activityName AND date = :date")
    suspend fun getStat(packageName: String, activityName: String, date: String): DailyStatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stat: DailyStatEntity)

    @Query("""
        INSERT OR REPLACE INTO daily_stats (package_name, activity_name, date, launch_count, created_at)
        VALUES (
            :packageName,
            :activityName,
            :date,
            COALESCE(
                (SELECT launch_count FROM daily_stats WHERE package_name = :packageName AND activity_name = :activityName AND date = :date),
                0
            ) + :count,
            :createdAt
        )
    """)
    suspend fun incrementLaunchCount(
        packageName: String,
        activityName: String,
        date: String,
        count: Int = 1,
        createdAt: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM daily_stats WHERE date < :cutoffDate")
    suspend fun deleteOldStats(cutoffDate: String)

    @Query("DELETE FROM daily_stats WHERE package_name = :packageName AND activity_name = :activityName")
    suspend fun deleteStats(packageName: String, activityName: String)

    @Query("DELETE FROM daily_stats WHERE package_name = :packageName")
    suspend fun deleteStatsByPackage(packageName: String)

    @Query("SELECT COUNT(*) FROM daily_stats")
    suspend fun getStatsCount(): Int
}

data class ComponentCount(
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "activity_name") val activityName: String,
    val totalCount: Int
)
