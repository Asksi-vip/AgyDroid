package com.agydroid.domain.usecase

import javax.inject.Inject

class ValidatePackageNameUseCase @Inject constructor() {
    operator fun invoke(packageName: String): Result<Unit> {
        val trimmed = packageName.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Package name cannot be empty"))
        }

        val pattern = "^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$".toRegex()
        if (!pattern.matches(trimmed)) {
            return Result.failure(
                IllegalArgumentException("Invalid package name format. Must be at least two segments (e.g. com.example.app), lowercase letters, numbers, or underscores, starting each segment with a letter.")
            )
        }

        val javaKeywords = setOf(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new", "package",
            "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient",
            "try", "void", "volatile", "while"
        )

        val segments = trimmed.split(".")
        for (segment in segments) {
            if (javaKeywords.contains(segment)) {
                return Result.failure(IllegalArgumentException("Package segment '$segment' is a reserved Java keyword"))
            }
        }

        return Result.success(Unit)
    }
}
