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

    /**
     * 查询时间窗口内的推荐应用 (不跨天情况)
     * JOIN apps 获取应用信息，LEFT JOIN 排除黑灰名单和隐藏应用，LIMIT 5
     */
    @Query("""
        SELECT ltr.package_name, ltr.activity_name, a.app_name, a.custom_name,
               a.first_letter,
               COUNT(*) as launch_count,
               SUM(
                   (
                       (:windowMinutes + 1.0 - MIN(
                           ABS(ltr.time_of_day_minutes - :currentMinutes),
                           1440 - ABS(ltr.time_of_day_minutes - :currentMinutes)
                       )) / (:windowMinutes + 1.0)
                   )
                   *
                   (
                       1.0 / (1.0 + ((:nowTimestamp - ltr.launch_timestamp) / 86400000.0 / 7.0))
                   )
                   *
                   (
                       CASE
                           WHEN (
                               CAST(strftime('%w', ltr.launch_timestamp / 1000, 'unixepoch', 'localtime') AS INTEGER) IN (0, 6)
                           ) = :isWeekend
                           THEN 1.15
                           ELSE 0.85
                       END
                   )
               ) as weighted_score
        FROM launch_time_records ltr
        INNER JOIN apps a ON ltr.package_name = a.package_name
                          AND ltr.activity_name = a.activity_name
                          AND a.is_hidden = 0
        LEFT JOIN blacklist b ON ltr.package_name = b.package_name
                              AND ltr.activity_name = b.activity_name
        LEFT JOIN graylist g ON ltr.package_name = g.package_name
                             AND ltr.activity_name = g.activity_name
        WHERE ltr.time_of_day_minutes >= :startMinutes
          AND ltr.time_of_day_minutes <= :endMinutes
          AND ltr.launch_timestamp >= :cutoffTimestamp
          AND b.package_name IS NULL
          AND g.package_name IS NULL
        GROUP BY ltr.package_name, ltr.activity_name
        ORDER BY weighted_score DESC, launch_count DESC
        LIMIT 5
    """)
    suspend fun getTimeRecommendations(
        startMinutes: Int,
        endMinutes: Int,
        cutoffTimestamp: Long,
        currentMinutes: Int,
        windowMinutes: Int,
        nowTimestamp: Long,
        isWeekend: Int
    ): List<TimeRecommendationResult>

    /**
     * 查询时间窗口内的推荐应用 (跨天情况: 如 23:30 - 00:30)
     * JOIN apps 获取应用信息，LEFT JOIN 排除黑灰名单和隐藏应用，LIMIT 5
     */
    @Query("""
        SELECT ltr.package_name, ltr.activity_name, a.app_name, a.custom_name,
               a.first_letter,
               COUNT(*) as launch_count,
               SUM(
                   (
                       (:windowMinutes + 1.0 - MIN(
                           ABS(ltr.time_of_day_minutes - :currentMinutes),
                           1440 - ABS(ltr.time_of_day_minutes - :currentMinutes)
                       )) / (:windowMinutes + 1.0)
                   )
                   *
                   (
                       1.0 / (1.0 + ((:nowTimestamp - ltr.launch_timestamp) / 86400000.0 / 7.0))
                   )
                   *
                   (
                       CASE
                           WHEN (
                               CAST(strftime('%w', ltr.launch_timestamp / 1000, 'unixepoch', 'localtime') AS INTEGER) IN (0, 6)
                           ) = :isWeekend
                           THEN 1.15
                           ELSE 0.85
                       END
                   )
               ) as weighted_score
        FROM launch_time_records ltr
        INNER JOIN apps a ON ltr.package_name = a.package_name
                          AND ltr.activity_name = a.activity_name
                          AND a.is_hidden = 0
        LEFT JOIN blacklist b ON ltr.package_name = b.package_name
                              AND ltr.activity_name = b.activity_name
        LEFT JOIN graylist g ON ltr.package_name = g.package_name
                             AND ltr.activity_name = g.activity_name
        WHERE (ltr.time_of_day_minutes >= :startMinutes OR ltr.time_of_day_minutes <= :endMinutes)
          AND ltr.launch_timestamp >= :cutoffTimestamp
          AND b.package_name IS NULL
          AND g.package_name IS NULL
        GROUP BY ltr.package_name, ltr.activity_name
        ORDER BY weighted_score DESC, launch_count DESC
        LIMIT 5
    """)
    suspend fun getTimeRecommendationsCrossDay(
        startMinutes: Int,
        endMinutes: Int,
        cutoffTimestamp: Long,
        currentMinutes: Int,
        windowMinutes: Int,
        nowTimestamp: Long,
        isWeekend: Int
    ): List<TimeRecommendationResult>

    @Query("DELETE FROM launch_time_records WHERE launch_timestamp < :cutoffTimestamp")
    suspend fun deleteOldRecords(cutoffTimestamp: Long)
}

data class TimeWindowStat(
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "activity_name") val activityName: String,
    @ColumnInfo(name = "launch_count") val launchCount: Int
)

data class TimeRecommendationResult(
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "activity_name") val activityName: String,
    @ColumnInfo(name = "app_name") val appName: String,
    @ColumnInfo(name = "custom_name") val customName: String?,
    @ColumnInfo(name = "first_letter") val firstLetter: String,
    @ColumnInfo(name = "launch_count") val launchCount: Int,
    @ColumnInfo(name = "weighted_score") val weightedScore: Double
)
