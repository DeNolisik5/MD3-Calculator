package com.narezany.calculator.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.narezany.calculator.calc.CalculatorUiState
import com.narezany.calculator.calc.CalculatorViewModel
import com.narezany.calculator.ui.theme.CalculatorTheme
import com.narezany.calculator.ui.theme.ExpressiveMotion

/** Every user intent the keypad can produce. */
sealed interface CalcAction {
    data class Digit(val value: Char) : CalcAction
    data class Operator(val symbol: String) : CalcAction
    data object Decimal : CalcAction
    data object Percent : CalcAction
    data object ToggleSign : CalcAction
    data object Backspace : CalcAction
    data object Clear : CalcAction
    data object Equals : CalcAction
}

@Composable
fun CalculatorRoute(viewModel: CalculatorViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CalculatorScreen(
        state = state,
        onAction = { action ->
            when (action) {
                is CalcAction.Digit -> viewModel.onDigit(action.value)
                is CalcAction.Operator -> viewModel.onOperator(action.symbol)
                CalcAction.Decimal -> viewModel.onDecimal()
                CalcAction.Percent -> viewModel.onPercent()
                CalcAction.ToggleSign -> viewModel.onToggleSign()
                CalcAction.Backspace -> viewModel.onBackspace()
                CalcAction.Clear -> viewModel.onClear()
                CalcAction.Equals -> viewModel.onEquals()
            }
        },
    )
}

@Composable
fun CalculatorScreen(
    state: CalculatorUiState,
    onAction: (CalcAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberCalculatorHaptics()
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to colors.surfaceContainerLowest,
                    0.45f to colors.surface,
                    1f to colors.surface,
                )
            )
    ) {
        // Expressive ambient light: two soft colour clouds tinted by the
        // dynamic (wallpaper based) palette.
        Box(
            Modifier
                .offset(x = (-120).dp, y = (-120).dp)
                .size(340.dp)
                .background(
                    Brush.radialGradient(
                        listOf(colors.primary.copy(alpha = 0.30f), Color.Transparent)
                    ),
                    CircleShape,
                )
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 110.dp, y = 90.dp)
                .size(300.dp)
                .background(
                    Brush.radialGradient(
                        listOf(colors.tertiary.copy(alpha = 0.22f), Color.Transparent)
                    ),
                    CircleShape,
                )
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp)
        ) {
            val landscape = maxWidth > maxHeight

            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(Modifier.height(if (landscape) 6.dp else 10.dp))

                DisplayPanel(
                    state = state,
                    compact = landscape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )

                Spacer(Modifier.height(if (landscape) 8.dp else 16.dp))

                Keypad(
                    onAction = onAction,
                    haptics = haptics,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(if (landscape) 2.4f else 1.45f),
                )

                Spacer(Modifier.height(if (landscape) 4.dp else 10.dp))
            }
        }
    }
}

@Composable
private fun DisplayPanel(
    state: CalculatorUiState,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(34.dp)
    val scrollState = rememberScrollState()

    LaunchedEffect(state.expression) {
        // Wait for layout, then keep the caret end of a long expression visible.
        withFrameNanos { }
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.surfaceContainerHigh.copy(alpha = 0.72f))
            .border(1.dp, colors.outlineVariant.copy(alpha = 0.45f), shape)
            .padding(horizontal = if (compact) 16.dp else 22.dp, vertical = if (compact) 10.dp else 18.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End,
    ) {
        AnimatedVisibility(
            visible = state.history.isNotEmpty() && !compact,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Text(
                text = state.history,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium),
                color = colors.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(4.dp))

        val display = state.expression.ifBlank { "0" }
        val baseSize = when {
            display.length <= 9 -> 54f
            display.length <= 12 -> 46f
            display.length <= 16 -> 38f
            display.length <= 22 -> 30f
            else -> 24f
        }
        val targetSize = if (compact) baseSize * 0.7f else baseSize
        val fontSize by animateFloatAsState(
            targetValue = targetSize,
            animationSpec = ExpressiveMotion.spatial<Float>(),
            label = "displayFontSize",
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = display,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = fontSize.sp),
                color = if (state.expression.isBlank()) {
                    colors.onSurfaceVariant.copy(alpha = 0.45f)
                } else {
                    colors.onSurface
                },
                maxLines = 1,
                softWrap = false,
            )
        }

        AnimatedVisibility(
            visible = state.previewVisible,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Text(
                text = "= ${state.preview}",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        AnimatedVisibility(
            visible = state.error != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Text(
                text = state.error.orEmpty(),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp),
                color = colors.error,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private enum class KeyKind { Digit, Operator, Function, Equals }

private data class KeySpec(
    val label: String,
    val description: String,
    val kind: KeyKind,
    val action: CalcAction,
)

private val PressedCornerRadius = 20.dp

private val KeypadRows: List<List<KeySpec>> = listOf(
    listOf(
        KeySpec("AC", "Сброс", KeyKind.Function, CalcAction.Clear),
        KeySpec("⌫", "Удалить последний символ", KeyKind.Function, CalcAction.Backspace),
        KeySpec("%", "Процент", KeyKind.Function, CalcAction.Percent),
        KeySpec("÷", "Деление", KeyKind.Operator, CalcAction.Operator("÷")),
    ),
    listOf(
        KeySpec("7", "Семь", KeyKind.Digit, CalcAction.Digit('7')),
        KeySpec("8", "Восемь", KeyKind.Digit, CalcAction.Digit('8')),
        KeySpec("9", "Девять", KeyKind.Digit, CalcAction.Digit('9')),
        KeySpec("×", "Умножение", KeyKind.Operator, CalcAction.Operator("×")),
    ),
    listOf(
        KeySpec("4", "Четыре", KeyKind.Digit, CalcAction.Digit('4')),
        KeySpec("5", "Пять", KeyKind.Digit, CalcAction.Digit('5')),
        KeySpec("6", "Шесть", KeyKind.Digit, CalcAction.Digit('6')),
        KeySpec("−", "Вычитание", KeyKind.Operator, CalcAction.Operator("-")),
    ),
    listOf(
        KeySpec("1", "Один", KeyKind.Digit, CalcAction.Digit('1')),
        KeySpec("2", "Два", KeyKind.Digit, CalcAction.Digit('2')),
        KeySpec("3", "Три", KeyKind.Digit, CalcAction.Digit('3')),
        KeySpec("+", "Сложение", KeyKind.Operator, CalcAction.Operator("+")),
    ),
    listOf(
        KeySpec("±", "Сменить знак", KeyKind.Function, CalcAction.ToggleSign),
        KeySpec("0", "Ноль", KeyKind.Digit, CalcAction.Digit('0')),
        KeySpec(".", "Десятичная точка", KeyKind.Digit, CalcAction.Decimal),
        KeySpec("=", "Равно", KeyKind.Equals, CalcAction.Equals),
    ),
)

@Composable
private fun Keypad(
    onAction: (CalcAction) -> Unit,
    haptics: CalculatorHaptics,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        KeypadRows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { key ->
                    CalcKey(
                        key = key,
                        haptics = haptics,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = { onAction(key.action) },
                    )
                }
            }
        }
    }
}

