package com.example.remindmelater.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.remindmelater.data.model.Reminder
import com.example.remindmelater.data.model.Timeframe
import com.example.remindmelater.ui.components.ComfortHoursSheet
import com.example.remindmelater.ui.viewmodel.ReminderViewModel

@Composable
fun RemindersScreen(
    viewModel: ReminderViewModel,
    modifier: Modifier = Modifier
) {
    val reminders by viewModel.reminders.collectAsState()
    val comfortStart by viewModel.comfortStart.collectAsState()
    val comfortEnd by viewModel.comfortEnd.collectAsState()
    var showComfortSheet by rememberSaveable { mutableStateOf(false) }
    var displayCount by rememberSaveable { mutableIntStateOf(10) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            )
            .systemBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeader(
            count = reminders.size,
            comfortLabel = "Comfort ${formatHour(comfortStart)} - ${formatHour(comfortEnd)}",
            onComfortClick = { showComfortSheet = true }
        )
        Spacer(Modifier.height(12.dp))

        if (reminders.isEmpty()) {
            EmptyState(modifier = Modifier.fillMaxSize())
        } else {
            val visible = reminders.take(displayCount)
            val hasMore = reminders.size > displayCount
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(visible, key = { it.id }) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        onDone = { viewModel.markDone(reminder.id) },
                        onMove = { updated -> viewModel.updateTimeframe(reminder, updated) }
                    )
                }
                if (hasMore) {
                    item {
                        TextButton(
                            onClick = { displayCount += 10 },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Load more (${reminders.size - displayCount} remaining)")
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    if (showComfortSheet) {
        ComfortHoursSheet(
            initialStart = comfortStart,
            initialEnd = comfortEnd,
            onDismiss = { showComfortSheet = false },
            onSave = { start, end ->
                viewModel.saveComfortHours(start, end)
                showComfortSheet = false
            }
        )
    }
}

@Composable
private fun ScreenHeader(
    count: Int,
    comfortLabel: String,
    onComfortClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "REMIND ME LATER",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Chaos queue",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (count == 1) "I’m tracking 1 thing for you"
                else "I’m tracking $count things for you",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedCard(
                onClick = onComfortClick,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = comfortLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderCard(
    reminder: Reminder,
    onDone: () -> Unit,
    onMove: (Timeframe) -> Unit
) {
    var expanded by rememberSaveable(reminder.id) { mutableStateOf(false) }
    var showHandledConfirm by rememberSaveable(reminder.id) { mutableStateOf(false) }
    val (emoji, timeframeTone) = timeframeBadge(reminder.timeframe)

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.40f),
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.30f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Badge(emoji, timeframeTone)
                        Text(
                            text = reminder.timeframe.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = reminder.text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showHandledConfirm = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 2.dp
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.Done, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("Handled")
                }
                OutlinedButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.2.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text(if (expanded) "Close" else "Not now")
                }
            }

            if (showHandledConfirm) {
                AlertDialog(
                    onDismissRequest = { showHandledConfirm = false },
                    title = { Text("Mark as handled?") },
                    text = {
                        Text("This reminder will be removed from your active queue.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showHandledConfirm = false
                                onDone()
                            }
                        ) {
                            Text("Yes, handled")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showHandledConfirm = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Cool, kick it to:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val options = listOf(
                        Timeframe.LATER_TODAY,
                        Timeframe.NEXT_FEW_DAYS,
                        Timeframe.NEXT_WEEKS,
                        Timeframe.NEXT_MONTH
                    )
                    options.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { option ->
                                val selected = option == reminder.timeframe
                                TextButton(
                                    onClick = {
                                        expanded = false
                                        if (!selected) onMove(option)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(
                                            width = 1.dp,
                                            color = if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(option.label)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Badge(emoji: String, tone: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(tone.copy(alpha = 0.18f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji)
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🧭", style = MaterialTheme.typography.headlineLarge)
                Text(
                    text = "Zero chaos in the queue",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Brain-dump on the other tab. I’ll take it from there.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun timeframeBadge(timeframe: Timeframe): Pair<String, androidx.compose.ui.graphics.Color> {
    return when (timeframe) {
        Timeframe.LATER_TODAY -> "⚡" to MaterialTheme.colorScheme.secondary
        Timeframe.NEXT_FEW_DAYS -> "🌤" to MaterialTheme.colorScheme.primary
        Timeframe.NEXT_WEEKS -> "🌙" to MaterialTheme.colorScheme.tertiary
        Timeframe.NEXT_MONTH -> "🌊" to MaterialTheme.colorScheme.onSurfaceVariant
    }
}

private fun formatHour(hour: Int): String {
    val suffix = if (hour < 12) "AM" else "PM"
    val h = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "$h:00 $suffix"
}
