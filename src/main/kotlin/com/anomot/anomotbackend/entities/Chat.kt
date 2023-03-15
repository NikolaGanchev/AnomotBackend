package com.anomot.anomotbackend.entities

import com.anomot.anomotbackend.utils.Constants
import java.io.Serializable
import java.util.*
import jakarta.persistence.*

@Entity
class Chat(
        @Column(length = Constants.MAX_CHAT_TITLE_LENGTH)
        var title: String,
        @Column(columnDefinition="TEXT")
        var description: String?,
        @Column(columnDefinition="TEXT")
        var info: String?,
        var password: String?,
        val creationDate: Date = Date(),
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
): Serializable