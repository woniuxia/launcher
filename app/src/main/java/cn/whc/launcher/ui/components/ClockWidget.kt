package cn.whc.launcher.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import cn.whc.launcher.data.model.ClockSettings
import cn.whc.launcher.ui.theme.OnSurfaceSecondary
import cn.whc.launcher.ui.theme.OnSurfaceTertiary
import cn.whc.launcher.ui.theme.ShadowColor
import cn.whc.launcher.util.LunarCalendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 时间日期农历显示组件 - Material You 风格
 */
@Composable
fun ClockWidget(
    settings: ClockSettings,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    onClick: () -> Unit = {}
) {
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    var currentDate by remember { mutableStateOf(LocalDate.now()) }
    var isVisible by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // 入场动画立即触发（不再延迟）
    LaunchedEffect(Unit) {
        isVisible = true
    }

    // 仅在前台生命周期更新时间，避免后台无意义唤醒
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                currentTime = LocalTime.now()
                currentDate = LocalDate.now()
                delay(1000)
            }
        }
    }

    // 文字阴影效果
    val textShadow = Shadow(
        color = ShadowColor,
        offset = Offset(0f, 2f),
        blurRadius = 8f
    )

    val lightShadow = Shadow(
        color = ShadowColor.copy(alpha = 0.3f),
        offset = Offset(0f, 1f),
        blurRadius = 4f
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(250))
    ) {
        Column(
            modifier = modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 时间
            if (settings.showTime) {
                val timeFormatter = if (settings.is24Hour) {
                    if (settings.showSeconds) {
                        DateTimeFormatter.ofPattern("HH:mm:ss")
                    } else {
                        DateTimeFormatter.ofPattern("HH:mm")
                    }
                } else {
                    if (settings.showSeconds) {
                        DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.getDefault())
                    } else {
                        DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
                    }
                }

                Text(
                    text = currentTime.format(timeFormatter),
                    style = TextStyle(
                        color = textColor,
                        fontSize = 84.sp,
                        fontWeight = FontWeight.Thin,
                        letterSpacing = (-1).sp,
                        shadow = textShadow
                    )
                )
            }

            // 日期
            if (settings.showDate) {
                val dateFormatter = DateTimeFormatter.ofPattern("EEEE  MM月dd日", Locale.CHINESE)
                Text(
                    text = currentDate.format(dateFormatter),
                    style = TextStyle(
                        color = OnSurfaceSecondary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.5.sp,
                        shadow = lightShadow
                    )
                )
            }

            // 农历
            if (settings.showLunar) {
                var lunar by remember { mutableStateOf<LunarCalendar.LunarDate?>(null) }
                LaunchedEffect(currentDate) {
                    lunar = withContext(Dispatchers.Default) {
                        LunarCalendar.solarToLunar(currentDate)
                    }
                }
                Box(modifier = Modifier.height(20.dp)) {
                    Text(
                        text = lunar?.let { "农历${it.getFullDateStr()}" } ?: "",
                        style = TextStyle(
                            color = OnSurfaceTertiary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 0.3.sp,
                            shadow = lightShadow
                        )
                    )
                }
            }

            // 节气/节日倒计时
            if (settings.showFestival) {
                var nextEvent by remember { mutableStateOf<LunarCalendar.FestivalInfo?>(null) }
                LaunchedEffect(currentDate) {
                    nextEvent = withContext(Dispatchers.Default) {
                        LunarCalendar.getNextEvent(currentDate)
                    }
                }
                val displayText = nextEvent?.let { event ->
                    when (event.daysUntil) {
                        0 -> "今天${event.name}"
                        1 -> "明天${event.name}"
                        else -> "距${event.name}还有${event.daysUntil}天"
                    }
                } ?: ""

                Box(modifier = Modifier.height(20.dp)) {
                    Text(
                        text = displayText,
                        style = TextStyle(
                            color = OnSurfaceTertiary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 0.3.sp,
                            shadow = lightShadow
                        )
                    )
                }
            }
        }
    }
}
