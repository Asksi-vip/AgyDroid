package com.agydroid.data.remote.github

import com.google.gson.annotations.SerializedName

data class GitHubUserResponse(
    val id: Long,
    val login: String,
    val name: String?,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("html_url") val htmlUrl: String?
)

data class CreateRepoRequest(
    val name: String,
    val description: String? = "Created via AgyDroid",
    val private: Boolean = true,
    @SerializedName("auto_init") val autoInit: Boolean = false
)

data class GitHubRepoResponse(
    val id: Long,
    val name: String,
    @SerializedName("full_name") val fullName: String,
    val owner: GitHubRepoOwner,
    val private: Boolean,
    @SerializedName("html_url") val htmlUrl: String,
    @SerializedName("clone_url") val cloneUrl: String
)

data class GitHubRepoOwner(
    val login: String,
    val id: Long,
    @SerializedName("avatar_url") val avatarUrl: String?
)

data class WorkflowRunsResponse(
    @SerializedName("total_count") val totalCount: Int,
    @SerializedName("workflow_runs") val workflowRuns: List<WorkflowRun>
)

data class WorkflowRun(
    val id: Long,
    val name: String?,
    val status: String, // queued, in_progress, completed
    val conclusion: String?, // success, failure, neutral, cancelled, timed_out, action_required
    @SerializedName("html_url") val htmlUrl: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("run_attempt") val runAttempt: Int
)

data class ArtifactsResponse(
    @SerializedName("total_count") val totalCount: Int,
    val artifacts: List<ArtifactItem>
)

data class ArtifactItem(
    val id: Long,
    val name: String,
    @SerializedName("size_in_bytes") val sizeInBytes: Long,
    @SerializedName("archive_download_url") val archiveDownloadUrl: String,
    val expired: Boolean,
    @SerializedName("created_at") val createdAt: String
)

data class CreateFileContentRequest(
    val message: String,
    val content: String, // Base64 encoded
    val branch: String = "main"
)
