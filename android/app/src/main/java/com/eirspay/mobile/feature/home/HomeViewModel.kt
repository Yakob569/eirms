package com.eirspay.mobile.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eirspay.domain.model.ActivityItem
import com.eirspay.domain.model.Branch
import com.eirspay.domain.model.BranchInfo
import com.eirspay.domain.model.DashboardRange
import com.eirspay.domain.model.SalesAnalytics
import com.eirspay.domain.repository.MerchantRepository
import com.eirspay.domain.repository.PreferencesRepository
import com.eirspay.domain.usecase.GetSalesAnalyticsUseCase
import com.eirspay.domain.usecase.ObserveRecentActivityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val range: DashboardRange = DashboardRange.DAYS_7,
    val branch: Branch? = null,
    val branches: List<BranchInfo> = emptyList(),
    /** Null until the first analytics result for the current range/branch arrives. */
    val analytics: SalesAnalytics? = null,
    val recent: List<ActivityItem> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getSalesAnalytics: GetSalesAnalyticsUseCase,
    observeRecentActivity: ObserveRecentActivityUseCase,
    merchant: MerchantRepository,
    private val prefs: PreferencesRepository,
) : ViewModel() {
    private val range = MutableStateFlow(DashboardRange.DAYS_7)
    private val analytics = MutableStateFlow<SalesAnalytics?>(null)
    private val branches = merchant.branches

    init {
        viewModelScope.launch {
            combine(range, prefs.selectedBranch) { r, b -> r to b }.collectLatest { (r, b) ->
                analytics.value = getSalesAnalytics(r, b)
            }
        }
    }

    val state: StateFlow<HomeUiState> = combine(range, prefs.selectedBranch, analytics, observeRecentActivity(50)) { r, b, a, recent ->
        HomeUiState(
            range = r, branch = b, branches = branches,
            analytics = a,
            recent = recent.filter { b == null || it.branch == b }.take(5),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState(branch = prefs.selectedBranch.value, branches = branches))

    fun selectRange(r: DashboardRange) { range.value = r }

    /** Tapping a branch in "By branch" toggles the global filter. */
    fun toggleBranch(branch: Branch) {
        prefs.selectBranch(if (prefs.selectedBranch.value == branch) null else branch)
    }
}
