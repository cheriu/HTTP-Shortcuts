package ch.rmy.android.http_shortcuts.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.wifi.WifiManager
import androidx.core.content.getSystemService
import ch.rmy.android.framework.extensions.showToast
import ch.rmy.android.framework.extensions.startActivity
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.data.enums.IpVersion
import java.net.Inet4Address
import java.net.Inet6Address
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

    fun getRouteAddress(ipVersion: IpVersion): String? =
        getRouteAddress(context, ipVersion)

    companion object {
        fun getRouteAddress(
            context: Context,
            ipVersion: IpVersion,
        ): String? =
            context.getSystemService<ConnectivityManager>()
                ?.let { connectivityManager ->
                    connectivityManager.activeNetwork
                        ?.let(connectivityManager::getLinkProperties)
                }
                ?.getLinkAddress(ipVersion)

        private fun LinkProperties.getLinkAddress(ipVersion: IpVersion): String? =
            linkAddresses
                .map { it.address }
                .filterNot { address ->
                    address.isAnyLocalAddress || address.isLoopbackAddress || address.isMulticastAddress
                }
                .let { addresses ->
                    when (ipVersion) {
                        IpVersion.V4 ->
                            addresses
                                .asSequence()
                                .filterNot { it.isLinkLocalAddress }
                                .mapNotNull { address ->
                                    (address as? Inet4Address)?.hostAddress
                                }
                                .firstOrNull()

                        IpVersion.V6 ->
                            addresses
                                .asSequence()
                                .mapNotNull { address ->
                                    (address as? Inet6Address)?.hostAddress?.substringBefore('%')
                                }
                                .firstOrNull()
                    }
                }
    }

    fun getIPV4RouteAddress(): String? =
        getRouteAddress(IpVersion.V4)

    fun getIPV6RouteAddress(): String? =
        getRouteAddress(IpVersion.V6)

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
