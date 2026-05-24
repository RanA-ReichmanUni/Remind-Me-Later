package com.impactdevelopment.remindmelater

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.impactdevelopment.remindmelater.ui.theme.RemindMeLaterTheme
import com.impactdevelopment.remindmelater.update.UpdateManager

class SplashActivity : ComponentActivity() {

    private lateinit var updateManager: UpdateManager
    private var navigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RemindMeLaterTheme {
                SplashContent()
            }
        }

        updateManager = UpdateManager(this)
        updateManager.checkAndEnforceUpdate(
            onUpToDate = { openMain() },
            onHardLock = { openUpdateBlock() }
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        updateManager.handleActivityResult(
            requestCode = requestCode,
            resultCode = resultCode,
            onHardLock = { openUpdateBlock() }
        )
    }

    private fun openMain() {
        if (navigated) return
        navigated = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun openUpdateBlock() {
        if (navigated) return
        navigated = true
        startActivity(Intent(this, UpdateBlockActivity::class.java))
        finish()
    }
}

@Composable
private fun SplashContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Checking for updates...",
            style = MaterialTheme.typography.titleMedium
        )
    }
}
