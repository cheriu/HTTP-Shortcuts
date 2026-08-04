package ch.rmy.android.http_shortcuts.data.dtos

import androidx.compose.runtime.Stable

@Stable
data class NetworkInterfaceInfo(
    val name: String,
    val isUp: Boolean,
    val isLoopback: Boolean,
    val isPointToPoint: Boolean,
    val isVirtual: Boolean,
    val mtu: Int,
    val macAddress: String?,
    val addresses: List<AddressInfo>,
)

@Stable
data class AddressInfo(
    val address: String,
    val prefixLength: Int,
)

@Stable
data class ActiveNetworkInfo(
    val interfaceName: String?,
    val transports: List<String>,
    val gateways: List<String>,
    val dnsServers: List<String>,
)
