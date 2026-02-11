package cn.whc.launcher.data.model

/**
 * 应用设置数据模型
 */
data class AppSettings(
    val core: CoreSettings = CoreSettings(),
    val advanced: AdvancedSettings = AdvancedSettings(),
    val layout: LayoutSettings = LayoutSettings(),
    val appearance: AppearanceSettings = AppearanceSettings(),
    val clock: ClockSettings = ClockSettings(),
    val search: SearchSettings = SearchSettings(),
    val gesture: GestureSettings = GestureSettings()
)

enum class PersonalPreset {
    LITE,
    BALANCED,
    FOCUS
}

data class CoreSettings(
    val preset: PersonalPreset = PersonalPreset.BALANCED,
    val homeDisplayCount: Int = 16,
    val drawerFrequentCount: Int = 5,
    val showSearch: Boolean = true,
    val showTimeRecommendation: Boolean = true,
    val backgroundType: BackgroundType = BackgroundType.BLUR,
    val blurStrength: Int = 20,
    val iconSize: Int = 56,
    val hapticFeedback: Boolean = true
)

data class AdvancedSettings(
    val showLunar: Boolean = true,
    val showFestival: Boolean = true,
    val swipeSensitivity: SwipeSensitivity = SwipeSensitivity.MEDIUM,
    val enableT9: Boolean = false,
    val schemaVersion: Int = 2
)

data class LayoutSettings(
    val columns: Int = 4,
    val rows: Int = 4,
    val iconSize: Int = 56,
    val iconSpacing: Int = 16,
    val verticalOffset: Int = 0,
    val homeDisplayCount: Int = 16,
    val drawerFrequentCount: Int = 5,
    val textSize: Int = 12,
    val showTimeRecommendation: Boolean = true
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
