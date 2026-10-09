package com.example

import com.example.users.usersModule
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

@Serializable
data class Message(val text: String)

@Serializable
data class Health(val status: String)

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) { json() }
    usersModule()

    routing {
        // GET /  -> plain text
        get("/") {
            call.respondText("Jap, kleiner Demo Webservice auf Codespaces ;-) ")
        }

        // GET /health  -> JSON
        get("/health") {
            call.respond(Health("ok"))
        }

        // POST /echo  -> echoes JSON body back
        post("/echo") {
            val msg = call.receive<Message>()
            call.respond(HttpStatusCode.OK, Message("You said: ${msg.text}"))
        }
    }
}
