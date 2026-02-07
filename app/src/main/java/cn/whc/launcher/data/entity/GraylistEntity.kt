package cn.whc.launcher.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

/**
 * 灰名单数据库实体
 * 灰名单应用不在首页、常用应用和推荐应用中展示，但在抽屉页面可见
 * 使用 packageName + activityName 作为复合主键
 */
@Entity(
    tableName = "graylist",
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
data class GraylistEntity(
    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "activity_name")
    val activityName: String,

    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis()
)
