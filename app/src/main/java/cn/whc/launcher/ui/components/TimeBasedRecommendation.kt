package cn.whc.launcher.ui.components

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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import cn.whc.launcher.R
import cn.whc.launcher.data.model.AppInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 时间段推荐悬浮组件。
 *
 * 在 `visible=true` 时显示可拖拽 FAB，展开后以扇形展示最多 5 个推荐应用。
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
    // 展开状态：当 visible 变化时重置。
    var isExpanded by remember(visible) { mutableStateOf(false) }
    // 保持现有行为：默认不进行首次自动展开。
    var hasAutoCollapsed by remember(visible) { mutableStateOf(true) }
    // 用于避免点击与拖拽手势冲突。
    var isDragging by remember { mutableStateOf(false) }

    // 动画状态：菜单展开进度与 FAB 透明度。
    val expandProgress = remember { Animatable(0f) }
    val fabAlphaAnim = remember { Animatable(0.6f) }

    // FAB 拖拽偏移（可持久化恢复）。
    var dragOffsetX by remember { mutableFloatStateOf(fabOffsetX) }
    var dragOffsetY by remember { mutableFloatStateOf(fabOffsetY) }

    val coroutineScope = rememberCoroutineScope()

    // 同步外部传入的位置。
    LaunchedEffect(fabOffsetX, fabOffsetY) {
        dragOffsetX = fabOffsetX
        dragOffsetY = fabOffsetY
    }

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
            // 3 秒后自动收起（拖拽中则不收起）。
            delay(3000)
            if (!isDragging && isExpanded) {
                isExpanded = false
                hasAutoCollapsed = true
                expandProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 200)
                )
                // 收起后回到半透明。
                fabAlphaAnim.animateTo(0.6f, tween(300))
            }
        }
    }

    // 响应手动展开/收起。
    LaunchedEffect(isExpanded) {
        if (!isExpanded && !hasAutoCollapsed && expandProgress.value > 0f) {
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
                // 收起后保持半透明。
                fabAlphaAnim.animateTo(0.6f, tween(300))
            }
        }
    }

    val radius = 80.dp
    val radiusPx = with(LocalDensity.current) { radius.toPx() }
    val iconSize = 44.dp
    val fabSize = 44.dp

    // 5 个推荐图标的扇形角度布局。
    val angles = listOf(
        270.0 * PI / 180.0,  // 上
        225.0 * PI / 180.0,  // 左上
        180.0 * PI / 180.0,  // 左
        135.0 * PI / 180.0,  // 左下
        90.0 * PI / 180.0    // 下
    )

    val currentFabAlpha = if (isDragging) 1f else fabAlphaAnim.value

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
                recommendations.take(5).forEachIndexed { index, app ->
                    val angle = angles.getOrElse(index) { angles.last() }

                    val offsetX = (cos(angle) * radiusPx * expandProgress.value).roundToInt()
                    val offsetY = (sin(angle) * radiusPx * expandProgress.value).roundToInt()

                    // 图标透明度做阶梯变化，提升展开观感。
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
                                    // 限制在屏幕可视范围内。
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
    // 每个组件使用稳定 key 读取图标缓存。
    val iconCache = cn.whc.launcher.util.LocalIconCache.current
    val componentKey = "${app.packageName}/${app.activityName}"
    var iconBitmap by remember(componentKey) {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }

    LaunchedEffect(componentKey) {
        iconBitmap = iconCache.getIcon(componentKey)
    }

    Box(
        modifier = modifier
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        iconBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = app.displayName,
                modifier = Modifier.size(44.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}
