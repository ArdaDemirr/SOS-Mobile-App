package com.example.sos

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.telephony.TelephonyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// 4 Distinct Tactical States
enum class LocalLinkStatus {
    WIFI, CELL_DATA, CELL_PLAIN, DISCONNECTED
}

object NetworkStateManager {

    // --- 1. SECURE UPLINK STATE ---
    private val _isServerOnline = MutableStateFlow(true)
    val isServerOnline: StateFlow<Boolean> = _isServerOnline.asStateFlow()

    fun updateServerState(isOnline: Boolean) {
        _isServerOnline.value = isOnline
    }

    // --- 2. LOCAL LINK STATE ---
    private val _localNetworkStatus = MutableStateFlow(LocalLinkStatus.DISCONNECTED)
    val localNetworkStatus: StateFlow<LocalLinkStatus> = _localNetworkStatus.asStateFlow()

    fun startObservingLocalNetwork(context: Context) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        // Helper function to evaluate the 4 states
        fun evaluateNetwork(caps: NetworkCapabilities?): LocalLinkStatus {
            val hasCarrier = !telephonyManager.networkOperatorName.isNullOrBlank()

            return when {
                caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> LocalLinkStatus.WIFI
                caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> LocalLinkStatus.CELL_DATA
                hasCarrier -> LocalLinkStatus.CELL_PLAIN // No data, but connected to a cell tower
                else -> LocalLinkStatus.DISCONNECTED     // Complete dead zone
            }
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, capabilities)
                _localNetworkStatus.value = evaluateNetwork(capabilities)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                // We lost internet. Check if we still have a plain GSM carrier.
                _localNetworkStatus.value = evaluateNetwork(null)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Capture the immediate state right when the app opens
        val currentNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(currentNetwork)
        _localNetworkStatus.value = evaluateNetwork(caps)
    }
}