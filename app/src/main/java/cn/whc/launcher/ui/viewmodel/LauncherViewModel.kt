package cn.whc.launcher.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.whc.launcher.data.cache.HomePageSnapshot
import cn.whc.launcher.data.model.AppInfo
import cn.whc.launcher.data.model.AppSettings
import cn.whc.launcher.data.model.LayoutSettings
import cn.whc.launcher.data.model.AppearanceSettings
import cn.whc.launcher.data.model.ClockSettings
import cn.whc.launcher.data.model.SearchSettings
import cn.whc.launcher.data.model.GestureSettings
import cn.whc.launcher.data.repository.AppRepository
import cn.whc.launcher.data.repository.SettingsRepository
import cn.whc.launcher.util.SearchHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LauncherViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository
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
        .stateIn(viewModelScope, SharingStarted.Eagerly, Pair(0f, 0f))

    private var autoDismissJob: Job? = null

    // 缓存的首页快照 (用于保存)
    private var cachedSnapshot: HomePageSnapshot? = null

    init {
        // 冷启动优化：优先从缓存加载，立即显示首页
        viewModelScope.launch {
            val cache = appRepository.loadHomePageFromCache()

            if (cache != null) {
                // 有缓存：立即使用缓存数据显示首页
                _homeApps.value = cache.homeApps.map { with(appRepository) { it.toAppInfo() } }
                if (cache.timeRecommendations.isNotEmpty()) {
                    _timeBasedRecommendations.value = cache.timeRecommendations.map {
                        with(appRepository) { it.toAppInfo() }
                    }
                    _showTimeRecommendation.value = true
                    startAutoDismissTimer()
                }
                cachedSnapshot = cache
                _isHomeDataReady.value = true

                // 后台异步验证和更新数据
                appRepository.syncInstalledApps()
            } else if (appRepository.hasHistoryData()) {
                // 无缓存但有历史数据：从数据库加载
                _isHomeDataReady.value = true

                // 延迟 100ms 加载时间推荐，确保首页先渲染
                delay(100)
                val recommendations = appRepository.getTimeBasedRecommendations()
                if (recommendations.isNotEmpty()) {
                    _timeBasedRecommendations.value = recommendations
                    _showTimeRecommendation.value = true
                    startAutoDismissTimer()
                }

                // 后台同步最新数据
                appRepository.syncInstalledApps()
            } else {
                // 首次启动：先显示空首页，后台同步数据
                _isHomeDataReady.value = true
                appRepository.syncInstalledApps()
            }
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
                if (apps.isNotEmpty() && !_isDataReady.value) {
                    _isDataReady.value = true
                    // 如果还没有时间推荐数据，尝试加载
                    if (_timeBasedRecommendations.value.isEmpty()) {
                        val recommendations = appRepository.getTimeBasedRecommendations()
                        if (recommendations.isNotEmpty()) {
                            _timeBasedRecommendations.value = recommendations
                            _showTimeRecommendation.value = true
                        }
                    }
                }
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
                    timeRecommendations = currentRecommendations
                )
            }
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
        viewModelScope.launch {
            if (query.isBlank()) {
                _searchResults.value = emptyList()
            } else {
                val allApps = appRepository.getAllApps()
                _searchResults.value = SearchHelper.searchApps(
                    query = query,
                    allApps = allApps,
                    enablePinyin = settings.value.search.enablePinyin
                )
            }
        }
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
        if (_timeBasedRecommendations.value.isNotEmpty() && !_showTimeRecommendation.value) {
            _showTimeRecommendation.value = true
        }
    }

    /**
     * 加载时间段推荐应用
     */
    private fun loadTimeBasedRecommendations() {
        viewModelScope.launch {
            val recommendations = appRepository.getTimeBasedRecommendations()
            if (recommendations.isNotEmpty()) {
                _timeBasedRecommendations.value = recommendations
                _showTimeRecommendation.value = true
                startAutoDismissTimer()
            }
        }
    }

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
}
