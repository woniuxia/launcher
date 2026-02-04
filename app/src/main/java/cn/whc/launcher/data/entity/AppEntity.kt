package cn.whc.launcher.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * 应用信息数据库实体
 * 注意: 图标从 PackageManager 实时加载，不存储在数据库
 * 使用 packageName + activityName 作为复合主键，支持同一包多个启动入口
 */
@Entity(
    tableName = "apps",
    primaryKeys = ["package_name", "activity_name"]
)
data class AppEntity(
    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "activity_name")
    val activityName: String,

    @ColumnInfo(name = "app_name")
    val appName: String,

    @ColumnInfo(name = "last_launch_time")
    val lastLaunchTime: Long = 0,

    @ColumnInfo(name = "first_letter")
    val firstLetter: String = "#",

    @ColumnInfo(name = "is_system_app")
    val isSystemApp: Boolean = false,

    @ColumnInfo(name = "is_hidden")
    val isHidden: Boolean = false,

    @ColumnInfo(name = "custom_name")
    val customName: String? = null,

    @ColumnInfo(name = "custom_icon_uri")
    val customIconUri: String? = null,

    @ColumnInfo(name = "home_position")
    val homePosition: Int = -1,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
