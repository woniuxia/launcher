package cn.whc.launcher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.whc.launcher.ui.components.FloatingSearchButton
import cn.whc.launcher.ui.components.WallpaperBackground
import cn.whc.launcher.ui.viewmodel.LauncherViewModel
import kotlinx.coroutines.launch

/**
 * 主启动器内容 (VerticalPager: 首页 + 应用抽屉)
 */
@Composable
fun LauncherContent(
    viewModel: LauncherViewModel,
    onNavigateToSettings: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val homeApps by viewModel.homeApps.collectAsState()
    val frequentApps by viewModel.frequentApps.collectAsState()
    val allAppsGrouped by viewModel.allAppsGrouped.collectAsState()
    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 2 }
    )
    val coroutineScope = rememberCoroutineScope()

    // 监听页面变化
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            viewModel.setCurrentPage(page)
        }
    }

    // 返回键处理
    BackHandler(enabled = true) {
        when {
            isSearchActive -> viewModel.setSearchActive(false)
            pagerState.currentPage == 1 -> {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(0)
                }
            }
            // 在首页不退出
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
            beyondViewportPageCount = 1
        ) { page ->
            when (page) {
                0 -> HomePage(
                    homeApps = homeApps,
                    settings = settings,
                    onAppClick = { viewModel.launchApp(it.packageName) }
                )
                1 -> AppDrawerPage(
                    frequentApps = frequentApps,
                    allAppsGrouped = allAppsGrouped,
                    settings = settings,
                    onAppClick = { viewModel.launchApp(it.packageName) },
                    onSettingsClick = onNavigateToSettings
                )
            }
        }

        // 悬浮搜索按钮 (仅在应用抽屉页显示)
        if (pagerState.currentPage == 1 && settings.search.enableSearch) {
            FloatingSearchButton(
                isExpanded = isSearchActive,
                searchQuery = searchQuery,
                searchResults = searchResults,
                onToggle = { viewModel.setSearchActive(it) },
                onQueryChange = { viewModel.updateSearchQuery(it) },
                onAppClick = { viewModel.launchApp(it.packageName) },
                modifier = Modifier
                    .align(if (isSearchActive) Alignment.TopCenter else Alignment.BottomEnd)
                    .then(
                        if (!isSearchActive) Modifier.padding(end = 16.dp, bottom = 80.dp)
                        else Modifier
                    )
            )
        }
    }
}
