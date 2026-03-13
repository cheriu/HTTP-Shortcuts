package ch.rmy.android.http_shortcuts.activities.cloudflare_config

import android.app.Application
import ch.rmy.android.framework.viewmodel.BaseViewModel
import ch.rmy.android.http_shortcuts.data.enums.IpVersion
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class CloudflareConfigViewModel
@Inject
constructor(
    application: Application,
) : BaseViewModel<Unit, CloudflareConfigViewState>(application) {

    private val _zoneId = MutableStateFlow("")
    val zoneId = _zoneId.asStateFlow()

    private val _dnsRecordId = MutableStateFlow("")
    val dnsRecordId = _dnsRecordId.asStateFlow()

    private val _apiToken = MutableStateFlow("")
    val apiToken = _apiToken.asStateFlow()

    private val _domainName = MutableStateFlow("")
    val domainName = _domainName.asStateFlow()

    private val _ttl = MutableStateFlow("3600")
    val ttl = _ttl.asStateFlow()

    private val _ipVersion = MutableStateFlow(IpVersion.V4)
    val ipVersion = _ipVersion.asStateFlow()

    private val _comment = MutableStateFlow("")
    val comment = _comment.asStateFlow()

    private val _proxied = MutableStateFlow(false)
    val proxied = _proxied.asStateFlow()

    private val _privateRouting = MutableStateFlow(false)
    val privateRouting = _privateRouting.asStateFlow()

    override suspend fun initialize(data: Unit) = CloudflareConfigViewState()

    fun onZoneIdChanged(zoneId: String) = runAction {
        _zoneId.value = zoneId
        updateSubmitButtonState()
    }

    fun onDnsRecordIdChanged(dnsRecordId: String) = runAction {
        _dnsRecordId.value = dnsRecordId
        updateSubmitButtonState()
    }

    fun onApiTokenChanged(apiToken: String) = runAction {
        _apiToken.value = apiToken
        updateSubmitButtonState()
    }

    fun onDomainNameChanged(domainName: String) = runAction {
        _domainName.value = domainName
    }

    fun onTtlChanged(ttl: String) = runAction {
        _ttl.value = ttl
    }

    fun onIpVersionChanged(ipVersion: IpVersion) = runAction {
        _ipVersion.value = ipVersion
    }

    fun onCommentChanged(comment: String) = runAction {
        _comment.value = comment
    }

    fun onProxiedChanged(proxied: Boolean) = runAction {
        _proxied.value = proxied
    }

    fun onPrivateRoutingChanged(privateRouting: Boolean) = runAction {
        _privateRouting.value = privateRouting
    }

    fun onSubmitButtonClicked() = runAction {
        closeScreen(
            result = CloudflareConfig(
                zoneId = zoneId.value,
                dnsRecordId = dnsRecordId.value,
                apiToken = apiToken.value,
                domainName = domainName.value,
                ttl = ttl.value,
                ipVersion = ipVersion.value,
                comment = comment.value,
                proxied = proxied.value,
                privateRouting = privateRouting.value,
            ),
        )
    }

    private suspend fun updateSubmitButtonState() {
        updateViewState {
            copy(
                submitButtonEnabled = zoneId.value.isNotEmpty() && dnsRecordId.value.isNotEmpty() && apiToken.value.isNotEmpty(),
            )
        }
    }
}
