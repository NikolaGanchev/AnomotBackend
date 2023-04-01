package com.anomot.anomotbackend.entities

import com.anomot.anomotbackend.utils.Constants
import java.io.Serializable
import java.util.*
import javax.persistence.*

@Entity
class ChatMember(
        @ManyToOne
        val chat: Chat,
        @ManyToOne
        val user: User,
        @Column(length = Constants.USERNAME_MAX_LENGTH)
        var chatUsername: String,
        val creationDate: Date = Date(),
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
): Serializable