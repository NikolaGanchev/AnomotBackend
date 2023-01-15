package com.anomot.anomotbackend.entities

import java.io.Serializable
import java.util.*
import javax.persistence.*

@Entity
class Comment(
        @Column(columnDefinition="TEXT")
        val text: String,
        @ManyToOne
        val parentBattle: Battle?,
        @ManyToOne
        val parentPost: Post?,
        @ManyToOne
        val parentComment: Comment?,
        @ManyToOne
        val commenter: User?,
        val isDeleted: Boolean = false,
        val isEdited: Boolean = false,
        var creationDate: Date = Date(),
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
): Serializable