package com.anomot.anomotbackend.entities

import com.anomot.anomotbackend.utils.PostType
import java.io.Serializable
import java.util.*
import javax.persistence.*

@Entity
class Battle(
        @OneToOne
        val goldPost: Post,
        @OneToOne
        val redPost: Post,
        @Enumerated(EnumType.ORDINAL)
        val type: PostType,
        var creationDate: Date = Date(),
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
): Serializable