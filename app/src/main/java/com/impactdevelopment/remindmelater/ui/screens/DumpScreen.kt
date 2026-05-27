package com.impactdevelopment.remindmelater.ui.screens
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.impactdevelopment.remindmelater.data.model.Timeframe
import com.impactdevelopment.remindmelater.ui.components.TimeframeSheet
import com.impactdevelopment.remindmelater.ui.viewmodel.ReminderViewModel
import kotlinx.coroutines.delay

@Composable
fun DumpScreen(
    viewModel: ReminderViewModel,
    prefillText: String?,
    onPrefillConsumed: () -> Unit,
    backgroundAnimationEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    var selectedTimeframe by remember { mutableStateOf(Timeframe.NEXT_FEW_DAYS) }
    var ignoreComfortHours by remember { mutableStateOf(false) }
    var showTimeframeSheet by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(false) }
    val comfortStart by viewModel.comfortStart.collectAsState()
    val comfortEnd by viewModel.comfortEnd.collectAsState()

    LaunchedEffect(prefillText) {
        if (!prefillText.isNullOrBlank()) {
            text = prefillText
            onPrefillConsumed()
        }
    }

    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(1800L)
            showSuccess = false
        }
    }

    LaunchedEffect(Unit) {
        contentVisible = true
    }

    val canSave = text.isNotBlank()
    val pulseTransition = rememberInfiniteTransition(label = "ctaPulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ctaPulse"
    )
    val buttonScale = if (canSave) 1f + (0.02f * pulse) else 1f
    val glowAlpha = if (canSave) 0.18f + (0.12f * pulse) else 0f
    val primaryColor = MaterialTheme.colorScheme.primary

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
    ) {
        if (backgroundAnimationEnabled) {
            AnimatedBackdrop(modifier = Modifier.fillMaxSize())
        } else {
            StaticBackdrop(modifier = Modifier.fillMaxSize())
        }

        // squeeze: 1.0 on generous screens (≥720dp), 0.0 on tight screens (≤520dp).
        // Only paddings and the text-field shrink — preserves proportions when room exists.
        val squeeze = ((maxHeight.value - 520f) / 200f).coerceIn(0f, 1f)

        val titleVertPad    = (14 + 8 * squeeze).dp   // 14..22
        val subtitleGap     = (6 + 10 * squeeze).dp    // 6..16
        val preDividerGap   = (8 + 12 * squeeze).dp    // 8..20
        val inputVertPad    = (12 + 8 * squeeze).dp     // 12..20
        val interCardGap    = (8 + 8 * squeeze).dp      // 8..16
        val buttonHeight    = (48 + 10 * squeeze).dp    // 48..58
        val textFieldHeight = (56 + 64 * squeeze).dp    // 56..120

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Spacer(Modifier.weight(3f))

            // ── Hero title tile + input, visually fused ──────────────────
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(durationMillis = 650, delayMillis = 60)) +
                    slideInVertically(animationSpec = tween(600), initialOffsetY = { it / 3 }) +
                    scaleIn(initialScale = 0.97f, animationSpec = tween(600)),
                exit = fadeOut(tween(durationMillis = 150))
            ) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column {
                        // Title section
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = titleVertPad),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Dump & Forget:",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Remind Me Later",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(subtitleGap))
                            Text(
                                text = "We'll ping you later, when it's more convenient.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(Modifier.height(preDividerGap))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            thickness = 1.dp
                        )
                        Spacer(Modifier.height(8.dp))

                        // Input section
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = inputVertPad),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Create,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "Dump your chaos here",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            ChaosPad(
                                value = text,
                                onValueChange = { text = it },
                                modifier = Modifier.fillMaxWidth().height(textFieldHeight),
                                placeholder = "What's on your mind?",
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (canSave) {
                                            viewModel.addReminder(text, selectedTimeframe, ignoreComfortHours)
                                            text = ""
                                            ignoreComfortHours = false
                                        }
                                    }
                                )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f).heightIn(min = interCardGap))

            // Timing vibe (tap to open sheet)
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(durationMillis = 550, delayMillis = 140)) +
                    slideInVertically(animationSpec = tween(550), initialOffsetY = { it / 4 }),
                exit = fadeOut(tween(durationMillis = 150))
            ) {
                Card(
                    onClick = { showTimeframeSheet = true },
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Timing vibe",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = selectedTimeframe.label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(2f).heightIn(min = interCardGap))
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(durationMillis = 550, delayMillis = 200)) +
                    slideInVertically(animationSpec = tween(550), initialOffsetY = { it / 4 }) +
                    scaleIn(initialScale = 0.98f, animationSpec = tween(550)),
                exit = fadeOut(tween(durationMillis = 150))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(buttonHeight)
                        .drawBehind {
                            if (glowAlpha > 0f) {
                                drawRoundRect(
                                    color = primaryColor.copy(alpha = glowAlpha),
                                    topLeft = Offset(-size.width * 0.05f, -size.height * 0.35f),
                                    size = Size(size.width * 1.1f, size.height * 1.7f),
                                    cornerRadius = CornerRadius(size.height * 0.6f, size.height * 0.6f)
                                )
                            }
                        }
                        .graphicsLayer(scaleX = buttonScale, scaleY = buttonScale)
                ) {
                    Button(
                        onClick = {
                            viewModel.addReminder(text, selectedTimeframe, ignoreComfortHours)
                            text = ""
                            ignoreComfortHours = false
                            showSuccess = true
                        },
                        enabled = canSave,
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(20.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 3.dp,
                            disabledElevation = 0.dp
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "Remind Me Later",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (canSave) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.Check, contentDescription = null)
                    }
                }
            }

            AnimatedVisibility(
                visible = contentVisible && text.isNotEmpty(),
                enter = fadeIn(tween(durationMillis = 300, delayMillis = 40)) +
                    slideInVertically(animationSpec = tween(300), initialOffsetY = { it / 3 }),
                exit = fadeOut(tween(durationMillis = 200)) +
                    slideOutVertically(animationSpec = tween(200), targetOffsetY = { it / 3 })
            ) {
                Column(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { text = "" }) {
                        Text("Clear draft")
                    }
                }
            }

            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(durationMillis = 400, delayMillis = 260)) +
                    slideInVertically(animationSpec = tween(400), initialOffsetY = { it / 3 }),
                exit = fadeOut(tween(durationMillis = 200))
            ) {
                Column(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Spacer(Modifier.height(6.dp))
                    // TODO: Test Button, REMOVE BEFORE RELEASE
                    /*
                    OutlinedButton(
                        onClick = { viewModel.scheduleInOneMinute(text.ifBlank { "Test reminder" }) },
                        border = BorderStroke(1.dp, Color(0xFFFF6B00).copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFF6B00)
                        )
                    ) {
                        Text("⚡ Test: fire in 1 min", style = MaterialTheme.typography.labelMedium)
                    }
                    */
                }
            }

            Spacer(Modifier.weight(2f).heightIn(min = interCardGap))
        }
    }

    // ── Full-screen success popup overlay ─────────────────────────────────
    AnimatedVisibility(
        visible = showSuccess,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(tween(150)) + scaleIn(
            initialScale = 0.7f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness    = Spring.StiffnessMedium
            )
        ),
        exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.92f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.50f)),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.padding(horizontal = 36.dp)) {
                SuccessBadge()
            }
        }
    }

    if (showTimeframeSheet) {
        TimeframeSheet(
            selected = selectedTimeframe,
            comfortStart = comfortStart,
            comfortEnd = comfortEnd,
            onSelected = { timeframe, ignore ->
                selectedTimeframe = timeframe
                ignoreComfortHours = ignore
            },
            onDismiss = { showTimeframeSheet = false }
        )
    }
}

