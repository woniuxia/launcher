package cn.whc.launcher.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

    // 首页应用列表 (响应设置变化)
    @OptIn(ExperimentalCoroutinesApi::class)
    val homeApps: StateFlow<List<AppInfo>> = settings
        .flatMapLatest { s ->
            appRepository.observeHomeApps(s.layout.homeDisplayCount)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 应用抽屉常用区应用
    @OptIn(ExperimentalCoroutinesApi::class)
    val frequentApps: StateFlow<List<AppInfo>> = settings
        .flatMapLatest { s ->
            appRepository.observeFrequentApps(
                excludeHomeApps = true,
                limit = s.layout.drawerFrequentCount
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

    init {
        // 同步已安装应用
        viewModelScope.launch {
            appRepository.syncInstalledApps()
        }
    }

    /**
     * 启动应用
     */
    fun launchApp(packageName: String) {
        viewModelScope.launch {
            appRepository.recordAppLaunch(packageName)
            appRepository.launchApp(packageName)
        }
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
    fun addToBlacklist(packageName: String) {
        viewModelScope.launch {
            appRepository.addToBlacklist(packageName)
        }
    }

    /**
     * 从黑名单移除
     */
    fun removeFromBlacklist(packageName: String) {
        viewModelScope.launch {
            appRepository.removeFromBlacklist(packageName)
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
}
