package com.eirspay.mobile.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eirspay.domain.model.ApiKey
import com.eirspay.domain.model.ApiKeyDraft
import com.eirspay.domain.model.MerchantBranch
import com.eirspay.domain.model.MerchantSettings
import com.eirspay.domain.model.MorCredentials
import com.eirspay.domain.model.SellerProfile
import com.eirspay.domain.model.Session
import com.eirspay.domain.model.SourceSystemProfile
import com.eirspay.domain.repository.AuthRepository
import com.eirspay.domain.repository.SettingsRepository
import com.eirspay.domain.usecase.CreateApiKeyUseCase
import com.eirspay.domain.usecase.ObserveSetupVerificationUseCase
import com.eirspay.domain.usecase.RunSetupVerificationUseCase
import com.eirspay.domain.usecase.SettingsRules
import com.eirspay.domain.usecase.SetupVerification
import com.eirspay.mobile.ui.designsystem.LoaderMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ---------------------------------------------------------------------------------------------
// Account
// ---------------------------------------------------------------------------------------------

data class PasswordForm(val open: Boolean = false, val current: String = "", val new: String = "", val confirm: String = "") {
    val valid: Boolean get() = current.isNotBlank() && new.length >= 8 && new == confirm
}

data class AccountUiState(val session: Session?, val settings: MerchantSettings, val password: PasswordForm)

@HiltViewModel
class AccountViewModel @Inject constructor(
    auth: AuthRepository,
    private val repo: SettingsRepository,
) : ViewModel() {
    private val password = MutableStateFlow(PasswordForm())

    val state: StateFlow<AccountUiState> = combine(auth.session, repo.settings, password) { s, o, p -> AccountUiState(s, o, p) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AccountUiState(auth.session.value, repo.settings.value, PasswordForm()))

    fun togglePassword() = password.update { if (it.open) PasswordForm() else it.copy(open = true) }
    fun setCurrent(v: String) = password.update { it.copy(current = v) }
    fun setNew(v: String) = password.update { it.copy(new = v) }
    fun setConfirm(v: String) = password.update { it.copy(confirm = v) }

    fun savePassword() {
        val p = password.value
        if (!p.valid) return
        viewModelScope.launch {
            repo.changePassword(p.current, p.new)
            password.value = PasswordForm()
        }
    }

    fun signOut(deviceId: String) = viewModelScope.launch { repo.signOutDevice(deviceId) }
    fun signOutOthers() = viewModelScope.launch { repo.signOutOtherDevices() }
}

// ---------------------------------------------------------------------------------------------
// MoR credentials & seller profile (read-only until Edit)
// ---------------------------------------------------------------------------------------------

data class LegalUiState(
    val settings: MerchantSettings,
    val verification: SetupVerification,
    val editingCredentials: Boolean = false,
    val credentials: MorCredentials = settings.credentials,
    val showSecrets: Boolean = false,
    val editingSeller: Boolean = false,
    val seller: SellerProfile = settings.seller,
)

private data class LegalDrafts(
    val editingCredentials: Boolean = false,
    val credentials: MorCredentials? = null,
    val showSecrets: Boolean = false,
    val editingSeller: Boolean = false,
    val seller: SellerProfile? = null,
)

