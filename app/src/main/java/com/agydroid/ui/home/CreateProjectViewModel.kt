package com.agydroid.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agydroid.domain.usecase.CreateProjectUseCase
import com.agydroid.domain.usecase.ValidatePackageNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CreateProjectUiState {
    object Idle : CreateProjectUiState()
    object Loading : CreateProjectUiState()
    data class Success(val projectId: String) : CreateProjectUiState()
    data class Error(val message: String) : CreateProjectUiState()
}

@HiltViewModel
class CreateProjectViewModel @Inject constructor(
    private val createProjectUseCase: CreateProjectUseCase,
    private val validatePackageNameUseCase: ValidatePackageNameUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreateProjectUiState>(CreateProjectUiState.Idle)
    val uiState: StateFlow<CreateProjectUiState> = _uiState.asStateFlow()

    fun createProject(name: String, packageName: String) {
        val trimmedName = name.trim()
        val trimmedPkg = packageName.trim()

        if (trimmedName.isEmpty()) {
            _uiState.value = CreateProjectUiState.Error("Project name cannot be empty")
            return
        }

        val validation = validatePackageNameUseCase(trimmedPkg)
        if (validation.isFailure) {
            _uiState.value = CreateProjectUiState.Error(validation.exceptionOrNull()?.message ?: "Invalid package name")
            return
        }

        viewModelScope.launch {
            _uiState.value = CreateProjectUiState.Loading
            val result = createProjectUseCase(trimmedName, trimmedPkg)
            if (result.isSuccess) {
                _uiState.value = CreateProjectUiState.Success(result.getOrNull()!!.id)
            } else {
                _uiState.value = CreateProjectUiState.Error(result.exceptionOrNull()?.message ?: "Failed to create project")
            }
        }
    }
}
