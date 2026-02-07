package cn.whc.launcher.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.whc.launcher.data.model.AppInfo
import cn.whc.launcher.data.model.AppSettings
import cn.whc.launcher.ui.components.AlphabetIndexBar
import cn.whc.launcher.ui.components.AppListItem
import cn.whc.launcher.ui.components.LetterHeader
import cn.whc.launcher.ui.components.SYMBOL_SETTINGS
import cn.whc.launcher.ui.theme.OnSurfacePrimary
import cn.whc.launcher.ui.theme.OnSurfaceSecondary
import cn.whc.launcher.ui.theme.PrimaryBlue
import cn.whc.launcher.ui.theme.SecondaryPurple
import cn.whc.launcher.ui.theme.ShadowColorLight
import cn.whc.launcher.ui.theme.SurfaceLight
import kotlinx.coroutines.launch

/**
 * 应用抽屉页面
 */
@Composable
fun AppDrawerPage(
    frequentApps: List<AppInfo>,
    allAppsGrouped: Map<String, List<AppInfo>>,
    settings: AppSettings,
    listState: LazyListState,
    onAppClick: (AppInfo) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    // 构建列表项
    val listItems = remember(frequentApps, allAppsGrouped) {
        buildList {
            // 常用区
            if (frequentApps.isNotEmpty()) {
                add(DrawerListItem.FrequentHeader)
                frequentApps.forEach { app ->
                    add(DrawerListItem.FrequentApp(app))
                }
                add(DrawerListItem.Divider)
            }

            // 按字母分组的所有应用
            allAppsGrouped.forEach { (letter, apps) ->
                add(DrawerListItem.LetterHeader(letter))
                apps.forEach { app ->
                    add(DrawerListItem.App(app))
                }
            }

            // 设置入口（带表头）
            add(DrawerListItem.LetterHeader(SYMBOL_SETTINGS))
            add(DrawerListItem.SettingsItem)
        }
    }

    // 字母与列表位置的映射
    val letterPositions = remember(listItems) {
        val positions = mutableMapOf<String, Int>()
        listItems.forEachIndexed { index, item ->
            if (item is DrawerListItem.LetterHeader) {
                positions[item.letter] = index
            }
        }
        positions
    }

    // 可用字母集合
    val availableLetters by remember(allAppsGrouped) {
        derivedStateOf { allAppsGrouped.keys + SYMBOL_SETTINGS }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // 主列表
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 20.dp)
            ) {
                items(
                    items = listItems,
                    key = { item ->
                        when (item) {
                            is DrawerListItem.FrequentHeader -> "frequent_header"
                            is DrawerListItem.FrequentApp -> "frequent_${item.app.componentKey}"
                            is DrawerListItem.Divider -> "divider"
                            is DrawerListItem.LetterHeader -> "header_${item.letter}"
                            is DrawerListItem.App -> "app_${item.app.componentKey}"
                            is DrawerListItem.SettingsItem -> "settings"
                        }
                    }
                ) { item ->
                    when (item) {
                        is DrawerListItem.FrequentHeader -> {
                            FrequentSectionHeader()
                        }
                        is DrawerListItem.FrequentApp -> {
                            AppListItem(
                                app = item.app,
                                onClick = { onAppClick(item.app) }
                            )
                        }
                        is DrawerListItem.Divider -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .height(0.5.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.White.copy(alpha = 0.15f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }
                        is DrawerListItem.LetterHeader -> {
                            LetterHeader(letter = item.letter)
                        }
                        is DrawerListItem.App -> {
                            AppListItem(
                                app = item.app,
                                onClick = { onAppClick(item.app) }
                            )
                        }
                        is DrawerListItem.SettingsItem -> {
                            SettingsItem(onClick = onSettingsClick)
                        }
                    }
                }
            }

            // 字母索引栏（只占下方2/3区域）
            Box(
                modifier = Modifier.fillMaxHeight(),
                contentAlignment = Alignment.BottomCenter
            ) {
                AlphabetIndexBar(
                    availableLetters = availableLetters,
                    onLetterSelected = { letter ->
                        letterPositions[letter]?.let { position ->
                            coroutineScope.launch {
                                // 直接定位到对应位置，偏移1/3屏幕高度使其显示在2/3处
                                val offset = -(listState.layoutInfo.viewportSize.height / 3)
                                listState.scrollToItem(position, offset)
                            }
                        }
                    },
                    hapticEnabled = settings.gesture.hapticFeedback,
                    showFavorites = frequentApps.isNotEmpty(),
                    onFavoritesClick = {
                        coroutineScope.launch {
                            // 直接定位到列表顶部（常用区）
                            listState.scrollToItem(0)
                        }
                    },
                    showSettings = true,
                    modifier = Modifier
                        .fillMaxHeight(0.67f)
                        .padding(vertical = 32.dp, horizontal = 4.dp)
                )
            }
        }
    }
}

/**
 * 常用区标题 - Material You 风格
 */
@Composable
private fun FrequentSectionHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 左侧强调色竖条
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(PrimaryBlue, SecondaryPurple)
                    ),
                    shape = RoundedCornerShape(1.5.dp)
                )
        )

        Text(
            text = "常用",
            color = OnSurfaceSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )
    }
}

/**
 * 设置入口项 - 与 AppListItem 保持一致的视觉风格
 */
@Composable
private fun SettingsItem(onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "settingsScale"
    )

    val bgAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.08f else 0f,
        animationSpec = tween(100),
        label = "settingsBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = bgAlpha))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(12.dp),
                    ambientColor = ShadowColorLight
                )
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "设置",
                tint = OnSurfaceSecondary,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = "设置",
            style = TextStyle(
                color = OnSurfacePrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.15.sp,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.3f),
                    offset = Offset(0f, 1f),
                    blurRadius = 2f
                )
            )
        )
    }
}

/**
 * 抽屉列表项类型
 */
private sealed class DrawerListItem {
    data object FrequentHeader : DrawerListItem()
    data class FrequentApp(val app: AppInfo) : DrawerListItem()
    data object Divider : DrawerListItem()
    data class LetterHeader(val letter: String) : DrawerListItem()
    data class App(val app: AppInfo) : DrawerListItem()
    data object SettingsItem : DrawerListItem()
}
