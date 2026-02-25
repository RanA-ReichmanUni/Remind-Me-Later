package com.example.remindmelater.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.remindmelater.data.model.Timeframe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeframeSheet(
    selected: Timeframe,
    onSelected: (Timeframe) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
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
                    onSelected(timeframe)
                    onDismiss()
                }
            )
        }
    }
}
