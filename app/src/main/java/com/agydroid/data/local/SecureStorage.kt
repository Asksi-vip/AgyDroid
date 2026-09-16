package com.agydroid.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "agydroid_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _githubTokenFlow = MutableStateFlow(getGitHubToken())
    val githubTokenFlow: Flow<String?> = _githubTokenFlow.asStateFlow()

    fun saveGitHubToken(token: String) {
        sharedPreferences.edit().putString(KEY_GITHUB_TOKEN, token).apply()
        _githubTokenFlow.value = token
    }

    fun getGitHubToken(): String? {
        return sharedPreferences.getString(KEY_GITHUB_TOKEN, null)
    }

    fun clearGitHubToken() {
        sharedPreferences.edit().remove(KEY_GITHUB_TOKEN).apply()
        _githubTokenFlow.value = null
    }

    fun saveGitHubUser(username: String, avatarUrl: String?) {
        sharedPreferences.edit()
            .putString(KEY_GITHUB_USERNAME, username)
            .putString(KEY_GITHUB_AVATAR, avatarUrl)
            .apply()
    }

    fun getGitHubUsername(): String? = sharedPreferences.getString(KEY_GITHUB_USERNAME, null)
    fun getGitHubAvatar(): String? = sharedPreferences.getString(KEY_GITHUB_AVATAR, null)

    fun saveAntigravityToken(token: String) {
        sharedPreferences.edit().putString(KEY_AGY_TOKEN, token).apply()
    }

    fun getAntigravityToken(): String? = sharedPreferences.getString(KEY_AGY_TOKEN, null)

    fun clearAntigravityToken() {
        sharedPreferences.edit().remove(KEY_AGY_TOKEN).apply()
    }

    companion object {
        private const val KEY_GITHUB_TOKEN = "github_personal_access_token"
        private const val KEY_GITHUB_USERNAME = "github_username"
        private const val KEY_GITHUB_AVATAR = "github_avatar_url"
        private const val KEY_AGY_TOKEN = "antigravity_oauth_token"
    }
}
