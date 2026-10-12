package com.eirspay.mobile.feature.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eirspay.domain.model.MerchantSettings
import com.eirspay.domain.model.Session
import com.eirspay.domain.repository.AuthRepository
import com.eirspay.domain.repository.CatalogRepository
import com.eirspay.domain.repository.MerchantRepository
import com.eirspay.domain.repository.SettingsRepository
import com.eirspay.domain.usecase.SettingsRules
import com.eirspay.domain.repository.WithholdingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class MoreUiState(
    val activeItems: Int = 0,
    val awaitingCertificates: Int = 0,
    /** Completed fiscal-readiness steps, 0..5. */
    val setupDone: Int = 0,
    val branchCount: Int = 0,
    val session: Session? = null,
    val settings: MerchantSettings? = null,
) {
    val setupVerified: Boolean get() = SettingsRules.isSetupVerified(setupDone, settings?.setupVerified == true)
}

@HiltViewModel
class MoreViewModel @Inject constructor(
    catalog: CatalogRepository,
    withholding: WithholdingRepository,
    merchant: MerchantRepository,
    auth: AuthRepository,
    settings: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<MoreUiState> = combine(
        catalog.observeItems(), withholding.observeRecords(), merchant.setupStepsDone, auth.session, settings.settings,
    ) { items, records, done, session, org ->
        MoreUiState(
            activeItems = items.count { it.active },
            awaitingCertificates = records.count { !it.certificateReceived },
            setupDone = done.coerceIn(0, 5),
            branchCount = org.branches.size,
            session = session,
            settings = org,
        )
    }.stateIn(
        viewModelScope, SharingStarted.Eagerly,
        MoreUiState(branchCount = settings.settings.value.branches.size, session = auth.session.value, settings = settings.settings.value),
    )
}
