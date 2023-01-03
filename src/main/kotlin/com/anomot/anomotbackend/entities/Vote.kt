package com.anomot.anomotbackend.entities

import java.io.Serializable
import javax.persistence.*

@Entity
class Vote(
        @ManyToOne
        val battle: Battle,
        @ManyToOne
        val post: Post,
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
): Serializable