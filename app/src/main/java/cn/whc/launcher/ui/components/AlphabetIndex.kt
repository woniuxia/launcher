package cn.whc.launcher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import cn.whc.launcher.ui.theme.BorderLight
import cn.whc.launcher.ui.theme.OnSurfacePrimary
import cn.whc.launcher.ui.theme.OnSurfaceSecondary
import cn.whc.launcher.ui.theme.OnSurfaceTertiary
import cn.whc.launcher.ui.theme.PrimaryBlue
import cn.whc.launcher.ui.theme.SecondaryPurple
import cn.whc.launcher.ui.theme.ShadowColorLight
import cn.whc.launcher.ui.theme.SurfaceLight
import cn.whc.launcher.ui.theme.SurfaceMedium
import cn.whc.launcher.util.LocalIconCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale

// 特殊索引符号
const val SYMBOL_FAVORITES = "\u2726"
const val SYMBOL_SETTINGS = "\u25ce"

/**
 * 字母索引栏组件 - Material You 风格
 */
@Composable
fun AlphabetIndexBar(
    availableLetters: Set<String>,
    onLetterSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    hapticEnabled: Boolean = true,
    showFavorites: Boolean = false,
    onFavoritesClick: (() -> Unit)? = null,
    showSettings: Boolean = false,
    onSettingsClick: (() -> Unit)? = null,
    externalSelectedLetter: String? = null,
    onExternalLetterConsumed: () -> Unit = {}
) {
    // 构建字母列表
    val letters = remember(availableLetters, showFavorites, showSettings) {
        buildList {
            if (showFavorites) add(SYMBOL_FAVORITES)
            val allLetters = ('A'..'Z').map { it.toString() } + "#"
            addAll(allLetters.filter { it in availableLetters })
            if (showSettings) add(SYMBOL_SETTINGS)
        }
    }
    var selectedLetter by remember { mutableStateOf<String?>(null) }
    var showPopup by remember { mutableStateOf(false) }
    var isTouching by remember { mutableStateOf(false) }
    var dragY by remember { mutableStateOf(0f) }
    var columnHeightPx by remember { mutableStateOf(0) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    // 外部触发的字母选中：显示弹窗并计算 Y 位置
    LaunchedEffect(externalSelectedLetter) {
        if (externalSelectedLetter != null && externalSelectedLetter in letters) {
            val index = letters.indexOf(externalSelectedLetter)
            selectedLetter = externalSelectedLetter
            // 根据字母索引和列高度计算居中 Y 位置
            if (columnHeightPx > 0 && letters.isNotEmpty()) {
                val letterH = columnHeightPx.toFloat() / letters.size
                dragY = letterH * index + letterH / 2
            }
            showPopup = true
            onExternalLetterConsumed()
        }
    }

    // 手指离开后延时隐藏弹窗；触摸期间弹窗始终可见
    LaunchedEffect(showPopup, isTouching) {
        if (showPopup && !isTouching) {
            delay(600)
            showPopup = false
        }
    }

    // 处理字母选择的回调
    val handleLetterSelected: (String) -> Unit = { letter ->
        when (letter) {
            SYMBOL_FAVORITES -> onFavoritesClick?.invoke()
            else -> onLetterSelected(letter)
        }
    }

    if (letters.isEmpty()) return

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .width(28.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(14.dp))
                .onSizeChanged { columnHeightPx = it.height }
                .pointerInput(letters) {
                    val letterHeight = { size.height.toFloat() / letters.size }
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()

                        // 按下立即显示弹窗
                        isTouching = true
                        val downIndex = (down.position.y / letterHeight())
                            .toInt().coerceIn(0, letters.lastIndex)
                        val downLetter = letters[downIndex]
                        selectedLetter = downLetter
                        dragY = down.position.y
                        showPopup = true
                        if (hapticEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        handleLetterSelected(downLetter)

                        // 等待拖拽或抬起
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                change.consume()
                                isTouching = false
                                break
                            }
                            change.consume()
                            dragY = change.position.y
                            val moveIndex = (change.position.y / letterHeight())
                                .toInt().coerceIn(0, letters.lastIndex)
                            val moveLetter = letters[moveIndex]
                            if (moveLetter != selectedLetter) {
                                selectedLetter = moveLetter
                                if (hapticEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                handleLetterSelected(moveLetter)
                            }
                        }
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            letters.forEach { letter ->
                val isSelected = letter == selectedLetter && showPopup
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) PrimaryBlue.copy(alpha = 0.3f) else Color.Transparent,
                    animationSpec = tween(150),
                    label = "letterBg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) PrimaryBlue else OnSurfaceSecondary,
                    animationSpec = tween(150),
                    label = "letterColor"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // 选中时的圆形背景
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(bgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = letter,
                            fontSize = if (isSelected) 11.sp else 10.sp,
                            color = textColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // 放大提示弹窗 - 显示在索引栏左侧，Y 轴跟随手指
        if (showPopup && selectedLetter != null) {
            val popupSizePx = with(density) { 56.dp.toPx() }
            val gapPx = with(density) { 8.dp.toPx() }
            val offsetX = (-(popupSizePx + gapPx)).toInt()
            val offsetY = (dragY - popupSizePx / 2).toInt()
            LetterPopup(letter = selectedLetter!!, offsetX = offsetX, offsetY = offsetY)
        }
    }
}

/**
 * 字母放大提示弹窗 - 浅灰色风格，跟随手指位置
 */
@Composable
private fun BoxScope.LetterPopup(letter: String, offsetX: Int, offsetY: Int) {
    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(offsetX, offsetY),
        properties = PopupProperties(focusable = false)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(12.dp),
                    ambientColor = ShadowColorLight,
                    spotColor = Color.Black.copy(alpha = 0.15f)
                )
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.55f))
                .border(
                    width = 0.5.dp,
                    color = Color.Black.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = letter,
                color = Color(0xFF333333),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * 应用列表项组件 - Material You 风格
 */
@Composable
fun AppListItem(
    app: cn.whc.launcher.data.model.AppInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Int = 48,
    textSize: Int = 16
) {
    val iconCache = LocalIconCache.current
    var isPressed by remember { mutableStateOf(false) }

    // 异步加载图标（通过 IconCache，一次性转为 ImageBitmap）
    val componentKey = "${app.packageName}/${app.activityName}"
    var iconBitmap by remember(componentKey) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(componentKey) {
        iconBitmap = iconCache.getIcon(componentKey)?.asImageBitmap()
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "listItemScale"
    )

    val bgAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.08f else 0f,
        animationSpec = tween(100),
        label = "listItemBg"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = bgAlpha))
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
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 图标带轻微阴影
        Box(
            modifier = Modifier
                .size(iconSize.dp)
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(12.dp),
                    ambientColor = ShadowColorLight
                )
                .clip(RoundedCornerShape(12.dp))
        ) {
            iconBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap,
                    contentDescription = app.displayName,
                    modifier = Modifier.size(iconSize.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }

        Text(
            text = app.displayName,
            style = TextStyle(
                color = OnSurfacePrimary,
                fontSize = textSize.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.15.sp,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.3f),
                    offset = Offset(0f, 1f),
                    blurRadius = 2f
                )
            )
        )
    }
}

/**
 * 字母分组头 - Material You 风格
 */
@Composable
fun LetterHeader(
    letter: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 8.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceLight)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 左侧强调色竖条
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(PrimaryBlue, SecondaryPurple)
                        )
                    )
            )

            Text(
                text = letter,
                style = TextStyle(
                    color = OnSurfaceSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            )
        }

        // 底部渐变分隔线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            BorderLight,
                            Color.Transparent
                        )
                    )
                )
        )
    }
}
