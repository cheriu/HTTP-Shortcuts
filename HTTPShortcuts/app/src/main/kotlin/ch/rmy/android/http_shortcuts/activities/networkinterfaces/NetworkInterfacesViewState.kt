package ch.rmy.android.http_shortcuts.activities.networkinterfaces

import androidx.compose.runtime.Stable
import ch.rmy.android.http_shortcuts.data.dtos.ActiveNetworkInfo
import ch.rmy.android.http_shortcuts.data.dtos.NetworkInterfaceInfo

@Stable
data class NetworkInterfacesViewState(
    val activeNetworkInfo: ActiveNetworkInfo?,
    val interfaces: List<NetworkInterfaceInfo>,
    val isRefreshing: Boolean = false,
)
