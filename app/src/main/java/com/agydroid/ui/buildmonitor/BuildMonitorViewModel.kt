package com.agydroid.ui.buildmonitor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agydroid.data.local.entities.BuildEntity
import com.agydroid.data.remote.github.WorkflowRun
import com.agydroid.data.repository.BuildRepository
import com.agydroid.data.repository.ProjectRepository
import com.agydroid.domain.usecase.MonitorBuildUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BuildMonitorViewModel @Inject constructor(
    private val buildRepository: BuildRepository,
    private val projectRepository: ProjectRepository,
    private val monitorBuildUseCase: MonitorBuildUseCase
) : ViewModel() {

    private val _currentRun = MutableStateFlow<WorkflowRun?>(null)
    val currentRun: StateFlow<WorkflowRun?> = _currentRun.asStateFlow()

    fun getBuildRecord(runId: Long): Flow<BuildEntity?> =
        buildRepository.getBuildByRunId(runId)

    fun startMonitoring(projectId: String, runId: Long) {
        viewModelScope.launch {
            val project = projectRepository.getProjectById(projectId).firstOrNull() ?: return@launch
            val owner = project.githubOwner ?: return@launch
            val repo = project.githubRepo ?: return@launch

            monitorBuildUseCase.monitor(projectId, owner, repo, runId)
                .collect { run ->
                    _currentRun.value = run
                }
        }
    }
}
