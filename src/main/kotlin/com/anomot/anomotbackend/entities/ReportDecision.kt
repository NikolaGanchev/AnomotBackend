package com.anomot.anomotbackend.entities

import java.io.Serializable
import java.util.*
import javax.persistence.*

@Entity
class ReportDecision(
        @ManyToOne
        val reportTicket: ReportTicket,
        @Column(columnDefinition="TEXT")
        val decision: String,
        @ManyToOne
        val decidedBy: User?,
        var creationDate: Date = Date(),
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
): Serializable
