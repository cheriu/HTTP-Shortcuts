package ch.rmy.android.http_shortcuts.activities.cloudflare_config

import androidx.compose.runtime.Stable

@Stable
data class CloudflareConfigViewState(
    val submitButtonEnabled: Boolean = false,
)
