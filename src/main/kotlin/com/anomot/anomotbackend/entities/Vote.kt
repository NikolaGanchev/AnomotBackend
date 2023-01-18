package com.anomot.anomotbackend.entities

import java.io.Serializable
import java.util.*
import javax.persistence.*

@Entity
class Vote(
        @ManyToOne
        val battle: Battle,
        @ManyToOne
        val post: Post?,
        @ManyToOne
        val voter: User,
        var creationDate: Date = Date(),
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
): Serializable