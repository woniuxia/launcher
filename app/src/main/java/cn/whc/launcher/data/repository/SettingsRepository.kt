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
import cn.whc.launcher.data.model.AdvancedSettings
import cn.whc.launcher.data.model.AppSettings
import cn.whc.launcher.data.model.AppearanceSettings
import cn.whc.launcher.data.model.BackgroundType
import cn.whc.launcher.data.model.ClockSettings
import cn.whc.launcher.data.model.CoreSettings
import cn.whc.launcher.data.model.GestureSettings
import cn.whc.launcher.data.model.LayoutSettings
import cn.whc.launcher.data.model.PersonalPreset
import cn.whc.launcher.data.model.SearchSettings
import cn.whc.launcher.data.model.SwipeSensitivity
import cn.whc.launcher.data.model.Theme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.settingsDataStore
    private val schemaEnsureMutex = Mutex()

    private companion object {
        @Volatile
        private var schemaEnsuredInProcess: Boolean = false
    }

    // === Schema Keys ===
    private val SETTINGS_SCHEMA_VERSION_KEY = intPreferencesKey("settings_schema_version")

    // === Core V2 Keys ===
    private val PRESET_KEY = stringPreferencesKey("preset")
    private val CORE_HOME_DISPLAY_COUNT_KEY = intPreferencesKey("core_home_display_count")
    private val CORE_DRAWER_FREQUENT_COUNT_KEY = intPreferencesKey("core_drawer_frequent_count")
    private val CORE_SHOW_SEARCH_KEY = booleanPreferencesKey("core_show_search")
    private val CORE_SHOW_TIME_RECOMMENDATION_KEY = booleanPreferencesKey("core_show_time_recommendation")
    private val CORE_BACKGROUND_TYPE_KEY = stringPreferencesKey("core_background_type")
    private val CORE_BLUR_STRENGTH_KEY = intPreferencesKey("core_blur_strength")
    private val CORE_ICON_SIZE_KEY = intPreferencesKey("core_icon_size")
    private val CORE_HAPTIC_FEEDBACK_KEY = booleanPreferencesKey("core_haptic_feedback")

    // === Advanced V2 Keys ===
    private val ADVANCED_SHOW_LUNAR_KEY = booleanPreferencesKey("advanced_show_lunar")
    private val ADVANCED_SHOW_FESTIVAL_KEY = booleanPreferencesKey("advanced_show_festival")
    private val ADVANCED_SWIPE_SENSITIVITY_KEY = stringPreferencesKey("advanced_swipe_sensitivity")
    private val ADVANCED_ENABLE_T9_KEY = booleanPreferencesKey("advanced_enable_t9")

    // === Legacy Layout Keys ===
    private val COLUMNS_KEY = intPreferencesKey("layout_columns")
    private val ROWS_KEY = intPreferencesKey("layout_rows")
    private val ICON_SIZE_KEY = intPreferencesKey("layout_icon_size")
    private val ICON_SPACING_KEY = intPreferencesKey("layout_icon_spacing")
    private val VERTICAL_OFFSET_KEY = intPreferencesKey("layout_vertical_offset")
    private val HOME_DISPLAY_COUNT_KEY = intPreferencesKey("layout_home_display_count")
    private val DRAWER_FREQUENT_COUNT_KEY = intPreferencesKey("layout_drawer_frequent_count")
    private val TEXT_SIZE_KEY = intPreferencesKey("layout_text_size")
    private val SHOW_TIME_RECOMMENDATION_KEY = booleanPreferencesKey("layout_show_time_recommendation")

    // === Legacy Appearance Keys ===
    private val THEME_KEY = stringPreferencesKey("appearance_theme")
    private val BACKGROUND_TYPE_KEY = stringPreferencesKey("appearance_background_type")
    private val BLUR_STRENGTH_KEY = intPreferencesKey("appearance_blur_strength")
    private val ICON_RADIUS_KEY = intPreferencesKey("appearance_icon_radius")
    private val SHOW_SHADOW_KEY = booleanPreferencesKey("appearance_show_shadow")

    // === Legacy Clock Keys ===
    private val SHOW_TIME_KEY = booleanPreferencesKey("clock_show_time")
    private val SHOW_SECONDS_KEY = booleanPreferencesKey("clock_show_seconds")
    private val SHOW_DATE_KEY = booleanPreferencesKey("clock_show_date")
    private val SHOW_LUNAR_KEY = booleanPreferencesKey("clock_show_lunar")
    private val SHOW_FESTIVAL_KEY = booleanPreferencesKey("clock_show_festival")
    private val IS_24_HOUR_KEY = booleanPreferencesKey("clock_is_24_hour")

    // === Legacy Search Keys ===
    private val ENABLE_SEARCH_KEY = booleanPreferencesKey("search_enable")
    private val ENABLE_PINYIN_KEY = booleanPreferencesKey("search_enable_pinyin")
    private val ENABLE_T9_KEY = booleanPreferencesKey("search_enable_t9")
    private val SHOW_SEARCH_HISTORY_KEY = booleanPreferencesKey("search_show_history")

    // === Legacy Gesture Keys ===
    private val SWIPE_SENSITIVITY_KEY = stringPreferencesKey("gesture_swipe_sensitivity")
    private val HAPTIC_FEEDBACK_KEY = booleanPreferencesKey("gesture_haptic_feedback")

    // === FAB Position Keys ===
    private val FAB_OFFSET_X_KEY = floatPreferencesKey("fab_offset_x")
    private val FAB_OFFSET_Y_KEY = floatPreferencesKey("fab_offset_y")

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        val core = CoreSettings(
            preset = enumValueOrDefault(prefs[PRESET_KEY], PersonalPreset.BALANCED),
            homeDisplayCount = prefs[CORE_HOME_DISPLAY_COUNT_KEY] ?: prefs[HOME_DISPLAY_COUNT_KEY] ?: 16,
            drawerFrequentCount = prefs[CORE_DRAWER_FREQUENT_COUNT_KEY] ?: prefs[DRAWER_FREQUENT_COUNT_KEY] ?: 5,
            showSearch = prefs[CORE_SHOW_SEARCH_KEY] ?: prefs[ENABLE_SEARCH_KEY] ?: true,
            showTimeRecommendation = prefs[CORE_SHOW_TIME_RECOMMENDATION_KEY]
                ?: prefs[SHOW_TIME_RECOMMENDATION_KEY]
                ?: true,
            backgroundType = enumValueOrDefault(
                prefs[CORE_BACKGROUND_TYPE_KEY] ?: prefs[BACKGROUND_TYPE_KEY],
                BackgroundType.BLUR
            ),
            blurStrength = prefs[CORE_BLUR_STRENGTH_KEY] ?: prefs[BLUR_STRENGTH_KEY] ?: 20,
            iconSize = prefs[CORE_ICON_SIZE_KEY] ?: prefs[ICON_SIZE_KEY] ?: 56,
            hapticFeedback = prefs[CORE_HAPTIC_FEEDBACK_KEY] ?: prefs[HAPTIC_FEEDBACK_KEY] ?: true
        )

        val schemaVersion = prefs[SETTINGS_SCHEMA_VERSION_KEY] ?: 1
        val advanced = AdvancedSettings(
            showLunar = prefs[ADVANCED_SHOW_LUNAR_KEY] ?: prefs[SHOW_LUNAR_KEY] ?: true,
            showFestival = prefs[ADVANCED_SHOW_FESTIVAL_KEY] ?: prefs[SHOW_FESTIVAL_KEY] ?: true,
            swipeSensitivity = enumValueOrDefault(
                prefs[ADVANCED_SWIPE_SENSITIVITY_KEY] ?: prefs[SWIPE_SENSITIVITY_KEY],
                SwipeSensitivity.MEDIUM
            ),
            enableT9 = prefs[ADVANCED_ENABLE_T9_KEY] ?: prefs[ENABLE_T9_KEY] ?: false,
            schemaVersion = schemaVersion
        )

        AppSettings(
            core = core,
            advanced = advanced,
            layout = LayoutSettings(
                columns = prefs[COLUMNS_KEY] ?: 4,
                rows = prefs[ROWS_KEY] ?: 4,
                iconSize = core.iconSize,
                iconSpacing = prefs[ICON_SPACING_KEY] ?: 16,
                verticalOffset = prefs[VERTICAL_OFFSET_KEY] ?: 0,
                homeDisplayCount = core.homeDisplayCount,
                drawerFrequentCount = core.drawerFrequentCount,
                textSize = prefs[TEXT_SIZE_KEY] ?: 12,
                showTimeRecommendation = core.showTimeRecommendation
            ),
            appearance = AppearanceSettings(
                theme = enumValueOrDefault(prefs[THEME_KEY], Theme.SYSTEM),
                backgroundType = core.backgroundType,
                blurStrength = core.blurStrength,
                iconRadius = prefs[ICON_RADIUS_KEY] ?: 16,
                showShadow = prefs[SHOW_SHADOW_KEY] ?: true
            ),
            clock = ClockSettings(
                showTime = prefs[SHOW_TIME_KEY] ?: true,
                showSeconds = prefs[SHOW_SECONDS_KEY] ?: false,
                showDate = prefs[SHOW_DATE_KEY] ?: true,
                showLunar = advanced.showLunar,
                showFestival = advanced.showFestival,
                is24Hour = prefs[IS_24_HOUR_KEY] ?: true
            ),
            search = SearchSettings(
                enableSearch = core.showSearch,
                enablePinyin = prefs[ENABLE_PINYIN_KEY] ?: true,
                enableT9 = advanced.enableT9,
                showSearchHistory = prefs[SHOW_SEARCH_HISTORY_KEY] ?: true
            ),
            gesture = GestureSettings(
                swipeSensitivity = advanced.swipeSensitivity,
                hapticFeedback = core.hapticFeedback
            )
        )
    }

    val layoutSettings: Flow<LayoutSettings> = settings.map { it.layout }

    suspend fun ensureSchemaV2(): Boolean {
        if (schemaEnsuredInProcess) return false

        return schemaEnsureMutex.withLock {
            if (schemaEnsuredInProcess) return@withLock false

            var migrated = false
            dataStore.edit { prefs ->
                val currentVersion = prefs[SETTINGS_SCHEMA_VERSION_KEY] ?: 1
                if (currentVersion >= 2) return@edit

                // 迁移策略：
                // - 新增 v2 键空间（core_* / advanced_* / preset）；
                // - 保留旧键不删除，确保旧版本回退或历史读取兼容；
                // - 默认预设落在 BALANCED，与当前产品默认一致。
                prefs[PRESET_KEY] = PersonalPreset.BALANCED.name
                prefs[CORE_HOME_DISPLAY_COUNT_KEY] = prefs[HOME_DISPLAY_COUNT_KEY] ?: 16
                prefs[CORE_DRAWER_FREQUENT_COUNT_KEY] = prefs[DRAWER_FREQUENT_COUNT_KEY] ?: 5
                prefs[CORE_SHOW_SEARCH_KEY] = prefs[ENABLE_SEARCH_KEY] ?: true
                prefs[CORE_SHOW_TIME_RECOMMENDATION_KEY] = prefs[SHOW_TIME_RECOMMENDATION_KEY] ?: true
                prefs[CORE_BACKGROUND_TYPE_KEY] = (prefs[BACKGROUND_TYPE_KEY] ?: BackgroundType.BLUR.name)
                prefs[CORE_BLUR_STRENGTH_KEY] = prefs[BLUR_STRENGTH_KEY] ?: 20
                prefs[CORE_ICON_SIZE_KEY] = prefs[ICON_SIZE_KEY] ?: 56
                prefs[CORE_HAPTIC_FEEDBACK_KEY] = prefs[HAPTIC_FEEDBACK_KEY] ?: true

                prefs[ADVANCED_SHOW_LUNAR_KEY] = prefs[SHOW_LUNAR_KEY] ?: true
                prefs[ADVANCED_SHOW_FESTIVAL_KEY] = prefs[SHOW_FESTIVAL_KEY] ?: true
                prefs[ADVANCED_SWIPE_SENSITIVITY_KEY] =
                    prefs[SWIPE_SENSITIVITY_KEY] ?: SwipeSensitivity.MEDIUM.name
                prefs[ADVANCED_ENABLE_T9_KEY] = prefs[ENABLE_T9_KEY] ?: false

                prefs[SETTINGS_SCHEMA_VERSION_KEY] = 2
                migrated = true
            }

            schemaEnsuredInProcess = true
            migrated
        }
    }

    suspend fun updateCoreSettings(core: CoreSettings) {
        dataStore.edit { prefs ->
            prefs[PRESET_KEY] = core.preset.name
            prefs[CORE_HOME_DISPLAY_COUNT_KEY] = core.homeDisplayCount
            prefs[CORE_DRAWER_FREQUENT_COUNT_KEY] = core.drawerFrequentCount
            prefs[CORE_SHOW_SEARCH_KEY] = core.showSearch
            prefs[CORE_SHOW_TIME_RECOMMENDATION_KEY] = core.showTimeRecommendation
            prefs[CORE_BACKGROUND_TYPE_KEY] = core.backgroundType.name
            prefs[CORE_BLUR_STRENGTH_KEY] = core.blurStrength
            prefs[CORE_ICON_SIZE_KEY] = core.iconSize
            prefs[CORE_HAPTIC_FEEDBACK_KEY] = core.hapticFeedback
            prefs[SETTINGS_SCHEMA_VERSION_KEY] = 2

            prefs[HOME_DISPLAY_COUNT_KEY] = core.homeDisplayCount
            prefs[DRAWER_FREQUENT_COUNT_KEY] = core.drawerFrequentCount
            prefs[ENABLE_SEARCH_KEY] = core.showSearch
            prefs[SHOW_TIME_RECOMMENDATION_KEY] = core.showTimeRecommendation
            prefs[BACKGROUND_TYPE_KEY] = core.backgroundType.name
            prefs[BLUR_STRENGTH_KEY] = core.blurStrength
            prefs[ICON_SIZE_KEY] = core.iconSize
            prefs[HAPTIC_FEEDBACK_KEY] = core.hapticFeedback
        }
    }

    suspend fun updateAdvancedSettings(advanced: AdvancedSettings) {
        dataStore.edit { prefs ->
            prefs[ADVANCED_SHOW_LUNAR_KEY] = advanced.showLunar
            prefs[ADVANCED_SHOW_FESTIVAL_KEY] = advanced.showFestival
            prefs[ADVANCED_SWIPE_SENSITIVITY_KEY] = advanced.swipeSensitivity.name
            prefs[ADVANCED_ENABLE_T9_KEY] = advanced.enableT9
            prefs[SETTINGS_SCHEMA_VERSION_KEY] = 2

            prefs[SHOW_LUNAR_KEY] = advanced.showLunar
            prefs[SHOW_FESTIVAL_KEY] = advanced.showFestival
            prefs[SWIPE_SENSITIVITY_KEY] = advanced.swipeSensitivity.name
            prefs[ENABLE_T9_KEY] = advanced.enableT9
        }
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
            prefs[SHOW_TIME_RECOMMENDATION_KEY] = layout.showTimeRecommendation

            prefs[CORE_ICON_SIZE_KEY] = layout.iconSize
            prefs[CORE_HOME_DISPLAY_COUNT_KEY] = layout.homeDisplayCount
            prefs[CORE_DRAWER_FREQUENT_COUNT_KEY] = layout.drawerFrequentCount
            prefs[CORE_SHOW_TIME_RECOMMENDATION_KEY] = layout.showTimeRecommendation
            prefs[SETTINGS_SCHEMA_VERSION_KEY] = 2
        }
    }

    suspend fun updateAppearanceSettings(appearance: AppearanceSettings) {
        dataStore.edit { prefs ->
            prefs[THEME_KEY] = appearance.theme.name
            prefs[BACKGROUND_TYPE_KEY] = appearance.backgroundType.name
            prefs[BLUR_STRENGTH_KEY] = appearance.blurStrength
            prefs[ICON_RADIUS_KEY] = appearance.iconRadius
            prefs[SHOW_SHADOW_KEY] = appearance.showShadow

            prefs[CORE_BACKGROUND_TYPE_KEY] = appearance.backgroundType.name
            prefs[CORE_BLUR_STRENGTH_KEY] = appearance.blurStrength
            prefs[SETTINGS_SCHEMA_VERSION_KEY] = 2
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

            prefs[ADVANCED_SHOW_LUNAR_KEY] = clock.showLunar
            prefs[ADVANCED_SHOW_FESTIVAL_KEY] = clock.showFestival
            prefs[SETTINGS_SCHEMA_VERSION_KEY] = 2
        }
    }

    suspend fun updateSearchSettings(search: SearchSettings) {
        dataStore.edit { prefs ->
            prefs[ENABLE_SEARCH_KEY] = search.enableSearch
            prefs[ENABLE_PINYIN_KEY] = search.enablePinyin
            prefs[ENABLE_T9_KEY] = search.enableT9
            prefs[SHOW_SEARCH_HISTORY_KEY] = search.showSearchHistory

            prefs[CORE_SHOW_SEARCH_KEY] = search.enableSearch
            prefs[ADVANCED_ENABLE_T9_KEY] = search.enableT9
            prefs[SETTINGS_SCHEMA_VERSION_KEY] = 2
        }
    }

    suspend fun updateGestureSettings(gesture: GestureSettings) {
        dataStore.edit { prefs ->
            prefs[SWIPE_SENSITIVITY_KEY] = gesture.swipeSensitivity.name
            prefs[HAPTIC_FEEDBACK_KEY] = gesture.hapticFeedback

            prefs[ADVANCED_SWIPE_SENSITIVITY_KEY] = gesture.swipeSensitivity.name
            prefs[CORE_HAPTIC_FEEDBACK_KEY] = gesture.hapticFeedback
            prefs[SETTINGS_SCHEMA_VERSION_KEY] = 2
        }
    }

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

    suspend fun exportAll(): Map<String, String> {
        // 单次快照导出，避免对 dataStore.data 持续 collect 导致协程不返回。
        val prefs = dataStore.data.first()
        return prefs.asMap().entries.associate { (key, value) ->
            key.name to value.toString()
        }
    }

    suspend fun importAll(settings: Map<String, String>) {
        dataStore.edit { prefs ->
            settings.forEach { (key, value) ->
                // 按 key 语义推断目标类型，导入时做安全解析：
                // - 解析失败直接跳过，避免坏数据污染本地配置；
                // - 未识别键按字符串保底落盘，兼容未来扩展键。
                when {
                    key.startsWith("layout_") || key.startsWith("appearance_blur") ||
                        key.startsWith("appearance_icon_radius") || key.startsWith("core_") -> {
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

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, default: T): T {
        if (value.isNullOrBlank()) return default
        return runCatching { enumValueOf<T>(value) }.getOrDefault(default)
    }
}
