package ch.rmy.android.http_shortcuts.scheduling

import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ch.rmy.android.framework.extensions.runIf
import ch.rmy.android.framework.extensions.tryOrLog
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltWorker
class IpChangeReconciliationWorker
@AssistedInject
constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val scheduler: IpChangeAutoUpdateScheduler,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        tryOrLog {
            scheduler.scheduleEligibleShortcuts()
        }
        return Result.success()
    }

    class Starter
    @Inject
    constructor(
        private val context: Context,
    ) {
        fun scheduleRepeating() {
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    PERIODIC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    PeriodicWorkRequestBuilder<IpChangeReconciliationWorker>(15, TimeUnit.MINUTES)
                        .setConstraints(networkConstraints())
                        .build(),
                )
        }

        fun scheduleNow() {
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    IMMEDIATE_WORK_NAME,
                    ExistingWorkPolicy.KEEP,
                    OneTimeWorkRequestBuilder<IpChangeReconciliationWorker>()
                        .runIf(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        }
                        .setConstraints(networkConstraints())
                        .build(),
                )
        }

        private fun networkConstraints() =
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "ip_change_reconciliation_periodic"
        private const val IMMEDIATE_WORK_NAME = "ip_change_reconciliation_immediate"
    }
}
