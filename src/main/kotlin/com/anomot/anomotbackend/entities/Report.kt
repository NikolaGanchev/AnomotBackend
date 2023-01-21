package com.anomot.anomotbackend.entities

import com.anomot.anomotbackend.utils.ReportReason
import com.anomot.anomotbackend.utils.ReportType
import java.util.*
import javax.persistence.*

@Entity
class Report(
        @ManyToOne
        val reporter: User,
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
        @Enumerated(EnumType.ORDINAL)
        val reportReason: ReportReason,
        @Column(columnDefinition="TEXT")
        val other: String?,
        @Column(columnDefinition = "uuid")
        val reportId: UUID,
        val decided: Boolean = false,
        @ManyToOne
        val decision: ReportDecision?,
        var creationDate: Date? = Date(),
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
)