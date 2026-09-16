package com.agydroid.data.repository

import com.agydroid.data.remote.bridge.BridgeApi
import com.agydroid.data.remote.bridge.FileItemDto
import com.agydroid.data.remote.bridge.ReadFileResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepository @Inject constructor(
    private val bridgeApi: BridgeApi
) {
    suspend fun getWorkspaceTree(workspace: String): Result<List<FileItemDto>> {
        return try {
            val response = bridgeApi.listFiles(workspace)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.tree)
            } else {
                Result.failure(Exception("Failed to read workspace tree: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun readFileContent(filePath: String): Result<ReadFileResponse> {
        return try {
            val response = bridgeApi.readFile(filePath)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to read file: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
