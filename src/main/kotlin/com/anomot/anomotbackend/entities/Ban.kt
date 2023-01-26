package com.anomot.anomotbackend.entities

import java.util.Date
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne

@Entity
class Ban(
        @ManyToOne
        val user: User,
        val until: Date,
        @ManyToOne
        val bannedBy: User?,
        val reason: String,
        val creationDate: Date = Date(),
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
)