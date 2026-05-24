package com.impactdevelopment.remindmelater

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.impactdevelopment.remindmelater.ui.theme.RemindMeLaterTheme

class UpdateBlockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RemindMeLaterTheme {
                UpdateBlockScreen(onUpdateNow = { openStore() })
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Intentionally disabled: hard lock screen
    }

    private fun openStore() {
        val packageName = applicationContext.packageName
        val marketUri = Uri.parse("market://details?id=$packageName")
        val webUri = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")

        val marketIntent = Intent(Intent.ACTION_VIEW, marketUri)
        try {
            startActivity(marketIntent)
        } catch (ex: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, webUri))
        }
    }
}

@Composable
private fun UpdateBlockScreen(onUpdateNow: () -> Unit) {
    BackHandler(onBack = { /* intentionally no-op */ })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Update required",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Please update to continue using the app.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )
        Button(onClick = onUpdateNow) {
            Text(text = "Update Now")
        }
    }
}
