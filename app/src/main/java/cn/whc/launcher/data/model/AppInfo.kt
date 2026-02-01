package cn.whc.launcher.data.model

import android.graphics.drawable.Drawable

/**
 * UI 层应用信息模型
 * 包含运行时计算的字段
 */
data class AppInfo(
    val packageName: String,
    val displayName: String,
    val icon: Drawable?,
    val launchCount30d: Int = 0,
    val score: Float = 0f,
    val firstLetter: String = "#",
    val isSystemApp: Boolean = false,
    val isHidden: Boolean = false,
    val homePosition: Int = -1,
    val lastLaunchTime: Long = 0
)

/**
 * 按字母分组的应用列表项
 */
sealed class AppListItem {
    data class Header(val letter: String) : AppListItem()
    data class App(val appInfo: AppInfo) : AppListItem()
}
