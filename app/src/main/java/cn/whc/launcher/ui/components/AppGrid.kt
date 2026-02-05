package cn.whc.launcher.ui.components

import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import cn.whc.launcher.data.model.AppInfo
import cn.whc.launcher.data.model.LayoutSettings
import cn.whc.launcher.data.repository.componentKey
import cn.whc.launcher.ui.theme.BorderLight
import cn.whc.launcher.ui.theme.ShadowColor
import cn.whc.launcher.ui.theme.ShadowColorLight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 应用图标网格组件 - Material You 风格
 */
@Composable
fun AppGrid(
    apps: List<AppInfo>,
    layoutSettings: LayoutSettings,
    showShadow: Boolean,
    iconRadius: Int,
    onAppClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(layoutSettings.columns),
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(layoutSettings.iconSpacing.dp),
        verticalArrangement = Arrangement.spacedBy((layoutSettings.iconSpacing + 4).dp)
    ) {
        items(
            items = apps,
            key = { it.componentKey }
        ) { app ->
            AppGridItem(
                app = app,
                iconSize = layoutSettings.iconSize,
                textSize = layoutSettings.textSize,
                showShadow = showShadow,
                iconRadius = iconRadius,
                onClick = { onAppClick(app) }
            )
        }
    }
}

/**
 * 单个应用图标项 - 带按压动画和玻璃质感
 */
@Composable
fun AppGridItem(
    app: AppInfo,
    iconSize: Int,
    textSize: Int,
    showShadow: Boolean,
    iconRadius: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPressed by remember { mutableStateOf(false) }

    // 异步加载图标 - 使用 ActivityInfo.loadIcon 获取 Activity 专属图标
    var icon by remember(app.componentKey) { mutableStateOf<Drawable?>(null) }
    LaunchedEffect(app.componentKey) {
        icon = withContext(Dispatchers.IO) {
            try {
                val activityInfo = context.packageManager.getActivityInfo(
                    ComponentName(app.packageName, app.activityName),
                    PackageManager.GET_META_DATA
                )
                activityInfo.loadIcon(context.packageManager)
            } catch (e: Exception) {
                // 回退到应用图标
                try {
                    context.packageManager.getApplicationIcon(app.packageName)
                } catch (e2: Exception) {
                    null
                }
            }
        }
    }

    // 按压缩放动画
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 400f
        ),
        label = "scale"
    )

    val shape = RoundedCornerShape(iconRadius.dp)

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
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
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 图标
        Box(
            modifier = Modifier
                .size(iconSize.dp)
                .then(
                    if (showShadow) {
                        Modifier
                            .shadow(
                                elevation = 6.dp,
                                shape = shape,
                                ambientColor = ShadowColorLight,
                                spotColor = ShadowColor
                            )
                    } else Modifier
                )
                .clip(shape)
                .border(
                    width = 0.5.dp,
                    color = BorderLight,
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            icon?.let { drawable ->
                DrawableImage(
                    drawable = drawable,
                    contentDescription = app.displayName,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 名称 - 添加文字阴影
        Text(
            text = app.displayName,
            style = TextStyle(
                color = Color.White,
                fontSize = textSize.sp,
                textAlign = TextAlign.Center,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.5f),
                    offset = Offset(0f, 1f),
                    blurRadius = 3f
                )
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Drawable 图片组件
 */
@Composable
fun DrawableImage(
    drawable: Drawable,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(drawable) {
        drawable.toBitmap().asImageBitmap()
    }
    Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}
