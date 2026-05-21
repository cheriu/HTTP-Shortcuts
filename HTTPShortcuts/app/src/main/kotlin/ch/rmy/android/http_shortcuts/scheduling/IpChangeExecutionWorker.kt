package ch.rmy.android.http_shortcuts.scheduling

import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ch.rmy.android.framework.extensions.logException
import ch.rmy.android.framework.extensions.runIf
import ch.rmy.android.framework.extensions.runIfNotNull
import ch.rmy.android.framework.utils.UUIDUtils.newUUID
import ch.rmy.android.http_shortcuts.activities.execute.DialogHandle
import ch.rmy.android.http_shortcuts.activities.execute.ExecuteDialogState
import ch.rmy.android.http_shortcuts.data.domains.certificate_pins.CertificatePinRepository
import ch.rmy.android.http_shortcuts.data.domains.request_headers.RequestHeaderRepository
import ch.rmy.android.http_shortcuts.data.domains.request_parameters.RequestParameterRepository
import ch.rmy.android.http_shortcuts.data.domains.shortcut_sync_state.ShortcutSyncStateRepository
import ch.rmy.android.http_shortcuts.data.domains.shortcuts.ShortcutId
import ch.rmy.android.http_shortcuts.data.domains.shortcuts.ShortcutRepository
import ch.rmy.android.http_shortcuts.data.domains.variables.GlobalVariableRepository
import ch.rmy.android.http_shortcuts.data.enums.IpVersion
import ch.rmy.android.http_shortcuts.data.enums.ParameterType
import ch.rmy.android.http_shortcuts.data.models.RequestParameter
import ch.rmy.android.http_shortcuts.data.models.Shortcut
import ch.rmy.android.http_shortcuts.extensions.canAutoUpdateOnIpChange
import ch.rmy.android.http_shortcuts.extensions.getAutoUpdateIpVersions
import ch.rmy.android.http_shortcuts.extensions.getRequestHeadersForShortcut
import ch.rmy.android.http_shortcuts.extensions.getRequestParametersForShortcut
import ch.rmy.android.http_shortcuts.http.HttpRequester
import ch.rmy.android.http_shortcuts.utils.NetworkUtil
import ch.rmy.android.http_shortcuts.variables.VariableManager
import ch.rmy.android.http_shortcuts.variables.VariableResolver
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.time.Duration
import kotlinx.coroutines.CancellationException

@HiltWorker
class IpChangeExecutionWorker
@AssistedInject
constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val shortcutRepository: ShortcutRepository,
    private val requestHeaderRepository: RequestHeaderRepository,
    private val requestParameterRepository: RequestParameterRepository,
    private val globalVariableRepository: GlobalVariableRepository,
    private val certificatePinRepository: CertificatePinRepository,
    private val httpRequester: HttpRequester,
    private val variableResolver: VariableResolver,
    private val shortcutSyncStateRepository: ShortcutSyncStateRepository,
    private val networkUtil: NetworkUtil,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val shortcutId = inputData.getString(INPUT_SHORTCUT_ID) ?: return Result.failure()
        val shortcut = try {
            shortcutRepository.getShortcutById(shortcutId)
        } catch (_: NoSuchElementException) {
            return Result.failure()
        }

        if (!shortcut.canAutoUpdateOnIpChange) {
            return Result.success()
        }

        val requestHeaders = requestHeaderRepository.getRequestHeadersForShortcut(shortcut)
        val requestParameters = requestParameterRepository.getRequestParametersForShortcut(shortcut)
        val monitoredIpVersions = shortcut.getAutoUpdateIpVersions(requestHeaders, requestParameters)
        if (monitoredIpVersions.isEmpty()) {
            return Result.success()
        }

        val currentIps = buildMap<IpVersion, String> {
            monitoredIpVersions.forEach { ipVersion ->
                networkUtil.getRouteAddress(ipVersion)?.let { put(ipVersion, it) }
            }
        }
        if (currentIps.isEmpty()) {
            return Result.success()
        }

        val syncState = shortcutSyncStateRepository.getByShortcutId(shortcut.id)
        val shouldRun = currentIps.any { (ipVersion, currentIp) ->
            currentIp != when (ipVersion) {
                IpVersion.V4 -> syncState?.lastSentIpv4
                IpVersion.V6 -> syncState?.lastSentIpv6
            }
        }
        if (!shouldRun || getBackgroundIneligibilityReason(shortcut, requestParameters) != null) {
            return Result.success()
        }

        val variableManager = VariableManager(globalVariableRepository.getGlobalVariables())
        try {
            variableResolver.resolve(
                variableManager = variableManager,
                variableKeysOrIds = VariableResolver.findResolvableVariableIdentifiersExcludingScripting(shortcut, requestHeaders, requestParameters),
                dialogHandle = NoUiDialogHandle,
            )
        } catch (_: UnsupportedOperationException) {
            return Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logException(e)
            return Result.success()
        }

        try {
            httpRequester.executeShortcut(
                context = applicationContext,
                shortcut = shortcut,
                headers = requestHeaders,
                parameters = requestParameters,
                storeDirectoryUri = null,
                sessionId = "${shortcut.id}_${newUUID()}",
                variableValues = variableManager.getVariableValues(),
                useCookieJar = shortcut.acceptCookies,
                certificatePins = certificatePinRepository.getCertificatePins(),
            )
            shortcutSyncStateRepository.updateLastSentIp(shortcut.id, currentIps[IpVersion.V4], currentIps[IpVersion.V6])
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logException(e)
        }

        return Result.success()
    }

    class Starter
    @Inject
    constructor(
        private val context: Context,
    ) {
        operator fun invoke(shortcutId: ShortcutId, debounceWindowMs: Duration?) {
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "ip_change_execution_$shortcutId",
                    ExistingWorkPolicy.KEEP,
                    OneTimeWorkRequestBuilder<IpChangeExecutionWorker>()
                        .setInputData(Data.Builder().putString(INPUT_SHORTCUT_ID, shortcutId).build())
                        .runIfNotNull(debounceWindowMs) {
                            setInitialDelay(it.inWholeMilliseconds, TimeUnit.MILLISECONDS)
                        }
                        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                        .runIf(debounceWindowMs == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        }
                        .build(),
                )
        }
    }

    private data object NoUiDialogHandle : DialogHandle {
        override suspend fun <T : Any> showDialog(dialogState: ExecuteDialogState<T>): T = throw UnsupportedOperationException()
    }

    private fun getBackgroundIneligibilityReason(shortcut: Shortcut, requestParameters: List<RequestParameter>): String? = when {
        shortcut.confirmationType != null -> "execution requires confirmation"
        shortcut.wifiSsid?.isNotEmpty() == true && networkUtil.getCurrentSsid().orEmpty() != shortcut.wifiSsid -> "not connected to required Wi-Fi SSID"
        shortcut.codeOnPrepare.isNotEmpty() || shortcut.codeOnSuccess.isNotEmpty() || shortcut.codeOnFailure.isNotEmpty() -> "shortcut uses scripting hooks"
        shortcut.usesGenericFileBody() -> "shortcut uses file upload body"
        shortcut.usesRequestParameters() && requestParameters.any { it.parameterType == ParameterType.FILE } -> "shortcut uses file request parameters"
        shortcut.responseStoreDirectoryId != null -> "shortcut stores response to a file"
        else -> null
    }

    companion object {
        private const val INPUT_SHORTCUT_ID = "shortcutId"
    }
}
