package cn.whc.launcher.ui.components

import android.Manifest
import android.app.WallpaperManager
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import cn.whc.launcher.data.model.BackgroundType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 壁纸背景组件
 */
@Composable
fun WallpaperBackground(
    backgroundType: BackgroundType,
    blurRadius: Int = 20,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var wallpaperBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var retryTrigger by remember { mutableIntStateOf(0) }

    // WallpaperManager 在所有版本上都需要 READ_EXTERNAL_STORAGE 权限
    val requiredPermission = Manifest.permission.READ_EXTERNAL_STORAGE

    // 初始化时就检查权限状态
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, requiredPermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    // 权限请求 launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (granted) {
            retryTrigger++
        }
    }

    // 检查权限并在需要时请求
    LaunchedEffect(backgroundType) {
        if (backgroundType != BackgroundType.SOLID && !permissionGranted) {
            permissionLauncher.launch(requiredPermission)
        }
    }

    // 获取壁纸
    LaunchedEffect(backgroundType, permissionGranted, retryTrigger) {
        if (backgroundType == BackgroundType.SOLID) {
            wallpaperBitmap = null
            return@LaunchedEffect
        }

        // 等待权限授予
        if (!permissionGranted) {
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            try {
                val wallpaperManager = WallpaperManager.getInstance(context)
                // 优先使用 peekDrawable（不会触发额外的权限检查）
                val drawable = wallpaperManager.peekDrawable() ?: wallpaperManager.drawable
                wallpaperBitmap = when (drawable) {
                    is BitmapDrawable -> drawable.bitmap
                    else -> {
                        // 处理非 BitmapDrawable 的情况（如动态壁纸）
                        // 尝试从 drawable 创建 bitmap
                        drawable?.let { d ->
                            if (d.intrinsicWidth > 0 && d.intrinsicHeight > 0) {
                                val bitmap = Bitmap.createBitmap(
                                    d.intrinsicWidth,
                                    d.intrinsicHeight,
                                    Bitmap.Config.ARGB_8888
                                )
                                val canvas = android.graphics.Canvas(bitmap)
                                d.setBounds(0, 0, canvas.width, canvas.height)
                                d.draw(canvas)
                                bitmap
                            } else null
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                wallpaperBitmap = null
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (backgroundType) {
            BackgroundType.BLUR -> {
                wallpaperBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    renderEffect = RenderEffect
                                        .createBlurEffect(
                                            blurRadius.toFloat(),
                                            blurRadius.toFloat(),
                                            Shader.TileMode.CLAMP
                                        )
                                        .asComposeRenderEffect()
                                }
                            },
                        contentScale = ContentScale.Crop
                    )
                } ?: Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }
            BackgroundType.IMAGE -> {
                wallpaperBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } ?: Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }
            BackgroundType.SOLID -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }
        }

        // 暗色遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        )
    }
}
