package dev.jhyub

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction

object Database {
    private val url = "jdbc:h2:~/skyst-sample.db;DB_CLOSE_DELAY=-1"
    private val user = "jhyub"
    private val driver = "org.h2.Driver"
    private val password = System.getenv("H2_PASSWORD") ?: "snucse"

    init {
        org.jetbrains.exposed.sql.Database.connect(url, driver, user, password)
        transaction {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(Users, UserTokens, Tasks)
        }
        println("Database connected")
    }
}

object Users : IntIdTable() {
    val name = varchar("name", length = 50)
    val email = varchar("email", length = 50).uniqueIndex()
    val password = varchar("password", length = 64)
}

object UserTokens : IntIdTable() {
    val value = varchar("value", length = 64).uniqueIndex()
    val isExpired = bool("is_expired").default(false)
    val owner = reference("owner", Users)
}

object Tasks : IntIdTable() {
    val name = varchar("name", length = 50)
    val description = varchar("description", length = 400).nullable()
    val dueDate = date("due_date")
    val isCompleted = bool("is_completed").default(false)
    val owner = reference("owner", Users)
}
