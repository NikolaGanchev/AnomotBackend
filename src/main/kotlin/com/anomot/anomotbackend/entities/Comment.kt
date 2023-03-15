package com.anomot.anomotbackend.entities

import java.io.Serializable
import java.util.*
import jakarta.persistence.*

@Entity
class Comment(
        @Column(columnDefinition="TEXT")
        var text: String,
        @ManyToOne
        val parentBattle: Battle?,
        @ManyToOne
        val parentPost: Post?,
        @ManyToOne
        val parentComment: Comment?,
        @ManyToOne
        val commenter: User?,
        val isDeleted: Boolean = false,
        var isEdited: Boolean = false,
        var creationDate: Date? = Date(),
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
): Serializable