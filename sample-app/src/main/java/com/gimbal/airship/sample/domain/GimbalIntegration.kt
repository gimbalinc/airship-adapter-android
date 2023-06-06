package com.gimbal.airship.sample.domain

import com.gimbal.airship.AirshipAdapter
import com.gimbal.android.AnalyticsManager
import com.gimbal.android.GimbalDebugger
import com.urbanairship.UAirship
import timber.log.Timber
import javax.inject.Inject

class GimbalIntegration @Inject constructor(
    private val airshipAdapter: AirshipAdapter,
) {
    private companion object {
        const val GIMBAL_API_KEY = "[YOUR GIMBAL API KEY]"
    }

    val adapterEnabled = airshipAdapter.isStarted

    fun startAndConfigureAdapter(userNotificationsEnabled : Boolean) {
        UAirship.shared() {
            it.pushManager.userNotificationsEnabled = userNotificationsEnabled
            it.pushManager.addPushListener { message, notificationPosted ->
                Timber.d("Notification Posted!")
            }
            it.contact.identify("MykUserID131313")
        }
        airshipAdapter.setShouldTrackCustomEntryEvent(true)
        airshipAdapter.setShouldTrackCustomExitEvent(true)
        airshipAdapter.start(GIMBAL_API_KEY)
        Timber.i("Enabling Gimbal place monitoring w/ Airship custom events")
        GimbalDebugger.enableStatusLogging()

        // Comment or uncomment as needed. Specify null to invoke User Analytics ID deletion.
        updateUserAnalyticsId("@n@lyt1c5-ID");
        // updateUserAnalyticsId(null);
    }

    fun stopAdapter() {
        airshipAdapter.stop()
    }

    private fun updateUserAnalyticsId(userAnalyticsId: String?) {
        if (userAnalyticsId == null) {
            AnalyticsManager.getInstance().deleteUserAnalyticsID()
            Timber.i("Deleted User Analytics Id")
        } else {
            AnalyticsManager.getInstance().setUserAnalyticsID(userAnalyticsId)
            Timber.i("Set new User Analytics Id: {}", userAnalyticsId)
        }
    }
}
