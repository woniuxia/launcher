package cn.whc.launcher.ui.viewmodel

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.whc.launcher.data.cache.HomePageCache
import cn.whc.launcher.data.cache.HomePageSnapshot
import cn.whc.launcher.data.model.AppInfo
import cn.whc.launcher.data.model.AppSettings
import cn.whc.launcher.data.model.CoreSettings
import cn.whc.launcher.data.model.PersonalPreset
import cn.whc.launcher.data.model.LayoutSettings
import cn.whc.launcher.data.model.AppearanceSettings
import cn.whc.launcher.data.model.ClockSettings
import cn.whc.launcher.data.model.SearchSettings
import cn.whc.launcher.data.model.GestureSettings
import cn.whc.launcher.data.repository.AppRepository
import cn.whc.launcher.data.repository.SettingsRepository
import cn.whc.launcher.util.IconCache
import cn.whc.launcher.util.SearchHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class LauncherViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository,
    private val iconCache: IconCache
) : ViewModel() {

    // 设置
    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    // 布局设置 (仅用于 homeApps/frequentApps 订阅)
    private val layoutSettings = settingsRepository.layoutSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LayoutSettings()
        )

    // 首页应用列表 (响应布局设置变化)
    // 初始值从缓存加载，后续由 Flow 更新
    private val _homeApps = MutableStateFlow<List<AppInfo>>(emptyList())
    @OptIn(ExperimentalCoroutinesApi::class)
    val homeApps: StateFlow<List<AppInfo>> = _homeApps.asStateFlow()

    // 缓存的首页应用 (用于 Flow 订阅，仅响应布局设置变化)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val homeAppsFlow = layoutSettings
        .flatMapLatest { layout ->
            appRepository.observeHomeApps(layout.homeDisplayCount)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 应用抽屉常用区应用 (仅响应布局设置变化)
    @OptIn(ExperimentalCoroutinesApi::class)
    val frequentApps: StateFlow<List<AppInfo>> = layoutSettings
        .flatMapLatest { layout ->
            appRepository.observeFrequentApps(
                excludeHomeApps = true,
                limit = layout.drawerFrequentCount,
                homeAppLimit = layout.homeDisplayCount
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 所有应用 (按字母分组)
    val allAppsGrouped: StateFlow<Map<String, List<AppInfo>>> = appRepository.observeAllAppsGrouped()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    // 数据是否就绪（用于启动时的过渡动画）
    private val _isDataReady = MutableStateFlow(false)
    val isDataReady: StateFlow<Boolean> = _isDataReady.asStateFlow()

    // 首页数据是否就绪（用于 SplashScreen 控制）
    private val _isHomeDataReady = MutableStateFlow(false)
    val isHomeDataReady: StateFlow<Boolean> = _isHomeDataReady.asStateFlow()

    // 搜索相关状态
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    private val _searchResults = MutableStateFlow<List<AppInfo>>(emptyList())
    val searchResults: StateFlow<List<AppInfo>> = _searchResults.asStateFlow()

    // 搜索基础数据缓存（由 allAppsGrouped 构建，避免每次输入访问数据库）
    private val _searchBaseApps = MutableStateFlow<List<AppInfo>>(emptyList())

    // 当前页面状态
    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    // 时间段推荐相关状态
    private val _timeBasedRecommendations = MutableStateFlow<List<AppInfo>>(emptyList())
    val timeBasedRecommendations: StateFlow<List<AppInfo>> = _timeBasedRecommendations.asStateFlow()

    private val _showTimeRecommendation = MutableStateFlow(false)
    val showTimeRecommendation: StateFlow<Boolean> = _showTimeRecommendation.asStateFlow()

    // FAB 位置状态
    val fabPosition: StateFlow<Pair<Float, Float>> = settingsRepository.fabPosition
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(0f, 0f))

    private var autoDismissJob: Job? = null

    // 缓存的首页快照 (用于保存)
    private var cachedSnapshot: HomePageSnapshot? = null
    private var lastRecommendationTimeBucket: Int? = null
    private val startupTraceStartMs = SystemClock.elapsedRealtime()

    private companion object {
        private const val TAG = "StartupTrace"
        private const val RECOMMENDATION_TIME_BUCKET_MINUTES = 30
        private const val POST_START_SYNC_DELAY_MS = 250L
        private const val ICON_PRELOAD_PRIMARY_COUNT = 6
        private const val ICON_PRELOAD_SECONDARY_DELAY_MS = 300L
        private const val STARTUP_SYNC_MIN_INTERVAL_MS = 30 * 60 * 1000L
    }

    init {
        Timber.tag(TAG).d("[S1] ViewModel init start")

        // 设置迁移放到后台执行，避免阻塞冷启动首屏
        viewModelScope.launch(Dispatchers.IO) {
            val migrationStart = SystemClock.elapsedRealtime()
            Timber.tag(TAG).d("[S2] schema migration start")
            val migrated = settingsRepository.ensureSchemaV2()
            val cost = SystemClock.elapsedRealtime() - migrationStart
            if (migrated) {
                Timber.tag(TAG).d("[S2] schema migration done, cost=%dms", cost)
            } else {
                Timber.tag(TAG).d("[S2] schema migration skipped, cost=%dms", cost)
            }
        }

        // 冷启动优化：优先从缓存加载，立即显示首页
        viewModelScope.launch(Dispatchers.Default) {
            val coldStartBegin = SystemClock.elapsedRealtime()
            Timber.tag(TAG).d("[S3] cold-start pipeline begin")

            // 使用内存中的最新设置，避免冷启动同步读取 DataStore
            val enableRecommendation = settings.value.layout.showTimeRecommendation
            Timber.tag(TAG).d("[S3] recommendation enabled=%s", enableRecommendation)

            val cacheLoadStart = SystemClock.elapsedRealtime()
            val cache = appRepository.loadHomePageFromCache()
            Timber.tag(TAG).d(
                "[S4] cache load done, hit=%s, cost=%dms",
                cache != null,
                SystemClock.elapsedRealtime() - cacheLoadStart
            )

            if (cache != null) {
                // 有缓存：立即使用缓存数据显示首页
                _homeApps.value = cache.homeApps.map { with(appRepository) { it.toAppInfo() } }
                if (enableRecommendation && shouldUseCachedRecommendations(cache)) {
                    _timeBasedRecommendations.value = cache.timeRecommendations.map {
                        with(appRepository) { it.toAppInfo() }
                    }
                    _showTimeRecommendation.value = true
                    lastRecommendationTimeBucket = currentTimeBucket()
                    startAutoDismissTimer()
                } else if (enableRecommendation) {
                    refreshTimeRecommendationsIfNeeded(force = true, source = "init-cache")
                }
                cachedSnapshot = cache
                _isHomeDataReady.value = true
                _isDataReady.value = true  // 缓存就绪即允许内容可见，无需等 Room
                Timber.tag(TAG).d(
                    "[S5] splash released (cache path), elapsed=%dms, homeApps=%d",
                    SystemClock.elapsedRealtime() - startupTraceStartMs,
                    _homeApps.value.size
                )

                // 预加载首页图标，使 Compose 首帧渲染时 LRU 已有数据
                val allComponentKeys = _homeApps.value.map { it.componentKey } +
                    _timeBasedRecommendations.value.map { it.componentKey }
                if (allComponentKeys.isNotEmpty()) {
                    viewModelScope.launch {
                        val primaryKeys = allComponentKeys.take(ICON_PRELOAD_PRIMARY_COUNT)
                        val secondaryKeys = allComponentKeys.drop(ICON_PRELOAD_PRIMARY_COUNT)

                        val primaryStart = SystemClock.elapsedRealtime()
                        Timber.tag(TAG).d(
                            "[S6A] icon preload start, count=%d",
                            primaryKeys.size
                        )
                        iconCache.preloadIcons(primaryKeys)
                        Timber.tag(TAG).d(
                            "[S6A] icon preload done, cost=%dms",
                            SystemClock.elapsedRealtime() - primaryStart
                        )

                        if (secondaryKeys.isNotEmpty()) {
                            delay(ICON_PRELOAD_SECONDARY_DELAY_MS)
                            val secondaryStart = SystemClock.elapsedRealtime()
                            Timber.tag(TAG).d(
                                "[S6B] icon preload start, count=%d, delayed=%dms",
                                secondaryKeys.size,
                                ICON_PRELOAD_SECONDARY_DELAY_MS
                            )
                            iconCache.preloadIcons(secondaryKeys)
                            Timber.tag(TAG).d(
                                "[S6B] icon preload done, cost=%dms",
                                SystemClock.elapsedRealtime() - secondaryStart
                            )
                        }
                    }
                }

                // 后台异步验证和更新数据
                viewModelScope.launch {
                    val cacheAgeMs = System.currentTimeMillis() - cache.timestamp
                    if (cacheAgeMs < STARTUP_SYNC_MIN_INTERVAL_MS) {
                        Timber.tag(TAG).d(
                            "[S7] app sync skipped, cacheAge=%dms < minInterval=%dms",
                            cacheAgeMs,
                            STARTUP_SYNC_MIN_INTERVAL_MS
                        )
                    } else {
                        delay(POST_START_SYNC_DELAY_MS)
                        val syncStart = SystemClock.elapsedRealtime()
                        Timber.tag(TAG).d("[S7] app sync start (delayed=%dms)", POST_START_SYNC_DELAY_MS)
                        appRepository.syncInstalledApps()
                        Timber.tag(TAG).d(
                            "[S7] app sync done, cost=%dms",
                            SystemClock.elapsedRealtime() - syncStart
                        )
                    }
                }
            } else {
                // 无缓存场景：优先展示空首页，后台同步数据
                _isHomeDataReady.value = true
                _isDataReady.value = true
                Timber.tag(TAG).d(
                    "[S5] splash released (no-cache path), elapsed=%dms",
                    SystemClock.elapsedRealtime() - startupTraceStartMs
                )

                if (enableRecommendation) {
                    // 延迟 100ms 加载时间推荐，确保首页先渲染
                    viewModelScope.launch {
                        delay(100)
                        refreshTimeRecommendationsIfNeeded(force = true, source = "init-no-cache")
                    }
                }

                viewModelScope.launch {
                    delay(POST_START_SYNC_DELAY_MS)
                    val syncStart = SystemClock.elapsedRealtime()
                    Timber.tag(TAG).d("[S7] app sync start (delayed=%dms)", POST_START_SYNC_DELAY_MS)
                    appRepository.syncInstalledApps()
                    Timber.tag(TAG).d(
                        "[S7] app sync done, cost=%dms",
                        SystemClock.elapsedRealtime() - syncStart
                    )
                }
            }

            Timber.tag(TAG).d(
                "[S8] cold-start pipeline end, cost=%dms",
                SystemClock.elapsedRealtime() - coldStartBegin
            )
        }

        // 监听 homeAppsFlow 更新 _homeApps 并保存缓存
        viewModelScope.launch {
            homeAppsFlow.collect { apps ->
                if (apps.isNotEmpty()) {
                    _homeApps.value = apps
                    // 保存缓存
                    saveHomePageCache()
                }
            }
        }

        // 监听全部数据就绪状态（allAppsGrouped 有数据时标记就绪）
        viewModelScope.launch {
            allAppsGrouped.collect { apps ->
                _searchBaseApps.value = apps.values.flatten()

                if (apps.isNotEmpty() && !_isDataReady.value) {
                    _isDataReady.value = true
                    // 如果还没有时间推荐数据且功能已启用，尝试加载
                    if (_timeBasedRecommendations.value.isEmpty()
                        && settings.value.layout.showTimeRecommendation
                    ) {
                        refreshTimeRecommendationsIfNeeded(force = true, source = "all-apps-ready")
                    }
                }
            }
        }

        // 监听黑灰名单变化，重新获取推荐（含补足逻辑）
        viewModelScope.launch {
            combine(
                appRepository.observeBlacklist(),
                appRepository.observeGraylist()
            ) { blacklist, graylist -> blacklist.size to graylist.size }
                .drop(1)
                .collect {
                    // 仅在功能启用且已有推荐数据时重新获取
                    if (_timeBasedRecommendations.value.isNotEmpty()
                        && settings.value.layout.showTimeRecommendation
                    ) {
                        refreshTimeRecommendationsIfNeeded(force = true, source = "list-changed")
                    }
                }
        }

        // 搜索防抖与去重
        viewModelScope.launch {
            _searchQuery
                .debounce(120)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isBlank()) {
                        _searchResults.value = emptyList()
                        return@collect
                    }

                    _searchResults.value = SearchHelper.searchApps(
                        query = query,
                        allApps = _searchBaseApps.value,
                        enablePinyin = settings.value.search.enablePinyin
                    )
                }
        }
    }

    /**
     * 保存首页缓存
     */
    private fun saveHomePageCache() {
        viewModelScope.launch {
            val currentHomeApps = _homeApps.value
            val currentRecommendations = _timeBasedRecommendations.value
            val availableLetters = allAppsGrouped.value.keys

            if (currentHomeApps.isNotEmpty()) {
                appRepository.saveHomePageCache(
                    homeApps = currentHomeApps,
                    availableLetters = availableLetters,
                    timeRecommendations = currentRecommendations,
                    timeRecommendationsTimestamp = System.currentTimeMillis()
                )
            }
        }
    }

    private fun currentTimeBucket(): Int {
        val calendar = Calendar.getInstance()
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        return currentMinutes / RECOMMENDATION_TIME_BUCKET_MINUTES
    }

    private fun shouldUseCachedRecommendations(cache: HomePageSnapshot): Boolean {
        if (cache.timeRecommendations.isEmpty()) return false
        val age = System.currentTimeMillis() - cache.timeRecommendationsTimestamp
        return age <= HomePageCache.TIME_RECOMMENDATION_MAX_AGE_MS
    }

    private fun refreshTimeRecommendationsIfNeeded(
        force: Boolean = false,
        source: String = "unknown"
    ) {
        if (!settings.value.layout.showTimeRecommendation) return
        if (!_isHomeDataReady.value && !force) return
        if (!force && _currentPage.value != 0) return

        val currentBucket = currentTimeBucket()
        val shouldRefresh = force || lastRecommendationTimeBucket == null ||
            lastRecommendationTimeBucket != currentBucket || _timeBasedRecommendations.value.isEmpty()

        if (!shouldRefresh) return

        viewModelScope.launch {
            val recommendationStart = SystemClock.elapsedRealtime()
            Timber.tag(TAG).d(
                "[R1] recommendation refresh start, source=%s, force=%s, bucket=%d",
                source,
                force,
                currentBucket
            )
            val recommendations = appRepository.getTimeBasedRecommendations()
            _timeBasedRecommendations.value = recommendations
            _showTimeRecommendation.value = recommendations.isNotEmpty()
            lastRecommendationTimeBucket = currentBucket
            saveHomePageCache()
            Timber.tag(TAG).d(
                "[R1] recommendation refresh done, count=%d, cost=%dms",
                recommendations.size,
                SystemClock.elapsedRealtime() - recommendationStart
            )
        }
    }

    /**
     * 启动应用
     */
    fun launchApp(packageName: String, activityName: String) {
        // 先启动应用，确保响应即时
        appRepository.launchApp(packageName, activityName)
        // 异步记录启动统计，不阻塞用户操作
        viewModelScope.launch {
            appRepository.recordAppLaunch(packageName, activityName)
        }
    }

    /**
     * 打开系统时钟
     */
    fun openClock() {
        appRepository.openClock()
    }

    /**
     * 更新搜索关键词
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * 设置搜索激活状态
     */
    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            _searchQuery.value = ""
            _searchResults.value = emptyList()
        }
    }

    /**
     * 更新当前页面
     */
    fun setCurrentPage(page: Int) {
        _currentPage.value = page
    }

    /**
     * 添加到黑名单
     */
    fun addToBlacklist(packageName: String, activityName: String) {
        viewModelScope.launch {
            appRepository.addToBlacklist(packageName, activityName)
        }
    }

    /**
     * 从黑名单移除
     */
    fun removeFromBlacklist(packageName: String, activityName: String) {
        viewModelScope.launch {
            appRepository.removeFromBlacklist(packageName, activityName)
        }
    }

    /**
     * 获取黑名单应用列表
     */
    val blacklist: StateFlow<List<AppInfo>> = appRepository.observeBlacklist()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * 添加到灰名单
     */
    fun addToGraylist(packageName: String, activityName: String) {
        viewModelScope.launch {
            appRepository.addToGraylist(packageName, activityName)
        }
    }

    /**
     * 从灰名单移除
     */
    fun removeFromGraylist(packageName: String, activityName: String) {
        viewModelScope.launch {
            appRepository.removeFromGraylist(packageName, activityName)
        }
    }

    /**
     * 获取灰名单应用列表
     */
    val graylist: StateFlow<List<AppInfo>> = appRepository.observeGraylist()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * 更新布局设置
     */
    fun updateLayoutSettings(layout: LayoutSettings) {
        viewModelScope.launch {
            settingsRepository.updateLayoutSettings(layout)
        }
    }

    /**
     * 更新外观设置
     */
    fun updateAppearanceSettings(appearance: AppearanceSettings) {
        viewModelScope.launch {
            settingsRepository.updateAppearanceSettings(appearance)
        }
    }

    /**
     * 更新时钟设置
     */
    fun updateClockSettings(clock: ClockSettings) {
        viewModelScope.launch {
            settingsRepository.updateClockSettings(clock)
        }
    }

    /**
     * 更新搜索设置
     */
    fun updateSearchSettings(search: SearchSettings) {
        viewModelScope.launch {
            settingsRepository.updateSearchSettings(search)
        }
    }

    /**
     * 更新手势设置
     */
    fun updateGestureSettings(gesture: GestureSettings) {
        viewModelScope.launch {
            settingsRepository.updateGestureSettings(gesture)
        }
    }

    /**
     * 刷新应用列表
     */
    fun refreshApps() {
        viewModelScope.launch {
            appRepository.syncInstalledApps()
        }
    }

    /**
     * 处理应用安装
     */
    fun onAppInstalled(packageName: String) {
        viewModelScope.launch {
            appRepository.onAppInstalled(packageName)
        }
    }

    /**
     * 处理应用卸载
     */
    fun onAppUninstalled(packageName: String) {
        viewModelScope.launch {
            appRepository.onAppUninstalled(packageName)
        }
    }

    /**
     * 处理应用更新
     */
    fun onAppUpdated(packageName: String) {
        viewModelScope.launch {
            appRepository.onAppUpdated(packageName)
        }
    }

    /**
     * 刷新应用排序（页面重新激活时调用）
     */
    fun refreshAppSort() {
        appRepository.triggerSortRefresh()
    }

    /**
     * 恢复时间段推荐显示（页面 resume 时调用）
     * 如果有推荐数据但 FAB 被隐藏了，恢复显示
     */
    fun restoreTimeRecommendation() {
        refreshTimeRecommendationsIfNeeded(source = "resume")

        if (settings.value.layout.showTimeRecommendation
            && _timeBasedRecommendations.value.isNotEmpty()
            && !_showTimeRecommendation.value
        ) {
            _showTimeRecommendation.value = true
        }
    }

    /**
     * 加载时间段推荐应用
     */
    /**
     * 启动自动关闭计时器 (已移至组件内部处理)
     */
    private fun startAutoDismissTimer() {
        // 组件内部已有收起动画逻辑，这里不再隐藏
    }

    /**
     * 关闭时间段推荐
     */
    fun dismissTimeRecommendation() {
        autoDismissJob?.cancel()
        _showTimeRecommendation.value = false
    }

    /**
     * 从时间段推荐启动应用
     */
    fun launchRecommendedApp(app: AppInfo) {
        dismissTimeRecommendation()
        launchApp(app.packageName, app.activityName)
    }

    /**
     * 更新 FAB 位置
     */
    fun updateFabPosition(offsetX: Float, offsetY: Float) {
        viewModelScope.launch {
            settingsRepository.updateFabPosition(offsetX, offsetY)
        }
    }

    fun applyPreset(preset: PersonalPreset) {
        val current = settings.value
        val target = when (preset) {
            PersonalPreset.LITE -> CoreSettings(
                preset = preset,
                homeDisplayCount = 12,
                drawerFrequentCount = 4,
                showSearch = true,
                showTimeRecommendation = false,
                backgroundType = current.appearance.backgroundType,
                blurStrength = 12,
                iconSize = 54,
                hapticFeedback = false
            )
            PersonalPreset.BALANCED -> CoreSettings(
                preset = preset,
                homeDisplayCount = 16,
                drawerFrequentCount = 5,
                showSearch = true,
                showTimeRecommendation = true,
                backgroundType = current.appearance.backgroundType,
                blurStrength = 20,
                iconSize = 56,
                hapticFeedback = true
            )
            PersonalPreset.FOCUS -> CoreSettings(
                preset = preset,
                homeDisplayCount = 20,
                drawerFrequentCount = 6,
                showSearch = true,
                showTimeRecommendation = true,
                backgroundType = current.appearance.backgroundType,
                blurStrength = 10,
                iconSize = 58,
                hapticFeedback = true
            )
        }

        viewModelScope.launch {
            settingsRepository.updateCoreSettings(target)
        }
    }
}
