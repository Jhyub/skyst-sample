package dev.jhyub.models

import dev.jhyub.Tasks
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Task(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<Task>(Tasks)

    var name by Tasks.name
    var description by Tasks.description
    var dueDate by Tasks.dueDate
    var isCompleted by Tasks.isCompleted
    var owner by User referencedOn Tasks.owner
}