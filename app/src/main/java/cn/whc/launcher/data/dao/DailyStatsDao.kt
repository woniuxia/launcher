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
        SELECT package_name, SUM(launch_count) as totalCount
        FROM daily_stats
        WHERE date >= date('now', '-' || :days || ' days')
        GROUP BY package_name
    """)
    suspend fun getRecentStats(days: Int): List<PackageCount>

    @Query("""
        SELECT package_name, SUM(launch_count) as totalCount
        FROM daily_stats
        WHERE date >= :startDate
        GROUP BY package_name
    """)
    suspend fun getStatsSince(startDate: String): List<PackageCount>

    @Query("SELECT * FROM daily_stats WHERE package_name = :packageName AND date = :date")
    suspend fun getStatByPackageAndDate(packageName: String, date: String): DailyStatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stat: DailyStatEntity)

    @Query("""
        INSERT OR REPLACE INTO daily_stats (package_name, date, launch_count, created_at)
        VALUES (
            :packageName,
            :date,
            COALESCE(
                (SELECT launch_count FROM daily_stats WHERE package_name = :packageName AND date = :date),
                0
            ) + :count,
            :createdAt
        )
    """)
    suspend fun incrementLaunchCount(
        packageName: String,
        date: String,
        count: Int = 1,
        createdAt: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM daily_stats WHERE date < :cutoffDate")
    suspend fun deleteOldStats(cutoffDate: String)

    @Query("DELETE FROM daily_stats WHERE package_name = :packageName")
    suspend fun deleteStatsByPackage(packageName: String)

    @Query("SELECT COUNT(*) FROM daily_stats")
    suspend fun getStatsCount(): Int
}

data class PackageCount(
    @ColumnInfo(name = "package_name") val packageName: String,
    val totalCount: Int
)
