package ch.rmy.android.http_shortcuts.activities.networkinterfaces

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.components.SimpleScaffold
import ch.rmy.android.http_shortcuts.components.bindViewModel

@Composable
fun NetworkInterfacesScreen() {
    val (viewModel, state) = bindViewModel<NetworkInterfacesViewState, NetworkInterfacesViewModel>()

    SimpleScaffold(
        viewState = state,
        title = stringResource(R.string.title_network_interfaces),
        actions = { viewState ->
            IconButton(
                onClick = viewModel::onRefreshRequested,
                enabled = !viewState.isRefreshing,
            ) {
                Icon(
                    painter = painterResource(R.drawable.outline_refresh_24),
                    contentDescription = stringResource(R.string.action_refresh),
                )
            }
        },
    ) { viewState ->
        NetworkInterfacesContent(
            activeNetworkInfo = viewState.activeNetworkInfo,
            interfaces = viewState.interfaces,
        )
    }
}
