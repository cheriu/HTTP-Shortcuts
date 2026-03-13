package ch.rmy.android.http_shortcuts.activities.cloudflare_config

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.components.BackButton
import ch.rmy.android.http_shortcuts.components.SimpleScaffold
import ch.rmy.android.http_shortcuts.components.ToolbarIcon
import ch.rmy.android.http_shortcuts.components.bindViewModel

@Composable
fun CloudflareConfigScreen() {
    val (viewModel, state) = bindViewModel<CloudflareConfigViewState, CloudflareConfigViewModel>()

    val zoneId by viewModel.zoneId.collectAsStateWithLifecycle()
    val dnsRecordId by viewModel.dnsRecordId.collectAsStateWithLifecycle()
    val apiToken by viewModel.apiToken.collectAsStateWithLifecycle()
    val domainName by viewModel.domainName.collectAsStateWithLifecycle()
    val ttl by viewModel.ttl.collectAsStateWithLifecycle()
    val ipVersion by viewModel.ipVersion.collectAsStateWithLifecycle()
    val comment by viewModel.comment.collectAsStateWithLifecycle()
    val proxied by viewModel.proxied.collectAsStateWithLifecycle()
    val privateRouting by viewModel.privateRouting.collectAsStateWithLifecycle()

    SimpleScaffold(
        viewState = state,
        title = stringResource(R.string.title_cloudflare_config),
        backButton = BackButton.CROSS,
        actions = { viewState ->
            ToolbarIcon(
                painterResource(R.drawable.outline_check_24),
                contentDescription = stringResource(R.string.save_button),
                enabled = viewState.submitButtonEnabled,
                onClick = viewModel::onSubmitButtonClicked,
            )
        },
    ) {
        CloudflareConfigContent(
            zoneId = zoneId,
            dnsRecordId = dnsRecordId,
            apiToken = apiToken,
            domainName = domainName,
            ttl = ttl,
            ipVersion = ipVersion,
            comment = comment,
            proxied = proxied,
            privateRouting = privateRouting,
            onZoneIdChanged = viewModel::onZoneIdChanged,
            onDnsRecordIdChanged = viewModel::onDnsRecordIdChanged,
            onApiTokenChanged = viewModel::onApiTokenChanged,
            onDomainNameChanged = viewModel::onDomainNameChanged,
            onTtlChanged = viewModel::onTtlChanged,
            onIpVersionChanged = viewModel::onIpVersionChanged,
            onCommentChanged = viewModel::onCommentChanged,
            onProxiedChanged = viewModel::onProxiedChanged,
            onPrivateRoutingChanged = viewModel::onPrivateRoutingChanged,
        )
    }
}
