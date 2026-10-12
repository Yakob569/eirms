package com.eirspay.mobile.feature.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eirspay.domain.model.Branch
import com.eirspay.domain.model.BranchInfo
import com.eirspay.domain.model.Session
import com.eirspay.domain.repository.AuthRepository
import com.eirspay.domain.repository.MerchantRepository
import com.eirspay.domain.repository.PreferencesRepository
import com.eirspay.domain.usecase.SignOutUseCase
import com.eirspay.mobile.ui.designsystem.LoaderMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val session: Session? = null,
    val selectedBranch: Branch? = null,
    val branches: List<BranchInfo> = emptyList(),
    val loading: LoaderMessage? = null,
)

sealed interface MainEvent {
    data class TerminalStarted(val branch: Branch) : MainEvent
    data object SignedOut : MainEvent
}

@HiltViewModel
class MainViewModel @Inject constructor(
    auth: AuthRepository,
    merchant: MerchantRepository,
    private val prefs: PreferencesRepository,
    private val signOutUseCase: SignOutUseCase,
) : ViewModel() {
    private val loading = MutableStateFlow<LoaderMessage?>(null)
    private val branches = merchant.branches

    val state: StateFlow<MainUiState> = combine(auth.session, prefs.selectedBranch, loading) { s, b, l ->
        MainUiState(s, b, branches, l)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState(auth.session.value, prefs.selectedBranch.value, branches))

    private val _events = Channel<MainEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun selectBranch(branch: Branch?) = prefs.selectBranch(branch)

    fun startTerminal(branch: Branch) {
        if (loading.value != null) return
        viewModelScope.launch {
            loading.value = LoaderMessage("Starting terminal mode", "Locking the app to sales · " + branch.displayName)
            delay(1800)
            _events.send(MainEvent.TerminalStarted(branch))
            loading.value = null
        }
    }

    fun signOut() {
        viewModelScope.launch {
            signOutUseCase()
            _events.send(MainEvent.SignedOut)
        }
    }
}
