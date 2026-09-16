package com.agydroid.data.repository

import com.agydroid.data.local.dao.ProjectDao
import com.agydroid.data.local.entities.ProjectEntity
import com.agydroid.data.local.entities.ProjectStatus
import com.agydroid.data.remote.bridge.BridgeApi
import com.agydroid.data.remote.bridge.CreateWorkspaceRequest
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepository @Inject constructor(
    private val projectDao: ProjectDao,
    private val bridgeApi: BridgeApi
) {
    fun getAllProjects(): Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    fun getProjectById(id: String): Flow<ProjectEntity?> = projectDao.getProjectById(id)

    suspend fun createProject(
        name: String,
        packageName: String,
        imageUri: String? = null
    ): Result<ProjectEntity> {
        return try {
            val trimmedName = name.trim()
            val trimmedPkg = packageName.trim()

            // Ask Termux bridge to prepare workspace directory
            val bridgeResponse = try {
                bridgeApi.createWorkspace(CreateWorkspaceRequest(trimmedName, trimmedPkg))
            } catch (e: Exception) {
                null
            }

            val workspacePath = if (bridgeResponse?.isSuccessful == true && bridgeResponse.body() != null) {
                bridgeResponse.body()!!.workspace
            } else {
                "/data/data/com.termux/files/home/AgyDroid-projects/$trimmedName"
            }

            val project = ProjectEntity(
                id = UUID.randomUUID().toString(),
                name = trimmedName,
                packageName = trimmedPkg,
                workspacePath = workspacePath,
                status = ProjectStatus.CREATED,
                imageUri = imageUri,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            projectDao.insertProject(project)
            Result.success(project)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateStatus(id: String, status: ProjectStatus) {
        projectDao.updateStatus(id, status.name)
    }

    suspend fun updateGitHub(id: String, repo: String, owner: String) {
        projectDao.updateGitHub(id, repo, owner)
    }

    suspend fun updateApkUrl(id: String, apkUrl: String) {
        projectDao.updateApkUrl(id, apkUrl)
    }

    suspend fun deleteProject(id: String) {
        projectDao.deleteProject(id)
    }
}
