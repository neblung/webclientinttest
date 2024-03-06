package com.github.neblung.webclientinttest.api

import com.github.neblung.webclientinttest.UpstreamApiException
import com.github.neblung.webclientinttest.model.GitHubOwnerResponse
import com.github.neblung.webclientinttest.model.GitHubRepoResponse
import com.github.neblung.webclientinttest.model.PageableGitHubResponse
import io.javalin.Javalin
import io.javalin.http.Context
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.TestPropertySource
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

private const val TEST_KEY = "TEST_KEY"
private const val TEST_PORT = 8082
private const val TEST_VERSION = "2022-11-28"

@OptIn(ExperimentalCoroutinesApi::class)
@TestPropertySource(
    properties = [
        "api.github.url=http://localhost:${TEST_PORT}",
        "api.github.key=$TEST_KEY",
        "api.github.version=$TEST_VERSION",
    ],
)
@SpringBootTest(
    webEnvironment = RANDOM_PORT,
)
class GitHubApiTests {
    @Autowired
    private lateinit var gitHubApi: GitHubApi
    private lateinit var javalin: Javalin
    private var handler: (Context) -> Unit = { }

    @BeforeEach
    fun `set up`() {
        javalin = Javalin.create()
        javalin.get("/users/{username}/repos") { ctx ->
            ctx.status(404)
            handler(ctx)
        }
        javalin.start(TEST_PORT)
    }

    private fun randomInt() = (1..100).random()

    @AfterEach
    fun `tear down`() {
        javalin.stop()
    }

    private suspend fun listRepositories(
        username: String = randomInt().toString(),
        page: Int = randomInt(),
        perPage: Int = randomInt(),
        handler: (Context) -> Unit
    ): PageableGitHubResponse<GitHubRepoResponse>? {
        this.handler = handler
        // method under test
        return gitHubApi.listRepositoriesByUsername(username, page, perPage)
    }

    @Test
    fun `parameters should be sent to github api`() = runTest {
        listRepositories(username = "the-user-name", page = 42, perPage = 1017) { ctx ->
            ctx.pathParam("username") shouldBe "the-user-name"
            ctx.queryParam("page") shouldBe "42"
            ctx.queryParam("per_page") shouldBe "1017"
        }
    }

    @Test
    fun `headers should be sent`() = runTest {
        listRepositories { ctx ->
            ctx.header("Authorization") shouldBe "Bearer $TEST_KEY"
            ctx.header("X-GitHub-Api-Version") shouldBe TEST_VERSION
            ctx.header("Accept") shouldBe "application/vnd.github+json"
        }
    }

    @Test
    fun `github responds with 404 -- should return null`() = runTest {
        listRepositories { ctx -> ctx.status(404) } shouldBe null
    }

    @Test
    fun `github responds with 401 -- should raise UpstreamApiException`() = runTest {
        val thrown = assertThrows<UpstreamApiException> {
            listRepositories { ctx -> ctx.status(401) }
        }
        with(thrown) {
            msg shouldBe "GitHub API request failed (401 UNAUTHORIZED)."
            statusCode shouldBe HttpStatus.UNAUTHORIZED
        }
    }

    @Test
    fun `github returns empty list -- should return empty list and next=false`() = runTest {
        val result = listRepositories { ctx ->
            ctx.json("[]")
            ctx.status(200)
        }

        with(result) {
            shouldNotBeNull()
            items shouldHaveSize 0
            hasMoreItems shouldBe false
        }
    }

    @Nested
    inner class GitHubReturnsPayload {
        private val stubbedRepos = listOf(
            GitHubRepoResponse(fork = true, name = "repo1", owner = GitHubOwnerResponse(login = "this-owner")),
            GitHubRepoResponse(fork = false, name = "repo2", owner = GitHubOwnerResponse(login = "that-owner")),
        )

        @OptIn(ExperimentalContracts::class)
        private fun checkItems(result: PageableGitHubResponse<GitHubRepoResponse>?) {
            contract {
                returns() implies (result != null)
            }
            with(result) {
                shouldNotBeNull()
                items shouldBe stubbedRepos
            }
        }

        private suspend fun listRepositories(linkHeader: String): PageableGitHubResponse<GitHubRepoResponse>? {
            return listRepositories { ctx ->
                ctx.json(stubbedRepos)
                ctx.status(200)
                ctx.res().addHeader(HttpHeaders.LINK, linkHeader)
            }
        }

        @Test
        fun `link header contains no next -- should return list and next=false`() = runTest {
            val result = listRepositories("")

            checkItems(result)
            result.hasMoreItems shouldBe false
        }

        @Test
        fun `link header contains next -- should return list and next=true`() = runTest {
            val result = listRepositories("""<https://some.where>; rel="next"""")

            checkItems(result)
            result.hasMoreItems shouldBe true
        }
    }
}
