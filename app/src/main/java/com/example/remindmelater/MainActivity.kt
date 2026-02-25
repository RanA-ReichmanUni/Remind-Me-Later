package com.example.remindmelater

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.remindmelater.receiver.NotificationActionReceiver
import com.example.remindmelater.scheduler.ReminderScheduler
import com.example.remindmelater.ui.components.ComfortHoursSheet
import com.example.remindmelater.ui.screens.DumpScreen
import com.example.remindmelater.ui.screens.RemindersScreen
import com.example.remindmelater.ui.theme.RemindMeLaterTheme
import com.example.remindmelater.ui.viewmodel.ReminderViewModel

private enum class Tab(val label: String, val icon: ImageVector) {
    DUMP("Dump", Icons.Outlined.Create),
    REMINDERS("Reminders", Icons.Default.Notifications)
}

class MainActivity : ComponentActivity() {

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied — we continue either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request POST_NOTIFICATIONS on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Reschedule intent from notification action
        val rescheduleId   = intent.getLongExtra(ReminderScheduler.EXTRA_REMINDER_ID, -1L)
        val isReschedule   = intent.action == NotificationActionReceiver.ACTION_RESCHEDULE

        setContent {
            RemindMeLaterTheme {
                val vm: ReminderViewModel = viewModel()
                val hasOnboarded by vm.hasOnboarded.collectAsState()
                val comfortStart by vm.comfortStart.collectAsState()
                val comfortEnd   by vm.comfortEnd.collectAsState()

                var selectedTab by remember { mutableStateOf(Tab.DUMP) }
                var prefillText by remember { mutableStateOf<String?>(null) }

                // When arriving from a reschedule action, switch to Dump tab
                LaunchedEffect(isReschedule, rescheduleId) {
                    if (isReschedule && rescheduleId != -1L) {
                        selectedTab = Tab.DUMP
                        // Fetch the reminder text so we can pre-fill it
                        // (the ViewModel already has the list; we find it there)
                    }
                }

                // Onboarding: if first launch, show comfort hours sheet before anything else
                if (!hasOnboarded) {
                    ComfortHoursSheet(
                        initialStart = comfortStart,
                        initialEnd   = comfortEnd,
                        onDismiss    = { /* intentionally no-op: user must save before proceeding */ },
                        onSave       = { s, e -> vm.saveComfortHours(s, e) }
                    )
                } else {
                Scaffold(
                    bottomBar = {
                        PremiumBottomBar(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it }
                        )
                    }
                ) { innerPadding ->
                    when (selectedTab) {
                        Tab.DUMP -> DumpScreen(
                            viewModel        = vm,
                            prefillText      = prefillText,
                            onPrefillConsumed = { prefillText = null },
                            modifier         = Modifier.padding(innerPadding)
                        )
                        Tab.REMINDERS -> RemindersScreen(
                            viewModel = vm,
                            modifier  = Modifier.padding(innerPadding)
                        )
                    }
                }
                } // end if/else hasOnboarded
            }
        }
    }
}

@Composable
private fun PremiumBottomBar(
    selectedTab: Tab,
    onTabSelected: (Tab) -> Unit
) {
    Surface(
        tonalElevation = 1.dp,
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(26.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Tab.entries.forEach { tab ->
                PremiumNavItem(
                    tab = tab,
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PremiumNavItem(
    tab: Tab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.01f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 550f),
        label = "navScale"
    )
    val iconTint by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "navIcon"
    )
    val labelTint by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "navLabel"
    )

    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                else MaterialTheme.colorScheme.surface
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.label,
                tint = iconTint,
                modifier = Modifier.size((20 * scale).dp)
            )
            Text(
                text = tab.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = labelTint
            )
            Text(
                text = if (selected) "•" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}