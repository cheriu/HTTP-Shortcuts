package ch.rmy.android.http_shortcuts.scheduling

import ch.rmy.android.http_shortcuts.data.domains.request_headers.RequestHeaderRepository
import ch.rmy.android.http_shortcuts.data.domains.request_parameters.RequestParameterRepository
import ch.rmy.android.http_shortcuts.data.domains.shortcut_sync_state.ShortcutSyncStateRepository
import ch.rmy.android.http_shortcuts.data.domains.shortcuts.ShortcutRepository
import ch.rmy.android.http_shortcuts.data.enums.IpVersion
import ch.rmy.android.http_shortcuts.extensions.canAutoUpdateOnIpChange
import ch.rmy.android.http_shortcuts.extensions.getAutoUpdateIpVersions
import ch.rmy.android.http_shortcuts.extensions.getRequestHeadersForShortcut
import ch.rmy.android.http_shortcuts.extensions.getRequestParametersForShortcut
import ch.rmy.android.http_shortcuts.utils.NetworkUtil
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class IpChangeAutoUpdateScheduler
@Inject
constructor(
    private val shortcutRepository: ShortcutRepository,
    private val requestHeaderRepository: RequestHeaderRepository,
    private val requestParameterRepository: RequestParameterRepository,
    private val shortcutSyncStateRepository: ShortcutSyncStateRepository,
    private val networkUtil: NetworkUtil,
    private val executionStarter: IpChangeExecutionWorker.Starter,
) {
    suspend fun scheduleEligibleShortcuts() {
        val currentIpv4 = networkUtil.getIPV4RouteAddress()
        val currentIpv6 = networkUtil.getIPV6RouteAddress()

        shortcutRepository.getShortcuts()
            .filter { it.canAutoUpdateOnIpChange }
            .forEach { shortcut ->
                val requestHeaders = requestHeaderRepository.getRequestHeadersForShortcut(shortcut)
                val requestParameters = requestParameterRepository.getRequestParametersForShortcut(shortcut)
                val monitoredIpVersions = shortcut.getAutoUpdateIpVersions(requestHeaders, requestParameters)
                if (monitoredIpVersions.isEmpty()) {
                    return@forEach
                }

                val syncState = shortcutSyncStateRepository.getByShortcutId(shortcut.id)
                val shouldRun = monitoredIpVersions.any { ipVersion ->
                    val currentIp = when (ipVersion) {
                        IpVersion.V4 -> currentIpv4
                        IpVersion.V6 -> currentIpv6
                    } ?: return@any false
                    val lastSentIp = when (ipVersion) {
                        IpVersion.V4 -> syncState?.lastSentIpv4
                        IpVersion.V6 -> syncState?.lastSentIpv6
                    }
                    currentIp != lastSentIp
                }

                if (shouldRun) {
                    executionStarter(shortcut.id, shortcut.debounceWindowMs?.milliseconds)
                }
            }
    }
}
