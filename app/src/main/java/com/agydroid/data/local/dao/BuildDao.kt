package com.agydroid.data.local.dao

import androidx.room.*
import com.agydroid.data.local.entities.BuildEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BuildDao {
    @Query("SELECT * FROM builds WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getBuildsForProject(projectId: String): Flow<List<BuildEntity>>

    @Query("SELECT * FROM builds WHERE runId = :runId LIMIT 1")
    fun getBuildByRunId(runId: Long): Flow<BuildEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuild(build: BuildEntity)

    @Update
    suspend fun updateBuild(build: BuildEntity)

    @Query("UPDATE builds SET status = :status, conclusion = :conclusion, updatedAt = :now WHERE runId = :runId")
    suspend fun updateBuildStatus(runId: Long, status: String, conclusion: String?, now: Long = System.currentTimeMillis())

    @Query("UPDATE builds SET apkUrl = :url, artifactId = :artifactId WHERE runId = :runId")
    suspend fun updateApkArtifact(runId: Long, url: String, artifactId: Long)
}
