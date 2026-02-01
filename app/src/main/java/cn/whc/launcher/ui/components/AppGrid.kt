package cn.whc.launcher.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import cn.whc.launcher.data.model.AppInfo
import cn.whc.launcher.data.model.LayoutSettings

/**
 * 应用图标网格组件
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
        verticalArrangement = Arrangement.spacedBy(layoutSettings.iconSpacing.dp)
    ) {
        items(
            items = apps,
            key = { it.packageName }
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
 * 单个应用图标项
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
    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 图标
        Box(
            modifier = Modifier
                .size(iconSize.dp)
                .then(
                    if (showShadow) {
                        Modifier.shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(iconRadius.dp)
                        )
                    } else Modifier
                )
                .clip(RoundedCornerShape(iconRadius.dp))
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            app.icon?.let { drawable ->
                DrawableImage(
                    drawable = drawable,
                    contentDescription = app.displayName,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 名称
        Text(
            text = app.displayName,
            color = Color.White,
            fontSize = textSize.sp,
            textAlign = TextAlign.Center,
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
