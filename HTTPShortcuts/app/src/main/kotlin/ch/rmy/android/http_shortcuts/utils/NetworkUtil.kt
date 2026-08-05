package ch.rmy.android.http_shortcuts.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import androidx.core.content.getSystemService
import ch.rmy.android.framework.extensions.showToast
import ch.rmy.android.framework.extensions.startActivity
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.data.dtos.ActiveNetworkInfo
import ch.rmy.android.http_shortcuts.data.dtos.AddressInfo
import ch.rmy.android.http_shortcuts.data.dtos.NetworkInterfaceInfo
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.NetworkInterface
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class IpAddressInfo(
    val address: String,
    val interfaceName: String?,
)

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

    companion object {
        const val NO_ACTIVE_NETWORK_FINGERPRINT = ""

        fun getIpv4AddressInfo(context: Context): IpAddressInfo? {
            val connectivityManager = context.applicationContext.getSystemService<ConnectivityManager>()
                ?: return null
            val activeNetwork = connectivityManager.activeNetwork
                ?: return null
            val linkProperties = connectivityManager.getLinkProperties(activeNetwork)
                ?: return null
            val address = linkProperties.linkAddresses
                ?.asSequence()
                ?.map { it.address }
                ?.filter { it is Inet4Address && !it.isLoopbackAddress }
                ?.map { it.hostAddress }
                ?.firstOrNull()
                ?: return null
            return IpAddressInfo(address, linkProperties.interfaceName)
        }

        fun getIpv6AddressInfo(context: Context): IpAddressInfo? {
            val connectivityManager = context.applicationContext.getSystemService<ConnectivityManager>()
                ?: return null
            val activeNetwork = connectivityManager.activeNetwork
                ?: return null
            val linkProperties = connectivityManager.getLinkProperties(activeNetwork)
                ?: return null
            val address = linkProperties.linkAddresses
                ?.asSequence()
                ?.map { it.address }
                ?.filter { it is Inet6Address && !it.isLinkLocalAddress && !it.isLoopbackAddress }
                ?.map { it.hostAddress }
                ?.firstOrNull()
                ?: return null
            return IpAddressInfo(address, linkProperties.interfaceName)
        }
    }

    private fun formatIPV4Address(ip: Int): String =
        "${ip shr 0 and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"

    fun getNetworkInterfacesInfo(): List<NetworkInterfaceInfo> =
        try {
            NetworkInterface.getNetworkInterfaces()
                ?.asSequence()
                ?.map { networkInterface ->
                    NetworkInterfaceInfo(
                        name = networkInterface.name,
                        isUp = networkInterface.isUp,
                        isLoopback = networkInterface.isLoopback,
                        isPointToPoint = networkInterface.isPointToPoint,
                        isVirtual = networkInterface.isVirtual,
                        mtu = networkInterface.mtu,
                        macAddress = networkInterface.hardwareAddress?.formatMacAddress(),
                        addresses = networkInterface.interfaceAddresses.map { addressInfo ->
                            AddressInfo(
                                address = addressInfo.address.hostAddress ?: "",
                                prefixLength = addressInfo.networkPrefixLength.toInt(),
                            )
                        },
                    )
                }
                ?.sortedBy { it.name }
                ?.toList()
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

    fun getActiveIpv4Address(): String? =
        getIpv4AddressInfo(context)?.address

    fun getActiveIpv6Address(): String? =
        getIpv6AddressInfo(context)?.address

    fun getActiveNetworkInfo(): ActiveNetworkInfo? {
        val connectivityManager = context.applicationContext.getSystemService<ConnectivityManager>()
            ?: return null
        val activeNetwork = connectivityManager.activeNetwork
            ?: return null
        val linkProperties = connectivityManager.getLinkProperties(activeNetwork)
            ?: return null
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        val interfaceName = linkProperties.routes
            .firstOrNull { it.isDefaultRoute }
            ?.getInterface()
        val gateways = linkProperties.routes
            .filter { it.isDefaultRoute }
            .mapNotNull { it.gateway?.hostAddress }
        val dnsServers = linkProperties.dnsServers.mapNotNull { it.hostAddress }
        val transports = buildList {
            val caps = capabilities ?: return@buildList
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) add("Wi-Fi")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) add("Cellular")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) add("Ethernet")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) add("VPN")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) add("Bluetooth")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_USB)) add("USB")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE)) add("Wi-Fi Aware")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_LOWPAN)) add("LoWPAN")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_SATELLITE)) add("Satellite")
        }

        return ActiveNetworkInfo(
            interfaceName = interfaceName,
            transports = transports,
            gateways = gateways,
            dnsServers = dnsServers,
        )
    }

    private fun ByteArray.formatMacAddress(): String =
        joinToString(":") { String.format("%02x", it) }

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
