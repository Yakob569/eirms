package com.eirspay.data.fake

import com.eirspay.data.seed.SettingsSampleData
import com.eirspay.domain.model.*
import com.eirspay.domain.repository.SettingsRepository
import com.eirspay.domain.usecase.SettingsRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.security.SecureRandom
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** In-memory merchant settings. Verifications simulate the MoR round-trip with [FakeLatency]. */
@Singleton
class FakeSettingsRepository @Inject constructor() : SettingsRepository {
    private val state = MutableStateFlow(SettingsSampleData.settings())
    override val settings: StateFlow<MerchantSettings> = state.asStateFlow()

    private var counter = 0
    private val random = SecureRandom()
    private val day = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

    private fun nextId(prefix: String) = prefix + (System.currentTimeMillis().toString(36) + (++counter)).uppercase()

    override suspend fun saveCredentials(credentials: MorCredentials) = state.update { it.copy(credentials = credentials) }

    override suspend fun saveSeller(profile: SellerProfile) = state.update { it.copy(seller = profile) }

    override suspend fun addBranch(): MerchantBranch {
        val branch = MerchantBranch(
            id = nextId("b"), name = "New branch", code = "BR-00${state.value.branches.size + 1}", isMain = false, active = true,
            manager = null, address = BranchAddress(), transactionTypes = listOf("B2C"), documentTypes = listOf("INV", "RCT"),
            natures = listOf("GOODS"), operations = listOf("Single Register", "Receipt Issue"),
            permissions = listOf("Issue invoices", "Issue receipts"),
        )
        state.update { it.copy(branches = it.branches + branch) }
        return branch
    }

    override suspend fun saveBranch(branch: MerchantBranch) = state.update { s ->
        // The main branch is always active.
        val b = if (branch.isMain) branch.copy(active = true) else branch
        s.copy(branches = s.branches.map { if (it.id == b.id) b else it })
    }

    override suspend fun deleteBranch(id: String) = state.update { s ->
        val branch = s.branch(id) ?: return@update s
        check(SettingsRules.branchDeleteBlocker(branch, s) == null) { "Branch $id can't be deleted." }
        s.copy(branches = s.branches.filter { it.id != id })
    }

    override suspend fun addSourceSystem(branchId: String): SourceSystemProfile {
        val system = SourceSystemProfile(
            id = nextId("s"), label = "New source system", type = "POS", branchId = branchId, active = true,
            verification = VerificationStatus.UNVERIFIED, verificationNote = "Not verified yet", taxTypes = listOf("VAT15"),
            cashier = "", salesPerson = "", systemId = "SS-pending", lastDocument = "—",
        )
        state.update { it.copy(sourceSystems = it.sourceSystems + system) }
        return system
    }

    override suspend fun saveSourceSystem(system: SourceSystemProfile) = state.update { s ->
        // EXCISE is derived from the merchant, never stored on a source system.
        val clean = system.copy(taxTypes = system.taxTypes.filter { it != SettingsCatalog.EXCISE })
        s.copy(sourceSystems = s.sourceSystems.map { if (it.id == clean.id) clean else it })
    }

    override suspend fun deleteSourceSystem(id: String) = state.update { s ->
        val system = s.sourceSystem(id) ?: return@update s
        check(SettingsRules.sourceSystemDeleteBlocker(system, s) == null) { "Source system $id has API keys." }
        s.copy(sourceSystems = s.sourceSystems.filter { it.id != id })
    }

    override suspend fun verifySourceSystem(id: String, hsn: String): SourceSystemProfile {
        FakeLatency.wait()
        val code = hsn.trim().ifBlank { "8471" }
        state.update { s ->
            s.copy(sourceSystems = s.sourceSystems.map {
                if (it.id == id) it.copy(verification = VerificationStatus.VERIFIED, verificationNote = "Verified just now · HSN $code") else it
            })
        }
        return state.value.sourceSystem(id) ?: error("Unknown source system $id")
    }

    override suspend fun setExciseEnabled(enabled: Boolean) = state.update { it.copy(exciseEnabled = enabled) }

    override suspend fun createApiKey(draft: ApiKeyDraft): CreatedApiKey {
        val secret = "eirs_live_" + (1..32).joinToString("") { "0123456789abcdef"[random.nextInt(16)].toString() }
        val key = ApiKey(
            id = nextId("k"), label = draft.label.ifBlank { "Untitled key" }, sourceSystemId = draft.sourceSystemId, active = true,
            created = day.format(LocalDate.now()), operations = draft.operations, transactionTypes = draft.transactionTypes,
            taxTypes = draft.taxTypes, tail = secret.takeLast(4), users = draft.users,
        )
        state.update { it.copy(apiKeys = listOf(key) + it.apiKeys) }
        return CreatedApiKey(key, secret)
    }

    override suspend fun saveApiKey(key: ApiKey) = state.update { s ->
        // A key's binding and secret never change after creation.
        s.copy(apiKeys = s.apiKeys.map { if (it.id == key.id) key.copy(sourceSystemId = it.sourceSystemId, tail = it.tail) else it })
    }

    override suspend fun revokeApiKey(id: String) = state.update { s ->
        s.copy(apiKeys = s.apiKeys.map { if (it.id == id) it.copy(active = false) else it })
    }

    override suspend fun changePassword(current: String, newPassword: String) {
        require(current.isNotBlank() && newPassword.length >= 8) { "Password too short." }
        state.update { it.copy(passwordNote = "Changed just now") }
    }

    override suspend fun signOutDevice(id: String) = state.update { s -> s.copy(devices = s.devices.filter { it.id != id || it.current }) }

    override suspend fun signOutOtherDevices() = state.update { s -> s.copy(devices = s.devices.filter { it.current }) }

    override suspend fun verifySetup(): Boolean {
        FakeLatency.wait()
        state.update { it.copy(setupVerified = true) }
        return true
    }
}
