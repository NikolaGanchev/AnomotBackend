package com.anomot.anomotbackend.entities

import com.anomot.anomotbackend.utils.NotificationType
import java.io.Serializable
import java.util.*
import javax.persistence.*

@Entity
@Inheritance(
        strategy = InheritanceType.JOINED
)
class Notification(
        @ManyToOne
        val user: User,
        @Enumerated(EnumType.ORDINAL)
        val type: NotificationType,
        var creationDate: Date = Date(),
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
): Serializable