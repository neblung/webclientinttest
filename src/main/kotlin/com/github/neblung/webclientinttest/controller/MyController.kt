package com.github.neblung.webclientinttest.controller

import com.github.neblung.webclientinttest.api.GitHubApi
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class MyController(private val github: GitHubApi) {
    @GetMapping("/{username}")
    suspend fun listRepos(@PathVariable("username") username: String) =
        github.listRepositoriesByUsername(username, 1, 10)
}
