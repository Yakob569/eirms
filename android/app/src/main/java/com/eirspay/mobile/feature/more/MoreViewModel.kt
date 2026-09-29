package com.eirspay.mobile.feature.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eirspay.domain.model.Session
import com.eirspay.domain.repository.AuthRepository
import com.eirspay.domain.repository.CatalogRepository
import com.eirspay.domain.repository.MerchantRepository
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
)

@HiltViewModel
class MoreViewModel @Inject constructor(
    catalog: CatalogRepository,
    withholding: WithholdingRepository,
    merchant: MerchantRepository,
    auth: AuthRepository,
) : ViewModel() {
    private val branchCount = merchant.branches.size

    val state: StateFlow<MoreUiState> = combine(
        catalog.observeItems(), withholding.observeRecords(), merchant.setupStepsDone, auth.session,
    ) { items, records, done, session ->
        MoreUiState(
            activeItems = items.count { it.active },
            awaitingCertificates = records.count { !it.certificateReceived },
            setupDone = done.coerceIn(0, 5),
            branchCount = branchCount,
            session = session,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MoreUiState(branchCount = branchCount, session = auth.session.value))
}
