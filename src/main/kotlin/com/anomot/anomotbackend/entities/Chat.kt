package com.anomot.anomotbackend.entities

import com.anomot.anomotbackend.utils.Constants
import java.io.Serializable
import java.util.*
import javax.persistence.*

@Entity
class Chat(
        @Column(length = Constants.MAX_CHAT_TITLE_LENGTH)
        val title: String,
        @Column(columnDefinition="TEXT")
        val description: String?,
        @Column(columnDefinition="TEXT")
        val info: String?,
        val password: String?,
        val creationDate: Date = Date(),
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
): Serializable