package cn.whc.launcher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import cn.whc.launcher.data.model.SwipeSensitivity
import cn.whc.launcher.ui.components.FloatingSearchButton
import cn.whc.launcher.ui.components.WallpaperBackground
import cn.whc.launcher.ui.viewmodel.LauncherViewModel
import cn.whc.launcher.util.IconCache
import kotlinx.coroutines.launch

/**
 * 主启动器内容 (VerticalPager: 首页 + 应用抽屉)
 */
@Composable
fun LauncherContent(
    viewModel: LauncherViewModel,
    iconCache: IconCache,
    onNavigateToSettings: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val homeApps by viewModel.homeApps.collectAsState()
    val frequentApps by viewModel.frequentApps.collectAsState()
    val allAppsGrouped by viewModel.allAppsGrouped.collectAsState()
    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isDataReady by viewModel.isDataReady.collectAsState()
    val showTimeRecommendation by viewModel.showTimeRecommendation.collectAsState()
    val timeBasedRecommendations by viewModel.timeBasedRecommendations.collectAsState()
    val fabPosition by viewModel.fabPosition.collectAsState()

    // 内容淡入动画
    val contentAlpha by animateFloatAsState(
        targetValue = if (isDataReady) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "contentAlpha"
    )

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 2 }
    )
    val drawerListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 页面重新激活时刷新应用排序
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshAppSort()
    }

    // 计算抽屉列表中字母头的位置（与 AppDrawerPage 的列表结构一致）
    val drawerLetterPositions = remember(frequentApps, allAppsGrouped) {
        val positions = mutableMapOf<String, Int>()
        var index = 0
        // 常用区
        if (frequentApps.isNotEmpty()) {
            index++ // FrequentHeader
            index += frequentApps.size // FrequentApp items
            index++ // Divider
        }
        // 按字母分组的应用
        allAppsGrouped.forEach { (letter, apps) ->
            positions[letter] = index
            index++ // LetterHeader
            index += apps.size // App items
        }
        positions
    }

    // 可用字母集合
    val availableLetters by remember(allAppsGrouped) {
        derivedStateOf { allAppsGrouped.keys }
    }

    // 标记是否通过字母索引跳转（避免重置列表位置）
    var isLetterNavigation by remember { mutableStateOf(false) }

    // 监听页面变化
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            viewModel.setCurrentPage(page)
            // 从首页上滑进入应用抽屉时，滚动到列表顶部（除非是字母索引跳转）
            if (page == 1 && !isLetterNavigation) {
                drawerListState.scrollToItem(0)
            }
            isLetterNavigation = false
        }
    }

    // 返回键处理：仅在搜索激活或抽屉页时拦截
    BackHandler(enabled = isSearchActive || pagerState.currentPage == 1) {
        when {
            isSearchActive -> viewModel.setSearchActive(false)
            pagerState.currentPage == 1 -> {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(0)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 壁纸背景
        WallpaperBackground(
            backgroundType = settings.appearance.backgroundType,
            blurRadius = settings.appearance.blurStrength
        )

        // 页面内容
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            // 根据用户设置的灵敏度调整滑动切换阈值
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                snapPositionalThreshold = when (settings.gesture.swipeSensitivity) {
                    SwipeSensitivity.LOW -> 0.25f    // 需滑动 25% 屏幕高度
                    SwipeSensitivity.MEDIUM -> 0.12f // 需滑动 12% 屏幕高度
                    SwipeSensitivity.HIGH -> 0.04f   // 需滑动 4% 屏幕高度
                }
            )
        ) { page ->
            when (page) {
                0 -> {
                    HomePage(
                        homeApps = homeApps,
                        availableLetters = availableLetters,
                        settings = settings,
                        iconCache = iconCache,
                        onAppClick = { viewModel.launchApp(it.packageName, it.activityName) },
                        onClockClick = { viewModel.openClock() },
                        onLetterSelected = { letter ->
                            drawerLetterPositions[letter]?.let { position ->
                                coroutineScope.launch {
                                    isLetterNavigation = true
                                    // 直接跳转到抽屉页
                                    pagerState.scrollToPage(1)
                                    // 直接定位到对应位置，偏移1/3屏幕高度使其显示在2/3处
                                    val offset = -(drawerListState.layoutInfo.viewportSize.height / 3)
                                    drawerListState.scrollToItem(position, offset)
                                }
                            }
                        },
                        showFavorites = frequentApps.isNotEmpty(),
                        onFavoritesClick = {
                            coroutineScope.launch {
                                isLetterNavigation = true
                                // 直接跳转到抽屉页并定位到顶部（常用区）
                                pagerState.scrollToPage(1)
                                drawerListState.scrollToItem(0)
                            }
                        },
                        onSettingsClick = onNavigateToSettings,
                        showTimeRecommendation = showTimeRecommendation,
                        timeRecommendations = timeBasedRecommendations,
                        onRecommendedAppClick = { viewModel.launchRecommendedApp(it) },
                        fabOffsetX = fabPosition.first,
                        fabOffsetY = fabPosition.second,
                        onFabPositionChanged = { x, y -> viewModel.updateFabPosition(x, y) },
                        modifier = Modifier.alpha(contentAlpha)
                    )
                }
                1 -> {
                    AppDrawerPage(
                        frequentApps = frequentApps,
                        allAppsGrouped = allAppsGrouped,
                        settings = settings,
                        listState = drawerListState,
                        onAppClick = { viewModel.launchApp(it.packageName, it.activityName) },
                        onSettingsClick = onNavigateToSettings,
                        modifier = Modifier.alpha(contentAlpha)
                    )
                }
            }
        }

        // 悬浮搜索按钮 (仅在应用抽屉页且数据就绪后显示)
        if (isDataReady && pagerState.currentPage == 1 && settings.search.enableSearch) {
            FloatingSearchButton(
                isExpanded = isSearchActive,
                searchQuery = searchQuery,
                searchResults = searchResults,
                onToggle = { viewModel.setSearchActive(it) },
                onQueryChange = { viewModel.updateSearchQuery(it) },
                onAppClick = { viewModel.launchApp(it.packageName, it.activityName) },
                modifier = Modifier
                    .align(if (isSearchActive) Alignment.TopCenter else Alignment.BottomEnd)
                    .then(
                        if (!isSearchActive) Modifier.padding(end = 48.dp, bottom = 80.dp)
                        else Modifier
                    )
            )
        }
    }
}
