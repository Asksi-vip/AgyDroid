package com.agydroid.domain.usecase

import com.agydroid.data.local.SecureStorage
import com.agydroid.data.remote.github.CreateFileContentRequest
import com.agydroid.data.remote.github.CreateRepoRequest
import com.agydroid.data.remote.github.GitHubApi
import com.agydroid.data.remote.github.GitHubRepoResponse
import com.agydroid.data.repository.ProjectRepository
import android.util.Base64
import javax.inject.Inject

class CreateGitHubRepoUseCase @Inject constructor(
    private val gitHubApi: GitHubApi,
    private val projectRepository: ProjectRepository,
    private val secureStorage: SecureStorage
) {
    suspend operator fun invoke(
        projectId: String,
        repoName: String,
        isPrivate: Boolean = true
    ): Result<GitHubRepoResponse> {
        if (!secureStorage.hasValidGitHubToken()) {
            return Result.failure(IllegalStateException("No GitHub token configured"))
        }

        return try {
            val response = gitHubApi.createRepository(
                CreateRepoRequest(
                    name = repoName,
                    description = "Built with AgyDroid & Antigravity CLI",
                    private = isPrivate,
                    autoInit = true
                )
            )

            if (response.isSuccessful && response.body() != null) {
                val repo = response.body()!!
                projectRepository.updateGitHub(projectId, repo.name, repo.owner.login)

                // Inject workflow file
                setupGitHubActionsWorkflow(repo.owner.login, repo.name)

                Result.success(repo)
            } else {
                Result.failure(Exception("Failed to create GitHub repo: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun setupGitHubActionsWorkflow(owner: String, repo: String) {
        val workflowYaml = """
name: Build Android APK
on:
  push:
    branches: [ main, master ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3

      - name: Make Gradle executable
        run: chmod +x ./gradlew || true

      - name: Build Debug APK
        run: ./gradlew assembleDebug --stacktrace

      - name: Upload APK Artifact
        uses: actions/upload-artifact@v4
        with:
          name: app-debug
          path: app/build/outputs/apk/debug/*.apk
""".trimIndent()

        val encoded = Base64.encodeToString(workflowYaml.toByteArray(), Base64.NO_WRAP)
        try {
            gitHubApi.createOrUpdateFileContents(
                owner = owner,
                repo = repo,
                path = ".github/workflows/build.yml",
                request = CreateFileContentRequest(
                    message = "Add CI workflow for Android APK build",
                    content = encoded
                )
            )
        } catch (e: Exception) {
            // Non-fatal if autoInit branch isn't ready immediately
        }
    }
}
