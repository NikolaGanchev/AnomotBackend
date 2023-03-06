package com.anomot.anomotbackend.entities

import java.io.Serializable
import java.util.*
import javax.persistence.*

@Entity
class ChatMessage(
        @ManyToOne
        val member: ChatMember,
        @Column(columnDefinition="TEXT")
        val message: String,
        val isSystem: Boolean = false,
        val creationDate: Date = Date(),
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
): Serializable