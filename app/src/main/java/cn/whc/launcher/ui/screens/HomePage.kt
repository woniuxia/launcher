package cn.whc.launcher.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.whc.launcher.data.model.AppInfo
import cn.whc.launcher.data.model.AppSettings
import cn.whc.launcher.ui.components.AlphabetIndexBar
import cn.whc.launcher.ui.components.AppGrid
import cn.whc.launcher.ui.components.ClockWidget
import cn.whc.launcher.ui.components.TimeBasedRecommendation
import cn.whc.launcher.util.IconCache

/**
 * 首页屏幕
 */
@Composable
fun HomePage(
    homeApps: List<AppInfo>,
    availableLetters: Set<String>,
    settings: AppSettings,
    iconCache: IconCache,
    onAppClick: (AppInfo) -> Unit,
    onClockClick: () -> Unit,
    onLetterSelected: (String) -> Unit,
    showFavorites: Boolean = false,
    onFavoritesClick: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    showTimeRecommendation: Boolean = false,
    timeRecommendations: List<AppInfo> = emptyList(),
    onRecommendedAppClick: (AppInfo) -> Unit = {},
    fabOffsetX: Float = 0f,
    fabOffsetY: Float = 0f,
    onFabPositionChanged: (Float, Float) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 36.dp, end = 16.dp)
                    .offset(y = settings.layout.verticalOffset.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(48.dp))

                // 时间日期农历
                ClockWidget(
                    settings = settings.clock,
                    textColor = Color.White,
                    onClick = onClockClick
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 应用网格
                AppGrid(
                    apps = homeApps,
                    layoutSettings = settings.layout,
                    showShadow = settings.appearance.showShadow,
                    iconRadius = settings.appearance.iconRadius,
                    iconCache = iconCache,
                    onAppClick = onAppClick,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.weight(1f))
            }

            // 字母索引栏（只占下方2/3区域）
            Box(
                modifier = Modifier.fillMaxHeight(),
                contentAlignment = Alignment.BottomCenter
            ) {
                AlphabetIndexBar(
                    availableLetters = availableLetters,
                    onLetterSelected = onLetterSelected,
                    hapticEnabled = settings.gesture.hapticFeedback,
                    showFavorites = showFavorites,
                    onFavoritesClick = onFavoritesClick,
                    showSettings = onSettingsClick != null,
                    onSettingsClick = onSettingsClick,
                    modifier = Modifier
                        .fillMaxHeight(0.67f)
                        .padding(vertical = 32.dp, horizontal = 4.dp)
                )
            }
        }

        // 时间段推荐悬浮组件 (可拖动)
        TimeBasedRecommendation(
            visible = showTimeRecommendation,
            recommendations = timeRecommendations,
            onAppClick = onRecommendedAppClick,
            fabOffsetX = fabOffsetX,
            fabOffsetY = fabOffsetY,
            onPositionChanged = onFabPositionChanged,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 48.dp, bottom = 100.dp)
        )
    }
}
