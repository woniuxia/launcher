package cn.whc.launcher.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.whc.launcher.data.model.ClockSettings
import cn.whc.launcher.util.LunarCalendar
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 时间日期农历显示组件
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

    // 每秒更新时间
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            currentDate = LocalDate.now()
            delay(1000)
        }
    }

    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
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
                color = textColor,
                fontSize = 72.sp,
                fontWeight = FontWeight.Light
            )
        }

        // 日期
        if (settings.showDate) {
            val dateFormatter = DateTimeFormatter.ofPattern("EEEE MM月dd日", Locale.CHINESE)
            Text(
                text = currentDate.format(dateFormatter),
                color = textColor.copy(alpha = 0.8f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal
            )
        }

        // 农历
        if (settings.showLunar) {
            val lunar = remember(currentDate) {
                LunarCalendar.solarToLunar(currentDate)
            }
            Text(
                text = "农历${lunar.getFullDateStr()}",
                color = textColor.copy(alpha = 0.6f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
