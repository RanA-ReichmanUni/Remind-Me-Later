package com.example.remindmelater.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.remindmelater.data.model.Timeframe
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeframeSheet(
    selected: Timeframe,
    comfortStart: Int,
    comfortEnd: Int,
    onSelected: (Timeframe, ignoreComfortHours: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    // Set to true when Later Today is picked near/outside comfort hours
    var showComfortWarning by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = {
        showComfortWarning = false
        onDismiss()
    }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Timing vibe",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "When should future-you deal with this?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TimeframeSelector(
                selected = selected,
                onSelected = { timeframe ->
                    if (timeframe == Timeframe.LATER_TODAY &&
                        isOutsideOrNearComfortEnd(comfortStart, comfortEnd)
                    ) {
                        showComfortWarning = true
                    } else {
                        onSelected(timeframe, false)
                        onDismiss()
                    }
                }
            )
        }
    }

    if (showComfortWarning) {
        AlertDialog(
            onDismissRequest = { showComfortWarning = false },
            title = { Text("Outside your comfort hours") },
            text = { Text(buildWarningMessage(comfortStart, comfortEnd)) },
            confirmButton = {
                Button(onClick = {
                    showComfortWarning = false
                    onSelected(Timeframe.LATER_TODAY, true)
                    onDismiss()
                }) {
                    Text("Alert me outside comfort hours")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showComfortWarning = false
                    onSelected(Timeframe.LATER_TODAY, false)
                    onDismiss()
                }) {
                    Text("Schedule for tomorrow")
                }
            }
        )
    }
}

/** Returns true when outside comfort hours entirely, or < 30 min before comfort end. */
private fun isOutsideOrNearComfortEnd(comfortStart: Int, comfortEnd: Int): Boolean {
    val cal    = Calendar.getInstance()
    val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    return if (comfortEnd > comfortStart) {
        // Normal daytime window
        nowMin < comfortStart * 60 || nowMin >= comfortEnd * 60 || (comfortEnd * 60 - nowMin) < 30
    } else {
        // Night-shift: window = [comfortStart, 24) ∪ [0, comfortEnd)
        val inWindow = nowMin >= comfortStart * 60 || nowMin < comfortEnd * 60
        if (!inWindow) return true
        val minsToEnd = if (nowMin >= comfortStart * 60)
            (24 * 60 - nowMin) + comfortEnd * 60   // night portion; end is past midnight
        else
            comfortEnd * 60 - nowMin               // morning portion
        minsToEnd < 30
    }
}

private fun buildWarningMessage(comfortStart: Int, comfortEnd: Int): String {
    val cal        = Calendar.getInstance()
    val nowMin     = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    val nightShift = comfortEnd <= comfortStart

    // Night-shift: inside window but near end
    if (nightShift && (nowMin >= comfortStart * 60 || nowMin < comfortEnd * 60)) {
        val remaining = if (nowMin >= comfortStart * 60)
            (24 * 60 - nowMin) + comfortEnd * 60
        else
            comfortEnd * 60 - nowMin
        return "Only $remaining minute${if (remaining == 1) "" else "s"} left in your overnight comfort window. " +
                "Send it before your window closes, or push it to tonight?"
    }

    return when {
        nowMin < comfortStart * 60 ->
            "Your comfort hours haven't kicked in yet — they start at ${comfortStart.to12h()}. " +
            "Want this alert to fire outside your quiet window anyway?"
        nowMin >= comfortEnd * 60 ->
            "You're currently outside your comfort hours — they ended at ${comfortEnd.to12h()} today. " +
            "Send the alert today anyway, or reschedule it for tomorrow morning?"
        else -> {
            val remaining = comfortEnd * 60 - nowMin
            "Only $remaining minute${if (remaining == 1) "" else "s"} left in your comfort window. " +
            "The reminder might arrive right as your quiet time starts. " +
            "Send it today anyway, or push it to tomorrow morning?"
        }
    }
}

private fun Int.to12h(): String {
    val suffix = if (this < 12) "AM" else "PM"
    val h = when { this == 0 -> 12; this > 12 -> this - 12; else -> this }
    return "$h:00 $suffix"
}
