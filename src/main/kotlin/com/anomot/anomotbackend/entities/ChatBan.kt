package com.anomot.anomotbackend.entities

import java.io.Serializable
import java.util.*
import javax.persistence.*

@Entity
class ChatBan(
        @ManyToOne
        val user: User,
        @Column(columnDefinition="TEXT")
        val reason: String,
        @ManyToOne
        val bannedBy: User,
        val until: Date,
        val creationDate: Date = Date(),
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
): Serializable