/**
 * M3 Expressive key.
 *
 * Geometry (corner radius, scale, elevation) is driven by the M3E spatial
 * spring — `ExpressiveMotion.spatial()`, i.e. the expressive default
 * `spring(dampingRatio = 0.8f, stiffness = 380f)` — so the key overshoots
 * slightly and settles instead of easing linearly. Colour uses the matching
 * effects spring (1.0 / 1600).
 *
 * At rest the key is a full pill (`percent = 50`). While pressed it morphs to
 * the M3 "large increased" corner (20dp) and back. The animated shape is
 * applied with `clip`, so background, border and ripple all follow it.
 */
@Composable
private fun CalcKey(
    key: KeySpec,
    haptics: CalculatorHaptics,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    val containerColor = when (key.kind) {
        KeyKind.Digit -> colors.surfaceContainerHigh
        KeyKind.Function -> colors.secondaryContainer
        KeyKind.Operator -> colors.primaryContainer
        KeyKind.Equals -> colors.primary
    }
    val contentColor = when (key.kind) {
        KeyKind.Digit -> colors.onSurface
        KeyKind.Function -> colors.onSecondaryContainer
        KeyKind.Operator -> colors.onPrimaryContainer
        KeyKind.Equals -> colors.onPrimary
    }

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    // Expressive motion tokens: spatial springs move geometry, effects springs
    // change colour. Values are the M3E defaults (0.8/380 and 1.0/1600).
    val spatialDp = ExpressiveMotion.spatial<Dp>()
    val spatialFloat = ExpressiveMotion.spatial<Float>()
    val effectsColor = ExpressiveMotion.effects<Color>()

    BoxWithConstraints(modifier = modifier) {
        // Rest: a full pill, exactly like an M3 Expressive button.
        val pillRadius = minOf(maxWidth, maxHeight) / 2
        // Pressed: morph into the M3 "large increased" corner token.
        val cornerRadius by animateDpAsState(
            targetValue = if (pressed) PressedCornerRadius else pillRadius,
            animationSpec = spatialDp,
            label = "keyCornerRadius",
        )
        val shape = RoundedCornerShape(cornerRadius)

        val scale by animateFloatAsState(
            targetValue = if (pressed) 0.92f else 1f,
            animationSpec = spatialFloat,
            label = "keyScale",
        )
        val elevation by animateDpAsState(
            targetValue = if (pressed) 0.dp else 3.dp,
            animationSpec = spatialDp,
            label = "keyElevation",
        )
        val background by animateColorAsState(
            targetValue = if (pressed) lerp(containerColor, contentColor, 0.16f) else containerColor,
            animationSpec = effectsColor,
            label = "keyColor",
        )
        val borderColor = if (key.kind == KeyKind.Equals) {
            colors.onPrimary.copy(alpha = 0.22f)
        } else {
            colors.outlineVariant.copy(alpha = 0.28f)
        }

        // Keys grow and shrink with the available space, so the keypad stays
        // readable in landscape as well. Captured here because BoxWithConstraints
        // scope is not accessible from the nested Box below.
        val labelSize = (maxHeight.value * 0.42f).coerceIn(15f, 33f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .shadow(elevation = elevation, shape = shape, clip = false)
                .clip(shape)
                .background(background)
                .border(1.dp, borderColor, shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = contentColor),
                    onClickLabel = key.description,
                    onClick = {
                        haptics.tick(strong = key.kind == KeyKind.Equals)
                        onClick()
                    },
                )
                .semantics { contentDescription = key.description },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = key.label,
                color = contentColor,
                fontSize = labelSize.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 860)
@Composable
private fun CalculatorPreview() {
    CalculatorTheme(darkTheme = true) {
        CalculatorScreen(
            state = CalculatorUiState(
                expression = "128×4+15%",
                preview = "588.8",
                history = "32÷8 =",
            ),
            onAction = {},
        )
    }
}
