package cn.whc.launcher.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.whc.launcher.data.model.AppInfo
import cn.whc.launcher.ui.theme.BorderLight
import cn.whc.launcher.ui.theme.OnSurfaceTertiary
import cn.whc.launcher.ui.theme.PrimaryBlue
import cn.whc.launcher.ui.theme.PrimaryBlueDark
import cn.whc.launcher.ui.theme.SecondaryPurple
import cn.whc.launcher.ui.theme.ShadowColor
import cn.whc.launcher.ui.theme.SurfaceMedium

/**
 * 悬浮搜索按钮 - Material You 风格
 */
@Composable
fun FloatingSearchButton(
    isExpanded: Boolean,
    searchQuery: String,
    searchResults: List<AppInfo>,
    onToggle: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    // 关闭搜索的返回键处理
    BackHandler(enabled = isExpanded) {
        onToggle(false)
    }

    Box(modifier = modifier) {
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = {
                if (targetState) {
                    // 展开: 淡入 + 放大
                    (fadeIn(tween(200)) + scaleIn(
                        initialScale = 0.8f,
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
                    )) togetherWith
                    (fadeOut(tween(150)) + scaleOut(targetScale = 0.6f))
                } else {
                    // 收缩: 淡入 + 缩小回来
                    (fadeIn(tween(200)) + scaleIn(
                        initialScale = 1.2f,
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
                    )) togetherWith
                    (fadeOut(tween(150)) + scaleOut(targetScale = 1.2f))
                }
            },
            label = "searchTransition"
        ) { expanded ->
            if (expanded) {
                SearchOverlay(
                    query = searchQuery,
                    results = searchResults,
                    onQueryChange = onQueryChange,
                    onClose = { onToggle(false) },
                    onAppClick = {
                        onAppClick(it)
                        onToggle(false)
                    }
                )
            } else {
                // 渐变 FAB
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .shadow(
                            elevation = 12.dp,
                            shape = CircleShape,
                            ambientColor = ShadowColor,
                            spotColor = PrimaryBlueDark.copy(alpha = 0.4f)
                        )
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PrimaryBlue, SecondaryPurple)
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = { onToggle(true) }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜索",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 搜索覆盖层 - Material You 风格
 */
@Composable
private fun SearchOverlay(
    query: String,
    results: List<AppInfo>,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    onAppClick: (AppInfo) -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // 搜索框 - 玻璃质感
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = ShadowColor,
                    spotColor = PrimaryBlueDark.copy(alpha = 0.3f)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(SurfaceMedium)
                .border(
                    width = 1.dp,
                    color = if (query.isNotEmpty()) PrimaryBlue.copy(alpha = 0.4f) else BorderLight,
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = if (query.isNotEmpty()) PrimaryBlue else OnSurfaceTertiary,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 16.sp,
                    letterSpacing = 0.15.sp
                ),
                cursorBrush = SolidColor(PrimaryBlue),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = "搜索应用...",
                                color = OnSurfaceTertiary,
                                fontSize = 16.sp,
                                letterSpacing = 0.15.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = OnSurfaceTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 搜索结果
        if (results.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(
                    items = results,
                    key = { it.packageName }
                ) { app ->
                    AppListItem(
                        app = app,
                        onClick = { onAppClick(app) }
                    )
                }
            }
        } else if (query.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "未找到匹配的应用",
                    color = OnSurfaceTertiary,
                    fontSize = 14.sp,
                    letterSpacing = 0.25.sp
                )
            }
        }
    }
}
