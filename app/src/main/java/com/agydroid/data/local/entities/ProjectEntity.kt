package com.agydroid.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ProjectStatus {
    CREATED, PLANNING, CODING, READY,
    BUILDING, BUILD_FAILED, FIXING,
    UPLOADING, CI_BUILDING, COMPLETED, ERROR
}

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val packageName: String,
    val workspacePath: String,
    val githubRepo: String? = null,
    val githubOwner: String? = null,
    val status: ProjectStatus = ProjectStatus.CREATED,
    val imageUri: String? = null,
    val conversationId: String? = null,
    val latestApkUrl: String? = null,
    val latestBuildNumber: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
