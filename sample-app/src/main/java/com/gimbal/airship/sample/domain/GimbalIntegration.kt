package com.gimbal.airship.sample.domain

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ProcessLifecycleOwner
import com.gimbal.airship.AirshipAdapter
import com.urbanairship.UAirship
import timber.log.Timber
import javax.inject.Inject

class GimbalIntegration @Inject constructor(
    private val airshipAdapter: AirshipAdapter,
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
            }
        }
    }

    private fun startAndConfigureAdapter() {
        UAirship.shared() {
            it.pushManager.userNotificationsEnabled = true
        }
        airshipAdapter.setShouldTrackCustomEntryEvent(true)
        airshipAdapter.setShouldTrackCustomExitEvent(true)
        airshipAdapter.start(GIMBAL_API_KEY)
        Timber.i("Enabling Gimbal place monitoring w/ Airship custom events")
    }
}
