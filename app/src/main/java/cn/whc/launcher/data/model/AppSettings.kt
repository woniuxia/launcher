package cn.whc.launcher.data.model

/**
 * 应用设置数据模型
 */
data class AppSettings(
    val layout: LayoutSettings = LayoutSettings(),
    val appearance: AppearanceSettings = AppearanceSettings(),
    val clock: ClockSettings = ClockSettings(),
    val search: SearchSettings = SearchSettings(),
    val gesture: GestureSettings = GestureSettings()
)

data class LayoutSettings(
    val columns: Int = 4,
    val rows: Int = 4,
    val iconSize: Int = 56,
    val iconSpacing: Int = 16,
    val verticalOffset: Int = 0,
    val homeDisplayCount: Int = 16,
    val drawerFrequentCount: Int = 5,
    val textSize: Int = 12
)

data class AppearanceSettings(
    val theme: Theme = Theme.SYSTEM,
    val backgroundType: BackgroundType = BackgroundType.BLUR,
    val blurStrength: Int = 20,
    val iconRadius: Int = 16,
    val showShadow: Boolean = true
)

data class ClockSettings(
    val showTime: Boolean = true,
    val showSeconds: Boolean = false,
    val showDate: Boolean = true,
    val showLunar: Boolean = true,
    val showFestival: Boolean = true,
    val is24Hour: Boolean = true
)

data class SearchSettings(
    val enableSearch: Boolean = true,
    val enablePinyin: Boolean = true,
    val enableT9: Boolean = false,
    val showSearchHistory: Boolean = true
)

data class GestureSettings(
    val swipeSensitivity: SwipeSensitivity = SwipeSensitivity.MEDIUM,
    val hapticFeedback: Boolean = true
)

enum class Theme { LIGHT, DARK, SYSTEM }
enum class BackgroundType { SOLID, BLUR, IMAGE }
enum class SwipeSensitivity { LOW, MEDIUM, HIGH }
