package com.impactdevelopment.remindmelater.update

import android.app.Activity
import androidx.activity.ComponentActivity
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.impactdevelopment.remindmelater.BuildConfig

class UpdateManager(private val activity: ComponentActivity) {

    private val appUpdateManager = AppUpdateManagerFactory.create(activity)
    private val remoteConfig = Firebase.remoteConfig

    fun checkAndEnforceUpdate(
        onUpToDate: () -> Unit,
        onHardLock: () -> Unit
    ) {
        val settings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0 else 3600
        }
        remoteConfig.setConfigSettingsAsync(settings)
        remoteConfig.setDefaultsAsync(
            mapOf(MIN_VERSION_KEY to BuildConfig.VERSION_CODE.toLong())
        )

        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    onUpToDate()
                    return@addOnCompleteListener
                }

                val minRequired = remoteConfig.getLong(MIN_VERSION_KEY)
                val current = BuildConfig.VERSION_CODE.toLong()
                if (current < minRequired) {
                    appUpdateManager.appUpdateInfo
                        .addOnSuccessListener { info ->
                            val availability = info.updateAvailability()
                            val canImmediate =
                                availability == UpdateAvailability.UPDATE_AVAILABLE ||
                                    availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS

                            if (canImmediate && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                                val options = AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE)
                                    .build()
                                try {
                                    appUpdateManager.startUpdateFlowForResult(
                                        info,
                                        activity,
                                        options,
                                        UPDATE_REQUEST_CODE
                                    )
                                } catch (ex: Exception) {
                                    onHardLock()
                                }
                            } else {
                                onHardLock()
                            }
                        }
                        .addOnFailureListener { onHardLock() }
                } else {
                    onUpToDate()
                }
            }
    }

    fun handleActivityResult(
        requestCode: Int,
        resultCode: Int,
        onHardLock: () -> Unit
    ) {
        if (requestCode != UPDATE_REQUEST_CODE) return
        if (resultCode != Activity.RESULT_OK) {
            onHardLock()
        }
    }

    companion object {
        const val UPDATE_REQUEST_CODE = 9001
        const val MIN_VERSION_KEY = "min_version_required"
    }
}
