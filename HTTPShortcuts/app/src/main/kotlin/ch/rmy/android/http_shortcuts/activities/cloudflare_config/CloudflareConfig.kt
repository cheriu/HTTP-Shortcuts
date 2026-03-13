package ch.rmy.android.http_shortcuts.activities.cloudflare_config

import ch.rmy.android.http_shortcuts.data.enums.IpVersion
import java.io.Serializable

data class CloudflareConfig(
    val zoneId: String,
    val dnsRecordId: String,
    val apiToken: String,
    val domainName: String,
    val ttl: String,
    val ipVersion: IpVersion,
    val comment: String,
    val proxied: Boolean,
    val privateRouting: Boolean,
) : Serializable
