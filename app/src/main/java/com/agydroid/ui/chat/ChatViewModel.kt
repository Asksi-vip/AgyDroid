package com.agydroid.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agydroid.data.local.entities.MessageEntity
import com.agydroid.data.local.entities.ProjectEntity
import com.agydroid.data.local.entities.ProjectStatus
import com.agydroid.data.repository.BuildRepository
import com.agydroid.data.repository.ChatRepository
import com.agydroid.data.repository.ProjectRepository
import com.agydroid.domain.usecase.CreateGitHubRepoUseCase
import com.agydroid.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val isStreaming: Boolean = false,
    val currentActivity: String? = null,
    val buildError: String? = null,
    val isBuilding: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val chatRepository: ChatRepository,
    private val sendMessageUseCase: SendMessageUseCase,
    private val createGitHubRepoUseCase: CreateGitHubRepoUseCase,
    private val buildRepository: BuildRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var currentStreamJob: Job? = null

    fun getProject(projectId: String): Flow<ProjectEntity?> =
        projectRepository.getProjectById(projectId)

    fun getMessages(projectId: String): Flow<List<MessageEntity>> =
        chatRepository.getMessagesForProject(projectId)

    fun sendMessage(project: ProjectEntity, prompt: String) {
        if (prompt.isBlank() || _uiState.value.isStreaming) return

        currentStreamJob?.cancel()
        currentStreamJob = viewModelScope.launch {
            _uiState.update { it.copy(isStreaming = true, currentActivity = "Thinking...") }
            try {
                val (_, streamFlow) = sendMessageUseCase.sendUserMessage(
                    projectId = project.id,
                    workspace = project.workspacePath,
                    content = prompt,
                    conversationId = project.conversationId
                )

                streamFlow.collect { event ->
                    // Activity updates from events
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(buildError = e.message) }
            } finally {
                _uiState.update { it.copy(isStreaming = false, currentActivity = null) }
            }
        }
    }

    fun cancelGeneration() {
        currentStreamJob?.cancel()
        _uiState.update { it.copy(isStreaming = false, currentActivity = null) }
    }

    fun triggerBuild(project: ProjectEntity, onBuildStarted: (Long) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBuilding = true) }
            projectRepository.updateStatus(project.id, ProjectStatus.UPLOADING)

            val owner = project.githubOwner
            val repoName = project.githubRepo ?: project.name.lowercase().replace(" ", "-")

            // Create GitHub repo if needed
            if (project.githubRepo.isNullOrBlank()) {
                val result = createGitHubRepoUseCase(project.id, repoName)
                if (result.isFailure) {
                    _uiState.update {
                        it.copy(isBuilding = false, buildError = result.exceptionOrNull()?.message)
                    }
                    projectRepository.updateStatus(project.id, ProjectStatus.BUILD_FAILED)
                    return@launch
                }
            }

            // Get the workflow run ID
            val activeOwner = project.githubOwner ?: owner ?: ""
            val activeRepo = project.githubRepo ?: repoName

            val runResult = buildRepository.getLatestWorkflowRun(activeOwner, activeRepo)
            val runId = runResult.getOrNull()?.id ?: System.currentTimeMillis()

            buildRepository.recordBuildStart(project.id, runId)
            projectRepository.updateStatus(project.id, ProjectStatus.CI_BUILDING)
            _uiState.update { it.copy(isBuilding = false) }

            onBuildStarted(runId)
        }
    }
}
