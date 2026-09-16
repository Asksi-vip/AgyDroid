package com.agydroid.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agydroid.data.local.entities.ProjectEntity
import com.agydroid.data.remote.bridge.BridgeApi
import com.agydroid.data.remote.bridge.BridgeStatusResponse
import com.agydroid.data.repository.AuthRepository
import com.agydroid.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val authRepository: AuthRepository,
    private val bridgeApi: BridgeApi,
    private val internalEngineManager: com.agydroid.engine.InternalEngineManager
) : ViewModel() {

    val projects: StateFlow<List<ProjectEntity>> = projectRepository.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _bridgeStatus = MutableStateFlow<BridgeStatusResponse?>(null)
    val bridgeStatus: StateFlow<BridgeStatusResponse?> = _bridgeStatus.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredProjects: StateFlow<List<ProjectEntity>> = combine(projects, _searchQuery) { list, query ->
        if (query.isBlank()) list
        else list.filter { it.name.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        checkBridgeStatus()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun checkBridgeStatus() {
        viewModelScope.launch {
            try {
                val res = bridgeApi.getStatus()
                if (res.isSuccessful && res.body() != null) {
                    _bridgeStatus.value = res.body()
                } else {
                    _bridgeStatus.value = internalEngineManager.getEngineStatus()
                }
            } catch (e: Exception) {
                _bridgeStatus.value = internalEngineManager.getEngineStatus()
            }
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            projectRepository.deleteProject(id)
        }
    }

    fun logout() {
        authRepository.logout()
    }
}
