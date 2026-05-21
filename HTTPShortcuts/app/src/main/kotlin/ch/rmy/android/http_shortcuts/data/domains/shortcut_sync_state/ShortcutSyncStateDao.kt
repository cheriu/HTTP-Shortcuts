package ch.rmy.android.http_shortcuts.data.domains.shortcut_sync_state

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ch.rmy.android.http_shortcuts.data.domains.shortcuts.ShortcutId
import ch.rmy.android.http_shortcuts.data.models.ShortcutSyncState
import kotlinx.coroutines.flow.Flow

@Dao
interface ShortcutSyncStateDao {
    @Query("SELECT * FROM shortcut_sync_state")
    fun observeAll(): Flow<List<ShortcutSyncState>>

    @Query("SELECT * FROM shortcut_sync_state WHERE shortcut_id = :shortcutId")
    suspend fun getByShortcutId(shortcutId: ShortcutId): ShortcutSyncState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(shortcutSyncState: ShortcutSyncState)

    @Query("DELETE FROM shortcut_sync_state WHERE shortcut_id = :shortcutId")
    suspend fun deleteByShortcutId(shortcutId: ShortcutId)

    @Query("DELETE FROM shortcut_sync_state WHERE shortcut_id IN (:shortcutIds)")
    suspend fun deleteByShortcutIds(shortcutIds: List<ShortcutId>)

    @Query("DELETE FROM shortcut_sync_state")
    suspend fun deleteAll()
}
