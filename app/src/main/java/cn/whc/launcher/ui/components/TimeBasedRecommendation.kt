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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import cn.whc.launcher.data.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 时间段推荐悬浮组件
 * 位置: 首页右下角
 * 样式: 3个应用图标从右下角扇形展开
 */
@Composable
fun TimeBasedRecommendation(
    visible: Boolean,
    recommendations: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val expandProgress = remember { Animatable(0f) }

    LaunchedEffect(visible) {
        if (visible) {
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
        }
    }

    val radius = 70.dp
    val radiusPx = with(LocalDensity.current) { radius.toPx() }
    val iconSize = 52.dp

    // 角度配置: 90°(上方), 180°(左侧), 270°(下方)
    val angles = listOf(
        270.0 * PI / 180.0,  // 上方
        180.0 * PI / 180.0,  // 左侧
        90.0 * PI / 180.0    // 下方
    )

    if (visible || expandProgress.value > 0f) {
        Box(modifier = modifier) {
            recommendations.take(3).forEachIndexed { index, app ->
                val angle = angles.getOrElse(index) { angles.last() }

                val offsetX = (cos(angle) * radiusPx * expandProgress.value).roundToInt()
                val offsetY = (sin(angle) * radiusPx * expandProgress.value).roundToInt()

                val itemAlpha = ((expandProgress.value - index * 0.1f) / 0.7f).coerceIn(0f, 1f)

                RecommendationIcon(
                    app = app,
                    onClick = { onAppClick(app) },
                    modifier = Modifier
                        .offset { IntOffset(offsetX, offsetY) }
                        .alpha(itemAlpha)
                        .size(iconSize)
                )
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
                activityInfo.loadIcon(context.packageManager)?.toBitmap()
            } catch (e: Exception) {
                try {
                    context.packageManager.getApplicationIcon(app.packageName)?.toBitmap()
                } catch (e2: Exception) {
                    null
                }
            }
        }
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.3f),
                spotColor = Color.Black.copy(alpha = 0.3f)
            )
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.9f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        icon?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = app.displayName,
                modifier = Modifier
                    .padding(6.dp)
                    .size(40.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}
