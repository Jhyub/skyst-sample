package dev.jhyub.models

import dev.jhyub.UserTokens
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class UserToken(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<UserToken>(UserTokens)

    var value by UserTokens.value
    var isExpired by UserTokens.isExpired
    var owner by User referencedOn UserTokens.owner
}