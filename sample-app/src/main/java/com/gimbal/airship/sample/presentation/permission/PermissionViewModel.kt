package com.gimbal.airship.sample.presentation.permission

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.gimbal.airship.sample.domain.GimbalIntegration
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PermissionViewModel @Inject constructor(
    gimbalIntegration: GimbalIntegration
): ViewModel() {

    val adapterEnabled: MutableLiveData<Boolean> = gimbalIntegration.adapterEnabled
}
