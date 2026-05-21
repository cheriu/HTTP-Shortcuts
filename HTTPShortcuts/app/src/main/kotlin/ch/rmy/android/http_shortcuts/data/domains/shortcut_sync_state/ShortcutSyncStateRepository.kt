package ch.rmy.android.http_shortcuts.data.domains.shortcut_sync_state

import ch.rmy.android.http_shortcuts.data.Database
import ch.rmy.android.http_shortcuts.data.domains.BaseRepository
import ch.rmy.android.http_shortcuts.data.domains.shortcuts.ShortcutId
import ch.rmy.android.http_shortcuts.data.models.ShortcutSyncState
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ShortcutSyncStateRepository
@Inject
constructor(
    database: Database,
) : BaseRepository(database) {
    fun observeAll(): Flow<List<ShortcutSyncState>> = queryFlow {
        shortcutSyncStateDao().observeAll()
    }

    suspend fun getByShortcutId(shortcutId: ShortcutId): ShortcutSyncState? = query {
        shortcutSyncStateDao().getByShortcutId(shortcutId)
    }

    suspend fun updateLastSentIp(
        shortcutId: ShortcutId,
        lastSentIpv4: String? = null,
        lastSentIpv6: String? = null,
    ) = query {
        val existingState = shortcutSyncStateDao().getByShortcutId(shortcutId)
        shortcutSyncStateDao().insertOrUpdate(
            ShortcutSyncState(
                shortcutId = shortcutId,
                lastSentIpv4 = lastSentIpv4 ?: existingState?.lastSentIpv4,
                lastSentIpv6 = lastSentIpv6 ?: existingState?.lastSentIpv6,
                lastSuccessAt = Instant.now(),
            ),
        )
    }
}
