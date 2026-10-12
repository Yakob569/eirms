package com.eirspay.mobile.navigation

import androidx.lifecycle.ViewModel
import com.eirspay.domain.model.Session
import com.eirspay.domain.repository.AuthRepository
import com.eirspay.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** App-level state the navigation graph needs to decide where the splash goes. */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val preferences: PreferencesRepository,
) : ViewModel() {
    val session: StateFlow<Session?> = auth.session

    fun destinationAfterSplash(): Any = when {
        auth.session.value != null -> MainRoute
        preferences.onboardingCompleted.value -> SignInRoute
        else -> OnboardingRoute
    }
}
