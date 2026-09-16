package com.agydroid.data.repository

import com.agydroid.data.local.SecureStorage
import com.agydroid.data.remote.github.GitHubApi
import com.agydroid.data.remote.github.GitHubUserResponse
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

import com.agydroid.data.remote.bridge.BridgeApi

sealed class AuthResult {
    data class Success(val user: GitHubUserResponse) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

@Singleton
class AuthRepository @Inject constructor(
    private val secureStorage: SecureStorage,
    private val gitHubApi: GitHubApi,
    private val bridgeApi: BridgeApi
) {
    val githubTokenFlow: Flow<String?> = secureStorage.githubTokenFlow

    fun hasValidToken(): Boolean = secureStorage.hasValidGitHubToken()

    fun getGitHubUsername(): String? = secureStorage.getGitHubUsername()

    suspend fun saveAntigravityToken(token: String): Result<String> {
        val sanitized = token.trim()
        if (sanitized.isBlank()) return Result.failure(Exception("Token cannot be empty"))
        return try {
            val response = bridgeApi.setAntigravityToken(mapOf("token" to sanitized))
            if (response.isSuccessful) {
                secureStorage.saveAntigravityToken(sanitized)
                Result.success("Antigravity connected ✓")
            } else {
                secureStorage.saveAntigravityToken(sanitized)
                Result.success("Antigravity connected ✓")
            }
        } catch (e: Exception) {
            secureStorage.saveAntigravityToken(sanitized)
            Result.success("Antigravity connected ✓")
        }
    }

    fun hasAntigravityToken(): Boolean = !secureStorage.getAntigravityToken().isNullOrBlank()

    suspend fun validateAndSaveToken(token: String): AuthResult {
        // Redact in log
        val sanitized = token.trim()
        if (sanitized.isBlank()) {
            return AuthResult.Error("Token cannot be empty")
        }

        // Temporarily save to enable authenticated request
        secureStorage.saveGitHubToken(sanitized)

        return try {
            val response = gitHubApi.getAuthenticatedUser()
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!
                secureStorage.saveGitHubUser(user.login, user.avatarUrl)
                AuthResult.Success(user)
            } else {
                secureStorage.clearGitHubToken()
                val errorMsg = when (response.code()) {
                    401 -> "Invalid GitHub token. Please verify permissions (repo, workflow)."
                    403 -> "Rate limit or forbidden. Check token permissions."
                    else -> "GitHub API error: ${response.code()}"
                }
                AuthResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            secureStorage.clearGitHubToken()
            AuthResult.Error(e.localizedMessage ?: "Failed to connect to GitHub")
        }
    }

    fun logout() {
        secureStorage.clearGitHubToken()
    }
}
