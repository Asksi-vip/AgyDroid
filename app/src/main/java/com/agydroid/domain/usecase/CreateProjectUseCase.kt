package com.agydroid.domain.usecase

import com.agydroid.data.local.entities.ProjectEntity
import com.agydroid.data.repository.ProjectRepository
import javax.inject.Inject

class CreateProjectUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val validatePackageNameUseCase: ValidatePackageNameUseCase
) {
    suspend operator fun invoke(
        name: String,
        packageName: String,
        imageUri: String? = null
    ): Result<ProjectEntity> {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Project name cannot be empty"))
        }

        val pkgResult = validatePackageNameUseCase(packageName)
        if (pkgResult.isFailure) {
            return Result.failure(pkgResult.exceptionOrNull()!!)
        }

        return projectRepository.createProject(trimmedName, packageName, imageUri)
    }
}
