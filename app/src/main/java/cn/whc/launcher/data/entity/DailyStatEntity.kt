package cn.whc.launcher.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 每日使用统计数据库实体
 * 保留30天数据
 */
@Entity(
    tableName = "daily_stats",
    foreignKeys = [
        ForeignKey(
            entity = AppEntity::class,
            parentColumns = ["package_name"],
            childColumns = ["package_name"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["package_name"]),
        Index(value = ["date"]),
        Index(value = ["package_name", "date"], unique = true)
    ]
)
data class DailyStatEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "date")
    val date: String,  // 格式: YYYY-MM-DD

    @ColumnInfo(name = "launch_count")
    val launchCount: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
