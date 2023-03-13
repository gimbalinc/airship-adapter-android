package com.gimbal.airship.sample.presentation.permission

import androidx.lifecycle.ViewModel
import com.gimbal.airship.AirshipAdapter
import com.urbanairship.UAirship
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PermissionViewModel @Inject constructor(
    private val airshipAdapter: AirshipAdapter
): ViewModel() {

    fun onPermissionsGranted() {
        UAirship.shared() {
            it.pushManager.userNotificationsEnabled = true
        }
        airshipAdapter.setShouldTrackCustomEntryEvent(true)
        airshipAdapter.setShouldTrackCustomExitEvent(true)
        airshipAdapter.start("[YOUR GIMBAL API KEY]")
        Timber.i("Enabling Gimbal place monitoring w/ Airship custom events")
    }
}
