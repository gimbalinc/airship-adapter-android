package com.gimbal.airship.sample.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.gimbal.airship.AirshipAdapter
import com.gimbal.airship.sample.domain.PlaceEventRepository
import com.urbanairship.UAirship
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val airshipAdapter: AirshipAdapter,
    private val placeEventRepository: PlaceEventRepository
) : ViewModel() {

    val placeEvents = placeEventRepository.getPlaceEvents().asLiveData()

    fun onPermissionsGranted() {
        UAirship.shared() {
            it.pushManager.userNotificationsEnabled = true
        }
        airshipAdapter.setShouldTrackCustomEntryEvent(true)
        airshipAdapter.setShouldTrackCustomExitEvent(true)
        airshipAdapter.start("[YOUR GIMBAL API KEY]")
    }

    fun onDeleteClick() {
        viewModelScope.launch {
            placeEventRepository.clearPlaceEvents()
        }
    }
}
