package cn.whc.launcher.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Material You 主色调
val PrimaryBlue = Color(0xFF2196F3)
val PrimaryBlueDark = Color(0xFF1976D2)
val SecondaryPurple = Color(0xFF7C4DFF)
val TertiaryTeal = Color(0xFF00BCD4)
val AccentPink = Color(0xFFFF4081)

// 表面色 - 玻璃质感
val SurfaceLight = Color(0x1AFFFFFF)      // 10% 白
val SurfaceMedium = Color(0x26FFFFFF)     // 15% 白
val SurfaceElevated = Color(0x33FFFFFF)   // 20% 白
val SurfaceHighlight = Color(0x4DFFFFFF)  // 30% 白

// 边框和分隔线
val BorderLight = Color(0x1AFFFFFF)       // 10% 白
val BorderMedium = Color(0x33FFFFFF)      // 20% 白
val DividerColor = Color(0x26FFFFFF)      // 15% 白

// 文字层级
val OnSurfacePrimary = Color.White
val OnSurfaceSecondary = Color(0xB3FFFFFF)   // 70%
val OnSurfaceTertiary = Color(0x80FFFFFF)    // 50%
val OnSurfaceDisabled = Color(0x4DFFFFFF)    // 30%

// 阴影色
val ShadowColor = Color(0x40000000)          // 25% 黑
val ShadowColorLight = Color(0x26000000)     // 15% 黑

// 渐变
val PrimaryGradient = Brush.linearGradient(
    colors = listOf(PrimaryBlue, SecondaryPurple)
)

val SurfaceGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0x26FFFFFF),
        Color(0x1AFFFFFF)
    )
)

// 遮罩渐变
val OverlayGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0x66000000),  // 40% 黑 - 顶部
        Color(0x40000000),  // 25% 黑 - 中部
        Color(0x59000000)   // 35% 黑 - 底部
    )
)