@Composable
private fun SuccessBadge() {
    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        border = BorderStroke(
            width = 1.5.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
            Text(
                text = "Got it!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = "We'll remind you later 🤙",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ChaosPad(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions
) {
    val focusStrength by animateFloatAsState(
        targetValue = if (value.isNotBlank()) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "padFocus"
    )
    val glowColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f + (0.25f * focusStrength)),
        animationSpec = tween(durationMillis = 300),
        label = "padGlow"
    )
    val topGradient = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.36f + (0.1f * focusStrength))

    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        topGradient,
                        MaterialTheme.colorScheme.surface
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(2.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, glowColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                if (value.isBlank()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxSize(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    maxLines = 5
                )
            }
        }
    }
}

@Composable
private fun AnimatedBackdrop(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val drift = rememberInfiniteTransition(label = "backdrop")
    val driftX by drift.animateFloat(
        initialValue = -90f,
        targetValue = 90f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "driftX"
    )
    val driftY by drift.animateFloat(
        initialValue = 70f,
        targetValue = -70f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "driftY"
    )
    val glow by drift.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Canvas(modifier = modifier) {
        val baseGradient = Brush.linearGradient(
            colors = listOf(
                colors.surface,
                colors.primaryContainer.copy(alpha = 0.32f),
                colors.secondaryContainer.copy(alpha = 0.24f)
            ),
            start = Offset(0f + driftX, size.height * 0.1f + driftY),
            end = Offset(size.width - driftX, size.height * 0.9f - driftY)
        )
        drawRect(brush = baseGradient)

        drawCircle(
            color = colors.primary.copy(alpha = 0.14f + glow),
            center = Offset(size.width * 0.2f + driftX, size.height * 0.2f + driftY),
            radius = size.minDimension * 0.45f
        )
        drawCircle(
            color = colors.tertiary.copy(alpha = 0.12f + (glow * 0.6f)),
            center = Offset(size.width * 0.9f - driftX, size.height * 0.15f - driftY),
            radius = size.minDimension * 0.28f
        )
        drawCircle(
            color = colors.secondary.copy(alpha = 0.1f + (glow * 0.5f)),
            center = Offset(size.width * 0.85f + driftY, size.height * 0.85f - driftX),
            radius = size.minDimension * 0.55f
        )
    }
}

@Composable
private fun StaticBackdrop(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val driftX = 28f
    val driftY = -18f
    val glow = 0.08f

    Canvas(modifier = modifier) {
        val baseGradient = Brush.linearGradient(
            colors = listOf(
                colors.surface,
                colors.primaryContainer.copy(alpha = 0.28f),
                colors.secondaryContainer.copy(alpha = 0.22f)
            ),
            start = Offset(0f + driftX, size.height * 0.1f + driftY),
            end = Offset(size.width - driftX, size.height * 0.9f - driftY)
        )
        drawRect(brush = baseGradient)

        drawCircle(
            color = colors.primary.copy(alpha = 0.14f + glow),
            center = Offset(size.width * 0.2f + driftX, size.height * 0.2f + driftY),
            radius = size.minDimension * 0.45f
        )
        drawCircle(
            color = colors.tertiary.copy(alpha = 0.12f + (glow * 0.6f)),
            center = Offset(size.width * 0.9f - driftX, size.height * 0.15f - driftY),
            radius = size.minDimension * 0.28f
        )
        drawCircle(
            color = colors.secondary.copy(alpha = 0.1f + (glow * 0.5f)),
            center = Offset(size.width * 0.85f + driftY, size.height * 0.85f - driftX),
            radius = size.minDimension * 0.55f
        )
    }
}
