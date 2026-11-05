package com.example.core.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

data class NetState(
    val online: Boolean,
    val isWifi: Boolean,
    val downKbps: Int,
    val validated: Boolean
)

object NetworkMonitor {
    fun isOnline(ctx: Context): Boolean {
        val cm = ctx.getSystemService(ConnectivityManager::class.java) ?: return false
        return cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }?.let {
            it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } == true
    }

    fun states(ctx: Context): Flow<NetState> = callbackFlow {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        if (cm == null) {
            trySend(NetState(online = false, isWifi = false, downKbps = 0, validated = false))
            close()
            return@callbackFlow
        }

        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(n: Network, caps: NetworkCapabilities) {
                trySend(NetState(
                    online = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
                    isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
                    downKbps = caps.linkDownstreamBandwidthKbps,
                    validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                ))
            }
            override fun onLost(n: Network) {
                trySend(NetState(online = false, isWifi = false, downKbps = 0, validated = false))
            }
        }
        cm.registerDefaultNetworkCallback(cb)
        awaitClose { cm.unregisterNetworkCallback(cb) }
    }.conflate()
}
