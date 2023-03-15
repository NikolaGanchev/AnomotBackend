package com.anomot.anomotbackend.entities

import java.io.Serializable
import java.util.*
import jakarta.persistence.*

@Table(
        uniqueConstraints=[UniqueConstraint(columnNames=arrayOf("post_id", "liked_by_id"))]
)
@Entity
class Like(
        @ManyToOne
        val post: Post,
        @ManyToOne
        val likedBy: User,
        var creationDate: Date = Date(),
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
): Serializable