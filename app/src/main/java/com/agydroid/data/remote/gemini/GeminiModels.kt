package com.agydroid.data.remote.gemini

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

data class GeminiChatRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiSystemInstruction? = null,
    val generationConfig: GeminiGenerationConfig? = null
)

data class GeminiContent(
    val role: String, // "user" or "model"
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String
)

data class GeminiSystemInstruction(
    val parts: List<GeminiPart>
)

data class GeminiGenerationConfig(
    val temperature: Float = 0.7f,
    val maxOutputTokens: Int = 8192
)

data class GeminiChatResponse(
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    val content: GeminiContent?
)

interface GeminiApi {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String = "gemini-1.5-flash",
        @Query("key") apiKey: String,
        @Body request: GeminiChatRequest
    ): Response<GeminiChatResponse>
}
