package com.example.remindmelater.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComfortHoursSheet(
    initialStart: Int,
    initialEnd: Int,
    onDismiss: () -> Unit,
    onSave: (start: Int, end: Int) -> Unit
) {
    var start by remember(initialStart) { mutableIntStateOf(initialStart) }
    var end   by remember(initialEnd)   { mutableIntStateOf(initialEnd) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                "Comfort hours",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "We'll only send reminders during these hours — so you're never bothered outside your day.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HourStepper(
                label = "From",
                hour = start,
                onDecrease = { if (start > 0) start-- },
                onIncrease = { if (start < 23) start++ }
            )

            HourStepper(
                label = "Until",
                hour = end,
                onDecrease = { if (end > 0) end-- },
                onIncrease = { if (end < 23) end++ }
            )

            if (end <= start) {
                Text(
                    text = "Spans overnight — comfort window crosses midnight (night shift).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Button(
                onClick = { onSave(start, end) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = end != start
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun HourStepper(
    label: String,
    hour: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrease) {
                Text("−", style = MaterialTheme.typography.headlineSmall)
            }
            Text(
                formatHour(hour),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.widthIn(min = 64.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            IconButton(onClick = onIncrease) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}

private fun formatHour(hour: Int): String {
    val suffix = if (hour < 12) "AM" else "PM"
    val h = when {
        hour == 0   -> 12
        hour > 12   -> hour - 12
        else        -> hour
    }
    return "$h:00 $suffix"
}
