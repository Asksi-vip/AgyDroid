package com.agydroid.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class BuildStatus { QUEUED, IN_PROGRESS, COMPLETED, FAILED, CANCELLED }

@Entity(tableName = "builds")
data class BuildEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val runId: Long,
    val status: BuildStatus = BuildStatus.QUEUED,
    val conclusion: String? = null,
    val apkUrl: String? = null,
    val artifactId: Long? = null,
    val logsUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
