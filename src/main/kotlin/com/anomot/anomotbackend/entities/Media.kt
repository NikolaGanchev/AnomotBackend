package com.anomot.anomotbackend.entities

import com.anomot.anomotbackend.utils.MediaType
import java.io.Serializable
import java.util.*
import javax.persistence.*

@Entity
class Media(
        @Column(columnDefinition = "uuid")
        var name: UUID,
        var duration: Float?,
        var phash: ByteArray?,
        @Enumerated(EnumType.ORDINAL)
        var mediaType: MediaType,
        var creationDate: Date = Date(),
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
): Serializable