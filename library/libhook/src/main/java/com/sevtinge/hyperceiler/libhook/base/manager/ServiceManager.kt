package com.sevtinge.hyperceiler.libhook.base.manager

import com.sevtinge.hyperceiler.libhook.utils.log.AndroidLog
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils
import io.github.libxposed.service.RemotePreferences
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean


object ServiceManager {

    private const val TAG = "HyperCeiler/ServiceManager"

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Connecting)
    val connectionState = _connectionState.asStateFlow()

    private val isInitialized = AtomicBoolean(false)

    @JvmStatic
    val service: XposedService?
        get() = (connectionState.value as? ConnectionState.Connected)?.service

    @JvmStatic
    val isModuleActivated: Boolean
        get() = connectionState.value is ConnectionState.Connected

    fun init() {
        if (!isInitialized.compareAndSet(false, true)) {
            return
        }

        val listener = object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(boundService: XposedService) {
                AndroidLog.i(TAG, "LSPosed service connected: ${boundService.frameworkName} v${boundService.frameworkVersion}")
                _connectionState.value = ConnectionState.Connected(boundService)
                PrefsUtils.remotePrefs =
                    service!!.getRemotePreferences(PrefsUtils.mPrefsName + "_remote") as RemotePreferences
            }

            override fun onServiceDied(deadService: XposedService) {
                if (service == deadService) {
                    AndroidLog.e(TAG, "LSPosed service died.")
                    _connectionState.value = ConnectionState.Disconnected
                    PrefsUtils.remotePrefs = null
                }
            }
        }

        XposedServiceHelper.registerListener(listener)
        AndroidLog.i(TAG, "ServiceManager initialized and listener registered.")
    }
}

sealed interface ConnectionState {
    data object Connecting : ConnectionState
    data class Connected(val service: XposedService) : ConnectionState
    data object Disconnected : ConnectionState
}
