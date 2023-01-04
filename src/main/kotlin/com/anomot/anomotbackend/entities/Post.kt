package com.anomot.anomotbackend.entities

import com.anomot.anomotbackend.utils.PostType
import java.io.Serializable
import java.util.*
import javax.persistence.*

@Entity
class Post(
        @ManyToOne
        val poster: User,
        @OneToOne
        val media: Media?,
        @Column(columnDefinition="TEXT")
        val text: String?,
        @Enumerated(EnumType.ORDINAL)
        val type: PostType,
        var creationDate: Date = Date(),
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
): Serializable