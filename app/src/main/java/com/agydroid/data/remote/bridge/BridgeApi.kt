package com.agydroid.data.remote.bridge

import retrofit2.Response
import retrofit2.http.*

interface BridgeApi {
    @GET("status")
    suspend fun getStatus(): Response<BridgeStatusResponse>

    @POST("workspace/create")
    suspend fun createWorkspace(@Body request: CreateWorkspaceRequest): Response<CreateWorkspaceResponse>

    @GET("files")
    suspend fun listFiles(@Query("workspace") workspace: String): Response<FileTreeResponse>

    @GET("files/read")
    suspend fun readFile(@Query("path") path: String): Response<ReadFileResponse>
}
