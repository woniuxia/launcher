package cn.whc.launcher.data.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cn.whc.launcher.data.entity.LaunchTimeEntity

@Dao
interface LaunchTimeDao {

    @Insert
    suspend fun insert(record: LaunchTimeEntity)

    /**
     * 查询时间窗口内的应用启动统计 (不跨天情况)
     */
    @Query("""
        SELECT package_name, activity_name, COUNT(*) as launch_count
        FROM launch_time_records
        WHERE time_of_day_minutes >= :startMinutes
          AND time_of_day_minutes <= :endMinutes
          AND launch_timestamp >= :cutoffTimestamp
        GROUP BY package_name, activity_name
        ORDER BY launch_count DESC
    """)
    suspend fun getTimeWindowStats(
        startMinutes: Int,
        endMinutes: Int,
        cutoffTimestamp: Long
    ): List<TimeWindowStat>

    /**
     * 查询时间窗口内的应用启动统计 (跨天情况: 如 23:30 - 00:30)
     */
    @Query("""
        SELECT package_name, activity_name, COUNT(*) as launch_count
        FROM launch_time_records
        WHERE (time_of_day_minutes >= :startMinutes OR time_of_day_minutes <= :endMinutes)
          AND launch_timestamp >= :cutoffTimestamp
        GROUP BY package_name, activity_name
        ORDER BY launch_count DESC
    """)
    suspend fun getTimeWindowStatsCrossDay(
        startMinutes: Int,
        endMinutes: Int,
        cutoffTimestamp: Long
    ): List<TimeWindowStat>

    @Query("DELETE FROM launch_time_records WHERE launch_timestamp < :cutoffTimestamp")
    suspend fun deleteOldRecords(cutoffTimestamp: Long)
}

data class TimeWindowStat(
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "activity_name") val activityName: String,
    @ColumnInfo(name = "launch_count") val launchCount: Int
)
