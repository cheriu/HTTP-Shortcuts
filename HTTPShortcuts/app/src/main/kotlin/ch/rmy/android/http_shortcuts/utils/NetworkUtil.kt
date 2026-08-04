package ch.rmy.android.http_shortcuts.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import androidx.core.content.getSystemService
import ch.rmy.android.framework.extensions.showToast
import ch.rmy.android.framework.extensions.startActivity
import ch.rmy.android.http_shortcuts.R
import java.net.NetworkInterface
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NetworkUtil
@Inject
constructor(
    private val context: Context,
    private val activityProvider: ActivityProvider,
    private val restrictionsUtil: RestrictionsUtil,
) {

    fun isNetworkConnected(): Boolean =
        context.getSystemService<ConnectivityManager>()
            ?.activeNetworkInfo
            ?.isConnected == true

    fun isNetworkPerformanceRestricted() =
        restrictionsUtil.isDataSaverModeEnabled() || restrictionsUtil.isBatterySaverModeEnabled()

    fun getCurrentSsid(): String? =
        context.applicationContext.getSystemService<WifiManager>()
            ?.connectionInfo
            ?.ssid
            ?.trim('"')

    fun getIPV4Address(): String? =
        context.applicationContext.getSystemService<WifiManager>()
            ?.connectionInfo
            ?.ipAddress
            ?.takeUnless { it == 0 }
            ?.let(::formatIPV4Address)

    /**
     * Returns a deterministic fingerprint of the IP addresses of the interface that carries the
     * device's internet traffic (i.e., the interface of the default network's default route),
     * or an empty string if the device is currently disconnected from the internet.
     *
     * This intentionally ignores all other network interfaces (e.g., the cellular interface used
     * for calls), as only the default network's interface determines the device's IP address.
     */
    fun getNetworkFingerprint(): String? {
        val connectivityManager = context.applicationContext.getSystemService<ConnectivityManager>()
            ?: return null
        val activeNetwork = connectivityManager.activeNetwork
            ?: return NO_ACTIVE_NETWORK_FINGERPRINT
        val linkProperties = connectivityManager.getLinkProperties(activeNetwork)
            ?: return null
        val interfaceName = linkProperties.routes
            .firstOrNull { it.isDefaultRoute }
            ?.getInterface()
            ?: return null

        val addresses = try {
            NetworkInterface.getNetworkInterfaces()
                .asSequence()
                .firstOrNull { it.name == interfaceName }
                ?.inetAddresses
                ?.asSequence()
                ?.map { it.hostAddress }
                ?.sorted()
                ?.toList()
        } catch (_: Exception) {
            return null
        }
        if (addresses == null) {
            return null
        }

        return "$interfaceName:${addresses.joinToString(separator = ",")}"
    }

    private companion object {
        const val NO_ACTIVE_NETWORK_FINGERPRINT = ""
    }

    private fun formatIPV4Address(ip: Int): String =
        "${ip shr 0 and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"

    suspend fun showWifiPicker() {
        try {
            activityProvider.withActivity { activity ->
                Intent(WifiManager.ACTION_PICK_WIFI_NETWORK)
                    .putExtra("extra_prefs_show_button_bar", true)
                    .putExtra("wifi_enable_next_on_connect", true)
                    .startActivity(activity)
            }
        } catch (_: ActivityNotFoundException) {
            CoroutineScope(Dispatchers.Main).launch {
                context.showToast(R.string.error_not_supported)
            }
        }
    }
}
