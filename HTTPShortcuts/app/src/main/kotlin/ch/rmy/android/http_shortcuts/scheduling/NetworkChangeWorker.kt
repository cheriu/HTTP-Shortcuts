package ch.rmy.android.http_shortcuts.scheduling

import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ch.rmy.android.framework.extensions.logInfo
import ch.rmy.android.framework.extensions.runIf
import ch.rmy.android.framework.extensions.tryOrLog
import ch.rmy.android.http_shortcuts.activities.execute.ExecutionStarter
import ch.rmy.android.http_shortcuts.data.domains.shortcuts.ShortcutRepository
import ch.rmy.android.http_shortcuts.data.enums.ShortcutTriggerType
import ch.rmy.android.http_shortcuts.data.settings.DeviceLocalPreferences
import ch.rmy.android.http_shortcuts.utils.NetworkUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltWorker
class NetworkChangeWorker
@AssistedInject
constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val networkUtil: NetworkUtil,
    private val deviceLocalPreferences: DeviceLocalPreferences,
    private val shortcutRepository: ShortcutRepository,
    private val executionStarter: ExecutionStarter,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        tryOrLog {
            val currentFingerprint = networkUtil.getNetworkFingerprint() ?: return Result.success()
            val lastFingerprint = deviceLocalPreferences.lastNetworkChangeState
            deviceLocalPreferences.lastNetworkChangeState = currentFingerprint

            if (lastFingerprint == null || lastFingerprint == currentFingerprint) {
                return Result.success()
            }

            logInfo("Network state changed, triggering shortcuts")
            shortcutRepository.getTriggerShortcutsThatTriggerOnNetworkChange()
                .forEach { shortcut ->
                    executionStarter.execute(shortcut.id, ShortcutTriggerType.NETWORK_CHANGE)
                }
        }
        return Result.success()
    }

    class Starter
    @Inject
    constructor(
        private val context: Context,
    ) {
        fun scheduleCheck() {
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    CHECK_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<NetworkChangeWorker>()
                        .setInitialDelay(DEBOUNCE_MILLIS, TimeUnit.MILLISECONDS)
                        .runIf(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        }
                        .build(),
                )
        }

        fun scheduleRepeating() {
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    RECONCILIATION_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    PeriodicWorkRequestBuilder<NetworkChangeWorker>(RECONCILIATION_INTERVAL_MINUTES, TimeUnit.MINUTES)
                        .build(),
                )
        }
    }

    companion object {
        private const val CHECK_WORK_NAME = "network_change_check"
        private const val RECONCILIATION_WORK_NAME = "network_change_reconciliation"
        private const val RECONCILIATION_INTERVAL_MINUTES = 15L

        private val DEBOUNCE_MILLIS = 5.seconds.inWholeMilliseconds
    }
}
