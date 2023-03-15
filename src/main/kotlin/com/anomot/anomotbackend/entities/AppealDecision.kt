package com.anomot.anomotbackend.entities

import com.anomot.anomotbackend.utils.AppealAction
import java.util.*
import jakarta.persistence.*

@Entity
class AppealDecision(
        @ManyToOne
        val decidedBy: User? = null,
        @Enumerated(EnumType.ORDINAL)
        val decision: AppealAction,
        @Column(columnDefinition="TEXT")
        val explanation: String,
        var creationDate: Date = Date(),
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
)