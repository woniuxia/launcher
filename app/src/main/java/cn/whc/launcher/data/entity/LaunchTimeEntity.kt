package cn.whc.launcher.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 应用启动时间记录表
 * 记录每次应用启动的精确时间戳，用于时间段推荐算法
 * 保留30天数据
 */
@Entity(
    tableName = "launch_time_records",
    foreignKeys = [
        ForeignKey(
            entity = AppEntity::class,
            parentColumns = ["package_name", "activity_name"],
            childColumns = ["package_name", "activity_name"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["time_of_day_minutes", "launch_timestamp"]),
        Index(value = ["package_name", "activity_name"]),
        Index(value = ["launch_timestamp"])
    ]
)
data class LaunchTimeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "activity_name")
    val activityName: String,

    /**
     * 启动时间戳 (毫秒)
     */
    @ColumnInfo(name = "launch_timestamp")
    val launchTimestamp: Long,

    /**
     * 一天中的分钟数 (0-1439)
     * 计算方式: hour * 60 + minute
     */
    @ColumnInfo(name = "time_of_day_minutes")
    val timeOfDayMinutes: Int
)
