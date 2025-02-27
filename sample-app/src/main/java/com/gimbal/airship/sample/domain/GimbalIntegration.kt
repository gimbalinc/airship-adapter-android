package com.gimbal.airship.sample.domain

import android.Manifest
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ProcessLifecycleOwner
import com.gimbal.airship.AirshipAdapter
import com.gimbal.android.Gimbal
import com.gimbal.android.GimbalDebugger
import com.google.firebase.FirebaseApp
import com.urbanairship.UAirship
import timber.log.Timber
import javax.inject.Inject

class GimbalIntegration @Inject constructor(
    private val airshipAdapter: AirshipAdapter
) {
    private companion object {
        const val GIMBAL_API_KEY = "[YOUR GIMBAL API KEY]"
    }

    val adapterEnabled = MutableLiveData(airshipAdapter.isStarted)

    init {
        adapterEnabled.observe(ProcessLifecycleOwner.get()) {
            if (it) {
                startAndConfigureAdapter()
            } else {
                airshipAdapter.stop()
                Timber.w("Gimbal Adapter Stopped.")
            }
        }
    }

    private fun startAndConfigureAdapter() {
        val context = UAirship.getApplicationContext()


        val hasForegroundLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val hasBackgroundLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasForegroundLocation || !hasBackgroundLocation) {
            Timber.e("Missing required location permissions! Gimbal may not work properly.")
            return
        }
        val locationManager = context.getSystemService(LocationManager::class.java)
        val isLocationEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER)
                ?: false || locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                ?: false

        if (!isLocationEnabled) {
            Timber.e("Location services are disabled! Prompting user to enable...")
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            return
        }
        UAirship.shared {
            it.pushManager.userNotificationsEnabled = true
            Timber.i("Airship Push Notifications Enabled.")
        }
        airshipAdapter.setShouldTrackCustomEntryEvent(true)
        airshipAdapter.setShouldTrackCustomExitEvent(true)
        airshipAdapter.start(GIMBAL_API_KEY)


        if (!Gimbal.isStarted()) {
            Timber.w("Gimbal was not started. Restarting Gimbal...")
            Gimbal.stop()
            Gimbal.start()
        }

        Timber.i("Gimbal Started: ${Gimbal.isStarted()}")
        Timber.i("Enabling Gimbal place monitoring with Airship custom events.")

        GimbalDebugger.enablePlaceLogging()
        GimbalDebugger.enableStatusLogging()

        Timber.i("Gimbal API Key: $GIMBAL_API_KEY")
        Timber.i("Gimbal Instance ID: ${Gimbal.getApplicationInstanceIdentifier()}")
    }
}