/** Backs the MoR credentials and Seller profile pages: they show saved values until Edit, then a draft. */
@HiltViewModel
class LegalSettingsViewModel @Inject constructor(
    private val repo: SettingsRepository,
    observeVerification: ObserveSetupVerificationUseCase,
) : ViewModel() {
    private val drafts = MutableStateFlow(LegalDrafts())

    val state: StateFlow<LegalUiState> = combine(repo.settings, observeVerification(), drafts) { o, v, d ->
        LegalUiState(
            settings = o, verification = v,
            editingCredentials = d.editingCredentials, credentials = d.credentials ?: o.credentials, showSecrets = d.showSecrets,
            editingSeller = d.editingSeller, seller = d.seller ?: o.seller,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LegalUiState(repo.settings.value, SetupVerification(0, repo.settings.value.setupVerified)))

    /** Edit → draft from the saved values; Save → persist and lock again. */
    fun toggleCredentialsEdit() {
        val d = drafts.value
        if (!d.editingCredentials) {
            drafts.update { it.copy(editingCredentials = true, credentials = repo.settings.value.credentials) }
        } else {
            val draft = d.credentials ?: return
            viewModelScope.launch {
                repo.saveCredentials(draft.copy(clientId = draft.clientId.trim(), tin = draft.tin.trim()))
                drafts.update { it.copy(editingCredentials = false, credentials = null) }
            }
        }
    }

    fun editCredentials(f: (MorCredentials) -> MorCredentials) = drafts.update {
        if (!it.editingCredentials || it.credentials == null) it else it.copy(credentials = f(it.credentials))
    }

    fun toggleSecrets() = drafts.update { it.copy(showSecrets = !it.showSecrets) }

    fun toggleSellerEdit() {
        val d = drafts.value
        if (!d.editingSeller) {
            drafts.update { it.copy(editingSeller = true, seller = repo.settings.value.seller) }
        } else {
            val draft = d.seller ?: return
            viewModelScope.launch {
                repo.saveSeller(draft)
                drafts.update { it.copy(editingSeller = false, seller = null) }
            }
        }
    }

    fun editSeller(f: (SellerProfile) -> SellerProfile) = drafts.update {
        if (!it.editingSeller || it.seller == null) it else it.copy(seller = f(it.seller))
    }
}

// ---------------------------------------------------------------------------------------------
// Verify setup
// ---------------------------------------------------------------------------------------------

data class VerifyUiState(val settings: MerchantSettings, val verification: SetupVerification, val loading: LoaderMessage? = null)

@HiltViewModel
class VerifySetupViewModel @Inject constructor(
    repo: SettingsRepository,
    observeVerification: ObserveSetupVerificationUseCase,
    private val runVerification: RunSetupVerificationUseCase,
) : ViewModel() {
    private val loading = MutableStateFlow<LoaderMessage?>(null)

    val state: StateFlow<VerifyUiState> = combine(repo.settings, observeVerification(), loading) { o, v, l -> VerifyUiState(o, v, l) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, VerifyUiState(repo.settings.value, SetupVerification(0, repo.settings.value.setupVerified)))

    fun run() {
        val v = state.value.verification
        if (!v.canRun || loading.value != null) return
        loading.value = LoaderMessage("Verifying with MoR", "Sending the signed probe invoice")
        viewModelScope.launch {
            try { runVerification(v) } finally { loading.value = null }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Lists (branches, source systems, API keys) — one VM; each list page gets its own instance.
// ---------------------------------------------------------------------------------------------

@HiltViewModel
class SettingsListViewModel @Inject constructor(private val repo: SettingsRepository) : ViewModel() {
    val settings: StateFlow<MerchantSettings> = repo.settings

    private val _created = Channel<String>(Channel.BUFFERED)
    /** Emits the id of a branch or source system just added from a "+" button. */
    val created: Flow<String> = _created.receiveAsFlow()

    fun addBranch() = viewModelScope.launch { _created.send(repo.addBranch().id) }

    /** "+" on the source systems list adds to the main branch. */
    fun addSourceSystem() = viewModelScope.launch {
        val main = repo.settings.value.branches.firstOrNull { it.isMain } ?: repo.settings.value.branches.first()
        _created.send(repo.addSourceSystem(main.id).id)
    }

    fun setExcise(enabled: Boolean) = viewModelScope.launch { repo.setExciseEnabled(enabled) }
}

// ---------------------------------------------------------------------------------------------
// Branch detail
// ---------------------------------------------------------------------------------------------

data class BranchDetailUiState(
    val settings: MerchantSettings,
    val draft: MerchantBranch?,
    val expandedSystem: String? = null,
) {
    val deleteBlocker: String? get() = draft?.let { SettingsRules.branchDeleteBlocker(it, settings) }
}

sealed interface DetailEvent {
    data object Closed : DetailEvent
    data class OpenSourceSystem(val id: String) : DetailEvent
}

@HiltViewModel
class BranchDetailViewModel @Inject constructor(private val repo: SettingsRepository) : ViewModel() {
    private val draft = MutableStateFlow<MerchantBranch?>(null)
    private val expanded = MutableStateFlow<String?>(null)
    private var boundId: String? = null

    val state: StateFlow<BranchDetailUiState> = combine(repo.settings, draft, expanded) { o, d, e -> BranchDetailUiState(o, d, e) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, BranchDetailUiState(repo.settings.value, null))

    private val _events = Channel<DetailEvent>(Channel.BUFFERED)
    val events: Flow<DetailEvent> = _events.receiveAsFlow()

    fun bind(id: String) {
        if (boundId == id) return
        boundId = id
        draft.value = repo.settings.value.branch(id)
    }

    fun edit(f: (MerchantBranch) -> MerchantBranch) = draft.update { it?.let(f) }
    fun toggleExpanded(systemId: String) = expanded.update { if (it == systemId) null else systemId }

    fun save() {
        val d = draft.value ?: return
        viewModelScope.launch { repo.saveBranch(d); _events.send(DetailEvent.Closed) }
    }

    fun delete() {
        val d = draft.value ?: return
        if (state.value.deleteBlocker != null) return
        viewModelScope.launch { repo.deleteBranch(d.id); _events.send(DetailEvent.Closed) }
    }

    /** Saves pending edits, then adds a source system under this branch and opens it. */
    fun addSourceSystem() {
        val d = draft.value ?: return
        viewModelScope.launch {
            repo.saveBranch(d)
            _events.send(DetailEvent.OpenSourceSystem(repo.addSourceSystem(d.id).id))
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Source system detail
// ---------------------------------------------------------------------------------------------

data class SourceSystemDetailUiState(
    val settings: MerchantSettings,
    val draft: SourceSystemProfile?,
    val hsn: String = "",
    val loading: LoaderMessage? = null,
) {
    val deleteBlocker: String? get() = draft?.let { SettingsRules.sourceSystemDeleteBlocker(it, settings) }
}

@HiltViewModel
class SourceSystemDetailViewModel @Inject constructor(private val repo: SettingsRepository) : ViewModel() {
    private val draft = MutableStateFlow<SourceSystemProfile?>(null)
    private val hsn = MutableStateFlow("")
    private val loading = MutableStateFlow<LoaderMessage?>(null)
    private var boundId: String? = null

    val state: StateFlow<SourceSystemDetailUiState> =
        combine(repo.settings, draft, hsn, loading) { o, d, h, l -> SourceSystemDetailUiState(o, d, h, l) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, SourceSystemDetailUiState(repo.settings.value, null))

    private val _closed = Channel<Unit>(Channel.BUFFERED)
    val closed: Flow<Unit> = _closed.receiveAsFlow()

    fun bind(id: String) {
        if (boundId == id) return
        boundId = id
        draft.value = repo.settings.value.sourceSystem(id)
    }

    fun edit(f: (SourceSystemProfile) -> SourceSystemProfile) = draft.update { it?.let(f) }
    fun setHsn(v: String) { hsn.value = v.filter { it.isDigit() || it == '.' }.take(10) }

    fun verify() {
        val d = draft.value ?: return
        if (loading.value != null) return
        loading.value = LoaderMessage("Verifying with MoR", "Sending a probe for " + d.label)
        viewModelScope.launch {
            try {
                val verified = repo.verifySourceSystem(d.id, hsn.value)
                draft.update { it?.copy(verification = verified.verification, verificationNote = verified.verificationNote) }
            } finally {
                loading.value = null
            }
        }
    }

    fun save() {
        val d = draft.value ?: return
        viewModelScope.launch { repo.saveSourceSystem(d); _closed.send(Unit) }
    }

    fun delete() {
        val d = draft.value ?: return
        if (state.value.deleteBlocker != null) return
        viewModelScope.launch { repo.deleteSourceSystem(d.id); _closed.send(Unit) }
    }
}

// ---------------------------------------------------------------------------------------------
// API key detail / create
// ---------------------------------------------------------------------------------------------

data class ApiKeyDetailUiState(
    val settings: MerchantSettings,
    /** Create mode: no key yet, editing [draft]. */
    val isNew: Boolean,
    val draft: ApiKeyDraft,
    val key: ApiKey?,
    /** The full secret, only right after creation and only until the page is left. */
    val revealedSecret: String? = null,
    val assignOpen: Boolean = false,
) {
    val sourceSystemId: String get() = key?.sourceSystemId ?: draft.sourceSystemId
    val ceiling: SettingsRules.KeyCeiling get() = SettingsRules.keyCeiling(sourceSystemId, settings)
    val label: String get() = key?.label ?: draft.label
    val operations: List<String> get() = key?.operations ?: draft.operations
    val transactionTypes: List<String> get() = key?.transactionTypes ?: draft.transactionTypes
    val taxTypes: List<String> get() = key?.taxTypes ?: draft.taxTypes
    val users: List<String> get() = key?.users ?: draft.users
}

private data class KeyLocal(
    val keyId: String? = null,
    val draft: ApiKeyDraft = ApiKeyDraft(
        label = "", sourceSystemId = "s1", operations = listOf("Single Register", "Receipt Issue"),
        transactionTypes = listOf("B2C"), taxTypes = listOf("VAT15"), users = emptyList(),
    ),
    val secret: String? = null,
    val assignOpen: Boolean = false,
)

/**
 * Create mode edits a local draft; an existing key's changes apply immediately (it has no Save,
 * only Revoke). After Create the page switches to the new key and reveals its secret once.
 */
@HiltViewModel
class ApiKeyDetailViewModel @Inject constructor(
    private val repo: SettingsRepository,
    private val createKey: CreateApiKeyUseCase,
) : ViewModel() {
    private val local = MutableStateFlow(KeyLocal())
    private var bound = false

    val state: StateFlow<ApiKeyDetailUiState> = combine(repo.settings, local) { o, l ->
        val key = l.keyId?.let { o.apiKey(it) }
        ApiKeyDetailUiState(o, isNew = l.keyId == null, draft = l.draft, key = key, revealedSecret = l.secret, assignOpen = l.assignOpen)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ApiKeyDetailUiState(repo.settings.value, true, KeyLocal().draft, null))

    private val _copied = MutableStateFlow(false)
    val copied: StateFlow<Boolean> = _copied.asStateFlow()

    /** [id] null opens the create form, bound to the first active source system. */
    fun bind(id: String?) {
        if (bound) return
        bound = true
        if (id != null) {
            local.update { it.copy(keyId = id) }
        } else {
            val first = repo.settings.value.sourceSystems.firstOrNull { it.active }?.id ?: "s1"
            local.update { it.copy(draft = SettingsRules.clamp(it.draft.copy(sourceSystemId = first), SettingsRules.keyCeiling(first, repo.settings.value))) }
        }
    }

    private fun editKey(f: (ApiKey) -> ApiKey) {
        val key = state.value.key ?: return
        viewModelScope.launch { repo.saveApiKey(f(key)) }
    }

    private fun toggle(list: List<String>, v: String) = if (v in list) list - v else list + v

    fun setLabel(v: String) =
        if (state.value.isNew) local.update { it.copy(draft = it.draft.copy(label = v)) } else editKey { it.copy(label = v) }

    /** Only while creating: a key stays bound to the source system it was created for. */
    fun bindTo(sourceSystemId: String) {
        if (!state.value.isNew) return
        local.update {
            val moved = it.draft.copy(sourceSystemId = sourceSystemId)
            it.copy(draft = SettingsRules.clamp(moved, SettingsRules.keyCeiling(sourceSystemId, repo.settings.value)))
        }
    }

    fun toggleOperation(v: String) = if (state.value.isNew) local.update { it.copy(draft = it.draft.copy(operations = toggle(it.draft.operations, v))) }
    else editKey { it.copy(operations = toggle(it.operations, v)) }

    fun toggleTransactionType(v: String) = if (state.value.isNew) local.update { it.copy(draft = it.draft.copy(transactionTypes = toggle(it.draft.transactionTypes, v))) }
    else editKey { it.copy(transactionTypes = toggle(it.transactionTypes, v)) }

    fun toggleTaxType(v: String) = if (state.value.isNew) local.update { it.copy(draft = it.draft.copy(taxTypes = toggle(it.draft.taxTypes, v))) }
    else editKey { it.copy(taxTypes = toggle(it.taxTypes, v)) }

    fun toggleAssign() = local.update { it.copy(assignOpen = !it.assignOpen) }

    fun assign(user: String) = if (state.value.isNew) local.update { it.copy(draft = it.draft.copy(users = it.draft.users + user)) }
    else editKey { it.copy(users = it.users + user) }

    fun unassign(user: String) = if (state.value.isNew) local.update { it.copy(draft = it.draft.copy(users = it.draft.users - user)) }
    else editKey { it.copy(users = it.users - user) }

    fun create() {
        if (!state.value.isNew) return
        viewModelScope.launch {
            val created = createKey(local.value.draft, repo.settings.value)
            _copied.value = false
            local.update { it.copy(keyId = created.key.id, secret = created.secret, assignOpen = false) }
        }
    }

    fun markCopied() { _copied.value = true }

    fun revoke() {
        val key = state.value.key ?: return
        viewModelScope.launch { repo.revokeApiKey(key.id) }
    }
}
