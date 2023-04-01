package com.anomot.anomotbackend.entities

import com.anomot.anomotbackend.utils.ChatRoles
import java.io.Serializable
import java.util.*
import javax.persistence.*

@Entity
class ChatRole(
        @ManyToOne
        val chatMember: ChatMember,
        @Enumerated(EnumType.ORDINAL)
        val role: ChatRoles,
        val creationDate: Date = Date(),
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
): Serializable