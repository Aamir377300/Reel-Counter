package com.aamir.reelcounter.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aamir.reelcounter.ReelCounterApp
import com.aamir.reelcounter.util.PermissionHelpers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SetupChecklist(
    val accessibility: Boolean = false,
    val notifications: Boolean = false,
    val battery: Boolean = false
) {
    val allReady: Boolean get() = accessibility && notifications && battery
}

class SetupViewModel(app: Application) : AndroidViewModel(app) {

    private val _checklist = MutableStateFlow(SetupChecklist())
    val checklist: StateFlow<SetupChecklist> = _checklist.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            while (true) {
                delay(1_500)
                refresh()
            }
        }
    }

    fun refresh() {
        val ctx = getApplication<Application>()
        _checklist.update {
            SetupChecklist(
                accessibility = PermissionHelpers.isAccessibilityEnabled(ctx),
                notifications = PermissionHelpers.areNotificationsEnabled(ctx),
                battery = PermissionHelpers.isBatteryUnrestricted(ctx)
            )
        }
    }

    fun markSetupDone() {
        viewModelScope.launch {
            ReelCounterApp.get().preferences.setSetupCompleted(true)
        }
    }

    companion object {
        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SetupViewModel(app) as T
                }
            }
    }
}
