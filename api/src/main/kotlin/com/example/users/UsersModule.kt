package com.example.users

import io.ktor.http.HttpStatusCode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

@Serializable
data class User(
    val id: Int,
    val name: String,
    val username: String,
    val email: String,
    val address: Address,
    val phone: String,
    val website: String,
    val company: Company
)

@Serializable
data class Address(
    val street: String,
    val suite: String,
    val city: String,
    val zipcode: String,
    val geo: Geo
)

@Serializable
data class Geo(
    val lat: String,
    val lng: String
)

@Serializable
data class Company(
    val name: String,
    val catchPhrase: String,
    val bs: String
)

fun Application.usersModule() {

    //Call user service
    val service = UsersService()
    service.start()
    monitor.subscribe(ApplicationStopped) {
        service.close()
    }

    routing {
        get("/users") {
            val users = service.cachedUsers()
            if (users == null) {
                call.response.headers.append(HttpHeaders.RetryAfter, "15")
                call.respondText(
                    "User data is not available yet; retry shortly.",
                    status = HttpStatusCode.ServiceUnavailable
                )
            } else {
                call.respond(users)
            }
        }
    }
}

private class UsersService {
    private val logger = LoggerFactory.getLogger(UsersService::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshMutex = Mutex()
    private val upstreamProxy = System.getenv("HTTPS_PROXY")
        ?.takeIf(String::isNotBlank)
        ?: System.getenv("https_proxy")
        ?.takeIf(String::isNotBlank)
        ?: System.getenv("HTTP_PROXY")
        ?.takeIf(String::isNotBlank)
        ?: System.getenv("http_proxy")
        ?.takeIf(String::isNotBlank)
    private val client = HttpClient(CIO) {
        engine {
            upstreamProxy?.let { proxy = ProxyBuilder.http(URLBuilder(it).build()) }
        }
        expectSuccess = true
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
            connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
            socketTimeoutMillis = REQUEST_TIMEOUT_MILLIS
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    init {
        if (upstreamProxy != null) {
            logger.info("Using configured HTTP proxy for upstream user requests")
        }
    }

    @Volatile
    private var users: List<User>? = null

    fun start() {
        scope.launch {
            while (isActive) {
                val nextFetchDelay = try {
                    refreshUsers()
                    POLL_INTERVAL_MILLIS
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    logger.error("Failed to fetch users from {}", USERS_URL, exception)
                    RETRY_INTERVAL_MILLIS
                }
                delay(nextFetchDelay)
            }
        }
    }

    fun cachedUsers(): List<User>? = users

    private suspend fun refreshUsers() {
        refreshMutex.withLock {
            fetchUsers()
        }
    }

    private suspend fun fetchUsers(): List<User> {
        val fetchedUsers: List<User> = client.get(USERS_URL).body()
        users = fetchedUsers
        logger.info("Fetched {} users from {}", fetchedUsers.size, USERS_URL)
        return fetchedUsers
    }

    fun close() {
        scope.cancel()
        client.close()
    }

    private companion object {
        const val USERS_URL = "https://jsonplaceholder.typicode.com/users"
        const val POLL_INTERVAL_MILLIS = 10 * 60 * 1000L
        const val RETRY_INTERVAL_MILLIS = 15 * 1000L
        const val CONNECT_TIMEOUT_MILLIS = 5 * 1000L
        const val REQUEST_TIMEOUT_MILLIS = 10 * 1000L
    }
}
