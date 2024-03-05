package com.github.neblung.webclientinttest.model

data class PageableGitHubResponse<T>(
    val items: List<T>,
    val hasMoreItems: Boolean,
)

data class GitHubRepoResponse(
    val fork: Boolean,
    val name: String,
    val owner: GitHubOwnerResponse,
)

data class GitHubOwnerResponse(
    val login: String,
)

