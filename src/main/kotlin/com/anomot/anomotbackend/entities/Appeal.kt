package com.anomot.anomotbackend.entities

import com.anomot.anomotbackend.utils.AppealObjective
import com.anomot.anomotbackend.utils.AppealReason
import java.util.*
import javax.persistence.*

@Table(
        uniqueConstraints=[UniqueConstraint(columnNames=arrayOf("media_id", "appealed_by_id"))]
)
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
        var decided: Boolean = false,
        @ManyToOne
        var decision: AppealDecision? = null,
        var creationDate: Date? = Date(),
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
)