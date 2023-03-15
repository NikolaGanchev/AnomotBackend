package com.anomot.anomotbackend.entities

import com.anomot.anomotbackend.utils.PostType
import java.io.Serializable
import java.util.*
import jakarta.persistence.*

@Entity
class Battle(
        @OneToOne
        val goldPost: Post?,
        @OneToOne
        val redPost: Post?,
        @Enumerated(EnumType.ORDINAL)
        val type: PostType,
        var totalVotePossibilities: Int,
        var creationDate: Date = Date(),
        var finishDate: Date? = null,
        var finished: Boolean = false,
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
): Serializable