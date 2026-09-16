package com.agydroid.data.remote.bridge

import com.google.gson.annotations.SerializedName

data class BridgeStatusResponse(
    val status: String,
    @SerializedName("agy_version") val agyVersion: String?,
    @SerializedName("bridge_version") val bridgeVersion: String?,
    @SerializedName("agy_path") val agyPath: String?
)

data class CreateWorkspaceRequest(
    @SerializedName("project_name") val projectName: String,
    @SerializedName("package_name") val packageName: String
)

data class CreateWorkspaceResponse(
    val success: Boolean,
    val workspace: String,
    @SerializedName("project_name") val projectName: String
)

data class ChatPromptRequest(
    val prompt: String,
    val workspace: String,
    @SerializedName("conversation_id") val conversationId: String? = null,
    val model: String? = "gemini-3.8-flash-medium"
)

data class FileItemDto(
    val name: String,
    val path: String,
    @SerializedName("is_dir") val isDir: Boolean,
    val size: Long,
    val children: List<FileItemDto>? = null
)

data class FileTreeResponse(
    val workspace: String,
    val tree: List<FileItemDto>
)

data class ReadFileResponse(
    val path: String,
    val name: String,
    val content: String,
    val size: Long
)

sealed class BridgeEvent {
    data class Start(val message: String) : BridgeEvent()
    data class StreamData(val rawJson: String, val type: String?, val content: String?) : BridgeEvent()
    data class Text(val content: String) : BridgeEvent()
    data class Done(val exitCode: Int, val success: Boolean) : BridgeEvent()
    data class Error(val message: String) : BridgeEvent()
}
