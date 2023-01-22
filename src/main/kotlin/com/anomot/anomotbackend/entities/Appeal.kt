package com.anomot.anomotbackend.entities

import com.anomot.anomotbackend.utils.AppealObjective
import com.anomot.anomotbackend.utils.AppealReason
import java.util.*
import javax.persistence.*

@Entity
class Appeal(
        @ManyToOne
        val appealedBy: User,
        @Enumerated(EnumType.ORDINAL)
        val reason: AppealReason,
        @Enumerated(EnumType.ORDINAL)
        val objective: AppealObjective,
        @ManyToOne
        val media: Media,
        val decided: Boolean = false,
        @ManyToOne
        val decidedBy: User? = null,
        @Column(columnDefinition="TEXT")
        val decision: String? = null,
        var creationDate: Date? = Date(),
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
)