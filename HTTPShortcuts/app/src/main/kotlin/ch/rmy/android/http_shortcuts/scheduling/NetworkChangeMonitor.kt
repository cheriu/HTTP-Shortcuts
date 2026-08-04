package ch.rmy.android.http_shortcuts.scheduling

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import ch.rmy.android.framework.extensions.tryOrLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkChangeMonitor
@Inject
constructor(
    private val context: Context,
    private val starter: NetworkChangeWorker.Starter,
) {
    private val connectivityManager by lazy {
        context.getSystemService<ConnectivityManager>()
    }

    private var started = false

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            starter.scheduleCheck()
        }

        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
            starter.scheduleCheck()
        }

        override fun onLost(network: Network) {
            starter.scheduleCheck()
        }
    }

    fun start() {
        if (started) {
            return
        }

        val connectivityManager = connectivityManager ?: return
        tryOrLog {
            connectivityManager.registerNetworkCallback(NetworkRequest.Builder().build(), callback)
            starter.scheduleRepeating()
            started = true
        }
    }
}
