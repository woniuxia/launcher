package cn.whc.launcher.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cn.whc.launcher.data.model.AppSettings
import cn.whc.launcher.data.model.AppearanceSettings
import cn.whc.launcher.data.model.BackgroundType
import cn.whc.launcher.data.model.ClockSettings
import cn.whc.launcher.data.model.GestureSettings
import cn.whc.launcher.data.model.LayoutSettings
import cn.whc.launcher.data.model.SearchSettings
import cn.whc.launcher.data.model.SwipeSensitivity
import cn.whc.launcher.data.model.Theme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.settingsDataStore

    // === Layout Keys ===
    private val COLUMNS_KEY = intPreferencesKey("layout_columns")
    private val ROWS_KEY = intPreferencesKey("layout_rows")
    private val ICON_SIZE_KEY = intPreferencesKey("layout_icon_size")
    private val ICON_SPACING_KEY = intPreferencesKey("layout_icon_spacing")
    private val VERTICAL_OFFSET_KEY = intPreferencesKey("layout_vertical_offset")
    private val HOME_DISPLAY_COUNT_KEY = intPreferencesKey("layout_home_display_count")
    private val DRAWER_FREQUENT_COUNT_KEY = intPreferencesKey("layout_drawer_frequent_count")
    private val TEXT_SIZE_KEY = intPreferencesKey("layout_text_size")

    // === Appearance Keys ===
    private val THEME_KEY = stringPreferencesKey("appearance_theme")
    private val BACKGROUND_TYPE_KEY = stringPreferencesKey("appearance_background_type")
    private val BLUR_STRENGTH_KEY = intPreferencesKey("appearance_blur_strength")
    private val ICON_RADIUS_KEY = intPreferencesKey("appearance_icon_radius")
    private val SHOW_SHADOW_KEY = booleanPreferencesKey("appearance_show_shadow")

    // === Clock Keys ===
    private val SHOW_TIME_KEY = booleanPreferencesKey("clock_show_time")
    private val SHOW_SECONDS_KEY = booleanPreferencesKey("clock_show_seconds")
    private val SHOW_DATE_KEY = booleanPreferencesKey("clock_show_date")
    private val SHOW_LUNAR_KEY = booleanPreferencesKey("clock_show_lunar")
    private val SHOW_FESTIVAL_KEY = booleanPreferencesKey("clock_show_festival")
    private val IS_24_HOUR_KEY = booleanPreferencesKey("clock_is_24_hour")

    // === Search Keys ===
    private val ENABLE_SEARCH_KEY = booleanPreferencesKey("search_enable")
    private val ENABLE_PINYIN_KEY = booleanPreferencesKey("search_enable_pinyin")
    private val ENABLE_T9_KEY = booleanPreferencesKey("search_enable_t9")
    private val SHOW_SEARCH_HISTORY_KEY = booleanPreferencesKey("search_show_history")

    // === Gesture Keys ===
    private val SWIPE_SENSITIVITY_KEY = stringPreferencesKey("gesture_swipe_sensitivity")
    private val HAPTIC_FEEDBACK_KEY = booleanPreferencesKey("gesture_haptic_feedback")

    // === FAB Position Keys ===
    private val FAB_OFFSET_X_KEY = floatPreferencesKey("fab_offset_x")
    private val FAB_OFFSET_Y_KEY = floatPreferencesKey("fab_offset_y")

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            layout = LayoutSettings(
                columns = prefs[COLUMNS_KEY] ?: 4,
                rows = prefs[ROWS_KEY] ?: 4,
                iconSize = prefs[ICON_SIZE_KEY] ?: 56,
                iconSpacing = prefs[ICON_SPACING_KEY] ?: 16,
                verticalOffset = prefs[VERTICAL_OFFSET_KEY] ?: 0,
                homeDisplayCount = prefs[HOME_DISPLAY_COUNT_KEY] ?: 16,
                drawerFrequentCount = prefs[DRAWER_FREQUENT_COUNT_KEY] ?: 5,
                textSize = prefs[TEXT_SIZE_KEY] ?: 12
            ),
            appearance = AppearanceSettings(
                theme = Theme.valueOf(prefs[THEME_KEY] ?: Theme.SYSTEM.name),
                backgroundType = BackgroundType.valueOf(prefs[BACKGROUND_TYPE_KEY] ?: BackgroundType.BLUR.name),
                blurStrength = prefs[BLUR_STRENGTH_KEY] ?: 20,
                iconRadius = prefs[ICON_RADIUS_KEY] ?: 16,
                showShadow = prefs[SHOW_SHADOW_KEY] ?: true
            ),
            clock = ClockSettings(
                showTime = prefs[SHOW_TIME_KEY] ?: true,
                showSeconds = prefs[SHOW_SECONDS_KEY] ?: false,
                showDate = prefs[SHOW_DATE_KEY] ?: true,
                showLunar = prefs[SHOW_LUNAR_KEY] ?: true,
                showFestival = prefs[SHOW_FESTIVAL_KEY] ?: true,
                is24Hour = prefs[IS_24_HOUR_KEY] ?: true
            ),
            search = SearchSettings(
                enableSearch = prefs[ENABLE_SEARCH_KEY] ?: true,
                enablePinyin = prefs[ENABLE_PINYIN_KEY] ?: true,
                enableT9 = prefs[ENABLE_T9_KEY] ?: false,
                showSearchHistory = prefs[SHOW_SEARCH_HISTORY_KEY] ?: true
            ),
            gesture = GestureSettings(
                swipeSensitivity = SwipeSensitivity.valueOf(
                    prefs[SWIPE_SENSITIVITY_KEY] ?: SwipeSensitivity.MEDIUM.name
                ),
                hapticFeedback = prefs[HAPTIC_FEEDBACK_KEY] ?: true
            )
        )
    }

    suspend fun updateLayoutSettings(layout: LayoutSettings) {
        dataStore.edit { prefs ->
            prefs[COLUMNS_KEY] = layout.columns
            prefs[ROWS_KEY] = layout.rows
            prefs[ICON_SIZE_KEY] = layout.iconSize
            prefs[ICON_SPACING_KEY] = layout.iconSpacing
            prefs[VERTICAL_OFFSET_KEY] = layout.verticalOffset
            prefs[HOME_DISPLAY_COUNT_KEY] = layout.homeDisplayCount
            prefs[DRAWER_FREQUENT_COUNT_KEY] = layout.drawerFrequentCount
            prefs[TEXT_SIZE_KEY] = layout.textSize
        }
    }

    suspend fun updateAppearanceSettings(appearance: AppearanceSettings) {
        dataStore.edit { prefs ->
            prefs[THEME_KEY] = appearance.theme.name
            prefs[BACKGROUND_TYPE_KEY] = appearance.backgroundType.name
            prefs[BLUR_STRENGTH_KEY] = appearance.blurStrength
            prefs[ICON_RADIUS_KEY] = appearance.iconRadius
            prefs[SHOW_SHADOW_KEY] = appearance.showShadow
        }
    }

    suspend fun updateClockSettings(clock: ClockSettings) {
        dataStore.edit { prefs ->
            prefs[SHOW_TIME_KEY] = clock.showTime
            prefs[SHOW_SECONDS_KEY] = clock.showSeconds
            prefs[SHOW_DATE_KEY] = clock.showDate
            prefs[SHOW_LUNAR_KEY] = clock.showLunar
            prefs[SHOW_FESTIVAL_KEY] = clock.showFestival
            prefs[IS_24_HOUR_KEY] = clock.is24Hour
        }
    }

    suspend fun updateSearchSettings(search: SearchSettings) {
        dataStore.edit { prefs ->
            prefs[ENABLE_SEARCH_KEY] = search.enableSearch
            prefs[ENABLE_PINYIN_KEY] = search.enablePinyin
            prefs[ENABLE_T9_KEY] = search.enableT9
            prefs[SHOW_SEARCH_HISTORY_KEY] = search.showSearchHistory
        }
    }

    suspend fun updateGestureSettings(gesture: GestureSettings) {
        dataStore.edit { prefs ->
            prefs[SWIPE_SENSITIVITY_KEY] = gesture.swipeSensitivity.name
            prefs[HAPTIC_FEEDBACK_KEY] = gesture.hapticFeedback
        }
    }

    // === FAB Position ===
    val fabPosition: Flow<Pair<Float, Float>> = dataStore.data.map { prefs ->
        Pair(
            prefs[FAB_OFFSET_X_KEY] ?: 0f,
            prefs[FAB_OFFSET_Y_KEY] ?: 0f
        )
    }

    suspend fun updateFabPosition(offsetX: Float, offsetY: Float) {
        dataStore.edit { prefs ->
            prefs[FAB_OFFSET_X_KEY] = offsetX
            prefs[FAB_OFFSET_Y_KEY] = offsetY
        }
    }

    /**
     * 导出所有设置为 Map
     */
    suspend fun exportAll(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        dataStore.data.collect { prefs ->
            prefs.asMap().forEach { (key, value) ->
                result[key.name] = value.toString()
            }
        }
        return result
    }

    /**
     * 从 Map 导入设置
     */
    suspend fun importAll(settings: Map<String, String>) {
        dataStore.edit { prefs ->
            settings.forEach { (key, value) ->
                when {
                    key.startsWith("layout_") || key.startsWith("appearance_blur") ||
                            key.startsWith("appearance_icon_radius") || key.startsWith("layout_") -> {
                        val intKey = intPreferencesKey(key)
                        value.toIntOrNull()?.let { prefs[intKey] = it }
                    }
                    key.contains("show_") || key.contains("enable_") ||
                            key.contains("is_") || key.contains("haptic") -> {
                        val boolKey = booleanPreferencesKey(key)
                        value.toBooleanStrictOrNull()?.let { prefs[boolKey] = it }
                    }
                    else -> {
                        val strKey = stringPreferencesKey(key)
                        prefs[strKey] = value
                    }
                }
            }
        }
    }
}
