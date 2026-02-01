package cn.whc.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.whc.launcher.data.model.AppSettings
import cn.whc.launcher.data.model.BackgroundType
import cn.whc.launcher.data.model.SwipeSensitivity
import cn.whc.launcher.data.model.Theme
import cn.whc.launcher.ui.viewmodel.LauncherViewModel

/**
 * 设置页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: LauncherViewModel,
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 顶部栏
            TopAppBar(
                title = {
                    Text(
                        text = "设置",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // 布局设置
                item {
                    SettingsSection(title = "布局设置")
                }

                item {
                    SliderSettingItem(
                        title = "网格列数",
                        value = settings.layout.columns.toFloat(),
                        valueRange = 3f..6f,
                        steps = 2,
                        valueLabel = "${settings.layout.columns} 列",
                        onValueChange = {
                            viewModel.updateLayoutSettings(settings.layout.copy(columns = it.toInt()))
                        }
                    )
                }

                item {
                    SliderSettingItem(
                        title = "网格行数",
                        value = settings.layout.rows.toFloat(),
                        valueRange = 2f..6f,
                        steps = 3,
                        valueLabel = "${settings.layout.rows} 行",
                        onValueChange = {
                            viewModel.updateLayoutSettings(settings.layout.copy(rows = it.toInt()))
                        }
                    )
                }

                item {
                    SliderSettingItem(
                        title = "图标大小",
                        value = settings.layout.iconSize.toFloat(),
                        valueRange = 48f..72f,
                        steps = 5,
                        valueLabel = "${settings.layout.iconSize}px",
                        onValueChange = {
                            viewModel.updateLayoutSettings(settings.layout.copy(iconSize = it.toInt()))
                        }
                    )
                }

                item {
                    SliderSettingItem(
                        title = "图标间距",
                        value = settings.layout.iconSpacing.toFloat(),
                        valueRange = 8f..32f,
                        steps = 5,
                        valueLabel = "${settings.layout.iconSpacing}px",
                        onValueChange = {
                            viewModel.updateLayoutSettings(settings.layout.copy(iconSpacing = it.toInt()))
                        }
                    )
                }

                item {
                    SliderSettingItem(
                        title = "垂直偏移",
                        value = settings.layout.verticalOffset.toFloat(),
                        valueRange = -200f..200f,
                        steps = 19,
                        valueLabel = "${settings.layout.verticalOffset}px",
                        onValueChange = {
                            viewModel.updateLayoutSettings(settings.layout.copy(verticalOffset = it.toInt()))
                        }
                    )
                }

                item {
                    SliderSettingItem(
                        title = "首页显示数量",
                        value = settings.layout.homeDisplayCount.toFloat(),
                        valueRange = 6f..36f,
                        steps = 14,
                        valueLabel = "${settings.layout.homeDisplayCount} 个",
                        onValueChange = {
                            viewModel.updateLayoutSettings(settings.layout.copy(homeDisplayCount = it.toInt()))
                        }
                    )
                }

                item {
                    SliderSettingItem(
                        title = "抽屉常用区数量",
                        value = settings.layout.drawerFrequentCount.toFloat(),
                        valueRange = 3f..15f,
                        steps = 11,
                        valueLabel = "${settings.layout.drawerFrequentCount} 个",
                        onValueChange = {
                            viewModel.updateLayoutSettings(settings.layout.copy(drawerFrequentCount = it.toInt()))
                        }
                    )
                }

                // 外观设置
                item {
                    SettingsSection(title = "外观设置")
                }

                item {
                    ChoiceSettingItem(
                        title = "主题模式",
                        currentValue = when (settings.appearance.theme) {
                            Theme.LIGHT -> "浅色"
                            Theme.DARK -> "深色"
                            Theme.SYSTEM -> "跟随系统"
                        },
                        options = listOf("浅色", "深色", "跟随系统"),
                        onSelect = { index ->
                            val theme = when (index) {
                                0 -> Theme.LIGHT
                                1 -> Theme.DARK
                                else -> Theme.SYSTEM
                            }
                            viewModel.updateAppearanceSettings(settings.appearance.copy(theme = theme))
                        }
                    )
                }

                item {
                    ChoiceSettingItem(
                        title = "背景样式",
                        currentValue = when (settings.appearance.backgroundType) {
                            BackgroundType.BLUR -> "模糊"
                            BackgroundType.SOLID -> "纯色"
                            BackgroundType.IMAGE -> "壁纸"
                        },
                        options = listOf("模糊", "纯色", "壁纸"),
                        onSelect = { index ->
                            val type = when (index) {
                                0 -> BackgroundType.BLUR
                                1 -> BackgroundType.SOLID
                                else -> BackgroundType.IMAGE
                            }
                            viewModel.updateAppearanceSettings(settings.appearance.copy(backgroundType = type))
                        }
                    )
                }

                item {
                    SliderSettingItem(
                        title = "模糊强度",
                        value = settings.appearance.blurStrength.toFloat(),
                        valueRange = 0f..50f,
                        steps = 9,
                        valueLabel = "${settings.appearance.blurStrength}",
                        onValueChange = {
                            viewModel.updateAppearanceSettings(settings.appearance.copy(blurStrength = it.toInt()))
                        }
                    )
                }

                item {
                    SliderSettingItem(
                        title = "图标圆角",
                        value = settings.appearance.iconRadius.toFloat(),
                        valueRange = 0f..32f,
                        steps = 7,
                        valueLabel = "${settings.appearance.iconRadius}px",
                        onValueChange = {
                            viewModel.updateAppearanceSettings(settings.appearance.copy(iconRadius = it.toInt()))
                        }
                    )
                }

                item {
                    SwitchSettingItem(
                        title = "图标阴影",
                        checked = settings.appearance.showShadow,
                        onCheckedChange = {
                            viewModel.updateAppearanceSettings(settings.appearance.copy(showShadow = it))
                        }
                    )
                }

                // 时间日期设置
                item {
                    SettingsSection(title = "时间日期设置")
                }

                item {
                    SwitchSettingItem(
                        title = "显示时间",
                        checked = settings.clock.showTime,
                        onCheckedChange = {
                            viewModel.updateClockSettings(settings.clock.copy(showTime = it))
                        }
                    )
                }

                item {
                    SwitchSettingItem(
                        title = "显示秒数",
                        checked = settings.clock.showSeconds,
                        onCheckedChange = {
                            viewModel.updateClockSettings(settings.clock.copy(showSeconds = it))
                        }
                    )
                }

                item {
                    SwitchSettingItem(
                        title = "显示日期",
                        checked = settings.clock.showDate,
                        onCheckedChange = {
                            viewModel.updateClockSettings(settings.clock.copy(showDate = it))
                        }
                    )
                }

                item {
                    SwitchSettingItem(
                        title = "显示农历",
                        checked = settings.clock.showLunar,
                        onCheckedChange = {
                            viewModel.updateClockSettings(settings.clock.copy(showLunar = it))
                        }
                    )
                }

                item {
                    SwitchSettingItem(
                        title = "24小时制",
                        checked = settings.clock.is24Hour,
                        onCheckedChange = {
                            viewModel.updateClockSettings(settings.clock.copy(is24Hour = it))
                        }
                    )
                }

                // 搜索设置
                item {
                    SettingsSection(title = "搜索设置")
                }

                item {
                    SwitchSettingItem(
                        title = "启用搜索",
                        checked = settings.search.enableSearch,
                        onCheckedChange = {
                            viewModel.updateSearchSettings(settings.search.copy(enableSearch = it))
                        }
                    )
                }

                item {
                    SwitchSettingItem(
                        title = "拼音搜索",
                        checked = settings.search.enablePinyin,
                        onCheckedChange = {
                            viewModel.updateSearchSettings(settings.search.copy(enablePinyin = it))
                        }
                    )
                }

                // 手势设置
                item {
                    SettingsSection(title = "手势设置")
                }

                item {
                    ChoiceSettingItem(
                        title = "上滑灵敏度",
                        currentValue = when (settings.gesture.swipeSensitivity) {
                            SwipeSensitivity.LOW -> "低"
                            SwipeSensitivity.MEDIUM -> "中等"
                            SwipeSensitivity.HIGH -> "高"
                        },
                        options = listOf("低", "中等", "高"),
                        onSelect = { index ->
                            val sensitivity = when (index) {
                                0 -> SwipeSensitivity.LOW
                                1 -> SwipeSensitivity.MEDIUM
                                else -> SwipeSensitivity.HIGH
                            }
                            viewModel.updateGestureSettings(settings.gesture.copy(swipeSensitivity = sensitivity))
                        }
                    )
                }

                item {
                    SwitchSettingItem(
                        title = "震动反馈",
                        checked = settings.gesture.hapticFeedback,
                        onCheckedChange = {
                            viewModel.updateGestureSettings(settings.gesture.copy(hapticFeedback = it))
                        }
                    )
                }

                // 关于
                item {
                    SettingsSection(title = "关于")
                }

                item {
                    TextSettingItem(
                        title = "版本",
                        value = "1.0.0"
                    )
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String) {
    Column {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            color = Color(0xFF007AFF),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun SliderSettingItem(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: String,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp
            )
            Text(
                text = valueLabel,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF007AFF),
                activeTrackColor = Color(0xFF007AFF),
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
private fun SwitchSettingItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF007AFF),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.White.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun ChoiceSettingItem(
    title: String,
    currentValue: String,
    options: List<String>,
    onSelect: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = option == currentValue
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) Color(0xFF007AFF)
                            else Color.White.copy(alpha = 0.1f)
                        )
                        .clickable { onSelect(index) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TextSettingItem(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp
        )
        Text(
            text = value,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp
        )
    }
}
