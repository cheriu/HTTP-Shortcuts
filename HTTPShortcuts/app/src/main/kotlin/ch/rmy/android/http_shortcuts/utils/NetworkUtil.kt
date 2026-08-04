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
     * Returns a deterministic fingerprint of the IP addresses of all mobile data (rmnet*) and
     * Wi-Fi (wlan*) interfaces, or null if no such interfaces exist.
     */
    fun getNetworkFingerprint(): String? {
        val interfaces = try {
            NetworkInterface.getNetworkInterfaces()
        } catch (_: Exception) {
            null
        } ?: return null

        val monitoredInterfaces = interfaces
            .asSequence()
            .filter { interfaceInfo ->
                interfaceInfo.name.startsWith("rmnet") || interfaceInfo.name.startsWith("wlan")
            }
            .map { interfaceInfo ->
                interfaceInfo.name to interfaceInfo.inetAddresses
                    .asSequence()
                    .map { it.hostAddress }
                    .sorted()
                    .toList()
            }
            .sortedBy { it.first }
            .toList()

        if (monitoredInterfaces.isEmpty()) {
            return null
        }

        return monitoredInterfaces.joinToString(separator = "\n") { (name, addresses) ->
            "$name:${addresses.joinToString(separator = ",")}"
        }
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
