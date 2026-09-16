package com.agydroid.ui.fileexplorer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agydroid.data.remote.bridge.FileItemDto
import com.agydroid.data.repository.FileRepository
import com.agydroid.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class FileExplorerUiState {
    object Loading : FileExplorerUiState()
    data class Success(val files: List<FileItemDto>) : FileExplorerUiState()
    data class Error(val message: String) : FileExplorerUiState()
}

@HiltViewModel
class FileExplorerViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<FileExplorerUiState>(FileExplorerUiState.Loading)
    val uiState: StateFlow<FileExplorerUiState> = _uiState.asStateFlow()

    private val _selectedFileContent = MutableStateFlow<Pair<String, String>?>(null)
    val selectedFileContent: StateFlow<Pair<String, String>?> = _selectedFileContent.asStateFlow()

    fun loadFiles(projectId: String) {
        viewModelScope.launch {
            _uiState.value = FileExplorerUiState.Loading
            val project = projectRepository.getProjectById(projectId).firstOrNull()
            if (project == null) {
                _uiState.value = FileExplorerUiState.Error("Project not found")
                return@launch
            }

            val result = fileRepository.getWorkspaceTree(project.workspacePath)
            if (result.isSuccess) {
                _uiState.value = FileExplorerUiState.Success(result.getOrNull() ?: emptyList())
            } else {
                _uiState.value = FileExplorerUiState.Error(result.exceptionOrNull()?.message ?: "Failed to read files")
            }
        }
    }

    fun openFile(filePath: String) {
        viewModelScope.launch {
            val result = fileRepository.readFileContent(filePath)
            if (result.isSuccess) {
                val file = result.getOrNull()!!
                _selectedFileContent.value = Pair(file.name, file.content)
            }
        }
    }

    fun closeFileViewer() {
        _selectedFileContent.value = null
    }
}
