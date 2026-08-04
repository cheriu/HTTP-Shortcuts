package ch.rmy.android.http_shortcuts.activities.networkinterfaces

import android.app.Application
import ch.rmy.android.framework.viewmodel.BaseViewModel
import ch.rmy.android.http_shortcuts.utils.NetworkUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NetworkInterfacesViewModel
@Inject
constructor(
    application: Application,
    private val networkUtil: NetworkUtil,
) : BaseViewModel<Unit, NetworkInterfacesViewState>(application) {

    override suspend fun initialize(data: Unit) = NetworkInterfacesViewState(
        activeNetworkInfo = networkUtil.getActiveNetworkInfo(),
        interfaces = networkUtil.getNetworkInterfacesInfo(),
    )

    fun onRefreshRequested() = runAction {
        updateViewState {
            copy(isRefreshing = true)
        }
        updateViewState {
            copy(
                activeNetworkInfo = networkUtil.getActiveNetworkInfo(),
                interfaces = networkUtil.getNetworkInterfacesInfo(),
                isRefreshing = false,
            )
        }
    }
}
