package com.archieapps.exercises.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.exercises.core.data.AuthRepository
import com.archieapps.exercises.core.data.CachedHome
import com.archieapps.exercises.core.data.HomeRepository
import com.archieapps.exercises.core.net.ApiResult
import com.archieapps.exercises.core.net.FailureKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val cached: CachedHome? = null,
    val refreshing: Boolean = false,
    val error: String? = null,
    val offline: Boolean = false,
) {
    val showSkeleton: Boolean get() = cached == null && refreshing
    val showEmpty: Boolean get() = cached == null && !refreshing
}

class HomeViewModel(
    private val homeRepository: HomeRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val refreshing = MutableStateFlow(true)
    private val failure = MutableStateFlow<ApiResult.Failure?>(null)

    val state: StateFlow<HomeUiState> = combine(
        homeRepository.observe(),
        refreshing,
        failure,
    ) { cached, isRefreshing, fail ->
        HomeUiState(
            cached = cached,
            refreshing = isRefreshing,
            error = fail?.message,
            offline = fail?.kind == FailureKind.OFFLINE,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(refreshing = true))

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            refreshing.value = true

            when (val result = homeRepository.refresh()) {
                is ApiResult.Success -> failure.value = null
                is ApiResult.Failure -> {
                    failure.value = result
                    if (result.kind == FailureKind.UNAUTHORIZED) authRepository.logout()
                }
            }

            refreshing.value = false
        }
    }
}
