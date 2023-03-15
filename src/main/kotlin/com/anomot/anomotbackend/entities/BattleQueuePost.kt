package com.anomot.anomotbackend.entities

import java.io.Serializable
import java.util.*
import jakarta.persistence.*

@Entity
class BattleQueuePost(
        @OneToOne
        @JoinColumn(unique = true)
        val post: Post,
        var creationDate: Date = Date(),
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
): Serializable