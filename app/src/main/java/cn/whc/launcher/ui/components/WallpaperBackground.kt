package cn.whc.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import cn.whc.launcher.data.model.BackgroundType

/**
 * 壁纸背景组件
 *
 * 从 Android 13 开始，普通应用无法通过 WallpaperManager 获取用户壁纸。
 * 此组件利用主题中配置的 windowShowWallpaper=true，让系统在窗口后面渲染壁纸。
 *
 * - IMAGE: 透明背景，直接显示系统壁纸
 * - BLUR: 透明背景 + 半透明遮罩模拟模糊效果
 * - SOLID: 纯黑背景
 */
@Composable
fun WallpaperBackground(
    backgroundType: BackgroundType,
    blurRadius: Int = 20,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (backgroundType) {
            BackgroundType.SOLID -> {
                // 纯黑背景 - 完全覆盖系统壁纸
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }
            BackgroundType.IMAGE -> {
                // 透明背景 - 系统壁纸通过 windowShowWallpaper 显示在后面
                // 添加轻微渐变遮罩增加可读性
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black.copy(alpha = 0.35f),
                                    0.3f to Color.Black.copy(alpha = 0.2f),
                                    0.6f to Color.Black.copy(alpha = 0.15f),
                                    1.0f to Color.Black.copy(alpha = 0.3f)
                                )
                            )
                        )
                )
            }
            BackgroundType.BLUR -> {
                // 模糊效果 - 由于无法获取壁纸位图，使用更重的半透明遮罩模拟
                // blurRadius 越大，遮罩越深
                val blurAlpha = (0.3f + (blurRadius / 100f) * 0.4f).coerceIn(0.3f, 0.7f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black.copy(alpha = blurAlpha + 0.1f),
                                    0.3f to Color.Black.copy(alpha = blurAlpha - 0.05f),
                                    0.6f to Color.Black.copy(alpha = blurAlpha - 0.1f),
                                    1.0f to Color.Black.copy(alpha = blurAlpha + 0.05f)
                                )
                            )
                        )
                )
            }
        }
    }
}
