package dev.jhyub

import dev.jhyub.controllers.task
import dev.jhyub.controllers.user
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respond(
                HttpStatusCode.OK,
                mapOf("msg" to "삐약이\uD83D\uDC23", "version" to "0.1.0")
            )
        }
        user()
        task()
    }
}
