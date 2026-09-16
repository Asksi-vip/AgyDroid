package com.agydroid.data.repository

import com.agydroid.data.local.dao.BuildDao
import com.agydroid.data.local.entities.BuildEntity
import com.agydroid.data.local.entities.BuildStatus
import com.agydroid.data.remote.github.GitHubApi
import com.agydroid.data.remote.github.WorkflowRun
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuildRepository @Inject constructor(
    private val buildDao: BuildDao,
    private val gitHubApi: GitHubApi
) {
    fun getBuildsForProject(projectId: String): Flow<List<BuildEntity>> {
        return buildDao.getBuildsForProject(projectId)
    }

    fun getBuildByRunId(runId: Long): Flow<BuildEntity?> {
        return buildDao.getBuildByRunId(runId)
    }

    suspend fun recordBuildStart(projectId: String, runId: Long): BuildEntity {
        val entity = BuildEntity(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            runId = runId,
            status = BuildStatus.IN_PROGRESS,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        buildDao.insertBuild(entity)
        return entity
    }

    suspend fun getLatestWorkflowRun(owner: String, repo: String): Result<WorkflowRun?> {
        return try {
            val response = gitHubApi.listWorkflowRuns(owner, repo, perPage = 1)
            if (response.isSuccessful && response.body() != null) {
                val runs = response.body()!!.workflowRuns
                Result.success(runs.firstOrNull())
            } else {
                Result.failure(Exception("Failed to get workflow runs: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun monitorRunProgress(
        owner: String,
        repo: String,
        runId: Long,
        pollIntervalMs: Long = 5000
    ): Flow<WorkflowRun> = flow {
        var isCompleted = false
        while (!isCompleted) {
            try {
                val response = gitHubApi.getWorkflowRun(owner, repo, runId)
                if (response.isSuccessful && response.body() != null) {
                    val run = response.body()!!
                    emit(run)

                    val status = when (run.status) {
                        "queued" -> BuildStatus.QUEUED
                        "in_progress" -> BuildStatus.IN_PROGRESS
                        "completed" -> {
                            isCompleted = true
                            if (run.conclusion == "success") BuildStatus.COMPLETED else BuildStatus.FAILED
                        }
                        else -> BuildStatus.IN_PROGRESS
                    }

                    buildDao.updateBuildStatus(runId, status.name, run.conclusion)

                    // If completed with success, look for APK artifact
                    if (isCompleted && run.conclusion == "success") {
                        val artifactsResponse = gitHubApi.listArtifacts(owner, repo, runId)
                        if (artifactsResponse.isSuccessful && artifactsResponse.body() != null) {
                            val artifact = artifactsResponse.body()!!.artifacts.firstOrNull {
                                it.name.contains("apk", ignoreCase = true) || it.name.contains("app", ignoreCase = true)
                            }
                            if (artifact != null) {
                                buildDao.updateApkArtifact(runId, artifact.archiveDownloadUrl, artifact.id)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Log and keep retrying
            }
            if (!isCompleted) {
                delay(pollIntervalMs)
            }
        }
    }
}
