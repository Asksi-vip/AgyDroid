package com.agydroid.domain.usecase

import com.agydroid.data.local.entities.ProjectStatus
import com.agydroid.data.remote.github.WorkflowRun
import com.agydroid.data.repository.BuildRepository
import com.agydroid.data.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class MonitorBuildUseCase @Inject constructor(
    private val buildRepository: BuildRepository,
    private val projectRepository: ProjectRepository
) {
    fun monitor(
        projectId: String,
        owner: String,
        repo: String,
        runId: Long
    ): Flow<WorkflowRun> {
        return buildRepository.monitorRunProgress(owner, repo, runId)
            .onEach { run ->
                when (run.status) {
                    "in_progress" -> projectRepository.updateStatus(projectId, ProjectStatus.CI_BUILDING)
                    "completed" -> {
                        if (run.conclusion == "success") {
                            projectRepository.updateStatus(projectId, ProjectStatus.COMPLETED)
                        } else {
                            projectRepository.updateStatus(projectId, ProjectStatus.BUILD_FAILED)
                        }
                    }
                }
            }
    }
}
