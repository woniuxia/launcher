package cn.whc.launcher.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

// 特殊索引符号
const val SYMBOL_FAVORITES = "✦"
const val SYMBOL_SETTINGS = "◎"

/**
 * 字母索引栏组件
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
    onSettingsClick: (() -> Unit)? = null
) {
    // 构建字母列表：✦ + 有数据的字母 + ◎
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
    val haptic = LocalHapticFeedback.current

    // 处理字母选择的回调
    val handleLetterSelected: (String) -> Unit = { letter ->
        when (letter) {
            SYMBOL_FAVORITES -> onFavoritesClick?.invoke()
            SYMBOL_SETTINGS -> onSettingsClick?.invoke()
            else -> onLetterSelected(letter)
        }
    }

    if (letters.isEmpty()) return

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .width(28.dp)
                .fillMaxHeight()
                .pointerInput(letters) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            showPopup = true
                            val index = (offset.y / (size.height.toFloat() / letters.size))
                                .toInt()
                                .coerceIn(0, letters.lastIndex)
                            val letter = letters[index]
                            if (letter != selectedLetter) {
                                selectedLetter = letter
                                if (hapticEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                handleLetterSelected(letter)
                            }
                        },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            val index = (change.position.y / (size.height.toFloat() / letters.size))
                                .toInt()
                                .coerceIn(0, letters.lastIndex)
                            val letter = letters[index]
                            if (letter != selectedLetter) {
                                selectedLetter = letter
                                if (hapticEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                handleLetterSelected(letter)
                            }
                        },
                        onDragEnd = {
                            showPopup = false
                        }
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            letters.forEach { letter ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clickable {
                            selectedLetter = letter
                            if (hapticEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            handleLetterSelected(letter)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = letter,
                        fontSize = 10.sp,
                        color = if (letter == selectedLetter && showPopup) {
                            Color(0xFF007AFF)
                        } else {
                            Color.White.copy(alpha = 0.8f)
                        },
                        fontWeight = if (letter == selectedLetter && showPopup) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 放大提示弹窗
        if (showPopup && selectedLetter != null) {
            LetterPopup(letter = selectedLetter!!)
        }
    }
}

/**
 * 字母放大提示弹窗
 */
@Composable
private fun BoxScope.LetterPopup(letter: String) {
    Popup(
        alignment = Alignment.Center,
        properties = PopupProperties(focusable = false)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF007AFF)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = letter,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * 应用列表项组件 (用于抽屉中的列表)
 */
@Composable
fun AppListItem(
    app: cn.whc.launcher.data.model.AppInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Int = 48,
    textSize: Int = 16
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        app.icon?.let { drawable ->
            DrawableImage(
                drawable = drawable,
                contentDescription = app.displayName,
                modifier = Modifier.size(iconSize.dp)
            )
        }

        Text(
            text = app.displayName,
            color = Color.White,
            fontSize = textSize.sp
        )
    }
}

/**
 * 字母分组头
 */
@Composable
fun LetterHeader(
    letter: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(Color.White.copy(alpha = 0.1f))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = letter,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
