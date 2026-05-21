package ch.rmy.android.http_shortcuts.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ch.rmy.android.http_shortcuts.data.domains.shortcuts.ShortcutId
import java.time.Instant

@Entity(tableName = "shortcut_sync_state")
data class ShortcutSyncState(
    @PrimaryKey
    @ColumnInfo(name = "shortcut_id")
    val shortcutId: ShortcutId,
    @ColumnInfo(name = "last_sent_ipv4")
    val lastSentIpv4: String? = null,
    @ColumnInfo(name = "last_sent_ipv6")
    val lastSentIpv6: String? = null,
    @ColumnInfo(name = "last_success_at")
    val lastSuccessAt: Instant? = null,
)
