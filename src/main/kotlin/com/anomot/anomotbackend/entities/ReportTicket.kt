package com.anomot.anomotbackend.entities

import com.anomot.anomotbackend.utils.ReportType
import java.util.*
import javax.persistence.*

@Entity
class ReportTicket(
        @Enumerated(EnumType.ORDINAL)
        val type: ReportType,
        @ManyToOne
        val post: Post?,
        @ManyToOne
        val battle: Battle?,
        @ManyToOne
        val comment: Comment?,
        @ManyToOne
        val user: User?,
        var decided: Boolean = false,
        var creationDate: Date = Date(),
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
)