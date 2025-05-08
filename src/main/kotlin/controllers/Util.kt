package dev.jhyub.controllers

import dev.jhyub.UserTokens
import dev.jhyub.models.User
import dev.jhyub.models.UserToken
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.transactions.transaction


suspend fun RoutingContext.login(p: Parameters? = null, body: suspend RoutingContext.(User, Parameters)->Unit) {
    val params = p ?: call.receiveParameters()
    val tokenStr = call.request.queryParameters["token"] ?: params["token"]

    if (tokenStr == null) {
        println("tokenStr is null")
        return call.respond(HttpStatusCode.BadRequest)
    }

    return tokenStr.let {
        val token = transaction {
            UserToken.find { UserTokens.value eq it }.firstOrNull()
        }

        if (token == null) {
            return call.respond(HttpStatusCode.NotFound)
        }

        if (transaction { token.isExpired }) {
            return call.respond(HttpStatusCode.Forbidden)
        }


        val id = call.parameters["user_id"]?.toIntOrNull()
        id?.let {
            if (transaction { id != token.owner.id.value }) {
                return call.respond(HttpStatusCode.Forbidden)
            }
        }

        body(transaction { token.owner }, params)
    }
}
