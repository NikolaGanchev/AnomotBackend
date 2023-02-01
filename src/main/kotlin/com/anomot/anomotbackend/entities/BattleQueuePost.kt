package com.anomot.anomotbackend.entities

import java.io.Serializable
import javax.persistence.*

@Entity
class BattleQueuePost(
        @OneToOne
        @JoinColumn(unique = true)
        val post: Post,
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
): Serializable