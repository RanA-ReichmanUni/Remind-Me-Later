package com.example.remindmelater.ui.screens

import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.remindmelater.MainActivity
import com.example.remindmelater.receiver.NotificationActionReceiver
import com.example.remindmelater.scheduler.ReminderScheduler
import com.example.remindmelater.ui.theme.RemindMeLaterTheme

class AlarmActivity : ComponentActivity() {

    companion object {
        const val EXTRA_REMINDER_TEXT = "reminder_text"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val reminderId   = intent.getLongExtra(ReminderScheduler.EXTRA_REMINDER_ID, -1L)
        val reminderText = intent.getStringExtra(EXTRA_REMINDER_TEXT) ?: "Your reminder"

        // Dismiss the notification immediately
        val nm = getSystemService(NotificationManager::class.java)
        nm.cancel(reminderId.toInt())

        setContent {
            RemindMeLaterTheme {
                AlarmScreen(
                    reminderText        = reminderText,
                    onDone              = {
                        sendAction(NotificationActionReceiver.ACTION_DONE, reminderId)
                        finish()
                    },
                    onSnoozeLaterToday  = {
                        sendAction(NotificationActionReceiver.ACTION_SNOOZE_LATER_TODAY, reminderId)
                        finish()
                    },
                    onSnoozeTomorrow    = {
                        sendAction(NotificationActionReceiver.ACTION_SNOOZE_TOMORROW, reminderId)
                        finish()
                    },
                    onSnoozeReRandomize = {
                        sendAction(NotificationActionReceiver.ACTION_SNOOZE_RERANDOMIZE, reminderId)
                        finish()
                    },
                    onReschedule = {
                        val mainIntent = Intent(this, MainActivity::class.java).apply {
                            flags  = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            action = NotificationActionReceiver.ACTION_RESCHEDULE
                            putExtra(ReminderScheduler.EXTRA_REMINDER_ID, reminderId)
                        }
                        startActivity(mainIntent)
                        finish()
                    }
                )
            }
        }
    }

    private fun sendAction(action: String, reminderId: Long) {
        val intent = Intent(this, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(ReminderScheduler.EXTRA_REMINDER_ID, reminderId)
        }
        sendBroadcast(intent)
    }
}

@Composable
private fun AlarmScreen(
    reminderText: String,
    onDone: () -> Unit,
    onSnoozeLaterToday: () -> Unit,
    onSnoozeTomorrow: () -> Unit,
    onSnoozeReRandomize: () -> Unit,
    onReschedule: () -> Unit
) {
    var showSnoozeOptions by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        // Bell icon + Reminder text — centred on screen
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text  = "🔔",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text       = reminderText,
                style      = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 44.sp
                ),
                textAlign  = TextAlign.Center,
                color      = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text  = "You wanted to be reminded about this.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        // Action area — pinned to bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp)
                .padding(bottom = 44.dp),
            verticalArrangement   = Arrangement.spacedBy(12.dp),
            horizontalAlignment   = Alignment.CenterHorizontally
        ) {
            // Expandable snooze options panel
            AnimatedVisibility(
                visible = showSnoozeOptions,
                enter   = fadeIn(tween(250)) + expandVertically(tween(300)),
                exit    = fadeOut(tween(200)) + shrinkVertically(tween(250))
            ) {
                Surface(
                    shape   = RoundedCornerShape(20.dp),
                    color   = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Snooze until…",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                        SnoozeRow("⚡ Later today",  onSnoozeLaterToday)
                        SnoozeRow("🌤 Tomorrow",     onSnoozeTomorrow)
                        SnoozeRow("🔀 Re-randomize", onSnoozeReRandomize)
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }

            // Primary action row: Done  |  Snooze
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick  = onDone,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape    = RoundedCornerShape(16.dp)
                ) {
                    Text("✓  Done", fontWeight = FontWeight.Bold,
                         style = MaterialTheme.typography.titleSmall)
                }
                OutlinedButton(
                    onClick  = { showSnoozeOptions = !showSnoozeOptions },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape    = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        if (showSnoozeOptions) "✕  Cancel" else "💤  Snooze",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }

            TextButton(onClick = onReschedule) {
                Text(
                    "↺  Reschedule with a new timeframe",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SnoozeRow(label: String, onClick: () -> Unit) {
    TextButton(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp)
    ) {
        Text(
            label,
            style      = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color      = MaterialTheme.colorScheme.primary
        )
    }
}
