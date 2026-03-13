package ch.rmy.android.http_shortcuts.activities.cloudflare_config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.components.Spacing
import ch.rmy.android.http_shortcuts.data.enums.IpVersion

@Composable
fun CloudflareConfigContent(
    zoneId: String,
    dnsRecordId: String,
    apiToken: String,
    domainName: String,
    ttl: String,
    ipVersion: IpVersion,
    comment: String,
    proxied: Boolean,
    privateRouting: Boolean,
    onZoneIdChanged: (String) -> Unit,
    onDnsRecordIdChanged: (String) -> Unit,
    onApiTokenChanged: (String) -> Unit,
    onDomainNameChanged: (String) -> Unit,
    onTtlChanged: (String) -> Unit,
    onIpVersionChanged: (IpVersion) -> Unit,
    onCommentChanged: (String) -> Unit,
    onProxiedChanged: (Boolean) -> Unit,
    onPrivateRoutingChanged: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(Spacing.MEDIUM)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Spacing.MEDIUM),
    ) {
        TextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.label_cloudflare_zone_id)) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
            ),
            value = zoneId,
            onValueChange = onZoneIdChanged,
            singleLine = true,
        )

        TextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.label_cloudflare_dns_record_id)) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
            ),
            value = dnsRecordId,
            onValueChange = onDnsRecordIdChanged,
            singleLine = true,
        )

        TextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.label_cloudflare_api_token)) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
            ),
            value = apiToken,
            onValueChange = onApiTokenChanged,
            singleLine = true,
        )

        TextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.label_cloudflare_domain_name)) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
            ),
            value = domainName,
            onValueChange = onDomainNameChanged,
            singleLine = true,
        )

        TextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.label_cloudflare_ttl)) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Number,
            ),
            value = ttl,
            onValueChange = onTtlChanged,
            singleLine = true,
        )

        Text(stringResource(R.string.label_cloudflare_ip_version))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row {
                RadioButton(
                    selected = ipVersion == IpVersion.V4,
                    onClick = {
                        onIpVersionChanged(IpVersion.V4)
                    },
                )
                Text(
                    modifier = Modifier.padding(top = Spacing.SMALL),
                    text = stringResource(R.string.label_cloudflare_ipv4),
                )
            }
            Row {
                RadioButton(
                    selected = ipVersion == IpVersion.V6,
                    onClick = {
                        onIpVersionChanged(IpVersion.V6)
                    },
                )
                Text(
                    modifier = Modifier.padding(top = Spacing.SMALL),
                    text = stringResource(R.string.label_cloudflare_ipv6),
                )
            }
        }

        TextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.label_cloudflare_comment)) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                autoCorrectEnabled = true,
            ),
            value = comment,
            onValueChange = onCommentChanged,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.label_cloudflare_proxied),
            )
            Switch(
                checked = proxied,
                onCheckedChange = onProxiedChanged,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.label_cloudflare_private_routing),
            )
            Switch(
                checked = privateRouting,
                onCheckedChange = onPrivateRoutingChanged,
            )
        }
    }
}
