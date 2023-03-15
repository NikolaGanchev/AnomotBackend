package com.anomot.anomotbackend.entities

import com.anomot.anomotbackend.utils.ReportReason
import java.util.*
import jakarta.persistence.*

@Table(
        uniqueConstraints=[UniqueConstraint(columnNames=arrayOf("report_ticket_id", "reportReason", "reporter_id"))]
)
@Entity
class Report(
        @ManyToOne
        val reporter: User?,
        @ManyToOne
        val reportTicket: ReportTicket,
        @Enumerated(EnumType.ORDINAL)
        val reportReason: ReportReason,
        @Column(columnDefinition="TEXT")
        val other: String?,
        var creationDate: Date = Date(),
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
)