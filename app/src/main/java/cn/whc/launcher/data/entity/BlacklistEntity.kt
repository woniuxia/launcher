package cn.whc.launcher.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

/**
 * 黑名单数据库实体
 * 使用 packageName + activityName 作为复合主键
 */
@Entity(
    tableName = "blacklist",
    primaryKeys = ["package_name", "activity_name"],
    foreignKeys = [
        ForeignKey(
            entity = AppEntity::class,
            parentColumns = ["package_name", "activity_name"],
            childColumns = ["package_name", "activity_name"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BlacklistEntity(
    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "activity_name")
    val activityName: String,

    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis()
)
