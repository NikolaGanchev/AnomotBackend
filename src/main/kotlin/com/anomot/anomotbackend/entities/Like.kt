package com.anomot.anomotbackend.entities

import java.io.Serializable
import java.util.*
import javax.persistence.*

@Entity
class Like(
        @ManyToOne
        val post: Post?,
        @ManyToOne
        val likedBy: User,
        var creationDate: Date = Date(),
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
): Serializable