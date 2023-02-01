package com.anomot.anomotbackend.entities

import java.io.Serializable
import java.util.*
import javax.persistence.*

@Table(
        uniqueConstraints=[UniqueConstraint(columnNames=arrayOf("comment_id", "liked_by_id"))]
)
@Entity
class CommentLike(
        @ManyToOne
        val comment: Comment,
        @ManyToOne
        val likedBy: User,
        var creationDate: Date = Date(),
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
): Serializable