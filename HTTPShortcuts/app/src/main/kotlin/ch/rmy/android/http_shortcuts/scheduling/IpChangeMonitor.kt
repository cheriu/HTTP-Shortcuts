package ch.rmy.android.http_shortcuts.scheduling

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import androidx.core.content.getSystemService
import ch.rmy.android.framework.extensions.tryOrLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IpChangeMonitor
@Inject
constructor(
    private val context: Context,
    private val reconciliationStarter: IpChangeReconciliationWorker.Starter,
) {
    private val connectivityManager by lazy {
        context.getSystemService<ConnectivityManager>()
    }

    private var started = false

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            reconciliationStarter.scheduleNow()
        }

        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
            reconciliationStarter.scheduleNow()
        }

        override fun onLost(network: Network) {
            reconciliationStarter.scheduleNow()
        }
    }

    fun start() {
        if (started) {
            return
        }

        val connectivityManager = connectivityManager ?: return
        tryOrLog {
            connectivityManager.registerDefaultNetworkCallback(callback)
            reconciliationStarter.scheduleRepeating()
            started = true
        }
    }
}
