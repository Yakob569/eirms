package com.eirspay.domain.usecase

import com.eirspay.domain.model.*
import com.eirspay.domain.repository.MerchantRepository
import com.eirspay.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * The rules shared by every settings page: what a branch allows caps its source systems and API
 * keys, EXCISE is derived from the merchant's excise status, and deletes are blocked while
 * something still hangs off the item.
 */
object SettingsRules {
    private fun plural(n: Int, word: String) = "$n $word" + if (n == 1) "" else "s"

    /** A source system's tax types with EXCISE merged in when the merchant has excise enabled. */
    fun taxTypes(system: SourceSystemProfile, exciseEnabled: Boolean): List<String> =
        if (exciseEnabled) (system.taxTypes + SettingsCatalog.EXCISE).distinct() else system.taxTypes

    /** Read-only tax eligibility of a branch: the union of its source systems' tax types. */
    fun eligibleTaxTypes(branchId: String, settings: MerchantSettings): List<String> =
        settings.sourceSystemsOf(branchId).flatMap { taxTypes(it, settings.exciseEnabled) }.distinct()

    /** What a key bound to [system] may be granted: the branch's operations and transaction types, the system's tax types. */
    data class KeyCeiling(
        val branch: MerchantBranch?,
        val system: SourceSystemProfile?,
        val operations: List<String>,
        val transactionTypes: List<String>,
        val taxTypes: List<String>,
    )

    fun keyCeiling(sourceSystemId: String, settings: MerchantSettings): KeyCeiling {
        val system = settings.sourceSystem(sourceSystemId)
        val branch = system?.let { settings.branch(it.branchId) }
        return KeyCeiling(
            branch = branch,
            system = system,
            operations = branch?.operations.orEmpty(),
            transactionTypes = branch?.transactionTypes.orEmpty(),
            taxTypes = system?.let { taxTypes(it, settings.exciseEnabled) }.orEmpty(),
        )
    }

    /** Drops grants that the ceiling no longer allows (e.g. after rebinding a new key). */
    fun clamp(draft: ApiKeyDraft, ceiling: KeyCeiling): ApiKeyDraft = draft.copy(
        operations = draft.operations.filter { it in ceiling.operations },
        transactionTypes = draft.transactionTypes.filter { it in ceiling.transactionTypes },
        taxTypes = draft.taxTypes.filter { it in ceiling.taxTypes },
    )

    /** Why a branch can't be deleted, or null when it can. */
    fun branchDeleteBlocker(branch: MerchantBranch, settings: MerchantSettings): String? {
        val systems = settings.sourceSystemsOf(branch.id).size
        return when {
            branch.isMain -> "The main branch can’t be deleted."
            systems > 0 -> "Move or delete its ${plural(systems, "source system")} first."
            else -> null
        }
    }

    /** Why a source system can't be deleted, or null when it can. */
    fun sourceSystemDeleteBlocker(system: SourceSystemProfile, settings: MerchantSettings): String? {
        val keys = settings.keysOf(system.id).size
        return if (keys > 0) "Remove its ${plural(keys, "API key")} first." else null
    }

    /** Inactive branches can't take source systems (the current branch always stays selectable). */
    fun canHostSourceSystem(branch: MerchantBranch, system: SourceSystemProfile): Boolean =
        branch.active || branch.id == system.branchId

    /** New keys can only bind to active source systems; existing keys stay on theirs. */
    fun canBindKey(system: SourceSystemProfile, isNew: Boolean, currentId: String): Boolean =
        if (isNew) system.active else system.id == currentId

    /** The MoR probe needs credentials, seller profile and a source system first (steps 1–3). */
    fun canRunSetupVerification(stepsDone: Int, verified: Boolean): Boolean = stepsDone >= 3 || verified

    fun isSetupVerified(stepsDone: Int, verified: Boolean): Boolean = verified || stepsDone >= 5
}

class ObserveSettingsUseCase @Inject constructor(private val settings: SettingsRepository) {
    operator fun invoke(): Flow<MerchantSettings> = settings.settings
}

/** Verify setup: the probe state combined with the readiness steps. */
data class SetupVerification(val stepsDone: Int, val verified: Boolean) {
    val canRun: Boolean get() = SettingsRules.canRunSetupVerification(stepsDone, verified)
    val isVerified: Boolean get() = SettingsRules.isSetupVerified(stepsDone, verified)
}

class ObserveSetupVerificationUseCase @Inject constructor(
    private val settings: SettingsRepository,
    private val merchant: MerchantRepository,
) {
    operator fun invoke(): Flow<SetupVerification> =
        combine(settings.settings, merchant.setupStepsDone) { s, done -> SetupVerification(done.coerceIn(0, 5), s.setupVerified) }
}

/** Sends the signed probe invoice to MoR. Returns false when the prerequisites aren't met. */
class RunSetupVerificationUseCase @Inject constructor(private val settings: SettingsRepository) {
    suspend operator fun invoke(state: SetupVerification): Boolean {
        if (!state.canRun) return false
        return settings.verifySetup()
    }
}

/** Creates a key, clamped to what its source system and branch allow. */
class CreateApiKeyUseCase @Inject constructor(private val settings: SettingsRepository) {
    suspend operator fun invoke(draft: ApiKeyDraft, current: MerchantSettings): CreatedApiKey {
        val ceiling = SettingsRules.keyCeiling(draft.sourceSystemId, current)
        require(ceiling.system?.active == true) { "API keys can only be bound to an active source system." }
        val clamped = SettingsRules.clamp(draft, ceiling)
        return settings.createApiKey(clamped.copy(label = clamped.label.trim().ifBlank { "Untitled key" }))
    }
}
