package com.gimbal.airship.sample.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.gimbal.airship.sample.domain.GimbalIntegration
import com.gimbal.airship.sample.domain.PlaceEventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val placeEventRepository: PlaceEventRepository,
    gimbalIntegration: GimbalIntegration
) : ViewModel() {

    val placeEvents = placeEventRepository.getPlaceEvents().asLiveData()
    val adapterEnabled = gimbalIntegration.adapterEnabled

    fun onDeleteClick() {
        viewModelScope.launch {
            placeEventRepository.clearPlaceEvents()
        }
    }
}
