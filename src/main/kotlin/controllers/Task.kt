package dev.jhyub.controllers

import dev.jhyub.Tasks
import dev.jhyub.models.Task
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun Route.task() {
    route("/tasks") {
        get {
            login { user, params ->
                val date = call.request.queryParameters["date"] ?: params["date"]?.let {
                    val data = it.split("/").map(String::toInt)
                    LocalDate.of(data[0], data[1], data[2])
                }
                val noDetail = call.request.queryParameters["no_detail"]?.toBoolean() ?: params["no_detail"]?.toBoolean() ?: false

                val tasks = transaction { user.tasks.filter { date == null || it.dueDate == date } }

                @Serializable
                data class Response(val id: Int, val name: String, val description: String?, val due_date: String, val is_completed: Boolean)
                call.respond(
                    HttpStatusCode.OK,
                    if (noDetail) mapOf("tasks" to tasks.map { mapOf("id" to it.id.value) })
                            else mapOf("tasks" to tasks.map { Response(it.id.value, it.name, it.description, it.dueDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")), it.isCompleted) })
                )
            }
        }

        post("/create") {
            val param = call.receiveParameters()
            val name = param["name"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val description = param["description"]
            val dueDate = (param["due_date"] ?: return@post call.respond(HttpStatusCode.BadRequest)).let {
                val data = it.split("/").map(String::toInt)
                LocalDate.of(data[0], data[1], data[2])
            }

            login(param) { user, _ ->
                val id = transaction {
                    Task.new {
                        this.name = name
                        description?.let { this.description = it }
                        this.dueDate = dueDate
                        this.owner = user
                    }.id.value
                }
                call.respond(
                    HttpStatusCode.Created,
                    mapOf("id" to id)
                )
            }
        }

        route("/{id}") {
            get {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                login { user, _ ->
                    val task = transaction { Task.findById(id) } ?: return@login call.respond(HttpStatusCode.NotFound)
                    if (transaction { task.owner.id != user.id }) return@login call.respond(HttpStatusCode.Forbidden)

                    @Serializable
                    data class Response(val id: Int, val name: String, val description: String?, val due_date: String, val is_completed: Boolean)
                    call.respond(
                        HttpStatusCode.OK,
                        Response(task.id.value, task.name, task.description, task.dueDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")), task.isCompleted)
                    )
                }
            }

            post("/update") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                val params = call.receiveParameters()
                val name = params["name"]
                val description = params["description"]
                val dueDate = params["due_date"]
                login(params) { user, _ ->
                    val task = transaction { Task.findById(id) } ?: return@login call.respond(HttpStatusCode.NotFound)
                    if (transaction { task.owner.id != user.id }) return@login call.respond(HttpStatusCode.Forbidden)
                    name?.let { transaction { task.name = it } }
                    description?.let { transaction { task.description = it } }
                    dueDate?.let { transaction { task.dueDate = LocalDate.parse(it) } }
                    call.respond(HttpStatusCode.OK)
                }
            }

            post("/delete") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                login { user, _ ->
                    val task = transaction { Task.findById(id) } ?: return@login call.respond(HttpStatusCode.NotFound)
                    if (transaction { task.owner.id != user.id }) return@login call.respond(HttpStatusCode.Forbidden)
                    transaction { task.delete() }
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}