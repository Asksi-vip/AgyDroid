package com.agydroid.data.local.dao

import androidx.room.*
import com.agydroid.data.local.entities.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectById(id: String): Flow<ProjectEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: String)

    @Query("UPDATE projects SET status = :status, updatedAt = :now WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE projects SET githubRepo = :repo, githubOwner = :owner, updatedAt = :now WHERE id = :id")
    suspend fun updateGitHub(id: String, repo: String, owner: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE projects SET latestApkUrl = :url, updatedAt = :now WHERE id = :id")
    suspend fun updateApkUrl(id: String, url: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE projects SET conversationId = :convId WHERE id = :id")
    suspend fun updateConversationId(id: String, convId: String)
}
