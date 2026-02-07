package cn.whc.launcher.ui.components

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import cn.whc.launcher.R
import cn.whc.launcher.data.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 时间段推荐悬浮组件
 * 位置: 可拖动，位置持久化
 * 样式: FAB + 5个应用图标扇形展开
 * 行为: 启动时展开，3秒后自动收起变透明，支持拖动
 */
@Composable
fun TimeBasedRecommendation(
    visible: Boolean,
    recommendations: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    fabOffsetX: Float,
    fabOffsetY: Float,
    onPositionChanged: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // 展开状态：启动时默认展开
    // key 绑定 visible，当 visible 变化时重置状态
    var isExpanded by remember(visible) { mutableStateOf(false) }
    // 是否已完成首次自动收起
    var hasAutoCollapsed by remember(visible) { mutableStateOf(false) }
    // 是否正在拖动
    var isDragging by remember { mutableStateOf(false) }

    val expandProgress = remember { Animatable(0f) }
    val fabAlphaAnim = remember { Animatable(1f) }

    // 拖动偏移量
    var dragOffsetX by remember { mutableFloatStateOf(fabOffsetX) }
    var dragOffsetY by remember { mutableFloatStateOf(fabOffsetY) }

    val coroutineScope = rememberCoroutineScope()

    // 同步外部传入的位置
    LaunchedEffect(fabOffsetX, fabOffsetY) {
        dragOffsetX = fabOffsetX
        dragOffsetY = fabOffsetY
    }

    // 首次显示时展开，3秒后自动收起
    LaunchedEffect(visible) {
        if (visible && !hasAutoCollapsed) {
            isExpanded = true
            fabAlphaAnim.snapTo(1f)
            expandProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            // 3秒后自动收起（用户手动收起时协程会被取消）
            delay(3000)
            if (!isDragging && isExpanded) {
                isExpanded = false
                hasAutoCollapsed = true
                expandProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 200)
                )
                // 收起后变半透明
                fabAlphaAnim.animateTo(0.6f, tween(300))
            }
        }
    }

    // 响应手动展开/收起（首次自动展开期间的手动收起也在此处理）
    LaunchedEffect(isExpanded) {
        if (!isExpanded && !hasAutoCollapsed && expandProgress.value > 0f) {
            // 首次自动展开期间用户手动点击收起
            hasAutoCollapsed = true
            expandProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 200)
            )
            fabAlphaAnim.animateTo(0.6f, tween(300))
        } else if (hasAutoCollapsed) {
            if (isExpanded) {
                fabAlphaAnim.animateTo(1f, tween(200))
                expandProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            } else {
                expandProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 200)
                )
                // 收起后变半透明
                fabAlphaAnim.animateTo(0.6f, tween(300))
            }
        }
    }

    val radius = 80.dp
    val radiusPx = with(LocalDensity.current) { radius.toPx() }
    val iconSize = 44.dp
    val fabSize = 44.dp

    // 角度配置: 5个应用图标左侧半圆展开 (从上到下180度，最常用在上方)
    val angles = listOf(
        270.0 * PI / 180.0,  // 上 (最常用)
        225.0 * PI / 180.0,  // 左上
        180.0 * PI / 180.0,  // 左
        135.0 * PI / 180.0,  // 左下
        90.0 * PI / 180.0    // 下
    )

    // FAB 交互状态
    val currentFabAlpha = if (isDragging) 1f else fabAlphaAnim.value

    // FAB 始终显示（只要 visible 为 true）
    if (visible) {
        BoxWithConstraints(modifier = modifier) {
            val maxWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
            val maxHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
            val fabSizePx = with(LocalDensity.current) { fabSize.toPx() }

            Box(
                modifier = Modifier
                    .offset { IntOffset(dragOffsetX.roundToInt(), dragOffsetY.roundToInt()) }
                    .alpha(currentFabAlpha)
            ) {
                // 展开的应用图标
                recommendations.take(5).forEachIndexed { index, app ->
                    val angle = angles.getOrElse(index) { angles.last() }

                    val offsetX = (cos(angle) * radiusPx * expandProgress.value).roundToInt()
                    val offsetY = (sin(angle) * radiusPx * expandProgress.value).roundToInt()

                    val itemAlpha = ((expandProgress.value - index * 0.08f) / 0.6f).coerceIn(0f, 1f)

                    RecommendationIcon(
                        app = app,
                        onClick = { onAppClick(app) },
                        modifier = Modifier
                            .offset { IntOffset(offsetX, offsetY) }
                            .alpha(itemAlpha)
                            .size(iconSize)
                    )
                }

                // 中心 FAB 按钮 (支持拖动和点击)
                Box(
                    modifier = Modifier
                        .size(fabSize)
                        .shadow(
                            elevation = 6.dp,
                            shape = CircleShape,
                            ambientColor = Color.Black.copy(alpha = 0.2f),
                            spotColor = Color.Black.copy(alpha = 0.2f)
                        )
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    isDragging = true
                                    coroutineScope.launch {
                                        fabAlphaAnim.animateTo(1f, tween(100))
                                    }
                                },
                                onDragEnd = {
                                    isDragging = false
                                    onPositionChanged(dragOffsetX, dragOffsetY)
                                    if (hasAutoCollapsed && !isExpanded) {
                                        coroutineScope.launch {
                                            fabAlphaAnim.animateTo(0.6f, tween(300))
                                        }
                                    }
                                },
                                onDragCancel = {
                                    isDragging = false
                                    if (hasAutoCollapsed && !isExpanded) {
                                        coroutineScope.launch {
                                            fabAlphaAnim.animateTo(0.6f, tween(300))
                                        }
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    // 限制在屏幕范围内
                                    dragOffsetX = (dragOffsetX + dragAmount.x).coerceIn(
                                        -maxWidthPx + fabSizePx,
                                        0f
                                    )
                                    dragOffsetY = (dragOffsetY + dragAmount.y).coerceIn(
                                        -maxHeightPx + fabSizePx,
                                        0f
                                    )
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    if (!isDragging) {
                                        isExpanded = !isExpanded
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (isExpanded) R.drawable.ic_close else R.drawable.ic_apps
                        ),
                        contentDescription = if (isExpanded) "收起" else "展开推荐",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecommendationIcon(
    app: AppInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var icon by remember(app.packageName, app.activityName) {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }

    LaunchedEffect(app.packageName, app.activityName) {
        icon = withContext(Dispatchers.IO) {
            try {
                val activityInfo = context.packageManager.getActivityInfo(
                    ComponentName(app.packageName, app.activityName),
                    PackageManager.GET_META_DATA
                )
                val drawable = activityInfo.loadIcon(context.packageManager)
                drawable?.toBitmap(
                    drawable.intrinsicWidth.coerceAtLeast(1),
                    drawable.intrinsicHeight.coerceAtLeast(1),
                    android.graphics.Bitmap.Config.ARGB_8888
                )
            } catch (e: Exception) {
                try {
                    val drawable = context.packageManager.getApplicationIcon(app.packageName)
                    drawable.toBitmap(
                        drawable.intrinsicWidth.coerceAtLeast(1),
                        drawable.intrinsicHeight.coerceAtLeast(1),
                        android.graphics.Bitmap.Config.ARGB_8888
                    )
                } catch (e2: Exception) {
                    null
                }
            }
        }
    }

    Box(
        modifier = modifier
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        icon?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = app.displayName,
                modifier = Modifier.size(44.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}
