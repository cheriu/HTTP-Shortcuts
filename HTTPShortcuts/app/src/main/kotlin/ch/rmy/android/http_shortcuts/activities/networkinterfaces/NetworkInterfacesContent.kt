package ch.rmy.android.http_shortcuts.activities.networkinterfaces

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.components.SettingsGroup
import ch.rmy.android.http_shortcuts.components.Spacing
import ch.rmy.android.http_shortcuts.components.VerticalSpacer
import ch.rmy.android.http_shortcuts.data.dtos.ActiveNetworkInfo
import ch.rmy.android.http_shortcuts.data.dtos.NetworkInterfaceInfo

@Composable
fun NetworkInterfacesContent(
    activeNetworkInfo: ActiveNetworkInfo?,
    interfaces: List<NetworkInterfaceInfo>,
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = Spacing.SMALL),
    ) {
        activeNetworkInfo?.let { info ->
            SettingsGroup(title = stringResource(R.string.label_network_interface_active)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.MEDIUM),
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.MEDIUM),
                    ) {
                        Text(
                            text = info.interfaceName ?: "—",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        VerticalSpacer(Spacing.TINY)
                        InfoRow(
                            label = stringResource(R.string.label_network_interface_transports),
                            value = formatList(info.transports),
                        )
                        InfoRow(
                            label = stringResource(R.string.label_network_interface_gateway),
                            value = formatList(info.gateways),
                        )
                        InfoRow(
                            label = stringResource(R.string.label_network_interface_dns),
                            value = formatList(info.dnsServers),
                        )
                    }
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = Spacing.SMALL),
            )
        }

        SettingsGroup(title = stringResource(R.string.title_network_interfaces)) {
            interfaces.forEachIndexed { index, networkInterface ->
                NetworkInterfaceItem(networkInterface = networkInterface)
                if (index < interfaces.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun NetworkInterfaceItem(networkInterface: NetworkInterfaceInfo) {
    var expanded by remember { mutableStateOf(false) }
    val stateLabel = stringResource(
        if (networkInterface.isUp) {
            R.string.label_network_interface_up
        } else {
            R.string.label_network_interface_down
        },
    )
    ListItem(
        headlineContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(networkInterface.name)
                Spacer(modifier = Modifier.width(Spacing.SMALL))
                Text(
                    text = stateLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (networkInterface.isUp) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }
        },
        supportingContent = if (expanded) {
            {
                Column(
                    modifier = Modifier.padding(top = Spacing.TINY),
                ) {
                    networkInterface.macAddress?.let { macAddress ->
                        InfoRow(
                            label = stringResource(R.string.label_network_interface_mac_address),
                            value = macAddress,
                        )
                    }
                    InfoRow(
                        label = stringResource(R.string.label_network_interface_mtu),
                        value = networkInterface.mtu.toString(),
                    )
                    if (networkInterface.addresses.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.label_network_interface_addresses),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        networkInterface.addresses.forEach { address ->
                            Text(
                                text = "${address.address}/${address.prefixLength}",
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        } else {
            null
        },
        trailingContent = {
            Icon(
                painter = painterResource(
                    if (expanded) {
                        R.drawable.outline_keyboard_arrow_up_24
                    } else {
                        R.drawable.outline_keyboard_arrow_down_24
                    },
                ),
                contentDescription = null,
            )
        },
        modifier = Modifier.clickable { expanded = !expanded },
    )
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(vertical = Spacing.TINY),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun formatList(values: List<String>): String =
    if (values.isEmpty()) {
        "—"
    } else {
        values.joinToString(", ")
    }
