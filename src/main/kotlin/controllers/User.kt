package dev.jhyub.controllers

import dev.jhyub.UserTokens
import dev.jhyub.Users
import dev.jhyub.models.User
import dev.jhyub.models.UserToken
import dev.jhyub.sha256
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.reflect.typeInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import org.jetbrains.exposed.sql.transactions.transaction
import javax.swing.UIManager.put

fun Route.user() {
    route("/users") {
        route("/{user_id}") {
            get {
                login { user, _ ->
                    @Serializable
                    data class Response(val id: Int, val name: String, val email: String)
                    call.respond(
                        HttpStatusCode.OK,
                        Response(user.id.value, user.name, user.email),
                    )
                }
            }

            post("/update") {
                val param = call.receiveParameters()
                val name = param["name"]
                val password = param["password"]
                // We won't allow changing email

                login(param) { user, _ ->
                    name?.let { transaction { user.name = name } }
                    // Password change will expire all tokens
                    password?.let {
                        transaction {
                            user.password = sha256(password)
                            user.tokens.forEach { it.isExpired = true }
                        }
                    }
                    call.respond(HttpStatusCode.OK)
                }
            }
        }

        post("/login") {
            val param = call.receiveParameters()
            val email = param["email"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val password = param["password"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val user = transaction {
                User.find { Users.email eq email }.firstOrNull()
            } ?: return@post call.respond(HttpStatusCode.NotFound)

            if (sha256(password) != user.password) return@post call.respond(HttpStatusCode.Forbidden)

            val token = transaction {
                UserToken.new {
                    owner = user
                    value = sha256(user.email + System.currentTimeMillis())
                    isExpired = false
                }.value
            }

            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "token" to token
                )
            )
        }

        post("/create") {
            val param = call.receiveParameters()
            val name = param["name"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val email = param["email"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val password = param["password"] ?: return@post call.respond(HttpStatusCode.BadRequest)

            if (transaction { User.find { Users.email eq email }.firstOrNull() } != null) {
                return@post call.respond(HttpStatusCode.BadRequest)
            }

            val id = transaction {
                User.new {
                    this.name = name
                    this.email = email
                    this.password = sha256(password)
                }.id.value
            }
            call.respond(
                HttpStatusCode.Created,
                mapOf("id" to id)
            )
        }

        route("/tokens/{token}") {
            get {
                val token = call.parameters["token"] ?: return@get call.respond(HttpStatusCode.BadRequest)

                val (owner, isExpired) = transaction {
                    UserToken.find { UserTokens.value eq token }.firstOrNull()?.let { it.owner to it.isExpired }
                } ?: return@get call.respond(HttpStatusCode.NotFound)

                @Serializable
                data class Response(val owner: Int, val is_expired: Boolean)
                call.respond(
                    HttpStatusCode.OK,
                    Response(owner.id.value, isExpired)
                )
            }

            post("/expire") {
                val token = call.parameters["token"]
                if (token == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }

                val tokenObj = transaction {
                    UserToken.find { UserTokens.value eq token }.firstOrNull()
                } ?: return@post call.respond(HttpStatusCode.NotFound)

                transaction { tokenObj.isExpired = true }
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}