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
     * 仅返回原始计数，不包含推荐加权，适合做调试或离线分析。
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
     * 仅窗口判定与不跨天版本不同，其余统计口径一致。
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
     *
     * 参数约束：
     * - startMinutes <= endMinutes，示例：09:30~10:30。
     * - cutoffTimestamp 通常为“最近30天”边界。
     *
     * weighted_score 由三部分相乘后累加得到：
     * 1) 时间贴合度：离 currentMinutes 越近，单条记录贡献越高；
     * 2) 新鲜度衰减：越近期的启动记录权重越高；
     * 3) 工作日/周末匹配：当天类型与历史记录一致时给予更高权重。
     *
     * 排序规则：
     * - 先按 weighted_score 降序；
     * - 分数接近时按 launch_count 降序兜底；
     * - 最多返回 5 个候选，减少上层内存处理压力。
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
     *
     * 参数约束：
     * - startMinutes > endMinutes，示例：23:30~00:30。
     * - 时间窗口采用 OR 条件：
     *   time_of_day_minutes >= startMinutes OR <= endMinutes。
     *
     * 其余加权、过滤和排序语义与 getTimeRecommendations() 保持一致，
     * 目的是在跨日场景下保持推荐口径不变，只调整窗口判定方式。
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
    /**
     * SQL 聚合得到的推荐置信分，值越大表示“当前时间下越可能要用”。
     * 该值不是全局稳定分数，仅用于本次时间窗口推荐排序。
     */
    @ColumnInfo(name = "weighted_score") val weightedScore: Double
)
